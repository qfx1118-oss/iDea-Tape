package cn.ideatape.recorder;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.location.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.*;
public final class TapeService extends Service implements BleLink.Events {
 public static final String CONNECT="connect",PHONE_START="phone-start",PHONE_STOP="phone-stop",STOP="stop",POSITION="position";
 public static volatile String placeStatus="等待设备连接";
 public static volatile String status="连接灵感磁带，或直接用手机录音";
 public static volatile boolean connected,phoneRecording,tapeRecording;public static volatile long phoneStarted,tapeSamples;
 public static volatile int libraryVersion;public static volatile long locationFixElapsed;public static volatile float locationAccuracy;private HandlerThread worker;private Handler io;
 private Repository repo;private BleLink link;private final Wire.Parser parser=new Wire.Parser();private PhoneRecorder phone;
 private String device="",key="";private Transfer transfer;private Wire.Meta current;private JSONObject location;
 private LocationManager locations;private Location latest;private boolean locationOn,phoneSession,wantsDevice,stopAfterPhone;private long lastRequest,lastList;private int rangeStart,rangeCount;private boolean awaiting;
 private final java.util.concurrent.ExecutorService geocoding=java.util.concurrent.Executors.newSingleThreadExecutor();
 private boolean geocodingBusy;private int placeGeneration;private long lastLookup,lastPlaceSend;private String road="";private Location roadFix;
 private boolean addressEnabled(){return locationOn&&getSharedPreferences("settings",0).getBoolean("shareAddress",false);}
 private void sendPlace(int state,String text){if(connected){link.send(Wire.PLACE,RoadLabel.packet(state,text));lastPlaceSend=SystemClock.elapsedRealtime();}}
 @SuppressWarnings("deprecation") private void updatePlace(){
  long now=SystemClock.elapsedRealtime();
  if(!connected){placeStatus="等待设备连接";return;}
  if(!addressEnabled()){placeGeneration++;road="";roadFix=null;placeStatus="设备地点显示已关闭";if(now-lastPlaceSend>5000)sendPlace(0,"");return;}
  if(latest==null||now-latest.getElapsedRealtimeNanos()/1000000>10000){placeGeneration++;road="";roadFix=null;placeStatus="等待新的定位";if(now-lastPlaceSend>5000)sendPlace(1,"");return;}
  if(roadFix!=null&&latest.distanceTo(roadFix)<80&&now-lastLookup<60000){placeStatus=road;if(now-lastPlaceSend>10000)sendPlace(2,road);return;}
  if(roadFix!=null&&latest.distanceTo(roadFix)>=80){roadFix=null;road="";placeStatus="正在更新道路名…";sendPlace(1,"");}
  if(!geocodingBusy&&now-lastLookup>=30000){
   if(!Geocoder.isPresent()){placeStatus="系统地址服务不可用";if(now-lastPlaceSend>10000)sendPlace(3,"");return;}
   final Location fix=new Location(latest);final int generation=++placeGeneration;geocodingBusy=true;roadFix=null;road="";lastLookup=now;placeStatus="正在解析街道…";sendPlace(1,"");
   geocoding.execute(()->{String label="";try{java.util.List<Address> result=new Geocoder(this,Locale.SIMPLIFIED_CHINESE).getFromLocation(fix.getLatitude(),fix.getLongitude(),1);if(result!=null&&!result.isEmpty()){Address a=result.get(0);label=RoadLabel.format(a.getThoroughfare(),a.getSubLocality(),a.getLocality());}}catch(Exception ignored){}final String resolved=label;io.post(()->{geocodingBusy=false;if(generation!=placeGeneration||!connected||!addressEnabled())return;if(latest==null||SystemClock.elapsedRealtime()-latest.getElapsedRealtimeNanos()/1000000>10000||latest.distanceTo(fix)>=80)return;road=resolved;roadFix=resolved.isEmpty()?null:fix;placeStatus=resolved.isEmpty()?"地址暂不可用":resolved;sendPlace(resolved.isEmpty()?3:2,resolved);});});
  }else if(!geocodingBusy&&roadFix==null&&now-lastPlaceSend>10000)sendPlace(3,"");
 }
 private final LocationListener positionListener=new LocationListener(){public void onLocationChanged(Location l){latest=l;locationFixElapsed=l.getElapsedRealtimeNanos()/1000000;locationAccuracy=l.hasAccuracy()?l.getAccuracy():-1;}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int s,Bundle b){}};
 public void onCreate(){super.onCreate();repo=new Repository(this);worker=new HandlerThread("tape_sync");worker.start();io=new Handler(worker.getLooper());link=new BleLink(this,io,this);phone=new PhoneRecorder();locations=getSystemService(LocationManager.class);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("sync","录音与设备同步",NotificationManager.IMPORTANCE_LOW));io.postDelayed(this::tick,1000);}
 public int onStartCommand(Intent intent,int flags,int id){if(intent==null){stopSelf();return START_NOT_STICKY;}String action=intent.getAction();
  locationOn=getSharedPreferences("settings",0).getBoolean("location",false)&&locationPermission();
  if(PHONE_START.equals(action)){phoneSession=true;stopAfterPhone=false;}
  if(CONNECT.equals(action)){wantsDevice=true;stopAfterPhone=false;}
  if(STOP.equals(action))wantsDevice=false;
  foreground();io.post(()->{try{
   updateLocation();
   if(POSITION.equals(action)){placeGeneration++;road="";roadFix=null;lastLookup=0;lastPlaceSend=0;updatePlace();}
   if(CONNECT.equals(action)){String addr=intent.getStringExtra("address");if(addr!=null)link.connect(addr);}
   else if(PHONE_START.equals(action)){if(phoneRecording)return;phoneRecording=true;phoneStarted=System.currentTimeMillis();phone.start(repo,snapshot(),(j,e)->io.post(()->{phoneRecording=false;phoneSession=false;if(e==null){status("手机录音已保存");libraryVersion++;}else status("录音未保存："+e.getMessage());if(stopAfterPhone||(!wantsDevice&&!locationOn)){stopForeground(true);stopSelf();}else foreground();}));status("手机正在录音…");}
   else if(PHONE_STOP.equals(action)){phone.stop();status("正在保存手机录音…");}
   else if(STOP.equals(action)){stopAfterPhone=true;link.close();connected=false;tapeRecording=false;closeTransfer();if(!phoneRecording){stopForeground(true);stopSelf();}else status("已停止设备接收，手机录音继续");}
  }catch(Exception e){if(PHONE_START.equals(action)){phoneRecording=false;phoneSession=false;}status("操作失败："+e.getMessage());foreground();}});return START_NOT_STICKY;
 }
 private boolean locationPermission(){return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 private void foreground(){
  Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n=new Notification.Builder(this,"sync").setSmallIcon(cn.ideatape.recorder.R.drawable.ic_tape).setContentTitle(phoneRecording?"灵感磁带 · 手机录音中":"灵感磁带 · 同步已开启").setContentText(status).setContentIntent(pi).setOngoing(true).build();
  int type=0;
  if(Build.VERSION.SDK_INT>=29){
   boolean ble=Build.VERSION.SDK_INT<31||checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
   if(ble&&wantsDevice)type|=ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
   if(locationOn)type|=ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
   if(phoneSession&&Build.VERSION.SDK_INT>=30)type|=ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
   if(type!=0||phoneSession)startForeground(7,n,type);else{stopForeground(true);stopSelf();}
  }else startForeground(7,n);
 }
 private void updateLocation(){
  try{locations.removeUpdates(positionListener);if(!locationOn)return;
   for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){
    if(!locations.getAllProviders().contains(provider))continue;
    try{Location l=locations.getLastKnownLocation(provider);if(l!=null&&(latest==null||l.getElapsedRealtimeNanos()>latest.getElapsedRealtimeNanos()))latest=l;if(latest!=null){locationFixElapsed=latest.getElapsedRealtimeNanos()/1000000;locationAccuracy=latest.hasAccuracy()?latest.getAccuracy():-1;}locations.requestLocationUpdates(provider,3000,0,positionListener,io.getLooper());}catch(SecurityException ignored){}
   }
  }catch(Exception e){status("定位暂不可用，录音仍会保存");}
 }
 private JSONObject snapshot(){
  if(!locationOn||latest==null)return null;long age=SystemClock.elapsedRealtimeNanos()-latest.getElapsedRealtimeNanos();if(age<0||age>10000000000L)return null;
  try{return new JSONObject().put("latitude",latest.getLatitude()).put("longitude",latest.getLongitude()).put("accuracyMeters",latest.hasAccuracy()?latest.getAccuracy():JSONObject.NULL).put("observedAt",latest.getTime()).put("ageMillis",age/1000000).put("provider",latest.getProvider());}catch(JSONException e){return null;}
 }
 public void status(String s){status=s;}
 public void ready(String addr){placeGeneration++;lastPlaceSend=0;lastLookup=0;roadFix=null;road="";device=addr;connected=true;parser.reset();status("已连接，正在同步设备录音…");byte[] clock=new byte[8];Wire.put64(clock,0,System.currentTimeMillis());link.send(Wire.TIME,clock);link.send(Wire.LIST,new byte[0]);lastList=SystemClock.elapsedRealtime();}
 public void disconnected(){placeGeneration++;road="";roadFix=null;placeStatus="等待设备连接";connected=false;tapeRecording=false;parser.reset();closeTransfer();current=null;awaiting=false;}
 public void bytes(byte[] b){parser.feed(b,(type,data)->{try{packet(type,data);}catch(Exception e){status("同步未完成，设备缓存保留："+e.getMessage());awaiting=false;}});}
 private void packet(int type,byte[] b)throws Exception {
  if(type==Wire.META||type==Wire.END){
   Wire.Meta m=new Wire.Meta(b);String next=m.key(device);
   if(m.complete()&&(repo.wasDeleted(next,m)||repo.alreadySaved(next,m))){link.send(Wire.COMMIT,m.commit());lastRequest=SystemClock.elapsedRealtime();return;}
   if(!next.equals(key)||transfer==null){
    closeTransfer();key=next;transfer=new Transfer(repo.cache,key);location=null;
    File context=new File(repo.cache,key+".context.json");
    if(context.isFile())try{JSONObject j=new JSONObject(new String(java.nio.file.Files.readAllBytes(context.toPath()),"UTF-8"));location=j.optJSONObject("location");}catch(Exception ignored){}
    else if((m.flags&4)!=0){location=snapshot();JSONObject j=new JSONObject().put("location",location==null?JSONObject.NULL:location);Repository.atomic(context,j.toString().getBytes("UTF-8"));}
   }
   current=m;tapeRecording=m.live();tapeSamples=(long)transfer.received()*505;
   progress(false,true);
   if(m.complete()){awaiting=false;finishOrPull();}else status("设备正在录音，手机实时接收…");
  }else if(type==Wire.DATA){
   if(b.length!=272||current==null||transfer==null||Wire.i32(b,0)!=current.id||Wire.i64(b,4)!=current.token)return;
   int index=Wire.i32(b,12);if(current.complete()&&index>=current.blocks)return;
   transfer.receive(index,Arrays.copyOfRange(b,16,272));tapeSamples=(long)transfer.received()*505;progress(false,false);
   if(current.complete()&&(!awaiting||transfer.hasRange(rangeStart,rangeCount))){awaiting=false;finishOrPull();}
  }else if(type==Wire.DELETED){status("录音已安全保存，设备缓存已清理");link.send(Wire.LIST,new byte[0]);lastList=SystemClock.elapsedRealtime();}
  else if(type==Wire.EMPTY){closeTransfer();current=null;key="";awaiting=false;tapeRecording=false;status(phoneRecording?"手机正在录音…":"已连接，设备录音已全部接收");}
  else if(type==Wire.ERROR){awaiting=false;if("missing".equals(new String(b,"UTF-8"))){closeTransfer();current=null;key="";status("设备录音已删除，跳过未完成缓存");link.send(Wire.LIST,new byte[0]);}else status("设备忙碌，录音缓存保留，稍后继续同步");}
 }
 private long lastProgress;
 private void progress(boolean saving,boolean force){
  if(current==null||transfer==null)return;long now=SystemClock.elapsedRealtime();if(!force&&now-lastProgress<500)return;
  byte[] p=new byte[21];Wire.put32(p,0,current.id);Wire.put64(p,4,current.token);Wire.put32(p,12,Math.min(transfer.received(),current.blocks));Wire.put32(p,16,current.blocks);p[20]=(byte)(saving?1:0);link.send(Wire.PROGRESS,p);lastProgress=now;
 }
 private void finishOrPull()throws Exception {
  if(current==null||!current.complete()||transfer==null)return;
  int missing=transfer.missing(current.blocks);
  if(missing>=0){rangeStart=missing;rangeCount=Math.min(16,current.blocks-missing);link.send(Wire.PULL,current.pull(rangeStart,rangeCount));awaiting=true;lastRequest=SystemClock.elapsedRealtime();status("正在补传："+transfer.received()+" / "+current.blocks+" 块");return;}
  if(!transfer.verify(current)){transfer.reset();status("校验未通过，正在重新补传，设备缓存保留");finishOrPull();return;}
  progress(true,true);repo.tape(key,device,current,location,transfer);libraryVersion++;Wire.Meta saved=current;
  transfer.close();transfer.remove();transfer=null;new File(repo.cache,key+".context.json").delete();current=null;key="";tapeRecording=false;
  /* A new save requires fsynced WAV + metadata; deliberate later deletion retains a matching receipt. */
  link.send(Wire.COMMIT,saved.commit());lastRequest=SystemClock.elapsedRealtime();status("手机已保存，正在确认设备缓存清理…");
 }
 private void closeTransfer(){if(transfer!=null)try{transfer.close();}catch(IOException ignored){}transfer=null;}
 private void tick(){try{long now=SystemClock.elapsedRealtime();updatePlace();if(connected){if(current!=null&&current.complete()&&now-lastRequest>4000){awaiting=false;finishOrPull();}if(now-lastList>5000&&!awaiting){link.send(Wire.LIST,new byte[0]);lastList=now;}}}catch(Exception e){status("同步暂停，缓存保留："+e.getMessage());}io.postDelayed(this::tick,1000);}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){geocoding.shutdownNow();placeGeneration++;phoneRecording=false;phone.stop();if(io!=null)io.post(()->{link.close();closeTransfer();try{locations.removeUpdates(positionListener);}catch(Exception ignored){}worker.quitSafely();});connected=false;tapeRecording=false;super.onDestroy();}
}

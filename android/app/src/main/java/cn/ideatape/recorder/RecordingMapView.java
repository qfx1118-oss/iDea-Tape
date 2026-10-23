package cn.ideatape.recorder;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import android.view.*;
import org.json.*;
import java.util.*;
/** Interactive OpenStreetMap view with local recording markers and consent-gated visible tiles. */
public final class RecordingMapView extends View {
 public interface Events {void select(ArrayList<JSONObject> recordings);void moved();}
 private final Paint paint=new Paint(3);private final Handler main=new Handler();
 private final ExecutorService network=new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(32));
 private final Set<String> pending=Collections.synchronizedSet(new HashSet<>());
 private final HashMap<String,Long> retry=new HashMap<>();
 private final LruCache<String,Bitmap> images=new LruCache<String,Bitmap>(12*1024*1024){protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
 private final File cache;private volatile boolean online,visible,resumed,closed;
 private final float density;private final GestureDetector gestures;private final ScaleGestureDetector scale;
 private ArrayList<JSONObject> points=new ArrayList<>();public Events events;private boolean fitted;
 private String message="底图未开启；录音标记仍可查看";
 private double cx=.5,cy=.5;private int zoom=3;private float scaleAccum=1;
 public RecordingMapView(Context c){super(c);cache=new File(c.getCacheDir(),"osm-tiles");cache.mkdirs();density=getResources().getDisplayMetrics().density;setContentDescription("录音位置地图，拖动移动，双指缩放；也可用下方列表选择录音");
  gestures=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){public boolean onDown(MotionEvent e){return true;}public boolean onScroll(MotionEvent a,MotionEvent b,float dx,float dy){cx=LibraryRules.wrap(cx+dx/world());cy=Math.max(0,Math.min(1,cy+dy/world()));changed();return true;}public boolean onDoubleTap(MotionEvent e){zoom(1);return true;}public boolean onSingleTapUp(MotionEvent e){pick(e.getX(),e.getY());return true;}});
  scale=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){scaleAccum*=d.getScaleFactor();if(scaleAccum>1.4){zoom(1);scaleAccum=1;}else if(scaleAccum<.7){zoom(-1);scaleAccum=1;}return true;}public boolean onScaleBegin(ScaleGestureDetector d){scaleAccum=1;return true;}});
 }
 public void online(boolean enabled){online=enabled;message=enabled?"正在加载底图…":"底图联网已关闭；录音标记仍可查看";invalidate();}
 public void resume(){resumed=true;invalidate();}public void pause(){resumed=false;}
 public void lowMemory(){images.evictAll();}
 public void onVisibilityAggregated(boolean value){super.onVisibilityAggregated(value);visible=value;}
 private double world(){return tileSize()*(1<<zoom);}private float tileSize(){return 256*density;}
 public void points(ArrayList<JSONObject> value){points=new ArrayList<>(value);if(!fitted&&getWidth()>0&&!points.isEmpty()){fit();}invalidate();}
 public boolean contains(JSONObject j){JSONObject l=j.optJSONObject("location");return l!=null&&LibraryRules.visible(l.optDouble("latitude",Double.NaN),l.optDouble("longitude",Double.NaN),cx,cy,getWidth(),getHeight(),world());}
 public void focus(JSONObject j){JSONObject l=j.optJSONObject("location");if(l==null)return;cx=LibraryRules.x(l.optDouble("longitude"));cy=LibraryRules.y(l.optDouble("latitude"));zoom=15;fitted=true;changed();}
 public void fit(){if(points.isEmpty())return;JSONObject first=points.get(0).optJSONObject("location");if(first==null)return;double base=LibraryRules.x(first.optDouble("longitude")),minx=base,maxx=base,miny=1,maxy=0;
  for(JSONObject j:points){JSONObject l=j.optJSONObject("location");if(l==null)continue;double x=base+LibraryRules.delta(LibraryRules.x(l.optDouble("longitude")),base),y=LibraryRules.y(l.optDouble("latitude"));minx=Math.min(minx,x);maxx=Math.max(maxx,x);miny=Math.min(miny,y);maxy=Math.max(maxy,y);}
  cx=LibraryRules.wrap((minx+maxx)/2);cy=(miny+maxy)/2;zoom=16;while(zoom>1&&((maxx-minx)*world()>getWidth()-80*density||(maxy-miny)*world()>getHeight()-80*density))zoom--;fitted=true;changed();
 }
 public void zoom(int amount){zoom=Math.max(1,Math.min(18,zoom+amount));changed();}
 private void changed(){invalidate();if(events!=null)events.moved();}
 protected void onSizeChanged(int w,int h,int ow,int oh){if(!fitted&&!points.isEmpty())fit();}
 public boolean onTouchEvent(MotionEvent e){getParent().requestDisallowInterceptTouchEvent(true);scale.onTouchEvent(e);if(!scale.isInProgress())gestures.onTouchEvent(e);if(e.getAction()==MotionEvent.ACTION_UP){performClick();getParent().requestDisallowInterceptTouchEvent(false);}return true;}
 public boolean performClick(){super.performClick();return true;}
 private float px(JSONObject l){return (float)(getWidth()/2+LibraryRules.delta(LibraryRules.x(l.optDouble("longitude")),cx)*world());}
 private float py(JSONObject l){return (float)(getHeight()/2+(LibraryRules.y(l.optDouble("latitude"))-cy)*world());}
 private void pick(float x,float y){ArrayList<JSONObject> hit=new ArrayList<>();for(JSONObject j:points){JSONObject l=j.optJSONObject("location");if(l!=null&&Math.hypot(px(l)-x,py(l)-y)<28*density)hit.add(j);}if(!hit.isEmpty()&&events!=null)events.select(hit);}
 protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(0xffdedbcf);int n=1<<zoom;float tile=tileSize();double left=cx*n-getWidth()/(2*tile),top=cy*n-getHeight()/(2*tile);boolean drawn=false;
  for(int yy=(int)Math.floor(top);yy<=Math.floor(top+getHeight()/tile);yy++)for(int xx=(int)Math.floor(left);xx<=Math.floor(left+getWidth()/tile);xx++){
   if(yy<0||yy>=n)continue;int tx=((xx%n)+n)%n;String key=zoom+"-"+tx+"-"+yy;Bitmap bitmap=images.get(key);float x=(float)((xx-left)*tile),y=(float)((yy-top)*tile);
   if(bitmap!=null){c.drawBitmap(bitmap,null,new RectF(x,y,x+tile+1,y+tile+1),paint);drawn=true;}else if(online&&resumed&&visible)load(key,zoom,tx,yy);
  }
  for(JSONObject j:points){JSONObject l=j.optJSONObject("location");if(l==null)continue;float x=px(l),y=py(l);if(x< -24*density||x>getWidth()+24*density||y< -24*density||y>getHeight()+24*density)continue;
   paint.setColor(0xfff8f1df);c.drawCircle(x,y,12*density,paint);paint.setColor(j.optBoolean("favorite")?0xff844829:0xffb34d2c);c.drawCircle(x,y,9*density,paint);paint.setColor(0xfff8f1df);Path play=new Path();play.moveTo(x-2*density,y-4*density);play.lineTo(x+4*density,y);play.lineTo(x-2*density,y+4*density);play.close();c.drawPath(play,paint);
  }
  if(!drawn){paint.setColor(0xfff5efdf);c.drawRect(0,0,getWidth(),30*density,paint);paint.setColor(0xff49493e);paint.setTextSize(12*density);c.drawText(message,10*density,20*density,paint);}
  paint.setColor(0xeef8f4e8);c.drawRect(0,getHeight()-25*density,getWidth(),getHeight(),paint);paint.setColor(0xff292a26);paint.setTextSize(11*density);c.drawText("© OpenStreetMap contributors",8*density,getHeight()-8*density,paint);
 }
 private void load(String key,int z,int x,int y){
  if(closed||pending.contains(key)||retry.containsKey(key)&&SystemClock.elapsedRealtime()<retry.get(key))return;pending.add(key);
  try{network.execute(()->{
   if(closed||!visible||!resumed||!online){pending.remove(key);return;}
   Bitmap bitmap=null;String failure=null;File file=new File(cache,key+".png");
   try{
    if(file.isFile()&&System.currentTimeMillis()-file.lastModified()<7L*86400000)bitmap=BitmapFactory.decodeFile(file.getPath());
    if(bitmap==null){
     HttpURLConnection http=(HttpURLConnection)new URL("https://tile.openstreetmap.org/"+z+"/"+x+"/"+y+".png").openConnection();
     http.setConnectTimeout(7000);http.setReadTimeout(7000);http.setRequestProperty("User-Agent","iDeaTape-Android/0.5.0 (contact: qfx1118@qq.com)");
     if(file.isFile())http.setIfModifiedSince(file.lastModified());
     try{
      int code=http.getResponseCode();
      if(code==304){file.setLastModified(System.currentTimeMillis());bitmap=BitmapFactory.decodeFile(file.getPath());}
      else if(code==200){ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream input=http.getInputStream()){byte[] chunk=new byte[8192];int count;while((count=input.read(chunk))!=-1){if(bytes.size()+count>1048576)throw new IOException("Tile too large");bytes.write(chunk,0,count);}}
       byte[] data=bytes.toByteArray();bitmap=BitmapFactory.decodeByteArray(data,0,data.length);if(bitmap!=null){File temp=new File(cache,key+".new");try(FileOutputStream out=new FileOutputStream(temp)){out.write(data);}if(!temp.renameTo(file))temp.delete();}
      }else throw new IOException("HTTP "+code);
     }finally{http.disconnect();}
    }
    if(bitmap==null)throw new IOException("Map tile unavailable");
   }catch(Exception error){failure="底图暂不可用；录音标记和播放仍可用";if(file.isFile())bitmap=BitmapFactory.decodeFile(file.getPath());}
   Bitmap result=bitmap;String error=failure;main.post(()->{pending.remove(key);if(closed)return;if(result!=null)images.put(key,result);if(error!=null){message=error;retry.put(key,SystemClock.elapsedRealtime()+30000);}invalidate();});
  });}catch(RejectedExecutionException error){pending.remove(key);}
 }
 public void close(){closed=true;network.shutdownNow();main.removeCallbacksAndMessages(null);images.evictAll();}
}

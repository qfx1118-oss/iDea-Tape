package cn.ideatape.recorder;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
/** Resolve each saved coordinate, never substitute the phone's current place. */
public final class RecordingPlaces {
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 private static final Map<String,Long> attempts=new HashMap<>();private static boolean busy;
 public static synchronized void resolve(Context context,Repository repo,ArrayList<JSONObject> items){
  final Context app=context.getApplicationContext();
  if(busy||!app.getSharedPreferences("settings",0).getBoolean("shareAddress",false)||!Geocoder.isPresent())return;
  ArrayList<JSONObject> batch=new ArrayList<>();long now=android.os.SystemClock.elapsedRealtime();
  for(JSONObject j:items){JSONObject location=j.optJSONObject("location");String key=j.optString("key");if(location==null||!location.optString("placeName").isEmpty()||now-attempts.getOrDefault(key,-120000L)<120000)continue;if(!LibraryRules.valid(location.optDouble("latitude",Double.NaN),location.optDouble("longitude",Double.NaN)))continue;batch.add(j);attempts.put(key,now);if(batch.size()==6)break;}
  if(batch.isEmpty())return;busy=true;
  worker.execute(()->{try{for(JSONObject j:batch){if(!app.getSharedPreferences("settings",0).getBoolean("shareAddress",false))break;try{JSONObject location=j.getJSONObject("location");double lat=location.getDouble("latitude"),lon=location.getDouble("longitude");List<Address> found=new Geocoder(app,Locale.SIMPLIFIED_CHINESE).getFromLocation(lat,lon,1);if(found!=null&&!found.isEmpty()){Address a=found.get(0);String name=RoadLabel.format(a.getThoroughfare(),a.getSubLocality(),a.getLocality());if(!name.isEmpty()&&app.getSharedPreferences("settings",0).getBoolean("shareAddress",false))repo.placeName(j.getString("key"),lat,lon,name);}}catch(Exception ignored){}}}finally{synchronized(RecordingPlaces.class){busy=false;}TapeService.libraryVersion++;}});
 }
}

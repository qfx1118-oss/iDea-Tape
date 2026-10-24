package cn.ideatape.tests;
import android.app.*;
import android.content.*;
import android.media.MediaPlayer;
import android.os.*;
import java.lang.reflect.*;
/** Separate signed emulator test APK; no test hooks are included in the production app. */
public final class PlayerInstrumentation extends Instrumentation {
 private Activity activity;private Throwable failure;
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private Object field(String n)throws Exception{Field f=activity.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(activity);}
 private void call(String n,Class<?> type,Object value)throws Exception{Method m=activity.getClass().getDeclaredMethod(n,type);m.setAccessible(true);m.invoke(activity,value);}
 private void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable e){failure=e;}});if(failure!=null)throw new RuntimeException(failure);}
 private MediaPlayer player(){try{return (MediaPlayer)field("player");}catch(Exception e){throw new RuntimeException(e);}}
 private void play(){main(()->{try{call("play",String.class,"test060-player");}catch(Exception e){throw new RuntimeException(e);}});}
 private int position(){int[] p={0};main(()->p[0]=player().getCurrentPosition());return p[0];}
 private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public void onStart(){Bundle report=new Bundle();try{
  Intent i=getTargetContext().getPackageManager().getLaunchIntentForPackage("cn.ideatape.recorder");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);activity=startActivitySync(i);Thread.sleep(900);play();Thread.sleep(1200);check(position()>400,"play advances");
  play();int paused=position();Thread.sleep(600);check(Math.abs(position()-paused)<60,"pause preserves position");
  main(()->player().seekTo(30000));Thread.sleep(350);check(Math.abs(position()-30000)<200,"seek works while paused");
  for(float speed:new float[]{1,1.25f,1.5f,1.75f,2}){main(()->{try{call("applySpeed",float.class,speed);}catch(Exception e){throw new RuntimeException(e);}});check(!player().isPlaying(),"changing speed preserves pause");play();check(Math.abs(player().getPlaybackParams().getSpeed()-speed)<.01,"speed applied expected="+speed+" actual="+player().getPlaybackParams().getSpeed());play();}
  play();int before=position();Thread.sleep(1000);int advance=position()-before;check(advance>1700&&advance<2400,"2x actual advance: "+advance);play();
  main(()->{try{call("seekBy",int.class,-10000);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(250);check(position()<25000,"back ten seconds");
  main(()->{try{call("seekBy",int.class,1000000);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(250);check(position()<=player().getDuration(),"seek clamps at end");
  main(()->{try{Object repo=field("repo");Object item=repo.getClass().getMethod("get",String.class).invoke(repo,"test060-player");call("showDetail",org.json.JSONObject.class,item);player().seekTo(10000);Field rate=activity.getClass().getDeclaredField("playbackSpeed");rate.setAccessible(true);rate.setFloat(activity,1.5f);call("applySpeed",float.class,1.5f);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(300);play();
  main(()->{try{android.view.View tape=(android.view.View)field("playerTape");long t=SystemClock.uptimeMillis();tape.dispatchTouchEvent(android.view.MotionEvent.obtain(t,t,0,100,100,0));}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(500);check(Math.abs(player().getPlaybackParams().getSpeed()-2)<.01,"hold uses 2x");
  main(()->{try{android.view.View tape=(android.view.View)field("playerTape");long t=SystemClock.uptimeMillis();tape.dispatchTouchEvent(android.view.MotionEvent.obtain(t-500,t,1,100,100,0));}catch(Exception e){throw new RuntimeException(e);}});check(Math.abs(player().getPlaybackParams().getSpeed()-1.5f)<.01,"release restores selected rate");
  for(int attempt=0;attempt<2;attempt++){
   main(()->{try{Object repo=field("repo");Object item=repo.getClass().getMethod("get",String.class).invoke(repo,"test060-player");call("deleteRecording",org.json.JSONObject.class,item);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(700);check(!field("pendingUndo").toString().isEmpty(),"undo visible");long deadline=(Long)field("undoDeadline");check(deadline-SystemClock.elapsedRealtime()>3000&&deadline-SystemClock.elapsedRealtime()<=5000,"five second window");Thread.sleep(1100);check(((android.widget.TextView)field("undoText")).getText().toString().contains("秒"),"visible countdown");
   if(attempt==0){main(()->{try{Method undo=activity.getClass().getDeclaredMethod("undoDelete");undo.setAccessible(true);undo.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(500);check(new java.io.File(getTargetContext().getFilesDir(),"recordings/test060-player.wav").isFile(),"undo restored audio");}
   else {Thread.sleep(3600);check(field("pendingUndo").toString().isEmpty(),"expires after five seconds");check(!new java.io.File(getTargetContext().getFilesDir(),"recordings/test060-player.wav").exists(),"final deletion removes audio");}
  }
  report.putString("stream","PASS: actual MediaPlayer play/pause, paused seeking, all five rates, 2x elapsed progress, skip and bounds, hold/release speed, five-second countdown, undo and final deletion. Advance="+advance);finish(Activity.RESULT_OK,report);
 }catch(Throwable e){report.putString("stream","FAIL: "+e);finish(Activity.RESULT_CANCELED,report);}}
}

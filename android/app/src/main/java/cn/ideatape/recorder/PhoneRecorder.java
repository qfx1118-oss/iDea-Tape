package cn.ideatape.recorder;
import android.media.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/* Capture on a dedicated thread. The service remains alive until the WAV is committed. */
public final class PhoneRecorder {
 public interface Result{void done(JSONObject saved,Exception error);}
 private final AtomicBoolean running=new AtomicBoolean();private Thread thread;
 public boolean active(){return running.get();}
 public void start(Repository repo,JSONObject location,Result result)throws IOException {
  if(!running.compareAndSet(false,true))return;
  long start=System.currentTimeMillis();String key="phone-"+UUID.randomUUID().toString();
  int size=Math.max(4096,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
  final AudioRecord audio;
  try{audio=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size*2);if(audio.getState()!=AudioRecord.STATE_INITIALIZED){audio.release();throw new IOException("麦克风初始化失败");}}
  catch(IOException e){running.set(false);throw e;}
  catch(RuntimeException e){running.set(false);throw new IOException(e);}
  thread=new Thread(()->{JSONObject saved=null;Exception error=null;File temp=new File(repo.directory,key+".wav.new");long bytes=0;
   try(RandomAccessFile out=new RandomAccessFile(temp,"rw")){
    out.write(Ima.header(0));audio.startRecording();byte[] buffer=new byte[2048];
    while(running.get()){
     int n=audio.read(buffer,0,buffer.length,AudioRecord.READ_BLOCKING);if(n<0)throw new IOException("麦克风读取失败: "+n);
     if((n&1)!=0)throw new IOException("Invalid PCM alignment");if(bytes+n>0x7ffff000L)throw new IOException("录音过长，请分段保存");out.write(buffer,0,n);bytes+=n;
    }
    audio.stop();if(bytes==0)throw new IOException("录音过短");out.seek(0);out.write(Ima.header(bytes/2));out.getFD().sync();
   }catch(Exception e){error=e;}
   finally{try{audio.stop();}catch(Exception ignored){}audio.release();}
   if(error==null)try{
    File target=repo.wav(key);if(!temp.renameTo(target))throw new IOException("录音保存失败");Repository.syncDir(repo.directory);
    saved=new JSONObject().put("key",key).put("title",LibraryRules.title("phone",start,start)).put("source","phone").put("startedAt",start).put("importedAt",start).put("samples",bytes/2).put("location",location==null?JSONObject.NULL:location).put("wavSha256",Repository.sha(target));repo.save(saved);
   }catch(Exception e){error=e;}
   running.set(false);result.done(saved,error);
  },"phone_recording");thread.start();
 }
 public void stop(){running.set(false);}
}

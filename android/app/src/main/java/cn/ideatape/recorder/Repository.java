package cn.ideatape.recorder;
import android.content.Context;
import android.system.*;
import org.json.*;
import java.io.*;
import java.security.*;
import java.util.*;
public final class Repository {
 public final File directory,cache;
 public Repository(Context c){directory=new File(c.getFilesDir(),"recordings");cache=new File(c.getFilesDir(),"incoming");directory.mkdirs();cache.mkdirs();}
 public static void atomic(File target,byte[] bytes)throws IOException{
  File temp=new File(target.getPath()+".new");try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}
  if(!temp.renameTo(target))throw new IOException("Cannot commit recording metadata");syncDir(target.getParentFile());
 }
 public static void syncDir(File dir)throws IOException{java.io.FileDescriptor fd=null;try{fd=Os.open(dir.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}catch(ErrnoException e){throw new IOException(e);}finally{if(fd!=null)try{Os.close(fd);}catch(ErrnoException ignored){}}}
 public static String sha(File f)throws IOException{try{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
 public File wav(String key){if(!key.matches("[A-Za-z0-9-]+"))throw new IllegalArgumentException();return new File(directory,key+".wav");}
 public JSONObject get(String key){if(!key.matches("[A-Za-z0-9-]+"))return null;try{return new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(directory,key+".json").toPath()),"UTF-8"));}catch(Exception e){return null;}}
 public boolean alreadySaved(String key,Wire.Meta m)throws IOException{
  JSONObject j=get(key);File f=wav(key);return j!=null&&f.isFile()&&j.optInt("blocks")==m.blocks&&j.optLong("encodedCrc32")==m.crc&&f.length()==44+(long)m.blocks*1010&&sha(f).equals(j.optString("wavSha256"));
 }
 public JSONObject tape(String key,String device,Wire.Meta m,JSONObject location,Transfer transfer)throws IOException,JSONException {
  if(!transfer.verify(m))throw new IOException("Audio checksum mismatch");
  File temp=new File(directory,key+".wav.new");
  try(FileInputStream in=new FileInputStream(transfer.encoded);FileOutputStream out=new FileOutputStream(temp)){
   out.write(Ima.header((long)m.blocks*505));byte[] block=new byte[256];for(int i=0;i<m.blocks;i++){int used=0;while(used<256){int n=in.read(block,used,256-used);if(n<0)throw new EOFException();used+=n;}out.write(Ima.decode(block));}out.getFD().sync();
  }
  File wav=wav(key);if(!temp.renameTo(wav))throw new IOException("Cannot save WAV");syncDir(directory);
  JSONObject previous=get(key);if(location==null&&previous!=null)location=previous.optJSONObject("location");
  JSONObject meta=new JSONObject().put("key",key).put("title",previous==null?LibraryRules.title("tape",m.epoch,System.currentTimeMillis()):previous.optString("title",LibraryRules.title("tape",m.epoch,System.currentTimeMillis()))).put("customTitle",previous!=null&&previous.optBoolean("customTitle")).put("favorite",previous!=null&&previous.optBoolean("favorite")).put("source","tape").put("device",device).put("recordingId",m.id).put("token",Long.toHexString(m.token)).put("startedAt",m.epoch).put("importedAt",System.currentTimeMillis()).put("samples",(long)m.blocks*505).put("blocks",m.blocks).put("encodedCrc32",m.crc).put("location",location==null?JSONObject.NULL:location).put("wavSha256",sha(wav));save(meta);return meta;
 }
 public void save(JSONObject meta)throws IOException,JSONException{atomic(new File(directory,meta.getString("key")+".json"),meta.toString(2).getBytes("UTF-8"));}
 public ArrayList<JSONObject> list(){ArrayList<JSONObject> result=new ArrayList<>();File[] files=directory.listFiles((d,n)->n.endsWith(".json"));if(files!=null)for(File f:files)try{JSONObject j=new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()),"UTF-8"));if(wav(j.getString("key")).isFile()){
 String old=j.optString("title");if(!j.optBoolean("customTitle")&&(old.equals("手机录音")||old.matches("磁带录音 [0-9]+"))){j.put("title",LibraryRules.title(j.optString("source"),j.optLong("startedAt"),j.optLong("importedAt")));save(j);}result.add(j);
 }}catch(Exception ignored){}result.sort((a,b)->Long.compare(b.optLong("startedAt")>0?b.optLong("startedAt"):b.optLong("importedAt"),a.optLong("startedAt")>0?a.optLong("startedAt"):a.optLong("importedAt")));return result;}
 public void rename(String key,String title)throws IOException,JSONException{JSONObject j=get(key);if(j==null)throw new IOException("Recording missing");j.put("customTitle",true);j.put("title",title.trim().isEmpty()?"未命名录音":title.trim());save(j);}
 public void favorite(String key,boolean value)throws IOException,JSONException{JSONObject j=get(key);if(j==null)throw new IOException("Recording missing");j.put("favorite",value);save(j);}
 public String exportName(String key,String extension){JSONObject j=get(key);String name=j==null?key:j.optString("title",key);name=name.replace('\\','_').replace('/','_').replaceAll("[:*?\"<>|]","_").replace('\n','_').replace('\r','_');if(name.length()>100)name=name.substring(0,100);return name+"-"+key.substring(Math.max(0,key.length()-6))+extension;}
}

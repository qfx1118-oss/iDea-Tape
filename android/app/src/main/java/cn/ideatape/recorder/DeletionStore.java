package cn.ideatape.recorder;

import java.io.*;
import java.util.*;

/** Reversible two-file deletion with a durable, audio/location-free receipt. */
public final class DeletionStore {
 public static final Object LOCK=new Object();
 public interface Sync { void directory(File directory)throws IOException; }
 private final File recordings,trash,receipts;
 private final Sync sync;
 public DeletionStore(File recordings,File trash,File receipts,Sync sync){this.recordings=recordings;this.trash=trash;this.receipts=receipts;this.sync=sync;}
 private static void valid(String key){if(key==null||!key.matches("[A-Za-z0-9-]+"))throw new IllegalArgumentException("Invalid recording key");}
 private File audio(String key){valid(key);return new File(recordings,key+".wav");}
 private File meta(String key){valid(key);return new File(recordings,key+".json");}
 private File receipt(String key){valid(key);return new File(receipts,key+".properties");}
 private File trashed(String key,String ext){valid(key);return new File(trash,key+ext);}
 public boolean deleted(String key){synchronized(LOCK){return receipt(key).isFile();}}
 private Properties read(String key)throws IOException{Properties p=new Properties();try(InputStream in=new FileInputStream(receipt(key))){p.load(in);}return p;}
 public boolean matches(String key,int blocks,long crc,long token){synchronized(LOCK){try{Properties p=read(key);return blocks>0&&key.equals(p.getProperty("key"))&&Integer.toString(blocks).equals(p.getProperty("blocks"))&&Long.toString(crc).equals(p.getProperty("crc"))&&Long.toHexString(token).equals(p.getProperty("token"));}catch(IOException e){return false;}}}
 public void remove(String key,int blocks,long crc,long token,long now)throws IOException{synchronized(LOCK){
  if(deleted(key))throw new IOException("Recording already deleted");
  File wav=audio(key),json=meta(key);if(!wav.isFile()||!json.isFile())throw new IOException("Recording is missing");
  if(!trash.isDirectory()&&!trash.mkdirs()||!receipts.isDirectory()&&!receipts.mkdirs())throw new IOException("Cannot prepare deletion");
  Properties p=new Properties();p.setProperty("key",key);p.setProperty("blocks",Integer.toString(blocks));p.setProperty("crc",Long.toString(crc));p.setProperty("token",Long.toHexString(token));p.setProperty("deletedAt",Long.toString(now));
  File temp=new File(receipts,key+".new");try(FileOutputStream out=new FileOutputStream(temp)){p.store(out,"Intentional deletion of a previously saved recording");out.getFD().sync();}
  if(!temp.renameTo(receipt(key)))throw new IOException("Cannot commit deletion receipt");
  try{sync.directory(receipts);}catch(IOException e){receipt(key).delete();throw e;}
  boolean movedWav=false,movedJson=false;
  try{
   movedWav=wav.renameTo(trashed(key,".wav"));if(!movedWav)throw new IOException("Cannot move audio");
   movedJson=json.renameTo(trashed(key,".json"));if(!movedJson)throw new IOException("Cannot move metadata");
   sync.directory(recordings);sync.directory(trash);
  }catch(IOException e){
   boolean restored=(!movedWav||trashed(key,".wav").renameTo(wav))&(!movedJson||trashed(key,".json").renameTo(json));
   if(restored){receipt(key).delete();sync.directory(receipts);}throw e;
  }
 }}
 public void undo(String key)throws IOException{synchronized(LOCK){
  if(!deleted(key)||!trashed(key,".wav").isFile()||!trashed(key,".json").isFile())throw new IOException("Undo is no longer available");
  if(audio(key).exists()||meta(key).exists())throw new IOException("Recording identity is occupied");
  if(!trashed(key,".wav").renameTo(audio(key)))throw new IOException("Cannot restore audio");
  if(!trashed(key,".json").renameTo(meta(key))){audio(key).renameTo(trashed(key,".wav"));throw new IOException("Cannot restore metadata");}
  try{sync.directory(recordings);sync.directory(trash);
   if(!receipt(key).delete())throw new IOException("Cannot restore recording visibility");
  }catch(IOException e){audio(key).renameTo(trashed(key,".wav"));meta(key).renameTo(trashed(key,".json"));throw e;}
  /* Once the receipt is gone, restored files remain visible even if final fsync fails. */
  sync.directory(receipts);
 }}
 public void finish(String key)throws IOException{synchronized(LOCK){
  if(!deleted(key))return;
  for(File f:new File[]{trashed(key,".wav"),trashed(key,".json"),audio(key),meta(key)})if(f.exists()&&!f.delete())throw new IOException("Cannot finish deletion");
  sync.directory(recordings);if(trash.isDirectory())sync.directory(trash);
 }}
 public void purgeBefore(long cutoff)throws IOException{synchronized(LOCK){File[] files=receipts.listFiles((d,n)->n.endsWith(".properties"));if(files==null)return;for(File file:files){String key=file.getName().substring(0,file.getName().length()-11);try{if(Long.parseLong(read(key).getProperty("deletedAt","0"))<cutoff)finish(key);}catch(NumberFormatException ignored){}}}}
}

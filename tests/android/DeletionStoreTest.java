import cn.ideatape.recorder.DeletionStore;
import java.io.*;
import java.nio.file.*;
public final class DeletionStoreTest {
 private static void check(boolean value){if(!value)throw new AssertionError();}
 private static void save(File folder,String key)throws Exception{Files.write(new File(folder,key+".wav").toPath(),new byte[]{1,2,3});Files.write(new File(folder,key+".json").toPath(),"private title and GPS".getBytes("UTF-8"));}
 public static void main(String[] args)throws Exception{
  File base=Files.createTempDirectory("tape-delete").toFile(),recordings=new File(base,"recordings");recordings.mkdirs();File trash=new File(base,"trash"),receipts=new File(base,"deleted");DeletionStore d=new DeletionStore(recordings,trash,receipts,dir->{});
  save(recordings,"one");save(recordings,"other");d.remove("one",4,33,-4,1000);check(d.deleted("one"));check(!new File(recordings,"one.wav").exists());check(new File(recordings,"other.wav").isFile());
  check(d.matches("one",4,33,-4));check(!d.matches("one",3,33,-4));check(!d.matches("one",4,34,-4));check(!d.matches("one",4,33,9));check(!d.matches("one",0,33,-4));
  String proof=new String(Files.readAllBytes(new File(receipts,"one.properties").toPath()),"UTF-8");check(!proof.contains("GPS")&&!proof.contains("private title"));
  d.undo("one");check(!d.deleted("one"));check(new File(recordings,"one.wav").isFile());check(new File(recordings,"one.json").isFile());
  d.remove("one",4,33,-4,2000);d.purgeBefore(1500);check(new File(trash,"one.wav").isFile());d.purgeBefore(3000);check(!new File(trash,"one.wav").exists());check(!new File(trash,"one.json").exists());check(d.matches("one",4,33,-4));
  boolean failed=false;try{d.undo("one");}catch(IOException e){failed=true;}check(failed);
  save(recordings,"partial");new File(recordings,"partial.json").delete();failed=false;try{d.remove("partial",1,1,1,4000);}catch(IOException e){failed=true;}check(failed&&!d.deleted("partial")&&new File(recordings,"partial.wav").isFile());
  failed=false;try{d.deleted("../other");}catch(IllegalArgumentException e){failed=true;}check(failed);
  save(recordings,"rollback");DeletionStore broken=new DeletionStore(recordings,trash,receipts,dir->{if(dir.equals(recordings))throw new IOException("injected sync failure");});failed=false;try{broken.remove("rollback",1,1,1,5000);}catch(IOException e){failed=true;}check(failed&&!broken.deleted("rollback")&&new File(recordings,"rollback.wav").isFile()&&new File(recordings,"rollback.json").isFile());
  save(recordings,"receipt-failure");DeletionStore badReceipt=new DeletionStore(recordings,trash,receipts,dir->{if(dir.equals(receipts))throw new IOException("injected receipt sync failure");});failed=false;try{badReceipt.remove("receipt-failure",1,1,1,6000);}catch(IOException e){failed=true;}check(failed&&!d.deleted("receipt-failure")&&new File(recordings,"receipt-failure.wav").isFile());
  save(recordings,"undo-failure");d.remove("undo-failure",1,1,1,7000);failed=false;try{broken.undo("undo-failure");}catch(IOException e){failed=true;}check(failed&&d.deleted("undo-failure")&&new File(trash,"undo-failure.wav").isFile());d.undo("undo-failure");check(!d.deleted("undo-failure"));
  System.out.println("Phone deletion: undo, purge, replay receipt, rollback, identity/path isolation PASS");
 }
}

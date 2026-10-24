package cn.ideatape.recorder;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;
/* Durable encoded cache. A bit is persisted only after the corresponding audio is synced. */
public final class Transfer implements Closeable {
 private final File state;public final File encoded;private final RandomAccessFile file;private final BitSet blocks=new BitSet();private int unsynced;
 public Transfer(File directory,String key)throws IOException{
  if(!key.matches("[A-Za-z0-9-]+"))throw new IOException("Invalid recording key");
  directory.mkdirs();state=new File(directory,key+".bits");encoded=new File(directory,key+".adpcm.part");file=new RandomAccessFile(encoded,"rw");
  if(state.isFile()){try(DataInputStream in=new DataInputStream(new FileInputStream(state))){if(in.readInt()!=0x49544231)throw new IOException();int n=in.readInt();if(n<0||n>4096)throw new IOException();byte[] b=new byte[n];in.readFully(b);blocks.or(BitSet.valueOf(b));for(int i=blocks.nextSetBit(0);i>=0;i=blocks.nextSetBit(i+1))if((long)(i+1)*256>file.length())blocks.clear(i);}catch(IOException ex){blocks.clear();}}
 }
 public void receive(int index,byte[] b)throws IOException {if(index<0||index>=32768||b.length!=256)throw new IOException("Invalid block bounds");if(blocks.get(index))return;Ima.decode(b);file.seek((long)index*256);file.write(b);blocks.set(index);if(++unsynced>=64)checkpoint();}
 public int missing(int count){int n=blocks.nextClearBit(0);return n<count?n:-1;}
 public boolean hasRange(int start,int count){return blocks.nextClearBit(start)>=start+count;}
 public int received(){return blocks.cardinality();}
 public void checkpoint()throws IOException{
  file.getFD().sync();byte[] bitmap=blocks.toByteArray();File temp=new File(state.getPath()+".tmp");
  try(FileOutputStream out=new FileOutputStream(temp);DataOutputStream data=new DataOutputStream(out)){data.writeInt(0x49544231);data.writeInt(bitmap.length);data.write(bitmap);data.flush();out.getFD().sync();}
  if(!temp.renameTo(state))throw new IOException("Cannot save recovery state");unsynced=0;
 }
 public boolean verify(Wire.Meta m)throws IOException {if(!m.complete()||m.blocks==0||missing(m.blocks)>=0)return false;checkpoint();file.seek(0);CRC32 crc=new CRC32();byte[] b=new byte[256];for(int i=0;i<m.blocks;i++){file.readFully(b);crc.update(b);}return crc.getValue()==m.crc;}
 public void reset()throws IOException{blocks.clear();file.setLength(0);checkpoint();}
 public void close()throws IOException{try{checkpoint();}finally{file.close();}}
 public void remove(){state.delete();encoded.delete();}
}

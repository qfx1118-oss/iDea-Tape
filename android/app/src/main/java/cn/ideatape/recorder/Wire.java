package cn.ideatape.recorder;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.CRC32;
public final class Wire {
 public static final int META=1,DATA=2,END=3,EMPTY=4,DELETED=5,ERROR=6,LIST=16,PULL=17,COMMIT=18,TIME=19,PROGRESS=20;
 public static final String SERVICE="6e400001-b5a3-f393-e0a9-e50e24dcca9e",RX="6e400002-b5a3-f393-e0a9-e50e24dcca9e",TX="6e400003-b5a3-f393-e0a9-e50e24dcca9e";
 public static int i32(byte[] p,int n){return ByteBuffer.wrap(p,n,4).order(ByteOrder.LITTLE_ENDIAN).getInt();}
 public static long u32(byte[] p,int n){return i32(p,n)&0xffffffffL;}
 public static long i64(byte[] p,int n){return ByteBuffer.wrap(p,n,8).order(ByteOrder.LITTLE_ENDIAN).getLong();}
 public static void put32(byte[] p,int n,long v){ByteBuffer.wrap(p,n,4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)v);}
 public static void put64(byte[] p,int n,long v){ByteBuffer.wrap(p,n,8).order(ByteOrder.LITTLE_ENDIAN).putLong(v);}
 public static byte[] frame(int type,byte[] p){if(p.length>272)throw new IllegalArgumentException();byte[] out=new byte[p.length+10];out[0]='I';out[1]='T';out[2]=1;out[3]=(byte)type;out[4]=(byte)p.length;out[5]=(byte)(p.length>>8);System.arraycopy(p,0,out,6,p.length);CRC32 crc=new CRC32();crc.update(out,0,p.length+6);put32(out,p.length+6,crc.getValue());return out;}
 public interface Listener {void receive(int type,byte[] data);}
 public static final class Parser {
  private byte[] buffer=new byte[564];private int used;
  public void reset(){used=0;}
  public void feed(byte[] bytes,Listener callback){for(byte b:bytes){if(used==buffer.length)shift(1);buffer[used++]=b;while(used>=6){int len=(buffer[4]&255)|((buffer[5]&255)<<8);if(buffer[0]!='I'||buffer[1]!='T'||buffer[2]!=1||len>272){shift(1);continue;}if(used<len+10)break;CRC32 crc=new CRC32();crc.update(buffer,0,len+6);if(u32(buffer,len+6)!=crc.getValue()){shift(1);continue;}int type=buffer[3]&255;byte[] payload=Arrays.copyOfRange(buffer,6,6+len);shift(len+10);callback.receive(type,payload);}}}
  private void shift(int n){System.arraycopy(buffer,n,buffer,0,used-n);used-=n;}
 }
 public static final class Meta {
  public final int id,blocks,flags;public final long token,epoch,crc;
  public Meta(byte[] p){if(p.length!=32)throw new IllegalArgumentException("Invalid recording metadata");id=i32(p,0);token=i64(p,4);epoch=i64(p,12);blocks=i32(p,20);crc=u32(p,24);flags=p[28]&255;if(id<1||token==0||blocks<0||blocks>=32768||(flags&~7)!=0)throw new IllegalArgumentException("Invalid metadata bounds");}
  public boolean live(){return (flags&1)!=0;}public boolean complete(){return (flags&2)!=0;}
  public byte[] commit(){byte[] p=new byte[20];put32(p,0,id);put64(p,4,token);put32(p,12,blocks);put32(p,16,crc);return p;}
  public byte[] pull(int first,int count){byte[] p=new byte[20];put32(p,0,id);put64(p,4,token);put32(p,12,first);put32(p,16,count);return p;}
  public String key(String device){return device.replace(":","")+"-"+id+"-"+Long.toHexString(token);}
 }
}

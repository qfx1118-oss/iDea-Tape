import cn.ideatape.recorder.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class ProtocolTest {
 private static void check(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  byte[] data=Files.readAllBytes(Paths.get(args[0])),raw=Files.readAllBytes(Paths.get(args[1]));Wire.Meta m=new Wire.Meta(raw);check(m.id==42&&m.blocks==4&&m.token==0xf123456789abcdefL&&m.epoch==1800000000000L);
  byte[] frame=Wire.frame(Wire.META,raw);
  for(int split=0;split<=frame.length;split++){Wire.Parser parser=new Wire.Parser();int[] count={0};Wire.Listener listener=(t,p)->{check(t==Wire.META&&Arrays.equals(p,raw));count[0]++;};parser.feed(Arrays.copyOfRange(frame,0,split),listener);parser.feed(Arrays.copyOfRange(frame,split,frame.length),listener);check(count[0]==1);}
  Wire.Parser parser=new Wire.Parser();int[] received={0};byte[] damaged=frame.clone();damaged[15]^=1;parser.feed(new byte[]{1,2,'I','T',1,0,(byte)255,(byte)255},(t,b)->received[0]++);parser.feed(damaged,(t,b)->received[0]++);parser.feed(frame,(t,b)->received[0]++);check(received[0]==1);
  File dir=Files.createTempDirectory("idea-tape-transfer").toFile();String key=m.key("AA:BB:CC:DD:EE:FF");Transfer transfer=new Transfer(dir,key);
  transfer.receive(3,Arrays.copyOfRange(data,768,1024));transfer.receive(0,Arrays.copyOfRange(data,0,256));check(transfer.missing(4)==1);transfer.close();
  transfer=new Transfer(dir,key);check(transfer.received()==2);transfer.receive(2,Arrays.copyOfRange(data,512,768));transfer.receive(1,Arrays.copyOfRange(data,256,512));check(transfer.verify(m));
  byte[] wrong=raw.clone();wrong[24]^=1;check(!transfer.verify(new Wire.Meta(wrong)));byte[] pcm=Ima.decode(Arrays.copyOfRange(data,0,256));check(pcm.length==1010);byte[] wav=Ima.header(2020);check(Wire.u32(wav,40)==4040&&wav[20]==1&&wav[22]==1);
  transfer.close();try(RandomAccessFile file=new RandomAccessFile(new File(dir,key+".adpcm.part"),"rw")){file.setLength(256);}transfer=new Transfer(dir,key);check(!transfer.verify(m));transfer.close();transfer.remove();
  byte[] known=new byte[256];Arrays.fill(known,(byte)0x77);Arrays.fill(known,0,4,(byte)0);pcm=Ima.decode(known);check(pcm[2]==11&&pcm[4]==41);
  ReelMotion motion=new ReelMotion();check(motion.step(false,100)==0&&motion.step(false,1000)==0);motion.step(true,1020);for(int t=1040;t<=2020;t+=20)motion.step(true,t);double stop=motion.step(false,2040);check(Math.abs(motion.step(false,2340)-stop-16)<.001);check(Math.abs(motion.step(false,2520)-stop-12)<.001);
  System.out.println("Android Java: C-compatible metadata, framing, IMA decode, durable resume, corruption detection, WAV and reel inertia PASS");
 }
}

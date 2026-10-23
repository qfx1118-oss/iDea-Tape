package cn.ideatape.recorder;
import java.io.*;
public final class Ima {
 private static final int[] STEPS={7,8,9,10,11,12,13,14,16,17,19,21,23,25,28,31,34,37,41,45,50,55,60,66,73,80,88,97,107,118,130,143,157,173,190,209,230,253,279,307,337,371,408,449,494,544,598,658,724,796,876,963,1060,1166,1282,1411,1552,1707,1878,2066,2272,2499,2749,3024,3327,3660,4026,4428,4871,5358,5894,6484,7132,7845,8630,9493,10442,11487,12635,13899,15289,16818,18500,20350,22385,24623,27086,29794,32767};
 private static final int[] CHANGE={-1,-1,-1,-1,2,4,6,8,-1,-1,-1,-1,2,4,6,8};
 public static byte[] decode(byte[] b)throws IOException {
  if(b.length!=256||(b[2]&255)>88||b[3]!=0)throw new IOException("Invalid audio block");
  byte[] out=new byte[1010];int predictor=(short)((b[0]&255)|((b[1]&255)<<8)),index=b[2]&255;out[0]=b[0];out[1]=b[1];
  for(int i=1;i<505;i++){int code=(b[4+(i-1)/2]>>(((i-1)%2)*4))&15,step=STEPS[index],diff=step/8;if((code&1)!=0)diff+=step/4;if((code&2)!=0)diff+=step/2;if((code&4)!=0)diff+=step;predictor+=((code&8)!=0?-diff:diff);predictor=Math.max(-32768,Math.min(32767,predictor));index=Math.max(0,Math.min(88,index+CHANGE[code]));out[i*2]=(byte)predictor;out[i*2+1]=(byte)(predictor>>8);}
  return out;
 }
 public static byte[] header(long samples){if(samples<0||samples>0x7ffff000L/2)throw new IllegalArgumentException();byte[] p=new byte[44];byte[] riff={'R','I','F','F'},fmt={'W','A','V','E','f','m','t',' '},data={'d','a','t','a'};System.arraycopy(riff,0,p,0,4);Wire.put32(p,4,36+samples*2);System.arraycopy(fmt,0,p,8,8);Wire.put32(p,16,16);p[20]=1;p[22]=1;Wire.put32(p,24,16000);Wire.put32(p,28,32000);p[32]=2;p[34]=16;System.arraycopy(data,0,p,36,4);Wire.put32(p,40,samples*2);return p;}
}

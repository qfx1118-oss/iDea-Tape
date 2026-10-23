package cn.ideatape.recorder;
import java.nio.charset.StandardCharsets;
/** Keeps road first on the small device display; never adds inferred address parts. */
public final class RoadLabel {
 public static String format(String road,String district,String city){String r=clean(road),d=clean(district),c=clean(city);return limit(!r.isEmpty()?r:!d.isEmpty()?d:c,96);}
 private static String clean(String s){return s==null?"":s.replaceAll("[\\p{Cntrl}]", "").trim();}
 public static String limit(String s,int max){StringBuilder out=new StringBuilder();int bytes=0;for(int i=0;i<s.length();){int cp=s.codePointAt(i);String unit=new String(Character.toChars(cp));int n=unit.getBytes(StandardCharsets.UTF_8).length;if(bytes+n>max)break;out.append(unit);bytes+=n;i+=Character.charCount(cp);}return out.toString();}
 public static byte[] packet(int state,String text){byte[] b=(state==2?limit(text,96):"").getBytes(StandardCharsets.UTF_8);byte[] out=new byte[b.length+2];out[0]=(byte)state;out[1]=(byte)b.length;System.arraycopy(b,0,out,2,b.length);return out;}
}

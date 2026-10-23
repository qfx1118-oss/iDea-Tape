package cn.ideatape.recorder;
import java.text.SimpleDateFormat;
import java.util.*;
/** Pure rules shared by list, map and export; coordinates remain WGS84. */
public final class LibraryRules {
 public static String title(String source,long started,long imported){return ("phone".equals(source)?"手机录音 ":"灵感磁带 ")+(started>0?"":"导入于 ")+new SimpleDateFormat("yyyy-MM-dd HH-mm-ss",Locale.ROOT).format(new Date(started>0?started:imported));}
 public static boolean matches(String title,String source,long started,long imported,boolean favorite,String query,boolean favorites,long from,long until){
  long date=started>0?started:imported;String hay=title+" "+("phone".equals(source)?"手机":"灵感磁带")+" "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).format(new Date(date));
  return (!favorites||favorite)&&date>=from&&date<until&&hay.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
 }
 public static double x(double longitude){return (longitude+180)/360;}
 public static double y(double latitude){double r=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,latitude)));return (1-Math.log(Math.tan(r)+1/Math.cos(r))/Math.PI)/2;}
 public static double latitude(double y){return Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*y))));}
 public static double longitude(double x){return x*360-180;}
 public static double wrap(double x){return x-Math.floor(x);}
 public static double delta(double a,double b){double d=a-b;return d-Math.floor(d+.5);}
 public static boolean valid(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
 public static boolean visible(double lat,double lon,double cx,double cy,double width,double height,double world){return valid(lat,lon)&&Math.abs(delta(x(lon),cx)*world)<=width/2&&Math.abs((y(lat)-cy)*world)<=height/2;}
}

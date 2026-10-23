import cn.ideatape.recorder.LibraryRules;
import java.util.*;
public final class LibraryRulesTest {
 private static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));long stamp=1792800000000L;
  String title=LibraryRules.title("phone",stamp,stamp);check(title.startsWith("手机录音 2026-"));check(!title.contains("导入于"));check(LibraryRules.title("tape",0,stamp).contains("导入于"));
  check(LibraryRules.matches(title,"phone",stamp,stamp,true,"手机",true,stamp,stamp+1));check(!LibraryRules.matches(title,"phone",stamp,stamp,false,"",true,0,Long.MAX_VALUE));check(!LibraryRules.matches(title,"phone",stamp,stamp,true,"",false,0,stamp));check(LibraryRules.matches(title,"phone",0,stamp,true,"2026",false,stamp,stamp+1));
  for(double lat:new double[]{-85,-30,0,30,85})check(Math.abs(LibraryRules.latitude(LibraryRules.y(lat))-lat)<1e-7);
  check(LibraryRules.visible(0,-179.9,LibraryRules.x(179.9),.5,100,100,1000));check(!LibraryRules.visible(50,50,.5,.5,100,100,1000));check(!LibraryRules.valid(Double.NaN,0));check(!LibraryRules.valid(0,181));check(LibraryRules.valid(90,180));
  System.out.println("Date names, search/favorites/time boundaries, map projection/dateline: PASS");
 }
}

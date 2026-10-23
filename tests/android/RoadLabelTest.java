import cn.ideatape.recorder.RoadLabel;
public class RoadLabelTest {
 public static void main(String[] args){
  if(!RoadLabel.format("人民路","锦江区","成都").equals("人民路"))throw new AssertionError();
  if(!RoadLabel.format(null,"锦江区","成都").equals("锦江区"))throw new AssertionError();
  if(!RoadLabel.format(null,null,null).isEmpty())throw new AssertionError();
  if(!RoadLabel.limit("人民路",7).equals("人民"))throw new AssertionError();
  byte[] p=RoadLabel.packet(2,"人民路");if(p.length!=11||p[1]!=9)throw new AssertionError();
  if(RoadLabel.packet(0,"人民路").length!=2)throw new AssertionError();
  System.out.println("Road labels, fallbacks, UTF-8 boundaries PASS");
 }
}

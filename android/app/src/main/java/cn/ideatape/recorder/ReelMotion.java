package cn.ideatape.recorder;
public final class ReelMotion {
 private boolean moving,initialized,coasting;private long tick,changed;private double angle,stop;
 public double step(boolean next,long now){if(!initialized){initialized=true;tick=changed=now;moving=next;}if(next!=moving){stop=angle;moving=next;coasting=!next;changed=now;}double age=now-changed;if(moving)angle+=210*Math.min(1,age/180)*(now-tick)/1000.0;else if(coasting){double offset;if(age<300)offset=16*(1-Math.pow(1-age/300,3));else if(age<480){double t=(age-300)/180;offset=16-4*t*t*(3-2*t);}else if(age<640){double t=(age-480)/160;offset=12+0.7*Math.sin(t*Math.PI*2)*(1-t);}else offset=12;angle=stop+offset;}tick=now;return angle;}
 public boolean settling(long now){return coasting&&now-changed<660;}
}

package cn.ideatape.recorder;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
public final class CassetteView extends View {
 private final Paint p=new Paint(3);private final ReelMotion motion=new ReelMotion();private boolean moving,recording;
 public CassetteView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription("复古磁带，录音时红点亮起，停止时转轴惯性回弹");}
 public void state(boolean move,boolean record){if(move!=moving||record!=recording){moving=move;recording=record;invalidate();}}
 private void rect(Canvas c,float x,float y,float w,float h,float r,int color){p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
 protected void onDraw(Canvas canvas){super.onDraw(canvas);long now=SystemClock.elapsedRealtime();float angle=(float)motion.step(moving,now);canvas.save();canvas.scale(getWidth()/240f,getHeight()/148f);
  rect(canvas,4,7,232,132,9,0xff292a26);rect(canvas,14,18,212,29,3,0xffc26738);
  p.setColor(0xffe5dfcd);p.setTypeface(Typeface.create("monospace",0));p.setTextSize(11);canvas.drawText("iDea / NORMAL TYPE I",24,37,p);
  if(recording){p.setColor(0xffed342c);canvas.drawCircle(213,32,4,p);}
  rect(canvas,20,57,200,49,25,0xff191c18);p.setColor(0xff978565);p.setStrokeWidth(2);canvas.drawLine(55,88,185,88,p);
  rect(canvas,88,70,64,20,2,0xff555746);p.setColor(0xffbfbfa8);p.setTextSize(11);canvas.drawText("A / 90",99,84,p);
  reel(canvas,55,82,angle);reel(canvas,185,82,angle*1.07f+17);
  rect(canvas,75,118,90,14,3,0xff45463c);p.setColor(0xff171b16);canvas.drawCircle(88,125,3,p);canvas.drawCircle(152,125,3,p);
  p.setColor(0xffaaa994);p.setTextSize(10);canvas.drawText("A",117,129,p);
  for(float[] q:new float[][]{{13,16},{227,16},{13,130},{227,130}}){p.setColor(0xff77776b);canvas.drawCircle(q[0],q[1],2,p);}
  canvas.restore();if(moving||motion.settling(now))postInvalidateOnAnimation();
 }
 private void reel(Canvas c,float x,float y,float a){p.setColor(0xff62533c);c.drawCircle(x,y,23,p);p.setColor(0xffd9d3bd);c.drawCircle(x,y,16,p);for(int i=0;i<6;i++){double r=(a+i*60)*Math.PI/180;p.setColor(0xff4c4c40);c.drawCircle(x+(float)Math.cos(r)*11,y+(float)Math.sin(r)*11,2.5f,p);}p.setColor(0xff262822);c.drawCircle(x,y,4,p);}
}

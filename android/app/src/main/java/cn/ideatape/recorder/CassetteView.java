package cn.ideatape.recorder;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** Authored cassette geometry: fixed aspect, static shell, animated mechanical hubs. */
public final class CassetteView extends View {
 private final Paint p=new Paint(3);private final ReelMotion motion=new ReelMotion();private final Typeface brand;private boolean moving,recording;
 public CassetteView(Context c){super(c);brand=c.getResources().getFont(R.font.idea_display);setContentDescription("灵感磁带，转轴停止时惯性回弹");}
 public void state(boolean move,boolean record){if(move!=moving||record!=recording){moving=move;recording=record;invalidate();}}
 private void rect(Canvas c,float x,float y,float w,float h,float r,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
 protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(resolveSize(width,w),resolveSize(Math.round(width/1.586f),h));}
 protected void onDraw(Canvas c){super.onDraw(c);long now=SystemClock.elapsedRealtime();float a=(float)motion.step(moving,now);c.save();float scale=Math.min(getWidth()/240f,getHeight()/151.3f);c.translate((getWidth()-240*scale)/2,(getHeight()-151.3f*scale)/2);c.scale(scale,scale);
  rect(c,1,1,238,149,8,0xff181c1b);rect(c,4,4,232,143,6,0xff777367);rect(c,5,5,230,141,5,0xff252a28);rect(c,12,12,216,98,7,0xffa99e82);rect(c,13,13,214,96,6,0xff1e2422);

  p.setTypeface(brand);p.setTextSize(100);p.setColor(0xffeee0c1);String word="ıDea Tape";Rect lettering=new Rect();p.getTextBounds(word,0,word.length(),lettering);float size=100*190/lettering.width();p.setTextSize(size);p.getTextBounds(word,0,word.length(),lettering);float tx=(240-lettering.left-lettering.right)/2f,baseline=47;
  c.save();c.rotate(-3,120,43);c.drawText(word,tx,baseline,p);p.setColor(recording?0xffe54d2d:0xffeee0c1);c.drawCircle(tx+size*.26425f,baseline-size*.62075f,size*.10575f,p);p.setColor(0xffeee0c1);p.setStrokeWidth(2.2f);c.drawLine(24,57,216,57,p);c.restore();
  rect(c,47,67,146,31,15,0xff080d0c);rect(c,96,72,48,21,2,0xff584936);p.setColor(0xffb3a17e);p.setStrokeWidth(.7f);for(int x=98;x<144;x+=5)c.drawLine(x,73,x,92,p);
  reel(c,67,83,a);reel(c,173,83,a*1.07f+17);
  rect(c,41,111,158,32,3,0xff171d1c);p.setColor(0xff454b46);p.setStrokeWidth(.6f);for(int y=116;y<142;y+=2)c.drawLine(44,y,196,y,p);
  for(int x:new int[]{64,84,156,176}){p.setColor(0xff090e0b);c.drawCircle(x,133,x==64||x==176?5:4,p);}
  for(float[] q:new float[][]{{9,9},{231,9},{9,142},{231,142},{120,119}}){p.setColor(0xff0d1210);c.drawCircle(q[0],q[1],3.6f,p);p.setColor(0xff9c927b);p.setStrokeWidth(.7f);c.drawLine(q[0]-1.8f,q[1],q[0]+1.8f,q[1],p);c.drawLine(q[0],q[1]-1.8f,q[0],q[1]+1.8f,p);}
  c.restore();if(isShown()&&hasWindowFocus()&&(moving||motion.settling(now)))postInvalidateDelayed(33);
 }
 private void reel(Canvas c,float x,float y,float a){p.setColor(0xff74634c);c.drawCircle(x,y,15,p);p.setColor(0xffcabb9a);c.drawCircle(x,y,11.8f,p);p.setColor(0xff8e7c60);c.drawCircle(x,y,8.6f,p);c.save();c.rotate(a,x,y);for(int i=0;i<6;i++){c.rotate(60,x,y);rect(c,x-1.5f,y-12,3,4,1,0xffe7d8b9);}c.restore();}
}

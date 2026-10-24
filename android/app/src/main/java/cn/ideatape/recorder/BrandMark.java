package cn.ideatape.recorder;
import android.content.Context;
import android.graphics.*;
import android.view.View;
public final class BrandMark extends View {
 private final Paint p=new Paint(3);private final Typeface font;private boolean recording;
 public BrandMark(Context c){super(c);font=c.getResources().getFont(R.font.idea_display);setContentDescription("iDea Tape 灵感磁带");}
 public void recording(boolean value){if(recording!=value){recording=value;invalidate();}}
 protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density;p.setColor(0xff202624);c.drawRoundRect(0,0,Math.min(getWidth(),190*d),getHeight(),8*d,8*d,p);p.setTypeface(font);p.setTextSize(28*d);p.setColor(0xffeee0c1);float x=12*d,y=(getHeight()+20*d)/2;c.drawText("ıDea Tape",x,y,p);p.setColor(recording?0xffe54d2d:0xffeee0c1);c.drawCircle(x+7.4f*d,y-17.4f*d,3*d,p);}
}

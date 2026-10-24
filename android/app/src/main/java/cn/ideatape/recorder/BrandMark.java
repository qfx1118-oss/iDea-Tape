package cn.ideatape.recorder;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Center visible lettering, including italic overhangs and the recording dot. */
public final class BrandMark extends View {
 private final Paint p=new Paint(3);private final Typeface font;private final Rect bounds=new Rect();private boolean recording;
 public BrandMark(Context c){super(c);font=c.getResources().getFont(R.font.idea_display);setContentDescription("iDea Tape 灵感磁带");}
 public void recording(boolean value){if(recording!=value){recording=value;invalidate();}}
 protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=Math.min(getWidth(),190*d),h=getHeight();p.setColor(0xff202624);c.drawRoundRect(0,0,w,h,8*d,8*d,p);p.setTypeface(font);p.setTextSize(100);String word="ıDea Tape";p.getTextBounds(word,0,word.length(),bounds);float size=100*Math.min((w-24*d)/bounds.width(),(h-12*d)/bounds.height());p.setTextSize(size);p.getTextBounds(word,0,word.length(),bounds);float x=(w-bounds.left-bounds.right)/2f,y=(h-bounds.top-bounds.bottom)/2f;p.setColor(0xffeee0c1);c.drawText(word,x,y,p);p.setColor(recording?0xffe54d2d:0xffeee0c1);c.drawCircle(x+size*.26425f,y-size*.62075f,size*.10575f,p);}
}

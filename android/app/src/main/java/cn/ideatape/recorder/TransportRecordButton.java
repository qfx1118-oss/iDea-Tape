package cn.ideatape.recorder;
import android.content.Context;
import android.graphics.*;
import android.widget.Button;
/** Raised transport key; text is announced by accessibility, never painted. */
public final class TransportRecordButton extends Button {
 private final Paint ink=new Paint(3);private final float d;
 public TransportRecordButton(Context context){super(context);d=getResources().getDisplayMetrics().density;setBackground(null);setMinHeight((int)(78*d));setMinimumHeight((int)(78*d));setContentDescription("开始手机录音");setHapticFeedbackEnabled(true);}
 protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),inset=4*d,drop=isPressed()?4*d:0;
  ink.setColor(0xff5d5c50);c.drawRoundRect(inset,5*d,w-inset,h-3*d,8*d,8*d,ink);
  ink.setColor(0xffaca48e);c.drawRoundRect(inset,drop,w-inset,h-9*d+drop,8*d,8*d,ink);
  ink.setColor(0xffe9e2cf);c.drawRoundRect(inset+2*d,drop+2*d,w-inset-2*d,h-13*d+drop,6*d,6*d,ink);
  ink.setColor(0xfff8f2e1);c.drawLine(inset+10*d,drop+3*d,w-inset-10*d,drop+3*d,ink);
  float x=w/2,y=(h-13*d)/2+drop;
  ink.setColor(0xff69291f);if(isSelected())c.drawRoundRect(x-12*d,y-11*d,x+12*d,y+13*d,2*d,2*d,ink);else c.drawCircle(x,y+1*d,15*d,ink);
  ink.setColor(isSelected()?0xffab3f2b:0xffc44330);if(isSelected())c.drawRoundRect(x-11*d,y-12*d,x+11*d,y+10*d,2*d,2*d,ink);else c.drawCircle(x,y-1*d,14*d,ink);
  if(isFocused()){ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(2*d);ink.setColor(0xff292a26);c.drawRoundRect(1*d,1*d,w-d,h-d,9*d,9*d,ink);ink.setStyle(Paint.Style.FILL);}
 }
 public boolean performClick(){performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);return super.performClick();}
}

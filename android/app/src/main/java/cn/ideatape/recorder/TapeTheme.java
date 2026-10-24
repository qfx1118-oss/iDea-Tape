package cn.ideatape.recorder;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
public final class TapeTheme {
 public static final int PAPER=0xfff3eee3,PANEL=0xfffcf9f2,INK=0xff2b302b,MUTED=0xff68675c,ACCENT=0xffa64f30,SOFT=0xffe9e1d2,GOLD=0xff956b14,DELETE=0xffa33e32;
 public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
 public static GradientDrawable surface(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
 public static Drawable ripple(Context c,int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x242b302b),surface(c,color,radius),surface(c,0xffffffff,radius));}
 public static TextView label(Context c,String value,float size,int color){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);v.setFontFeatureSettings("tnum");return v;}
 public static TapeIcon icon(Context c,String kind,int color,boolean fill){return new TapeIcon(kind,color,c.getResources().getDisplayMetrics().density).filled(fill);}
 public static Button iconButton(Context c,String kind,String description,int color,boolean fill){Button b=new Button(c){protected void onDraw(android.graphics.Canvas canvas){if(getText().length()==0){Drawable[] a=getCompoundDrawablesRelative();Drawable d=a[0]!=null?a[0]:a[1];if(d!=null){int w=d.getIntrinsicWidth(),h=d.getIntrinsicHeight();canvas.save();canvas.translate((getWidth()-w)/2f,(getHeight()-h)/2f);d.setBounds(0,0,w,h);d.draw(canvas);canvas.restore();return;}}super.onDraw(canvas);}};b.setStateListAnimator(null);b.setElevation(0);b.setText("");b.setContentDescription(description);b.setTooltipText(description);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));b.setGravity(Gravity.CENTER);b.setCompoundDrawablesRelativeWithIntrinsicBounds(icon(c,kind,color,fill),null,null,null);b.setBackground(ripple(c,android.graphics.Color.TRANSPARENT,12));return b;}
 public static void setIcon(Button b,String kind,String description,int color,boolean fill){b.setCompoundDrawablesRelativeWithIntrinsicBounds(icon(b.getContext(),kind,color,fill),null,null,null);b.setContentDescription(description);b.setTooltipText(description);}
}

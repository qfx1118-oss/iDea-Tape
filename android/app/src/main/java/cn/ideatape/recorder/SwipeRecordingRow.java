package cn.ideatape.recorder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Horizontal reveal is locked only after direction/slop; vertical library scrolling stays native. */
public final class SwipeRecordingRow extends FrameLayout {
 private final View foreground,dangerView;private final int slop;private final float reveal;
 private float downX,downY,initial;private boolean swiping,vertical,open;
 public Runnable onOpened;
 public SwipeRecordingRow(Context c,View content,View delete){super(c);slop=ViewConfiguration.get(c).getScaledTouchSlop();reveal=84*getResources().getDisplayMetrics().density;foreground=content;dangerView=delete;delete.setVisibility(INVISIBLE);
  LayoutParams danger=new LayoutParams((int)reveal,-1,Gravity.RIGHT);addView(delete,danger);addView(content,new LayoutParams(-1,-2));setClipToPadding(false);
 }
 public void reset(){foreground.animate().cancel();foreground.setTranslationX(0);dangerView.setVisibility(INVISIBLE);open=false;}
 public void close(){open=false;foreground.animate().translationX(0).setDuration(180).withEndAction(()->dangerView.setVisibility(INVISIBLE)).start();}
 public boolean onInterceptTouchEvent(android.view.MotionEvent e){
  if(e.getActionMasked()==MotionEvent.ACTION_DOWN){foreground.animate().cancel();downX=e.getX();downY=e.getY();initial=foreground.getTranslationX();swiping=false;vertical=false;}
  else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&!vertical){float dx=e.getX()-downX,dy=e.getY()-downY;if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){vertical=true;return false;}if(Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.3f){swiping=true;dangerView.setVisibility(VISIBLE);getParent().requestDisallowInterceptTouchEvent(true);return true;}}
  return swiping;
 }
 public boolean onTouchEvent(android.view.MotionEvent e){if(!swiping)return super.onTouchEvent(e);float dx=e.getX()-downX;
  if(e.getActionMasked()==MotionEvent.ACTION_MOVE)foreground.setTranslationX(Math.max(-reveal,Math.min(0,initial+dx)));
  else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){open=e.getActionMasked()!=MotionEvent.ACTION_CANCEL&&foreground.getTranslationX()<-reveal*.4f;foreground.animate().translationX(open?-reveal:0).setDuration(180).start();getParent().requestDisallowInterceptTouchEvent(false);swiping=false;if(open&&onOpened!=null)onOpened.run();performClick();}
  return true;
 }
 public boolean performClick(){super.performClick();return true;}
}

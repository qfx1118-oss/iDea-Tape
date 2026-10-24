package cn.ideatape.recorder;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** One 24-unit icon family, drawn with consistent rounded strokes. */
public final class TapeIcon extends Drawable {
 public static final String PAUSE="pause",PLAY="play",STOP="stop",STAR="star",MIC="mic",LIBRARY="library",MAP="map",DELETE="delete",MORE="more",SHARE="share",BACK="back",CALENDAR="calendar",SEARCH="search",BLUETOOTH="bluetooth",DISCONNECT="disconnect",PIN="pin",PLUS="plus",MINUS="minus",FIT="fit",GLOBE="globe",EDIT="edit",CLOSE="close",CHEVRON="chevron";
 private final String kind;private final Paint paint=new Paint(3);private final int size;private int color;private boolean filled;
 public TapeIcon(String kind,int color,float density){this.kind=kind;this.color=color;size=(int)(24*density+.5f);}
 public TapeIcon filled(boolean value){filled=value;return this;}
 public void setColor(int value){color=value;invalidateSelf();}
 private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,paint);}
 private void path(Canvas c,float... points){Path p=new Path();p.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);c.drawPath(p,paint);}
 public void draw(Canvas canvas){Rect b=getBounds();canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.65f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  switch(kind){
   case PLAY:paint.setStyle(Paint.Style.FILL);path(canvas,9,5,19,12,9,19,9,5);break;
   case PAUSE:paint.setStyle(Paint.Style.FILL);canvas.drawRect(7,5,10,19,paint);canvas.drawRect(14,5,17,19,paint);break;
   case STOP:paint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(6,6,18,18,2,2,paint);break;
   case STAR:Path star=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?9:4.2f;float x=12+(float)Math.cos(angle)*r,y=12+(float)Math.sin(angle)*r;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();if(filled)paint.setStyle(Paint.Style.FILL);canvas.drawPath(star,paint);break;
   case MIC:canvas.drawRoundRect(9,3,15,15,3,3,paint);canvas.drawArc(6,6,18,18,0,180,false,paint);line(canvas,12,18,12,22);line(canvas,8,22,16,22);break;
   case LIBRARY:canvas.drawRoundRect(3,5,21,20,3,3,paint);line(canvas,7,2,17,2);canvas.drawCircle(8,12,2,paint);canvas.drawCircle(16,12,2,paint);line(canvas,8,16,16,16);break;
   case MAP:path(canvas,3,5,9,3,15,6,21,4,21,19,15,21,9,18,3,20,3,5);line(canvas,9,3,9,18);line(canvas,15,6,15,21);break;
   case DELETE:line(canvas,4,6,20,6);path(canvas,9,6,9,3,15,3,15,6);path(canvas,6,6,7,21,17,21,18,6);line(canvas,10,10,10,17);line(canvas,14,10,14,17);break;
   case SHARE:canvas.drawCircle(6,12,2.5f,paint);canvas.drawCircle(18,5,2.5f,paint);canvas.drawCircle(18,19,2.5f,paint);line(canvas,8,11,16,6);line(canvas,8,13,16,18);break;
   case MORE:paint.setStyle(Paint.Style.FILL);for(int x=5;x<=19;x+=7)canvas.drawCircle(x,12,1.6f,paint);break;
   case BACK:path(canvas,11,5,4,12,11,19);line(canvas,4,12,21,12);break;
   case CALENDAR:canvas.drawRoundRect(3,5,21,21,3,3,paint);line(canvas,7,2,7,8);line(canvas,17,2,17,8);line(canvas,3,11,21,11);line(canvas,8,15,10,15);line(canvas,14,15,16,15);break;
   case SEARCH:canvas.drawCircle(10,10,6.5f,paint);line(canvas,15,15,21,21);break;
   case BLUETOOTH:path(canvas,7,6,17,16,12,21,12,3,17,8,7,18);break;
   case DISCONNECT:line(canvas,4,4,20,20);path(canvas,7,6,17,16,12,21,12,3,17,8,14,11);break;
   case PIN:canvas.drawOval(5,2,19,16,paint);path(canvas,6,13,12,22,18,13);canvas.drawCircle(12,9,2.2f,paint);break;
   case PLUS:line(canvas,5,12,19,12);line(canvas,12,5,12,19);break;
   case MINUS:line(canvas,5,12,19,12);break;
   case FIT:path(canvas,8,3,3,3,3,8);path(canvas,16,3,21,3,21,8);path(canvas,3,16,3,21,8,21);path(canvas,16,21,21,21,21,16);canvas.drawCircle(12,12,3,paint);break;
   case GLOBE:canvas.drawCircle(12,12,9,paint);canvas.drawOval(8,3,16,21,paint);line(canvas,3,12,21,12);line(canvas,5,7,19,7);line(canvas,5,17,19,17);break;
   case EDIT:path(canvas,4,16,16,4,20,8,8,20,3,21,4,16);line(canvas,13,7,17,11);break;
   case CLOSE:line(canvas,6,6,18,18);line(canvas,18,6,6,18);break;
   case CHEVRON:path(canvas,9,5,16,12,9,19);break;
   default:canvas.drawCircle(12,12,8,paint);
  }
  canvas.restore();
 }
 public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
 public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

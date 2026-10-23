package cn.ideatape.recorder;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

/** Native recycled rows with date groups and a single open swipe action. */
public final class RecordingListAdapter extends BaseAdapter {
 public interface Actions {void play(JSONObject item);void favorite(JSONObject item);void detail(JSONObject item);void more(JSONObject item);void delete(JSONObject item);}
 private final Context context;private final Actions actions;private final ArrayList<Object> entries=new ArrayList<>();private String playing="";private SwipeRecordingRow opened;
 public RecordingListAdapter(Context context,Actions actions){this.context=context;this.actions=actions;}
 private int dp(float n){return TapeTheme.dp(context,n);}
 public void submit(ArrayList<JSONObject> items,String playing){closeSwipe();this.playing=playing;entries.clear();String previous="";SimpleDateFormat f=new SimpleDateFormat("yyyy.MM.dd",Locale.getDefault());String today=f.format(new Date());Calendar yesterday=Calendar.getInstance();yesterday.add(Calendar.DAY_OF_YEAR,-1);String yesterdayText=f.format(yesterday.getTime());for(JSONObject item:items){long epoch=item.optLong("startedAt");if(epoch<=0)epoch=item.optLong("importedAt");String date=f.format(new Date(epoch));if(!date.equals(previous)){entries.add(date.equals(today)?"今天":date.equals(yesterdayText)?"昨天":date);previous=date;}entries.add(item);}notifyDataSetChanged();}
 public void closeSwipe(){if(opened!=null){opened.close();opened=null;}}
 public int getCount(){return entries.size();}public Object getItem(int position){return entries.get(position);}public long getItemId(int position){return position;}
 public int getViewTypeCount(){return 2;}public int getItemViewType(int position){return entries.get(position) instanceof String?0:1;}
 public boolean isEnabled(int position){return getItemViewType(position)==1;}
 private final class Row {
  JSONObject item;LinearLayout front;TextView title,meta,place;Button play,star,more;SwipeRecordingRow swipe;
  Row(){
   front=new LinearLayout(context);front.setOrientation(LinearLayout.HORIZONTAL);front.setGravity(Gravity.CENTER_VERTICAL);front.setPadding(dp(10),dp(12),dp(3),dp(12));front.setMinimumHeight(dp(88));front.setBackground(TapeTheme.surface(context,TapeTheme.PANEL,14));
   play=TapeTheme.iconButton(context,TapeIcon.PLAY,"播放",TapeTheme.ACCENT,false);play.setBackground(TapeTheme.ripple(context,TapeTheme.SOFT,24));front.addView(play,new LinearLayout.LayoutParams(dp(48),dp(48)));play.setOnClickListener(v->actions.play(item));
   LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(2),0,dp(2));front.addView(body,new LinearLayout.LayoutParams(0,-2,1));title=TapeTheme.label(context,"",15,TapeTheme.INK);title.setTypeface(Typeface.create("sans-serif-medium",0));title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);body.addView(title);meta=TapeTheme.label(context,"",11,TapeTheme.MUTED);meta.setMaxLines(1);meta.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams metadata=new LinearLayout.LayoutParams(-1,-2);metadata.topMargin=dp(7);body.addView(meta,metadata);place=TapeTheme.label(context,"",12,TapeTheme.MUTED);place.setMaxLines(1);place.setEllipsize(TextUtils.TruncateAt.END);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);pp.topMargin=dp(5);body.addView(place,pp);body.setOnClickListener(v->actions.detail(item));
   star=TapeTheme.iconButton(context,TapeIcon.STAR,"收藏",TapeTheme.MUTED,false);front.addView(star,new LinearLayout.LayoutParams(dp(48),dp(48)));star.setOnClickListener(v->actions.favorite(item));
   more=TapeTheme.iconButton(context,TapeIcon.MORE,"更多操作",TapeTheme.MUTED,false);front.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));more.setOnClickListener(v->actions.more(item));
   Button delete=TapeTheme.iconButton(context,TapeIcon.DELETE,"删除手机中的录音",0xfffff7ee,false);delete.setBackground(TapeTheme.ripple(context,TapeTheme.DELETE,14));delete.setOnClickListener(v->actions.delete(item));swipe=new SwipeRecordingRow(context,front,delete);swipe.onOpened=()->{if(opened!=null&&opened!=swipe)opened.close();opened=swipe;};
  }
  void bind(JSONObject value){item=value;swipe.reset();String key=item.optString("key");boolean active=key.equals(playing),favorite=item.optBoolean("favorite");title.setText(item.optString("title","未命名录音"));long epoch=item.optLong("startedAt");boolean unknown=epoch<=0;if(unknown)epoch=item.optLong("importedAt");long seconds=item.optLong("samples")/16000;String info=new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(epoch))+"  ·  "+String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60)+(unknown?"  · 导入":"");meta.setText(info);JSONObject point=item.optJSONObject("location");place.setVisibility(point==null?View.GONE:View.VISIBLE);if(point!=null){place.setText(point.optString("placeName","地址待解析"));android.graphics.drawable.Drawable pin=TapeTheme.icon(context,TapeIcon.PIN,TapeTheme.MUTED,false);pin.setBounds(0,0,dp(14),dp(14));place.setCompoundDrawablesRelative(pin,null,null,null);place.setCompoundDrawablePadding(dp(5));}android.graphics.drawable.Drawable source=TapeTheme.icon(context,"phone".equals(item.optString("source"))?TapeIcon.MIC:TapeIcon.LIBRARY,TapeTheme.MUTED,false);source.setBounds(0,0,dp(14),dp(14));meta.setCompoundDrawablesRelative(source,null,null,null);meta.setCompoundDrawablePadding(dp(5));
   TapeTheme.setIcon(play,active?TapeIcon.STOP:TapeIcon.PLAY,active?"停止播放":"播放录音",active?TapeTheme.PANEL:TapeTheme.ACCENT,false);play.setBackground(TapeTheme.ripple(context,active?TapeTheme.ACCENT:TapeTheme.SOFT,24));TapeTheme.setIcon(star,TapeIcon.STAR,favorite?"取消收藏":"收藏录音",favorite?TapeTheme.GOLD:TapeTheme.MUTED,favorite);front.setBackground(TapeTheme.surface(context,active?0xfff0e4d4:TapeTheme.PANEL,14));
  }
 }
 public View getView(int position,View convert,ViewGroup parent){Object item=getItem(position);if(item instanceof String){TextView header=convert instanceof TextView?(TextView)convert:TapeTheme.label(context,"",12,TapeTheme.MUTED);header.setText((String)item);header.setPadding(dp(4),dp(22),dp(4),dp(10));header.setTypeface(Typeface.create("sans-serif-medium",0));return header;}
  LinearLayout wrapper;Row row;if(convert instanceof LinearLayout&&convert.getTag() instanceof Row){wrapper=(LinearLayout)convert;row=(Row)wrapper.getTag();}else{row=new Row();wrapper=new LinearLayout(context);wrapper.setOrientation(1);wrapper.setPadding(0,0,0,dp(8));wrapper.addView(row.swipe,new LinearLayout.LayoutParams(-1,-2));wrapper.setTag(row);}row.bind((JSONObject)item);return wrapper;
 }
}

#include "tape_ui.h"
#include "tape_codec.h"
#include <math.h>
#include <stdio.h>
#include <string.h>
LV_FONT_DECLARE(tape_font_12);
LV_FONT_DECLARE(tape_font_16);
LV_FONT_DECLARE(tape_font_30);
static tape_view_t view;
static tape_motion_t motion;
static lv_obj_t *screen;
static double angle;
static const uint32_t paper=0xe5dfcd,ink=0x292a26,muted=0x777565,orange=0xc26738;
static void box(lv_layer_t *l,int x,int y,int w,int h,int radius,uint32_t bg,uint32_t border,int bw){
    lv_draw_rect_dsc_t d;lv_draw_rect_dsc_init(&d);d.bg_color=lv_color_hex(bg);d.radius=radius;d.border_color=lv_color_hex(border);d.border_width=bw;
    lv_area_t a={x,y,x+w-1,y+h-1};lv_draw_rect(l,&d,&a);
}
static void line(lv_layer_t *l,int x,int y,int xx,int yy,uint32_t color,int width){
    lv_draw_line_dsc_t d;lv_draw_line_dsc_init(&d);d.p1.x=x;d.p1.y=y;d.p2.x=xx;d.p2.y=yy;d.color=lv_color_hex(color);d.width=width;d.round_start=1;d.round_end=1;lv_draw_line(l,&d);
}
static void text(lv_layer_t *l,int x,int y,int w,const char *s,const lv_font_t *f,uint32_t color,lv_text_align_t align){
    lv_draw_label_dsc_t d;lv_draw_label_dsc_init(&d);d.text=s;d.font=f;d.color=lv_color_hex(color);d.align=align;d.text_local=1;
    lv_area_t a={x,y,x+w-1,y+f->line_height+3};lv_draw_label(l,&d,&a);
}
static void screw(lv_layer_t *l,int x,int y){box(l,x-3,y-3,7,7,4,0x67685d,0x161714,1);line(l,x-1,y+1,x+1,y-1,0xb8b5a3,1);}
static void reel(lv_layer_t *l,int cx,int cy,int tape_radius,double a){
    box(l,cx-tape_radius,cy-tape_radius,tape_radius*2+1,tape_radius*2+1,50,0x62533c,0x7e6e50,1);
    box(l,cx-14,cy-14,29,29,40,0xd9d3bd,0xa49b80,1);
    for(int i=0;i<6;i++){
        double t=(a+i*60)*0.01745329252;
        int x=cx+(int)(10*cos(t)),y=cy+(int)(10*sin(t));
        box(l,x-2,y-2,5,5,2,0x4c4c40,0x4c4c40,0);
    }
    box(l,cx-4,cy-4,9,9,5,0x262822,0x8d8c7b,1);
}
static void draw(lv_event_t *e){
    lv_layer_t *l=lv_event_get_layer(e);
    box(l,0,0,240,320,0,paper,paper,0);
    text(l,20,15,156,"iDea Tape",&lv_font_montserrat_20,ink,LV_TEXT_ALIGN_LEFT);
    char b[96];if(view.battery<0)snprintf(b,sizeof(b),"--%%");else snprintf(b,sizeof(b),"%d%%",view.battery);
    text(l,178,20,42,b,&tape_font_12,muted,LV_TEXT_ALIGN_RIGHT);
    line(l,20,46,220,46,0xaaa48e,1);
    if(view.model.mode==TAPE_EXPORT){
        text(l,22,60,196,"电脑导出",&tape_font_16,ink,LV_TEXT_ALIGN_LEFT);
        text(l,22,100,196,"1 连接热点",&tape_font_12,muted,LV_TEXT_ALIGN_LEFT);
        text(l,22,124,196,view.ssid,&tape_font_16,ink,LV_TEXT_ALIGN_LEFT);
        snprintf(b,sizeof(b),"密码 %s",view.password);
        text(l,22,152,196,b,&tape_font_16,ink,LV_TEXT_ALIGN_LEFT);
        text(l,22,195,196,"2 浏览器打开",&tape_font_12,muted,LV_TEXT_ALIGN_LEFT);
        text(l,22,220,196,"192.168.4.1",&lv_font_montserrat_20,ink,LV_TEXT_ALIGN_LEFT);
        text(l,22,276,196,"长按 OK 退出导出",&tape_font_12,muted,LV_TEXT_ALIGN_CENTER);return;
    }
    bool library=view.model.mode!=TAPE_RECORD;
    if(library)snprintf(b,sizeof(b),"TAPE %02u / %02u",view.model.count?view.model.selected+1:0,view.model.count);
    else snprintf(b,sizeof(b),"SIDE A   /   %02u TAPES",view.model.count);
    text(l,22,50,196,b,&tape_font_12,muted,LV_TEXT_ALIGN_LEFT);
    if(view.phone)text(l,190,50,30,"BLE",&tape_font_12,orange,LV_TEXT_ALIGN_RIGHT);
    box(l,14,70,212,121,9,ink,0x121511,2);
    box(l,23,79,194,28,3,orange,orange,0);
    text(l,32,85,160,"iDea  /  NORMAL TYPE I",&tape_font_12,paper,LV_TEXT_ALIGN_LEFT);
    if(view.model.recording)box(l,199,87,9,9,6,0xee342b,0x932d25,1);
    box(l,28,113,184,48,23,0x191c18,0x787667,1);
    line(l,64,143,176,143,0x978565,2);
    box(l,90,127,60,17,2,0x555746,0x797a67,1);
    text(l,94,129,52,"A  /  90",&tape_font_12,0xbfbfa8,LV_TEXT_ALIGN_CENTER);
    double ratio=view.duration?(double)view.samples/view.duration:0;
    if(ratio>1)ratio=1;
    reel(l,62,137,22-(int)(ratio*3),angle);
    reel(l,178,137,19+(int)(ratio*3),angle*1.07+17);
    screw(l,23,79);screw(l,217,79);screw(l,23,182);screw(l,217,182);
    box(l,72,169,96,17,4,0x45463c,0x111510,1);
    box(l,87,175,6,6,3,0x171b16,0x171b16,0);box(l,147,175,6,6,3,0x171b16,0x171b16,0);
    text(l,101,170,38,"A",&tape_font_12,0xaaa994,LV_TEXT_ALIGN_CENTER);
    unsigned seconds=(library && !view.model.playing)?view.duration/TAPE_RATE:view.samples/TAPE_RATE;
    snprintf(b,sizeof(b),"%02u:%02u",seconds/60,seconds%60);
    text(l,20,198,200,b,&tape_font_30,ink,LV_TEXT_ALIGN_CENTER);
    const char *status=view.model.recording?"录音中":view.model.playing?"播放中":library?(view.model.count?"已停止":"没有录音"):"录音待机";
    const char *transfer_status=view.sync.state==SYNC_DONE?"传输完成":view.sync.state==SYNC_PAUSED?"传输暂停 缓存保留":view.sync.state==SYNC_SAVING?"手机正在保存":view.sync.state==SYNC_RECEIVING?"正在传输到手机":view.sync.state==SYNC_LIVE?"录音中 边录边传":NULL;
    text(l,20,238,200,view.message[0]?view.message:transfer_status?transfer_status:status,&tape_font_16,view.model.recording?0xb6372c:ink,LV_TEXT_ALIGN_CENTER);
    if(view.sync.state && !view.model.recording && !view.model.playing){
        unsigned percent=view.sync.total?(unsigned)((uint64_t)view.sync.received*100/view.sync.total):0;
        if(view.sync.state!=SYNC_DONE && percent>=100)percent=99;
        if(view.sync.state==SYNC_DONE)percent=100;
        box(l,34,262,138,5,2,0xc7c1ac,0,0);if(percent)box(l,34,262,138*percent/100,5,2,orange,0,0);
        snprintf(b,sizeof(b),"%u%%",percent);text(l,176,257,40,b,&tape_font_12,muted,LV_TEXT_ALIGN_RIGHT);
    }else if(view.model.recording || view.model.playing){
        for(unsigned i=0;i<20;i++)box(l,36+i*8,263,5,4,1,i<view.peak*20/32768?(i>16?0xb6372c:orange):0xc7c1ac,0,0);
    }else{
        snprintf(b,sizeof(b),library?"TAPE %08u":"剩余 %u 分钟",library?view.id:view.remaining/60);
        text(l,22,260,196,b,&tape_font_12,muted,LV_TEXT_ALIGN_CENTER);
    }
    const char *h1,*h2;
    if(view.model.recording){h1=view.model.held_record?"松开上键 保存":"下键 结束并保存";h2="录音中";}
    else if(library){h1="上下 选择  OK 播放/停止";h2="长按上下 删除  长按OK 返回";}
    else {h1="按住上键录  下键开始/结束";h2="OK 选择音频  长按OK 电脑导出";}
    text(l,20,279,200,h1,&tape_font_12,muted,LV_TEXT_ALIGN_CENTER);
    text(l,20,296,200,h2,&tape_font_12,muted,LV_TEXT_ALIGN_CENTER);
    if(view.model.mode==TAPE_DELETE){
        box(l,18,91,204,156,8,paper,orange,2);
        text(l,28,110,184,"删除这段录音？",&tape_font_16,ink,LV_TEXT_ALIGN_CENTER);
        snprintf(b,sizeof(b),"TAPE %08u",view.id);text(l,28,145,184,b,&tape_font_16,muted,LV_TEXT_ALIGN_CENTER);
        text(l,28,184,184,"OK 确认删除",&tape_font_16,0xb6372c,LV_TEXT_ALIGN_CENTER);
        text(l,28,215,184,"上下 取消",&tape_font_12,muted,LV_TEXT_ALIGN_CENTER);
    }
}
void tape_ui_create(void){
    screen=lv_obj_create(NULL);lv_obj_remove_style_all(screen);lv_obj_set_size(screen,240,320);
    lv_obj_add_event_cb(screen,draw,LV_EVENT_DRAW_MAIN,NULL);lv_screen_load(screen);
}
void tape_ui_render(const tape_view_t *v,uint64_t now){
    bool changed=v->revision!=view.revision;
    view=*v;double previous=angle;
    angle=tape_motion_step(&motion,view.model.recording||view.model.playing,now);
    if(changed || fabs(previous-angle)>0.005)lv_obj_invalidate(screen);
}

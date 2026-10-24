#include "tape_ui.h"
#include "tape_codec.h"
#include <math.h>
#include <stdio.h>
#include <string.h>
LV_FONT_DECLARE(tape_brand_24);
LV_FONT_DECLARE(tape_brand_38);
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
    box(l,17,10,142,31,5,ink,ink,0);
    text(l,32,12,123,"ıDea Tape",&tape_brand_24,0xeee0c1,LV_TEXT_ALIGN_LEFT);
    box(l,36,13,5,5,3,view.model.recording?0xe54d2d:0xeee0c1,0,0);
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
    if(view.phone&&view.place.state){
        const char *place=view.place.state==2?view.place.label:view.place.state==1?"定位解析中":"地址暂不可用";
        /* Fit at most 12 full Unicode characters; road is deliberately first. */
        size_t end=0;unsigned count=0;while(place[end]&&count<12){end++;while((place[end]&0xc0)==0x80)end++;count++;}
        snprintf(b,sizeof(b),"%.*s%s",(int)end,place,place[end]?"…":"");
    }
    text(l,22,50,162,b,&tape_font_12,muted,LV_TEXT_ALIGN_LEFT);
    if(view.phone)text(l,190,50,30,"BLE",&tape_font_12,orange,LV_TEXT_ALIGN_RIGHT);
    box(l,14,68,212,134,8,0x202624,0x8d856f,2);
    box(l,21,75,198,94,6,0x191f1d,0x9e9379,1);

    text(l,29,77,184,"ıDea Tape",&tape_brand_38,0xeee0c1,LV_TEXT_ALIGN_LEFT);
    box(l,36,79,9,9,5,view.model.recording?0xe54d2d:0xeee0c1,0,0);
    line(l,27,118,212,112,0xeee0c1,2);
    text(l,154,155,55,"SIDE A",&tape_font_12,orange,LV_TEXT_ALIGN_RIGHT);
    box(l,42,119,156,34,17,0x0c110f,0x686250,1);
    box(l,96,123,48,24,2,0x584936,0x7e6e50,1);
    for(int x=100;x<144;x+=6)line(l,x,126,x,144,0x9b886b,1);
    reel(l,64,136,16,angle);reel(l,176,136,16,angle*1.07+17);
    box(l,54,173,132,23,3,0x161d1a,0x686250,1);
    for(int y=176;y<194;y+=3)line(l,58,y,182,y,0x414940,1);
    box(l,69,181,9,9,5,0x090e0b,0,0);box(l,162,181,9,9,5,0x090e0b,0,0);
    screw(l,23,77);screw(l,217,77);screw(l,23,193);screw(l,217,193);
    unsigned seconds=(library && !view.model.playing)?view.duration/TAPE_RATE:view.samples/TAPE_RATE;
    snprintf(b,sizeof(b),"%02u:%02u",seconds/60,seconds%60);
    text(l,20,207,200,b,&tape_font_30,ink,LV_TEXT_ALIGN_CENTER);
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

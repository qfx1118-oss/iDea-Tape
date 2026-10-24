#include "tape_ui.h"
#include "bsp_display_rounding.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
LV_FONT_DECLARE(tape_brand_24);
LV_FONT_DECLARE(tape_brand_30);
LV_FONT_DECLARE(tape_font_12);
LV_FONT_DECLARE(tape_font_16);
static unsigned char frame[240*320*3];
static void flush(lv_display_t *d,const lv_area_t *a,uint8_t *p){
    for(int y=a->y1;y<=a->y2;y++)for(int x=a->x1;x<=a->x2;x++){
        int i=(y*240+x)*3;frame[i]=p[2];frame[i+1]=p[1];frame[i+2]=p[0];p+=3;
        if(bsp_display_pixel_outside_rounded_rect(x,y,240,320,30))frame[i]=frame[i+1]=frame[i+2]=0;
    }
    lv_display_flush_ready(d);
}
static void font_check(void){
    const lv_font_t *brand[]={&tape_brand_24,&tape_brand_30};
    for(unsigned f=0;f<2;f++){lv_font_glyph_dsc_t d;assert(lv_font_get_glyph_dsc(brand[f],&d,0x131,0)&&!d.is_placeholder);assert(lv_font_get_glyph_dsc(brand[f],&d,'T',0)&&!d.is_placeholder);}

    FILE *f=fopen("assets/fonts/tape-characters.txt","r");assert(f);char buf[4096];size_t n=fread(buf,1,sizeof(buf)-1,f);fclose(f);buf[n]=0;
    for(unsigned i=0;i<n;){
        unsigned cp=(unsigned char)buf[i++];if(cp=='\n')continue;
        if(cp>=0xe0){cp&=15;cp=(cp<<6)|((unsigned char)buf[i++]&63);cp=(cp<<6)|((unsigned char)buf[i++]&63);}
        else if(cp>=0xc0){cp&=31;cp=(cp<<6)|((unsigned char)buf[i++]&63);}
        lv_font_glyph_dsc_t d;
        assert(lv_font_get_glyph_dsc(&tape_font_12,&d,cp,0) && !d.is_placeholder);
        assert(lv_font_get_glyph_dsc(&tape_font_16,&d,cp,0) && !d.is_placeholder);
    }
    for(unsigned cp=0x4e00;cp<=0x9fef;cp++){lv_font_glyph_dsc_t d;assert(lv_font_get_glyph_dsc(&tape_font_12,&d,cp,0)&&!d.is_placeholder);}
    lv_point_t size;
    const char *hints[]={"按住上键录  下键开始/结束","OK 选择音频  长按OK 电脑导出","上下 选择  OK 播放/停止","长按上下 删除  长按OK 返回"};
    for(unsigned i=0;i<4;i++){lv_text_get_size(&size,hints[i],&tape_font_12,0,0,1000,LV_TEXT_FLAG_NONE);assert(size.x<=200);}
}
int main(int argc,char **argv){
    assert(argc==2 || argc==3);lv_init();font_check();
    lv_display_t *d=lv_display_create(240,320);lv_display_set_color_format(d,LV_COLOR_FORMAT_RGB888);
    static unsigned char buffer[240*20*3];lv_display_set_buffers(d,buffer,NULL,sizeof(buffer),LV_DISPLAY_RENDER_MODE_PARTIAL);lv_display_set_flush_cb(d,flush);
    tape_ui_create();
    tape_view_t v={.battery=78,.remaining=420,.model={.count=8},.id=23,.duration=90*16000,.revision=1};
    if(argc==3){
        for(unsigned i=0;i<120;i++){
            v.model.recording=i<60;v.samples=16*16000;v.peak=i<60?20000:0;v.revision++;
            tape_ui_render(&v,1000+i*20);lv_refr_now(d);
            char path[512];snprintf(path,sizeof(path),"%s/frame-%03u.ppm",argv[1],i);FILE *f=fopen(path,"wb");assert(f);fprintf(f,"P6\n240 320\n255\n");fwrite(frame,1,sizeof(frame),f);fclose(f);
        }
        return 0;
    }
    const char *names[]={"standby","recording","library","playing","delete","export","empty","storage-error","transfer","transfer-done","street-name"};
    for(unsigned i=0;i<11;i++){
        v.sync=(tape_sync_view_t){0};v.model.mode=TAPE_RECORD;v.model.recording=false;v.model.playing=false;v.model.count=8;v.message[0]=0;v.samples=16*16000;v.revision++;
        if(i==1){v.model.recording=true;v.model.held_record=true;v.peak=24000;}
        if(i>=2 && i<=4)v.model.mode=TAPE_LIBRARY;
        if(i==3){v.model.playing=true;v.peak=16000;}
        if(i==4)v.model.mode=TAPE_DELETE;
        if(i==5){v.model.mode=TAPE_EXPORT;strcpy(v.ssid,"iDea-Tape-A1F0");strcpy(v.password,"12345678");}
        if(i==6){v.model.mode=TAPE_LIBRARY;v.model.count=0;v.duration=0;v.id=0;}
        if(i==7)strcpy(v.message,"存储不可用");
        if(i>=8){v.phone=true;v.sync=(tape_sync_view_t){.state=i==8?SYNC_RECEIVING:SYNC_DONE,.id=104,.received=i==8?73:100,.total=100};}
        if(i==10){v.phone=true;v.sync=(tape_sync_view_t){0};v.place.state=2;strcpy(v.place.label,"人民南路");}
        tape_ui_render(&v,1000+i*1000);lv_refr_now(d);
        char path[512];snprintf(path,sizeof(path),"%s/%s.ppm",argv[1],names[i]);FILE *f=fopen(path,"wb");assert(f);fprintf(f,"P6\n240 320\n255\n");fwrite(frame,1,sizeof(frame),f);fclose(f);
    }
    puts("Actual LVGL screens, Chinese glyphs and footer widths: PASS");
}

/* iDea Tape: one worker owns all state/audio/files; LVGL reads snapshots only.
 * Export is a mutually exclusive, read-only storage session. */
#include "tape_model.h"
#include "tape_codec.h"
#include "tape_store.h"
#include "tape_ui.h"
#include "tape_web.h"
#include "tape_ble.h"
#include "tape_sync.h"
#include "bsp_display.h"
#include "bsp_audio.h"
#include "bsp_button.h"
#include "bsp_battery.h"
#include "bsp_i2c.h"
#include "esp_timer.h"
#include "esp_log.h"
#include "esp_spiffs.h"
#include "esp_system.h"
#include "nvs_flash.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
#include <stdatomic.h>
#include <string.h>
#include <stdlib.h>
static const char *TAG="idea_tape";
typedef struct { tape_key_t key; tape_event_t event; } key_tape_t;
static QueueHandle_t keys;
static SemaphoreHandle_t view_lock;
static tape_view_t shared;
static atomic_bool overflow;
static bool keys_ready;
static uint64_t clock_ms(void){return (uint64_t)esp_timer_get_time()/1000;}
static void on_key(bsp_btn_t k,bsp_btn_ev_t e,void *arg){
    (void)arg;
    tape_event_t event;
    if(e==BSP_BTN_PRESS)event=TAPE_PRESS;
    else if(e==BSP_BTN_RELEASE)event=TAPE_RELEASE;
    else if(e==BSP_BTN_LONG)event=TAPE_LONG;
    else return;
    key_tape_t input={(tape_key_t)k,event};
    if(xQueueSend(keys,&input,0)!=pdTRUE)atomic_store(&overflow,true);
}
static void ui_tick(lv_timer_t *timer){
    (void)timer;tape_view_t v;
    if(xSemaphoreTake(view_lock,0)==pdTRUE){v=shared;xSemaphoreGive(view_lock);tape_ui_render(&v,clock_ms());}
}
static void publish(tape_view_t *v,const tape_store_t *s){
    v->phone=tape_ble_connected();v->sync=tape_sync_view();
    v->model.count=s->count;
    if(v->model.selected>=s->count)v->model.selected=s->count?s->count-1:0;
    if(!v->model.recording && s->count){v->id=s->files[v->model.selected].id;v->duration=s->files[v->model.selected].blocks*TAPE_SAMPLES;}
    else if(!s->count && !v->model.recording){v->id=0;v->duration=0;}
    v->remaining=s->free_bytes>262144?(unsigned)((s->free_bytes-262144)/TAPE_BLOCK*TAPE_SAMPLES/TAPE_RATE):0;
    v->revision++;
    xSemaphoreTake(view_lock,portMAX_DELAY);shared=*v;xSemaphoreGive(view_lock);
}
static bool audio_ready(void){
    return bsp_audio_init()==ESP_OK && bsp_audio_wake()==ESP_OK && bsp_audio_set_format(TAPE_RATE,16,1)==ESP_OK;
}
static unsigned peak(const int16_t *p){unsigned max=0;for(unsigned i=0;i<TAPE_SAMPLES;i++){unsigned a=p[i]<0?-(int)p[i]:p[i];if(a>max)max=a;}return max;}
static void worker(void *arg){
    (void)arg;
    tape_store_t store={0};tape_view_t v={.battery=-1};
    FILE *capture=NULL,*play=NULL;unsigned record_id=0,blocks=0;
    uint8_t encoded[TAPE_BLOCK];int16_t pcm[TAPE_SAMPLES];
    esp_err_t storage=tape_store_init(&store);
    if(storage!=ESP_OK)strcpy(v.message,"存储不可用");
    if(!keys_ready)strcpy(v.message,"按键不可用");
    uint64_t battery_due=0,notice_until=storage==ESP_OK?0:UINT64_MAX;
    tape_sync_init();
    publish(&v,&store);
    bool phone_was=false;unsigned pairing_was=0;
    for(;;){
        unsigned old_count=store.count;
        tape_sync_tick(&store,tape_model_storage_busy(&v.model));
        tape_sync_view_t sync=tape_sync_view();
        if(memcmp(&sync,&v.sync,sizeof(sync)))publish(&v,&store);
        unsigned pairing=tape_ble_passkey();bool phone=tape_ble_connected();
        if(pairing!=pairing_was || phone!=phone_was || old_count!=store.count){
            pairing_was=pairing;phone_was=phone;
            if(pairing){snprintf(v.message,sizeof(v.message),"配对码 %06u",pairing);notice_until=UINT64_MAX;}
            else {v.message[0]=0;notice_until=0;}
            publish(&v,&store);
        }
        if(atomic_exchange(&overflow,false)){
            tape_finished(&v.model);memset(v.model.down,0,sizeof(v.model.down));
            if(capture){bool saved=tape_store_finish(&store,capture,record_id);capture=NULL;tape_sync_end(saved);}
            if(play){fclose(play);play=NULL;}
            (void)bsp_audio_sleep();strcpy(v.message,"操作太快请重试");notice_until=clock_ms()+2500;publish(&v,&store);
        }
        key_tape_t input;
        while(xQueueReceive(keys,&input,0)==pdTRUE){
            tape_action_t action=tape_key(&v.model,input.key,input.event);
            if(storage==ESP_OK && !pairing){v.message[0]=0;notice_until=0;}
            if(action==TAPE_START){
                if(store.ready && audio_ready()){
                    bsp_audio_set_volume(0);
                    capture=tape_store_begin(&store,&record_id);
                }
                if(!capture){tape_finished(&v.model);strcpy(v.message,store.ready?"空间不足或设备错误":"存储不可用");notice_until=clock_ms()+3000;(void)bsp_audio_sleep();}
                else {blocks=0;v.samples=0;v.id=record_id;v.model.selected=0;tape_sync_begin(record_id);}
            }else if(action==TAPE_STOP){
                if(capture){
                    bool ok=tape_store_finish(&store,capture,record_id);capture=NULL;tape_sync_end(ok);
                    strcpy(v.message,ok?"已保存":"保存失败");notice_until=clock_ms()+1800;
                }
                (void)bsp_audio_sleep();v.peak=0;
            }else if(action==TAPE_PLAY){
                if(v.model.selected<store.count && audio_ready()){
                    bsp_audio_set_volume(65);play=tape_store_open(store.files[v.model.selected].id);
                }
                if(!play){tape_finished(&v.model);strcpy(v.message,"音频读取失败");notice_until=clock_ms()+2500;(void)bsp_audio_sleep();}
                v.samples=0;
            }else if(action==TAPE_HALT){
                if(play){fclose(play);play=NULL;}
                v.peak=0;(void)bsp_audio_sleep();
            }else if(action==TAPE_REMOVE){
                tape_sync_cancel_transfer();
                if(v.model.selected<store.count && !tape_store_delete(&store,store.files[v.model.selected].id)){
                    strcpy(v.message,"删除失败");notice_until=clock_ms()+2500;
                }
            }else if(action==TAPE_WIFI_ON){
                (void)bsp_audio_sleep();tape_sync_cancel_transfer();
                bool ble_stopped=tape_ble_stop();
                if(!store.ready || !ble_stopped || tape_web_start(&store,v.ssid,v.password)!=ESP_OK){
                    v.model.mode=TAPE_RECORD;strcpy(v.message,"连接失败");notice_until=clock_ms()+3000;(void)tape_ble_start();
                }
                ESP_LOGI(TAG,"Export mode heap=%u",(unsigned)esp_get_free_heap_size());
            }else if(action==TAPE_WIFI_OFF){tape_web_stop();(void)tape_ble_start();}
            publish(&v,&store);
        }
        if(capture){
            bool failed=bsp_audio_read(pcm,sizeof(pcm))!=ESP_OK;
            if(!failed){
                v.peak=peak(pcm);tape_encode(pcm,encoded);
                failed=fwrite(encoded,1,sizeof(encoded),capture)!=sizeof(encoded);
                if(!failed){tape_sync_block(encoded);blocks++;v.samples=blocks*TAPE_SAMPLES;}
            }
            if(!failed && blocks%64==0){
                if(fflush(capture))failed=true;
                size_t total=0,used=0;
                if(esp_spiffs_info("tapes",&total,&used)!=ESP_OK)failed=true;
                store.free_bytes=total*3/4>used?total*3/4-used:0;
            }
            if(failed || store.free_bytes<=262144){
                bool ok=tape_store_finish(&store,capture,record_id);capture=NULL;tape_sync_end(ok);tape_finished(&v.model);
                strcpy(v.message,!ok?"保存失败":failed?"录音设备错误":"空间已满 已保存");notice_until=clock_ms()+4000;
                (void)bsp_audio_sleep();
            }
            publish(&v,&store);
        }else if(play){
            bool failed=false;
            size_t n=fread(encoded,1,sizeof(encoded),play);
            if(n==sizeof(encoded)){
                failed=!tape_decode(encoded,pcm);
                if(!failed){v.peak=peak(pcm);failed=bsp_audio_write(pcm,sizeof(pcm))!=ESP_OK;if(!failed)v.samples+=TAPE_SAMPLES;}
            }else failed=n!=0 || ferror(play);
            if(failed || n!=sizeof(encoded) || v.samples>=v.duration){
                /* BSP TX queues up to 6*240 frames: 90 ms at 16 kHz.
                 * Let the final PCM reach the speaker before disabling I2S.
                 * Explicit user STOP follows the immediate TAPE_HALT path. */
                if(!failed)vTaskDelay(pdMS_TO_TICKS(100));
                fclose(play);play=NULL;tape_finished(&v.model);v.peak=0;(void)bsp_audio_sleep();
                if(failed){strcpy(v.message,"音频读取失败");notice_until=clock_ms()+3000;}
            }
            publish(&v,&store);
        }else vTaskDelay(pdMS_TO_TICKS(15));
        if(clock_ms()>=battery_due){
            v.battery=bsp_battery_soc();battery_due=clock_ms()+30000;publish(&v,&store);
        }
        if(notice_until && clock_ms()>=notice_until){v.message[0]=0;notice_until=0;publish(&v,&store);}
    }
}
void app_main(void){
    ESP_LOGI(TAG,"iDea Tape / AI Passport / 16 kHz mono");
    if(bsp_display_init()!=ESP_OK || !bsp_lvgl_init()){ESP_LOGE(TAG,"Display failed");return;}
    bsp_display_backlight(0);
    (void)bsp_i2c_init();(void)bsp_battery_init();
    /* Existing settings are never erased as an NVS recovery shortcut. */
    esp_err_t nvs=nvs_flash_init();if(nvs!=ESP_OK)ESP_LOGW(TAG,"NVS unavailable: %s",esp_err_to_name(nvs));
    keys=xQueueCreate(32,sizeof(key_tape_t));view_lock=xSemaphoreCreateMutex();
    if(!keys || !view_lock){ESP_LOGE(TAG,"Input allocation failed");return;}
    shared.battery=-1;shared.revision=1;strcpy(shared.message,"存储检查中");
    if(!bsp_lvgl_lock(1000))return;
    tape_ui_create();tape_ui_render(&shared,clock_ms());
    lv_timer_t *timer=lv_timer_create(ui_tick,20,NULL);bsp_lvgl_unlock();
    if(!timer){ESP_LOGE(TAG,"UI timer allocation failed");return;}
    bsp_display_backlight(65);
    keys_ready=bsp_button_init(on_key,NULL)==ESP_OK;
    if(xTaskCreate(worker,"idea_tape",6144,NULL,4,NULL)!=pdPASS)ESP_LOGE(TAG,"Worker allocation failed");
}

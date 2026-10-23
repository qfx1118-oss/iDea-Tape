#include "tape_web.h"
#include "tape_codec.h"
#include "tape_stream.h"
#include "esp_http_server.h"
#include "esp_wifi.h"
#include "esp_netif.h"
#include "esp_event.h"
#include "esp_random.h"
#include "esp_mac.h"
#include <stdlib.h>
#include <string.h>
static httpd_handle_t server;
static esp_netif_t *netif;
static bool wifi_initialized, wifi_running;
static tape_store_t snapshot;
extern const char tape_page_start[] asm("_binary_tape_page_html_start");
extern const char tape_page_end[] asm("_binary_tape_page_html_end");
static esp_err_t index_page(httpd_req_t *r){
    httpd_resp_set_type(r,"text/html; charset=utf-8");
    httpd_resp_set_hdr(r,"Cache-Control","no-store");
    return httpd_resp_send(r,tape_page_start,tape_page_end-tape_page_start);
}
static esp_err_t listing(httpd_req_t *r){
    httpd_resp_set_type(r,"application/json");httpd_resp_set_hdr(r,"Cache-Control","no-store");
    if(httpd_resp_sendstr_chunk(r,"[")!=ESP_OK)return ESP_FAIL;
    for(unsigned i=0;i<snapshot.count;i++){
        char line[100];snprintf(line,sizeof(line),"%s{\"id\":%u,\"seconds\":%.2f,\"bytes\":%u}",i?",":"",snapshot.files[i].id,(double)snapshot.files[i].blocks*TAPE_SAMPLES/TAPE_RATE,44+snapshot.files[i].blocks*TAPE_SAMPLES*2);
        if(httpd_resp_sendstr_chunk(r,line)!=ESP_OK)return ESP_FAIL;
    }
    if(httpd_resp_sendstr_chunk(r,"]")!=ESP_OK)return ESP_FAIL;
    return httpd_resp_send_chunk(r,NULL,0);
}
static bool send_audio(const void *data,size_t n,void *user){return httpd_resp_send_chunk((httpd_req_t*)user,data,n)==ESP_OK;}
static esp_err_t audio(httpd_req_t *r){
    const char *start=r->uri+7;char *end;unsigned long id=strtoul(start,&end,10);
    if(start==end || strcmp(end,".wav") || id>99999999)return httpd_resp_send_err(r,HTTPD_404_NOT_FOUND,"Unknown tape");
    const tape_file_t *file=NULL;
    for(unsigned i=0;i<snapshot.count;i++)if(snapshot.files[i].id==id)file=&snapshot.files[i];
    if(!file)return httpd_resp_send_err(r,HTTPD_404_NOT_FOUND,"Unknown tape");
    FILE *f=tape_store_open((unsigned)id);if(!f)return httpd_resp_send_err(r,HTTPD_500_INTERNAL_SERVER_ERROR,"Read failed");
    httpd_resp_set_type(r,"audio/wav");httpd_resp_set_hdr(r,"Cache-Control","no-store");
    char disposition[80];snprintf(disposition,sizeof(disposition),"inline; filename=\"iDea-Tape-%08lu.wav\"",id);
    httpd_resp_set_hdr(r,"Content-Disposition",disposition);
    bool ok=tape_stream(f,file->blocks,send_audio,r);
    fclose(f);
    if(!ok)return ESP_FAIL;
    return httpd_resp_send_chunk(r,NULL,0);
}
void tape_web_stop(void){
    if(server){httpd_stop(server);server=NULL;}
    if(wifi_running){esp_wifi_stop();wifi_running=false;}
    if(wifi_initialized){esp_wifi_deinit();wifi_initialized=false;}
    if(netif){esp_netif_destroy_default_wifi(netif);netif=NULL;}
}
esp_err_t tape_web_start(const tape_store_t *s,char ssid[33],char password[17]){
    snapshot=*s;
    esp_err_t e=esp_netif_init();if(e!=ESP_OK && e!=ESP_ERR_INVALID_STATE)return e;
    e=esp_event_loop_create_default();if(e!=ESP_OK && e!=ESP_ERR_INVALID_STATE)return e;
    netif=esp_netif_create_default_wifi_ap();if(!netif)return ESP_ERR_NO_MEM;
    wifi_init_config_t init=WIFI_INIT_CONFIG_DEFAULT();
    e=esp_wifi_init(&init);if(e!=ESP_OK)goto fail;wifi_initialized=true;
    e=esp_wifi_set_storage(WIFI_STORAGE_RAM);if(e!=ESP_OK)goto fail;
    uint8_t mac[6];e=esp_read_mac(mac,ESP_MAC_WIFI_SOFTAP);if(e!=ESP_OK)goto fail;
    snprintf(ssid,33,"iDea-Tape-%02X%02X",mac[4],mac[5]);
    snprintf(password,17,"%08lu",(unsigned long)(10000000+esp_random()%90000000));
    wifi_config_t config={0};strcpy((char*)config.ap.ssid,ssid);strcpy((char*)config.ap.password,password);
    config.ap.ssid_len=strlen(ssid);config.ap.channel=6;config.ap.max_connection=1;config.ap.authmode=WIFI_AUTH_WPA2_PSK;
    e=esp_wifi_set_mode(WIFI_MODE_AP);if(e!=ESP_OK)goto fail;
    e=esp_wifi_set_config(WIFI_IF_AP,&config);if(e!=ESP_OK)goto fail;
    e=esp_wifi_start();if(e!=ESP_OK)goto fail;wifi_running=true;
    httpd_config_t c=HTTPD_DEFAULT_CONFIG();c.stack_size=6144;c.max_open_sockets=2;c.backlog_conn=2;c.lru_purge_enable=true;c.recv_wait_timeout=2;c.send_wait_timeout=2;c.uri_match_fn=httpd_uri_match_wildcard;
    e=httpd_start(&server,&c);if(e!=ESP_OK)goto fail;
    httpd_uri_t routes[]={
        {.uri="/",.method=HTTP_GET,.handler=index_page},
        {.uri="/api/tapes",.method=HTTP_GET,.handler=listing},
        {.uri="/audio/*",.method=HTTP_GET,.handler=audio}
    };
    for(unsigned i=0;i<3;i++){e=httpd_register_uri_handler(server,&routes[i]);if(e!=ESP_OK)goto fail;}
    return ESP_OK;
fail:tape_web_stop();return e;
}

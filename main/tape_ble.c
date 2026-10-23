#include "tape_ble.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
#include "host/ble_hs.h"
#include "host/ble_gap.h"
#include "host/util/util.h"
#include "nimble/nimble_port.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"
#include "store/config/ble_store_config.h"
#include "esp_random.h"
#include <stdatomic.h>
#include <string.h>
void ble_store_config_init(void);
static const ble_uuid128_t service=BLE_UUID128_INIT(0x9e,0xca,0xdc,0x24,0x0e,0xe5,0xa9,0xe0,0x93,0xf3,0xa3,0xb5,0x01,0x00,0x40,0x6e);
static const ble_uuid128_t rx_uuid=BLE_UUID128_INIT(0x9e,0xca,0xdc,0x24,0x0e,0xe5,0xa9,0xe0,0x93,0xf3,0xa3,0xb5,0x02,0x00,0x40,0x6e);
static const ble_uuid128_t tx_uuid=BLE_UUID128_INIT(0x9e,0xca,0xdc,0x24,0x0e,0xe5,0xa9,0xe0,0x93,0xf3,0xa3,0xb5,0x03,0x00,0x40,0x6e);
static QueueHandle_t incoming,outgoing;
static SemaphoreHandle_t stopped,radio;
static TaskHandle_t host,sender_task;
static bool initialized,stop_requested;
static atomic_uint generation;
static atomic_bool active,ready;
static atomic_uint connection=BLE_HS_CONN_HANDLE_NONE,passkey;
static uint16_t tx_handle;
static uint8_t addr_type;
static bool subscribed,encrypted;
static tape_parser_t parser;
typedef struct {unsigned generation;uint16_t n;uint8_t bytes[TAPE_WIRE_MAX];} outgoing_t;
static void receive(const tape_packet_t *p,void *arg){(void)arg;if(atomic_load(&ready))xQueueSend(incoming,p,0);}
static int access_value(uint16_t c,uint16_t attr,struct ble_gatt_access_ctxt *ctxt,void *arg){
 (void)c;(void)attr;(void)arg;
 if(ctxt->op!=BLE_GATT_ACCESS_OP_WRITE_CHR)return BLE_ATT_ERR_READ_NOT_PERMITTED;
 if(!encrypted)return BLE_ATT_ERR_INSUFFICIENT_AUTHEN;
 uint16_t n=OS_MBUF_PKTLEN(ctxt->om);uint8_t b[512];
 if(n>sizeof(b)||ble_hs_mbuf_to_flat(ctxt->om,b,sizeof(b),NULL))return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
 tape_parse(&parser,b,n,receive,NULL);return 0;
}
static const struct ble_gatt_svc_def services[]={
 {.type=BLE_GATT_SVC_TYPE_PRIMARY,.uuid=&service.u,.characteristics=(struct ble_gatt_chr_def[]){
  {.uuid=&rx_uuid.u,.access_cb=access_value,.flags=BLE_GATT_CHR_F_WRITE|BLE_GATT_CHR_F_WRITE_ENC|BLE_GATT_CHR_F_WRITE_AUTHEN},
  {.uuid=&tx_uuid.u,.access_cb=access_value,.flags=BLE_GATT_CHR_F_NOTIFY,.val_handle=&tx_handle},
  {0}}},{0}};
static int gap(struct ble_gap_event *event,void *arg);
static int advertise(void){
 if(!atomic_load(&active))return 0;
 struct ble_hs_adv_fields f={0};f.flags=BLE_HS_ADV_F_DISC_GEN|BLE_HS_ADV_F_BREDR_UNSUP;
 f.uuids128=(ble_uuid128_t*)&service;f.num_uuids128=1;f.uuids128_is_complete=1;
 int rc=ble_gap_adv_set_fields(&f);if(rc)return rc;
 struct ble_hs_adv_fields scan={0};scan.name=(const uint8_t*)"iDea Tape";scan.name_len=9;scan.name_is_complete=1;
 rc=ble_gap_adv_rsp_set_fields(&scan);if(rc)return rc;
 struct ble_gap_adv_params p={.conn_mode=BLE_GAP_CONN_MODE_UND,.disc_mode=BLE_GAP_DISC_MODE_GEN};
 return ble_gap_adv_start(addr_type,NULL,BLE_HS_FOREVER,&p,gap,NULL);
}
static int gap(struct ble_gap_event *e,void *arg){
 (void)arg;
 switch(e->type){
 case BLE_GAP_EVENT_CONNECT:
  if(e->connect.status){advertise();break;}
  atomic_fetch_add(&generation,1);atomic_store(&connection,e->connect.conn_handle);subscribed=false;encrypted=false;parser.used=0;
  ble_gap_security_initiate(e->connect.conn_handle);break;
 case BLE_GAP_EVENT_DISCONNECT:
  atomic_fetch_add(&generation,1);xQueueReset(outgoing);xQueueReset(incoming);atomic_store(&ready,false);atomic_store(&connection,BLE_HS_CONN_HANDLE_NONE);atomic_store(&passkey,0);subscribed=false;encrypted=false;parser.used=0;advertise();break;
 case BLE_GAP_EVENT_SUBSCRIBE:
  if(e->subscribe.attr_handle==tx_handle)subscribed=e->subscribe.cur_notify;
  atomic_store(&ready,subscribed&&encrypted);break;
 case BLE_GAP_EVENT_ENC_CHANGE:{
  struct ble_gap_conn_desc d;
  if(!e->enc_change.status && !ble_gap_conn_find(e->enc_change.conn_handle,&d))encrypted=d.sec_state.encrypted&&d.sec_state.authenticated;
  else encrypted=false;
  atomic_store(&ready,subscribed&&encrypted);if(encrypted)atomic_store(&passkey,0);break;}
 case BLE_GAP_EVENT_PASSKEY_ACTION:
  if(e->passkey.params.action==BLE_SM_IOACT_DISP){
   unsigned code=100000+esp_random()%900000;atomic_store(&passkey,code);
   struct ble_sm_io io={.action=BLE_SM_IOACT_DISP,.passkey=code};ble_sm_inject_io(e->passkey.conn_handle,&io);
  }break;
 case BLE_GAP_EVENT_REPEAT_PAIRING:{
  struct ble_gap_conn_desc d;if(!ble_gap_conn_find(e->repeat_pairing.conn_handle,&d))ble_store_util_delete_peer(&d.peer_id_addr);
  return BLE_GAP_REPEAT_PAIRING_RETRY;}
 case BLE_GAP_EVENT_ADV_COMPLETE:advertise();break;
 default:break;
 }
 return 0;
}
static void sync_host(void){if(!ble_hs_util_ensure_addr(0)&&!ble_hs_id_infer_auto(0,&addr_type))advertise();}
static void reset_host(int reason){(void)reason;atomic_store(&ready,false);atomic_store(&connection,BLE_HS_CONN_HANDLE_NONE);}
static void run_host(void *arg){(void)arg;nimble_port_run();xSemaphoreGive(stopped);for(;;)vTaskSuspend(NULL);}
static void sender(void *arg){
 (void)arg;outgoing_t item;
 for(;;){
  if(xQueueReceive(outgoing,&item,portMAX_DELAY)!=pdTRUE)continue;
  if(item.generation!=atomic_load(&generation))continue;
  size_t off=0;unsigned attempts=0;uint16_t expected=(uint16_t)atomic_load(&connection);
  while(off<item.n && atomic_load(&ready) && expected==atomic_load(&connection) && item.generation==atomic_load(&generation)){
   xSemaphoreTake(radio,portMAX_DELAY);
   if(!atomic_load(&active)||!atomic_load(&ready)){xSemaphoreGive(radio);break;}
   unsigned mtu=ble_att_mtu(expected);unsigned n=mtu>3?mtu-3:20;if(n>item.n-off)n=item.n-off;
   struct os_mbuf *om=ble_hs_mbuf_from_flat(item.bytes+off,n);
   int rc=om?ble_gatts_notify_custom(expected,tx_handle,om):BLE_HS_ENOMEM;
   xSemaphoreGive(radio);
   if(!rc){off+=n;attempts=0;}else if(++attempts>30)break;
   vTaskDelay(pdMS_TO_TICKS(2));
  }
 }
}
esp_err_t tape_ble_start(void){
 if(atomic_load(&active))return ESP_OK;
 if(initialized)return ESP_ERR_INVALID_STATE;
 if(!incoming){
  incoming=xQueueCreate(8,sizeof(tape_packet_t));outgoing=xQueueCreate(24,sizeof(outgoing_t));stopped=xSemaphoreCreateBinary();radio=xSemaphoreCreateMutex();
  if(!incoming||!outgoing||!stopped||!radio){
   if(incoming)vQueueDelete(incoming);
   if(outgoing)vQueueDelete(outgoing);
   if(stopped)vSemaphoreDelete(stopped);
   if(radio)vSemaphoreDelete(radio);
   incoming=outgoing=NULL;stopped=radio=NULL;return ESP_ERR_NO_MEM;
  }
 }
 if(!sender_task && xTaskCreate(sender,"tape_ble_tx",3072,NULL,5,&sender_task)!=pdPASS)return ESP_ERR_NO_MEM;
 xQueueReset(incoming);xQueueReset(outgoing);xSemaphoreTake(stopped,0);
 esp_err_t e=nimble_port_init();if(e!=ESP_OK)return e;initialized=true;stop_requested=false;
 ble_svc_gap_init();ble_svc_gatt_init();ble_svc_gap_device_name_set("iDea Tape");
 ble_hs_cfg.reset_cb=reset_host;ble_hs_cfg.sync_cb=sync_host;
 ble_hs_cfg.sm_io_cap=BLE_HS_IO_DISPLAY_ONLY;ble_hs_cfg.sm_bonding=1;ble_hs_cfg.sm_mitm=1;ble_hs_cfg.sm_sc=1;
 ble_hs_cfg.sm_our_key_dist=BLE_SM_PAIR_KEY_DIST_ENC|BLE_SM_PAIR_KEY_DIST_ID;ble_hs_cfg.sm_their_key_dist=ble_hs_cfg.sm_our_key_dist;
 ble_hs_cfg.store_status_cb=ble_store_util_status_rr;ble_store_config_init();
 if(ble_gatts_count_cfg(services)||ble_gatts_add_svcs(services)){nimble_port_deinit();initialized=false;return ESP_FAIL;}
 atomic_store(&active,true);
 if(xTaskCreate(run_host,"tape_nimble",4096,NULL,6,&host)!=pdPASS){atomic_store(&active,false);nimble_port_deinit();initialized=false;return ESP_ERR_NO_MEM;}
 return ESP_OK;
}
bool tape_ble_stop(void){
 if(!initialized)return true;
 xSemaphoreTake(radio,portMAX_DELAY);atomic_store(&active,false);atomic_store(&ready,false);xSemaphoreGive(radio);
 if(!stop_requested){
  ble_gap_adv_stop();uint16_t c=(uint16_t)atomic_load(&connection);if(c!=BLE_HS_CONN_HANDLE_NONE)ble_gap_terminate(c,BLE_ERR_REM_USER_CONN_TERM);
  if(nimble_port_stop()!=0)return false;
  stop_requested=true;
 }
 if(xSemaphoreTake(stopped,pdMS_TO_TICKS(3000))!=pdTRUE)return false;
 vTaskDelete(host);host=NULL;nimble_port_deinit();initialized=false;stop_requested=false;atomic_store(&connection,BLE_HS_CONN_HANDLE_NONE);atomic_store(&passkey,0);return true;
}
bool tape_ble_send(uint8_t t,const void *b,size_t n){if(!atomic_load(&ready))return false;outgoing_t item;item.generation=atomic_load(&generation);item.n=(uint16_t)tape_frame(t,b,n,item.bytes);return item.n&&xQueueSend(outgoing,&item,0)==pdTRUE;}
bool tape_ble_receive(tape_packet_t *p){return incoming&&xQueueReceive(incoming,p,0)==pdTRUE;}
bool tape_ble_connected(void){return atomic_load(&ready);}
unsigned tape_ble_passkey(void){return atomic_load(&passkey);}

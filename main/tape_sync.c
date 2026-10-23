#include "tape_sync.h"
#include "tape_ble.h"
#include "esp_random.h"
#include "esp_timer.h"
#include <string.h>
static tape_meta_t recording,transfer;
static uint32_t recording_crc;
static uint64_t epoch_base,last_announce;
static FILE *reader;
static unsigned index_block,left;
static tape_sync_view_t progress;static uint64_t progress_at;
tape_sync_view_t tape_sync_view(void){return progress;}
static uint64_t ms(void){return (uint64_t)esp_timer_get_time()/1000;}
static bool send_meta(uint8_t type,const tape_meta_t *m){uint8_t b[TAPE_META_SIZE];tape_meta_pack(m,b);return tape_ble_send(type,b,sizeof(b));}
void tape_sync_init(void){(void)tape_ble_start();}
void tape_sync_cancel_transfer(void){if(reader){fclose(reader);reader=NULL;}left=0;}
void tape_sync_begin(unsigned id){
 recording=(tape_meta_t){.id=id,.token=((uint64_t)esp_random()<<32)|esp_random(),.epoch_ms=epoch_base?epoch_base+ms():0,.flags=TAPE_LIVE|TAPE_START_EVENT};
 if(!recording.token)recording.token=1;
 recording_crc=0xffffffff;tape_store_meta_save(&recording);send_meta(TP_META,&recording);recording.flags=TAPE_LIVE;
}
void tape_sync_block(const uint8_t block[256]){
 uint8_t b[272];tape_put32(b,recording.id);tape_put64(b+4,recording.token);tape_put32(b+12,recording.blocks);memcpy(b+16,block,256);
 recording_crc=tape_crc(recording_crc,block,256);recording.blocks++;recording.crc=recording_crc^0xffffffff;
 (void)tape_ble_send(TP_DATA,b,sizeof(b));
}
void tape_sync_end(bool saved){
 if(saved && recording.blocks){recording.flags=TAPE_COMPLETE;if(tape_store_meta_save(&recording))send_meta(TP_END,&recording);}
 memset(&recording,0,sizeof(recording));
}
void tape_sync_tick(tape_store_t *s,bool busy){
 if(progress.state && ms()-progress_at>6000)progress=(tape_sync_view_t){0};
 if(!tape_ble_connected()){tape_sync_cancel_transfer();if(progress.state && progress.state!=SYNC_PAUSED && progress.state!=SYNC_DONE){progress.state=SYNC_PAUSED;progress_at=ms();}return;}
 tape_packet_t p;
 while(tape_ble_receive(&p)){
  if(p.type==TP_TIME && p.size==8){uint64_t epoch=tape_get64(p.data);if(epoch>1700000000000ULL && epoch<4102444800000ULL)epoch_base=epoch-ms();continue;}
  if(p.type==TP_LIST){
   if(recording.id){send_meta(TP_META,&recording);continue;}
   if(busy){tape_ble_send(TP_ERROR,"busy",4);continue;}
   if(!s->count){tape_ble_send(TP_EMPTY,NULL,0);continue;}
   tape_meta_t meta;if(tape_store_meta_get(s->files[s->count-1].id,&meta))send_meta(TP_META,&meta);else tape_ble_send(TP_ERROR,"read",4);
  }else if(p.type==TP_PULL && p.size==20 && !busy){
   tape_meta_t meta;unsigned id=tape_get32(p.data),start=tape_get32(p.data+12),count=tape_get32(p.data+16);
   if(!count||count>16)continue;
   if(!tape_store_meta_get(id,&meta)||meta.token!=tape_get64(p.data+4)||start>=meta.blocks){tape_ble_send(TP_ERROR,"missing",7);continue;}
   tape_sync_cancel_transfer();reader=tape_store_open(id);if(!reader)continue;
   if(fseek(reader,(long)start*256,SEEK_SET)){tape_sync_cancel_transfer();continue;}
   transfer=meta;index_block=start;left=count<meta.blocks-start?count:meta.blocks-start;
  }else if(p.type==TP_PROGRESS && p.size==21){
   tape_meta_t meta;unsigned id=tape_get32(p.data);
   bool valid=recording.id==id?(meta=recording,true):tape_store_meta_get(id,&meta);
   if(valid && tape_progress_matches(&meta,p.data,p.size)){
    progress=(tape_sync_view_t){.state=(meta.flags&TAPE_LIVE)?SYNC_LIVE:p.data[20]?SYNC_SAVING:SYNC_RECEIVING,.id=id,.received=tape_get32(p.data+12),.total=meta.blocks};progress_at=ms();
   }
  }else if(p.type==TP_COMMIT && !busy && p.size==20){
   tape_meta_t meta;unsigned id=tape_get32(p.data);
   if(tape_store_meta_get(id,&meta)&&tape_commit_matches(&meta,p.data,p.size)){
    if(reader&&transfer.id==id)tape_sync_cancel_transfer();
    if(tape_store_delete(s,id)){progress=(tape_sync_view_t){.state=SYNC_DONE,.id=id,.received=meta.blocks,.total=meta.blocks};progress_at=ms();uint8_t b[12];tape_put32(b,id);tape_put64(b+4,meta.token);tape_ble_send(TP_DELETED,b,sizeof(b));}
   }
  }
 }
 if(recording.id && ms()-last_announce>1000){send_meta(TP_META,&recording);last_announce=ms();}
 if(reader && left && !busy){
  uint8_t b[272];tape_put32(b,transfer.id);tape_put64(b+4,transfer.token);tape_put32(b+12,index_block);
  if(fread(b+16,1,256,reader)!=256){tape_sync_cancel_transfer();return;}
  if(tape_ble_send(TP_DATA,b,sizeof(b))){index_block++;if(!--left)tape_sync_cancel_transfer();}
  else fseek(reader,-256,SEEK_CUR);
 }
}

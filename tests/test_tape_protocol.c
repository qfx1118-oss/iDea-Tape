#include "tape_protocol.h"
#include <assert.h>
#include <string.h>
#include <stdio.h>
static unsigned received;static tape_packet_t last;
static void got(const tape_packet_t *p,void *arg){(void)arg;received++;last=*p;}
int main(void){
 assert((tape_crc(0xffffffff,"123456789",9)^0xffffffff)==0xcbf43926);
 tape_meta_t m={.id=1,.token=0xfff0000001234567ULL,.epoch_ms=1800000000000ULL,.blocks=44,.crc=0x12345678,.flags=TAPE_COMPLETE};
 uint8_t packed[32];tape_meta_pack(&m,packed);tape_meta_t decoded;assert(tape_meta_unpack(&decoded,packed,32));assert(decoded.token==m.token && decoded.epoch_ms==m.epoch_ms);
 uint8_t ack[20];tape_put32(ack,m.id);tape_put64(ack+4,m.token);tape_put32(ack+12,m.blocks);tape_put32(ack+16,m.crc);
 assert(tape_commit_matches(&m,ack,20));ack[4]^=1;assert(!tape_commit_matches(&m,ack,20));ack[4]^=1;ack[16]^=1;assert(!tape_commit_matches(&m,ack,20));ack[16]^=1;m.flags=TAPE_LIVE;assert(!tape_commit_matches(&m,ack,20));m.flags=TAPE_COMPLETE;m.blocks=0;assert(!tape_commit_matches(&m,ack,20));
 m.blocks=44;uint8_t progress[21]={0};tape_put32(progress,m.id);tape_put64(progress+4,m.token);tape_put32(progress+12,22);tape_put32(progress+16,44);
 assert(tape_progress_matches(&m,progress,21));assert(!tape_progress_matches(&m,progress,20));progress[4]^=1;assert(!tape_progress_matches(&m,progress,21));progress[4]^=1;tape_put32(progress+12,45);assert(!tape_progress_matches(&m,progress,21));
 tape_put32(progress+12,20);tape_put32(progress+16,30);assert(!tape_progress_matches(&m,progress,21));m.flags=TAPE_LIVE;assert(tape_progress_matches(&m,progress,21));tape_put32(progress+16,45);assert(!tape_progress_matches(&m,progress,21));m.flags=TAPE_COMPLETE;
 uint8_t frame[TAPE_WIRE_MAX];size_t size=tape_frame(TP_META,packed,32,frame);assert(size==42);
 for(size_t split=0;split<=size;split++){tape_parser_t p={0};received=0;tape_parse(&p,frame,split,got,NULL);tape_parse(&p,frame+split,size-split,got,NULL);assert(received==1 && last.type==TP_META && !memcmp(last.data,packed,32));}
 tape_parser_t p={0};received=0;uint8_t junk[18]={0,'I','T',1,4,255,255};tape_parse(&p,junk,sizeof(junk),got,NULL);uint8_t broken[TAPE_WIRE_MAX];memcpy(broken,frame,size);broken[9]^=42;tape_parse(&p,broken,size,got,NULL);tape_parse(&p,frame,size,got,NULL);assert(received==1);
 uint8_t maximum[272];memset(maximum,0xa5,sizeof(maximum));size=tape_frame(TP_DATA,maximum,sizeof(maximum),frame);assert(size==TAPE_WIRE_MAX);p=(tape_parser_t){0};received=0;for(size_t i=0;i<size;i++)tape_parse(&p,frame+i,1,got,NULL);assert(received==1&&last.size==272);
 tape_place_view_t place={0};uint8_t road[]={2,9,0xe4,0xba,0xba,0xe6,0xb0,0x91,0xe8,0xb7,0xaf};
 assert(tape_place_unpack(&place,road,sizeof(road))&&place.state==2&&!strcmp(place.label,"人民路"));
 road[1]=8;assert(!tape_place_unpack(&place,road,sizeof(road)));road[1]=9;road[2]=0xc0;assert(!tape_place_unpack(&place,road,sizeof(road)));
 uint8_t disabled[]={0,0};assert(tape_place_unpack(&place,disabled,2)&&!place.label[0]);disabled[0]=2;assert(!tape_place_unpack(&place,disabled,2));
 uint8_t invalid[]={2,3,0xed,0xa0,0x80};assert(!tape_place_unpack(&place,invalid,5));invalid[2]=0xe0;invalid[3]=0x80;invalid[4]=0x80;assert(!tape_place_unpack(&place,invalid,5));invalid[2]=10;assert(!tape_place_unpack(&place,invalid,5));
 puts("BLE framing, CRC recovery, replay-safe deletion guard: PASS");
}

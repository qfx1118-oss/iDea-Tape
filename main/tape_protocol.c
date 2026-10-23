#include "tape_protocol.h"
#include <string.h>
uint32_t tape_crc(uint32_t s,const void *bytes,size_t n){const uint8_t *p=bytes;while(n--){s^=*p++;for(int i=0;i<8;i++)s=(s>>1)^(0xedb88320u & (0u-(s&1)));}return s;}
uint32_t tape_get32(const uint8_t *p){return (uint32_t)p[0]|((uint32_t)p[1]<<8)|((uint32_t)p[2]<<16)|((uint32_t)p[3]<<24);}
uint64_t tape_get64(const uint8_t *p){return (uint64_t)tape_get32(p)|((uint64_t)tape_get32(p+4)<<32);}
void tape_put32(uint8_t *p,uint32_t n){for(int i=0;i<4;i++)p[i]=(uint8_t)(n>>(8*i));}
void tape_put64(uint8_t *p,uint64_t n){tape_put32(p,(uint32_t)n);tape_put32(p+4,(uint32_t)(n>>32));}
void tape_meta_pack(const tape_meta_t *m,uint8_t p[TAPE_META_SIZE]){memset(p,0,TAPE_META_SIZE);tape_put32(p,m->id);tape_put64(p+4,m->token);tape_put64(p+12,m->epoch_ms);tape_put32(p+20,m->blocks);tape_put32(p+24,m->crc);p[28]=m->flags;}
bool tape_meta_unpack(tape_meta_t *m,const uint8_t *p,size_t n){if(n!=TAPE_META_SIZE)return false;*m=(tape_meta_t){.id=tape_get32(p),.token=tape_get64(p+4),.epoch_ms=tape_get64(p+12),.blocks=tape_get32(p+20),.crc=tape_get32(p+24),.flags=p[28]};return m->id && m->token && m->blocks<32768 && !(m->flags&~7);}
size_t tape_frame(uint8_t type,const void *payload,size_t n,uint8_t out[TAPE_WIRE_MAX]){if(n>272)return 0;out[0]='I';out[1]='T';out[2]=1;out[3]=type;out[4]=(uint8_t)n;out[5]=(uint8_t)(n>>8);if(n)memcpy(out+6,payload,n);tape_put32(out+6+n,tape_crc(0xffffffff,out,6+n)^0xffffffff);return n+10;}
void tape_parse(tape_parser_t *p,const void *bytes,size_t n,void (*receive)(const tape_packet_t *,void *),void *arg){
 const uint8_t *in=bytes;
 while(n--){
  if(p->used==sizeof(p->bytes)){memmove(p->bytes,p->bytes+1,--p->used);}
  p->bytes[p->used++]=*in++;
  while(p->used>=6){
   size_t len=(size_t)p->bytes[4]|((size_t)p->bytes[5]<<8);
   if(p->bytes[0]!='I'||p->bytes[1]!='T'||p->bytes[2]!=1||len>272){memmove(p->bytes,p->bytes+1,--p->used);continue;}
   if(p->used<len+10)break;
   if(tape_get32(p->bytes+6+len)!=(tape_crc(0xffffffff,p->bytes,6+len)^0xffffffff)){memmove(p->bytes,p->bytes+1,--p->used);continue;}
   tape_packet_t packet={.type=p->bytes[3],.size=(uint16_t)len};memcpy(packet.data,p->bytes+6,len);
   size_t consumed=len+10;memmove(p->bytes,p->bytes+consumed,p->used-consumed);p->used-=consumed;receive(&packet,arg);
  }
 }
}
bool tape_commit_matches(const tape_meta_t *m,const uint8_t *p,size_t n){return n==20 && (m->flags&TAPE_COMPLETE) && m->blocks && m->id==tape_get32(p) && m->token==tape_get64(p+4) && m->blocks==tape_get32(p+12) && m->crc==tape_get32(p+16);}

bool tape_progress_matches(const tape_meta_t *m,const uint8_t *p,size_t n){
 return n==21 && tape_get32(p)==m->id && tape_get64(p+4)==m->token &&
 tape_get32(p+12)<=tape_get32(p+16) && ((m->flags&TAPE_LIVE)?tape_get32(p+16)<=m->blocks:tape_get32(p+16)==m->blocks) && p[20]<=1;
}

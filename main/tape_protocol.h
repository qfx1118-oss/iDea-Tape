#pragma once
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#define TAPE_WIRE_MAX 282
#define TAPE_META_SIZE 32
#define TAPE_LIVE 1
#define TAPE_COMPLETE 2
#define TAPE_START_EVENT 4
enum { TP_META=1,TP_DATA=2,TP_END=3,TP_EMPTY=4,TP_DELETED=5,TP_ERROR=6,TP_LIST=16,TP_PULL=17,TP_COMMIT=18,TP_TIME=19,TP_PROGRESS=20,TP_PLACE=21 };
typedef struct { uint32_t id;uint64_t token,epoch_ms;uint32_t blocks,crc;uint8_t flags; } tape_meta_t;
typedef struct {uint8_t type;uint16_t size;uint8_t data[272];} tape_packet_t;
typedef struct {uint8_t bytes[TAPE_WIRE_MAX*2];size_t used;} tape_parser_t;
uint32_t tape_crc(uint32_t state,const void *bytes,size_t n);
uint32_t tape_get32(const uint8_t *p);
uint64_t tape_get64(const uint8_t *p);
void tape_put32(uint8_t *p,uint32_t n);
void tape_put64(uint8_t *p,uint64_t n);
void tape_meta_pack(const tape_meta_t *m,uint8_t out[TAPE_META_SIZE]);
bool tape_meta_unpack(tape_meta_t *m,const uint8_t *p,size_t n);
size_t tape_frame(uint8_t type,const void *payload,size_t n,uint8_t out[TAPE_WIRE_MAX]);
void tape_parse(tape_parser_t *p,const void *bytes,size_t n,void (*receive)(const tape_packet_t *,void *),void *arg);
bool tape_commit_matches(const tape_meta_t *m,const uint8_t *p,size_t n);

bool tape_progress_matches(const tape_meta_t *m,const uint8_t *p,size_t n);

enum { SYNC_NONE,SYNC_LIVE,SYNC_RECEIVING,SYNC_SAVING,SYNC_DONE,SYNC_PAUSED };
typedef struct {unsigned state,id,received,total;} tape_sync_view_t;

/* Optional phone-supplied current place; never persisted in firmware. */
typedef struct {unsigned state;char label[97];} tape_place_view_t;
bool tape_place_unpack(tape_place_view_t *out,const uint8_t *p,size_t n);

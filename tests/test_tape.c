#include "tape_model.h"
#include "tape_codec.h"
#include "tape_stream.h"
#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
static tape_action_t press(tape_model_t *m,tape_key_t k){return tape_key(m,k,TAPE_PRESS);}
static tape_action_t release(tape_model_t *m,tape_key_t k){return tape_key(m,k,TAPE_RELEASE);}
static tape_action_t hold(tape_model_t *m,tape_key_t k){return tape_key(m,k,TAPE_LONG);}
static void controls(void){
    tape_model_t m={0};
    assert(press(&m,TAPE_UP)==TAPE_START && m.recording && m.held_record);
    assert(hold(&m,TAPE_UP)==TAPE_NONE);
    assert(release(&m,TAPE_UP)==TAPE_STOP && !m.recording);
    assert(press(&m,TAPE_DOWN)==TAPE_START);
    assert(release(&m,TAPE_DOWN)==TAPE_NONE && m.recording);
    assert(press(&m,TAPE_OK)==TAPE_NONE);
    assert(release(&m,TAPE_OK)==TAPE_NONE && m.recording);
    assert(press(&m,TAPE_DOWN)==TAPE_STOP);
    release(&m,TAPE_DOWN);
    press(&m,TAPE_OK);release(&m,TAPE_OK);assert(m.mode==TAPE_LIBRARY);
    press(&m,TAPE_OK);assert(release(&m,TAPE_OK)==TAPE_NONE); /* Empty library. */
    m.count=3;m.selected=1;
    press(&m,TAPE_OK);assert(release(&m,TAPE_OK)==TAPE_PLAY && m.playing);
    press(&m,TAPE_DOWN);assert(m.selected==1); /* Long-delete targets the current tape. */
    assert(hold(&m,TAPE_DOWN)==TAPE_HALT && m.mode==TAPE_DELETE && !m.playing);
    assert(tape_model_storage_busy(&m)); /* Freeze deletion target against phone cache cleanup. */
    release(&m,TAPE_DOWN);assert(m.mode==TAPE_DELETE && m.selected==1);
    press(&m,TAPE_UP);release(&m,TAPE_UP);assert(m.mode==TAPE_LIBRARY && m.selected==1);assert(!tape_model_storage_busy(&m));
    press(&m,TAPE_UP);hold(&m,TAPE_UP);release(&m,TAPE_UP);
    press(&m,TAPE_OK);assert(release(&m,TAPE_OK)==TAPE_REMOVE && m.mode==TAPE_LIBRARY);
    press(&m,TAPE_OK);assert(hold(&m,TAPE_OK)==TAPE_HALT && m.mode==TAPE_RECORD);
    release(&m,TAPE_OK);assert(m.mode==TAPE_RECORD); /* No short action after a long press. */
    press(&m,TAPE_OK);assert(hold(&m,TAPE_OK)==TAPE_WIFI_ON);release(&m,TAPE_OK);
    press(&m,TAPE_UP);release(&m,TAPE_UP);assert(m.mode==TAPE_EXPORT && !m.recording);
    press(&m,TAPE_OK);assert(hold(&m,TAPE_OK)==TAPE_WIFI_OFF);release(&m,TAPE_OK);
    press(&m,TAPE_UP);tape_finished(&m);release(&m,TAPE_UP);assert(!m.recording);
    assert(release(&m,TAPE_OK)==TAPE_NONE); /* Stray release. */
}
static void mechanical(void){
    tape_motion_t m={0};assert(tape_motion_step(&m,false,100)==0);
    assert(tape_motion_step(&m,false,1000)==0); /* No phantom boot rotation. */
    tape_motion_step(&m,true,1020);
    for(unsigned t=1040;t<=2020;t+=20)tape_motion_step(&m,true,t);
    double stopped=tape_motion_step(&m,false,2040);
    double overshoot=tape_motion_step(&m,false,2340);
    double back=tape_motion_step(&m,false,2520);
    double settled=tape_motion_step(&m,false,2680);
    assert(fabs(overshoot-stopped-16)<.001 && fabs(back-stopped-12)<.001);
    assert(overshoot>back && fabs(settled-back)<.001);
    assert(tape_motion_step(&m,false,9000)==settled);
    assert(tape_motion_step(&m,true,9020)==settled); /* Restart has no pose jump. */
}
static void codec(void){
    uint8_t block[TAPE_BLOCK]={0};int16_t input[TAPE_SAMPLES]={0},output[TAPE_SAMPLES];
    tape_encode(input,block);assert(tape_decode(block,output));
    for(int i=0;i<TAPE_SAMPLES;i++)assert(output[i]==0);
    /* Independent known IMA nibble example: predictor 0, index 0, repeated +7. */
    memset(block,0x77,sizeof(block));memset(block,0,4);
    assert(tape_decode(block,output));assert(output[0]==0 && output[1]==11 && output[2]==41);
    block[2]=89;assert(!tape_decode(block,output));block[2]=0;block[3]=1;assert(!tape_decode(block,output));
    for(int i=0;i<TAPE_SAMPLES;i++)input[i]=(int16_t)(12000*sin(i*2*3.14159265*440/TAPE_RATE));
    tape_encode(input,block);assert(tape_decode(block,output));
    double signal=0,error=0;for(int i=0;i<TAPE_SAMPLES;i++){double d=output[i]-input[i];error+=d*d;signal+=(double)input[i]*input[i];}
    assert(10*log10(signal/error)>20);
    for(int i=0;i<TAPE_SAMPLES;i++)input[i]=(i%2)?32767:-32768;
    tape_encode(input,block);assert(tape_decode(block,output));
    uint8_t wav[44];tape_wav_header(wav,16000);
    assert(!memcmp(wav,"RIFF",4) && !memcmp(wav+8,"WAVEfmt ",8));
    assert(wav[20]==1 && wav[22]==1 && wav[34]==16);
    assert(wav[40]==0 && wav[41]==0x7d); /* 32000 bytes, little endian. */
}
typedef struct { size_t bytes; unsigned calls,fail_at; } stream_test_t;
static bool sink(const void *data,size_t n,void *user){
    assert(data);stream_test_t *s=user;s->calls++;if(s->fail_at && s->calls==s->fail_at)return false;s->bytes+=n;return true;
}
static void streaming(void){
    FILE *f=tmpfile();assert(f);uint8_t block[TAPE_BLOCK]={0};
    assert(fwrite(block,1,sizeof(block),f)==sizeof(block));rewind(f);
    stream_test_t s={0};assert(tape_stream(f,1,sink,&s) && s.bytes==44+TAPE_SAMPLES*2);
    rewind(f);s=(stream_test_t){0};assert(!tape_stream(f,2,sink,&s)); /* Truncated storage. */
    rewind(f);s=(stream_test_t){.fail_at=2};assert(!tape_stream(f,1,sink,&s)); /* Lost connection. */
    rewind(f);block[2]=99;assert(fwrite(block,1,sizeof(block),f)==sizeof(block));rewind(f);
    s=(stream_test_t){0};assert(!tape_stream(f,1,sink,&s));fclose(f);
}
int main(void){streaming();controls();mechanical();codec();puts("iDea Tape controls, mechanical inertia, IMA codec, WAV: PASS");}

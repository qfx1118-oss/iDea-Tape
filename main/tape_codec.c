#include "tape_codec.h"
#include <string.h>
static const int steps[89]={7,8,9,10,11,12,13,14,16,17,19,21,23,25,28,31,34,37,41,45,50,55,60,66,73,80,88,97,107,118,130,143,157,173,190,209,230,253,279,307,337,371,408,449,494,544,598,658,724,796,876,963,1060,1166,1282,1411,1552,1707,1878,2066,2272,2499,2749,3024,3327,3660,4026,4428,4871,5358,5894,6484,7132,7845,8630,9493,10442,11487,12635,13899,15289,16818,18500,20350,22385,24623,27086,29794,32767};
static const int indices[16]={-1,-1,-1,-1,2,4,6,8,-1,-1,-1,-1,2,4,6,8};
static int apply(int n,int *p,int *idx) {
    int s=steps[*idx], d=s/8;
    if(n&1)d+=s/4;
    if(n&2)d+=s/2;
    if(n&4)d+=s;
    *p+=(n&8)?-d:d;
    if(*p>32767)*p=32767;
    if(*p< -32768)*p= -32768;
    *idx+=indices[n];
    if(*idx<0)*idx=0;
    if(*idx>88)*idx=88;
    return *p;
}
void tape_encode(const int16_t *pcm,uint8_t *out) {
    int p=pcm[0],idx=0;
    int initial=pcm[1]-p;if(initial<0)initial= -initial;
    while(idx<88 && steps[idx]<initial*2)idx++;
    memset(out,0,TAPE_BLOCK); out[0]=(uint8_t)p;out[1]=(uint8_t)(p>>8);out[2]=(uint8_t)idx;
    for(int i=1;i<TAPE_SAMPLES;i++) {
        int s=steps[idx],diff=pcm[i]-p,n=0;
        if(diff<0){n=8;diff= -diff;}
        if(diff>=s){n|=4;diff-=s;}
        if(diff>=s/2){n|=2;diff-=s/2;}
        if(diff>=s/4)n|=1;
        apply(n,&p,&idx);
        out[4+(i-1)/2]|=(uint8_t)(n<<(((i-1)%2)*4));
    }
}
bool tape_decode(const uint8_t *in,int16_t *pcm) {
    if(in[2]>88 || in[3])return false;
    int p=(int16_t)((unsigned)in[0]|((unsigned)in[1]<<8)),idx=in[2];pcm[0]=(int16_t)p;
    for(int i=1;i<TAPE_SAMPLES;i++) pcm[i]=(int16_t)apply((in[4+(i-1)/2]>>(((i-1)%2)*4))&15,&p,&idx);
    return true;
}
static void le32(uint8_t *o,uint32_t v){for(int i=0;i<4;i++)o[i]=(uint8_t)(v>>(8*i));}
void tape_wav_header(uint8_t *out,uint32_t samples) {
    memset(out,0,44); memcpy(out,"RIFF",4);le32(out+4,36+samples*2);
    memcpy(out+8,"WAVEfmt ",8);le32(out+16,16);out[20]=1;out[22]=1;
    le32(out+24,TAPE_RATE);le32(out+28,TAPE_RATE*2);out[32]=2;out[34]=16;
    memcpy(out+36,"data",4);le32(out+40,samples*2);
}

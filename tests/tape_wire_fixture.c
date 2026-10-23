#include "tape_codec.h"
#include "tape_protocol.h"
#include <stdio.h>
#include <assert.h>
#include <math.h>
int main(int argc,char **argv){assert(argc==3);FILE *audio=fopen(argv[1],"wb"),*manifest=fopen(argv[2],"wb");assert(audio&&manifest);int16_t pcm[505];uint8_t b[256];uint32_t crc=0xffffffff;for(int n=0;n<4;n++){for(int i=0;i<505;i++)pcm[i]=(int16_t)(10000*sin((n*505+i)*2*3.14159265*440/16000));tape_encode(pcm,b);fwrite(b,1,256,audio);crc=tape_crc(crc,b,256);}tape_meta_t m={.id=42,.token=0xf123456789abcdefULL,.blocks=4,.crc=crc^0xffffffff,.epoch_ms=1800000000000ULL,.flags=TAPE_COMPLETE};uint8_t raw[32];tape_meta_pack(&m,raw);fwrite(raw,1,32,manifest);fclose(audio);fclose(manifest);}

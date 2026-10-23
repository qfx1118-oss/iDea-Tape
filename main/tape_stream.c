#include "tape_stream.h"
#include "tape_codec.h"
bool tape_stream(FILE *source,unsigned blocks,tape_sink_t sink,void *user){
    if(!source || !sink || blocks>0x7ffff000u/(TAPE_SAMPLES*2))return false;
    uint8_t header[44],encoded[TAPE_BLOCK];int16_t pcm[TAPE_SAMPLES];
    tape_wav_header(header,blocks*TAPE_SAMPLES);
    if(!sink(header,sizeof(header),user))return false;
    for(unsigned i=0;i<blocks;i++){
        if(fread(encoded,1,sizeof(encoded),source)!=sizeof(encoded) || !tape_decode(encoded,pcm))return false;
        if(!sink(pcm,sizeof(pcm),user))return false;
    }
    return !ferror(source);
}

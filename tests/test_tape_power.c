#include "tape_power.h"
#include <assert.h>
int main(void){
 for(unsigned key=0;key<3;key++){
  tape_power_t p={0};assert(!tape_power_tick(&p,29999,false,false));assert(tape_power_tick(&p,30000,false,false));
  assert(tape_power_key(&p,key,0,31000));assert(!p.asleep);assert(tape_power_key(&p,key,2,31500));assert(tape_power_key(&p,key,1,32000));assert(!tape_power_key(&p,key,0,33000));
 }
 tape_power_t p={0};assert(!tape_power_tick(&p,50000,true,false));assert(!tape_power_tick(&p,70000,false,false));assert(!tape_power_tick(&p,80000,false,true));assert(tape_power_tick(&p,110000,false,false));return 0;
}

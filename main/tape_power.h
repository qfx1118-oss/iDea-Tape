#pragma once
#include <stdbool.h>
#include <stdint.h>
/* Screen-only standby keeps capture, BLE and saved state alive. */
typedef struct { uint64_t activity; bool asleep; unsigned suppressed; } tape_power_t;
static inline bool tape_power_tick(tape_power_t *p,uint64_t now,bool held,bool pairing){
    if(held||pairing)p->activity=now;
    if(!p->asleep && now-p->activity>=30000){p->asleep=true;return true;}
    return false;
}
/* First wake gesture, including LONG/RELEASE, cannot perform an application action. */
static inline bool tape_power_key(tape_power_t *p,unsigned key,unsigned event,uint64_t now){
    p->activity=now;
    if(p->asleep){p->asleep=false;p->suppressed|=1u<<key;}
    bool consume=(p->suppressed&(1u<<key))!=0;
    if(event==1)p->suppressed&=~(1u<<key);
    return consume;
}

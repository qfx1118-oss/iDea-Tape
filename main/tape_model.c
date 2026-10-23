#include "tape_model.h"
#include <math.h>

tape_action_t tape_key(tape_model_t *m, tape_key_t k, tape_event_t e) {
    if ((unsigned)k > TAPE_OK) return TAPE_NONE;
    if (e == TAPE_PRESS) {
        if (m->down[k]) return TAPE_NONE;
        m->down[k] = true; m->long_seen[k] = false;
        if (m->mode == TAPE_RECORD && k != TAPE_OK) {
            if (m->recording) {
                if (k == TAPE_DOWN && !m->held_record) { tape_finished(m); return TAPE_STOP; }
                return TAPE_NONE;
            }
            m->recording = true; m->held_record = k == TAPE_UP;
            return TAPE_START;
        }
        return TAPE_NONE;
    }
    if (!m->down[k]) return TAPE_NONE;
    if (e == TAPE_LONG) {
        if (m->long_seen[k]) return TAPE_NONE;
        m->long_seen[k] = true;
        if (k == TAPE_OK) {
            if (m->recording) return TAPE_NONE;
            if (m->mode == TAPE_EXPORT) { m->mode=TAPE_RECORD; return TAPE_WIFI_OFF; }
            if (m->mode == TAPE_RECORD) { m->mode=TAPE_EXPORT; return TAPE_WIFI_ON; }
            m->mode=TAPE_RECORD; m->playing=false; return TAPE_HALT;
        }
        if (m->mode == TAPE_LIBRARY && m->count) {
            m->mode=TAPE_DELETE; m->playing=false; return TAPE_HALT;
        }
        return TAPE_NONE;
    }
    if (e != TAPE_RELEASE) return TAPE_NONE;
    m->down[k]=false;
    if (m->recording && m->held_record && k == TAPE_UP) { tape_finished(m); return TAPE_STOP; }
    if (m->long_seen[k]) return TAPE_NONE;
    if (m->recording) return TAPE_NONE;
    if (m->mode == TAPE_EXPORT) return TAPE_NONE;
    if (m->mode == TAPE_DELETE) {
        m->mode=TAPE_LIBRARY;
        return k == TAPE_OK ? TAPE_REMOVE : TAPE_NONE;
    }
    if (m->mode == TAPE_RECORD) {
        if (k == TAPE_OK) m->mode=TAPE_LIBRARY;
        return TAPE_NONE;
    }
    if (k == TAPE_OK) {
        if (!m->count) return TAPE_NONE;
        m->playing=!m->playing; return m->playing ? TAPE_PLAY : TAPE_HALT;
    }
    m->playing=false;
    if (m->count) m->selected = k == TAPE_UP ? (m->selected+m->count-1)%m->count : (m->selected+1)%m->count;
    return TAPE_HALT;
}
void tape_finished(tape_model_t *m) { m->recording=false; m->playing=false; m->held_record=false; }
double tape_motion_step(tape_motion_t *m, bool moving, uint64_t now) {
    if (!m->initialized) { m->initialized=true; m->tick=now; m->changed=now; m->moving=moving; }
    if (moving != m->moving) {
        /* Preserve the exact pose when a stop settles or is interrupted. */
        m->stop_angle=m->angle; m->moving=moving; m->coasting=!moving; m->changed=now;
    }
    double dt=(double)(now-m->tick)/1000.0;
    double age=(double)(now-m->changed);
    if (moving) {
        double ramp=age<180 ? age/180.0 : 1.0;
        m->angle += 210.0*ramp*dt;
    } else {
        /* 0..300 ms: coast +16 degrees; 300..480: return 4 degrees;
           480..640: damped final 0.7-degree take-up. No audio tail. */
        double offset;
        if (age<300) { double t=age/300; offset=16*(1-pow(1-t,3)); }
        else if (age<480) { double t=(age-300)/180; offset=16-4*(t*t*(3-2*t)); }
        else if (age<640) { double t=(age-480)/160; offset=12+0.7*sin(t*6.283185307)*(1-t); }
        else offset=12;
        if (m->coasting) m->angle=m->stop_angle+offset;
    }
    m->tick=now;
    return m->angle;
}

bool tape_model_storage_busy(const tape_model_t *m){return m->recording||m->playing||m->mode==TAPE_DELETE||m->mode==TAPE_EXPORT;}

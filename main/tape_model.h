#pragma once
#include <stdbool.h>
#include <stdint.h>
typedef enum { TAPE_RECORD, TAPE_LIBRARY, TAPE_DELETE, TAPE_EXPORT } tape_mode_t;
typedef enum { TAPE_NONE, TAPE_START, TAPE_STOP, TAPE_PLAY, TAPE_HALT, TAPE_REMOVE, TAPE_WIFI_ON, TAPE_WIFI_OFF } tape_action_t;
typedef enum { TAPE_UP, TAPE_DOWN, TAPE_OK } tape_key_t;
typedef enum { TAPE_PRESS, TAPE_RELEASE, TAPE_LONG } tape_event_t;
typedef struct {
    tape_mode_t mode;
    bool recording, playing, held_record;
    bool down[3], long_seen[3];
    unsigned count, selected;
} tape_model_t;
tape_action_t tape_key(tape_model_t *m, tape_key_t k, tape_event_t e);
void tape_finished(tape_model_t *m);
/* Motion uses elapsed milliseconds, independent of LVGL or the audio task. */
typedef struct { double angle, stop_angle; uint64_t changed, tick; bool moving, initialized, coasting; } tape_motion_t;
double tape_motion_step(tape_motion_t *m, bool moving, uint64_t now);

bool tape_model_storage_busy(const tape_model_t *m);

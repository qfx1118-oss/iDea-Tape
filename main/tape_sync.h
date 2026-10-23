#pragma once
#include "tape_store.h"
void tape_sync_init(void);
void tape_sync_begin(unsigned id);
void tape_sync_block(const uint8_t block[256]);
void tape_sync_end(bool saved);
void tape_sync_tick(tape_store_t *store,bool busy);
void tape_sync_cancel_transfer(void);

tape_sync_view_t tape_sync_view(void);

tape_place_view_t tape_sync_place(void);

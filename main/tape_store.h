#pragma once
#include <stdint.h>
#include <stdio.h>
#include <stdbool.h>
#include "esp_err.h"
#include "tape_protocol.h"
#define TAPE_FILES 64
typedef struct { unsigned id, blocks; } tape_file_t;
typedef struct { tape_file_t files[TAPE_FILES]; unsigned count, next; size_t free_bytes; bool ready; } tape_store_t;
esp_err_t tape_store_init(tape_store_t *s);
void tape_store_scan(tape_store_t *s);
FILE *tape_store_begin(tape_store_t *s, unsigned *id);
bool tape_store_finish(tape_store_t *s, FILE *f, unsigned id);
bool tape_store_delete(tape_store_t *s,unsigned id);
FILE *tape_store_open(unsigned id);

bool tape_store_meta_get(unsigned id,tape_meta_t *meta);
bool tape_store_meta_save(const tape_meta_t *meta);

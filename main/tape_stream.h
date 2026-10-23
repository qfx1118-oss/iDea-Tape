#pragma once
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
typedef bool (*tape_sink_t)(const void *data,size_t length,void *user);
/* Emits a standard PCM WAV without buffering the complete recording. */
bool tape_stream(FILE *source,unsigned blocks,tape_sink_t sink,void *user);

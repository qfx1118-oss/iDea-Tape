#pragma once
#include <stdint.h>
#include <stdbool.h>
#define TAPE_BLOCK 256
#define TAPE_SAMPLES 505
#define TAPE_RATE 16000
void tape_encode(const int16_t *pcm, uint8_t *out);
bool tape_decode(const uint8_t *in, int16_t *pcm);
void tape_wav_header(uint8_t *out, uint32_t samples);

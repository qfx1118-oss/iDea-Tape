#pragma once
#include "tape_store.h"
esp_err_t tape_web_start(const tape_store_t *s, char ssid[33], char password[17]);
void tape_web_stop(void);

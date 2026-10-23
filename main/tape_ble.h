#pragma once
#include "esp_err.h"
#include "tape_protocol.h"
esp_err_t tape_ble_start(void);
bool tape_ble_stop(void);
bool tape_ble_send(uint8_t type,const void *bytes,size_t length);
bool tape_ble_receive(tape_packet_t *packet);
bool tape_ble_connected(void);
unsigned tape_ble_passkey(void);

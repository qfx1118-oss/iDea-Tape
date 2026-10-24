idf_component_register(
    SRCS "idea_main.c" "tape_model.c" "tape_protocol.c" "tape_ble.c" "tape_sync.c" "tape_codec.c" "tape_stream.c" "tape_store.c" "tape_web.c" "tape_ui.c"
         "../assets/fonts/tape_font_12.c" "../assets/fonts/tape_font_16.c" "../assets/fonts/tape_font_30.c" "../assets/fonts/tape_brand_24.c" "../assets/fonts/tape_brand_38.c"
    INCLUDE_DIRS "."
    REQUIRES bt bsp esp_timer nvs_flash spiffs esp_partition esp_wifi esp_event esp_netif esp_http_server
    EMBED_FILES "tape_page.html"
)

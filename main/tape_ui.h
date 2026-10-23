#pragma once
#include "tape_model.h"
#include "lvgl.h"
#include "tape_protocol.h"
typedef struct {
    tape_model_t model;
    unsigned id, samples, duration, remaining, peak;
    int battery;
    bool phone;
    tape_sync_view_t sync;
    char message[80], ssid[33], password[17];
    unsigned revision;
} tape_view_t;
void tape_ui_create(void);
void tape_ui_render(const tape_view_t *view, uint64_t now);
/* Every product string lives here so font coverage can be checked deterministically. */
#define TAPE_UI_TEXT "iDea Tape 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-.:/%? "     "录音待机按住上键录松开保存下键开始再按结束短按选择音频长按电脑导出录音中正在保存播放已停止没有录音先记下一个想法上下选择播放停止返回删除这段确认取消存储不可用空间不足已满麦克风失败音频读取写入连接热点密码浏览器打开退出导出剩余分钟条准备设备错误操作太快请重试录制过短保存失败存储检查中按键不可用电量低恢复未完成的文件正常输入"

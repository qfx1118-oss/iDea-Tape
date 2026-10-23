#include "tape_store.h"
#include "tape_codec.h"
#include "esp_spiffs.h"
#include "esp_partition.h"
#include <dirent.h>
#include "nvs.h"
#include "esp_random.h"
#include <sys/stat.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
static void path(char *out,size_t n,unsigned id,const char *ext){snprintf(out,n,"/tapes/%08u.%s",id,ext);}
static bool file_id(const char *n,unsigned *id,bool *partial){
    if(strlen(n)!=12 || (strcmp(n+8,".itp") && strcmp(n+8,".tmp")))return false;
    unsigned v=0;for(int i=0;i<8;i++){if(n[i]<'0'||n[i]>'9')return false;v=v*10+(n[i]-'0');}
    if(!v)return false;
    *id=v;*partial=!strcmp(n+8,".tmp");return true;
}
static int order(const void *a,const void *b){unsigned x=((const tape_file_t*)a)->id,y=((const tape_file_t*)b)->id;return x<y?1:x>y?-1:0;}
void tape_store_scan(tape_store_t *s){
    s->count=0;if(!s->next)s->next=1;
    DIR *dir=opendir("/tapes");if(!dir)return;
    struct dirent *d;
    while((d=readdir(dir))){
        unsigned id;bool partial;
        if(!file_id(d->d_name,&id,&partial))continue;
        if(id>=s->next)s->next=id+1;
        char src[48],dst[48];path(src,sizeof(src),id,partial?"tmp":"itp");
        struct stat st;if(stat(src,&st)||st.st_size<TAPE_BLOCK)continue;
        unsigned blocks=(unsigned)st.st_size/TAPE_BLOCK;
        /* Interrupted final blocks are ignored; complete blocks are self-contained. */
        if(partial){path(dst,sizeof(dst),id,"itp");if(access(dst,F_OK)==0 || rename(src,dst))continue;}
        if(s->count<TAPE_FILES)s->files[s->count++]=(tape_file_t){id,blocks};
    }
    closedir(dir);qsort(s->files,s->count,sizeof(s->files[0]),order);
    size_t total=0,used=0;
    if(esp_spiffs_info("tapes",&total,&used)==ESP_OK)s->free_bytes=total*3/4>used?total*3/4-used:0;
}
esp_err_t tape_store_init(tape_store_t *s){
    memset(s,0,sizeof(*s));
    esp_vfs_spiffs_conf_t c={.base_path="/tapes",.partition_label="tapes",.max_files=4,.format_if_mount_failed=false};
    esp_err_t e=esp_vfs_spiffs_register(&c);
    if(e!=ESP_OK){
        /* Format only a completely erased new partition, never an unrecognized filesystem. */
        const esp_partition_t *p=esp_partition_find_first(ESP_PARTITION_TYPE_DATA,ESP_PARTITION_SUBTYPE_DATA_SPIFFS,"tapes");
        if(!p)return ESP_ERR_NOT_FOUND;
        uint8_t b[256];bool blank=true;
        for(size_t off=0;blank && off<p->size;off+=sizeof(b)){
            if(esp_partition_read(p,off,b,sizeof(b))!=ESP_OK)return ESP_FAIL;
            for(size_t i=0;i<sizeof(b);i++)if(b[i]!=255){blank=false;break;}
        }
        if(!blank)return e;
        if(esp_spiffs_format("tapes")!=ESP_OK)return ESP_FAIL;
        e=esp_vfs_spiffs_register(&c);
    }
    if(e!=ESP_OK)return e;
    s->ready=true;tape_store_scan(s);return ESP_OK;
}
FILE *tape_store_begin(tape_store_t *s,unsigned *id){
    tape_store_scan(s);
    if(!s->ready || s->count>=TAPE_FILES || s->free_bytes<262144 || s->next>99999999)return NULL;
    nvs_handle_t h;
    if(nvs_open("idea_tape",NVS_READWRITE,&h)!=ESP_OK)return NULL;
    uint32_t next=1;(void)nvs_get_u32(h,"next_id",&next);
    if(next>s->next)s->next=next;
    if(s->next>99999999){nvs_close(h);return NULL;}
    *id=s->next++;
    esp_err_t e=nvs_set_u32(h,"next_id",s->next);if(e==ESP_OK)e=nvs_commit(h);nvs_close(h);
    if(e!=ESP_OK)return NULL;
    char name[48];path(name,sizeof(name),*id,"tmp");return fopen(name,"wb");
}
bool tape_store_finish(tape_store_t *s,FILE *f,unsigned id){
    bool ok=fflush(f)==0;
    if(fsync(fileno(f)))ok=false;
    if(fclose(f))ok=false;
    char src[48],dst[48];path(src,sizeof(src),id,"tmp");path(dst,sizeof(dst),id,"itp");
    struct stat st;
    if(stat(src,&st)==0 && st.st_size>=TAPE_BLOCK){if(ok && rename(src,dst))ok=false;}
    else {unlink(src);ok=false;}
    tape_store_scan(s);return ok;
}
bool tape_store_delete(tape_store_t *s,unsigned id){char name[48];path(name,sizeof(name),id,"itp");bool ok=unlink(name)==0;if(ok){path(name,sizeof(name),id,"meta");unlink(name);}tape_store_scan(s);return ok;}
FILE *tape_store_open(unsigned id){char name[48];path(name,sizeof(name),id,"itp");return fopen(name,"rb");}

bool tape_store_meta_save(const tape_meta_t *m){
 char name[48];path(name,sizeof(name),m->id,"meta");FILE *f=fopen(name,"wb");if(!f)return false;
 uint8_t raw[TAPE_META_SIZE];tape_meta_pack(m,raw);bool ok=fwrite(raw,1,sizeof(raw),f)==sizeof(raw);
 if(fflush(f) || fsync(fileno(f)))ok=false;
 if(fclose(f))ok=false;
 return ok;
}
bool tape_store_meta_get(unsigned id,tape_meta_t *m){
 char name[48];path(name,sizeof(name),id,"itp");struct stat st;if(stat(name,&st)||st.st_size<256)return false;
 unsigned blocks=(unsigned)st.st_size/256;path(name,sizeof(name),id,"meta");FILE *f=fopen(name,"rb");
 uint8_t raw[TAPE_META_SIZE];bool valid=false,known=false;
 if(f){known=fread(raw,1,sizeof(raw),f)==sizeof(raw) && tape_meta_unpack(m,raw,sizeof(raw)) && m->id==id;valid=known && m->blocks==blocks && (m->flags&TAPE_COMPLETE);fclose(f);}
 if(valid)return true;
 if(!known)*m=(tape_meta_t){.id=id,.token=((uint64_t)esp_random()<<32)|esp_random()};
 m->blocks=blocks;m->flags=TAPE_COMPLETE;
 if(!m->token)m->token=1;
 f=tape_store_open(id);if(!f)return false;uint32_t crc=0xffffffff;uint8_t data[256];
 for(unsigned i=0;i<blocks;i++){if(fread(data,1,256,f)!=256){fclose(f);return false;}crc=tape_crc(crc,data,256);}
 fclose(f);m->crc=crc^0xffffffff;return tape_store_meta_save(m);
}

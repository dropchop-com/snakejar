#include <stdio.h>
#ifndef WIN32
  #include <dlfcn.h>
#else
  #include <windows.h>
#endif
#include <stdbool.h>
#include <wchar.h>
#include <stdarg.h>
#include <time.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <errno.h>
#include <jni.h>

#ifdef _MSC_VER
    #define _CRT_SECURE_NO_WARNINGS 1
    #define restrict __restrict
#endif

#ifndef _Included_sj_utils
#define _Included_sj_utils

#define __FSRC__ (strrchr(__FILE__, '/') ? strrchr(__FILE__, '/') + 1 : __FILE__)

typedef enum {
  sj_debug = 0,
  sj_info = 1,
  sj_warning = 2,
  sj_error = 3,
  sj_fatal = 4
}sj_log_level;

void sj_wlog_level_init(const char *restrict level);
void sj_wlog_file_init(const char *restrict fname);
void sj_wlog_finalize();
void sj_wlog(JNIEnv *pEnv, sj_log_level level, const char *restrict src, const int line, const wchar_t *restrict fmt, ...);

#ifndef WIN32
#define sj_log_error(fmt, args...) sj_wlog(NULL, sj_error, __FSRC__, __LINE__, fmt, ##args)
#define sj_log_warn(fmt, args...) sj_wlog(NULL, sj_warning, __FSRC__, __LINE__, fmt, ##args)
#define sj_log_info(fmt, args...) sj_wlog(NULL, sj_info, __FSRC__, __LINE__, fmt, ##args)
#define sj_log_debug(fmt, args...) sj_wlog(NULL, sj_debug, __FSRC__, __LINE__, fmt, ##args)

#define sj_jlog_error(pEnv, fmt, args...) sj_wlog(pEnv, sj_error, __FSRC__, __LINE__, fmt, ##args)
#define sj_jlog_warn(pEnv, fmt, args...) sj_wlog(pEnv, sj_warning, __FSRC__, __LINE__, fmt, ##args)
#define sj_jlog_info(pEnv, fmt, args...) sj_wlog(pEnv, sj_info, __FSRC__, __LINE__, fmt, ##args)
#define sj_jlog_debug(pEnv, fmt, args...) sj_wlog(pEnv, sj_debug, __FSRC__, __LINE__, fmt, ##args)
#else
#define sj_log_error(fmt, ...) sj_wlog(NULL, sj_error, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#define sj_log_warn(fmt, ...) sj_wlog(NULL, sj_warning, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#define sj_log_info(fmt, ...) sj_wlog(NULL, sj_info, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#define sj_log_debug(fmt, ...) sj_wlog(NULL, sj_debug, __FSRC__, __LINE__, fmt, __VA_ARGS__)

#define sj_jlog_error(pEnv, fmt, ...) sj_wlog(pEnv, sj_error, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#define sj_jlog_warn(pEnv, fmt, ...) sj_wlog(pEnv, sj_warning, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#define sj_jlog_info(pEnv, fmt, ...) sj_wlog(pEnv, sj_info, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#define sj_jlog_debug(pEnv, fmt, ...) sj_wlog(pEnv, sj_debug, __FSRC__, __LINE__, fmt, __VA_ARGS__)
#endif

void sj_pread_line(const char *restrict command, char *restrict buffer, size_t buffer_len);

void sj_load_lib(const char *restrict lib_location);


#define IMPL_PKG "com/dropchop/snakejar/impl/"
#define CLS_NAME_INTERPRETER IMPL_PKG "EmbeddedInterpreter"
#define CLS_NAME_BYTE_BUFF "java/nio/ByteBuffer"
#define CLS_NAME_STRING "java/lang/String"
#define CLS_NAME_EXCEPTION "java/lang/Exception"
#define CLS_NAME_SYSTEM "java/lang/System"
#define CLS_NAME_THREAD "java/lang/Thread"

JavaVM* sj_get_vm();
void sj_set_vm(JavaVM *vm);
JNIEnv* sj_get_env(JavaVM *vm);

void sj_throw_error(JNIEnv *pEnv, const char *restrict fmt, ...);

bool sj_jvm_get_sys_property(const char *key, char *buffer, size_t buffer_len);
bool sj_jni_get_sys_property(JNIEnv *pEnv, const char *key, char *buffer, size_t buffer_len);

char* sj_jni_jstring_to_cstr(JNIEnv *pEnv, jstring str);
char* sj_jni_call_getter_str(JNIEnv *pEnv, const char *getter_name, jclass cls, jobject object);

char* sj_jni_get_thread_name(JNIEnv *pEnv);

jobject sj_get_interpreter_compiled_module(JNIEnv *env, jobject interp_obj, jstring module_name);

#endif

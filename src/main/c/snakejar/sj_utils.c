#include "sj_utils.h"

sj_log_level SJ_ENABLED_LOG_LEVEL = sj_error;
FILE *SJ_LOG_FILE = NULL;

const char * SJ_LOG_LEVELS[] = {
    "DEBUG",
    "INFO ",
    "WARN ",
    "ERROR",
    "FATAL"
};

void sj_wlog_level_init(const char *restrict level) {
  if (strcmp(level, "error") == 0) {
    SJ_ENABLED_LOG_LEVEL = sj_error;
  } else if (strcmp(level, "warn") == 0) {
    SJ_ENABLED_LOG_LEVEL = sj_warning;
  } else if (strcmp(level, "info") == 0) {
    SJ_ENABLED_LOG_LEVEL = sj_info;
  } else if (strcmp(level, "debug") == 0) {
    SJ_ENABLED_LOG_LEVEL = sj_debug;
  }
}

void sj_wlog_file_init(const char *restrict fname) {
  if (fname != NULL) {
    return;
  }
  SJ_LOG_FILE = fopen(fname, "w+");
  if(!SJ_LOG_FILE) {
    perror("Log file opening failed");
  }
}

void sj_wlog_finalize() {
  if (SJ_LOG_FILE) {
    fflush(SJ_LOG_FILE);
    fclose(SJ_LOG_FILE);
  }
}

void sj_wlog(JNIEnv *pEnv, sj_log_level level, const char *restrict src, const int line, const wchar_t *restrict fmt, ...) {
  FILE *appender;
  if (SJ_ENABLED_LOG_LEVEL > level) {
    return;
  }

  if (SJ_LOG_FILE == NULL) {
    if (level >= sj_warning) {
      appender = stderr;
    } else {
      appender = stdout;
    }
  }

  struct timespec ts;
  timespec_get(&ts, TIME_UTC);
  char time_buf[100];

  size_t rc = strftime(time_buf, sizeof time_buf, "%Y-%m-%d %T", localtime(&ts.tv_sec));
  snprintf(time_buf + rc, sizeof time_buf - rc, ".%06ld", ts.tv_nsec / 1000);

  va_list args;
  va_start(args, fmt);
  wchar_t buf[2048];
  int rc2 = vswprintf(buf, sizeof buf / sizeof *buf, fmt, args);
  va_end(args);

  char *tname;
  if (pEnv == NULL) {
    //pEnv = sj_get_env(NULL);
  }
  if (pEnv != NULL) {
    tname = sj_jni_get_thread_name(pEnv);
  } else {
    tname = "unkn";
  }
  if(rc2 > 0) {
    fwprintf(appender, L"SnakeJar %s [%s] [%s:%d @ %s]: %ls\n",
      time_buf, SJ_LOG_LEVELS[level], src, line, tname, buf);
  } else {
    fwprintf(appender, L"SnakeJar %s [%s] [%s:%d @ %s]: (string too long)\n",
      SJ_LOG_LEVELS[level], time_buf, src, tname, line);
  }
  if (pEnv != NULL) {
    free(tname);
  }
  fflush(appender);
}

void sj_pread_line(const char *restrict command, char * buffer, size_t buffer_len) {
  FILE *stream; size_t num_read;
  sj_log_debug(L"Invoking [%s]...", command);
  stream = popen(command, "r");
  num_read = getline(&buffer, &buffer_len, stream);
  if (buffer[buffer_len - 1] != '\0') {
    sj_log_error(L"Insufficient buffer size for command response!");
    buffer[buffer_len - 1] = '\0';
  }
  //remove possible new line
  if (buffer[num_read - 1] == '\n') {
    buffer[num_read - 1] = '\0';
  }
  pclose(stream);
  sj_log_info(L"Invoked [%s] with result [%s].", command, buffer);
}

void sj_load_lib(const char *restrict lib_location) {
  sj_log_debug(L"Loading [%s]...", lib_location);
  if (!dlopen(lib_location, RTLD_LAZY | RTLD_NOLOAD | RTLD_GLOBAL)) {
    sj_log_error(L"Error loading [%s]!", lib_location);
  }
  sj_log_info(L"Loaded [%s].", lib_location);
}

JavaVM *sj_vm;

void sj_set_vm(JavaVM *vm) {
  sj_vm = vm;
}

JavaVM* sj_get_vm() {
  return sj_vm;
}

JNIEnv* sj_get_env(JavaVM *vm) {
  if (vm == NULL) {
    vm = sj_get_vm();
  }
  JNIEnv* env;
  if ((*sj_vm)->GetEnv(sj_vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
    sj_log_error(L"Could not obtain JNI environment!");
    return NULL;
  }
  return env;
}

void sj_throw_error(JNIEnv *env, const char *restrict fmt, ...) {
  jclass ex_cls;
  size_t buffer_len = 4096;
  char buffer[buffer_len];

  if (!(ex_cls = (*env)->FindClass(env, CLS_NAME_EXCEPTION))) {
    sj_log_error(L"Could not find [%s] class!", CLS_NAME_EXCEPTION);
    return;
  }

  va_list args;
  va_start(args, fmt);
  int rc = vsnprintf(buffer, sizeof buffer / sizeof *buffer, fmt, args);
  va_end(args);
  if (buffer[buffer_len - 1] != '\0') {
    sj_log_error(L"Insufficient buffer size for sj_throw_error!");
  }
  buffer[buffer_len - 1] = '\0';

  (*env)->ThrowNew(env, ex_cls, buffer);
}

bool sj_jni_get_sys_property(JNIEnv *env, const char *key, char *buffer, size_t buffer_len) {
	jclass system;
	jmethodID get;
	jstring string;
	const char *characters;
	char *result;

	if (!(system = (*env)->FindClass(env, CLS_NAME_SYSTEM))) {
		sj_log_error(L"Could not access System class!");
		return false;
	}

	if (!(get = (*env)->GetStaticMethodID(env, system, "getProperty", "(L" CLS_NAME_STRING ";)L" CLS_NAME_STRING ";"))) {
		sj_log_error(L"Could not find System.getProperty method!");
		return false;
	}

	if (!(string = (jstring)(*env)->CallStaticObjectMethod(env, system, get, (*env)->NewStringUTF(env, key)))) {
		return false;
	}

	characters = (*env)->GetStringUTFChars(env, string, NULL);
	strncpy(buffer, characters, buffer_len);
	if (buffer[buffer_len - 1] != '\0') {
    sj_log_error(L"Insufficient buffer size for get_property [%s]!", key);
  }
	buffer[buffer_len - 1] = '\0';
	(*env)->ReleaseStringUTFChars(env, string, characters);

	return true;
}

bool sj_jvm_get_sys_property(const char *key, char *buffer, size_t buffer_len) {
  JNIEnv *env = sj_get_env(NULL);
  return sj_jni_get_sys_property(env, key, buffer, buffer_len);
}

char* sj_jni_jstring_to_cstr(JNIEnv *env, jstring str) {
  size_t buffer_len;
  const char *characters;
  char *buffer;

  if ((*env)->IsSameObject(env, str, NULL)) {//is null
    return NULL;
  }
  characters = (*env)->GetStringUTFChars(env, str, NULL);
  if (characters == NULL) {
    return NULL;
  }
  //sj_log_debug(L"Got [%s]!", characters);
  buffer_len = strlen(characters) + 1;
  buffer = (char *)calloc(buffer_len, sizeof(char));
  if (buffer == NULL) {
      sj_log_error(L"Unable to allocate buffer!");
      (*env)->ReleaseStringUTFChars(env, str, characters);
      return NULL;
  }
  strncpy(buffer, characters, buffer_len);
  if (buffer[buffer_len - 1] != '\0') {
    sj_log_error(L"Insufficient buffer size for Thread.getName!");
  }
  buffer[buffer_len - 1] = '\0';
  //
  (*env)->ReleaseStringUTFChars(env, str, characters);

  return buffer;
}

char* sj_jni_call_getter_str(JNIEnv *env, const char *getter_name, jclass cls, jobject object) {
  jstring ret;
  jmethodID getter;

  if (!(getter = (*env)->GetMethodID(env, cls, getter_name, "()L" CLS_NAME_STRING ";"))) {
    sj_log_error(L"Could not find [%s] method!", getter_name);
    return NULL;
  }

  if (!(ret = (jstring)(*env)->CallObjectMethod(env, object, getter))) {
    sj_log_error(L"Invoking [%s] method failed!", getter_name);
    return NULL;
  }

  return sj_jni_jstring_to_cstr(env, ret);
}

char* sj_jni_get_thread_name(JNIEnv *env) {
  jclass thread_cls;
  jmethodID curr_thread, get_name;
  jobject thread;
  jstring name;

  if (!(thread_cls = (*env)->FindClass(env, CLS_NAME_THREAD))) {
    sj_log_error(L"Could not access Thread class!");
    return false;
  }

  if (!(curr_thread = (*env)->GetStaticMethodID(env, thread_cls, "currentThread", "()L" CLS_NAME_THREAD ";"))) {
    sj_log_error(L"Could not find Thread.currentThread method!");
    return false;
  }

  thread = (*env)->CallStaticObjectMethod(env, thread_cls, curr_thread);
  if( thread == NULL ) {
    return false;
  }
  return sj_jni_call_getter_str(env, "getName", thread_cls, thread);
}

jobject sj_get_interpreter_compiled_module(JNIEnv *env, jobject interp_obj, jstring module_name) {
  jmethodID method_id;
  jobject module;
  jclass interp_cls;
  if (!(interp_cls = (*env)->FindClass(env, CLS_NAME_INTERPRETER))) {
    sj_log_error(L"Could not find [%s] class!", CLS_NAME_INTERPRETER);
    return NULL;
  }
  if (!(method_id = (*env)->GetMethodID(env, interp_cls, "getCompiledModule", "(L" CLS_NAME_STRING ";)L" CLS_NAME_BYTE_BUFF ";"))) {
    sj_log_error(L"Could not find [%s.%s] method!", CLS_NAME_INTERPRETER, "getCompiledModule");
    return NULL;
  }
  if (!(module = (*env)->CallObjectMethod(env, interp_obj, method_id, module_name))) {
    sj_log_error(L"Invoke failed for [%s.%s] method!", CLS_NAME_INTERPRETER, "getCompiledModule");
    return NULL;
  }
  return module;
}

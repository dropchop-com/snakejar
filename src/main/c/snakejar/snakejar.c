#include "snakejar.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  char *buffer;
  JNIEnv *env;
  size_t buffer_len = 10240;

  sj_set_vm(vm);

  buffer = (char *)calloc(buffer_len, sizeof(char));
  if (buffer == NULL) {
    sj_log_error(L"Unable to allocate buffer!");
    return JNI_VERSION_1_6;
  }

  if (sj_jvm_get_sys_property("snakejar.log.level", buffer, buffer_len)) {
    sj_wlog_level_init(buffer);
  }
  if (sj_jvm_get_sys_property("snakejar.log.file", buffer, buffer_len)) {
    sj_wlog_file_init(buffer);
  }

  sj_jlog_debug(NULL, L"Loading Python library...");

  if (!sj_jvm_get_sys_property("snakejar.pylib.location", buffer, buffer_len)) {
    sj_pread_line(SJ_PY_CMD_LIB_PATH, buffer, buffer_len);
    sj_jlog_info(NULL, L"Loading Python library [%hs] based on python command [%hs]...", buffer, SJ_PY_CMD_LIB_PATH);
    sj_load_lib(buffer);
  } else {
    sj_jlog_info(NULL, L"Loading Python library from system property [%hs]...", buffer);
    sj_load_lib(buffer);
  }
  free(buffer);

  return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
}

#define PY_CMD_EXE_PATH "python -c \"import sys; print(sys.executable)\""

static bool pre_init(JNIEnv *env) {
  char *buffer; size_t buffer_len=10240; wchar_t *restrict wbuffer;

  sj_jlog_debug(env, L"Initializing Python...");

  buffer = (char *)calloc(buffer_len, sizeof(char));
  if (buffer == NULL) {
    sj_jlog_error(env, L"Unable to allocate buffer!");
    return false;
  }
  wbuffer = (wchar_t *)calloc(buffer_len, sizeof(wchar_t));
  if (wbuffer == NULL) {
    sj_jlog_error(env, L"Unable to allocate wbuffer!");
    free(buffer);
    return false;
  }

  sj_pread_line(PY_CMD_EXE_PATH, buffer, buffer_len);
  sj_jlog_debug(env, L"Detected python path [%hs].", buffer);

  size_t ret = mbstowcs(wbuffer, buffer, buffer_len);
  if (!ret) {
    sj_jlog_error(env, L"Unable to convert to wchar_t buffer!");
    free(buffer);
    free(wbuffer);
    return false;
  }
  free(buffer);

  sj_jlog_debug(env, L"Setting python path [%ls]!", wbuffer);
  Py_SetProgramName(wbuffer);
#ifdef WIN32 // https://github.com/python/cpython/issues/78906
  _Py_SetProgramFullPath(wbuffer);
#endif
  free(wbuffer);
  wchar_t *path = Py_GetProgramFullPath();
  sj_jlog_info(env, L"Python path is set to [%ls].", path);

  return true;
}

JNIEXPORT void JNICALL Java_com_dropchop_snakejar_impl_SnakeJarEmbedded__1initialize(JNIEnv *env, jobject obj) {
  PyThreadState *tstate, *main_tstate;
  PyGILState_STATE gil;
  PyObject *module;

  if (!pre_init(env)) {
    sj_jlog_error(env, L"Could not pre-initialize Python!");
    sj_throw_error(env, "Could not pre-initialize Python!");
    return;
  }
  Py_InitializeEx(0);//initialize with no signal handlers and get GIL
  if (!cache_frequent_classes(env)) {
    sj_jlog_warn(env, L"Unable to cache frequent classes!");
  }
  if (!cache_primitive_classes(env)) {
    sj_jlog_warn(env, L"Unable to cache primitive classes!");
  }
  tstate = PyThreadState_Get();
  main_tstate = PyEval_SaveThread();
  sj_set_main_thread_state(main_tstate);
#if PY_VERSION_HEX >= 0x03090000
  sj_jlog_debug(env, L"PyThreadState main [%p::%d] current [%p::%d].",
    main_tstate,
    main_tstate ? PyThreadState_GetID(main_tstate) : "NULL",
    tstate,
    tstate ? PyThreadState_GetID(tstate) : "NULL"
  );
#else
  sj_jlog_debug(env, L"PyThreadState main [%p] current [%p].", main_tstate, tstate);
#endif
  sj_jlog_info(env, L"Initialized Python with thread state [%p].", main_tstate);
}

JNIEXPORT void JNICALL Java_com_dropchop_snakejar_impl_SnakeJarEmbedded__1destroy(JNIEnv *env, jobject obj) {
  PyGILState_STATE gil;

  sj_jlog_debug(env, L"Python cleanup start...");
  if (Py_IsInitialized()) {
    gil = PyGILState_Ensure();
    Py_FinalizeEx();
    //PyGILState_Release(gil);
  }
  sj_jlog_info(env, L"Python cleanup done.");
}

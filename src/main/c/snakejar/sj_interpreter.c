#include "sj_interpreter.h"

static void register_module(JNIEnv *env, jobject interp_obj, const char *method, jstring module_name, PyObject *pyModule) {
  jobject module;
  jclass interp_cls;
  jmethodID register_mid;
  bool err = false; int sz = 2048;
  char err_msg[2048];

  if (!err && !(module = (*env)->NewDirectByteBuffer(env, (void*) pyModule, sizeof(PyObject)))) {
    snprintf(err_msg, sz, "Could not create imported module ptr container [%s] object!", CLS_NAME_BYTE_BUFF);
    sj_jlog_error(env, L"%s", err_msg);
    err = true;
  }
  if (!err && !(interp_cls = (*env)->FindClass(env, CLS_NAME_INTERPRETER))) {
    snprintf(err_msg, sz, "Could not find [%s] class!", CLS_NAME_INTERPRETER);
    sj_jlog_error(env, L"%s", err_msg);
    err = true;
  }
  if (!err && !(register_mid = (*env)->GetMethodID(env, interp_cls, method, "(L" CLS_NAME_STRING ";L" CLS_NAME_BYTE_BUFF ";)V"))) {
    snprintf(err_msg, sz, "Could not find [%s] method in [%s] class!", method, CLS_NAME_INTERPRETER);
    sj_jlog_error(env, L"%s", err_msg);
    err = true;
  }

  if (err) {
    sj_throw_error(env, "%s", err_msg);
    return;
  }

  (*env)->CallObjectMethod(env, interp_obj, register_mid, module_name, module);
}

JNIEXPORT void JNICALL Java_com_dropchop_snakejar_impl_EmbeddedInterpreter__1compile
  (JNIEnv *env, jobject interp_obj, jstring module_name, jstring file_name, jstring module_src) {
  jclass interp_cls;
  jobject module;
  jmethodID register_mid;
  PyObject *pyModule, *pyCode;
  PyGILState_STATE gil_state;

  bool err = false; int sz = 2048;
  char err_msg[2048];

  if ((*env)->IsSameObject(env, module_name, NULL)) {//is null
    sj_throw_error(env, "Missing module name for compilation!");
    return;
  }
  if ((*env)->IsSameObject(env, file_name, NULL)) {//is null
    sj_throw_error(env, "Missing file name for compilation!");
    return;
  }
  if ((*env)->IsSameObject(env, module_src, NULL)) {//is null
    sj_throw_error(env, "Missing source for compilation!");
    return;
  }

  char *f_name = sj_jni_jstring_to_cstr(env, file_name);
  char *m_name = sj_jni_jstring_to_cstr(env, module_name);
  char *m_src = sj_jni_jstring_to_cstr(env, module_src);
  sj_jlog_debug(env, L"Compiling [%s] into module [%s]...", f_name ? f_name : "NULL", m_name ? m_name : "NULL");

  gil_state = PyGILState_Ensure();

  pyCode = Py_CompileString(m_src, f_name, Py_file_input);
  if (!err && (pyCode == NULL || PyErr_Occurred())) {
    snprintf(err_msg, sz, "Compilation of module [%s] failed!", m_name ? m_name : "NULL");
    sj_jlog_error(env, L"%s", err_msg);
    PyErr_Print();
    err = true;
  } else {
    sj_jlog_debug(env, L"Compiled module [%s].", m_name ? m_name : "NULL");
  }
  if (!err) {
    pyModule = PyImport_ExecCodeModuleEx(m_name, pyCode, f_name);
    if (!err && (pyModule == NULL || PyErr_Occurred())) {
      snprintf(err_msg, sz, "Import of compiled module [%s] failed!", m_name ? m_name : "NULL");
      PyErr_Print();
      err = true;
    } else {
      sj_jlog_debug(env, L"Imported module [%s].", m_name ? m_name : "NULL");
    }
  }

  Py_XDECREF(pyCode);
  PyGILState_Release(gil_state);
  free(f_name);
  free(m_src);
  free(m_name);

  if (err) {
    sj_throw_error(env, "%s", err_msg);
    return;
  }

  register_module(env, interp_obj, "registerCompiledModule", module_name, pyModule);
}

JNIEXPORT void JNICALL Java_com_dropchop_snakejar_impl_EmbeddedInterpreter__1free_1module
  (JNIEnv *env, jobject interp_obj, jstring module_name, jobject module) {
  char *m_name = NULL;
  PyObject *pyModule = NULL;
  PyGILState_STATE gil_state;

  if ((*env)->IsSameObject(env, module, NULL)) {//is null
    return;
  }
  //char *m_name = sj_jni_jstring_to_cstr(env, module_name);

  pyModule = (PyObject*) (*env)->GetDirectBufferAddress(env, module);
  if (pyModule != NULL) {
    sj_jlog_debug(env, L"Found module [%s][%p].", m_name ? m_name : "NULL", pyModule);
    gil_state = PyGILState_Ensure();
    Py_XDECREF(pyModule);
    PyGILState_Release(gil_state);
    sj_jlog_debug(env, L"Released module [%s][%p].", m_name ? m_name : "NULL", pyModule);
  }

  //free(m_name);
}

JNIEXPORT jobject JNICALL Java_com_dropchop_snakejar_impl_EmbeddedInterpreter__1invoke_1func
  (JNIEnv *env, jobject interp_obj, jobject module, jstring module_name, jstring func_name,
  jclass ret_type, jobject kwargs, jobjectArray args) {
  jobject ret = NULL;
  PyObject *pModule, *callable;
  PyGILState_STATE gil_state;
  size_t sz = 2048;
  char err_msg[2048];

  char *m_name = sj_jni_jstring_to_cstr(env, module_name);
  char *f_name = sj_jni_jstring_to_cstr(env, func_name);

  if ((*env)->IsSameObject(env, module, NULL)) {//is null
    snprintf(err_msg, sz, "Unable to locate module [%s]!", m_name ? m_name : "NULL");
    sj_jlog_error(env, L"%s", err_msg);
    sj_throw_error(env, err_msg);
  } else {
    sj_jlog_debug(env, L"Invoking function [m:%s]->[f:%s]...", m_name ? m_name : "NULL", f_name ? f_name : "NULL");

    pModule = (PyObject*) (*env)->GetDirectBufferAddress(env, module);
    sj_jlog_debug(env, L"Found module [%s][%p].", m_name ? m_name : "NULL", pModule);

    gil_state = PyGILState_Ensure();

    sj_jlog_debug(env, L"Looking for [%s].", f_name ? f_name : "NULL");
    callable = PyObject_GetAttrString(pModule, f_name);
    if (callable == NULL || PyErr_Occurred()) {
      if (callable == NULL) {
        snprintf(err_msg, sz, "Unable to locate module [%s] function [%s]!", m_name ? m_name : "NULL", f_name ? f_name : "NULL");
        sj_jlog_error(env, L"%s", err_msg);
      }
      if (PyErr_Occurred()) {
        PyErr_Print();
      }
      sj_throw_error(env, err_msg);
    } else {
      sj_jlog_debug(env, L"Found function [%s][%p].", f_name ? f_name : "NULL", callable);
      ret = sj_invoke_as(env, callable, args, kwargs, ret_type);
      if (PyErr_Occurred()) {
        process_py_exception(env);
      } else {
        sj_jlog_info(env, L"Invoked function [m:%s]->[f:%s]",m_name ? m_name : "NULL", f_name ? f_name : "NULL");
      }
      Py_DECREF(callable);
    }

    PyGILState_Release(gil_state);
  }

  free(m_name);
  free(f_name);

  return ret;
}

JNIEXPORT jobject JNICALL Java_com_dropchop_snakejar_impl_EmbeddedInterpreter__1invoke_1class
  (JNIEnv *env, jobject interp_obj, jobject module, jstring module_name, jstring class_name, jstring func_name,
  jclass ret_type, jobject kwargs, jobjectArray args) {
  jobject ret = NULL;
    PyObject *pModule, *callable_cls, *callable;
    PyGILState_STATE gil_state;
    size_t sz = 2048;
    char err_msg[2048];

    char *m_name = sj_jni_jstring_to_cstr(env, module_name);
    char *c_name = sj_jni_jstring_to_cstr(env, class_name);
    char *f_name = sj_jni_jstring_to_cstr(env, func_name);

    if ((*env)->IsSameObject(env, module, NULL)) {//is null
      snprintf(err_msg, sz, "Unable to locate module [%s]!", m_name ? m_name : "NULL");
      sj_jlog_error(env, L"%s", err_msg);
      sj_throw_error(env, err_msg);
    } else {
      pModule = (PyObject*) (*env)->GetDirectBufferAddress(env, module);
      sj_jlog_debug(env, L"Found module [%s][%p].", m_name ? m_name : "NULL", pModule);

      gil_state = PyGILState_Ensure();

      sj_jlog_debug(env, L"Looking for class [%s].", c_name ? c_name : "NULL");
      callable_cls = PyObject_GetAttrString(pModule, c_name);
      if (callable_cls == NULL || PyErr_Occurred()) {
        if (callable_cls == NULL) {
          snprintf(err_msg, sz, "Unable to locate module [%s] class [%s]!", m_name ? m_name : "NULL", c_name ? c_name : "NULL");
          sj_jlog_error(env, L"%s", err_msg);
        }
        if (PyErr_Occurred()) {
          PyErr_Print();
        }
        sj_throw_error(env, err_msg);
      } else {
        sj_jlog_debug(env, L"Found class [%s][%p].", c_name ? c_name : "NULL", callable_cls);
        callable = PyObject_GetAttrString(callable_cls, f_name);
        if (callable == NULL || PyErr_Occurred()) {
          if (callable == NULL) {
            snprintf(err_msg, sz, "Unable to locate module [%s] class [%s] function [%s]!",
                                   m_name ? m_name : "NULL", c_name ? c_name : "NULL", f_name ? f_name : "NULL");
            sj_jlog_error(env, L"%s", err_msg);
          }
          if (PyErr_Occurred()) {
            PyErr_Print();
          }
          sj_throw_error(env, err_msg);
        } else {
          sj_jlog_debug(env, L"Found class [%s][%p] function [%s][%p].",
                               c_name ? c_name : "NULL", callable_cls, f_name ? f_name : "NULL", callable);
          ret = sj_invoke_as(env, callable, args, kwargs, ret_type);
          if (PyErr_Occurred()) {
            process_py_exception(env);
          } else {
            sj_jlog_info(env, L"Invoked module class function [%s.%s.%s]",
                                m_name ? m_name : "NULL", c_name ? c_name : "NULL", f_name ? f_name : "NULL");
          }
          Py_DECREF(callable);
        }
        Py_DECREF(callable_cls);
      }

      PyGILState_Release(gil_state);
    }

    free(m_name);
    free(c_name);
    free(f_name);

    return ret;
}

JNIEXPORT jobject JNICALL Java_com_dropchop_snakejar_impl_EmbeddedInterpreter__1invoke_1object
  (JNIEnv *env, jobject interp_obj, jobject module, jstring module_name, jstring class_name, jstring obj_name, jstring func_name,
  jclass ret_type, jobject kwargs, jobjectArray args) {
  jobject ret = NULL;
  PyObject *pModule, *callable;
  PyGILState_STATE gil_state;
  char str[1024];
  bool err = false; int sz = 2048; char err_msg[2048];

  return ret;
}


#include "Jep.h"
#include "sj_utils.h"

#ifndef _Included_sj_interpreter
#define _Included_sj_interpreter

PyThreadState* sj_get_main_thread_state();
void sj_set_main_thread_state(PyThreadState *tstate);

jobject sj_invoke_method_as(JNIEnv *env, const char *cname, jobjectArray args, jobject kwargs, jclass expectedType);
jobject sj_invoke_method(JNIEnv *env, const char *cname, jobjectArray args, jobject kwargs);

#endif
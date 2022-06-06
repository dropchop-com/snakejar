#include "Jep.h"
#include "convert_j2p.h"
#include "sj_utils.h"

#ifndef _Included_sj_convert
#define _Included_sj_convert

PyObject* sj_collection_As_PyList(JNIEnv*, jobject);
PyObject* sj_map_As_PyDict(JNIEnv*, jobject);
PyObject* sj_jobject_As_PyObject(JNIEnv*, jobject);

#endif

#include "sj_convert.h"

static PyObject* sj_jnumber_As_PyObject(JNIEnv *env, jobject jobj, jclass class)
{
  if ((*env)->IsSameObject(env, class, JBYTE_OBJ_TYPE)) {
    jbyte b = java_lang_Number_byteValue(env, jobj);
    if ((*env)->ExceptionCheck(env)) {
      process_java_exception(env);
      return NULL;
    }
    return jbyte_As_PyObject(b);
  } else if ((*env)->IsSameObject(env, class, JSHORT_OBJ_TYPE)) {
    jshort s = java_lang_Number_shortValue(env, jobj);
    if ((*env)->ExceptionCheck(env)) {
      process_java_exception(env);
      return NULL;
    }
    return jshort_As_PyObject(s);
  } else if ((*env)->IsSameObject(env, class, JINT_OBJ_TYPE)) {
    jint i = java_lang_Number_intValue(env, jobj);
    if ((*env)->ExceptionCheck(env)) {
      process_java_exception(env);
      return NULL;
    }
    return jint_As_PyObject(i);
  } else if ((*env)->IsSameObject(env, class, JLONG_OBJ_TYPE)) {
    jlong j = java_lang_Number_longValue(env, jobj);
    if ((*env)->ExceptionCheck(env)) {
      process_java_exception(env);
      return NULL;
    }
    return jlong_As_PyObject(j);
  } else if ((*env)->IsSameObject(env, class, JDOUBLE_OBJ_TYPE)) {
    jdouble d = java_lang_Number_doubleValue(env, jobj);
    if ((*env)->ExceptionCheck(env)) {
      process_java_exception(env);
      return NULL;
    }
    return jdouble_As_PyObject(d);
  } else if ((*env)->IsSameObject(env, class, JFLOAT_OBJ_TYPE)) {
    jfloat f = java_lang_Number_floatValue(env, jobj);
    if ((*env)->ExceptionCheck(env)) {
      process_java_exception(env);
      return NULL;
    }
    return jfloat_As_PyObject(f);
  } else if ((*env)->IsSameObject(env, class, JBIGINTEGER_TYPE)) {
    PyObject* pystr = jobject_As_PyString(env, jobj);
    if (pystr == NULL) {
      return NULL;
    }
    PyObject* pyint = PyLong_FromUnicodeObject(pystr, 10);
    Py_DECREF(pystr);
    return pyint;
  } else {
    return jobject_As_PyJObject(env, jobj, class);
  }
}

PyObject* sj_collection_As_PyList(JNIEnv *env, jobject jcollection)
{
  PyObject *pylist = NULL; /* a list */
  jobject itr;

  pylist = PyList_New(0);
  if (!pylist) {
    THROW_JEP(env, "Unable to create python list: PyList_New failed!");
    Py_RETURN_NONE;
  }
  itr = java_lang_Iterable_iterator(env, jcollection);
  if ((*env)->ExceptionCheck(env)) {
    Py_CLEAR(pylist);
    Py_RETURN_NONE;
  }

  while (java_util_Iterator_hasNext(env, itr)) {
    jobject  value;
    PyObject *pyval;

    value = java_util_Iterator_next(env, itr);
    if (!value) {
      if (!(*env)->ExceptionCheck(env)) {
        THROW_JEP(env, "List.iterator().next() returned null");
      }
      Py_CLEAR(pylist);
      Py_RETURN_NONE;
    }

    pyval = sj_jobject_As_PyObject(env, value);
    if (!pyval) {
      Py_CLEAR(pylist);
      Py_RETURN_NONE;
    }

    if (PyList_Append(pylist, pyval)) {
      process_py_exception(env);
      Py_DECREF(pyval);
      Py_CLEAR(pylist);
      Py_RETURN_NONE;
    }
    Py_DECREF(pyval);
    (*env)->DeleteLocalRef(env, value);
  }

  if (itr) {
    (*env)->DeleteLocalRef(env, itr);
  }

  return pylist;
}

PyObject* sj_map_As_PyDict(JNIEnv *env, jobject jmap)
{
  PyObject *pydict = NULL; /* a dictionary */
  jobject entrySet;
  jobject itr;

  pydict = PyDict_New();
  entrySet = java_util_Map_entrySet(env, jmap);
  if ((*env)->ExceptionCheck(env)) {
    Py_CLEAR(pydict);
    Py_RETURN_NONE;
  }
  itr = java_lang_Iterable_iterator(env, entrySet);
  if ((*env)->ExceptionCheck(env)) {
    Py_CLEAR(pydict);
    Py_RETURN_NONE;
  }

  while (java_util_Iterator_hasNext(env, itr)) {
    jobject  next;
    jobject  key;
    jobject  value;
    PyObject *pykey;
    PyObject *pyval;

    next = java_util_Iterator_next(env, itr);
    if (!next) {
      if (!(*env)->ExceptionCheck(env)) {
        THROW_JEP(env, "Map.entrySet().iterator().next() returned null");
      }
      Py_CLEAR(pydict);
      Py_RETURN_NONE;
    }

    // convert Map.Entry's key to a PyObject*
    key = java_util_Map_Entry_getKey(env, next);
    if ((*env)->ExceptionCheck(env)) {
      Py_CLEAR(pydict);
      Py_RETURN_NONE;
    }
    pykey = sj_jobject_As_PyObject(env, key);
    if (!pykey) {
      Py_CLEAR(pydict);
      Py_RETURN_NONE;
    }

    // convert Map.Entry's value to a PyObject*
    value = java_util_Map_Entry_getValue(env, next);
    if ((*env)->ExceptionCheck(env)) {
      Py_XDECREF(pykey);
      Py_CLEAR(pydict);
      Py_RETURN_NONE;
    }
    pyval = sj_jobject_As_PyObject(env, value);
    if (!pyval) {
      Py_DECREF(pykey);
      Py_CLEAR(pydict);
      Py_RETURN_NONE;
    }

    if (PyDict_SetItem(pydict, pykey, pyval)) {
      process_py_exception(env);
      Py_DECREF(pykey);
      Py_DECREF(pyval);
      Py_CLEAR(pydict);
      Py_RETURN_NONE;
    }
    Py_DECREF(pykey);
    Py_DECREF(pyval);

    (*env)->DeleteLocalRef(env, next);
    if (key) {
      (*env)->DeleteLocalRef(env, key);
    }
    if (value) {
      (*env)->DeleteLocalRef(env, value);
    }
  }

  if (itr) {
    (*env)->DeleteLocalRef(env, itr);
  }

  return pydict;
}

PyObject* sj_jobject_As_PyObject(JNIEnv *env, jobject jobj)
{
  PyObject* result = NULL;
  jclass    class  = NULL;
  if (jobj == NULL) {
    Py_RETURN_NONE;
  }
  class = (*env)->GetObjectClass(env, jobj);
  if ((*env)->IsSameObject(env, class, JSTRING_TYPE)) {
    result = jstring_As_PyString(env, (jstring) jobj);
  } else if ((*env)->IsAssignableFrom(env, class, JNUMBER_TYPE)) {
    result = sj_jnumber_As_PyObject(env, jobj, class);
  } else if ((*env)->IsSameObject(env, class, JBOOL_OBJ_TYPE)) {
    result = Boolean_As_PyObject(env, jobj);
  } else if ((*env)->IsSameObject(env, class, JCHAR_OBJ_TYPE)) {
    result = Character_As_PyObject(env, jobj);
  } else if ((*env)->IsAssignableFrom(env, class, JMAP_TYPE)) {
    result = sj_map_As_PyDict(env, jobj);
  } else if ((*env)->IsAssignableFrom(env, class, JCOLLECTION_TYPE)) {
    result = sj_collection_As_PyList(env, jobj);
  } else {
    (*env)->DeleteLocalRef(env, class);
    THROW_JEP(env, "Map.entrySet().iterator().next() returned null");
    Py_RETURN_NONE;
  }
  (*env)->DeleteLocalRef(env, class);
  return result;
}
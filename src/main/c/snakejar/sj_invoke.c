#include "sj_invoke.h"

/*
 * Invoke callable object.  Hold the thread state lock before calling.
 */
jobject sj_invoke_as(JNIEnv *env,
                     PyObject *callable,
                     jobjectArray args,
                     jobject kwargs,
                     jclass expectedType)
{
  jobject        ret      = NULL;
  PyObject      *pyargs   = NULL;    /* a tuple */
  PyObject      *pykwargs = NULL;    /* a dictionary */
  PyObject      *pyret    = NULL;
  int            arglen   = 0;
  Py_ssize_t     i        = 0;

  if (!PyCallable_Check(callable)) {
    THROW_JEP(env, "pyembed:invoke Invalid callable.");
    return NULL;
  }

  if (args != NULL) {
    arglen = (*env)->GetArrayLength(env, args);
    pyargs = PyTuple_New(arglen);
  } else {
    // pyargs should be a Tuple of size 0 if no args
    pyargs = PyTuple_New(arglen);
  }

  // convert Java arguments to a Python tuple
  for (i = 0; i < arglen; i++) {
    jobject   val;
    PyObject *pyval;

    val = (*env)->GetObjectArrayElement(env, args, (jsize) i);
    if ((*env)->ExceptionCheck(env)) { /* careful, NULL is okay */
      goto EXIT;
    }

    pyval = sj_jobject_As_PyObject(env, val);
    if (!pyval) {
      goto EXIT;
    }

    PyTuple_SET_ITEM(pyargs, i, pyval); /* steals */
    if (val) {
      (*env)->DeleteLocalRef(env, val);
    }
  }

  // convert Java arguments to a Python dictionary
  if (kwargs != NULL) {
    jobject entrySet;
    jobject itr;

    pykwargs = PyDict_New();
    entrySet = java_util_Map_entrySet(env, kwargs);
    if ((*env)->ExceptionCheck(env)) {
      goto EXIT;
    }
    itr = java_lang_Iterable_iterator(env, entrySet);
    if ((*env)->ExceptionCheck(env)) {
      goto EXIT;
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
        goto EXIT;
      }

      // convert Map.Entry's key to a PyObject*
      key = java_util_Map_Entry_getKey(env, next);
      if ((*env)->ExceptionCheck(env)) {
        goto EXIT;
      }
      pykey = jobject_As_PyObject(env, key);
      if (!pykey) {
        goto EXIT;
      }

      // convert Map.Entry's value to a PyObject*
      value = java_util_Map_Entry_getValue(env, next);
      if ((*env)->ExceptionCheck(env)) {
        Py_XDECREF(pykey);
        goto EXIT;
      }
      pyval = jobject_As_PyObject(env, value);
      if (!pyval) {
        Py_DECREF(pykey);
        goto EXIT;
      }

      if (PyDict_SetItem(pykwargs, pykey, pyval)) {
        process_py_exception(env);
        Py_DECREF(pykey);
        Py_DECREF(pyval);
        goto EXIT;
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
  } // end of while loop

  // if hasNext() threw an exception
  if ((*env)->ExceptionCheck(env)) {
    goto EXIT;
  }
  pyret = PyObject_Call(callable, pyargs, pykwargs);
  if (process_py_exception(env) || !pyret) {
    goto EXIT;
  }

  // handles errors
  ret = PyObject_As_jobject(env, pyret, expectedType);
  if (!ret) {
    process_py_exception(env);
  }

EXIT:
  Py_CLEAR(pyargs);
  Py_CLEAR(pykwargs);
  Py_XDECREF(pyret);

  return ret;
}
#include "Jep.h"
#include "sj_utils.h"
#include "sj_interpreter.h"

#ifndef _Included_snakejar
#define _Included_snakejar

#define SJ_PY_CMD_LIB_PATH "python -c \"import sysconfig; from os.path import join; print(join(sysconfig.get_config_var('LIBDIR'), sysconfig.get_config_var('LDLIBRARY')))\""

#endif
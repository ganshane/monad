// Copyright 2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_LOGGER_H_
#define MONAD_LOGGER_H_

#include "monad_config.h"
#include "monad.h"

//========> logger module
#define LOGGER_ROTATE_SIZE 5L * 1024 * 1024
#ifndef LOGGER_LEVEL
#define LOGGER_LEVEL LOGGER_LEVEL_DEBUG
#endif
#ifndef LOGGER_FILE_NAME
#define LOGGER_FILE_NAME "stdout"
#endif
#define LOGGER_LEVEL_ERROR  1
#define LOGGER_LEVEL_INFO  2
#define LOGGER_LEVEL_DEBUG  3

MONAD_BEGIN_API

int logger_init(const char* filename);
int logger_debug(const char* fmt,...);
int logger_info(const char* fmt,...);
int logger_error(const char* fmt,...);
int logger_close();

MONAD_END_API

#endif //MONAD_LOGGER_H_

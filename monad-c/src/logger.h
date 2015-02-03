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

#define LEVEL_NAME_LEN  8
#define LOG_BUF_LEN   4096

namespace monad {
  class Logger{
  public:
    static Logger Instance(){
      static Logger logger;
      return logger;
    }
    
    virtual ~Logger();
    void Open(const char* filename);
    
    int Debug(const char* fmt,...);
    int Info(const char* fmt,...);
    int Error(const char* fmt,...);
  private:
    Logger();
    int Rotate();
    int InternalLog(int level, const char *fmt, va_list ap);
    FILE *_logger_file;
    char _filename[PATH_MAX];
    uint64_t _file_length;
  };
  void OpenLogger(const char* filename);
}

#define LogDebug(fmt, args...)	\
monad::Logger::Instance().Debug(fmt, ##args)
#define LogInfo(fmt, args...)	\
monad::Logger::Instance().Info(fmt, ##args)
#define LogError(fmt, args...)	\
monad::Logger::Instance().Error(fmt, ##args)


#endif //MONAD_LOGGER_H_

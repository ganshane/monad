// Copyright 2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_LOGGER_H_
#define MONAD_LOGGER_H_

#include "monad_config.h"

#include "monad.h"
#include "mutex_lock.h"

//========> logger module
#define LOGGER_ROTATE_SIZE 5L * 1024 * 1024
#ifndef LOGGER_FILE_NAME
#define LOGGER_FILE_NAME "stdout"
#endif

#define LEVEL_NAME_LEN  8
#define LOG_BUF_LEN   4096

namespace monad {
  
  class Logger{
  public:
    static Logger* Instance(){
      static Logger logger;
      return &logger;
    }
    
    virtual ~Logger();
    void Open(const char* filename,const LoggerLevel level=LOGGER_LEVEL_INFO);
    
    int Debug(const char* fmt,...);
    int Info(const char* fmt,...);
    int Error(const char* fmt,...);
  private:
    Logger();
    int Rotate();
    int InternalLog(LoggerLevel level, const char *fmt, va_list ap);
    FILE *_logger_file;
    char _filename[PATH_MAX];
    uint64_t _file_length;
      //互斥锁
    Mutex *_mutex;
    LoggerLevel _level;
  };
  void OpenLogger(const char* filename,const LoggerLevel level);
}

#define LogDebug(fmt, args...)	\
monad::Logger::Instance()->Debug(fmt, ##args)
#define LogInfo(fmt, args...)	\
monad::Logger::Instance()->Info(fmt, ##args)
#define LogError(fmt, args...)	\
monad::Logger::Instance()->Error(fmt, ##args)


#endif //MONAD_LOGGER_H_

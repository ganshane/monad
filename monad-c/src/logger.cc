// Copyright 2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#include "logger.h"
#include <stdarg.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>
namespace monad {
  void OpenLogger(const char* filename){
    Logger::Instance().Open(filename);
  }
  Logger::Logger(){
    Open("stdout");
  }
  void Logger::Open(const char* filename){
    _filename[0]='\0';
    if (strcmp(filename, "stdout") == 0) {
      _logger_file = stdout;
    } else if (strcmp(filename, "stderr") == 0) {
      _logger_file = stderr;
    } else {
      _logger_file = fopen(filename, "a");
      strcpy(_filename, filename);
      if (_logger_file  == NULL) {
        return ;
      }
      struct stat st;
      int ret = fstat(fileno(_logger_file), &st);
      if (ret == -1) {
        fprintf(stderr, "fstat log file %s error!", filename);
        return ;
      }
      _file_length = st.st_size;
    }
  }
  
  Logger::~Logger(){
    if (_logger_file != stdin && _logger_file != stdout && _logger_file) {
      fclose(_logger_file);
    }
  }
  int Logger::Rotate(){
    if (_logger_file != stdin && _logger_file != stdout) {
      return 0;
    }
    fclose(_logger_file);
    char newpath[PATH_MAX];
    time_t time;
    struct timeval tv;
    struct tm *tm;
    gettimeofday(&tv, NULL);
    time = tv.tv_sec;
    tm = localtime(&time);
    sprintf(newpath, "%s.%04d%02d%02d-%02d%02d%02d",
            _filename,
            tm->tm_year + 1900, tm->tm_mon + 1, tm->tm_mday,
            tm->tm_hour, tm->tm_min, tm->tm_sec);
    
    //printf("rename %s => %s\n", this->filename, newpath);
    int ret = rename(_filename, newpath);
    if (ret == -1) {
      return ret;
    }
    _logger_file = fopen(_filename, "a");
    if (_logger_file  == NULL) {
      return -1;
    }
    return 0;
  }
  int Logger::InternalLog(int level, const char *fmt, va_list ap) {
    if (LOGGER_LEVEL < level) {
      return 0;
    }
    
    char buf[LOG_BUF_LEN];
    int len;
    char *ptr = buf;
    
    time_t time;
    struct timeval tv;
    struct tm *tm;
    gettimeofday(&tv, NULL);
    time = tv.tv_sec;
    tm = localtime(&time);
    /* %3ld 在数值位数超过3位的时候不起作用, 所以这里转成int */
    len = sprintf(ptr, "%04d-%02d-%02d %02d:%02d:%02d.%03d ",
                  tm->tm_year + 1900, tm->tm_mon + 1, tm->tm_mday,
                  tm->tm_hour, tm->tm_min, tm->tm_sec, (int)(tv.tv_usec / 1000));
    if (len < 0) {
      return -1;
    }
    ptr += len;
    
    switch (level){
      case LOGGER_LEVEL_DEBUG:
        memcpy(ptr, "[DEBUG] ", LEVEL_NAME_LEN);
        break;
      case LOGGER_LEVEL_ERROR:
        memcpy(ptr, "[ERROR] ", LEVEL_NAME_LEN);
        break;
      case LOGGER_LEVEL_INFO:
        memcpy(ptr, "[INFO ] ", LEVEL_NAME_LEN);
        break;
      default:
        break;
    }
    ptr += LEVEL_NAME_LEN;
    
    int space = sizeof(buf) - (ptr - buf) - 10;
    len = vsnprintf(ptr, space, fmt, ap);
    if (len < 0) {
      return -1;
    }
    ptr += len > space ? space : len;
    *ptr++ = '\n';
    *ptr = '\0';
    
    len = ptr - buf;
    // change to write(), without locking?
    fwrite(buf, len, 1, _logger_file);
    fflush(_logger_file);
    
    _file_length += len;
    if (LOGGER_ROTATE_SIZE > 0 && _file_length > LOGGER_ROTATE_SIZE) {
      Rotate();
    }
    
    return len;
  }
  
  int Logger::Debug(const char* fmt,...){
    va_list ap;
    va_start(ap, fmt);
    int ret = InternalLog(LOGGER_LEVEL_DEBUG, fmt, ap);
    va_end(ap);
    
    return ret;
  }
  
  int Logger::Info(const char* fmt,...){
    va_list ap;
    va_start(ap, fmt);
    int ret = InternalLog(LOGGER_LEVEL_INFO, fmt, ap);
    va_end(ap);
    return ret;
  }
  
  int Logger::Error(const char* fmt,...){
    va_list ap;
    va_start(ap, fmt);
    int ret = InternalLog(LOGGER_LEVEL_ERROR, fmt, ap);
    va_end(ap);
    return ret;
  }
}

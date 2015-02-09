#ifndef MONAD_TYPES_H_
#define MONAD_TYPES_H_
namespace monad {
  //禁止默认的copy构造和copy赋值操作
  class Uncopyable{
    protected:
      Uncopyable(){}
      ~Uncopyable(){}
    private:
      Uncopyable(const Uncopyable&);
      Uncopyable& operator=(const Uncopyable&);
  };
  //数据类型的定义
  enum DataType{
    BINLOG = 1,
    KV = 2,
    PARTITION_INFO = 3,
    PARTITION_SEQ = 4,
    //保留系统关键词 5-100
  };
  //定义数据库的类型
  enum BinlogType{
    LOG_BIN= 1,
    LOG_NONE= 2
  };
  //数据操作的类型
  enum DataCommandType{
    PUT = 1,
    DEL = 2,
    UPDATE = 3,
    COMMAND_UNKNOWN = 4
  };
  
  struct NoSQLOptions{
    unsigned int cache_size_mb;
    unsigned int write_buffer_mb;
    unsigned int max_open_files;
    unsigned int log_keeped_num;
    unsigned int block_size_kb;
    unsigned int max_mmap_size;
    unsigned int target_file_size;
    NoSQLOptions(){
      cache_size_mb = 8;
      write_buffer_mb = 32;
      max_open_files = 40;
      log_keeped_num = 0;
      block_size_kb = 32;
      max_mmap_size = 1000;
      target_file_size = 2 << 20;
    }
  };

  enum LoggerLevel{
    LOGGER_LEVEL_ERROR = 1,
    LOGGER_LEVEL_INFO = 2,
    LOGGER_LEVEL_DEBUG = 3
  };
}
#endif //MONAD_TYPES_H_

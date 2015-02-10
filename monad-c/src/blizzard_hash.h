// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_BLIZZARD_HASH_H_
#define MONAD_BLIZZARD_HASH_H_
#include <stdint.h>
#include <string>
#include "logger.h"
#include "monad.h"
namespace monad {
//暴雪Hash的类型
enum BlizzardHashType {
  BH_TYPE_0 = 0,
  BH_TYPE_1 = 1,
  BH_TYPE_2 = 2
};
//暴雪Hash的实现
class BlizzardHash: private Uncopyable {
 private:
  unsigned long _crypt_table[0x500];
  void InitCryptTable(); // 对哈希索引表预处理
  unsigned long HashString(const std::string &str, const BlizzardHashType hash_type);
  explicit BlizzardHash() {
    InitCryptTable();
  }
 public:
  static uint64_t HashString(const std::string &str) {
    static BlizzardHash instance;
    uint64_t ret;
    ret = instance.HashString(str, BH_TYPE_0);
    ret <<= 32;
    ret += instance.HashString(str, BH_TYPE_1);
    return ret;
  }
};//BlizzardHash

}//namespace monad
#endif //MONAD_BLIZZARD_HASH_H_

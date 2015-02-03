// Copyright 2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#include "blizzard_hash.h"

namespace monad {
    void BlizzardHash::InitCryptTable() {
      unsigned long seed = 0x00100001, index1 = 0, index2 = 0, i;
      for ( index1 = 0; index1 < 0x100; index1++ ) {
        for ( index2 = index1, i = 0; i < 5; i++, index2 += 0x100 ) {
          uint32_t temp1, temp2;
          seed = (seed * 125 + 3) % 0x2AAAAB;
          temp1 = (seed & 0xFFFF) << 0x10;
          seed = (seed * 125 + 3) % 0x2AAAAB;
          temp2 = (seed & 0xFFFF);
          _crypt_table[index2] = ( temp1 | temp2 );
        }
      }
    }
    unsigned long BlizzardHash::HashString(const std::string &str, const BlizzardHashType hash_type) {
      unsigned long seed1 = 0x7FED7FED, seed2 = 0xEEEEEEEE;
      int ch;
      int len = str.size();
      for (int i = 0; i < len; i++) {
        ch = toupper(str[i]);
        seed1 = _crypt_table[(hash_type << 8) + ch] ^ (seed1 + seed2);
        seed2 = ch + seed1 + seed2 + (seed2 << 5) + 3;
      }
      return seed1;
    }
}//namespace monad


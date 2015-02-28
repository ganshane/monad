// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_UTIL_CODING_H_
#define MONAD_UTIL_CODING_H_

#include <stdint.h>
#include <string.h>
#include <string>

#include "monad_endian.h"

namespace monad {
    // 对整数进行编码为字符
    // REQUIRES: dst has enough space for the value being written
    inline void EncodeFixed32ToTowCharWithBigEndian(char *buf, uint32_t value) {
#ifdef MONAD_BIG_ENDIAN
      memcpy(buf, &value, size);
#else
      buf[1] = value & 0xff;
      buf[0] = (value >> 8) & 0xff;
#endif
    }
    inline void EncodeFixed32WithBigEndian(char *buf, uint32_t value) {
#ifdef MONAD_BIG_ENDIAN
      memcpy(buf, &value, size);
#else
      buf[3] = value & 0xff;
      buf[2] = (value >> 8) & 0xff;
      buf[1] = (value >> 16) & 0xff;
      buf[0] = (value >> 24) & 0xff;
#endif
    }
    inline void EncodeFixed32WithBigEndian(std::string &buf, uint32_t value) {
      char tmp_buf[4];
      EncodeFixed32WithBigEndian(tmp_buf, value);
      buf.append(tmp_buf, 4);
    }
    inline void EncodeFixed32(char *buf, uint32_t value,
                              size_t size = sizeof(uint32_t)) {
#ifdef MONAD_LITTLE_ENDIAN
      memcpy(buf, &value, size);
#else
      if (size > 0 )
        buf[0] = value & 0xff;
      if (size > 1 )
        buf[1] = (value >> 8) & 0xff;
      if (size > 2 )
        buf[2] = (value >> 16) & 0xff;
      if (size > 3 )
        buf[3] = (value >> 24) & 0xff;
#endif
    }
    inline void EncodeFixed32(std::string &buf, const uint32_t &value,
                              size_t size = sizeof(uint32_t)) {
      char data[size];
      EncodeFixed32(data, value, size);
      buf.append(data, size);
    }
    
    inline void EncodeFixed64(char *buf, uint64_t value) {
#ifdef MONAD_LITTLE_ENDIAN
      memcpy(buf, &value, sizeof(value));
#else
      buf[0] = value & 0xff;
      buf[1] = (value >> 8) & 0xff;
      buf[2] = (value >> 16) & 0xff;
      buf[3] = (value >> 24) & 0xff;
      buf[4] = (value >> 32) & 0xff;
      buf[5] = (value >> 40) & 0xff;
      buf[6] = (value >> 48) & 0xff;
      buf[7] = (value >> 56) & 0xff;
#endif
    }
    
    inline void EncodeFixed64(std::string &buf, uint64_t value) {
      char data[8];
      EncodeFixed64(data, value);
      buf.append(data, 8);
    }
    inline void EncodeFixed64WithBigEndian(char *buf, uint64_t value) {
#ifdef MONAD_BIG_ENDIAN
      memcpy(buf, &value, sizeof(value));
#else
      buf[7] = value & 0xff;
      buf[6] = (value >> 8) & 0xff;
      buf[5] = (value >> 16) & 0xff;
      buf[4] = (value >> 24) & 0xff;
      buf[3] = (value >> 32) & 0xff;
      buf[2] = (value >> 40) & 0xff;
      buf[1] = (value >> 48) & 0xff;
      buf[0] = (value >> 56) & 0xff;
#endif
    }
    inline void EncodeFixed64WithBigEndian(std::string &buf, uint64_t value) {
      char data[8];
      EncodeFixed64WithBigEndian(data, value);
      buf.append(data, 8);
    }
    
    //对字符串还原出来对应的数字
    /*
     inline uint32_t DecodeFixed32(const std::string& data,size_t size=sizeof(uint32_t)) {
     const char* ptr=data.data();
     return DecodeFixed32(ptr,size);
     }
     */
    inline uint32_t DecodeFixed32WithBigEndian(const char *ptr) {
#ifdef MONAD_BIG_ENDIAN
      // Load the raw bytes
      uint32_t result;
      memcpy(&result, ptr, size);  // gcc optimizes this to a plain load
      return result;
#else
      uint32_t result(0);
      result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[0])) << 24;
      result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[1])) << 16;
      result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[2])) << 8;
      result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[3]));
      return result;
#endif
    }
    inline uint32_t DecodeFixed32(const char *ptr, size_t size = sizeof(uint32_t)) {
#ifdef MONAD_LITTLE_ENDIAN
      // Load the raw bytes
      uint32_t result(0);
      memcpy(&result, ptr, size);  // gcc optimizes this to a plain load
      return result;
#else
      uint32_t result(0);
      if (size > 0)
        result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[0]));
      if (size > 1)
        result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[1])) << 8;
      if (size > 2)
        result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[2])) << 16;
      if (size > 3)
        result |= static_cast<uint32_t>(static_cast<unsigned char>(ptr[3])) << 24;
      return result;
#endif
    }
    
    inline uint64_t DecodeFixed64(const char *ptr) {
#ifdef MONAD_LITTLE_ENDIAN
      // Load the raw bytes
      uint64_t result(0);
      memcpy(&result, ptr, sizeof(result));  // gcc optimizes this to a plain load
      return result;
#else
      uint64_t lo = DecodeFixed32(ptr);
      uint64_t hi = DecodeFixed32(ptr + 4);
      return (hi << 32) | lo;
#endif
    }
    inline uint64_t DecodeFixed64WithBigEndian(const char *ptr) {
#ifdef MONAD_BIG_ENDIAN
      // Load the raw bytes
      uint64_t result(0);
      memcpy(&result, ptr, sizeof(result));  // gcc optimizes this to a plain load
      return result;
#else
      uint64_t hi = DecodeFixed32WithBigEndian(ptr);
      uint64_t lo = DecodeFixed32WithBigEndian(ptr + 4);
      return (hi << 32) | lo;
#endif
    }
}  // namespace monad

#endif  // MONAD_UTIL_CODING_H_

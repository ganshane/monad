// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include <stdio.h>
#include <stdint.h>
#include "status.h"

namespace monad {

  const char *MonadStatus::CopyState(const char *state) {
    uint32_t size;
    memcpy(&size, state, sizeof(size));
    char *result = new char[size + 5];
    memcpy(result, state, size + 5);
    return result;
  }

  MonadStatus::MonadStatus(StatusCode code, const leveldb::Slice &msg, const leveldb::Slice &msg2) {
    assert(code != kOk);
    const uint32_t len1 = msg.size();
    const uint32_t len2 = msg2.size();
    const uint32_t size = len1 + (len2 ? (2 + len2) : 0);
    char *result = new char[size + 5];
    memcpy(result, &size, sizeof(size));
    result[4] = static_cast<char>(code);
    memcpy(result + 5, msg.data(), len1);
    if (len2) {
      result[5 + len1] = ':';
      result[6 + len1] = ' ';
      memcpy(result + 7 + len1, msg2.data(), len2);
    }
    state_ = result;
  }

  std::string MonadStatus::ToString() const {
    if (state_ == NULL) {
      return "OK";
    } else {
      char tmp[30];
      const char *type;
      snprintf(tmp, sizeof(tmp), "Code(%d): ", static_cast<int>(code()));
      type = tmp;
      std::string result(type);
      uint32_t length;
      memcpy(&length, state_, sizeof(length));
      result.append(state_ + 5, length);
      return result;
    }
  }

}//namespace monad

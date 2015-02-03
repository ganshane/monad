// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_STATUS_H_
#define MONAD_STATUS_H_

#include <string>
#include "monad_config.h"
#ifdef MONAD_HAVE_ROCKSDB
#include "rocksdb/slice.h"
#include "rocksdb/status.h"
#else
#include "leveldb/slice.h"
#include "leveldb/status.h"
#endif

namespace monad{
  enum StatusCode {
    kOk = 0,
    kFailPut = 1,
    kFailGet = 2,
    kNotFound = 3,
    kLeveldb = 4
  };

  class MonadStatus {
    public:
      // Create a success status.
      MonadStatus() : state_(NULL) { }
      ~MonadStatus() { delete[] state_; }

      // Copy the specified status.
      MonadStatus(const MonadStatus& s);
      void operator=(const MonadStatus& s);

      // Return a success status.
      static MonadStatus OK() { return MonadStatus(); }
      static MonadStatus FromLeveldbStatus(leveldb::Status& status) {
        if(status.ok())
          return OK();
        else
          return MonadStatus(kLeveldb,status.ToString());
      }

      // Returns true iff the status indicates success.
      bool ok() const { return (state_ == NULL); }

      // Return a string representation of this status suitable for printing.
      // Returns the string "OK" for success.
      std::string ToString() const;
      //构造函数
      MonadStatus(StatusCode code, const leveldb::Slice& msg, const leveldb::Slice& msg2 = leveldb::Slice());

      StatusCode code() const {
        return (state_ == NULL) ? kOk : static_cast<StatusCode>(state_[4]);
      }
      const char* GetState(){return state_;}
    private:
      // OK status has a NULL state_.  Otherwise, state_ is a new[] array
      // of the following form:
      //    state_[0..3] == length of message
      //    state_[4]    == code
      //    state_[5..]  == message
      const char* state_;
      static const char* CopyState(const char* s);
  };
  inline MonadStatus::MonadStatus(const MonadStatus& s) {
    state_ = (s.state_ == NULL) ? NULL : CopyState(s.state_);
  }
  inline void MonadStatus::operator=(const MonadStatus& s) {
    // The following condition catches both aliasing (when this == &s),
    // and the common case where both s and *this are ok.
    if (state_ != s.state_) {
      delete[] state_;
      state_ = (s.state_ == NULL) ? NULL : CopyState(s.state_);
    }
  }
}  // namespace nirvana

#endif //MONAD_UTIL_STATUS_H_

// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_MUTEXT_LOCK_H_
#define MONAD_MUTEXT_LOCK_H_

#include "monad_config.h"
#include "monad_types.h"

#ifndef MONAD_HAVE_WINDOWS
#include <pthread.h>
#endif

namespace monad {
#ifdef MONAD_HAVE_WINDOWS
    class CondVar;
    class Mutex :private Uncopyable{
    public:
      Mutex();
      ~Mutex();
      void Lock();
      void Unlock();
      void AssertHeld();

    private:
      friend class CondVar;
      // critical sections are more efficient than mutexes
      // but they are not recursive and can only be used to synchronize threads within the same process
      // we use opaque void * to avoid including windows.h in port_win.h
      void *cs_;
    };
    // the Win32 API offers a dependable condition variable mechanism, but only starting with
    // Windows 2008 and Vista
    // no matter what we will implement our own condition variable with a semaphore
    // implementation as described in a paper written by Andrew D. Birrell in 2003
    class CondVar {
    public:
      explicit CondVar(Mutex *mu);
      ~CondVar();
      void Wait();
      void Signal();
      void SignalAll();
    private:
      Mutex *mu_;
      Mutex wait_mtx_;
      long waiting_;
      void *sem1_;
      void *sem2_;
    };
#else
    //定义互斥类型,目前使用pthread.
    class Mutex: private Uncopyable {
    public:
      Mutex();
      ~Mutex();
      void Lock();
      void Unlock();
      void AssertHeld() { }
    private:
      pthread_mutex_t _mu;
    };//class Mutex
#endif

    //互斥锁
    class MutexLock: private Uncopyable {
    public:
      explicit MutexLock(Mutex *mu) : _mu(mu) {
        this->_mu->Lock();
      }
      ~MutexLock() { this->_mu->Unlock(); }
    private:
      Mutex *const _mu;
    };//class MutexLock
}//namespace monad
#endif //MONAD_MUTEXT_LOCK_H_


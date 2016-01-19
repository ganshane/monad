// Copyright 2014,2015,2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "monad_config.h"

#include "mutex_lock.h"

#include <cstdlib>
#include <stdio.h>
#include <string.h>

#ifdef MONAD_HAVE_WINDOWS
#include <windows.h>
#include <cassert>
#endif

namespace monad {
#ifdef MONAD_HAVE_WINDOWS
  Mutex::Mutex() :
  cs_(NULL) {
    assert(!cs_);
    cs_ = static_cast<void *>(new CRITICAL_SECTION());
    ::InitializeCriticalSection(static_cast<CRITICAL_SECTION *>(cs_));
    assert(cs_);
  }

  Mutex::~Mutex() {
    assert(cs_);
    ::DeleteCriticalSection(static_cast<CRITICAL_SECTION *>(cs_));
    delete static_cast<CRITICAL_SECTION *>(cs_);
    cs_ = NULL;
    assert(!cs_);
  }

  void Mutex::Lock() {
    assert(cs_);
    ::EnterCriticalSection(static_cast<CRITICAL_SECTION *>(cs_));
  }

  void Mutex::Unlock() {
    assert(cs_);
    ::LeaveCriticalSection(static_cast<CRITICAL_SECTION *>(cs_));
  }
  
  void Mutex::AssertHeld() {
    assert(cs_);
    assert(1);
  }

  CondVar::CondVar(Mutex *mu) :
  waiting_(0),
  mu_(mu),
  sem1_(::CreateSemaphore(NULL, 0, 10000, NULL)),
  sem2_(::CreateSemaphore(NULL, 0, 10000, NULL)) {
    assert(mu_);
  }

  CondVar::~CondVar() {
    ::CloseHandle(sem1_);
    ::CloseHandle(sem2_);
  }
  
  void CondVar::Wait() {
    mu_->AssertHeld();
    
    wait_mtx_.Lock();
    ++waiting_;
    wait_mtx_.Unlock();
    
    mu_->Unlock();
    
    // initiate handshake
    ::WaitForSingleObject(sem1_, INFINITE);
    ::ReleaseSemaphore(sem2_, 1, NULL);
    mu_->Lock();
  }
  
  void CondVar::Signal() {
    wait_mtx_.Lock();
    if (waiting_ > 0) {
      --waiting_;
      
      // finalize handshake
      ::ReleaseSemaphore(sem1_, 1, NULL);
      ::WaitForSingleObject(sem2_, INFINITE);
    }
    wait_mtx_.Unlock();
  }
  
  void CondVar::SignalAll() {
    wait_mtx_.Lock();
    ::ReleaseSemaphore(sem1_, waiting_, NULL);
    while (waiting_ > 0) {
      --waiting_;
      ::WaitForSingleObject(sem2_, INFINITE);
    }
    wait_mtx_.Unlock();
  }
#else
  static void PthreadCall(const char *label, int result) {
    if (result != 0) {
      fprintf(stderr,"pthread %s: %s", label, strerror(result));
      abort();
    }
  }
  Mutex::Mutex() { PthreadCall("init mutex", pthread_mutex_init(&_mu, NULL)); }
  
  Mutex::~Mutex() { PthreadCall("destroy mutex", pthread_mutex_destroy(&_mu)); }
  
  void Mutex::Lock() { PthreadCall("lock", pthread_mutex_lock(&_mu)); }
  
  void Mutex::Unlock() { PthreadCall("unlock", pthread_mutex_unlock(&_mu)); }
#endif
}//namespace monad

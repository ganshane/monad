#ifndef MONAD_H_
#define MONAD_H_

#include <limits.h>
#include <stdio.h>
#include <sys/stat.h>
//monad api
#ifdef __cplusplus
#define MONAD_BEGIN_API extern "C" {
#define MONAD_END_API }
#else
#define MONAD_BEGIN_API
#define MONAD_END_API
#endif

#if defined(_LP64)         || \
    defined(__LP64__)      || \
    defined(__64BIT__)     || \
    defined(__powerpc64__) || \
    defined(__osf__)
#define MONAD_64BIT
#endif

#if defined(WIN32)
typedef unsigned __int32 monad_uint32_t;
typedef unsigned __int64 monad_uint64_t;
typedef __int32 monad_int32_t;
typedef __int64 monad_int64_t;
#else
#include <stdint.h>
#endif

//无符号64位的最大值
#ifndef UINT64_MAX
  #define UINT64_MAX        18446744073709551615ULL
#endif
//无符号32位最大值
#ifndef UINT32_MAX
  #define UINT32_MAX        4294967295U
#endif



#include "monad_types.h"
#include "logger.h"

//禁止默认的copy构造和copy赋值操作
class Uncopyable;

#endif //MONAD_H_

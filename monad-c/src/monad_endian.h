// Copyright 2014,2015 Jun Tsai. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_UTIL_ENDIAN_H_
#define MONAD_UTIL_ENDIAN_H_
// GNU libc offers the helpful header <endian.h> which defines
// __BYTE_ORDER

#if defined (__GLIBC__)
# include <endian.h>
# if (__BYTE_ORDER == __LITTLE_ENDIAN)
#  define MONAD_LITTLE_ENDIAN
# elif (__BYTE_ORDER == __BIG_ENDIAN)
#  define MONAD_BIG_ENDIAN
# elif (__BYTE_ORDER == __PDP_ENDIAN)
#  define MONAD_PDP_ENDIAN
# else
#  error Unknown machine endianness detected.
# endif
# define MONAD_BYTE_ORDER __BYTE_ORDER
#elif defined(_BIG_ENDIAN) && !defined(_LITTLE_ENDIAN)
# define MONAD_BIG_ENDIAN
# define MONAD_BYTE_ORDER 4321
#elif defined(_LITTLE_ENDIAN) && !defined(_BIG_ENDIAN)
# define MONAD_LITTLE_ENDIAN
# define MONAD_BYTE_ORDER 1234
#elif defined(__sparc) || defined(__sparc__) \
   || defined(_POWER) || defined(__powerpc__) \
   || defined(__ppc__) || defined(__hpux) || defined(__hppa) \
   || defined(_MIPSEB) || defined(_POWER) \
   || defined(__s390__)
# define MONAD_BIG_ENDIAN
# define MONAD_BYTE_ORDER 4321
#elif defined(__i386__) || defined(__alpha__) \
   || defined(__ia64) || defined(__ia64__) \
   || defined(_M_IX86) || defined(_M_IA64) \
   || defined(_M_ALPHA) || defined(__amd64) \
   || defined(__amd64__) || defined(_M_AMD64) \
   || defined(__x86_64) || defined(__x86_64__) \
   || defined(_M_X64) || defined(__bfin__)

# define MONAD_LITTLE_ENDIAN
# define MONAD_BYTE_ORDER 1234
#else
# error The file util/endian.h needs to be set up for your CPU type.
#endif
#endif //MONAD_UTIL_ENDIAN_H_

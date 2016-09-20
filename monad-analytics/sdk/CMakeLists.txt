# Copyright 2016 the original author or authors. All rights reserved.
# site: http://www.ganshane.com
# on mingw
# cmake -DCMAKE_TOOLCHAIN_FILE=/build/Toolchain-cross-mingw32-linux.cmake \
#-DCMAKE_BUILD_TYPE=Release \
#-DENABLE_SDK=on \
#-DHOST=$HOST  -DARCH=$ARCH \

AUX_SOURCE_DIRECTORY("." ANALYTICS_SDK_SOURCE)

#snappy
INCLUDE_DIRECTORIES(../snappy ../leveldb-1.18/include ../leveldb-1.18)
LIST(APPEND ANALYTICS_SDK_SOURCE ../snappy/snappy.cc ../snappy/snappy-sinksource.cc)

IF(MONAD_HAVE_OSX)
    SET(MONAD_PRE_DEFINITION "-DOS_MACOSX -DLEVELDB_PLATFORM_POSIX")
#    ADD_DEFINITIONS(-DOS_MACOSX -DLEVELDB_PLATFORM_POSIX)
    LIST(APPEND ANALYTICS_SDK_SOURCE ../leveldb-1.18/port/port_posix.cc ../leveldb-1.18/util/env_posix.cc)
ELSEIF(MONAD_HAVE_WINDOWS)
    SET(MONAD_PRE_DEFINITION "-DOS_WINDOWS -DLEVELDB_PLATFORM_WINDOWS -DDLL_EXPORT")
    LIST(APPEND ANALYTICS_SDK_SOURCE ../leveldb-1.18/port/port_win.cc ../leveldb-1.18/util/env_win.cc)
ENDIF()
APPEND(MONAD_PRE_DEFINITION "-DNDEBUG -DSNAPPY")
#leveldb
LIST(APPEND ANALYTICS_SDK_SOURCE   ../leveldb-1.18/db/builder.cc 
    ../leveldb-1.18/db/c.cc 
    ../leveldb-1.18/db/db_impl.cc 
    ../leveldb-1.18/db/db_iter.cc 
    ../leveldb-1.18/db/dbformat.cc 
    ../leveldb-1.18/db/filename.cc 
    ../leveldb-1.18/db/log_reader.cc 
    ../leveldb-1.18/db/log_writer.cc 
    ../leveldb-1.18/db/memtable.cc 
    ../leveldb-1.18/db/repair.cc 
    ../leveldb-1.18/db/table_cache.cc 
    ../leveldb-1.18/db/version_edit.cc 
    ../leveldb-1.18/db/version_set.cc 
    ../leveldb-1.18/db/write_batch.cc 
    ../leveldb-1.18/table/block.cc 
    ../leveldb-1.18/table/block_builder.cc 
    ../leveldb-1.18/table/filter_block.cc 
    ../leveldb-1.18/table/format.cc 
    ../leveldb-1.18/table/iterator.cc 
    ../leveldb-1.18/table/merger.cc 
    ../leveldb-1.18/table/table.cc 
    ../leveldb-1.18/table/table_builder.cc 
    ../leveldb-1.18/table/two_level_iterator.cc 
    ../leveldb-1.18/util/arena.cc 
    ../leveldb-1.18/util/bloom.cc 
    ../leveldb-1.18/util/cache.cc 
    ../leveldb-1.18/util/coding.cc 
    ../leveldb-1.18/util/comparator.cc 
    ../leveldb-1.18/util/crc32c.cc 
    ../leveldb-1.18/util/env.cc 
    ../leveldb-1.18/util/filter_policy.cc
    ../leveldb-1.18/util/hash.cc 
    ../leveldb-1.18/util/histogram.cc 
    ../leveldb-1.18/util/logging.cc 
    ../leveldb-1.18/util/options.cc 
    ../leveldb-1.18/util/status.cc)

ADD_LIBRARY(monad SHARED ${ANALYTICS_SDK_SOURCE})
	SET_TARGET_PROPERTIES(monad PROPERTIES LINKER_LANGUAGE CXX)

IF(MONAD_HAVE_WINDOWS)
    TARGET_LINK_LIBRARIES(monad analytics shlwapi)
    SET_TARGET_PROPERTIES(monad PROPERTIES COMPILE_CXX_FLAGS "-std=c++11 -Wall -Wextra -Wno-unused-parameter -fno-omit-frame-pointer  -fno-strict-aliasing")
#    SET_TARGET_PROPERTIES(monad PROPERTIES LINK_FLAGS "-static -static-libgcc -static-libstdc++ -Wl,--kill-at -Wl,-allow-multiple-definition -Wl,--exclude-all-symbols")
    SET_TARGET_PROPERTIES(monad PROPERTIES LINK_FLAGS "-static -static-libgcc -static-libstdc++ -Wl,--kill-at -fno-strict-aliasing")
ELSE()
    TARGET_LINK_LIBRARIES(monad analytics)
    SET_TARGET_PROPERTIES(monad PROPERTIES COMPILE_CXX_FLAGS "-std=c++11 -fPIC -Wall -Wextra -Wno-unused-parameter  -fno-omit-frame-pointer   -fno-strict-aliasing")
ENDIF()
SET_TARGET_PROPERTIES(monad PROPERTIES DEFINE_SYMBOL "XDUMP ${MONAD_PRE_DEFINITION}")

ADD_EXECUTABLE(monad_demo monad_demo.cc)
SET_TARGET_PROPERTIES(monad_demo PROPERTIES COMPILE_FLAGS "-std=gnu++11 -Wno-unused-parameter -fno-omit-frame-pointer -Wall -Wextra -fno-strict-aliasing")
IF(MONAD_HAVE_WINDOWS)
    SET_TARGET_PROPERTIES(monad_demo PROPERTIES LINK_FLAGS "-static -static-libgcc -static-libstdc++ -Wl,--kill-at -Wl,-allow-multiple-definition -Wl,--exclude-all-symbols")
ENDIF()
TARGET_LINK_LIBRARIES(monad_demo monad)

SET(HEADERS ${PROJECT_SOURCE_DIR}/sdk/monad_sdk.h ${PROJECT_SOURCE_DIR}/sdk/monad_sdk_code.h)
SET_TARGET_PROPERTIES(monad PROPERTIES PUBLIC_HEADER "${HEADERS}")
  INSTALL(TARGETS monad
     EXPORT monad-sdk 
     RUNTIME DESTINATION bin COMPONENT runtime
#    ARCHIVE DESTINATION lib
     LIBRARY DESTINATION bin
     PUBLIC_HEADER DESTINATION include
    )
install(TARGETS monad_demo
     RUNTIME DESTINATION bin COMPONENT runtime) 

#INSTALL(FILES "monad_sdk.h" "monad_sdk_code.h"  DESTINATION include)

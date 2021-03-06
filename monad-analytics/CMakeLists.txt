# Copyright 2016 the original author or authors. All rights reserved.
# site: http://www.ganshane.com
PROJECT(analytics)
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

SET(ANALYTICS_VERSION_MAJOR 0)
SET(ANALYTICS_VERSION_MINOR 1)
SET(ANALYTICS_VERSION_PATCH 9)

INCLUDE(CheckIncludeFiles)
INCLUDE(CheckIncludeFileCXX)
#INCLUDE(FindProtobuf)


# global config file
CONFIGURE_FILE(${CMAKE_CURRENT_SOURCE_DIR}/src/config.h.in ${CMAKE_CURRENT_BINARY_DIR}/config.h)


# link all library as static.to release jni library must open the flag
OPTION(STATIC_LINK "statically link all library " OFF)
if(STATIC_LINK)
    ADD_LIBRARY(tcmalloc_minimal STATIC IMPORTED)
    SET_PROPERTY(TARGET tcmalloc_minimal  PROPERTY IMPORTED_LOCATION /usr/local/lib/libtcmalloc_minimal.a)
endif()

#libanalytics
AUX_SOURCE_DIRECTORY("src" ANALYTICS_SOURCE)
FILE(GLOB_RECURSE INTERNAL_HEADERS ${PROJECT_SOURCE_DIR}/src/*.h)
INCLUDE_DIRECTORIES(${PROJECT_SOURCE_DIR}/src)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_BINARY_DIR})


ADD_LIBRARY(analytics STATIC ${INTERNAL_HEADERS} ${ANALYTICS_SOURCE})
IF(CMAKE_SYSTEM_NAME MATCHES "Linux" )
  SET_TARGET_PROPERTIES(analytics PROPERTIES COMPILE_FLAGS "-fPIC -fno-omit-frame-pointer -Wall -Wextra -fno-strict-aliasing")
	TARGET_LINK_LIBRARIES(analytics)
ELSE()
  SET_TARGET_PROPERTIES(analytics PROPERTIES COMPILE_FLAGS "-Wall")
ENDIF()



# library jni
SET(SWIG_SOURCES "")


#build whole library
MESSAGE(STATUS "SWIG SOURCES ${SWIG_SOURCES}")
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR})

OPTION(ENABLE_CSHARP "enable csharp" OFF)
IF(ENABLE_CSHARP)
  # build matcher charp library using SWIG
  SET(SWIG_I_CSHARP src/analytics_csharp.i)
  SET_SOURCE_FILES_PROPERTIES(${SWIG_I_CSHARP} PROPERTIES CPLUSPLUS ON)
  SET_SOURCE_FILES_PROPERTIES(${SWIG_I_CSHARP} PROPERTIES SWIG_FLAGS "-namespace;Monad.Analytics.Bridge;-dllimport;libanalytics4csharp")
  SET(SWIG_SOURCES ${SWIG_SOURCES} ${SWIG_I_CSHARP})
  SET(CMAKE_SWIG_OUTDIR csharpapi)
  ADD_CUSTOM_COMMAND(OUTPUT ${CMAKE_SWIG_OUTDIR}
    COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_SWIG_OUTDIR}
    COMMAND ${CMAKE_COMMAND} -E make_directory ${PROJECT_BINARY_DIR}/src-csharp
    COMMENT "Creating target directory for analytics project csharp sources")

  SWIG_ADD_MODULE(analytics4csharp CSHARP ${SWIG_SOURCES})
  SWIG_LINK_LIBRARIES(analytics4csharp analytics)
  SET_TARGET_PROPERTIES(analytics4csharp PROPERTIES LINKER_LANGUAGE CXX)
  IF(MINGW)
    SET_TARGET_PROPERTIES(analytics4csharp PROPERTIES LINK_FLAGS "-static-libgcc -static-libstdc++ -Wl,--kill-at")
  ELSE()
    SET_TARGET_PROPERTIES(analytics4csharp PROPERTIES LINK_FLAGS "-felide-constructors")
  ENDIF()
  ADD_CUSTOM_TARGET(csharp-swig-out-dir ALL DEPENDS ${CMAKE_SWIG_OUTDIR})
  ADD_DEPENDENCIES(analytics4csharp analytics csharp-swig-out-dir)
ENDIF()

# build matcher jni library using SWIG
OPTION(ENABLE_JNI "enable test" OFF)
if(ENABLE_JNI)
  # swig support
  FIND_PACKAGE(SWIG REQUIRED)
  INCLUDE(${SWIG_USE_FILE})
  INCLUDE(UseSWIG)
# java jni support
  FIND_PACKAGE(Java REQUIRED)
  FIND_PACKAGE(JNI REQUIRED)
#INCLUDE(UseJava)
  INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH})
  INCLUDE_DIRECTORIES(${JAVA_INCLUDE_PATH2})

  SET(SWIG_I src/analytics.i)
  SET_SOURCE_FILES_PROPERTIES(${SWIG_I} PROPERTIES CPLUSPLUS ON)
  SET_SOURCE_FILES_PROPERTIES(${SWIG_I} PROPERTIES SWIG_FLAGS "-package;monad.analytics.jni")
  SET(SWIG_SOURCES  ${SWIG_I})
  SET(CMAKE_SWIG_OUTDIR javaapi)
  ADD_CUSTOM_COMMAND(OUTPUT ${CMAKE_SWIG_OUTDIR}
      COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_SWIG_OUTDIR}
      COMMAND ${CMAKE_COMMAND} -E make_directory ${PROJECT_BINARY_DIR}/src
      COMMENT "Creating target directory for analytics project JNI sources")

  SWIG_ADD_MODULE(analytics4j JAVA ${SWIG_SOURCES})
  SWIG_LINK_LIBRARIES(analytics4j analytics)
  SET_TARGET_PROPERTIES(analytics4j PROPERTIES LINKER_LANGUAGE CXX)
  IF(MINGW)
    SET_TARGET_PROPERTIES(analytics4j PROPERTIES LINK_FLAGS "-static -static-libgcc -static-libstdc++ -Wl,--kill-at -fno-strict-aliasing")
  ELSE()
    SET_TARGET_PROPERTIES(analytics4j PROPERTIES LINK_FLAGS "-felide-constructors -fno-strict-aliasing")
  ENDIF()
  SET_TARGET_PROPERTIES(analytics4j PROPERTIES COMPILE_FLAGS "-fno-strict-aliasing")

  ADD_CUSTOM_TARGET(jni-swig-out-dir ALL DEPENDS ${CMAKE_SWIG_OUTDIR})
  ADD_DEPENDENCIES(analytics4j analytics jni-swig-out-dir)
  INSTALL(TARGETS analytics4j
    ARCHIVE DESTINATION lib
    LIBRARY DESTINATION lib
    #    PUBLIC_HEADER DESTINATION include
    )
ENDIF()

# build em
OPTION(ENABLE_EM "enable em" OFF)
if(ENABLE_EM)
    ADD_SUBDIRECTORY(em)
ENDIF()
# build pnacl
OPTION(ENABLE_PNACL "enable pnacl" OFF)
if(ENABLE_PNACL)
    ADD_SUBDIRECTORY(pnacl)
ENDIF()
# build unit test
# you must config gtest directory using GTEST_ROOT variable.
OPTION(ENABLE_TEST "enable test" OFF)
if(ENABLE_TEST)
    ENABLE_TESTING()
    FIND_PACKAGE(GTest REQUIRED)
    INCLUDE_DIRECTORIES(${GTEST_INCLUDE_DIRS})

    ADD_SUBDIRECTORY(test)

ENDIF()

SET(CPACK_GENERATOR "DEB")
SET(CPACK_PACKAGE_VENDOR "EGF IT Software Department")
SET(CPACK_PACKAGE_VERSION_MAJOR ${ANALYTICS_VERSION_MAJOR})
SET(CPACK_PACKAGE_VERSION_MINOR ${ANALYTICS_VERSION_MINOR})
SET(CPACK_PACKAGE_VERSION_PATCH ${ANALYTICS_VERSION_PATCH})
SET(CPACK_PACKAGE_CONTACT "Jun Tsai <jcai@ganshane.com>")
SET(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Analytics Native Library")

SET(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "amd64")

INCLUDE(CPack)

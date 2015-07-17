# the name of the target operating system
SET(CMAKE_SYSTEM_NAME Generic)

set(COMPILER_PREFIX pnacl)
#SET(PNACL_ROOT /Users/jcai/dev/nacl_sdk/pepper_43)
#SET(PNACL_TOOL_PREFIX mac)

SET(PNACL_TOOLCHAIN ${PNACL_ROOT}/toolchain/${PNACL_TOOL_PREFIX}_pnacl)
# which compilers to use for C and C++
find_program(CMAKE_C_COMPILER NAMES ${COMPILER_PREFIX}-clang)
find_program(CMAKE_CXX_COMPILER NAMES ${COMPILER_PREFIX}-clang++)
SET(CMAKE_RANLIB ${COMPILER_PREFIX}-ranlib)
SET(CMAKE_AR ${PNACL_TOOLCHAIN}/bin/${COMPILER_PREFIX}-ar CACHE FILEPATH "Archiver")

SET(CMAKE_FIND_ROOT_PATH  ${PNACL_ROOT} ${PNACL_TOOLCHAIN})

# adjust the default behaviour of the FIND_XXX() commands:
# search headers and libraries in the target environment, search 
# programs in the host environment
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)

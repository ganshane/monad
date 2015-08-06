#!/bin/bash
set -e
git clone --depth=1 https://github.com/ganshane/monad.git
cd monad
mkdir -p monad-analytics/build
cd monad-analytics/build
cmake -DCMAKE_BUILD_TYPE=Release -DENABLE_EM=on \
  -DCMAKE_TOOLCHAIN_FILE=/build/emsdk_portable/emscripten/tag-1.34.3/cmake/Modules/Platform/Emscripten.cmake \
  -DCMAKE_CXX_FLAGS="--bind -s NO_EXIT_RUNTIME=1" ..
make

cp em/monad* /dist

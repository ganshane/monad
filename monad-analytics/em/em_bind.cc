#ifndef MONAD_EM_BIND_H_
#define MONAD_EM_BIND_H_

#include "bit_set_wrapper.h"
#include "sparse_bit_set.h"
#include "sparse_bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"

#include <emscripten/bind.h>

using namespace emscripten;
float lerp(float a, float b, float t) {
  return (1 - t) * a + t * b;
}

EMSCRIPTEN_BINDINGS(my_module) {
    function("lerp", &lerp);
}
namespace monad {
  // Binding code
  EMSCRIPTEN_BINDINGS(analytics) {
      class_<SparseBitSetWrapper>("BitSetWrapper")
          .constructor()
          .function("NewSeg",&monad::SparseBitSetWrapper::NewSeg)
          .function("ReadIndice",&monad::SparseBitSetWrapper::ReadIndice)
          .function("CreateBit",&monad::SparseBitSetWrapper::CreateBit)
          .function("ReadBitBlock",&monad::SparseBitSetWrapper::ReadBitBlock)
          .function("ReadNonZero",&monad::SparseBitSetWrapper::ReadNonZero)
          .function("FastSet",&monad::SparseBitSetWrapper::FastSet)
          .function("Set",&monad::SparseBitSetWrapper::Set)
          .function("Commit", &monad::SparseBitSetWrapper::Commit)
          .function("FastGet", &monad::SparseBitSetWrapper::FastGet)
          .function("SetWeight", &monad::SparseBitSetWrapper::SetWeight)
          .function("BitCount", &monad::SparseBitSetWrapper::BitCount)
          //.class_function("InPlaceAnd",method, allow_raw_pointers());
          .class_function("InPlaceAnd",select_overload<SparseBitSetWrapper*(SparseBitSetWrapper**,size_t)>(&monad::SparseBitSetWrapper::InPlaceAnd), allow_raw_pointers());
  };
}

#endif //MONAD_EM_BIND_H_

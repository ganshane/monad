// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef MONAD_EM_BIND_H_
#define MONAD_EM_BIND_H_

#include <emscripten.h>
#include <emscripten/bind.h>
#include <emscripten/val.h>
#include <map>
#include <sstream>


#include "bit_set_wrapper.h"
#include "bit_set_app.h"
#include "collection_helper.h"
//#include "sparse_bit_set.h"
//#include "sparse_bit_set_wrapper.h"
#include "roaring_bit_set_wrapper.h"

#include "top_bit_set_wrapper.h"


using namespace emscripten;
namespace monad {
  //api url
  static std::string api_url;

  /**
   * val作为map的key,此对象作为key的比较函数
   */
  struct ValComp {
      bool operator()(const val &lhs, const val &rhs) const {
        std::string ltype(lhs.typeof().as<std::string>());
        std::string rtype(rhs.typeof().as<std::string>());
        if (ltype != rtype) {
          return ltype < rtype;
        }
        if (ltype == "string") {
          return lhs.as<std::string>() < rhs.as<std::string>();
        } else if (ltype == "number") {
          return lhs.as<int>() < rhs.as<int>();
        } else {
          return false;
        }
      }
  };
  typedef ValComp KEY;

  //操作RoaringBitSetWrapper的函数对象
  //typedef RoaringBitSetWrapper WRAPPER;
  //typedef CollectionInfo<WRAPPER> COLL_INFO;
  //typedef WRAPPER* (*Action)(WRAPPER**,size_t);
  static void ReportProgressOnOperation(const val& on_progress,const std::string& message){
    ((val)on_progress)(val(message));
  }

  /*
  class EmApp:public BitSetApp<int32_t,ValComp,RoaringBitSetWrapper>{
  public:
    EmApp(){

    }
  protected:
    void WebGet(const std::string& url,const std::string& parameter,WrapperCallback callback){
    }
  };
   */
}

#endif //MONAD_EM_BIND_H_

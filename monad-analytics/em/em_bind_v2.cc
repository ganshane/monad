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
  /**
   * val作为map的key,此对象作为key的比较函数
   */
  struct Int32Comp {
      bool operator()(const int32_t &lhs, const int32_t &rhs) const {
        return lhs < rhs;
      }
  };

  typedef RoaringBitSetWrapper WRAPPER;
  typedef int32_t KEY;
  typedef Int32Comp KEY_COMPARATOR;
  typedef CollectionInfo<int32_t,WRAPPER> COLL_INFO;



  class EmApp:public BitSetApp<KEY,KEY_COMPARATOR,WRAPPER>{
  public:
    EmApp(){}
    void InitJavascript(const std::string& api,const val& on_progress,const val& on_fail){
      _seq = 0;
      MessageCallback progress_callback =[=](const int32_t code,const std::string& message){
        //on_progress.call<void>("call",NULL,val(code),val(message));
        static_cast<val>(on_progress)(code,val(message));
      };
      MessageCallback fail_callback =[=](const int32_t code,const std::string& message){
        //on_progress.call<void>("call",NULL,val(code),val(message));
        static_cast<val>(on_fail)(code,val(message));
      };
      BitSetAppOptions options;
      options.api_url = const_cast<char*>(api.c_str());
      options.fail_callback = fail_callback;
      options.progress_callback = progress_callback;
      Init(options);
    }
    void TestFunctionQuery(const std::string& index,const std::string& q,WrapperCallback callback){

    }
    void WebGetTopN(const std::string& url,const std::string& parameter,const val& callback,const val& key,const val& data){
      CallbackBinding* binding = new CallbackBinding();
      binding->app = this;

      binding->topN.push_back(key);
      binding->topN.push_back(data);
      binding->topN.push_back(callback);

      emscripten_async_wget2_data(url.c_str(),
                                  "POST",
                                  parameter.c_str(),
                                  binding, true,
                                  &OnLoadIdLabel,
                                  &OnFail, &OnProgress);
    }
  protected:
    int32_t _seq;
    struct CallbackBinding{
      EmApp* app;
      WrapperCallback callback;
      int32_t weight;

      //topN
      std::vector<val> topN;

    };

    int32_t& NewKey(){
      ++_seq;
      return _seq;
    }
    void WebGet(const std::string& url,const std::string& parameter,WrapperCallback callback,const int32_t weight){
      CallbackBinding* binding = new CallbackBinding();
      binding->app = this;
      binding->callback = callback;
      binding->weight= weight;
      emscripten_async_wget2_data(url.c_str(),"POST",parameter.c_str(),binding,true,&OnLoadRoaringBitSetBuffer,&OnFail,&OnProgress);
    }
    /**
     * loading http buffer stream as RoaringBitSetWrapper
     * @param arg binding
     * @param buffer http buffer stream
     * @param size buffer size
     */
    static void OnLoadRoaringBitSetBuffer(unsigned,void* arg,void* buffer,unsigned size){
      char * * bb = (char**) &buffer;
      uint32_t offset = 0;

      CallbackBinding* binding = (CallbackBinding*)arg;
      EmApp* me = binding->app;

      COLL_INFO& coll_info= me->CreateBitSetWrapper();
      WRAPPER* wrapper = coll_info.GetOrCreateBitSetWrapper();
      uint32_t seg_len = ReadUint32(bb);
      for(int seg=0;seg<seg_len;seg++) {
        //regionId
        int regionId = ReadUint32(bb);
        ReadUint32(bb);//读取长度，在C中没有用
        *bb = *bb + wrapper->NewSeg(regionId,*bb);
      }
      wrapper->Commit();
//      printf("bitCount:%d size:%d\n",wrapper->BitCount(),wrapper->SegCount());
      wrapper->SetWeight(binding->weight);

      binding->callback(&coll_info);
      delete binding;

      /*
      COLL_INFO* ci = me->FindWrapper(coll_info.GetKey());
      wrapper = ci->GetOrCreateBitSetWrapper();
      printf("after bitCount:%d size:%d\n",wrapper->BitCount(),wrapper->SegCount());
      */

    }
    /**
 * 当加载id的具体数据时候执行的操作
 */
    static void OnLoadIdLabel(unsigned task_id,void* arg,void* buffer,size_t size){
      CallbackBinding* binding = (CallbackBinding*)arg;
      EmApp* me = binding->app;

      val key(binding->topN[0]);
      val data(binding->topN[1]);
      val callback(binding->topN[2]);

      std::string result((char*)buffer,size);
      std::stringstream ss(result);
      std::string item;
      int i = 0;
      int len = data["length"].as<int>();
      while (std::getline(ss, item, ',')) {
        data[i].set("id",val(item));
        if(++i >= len){
          break;
        }
      }

      callback(data,key);

      delete binding;
    }
    static void OnFail(unsigned task_id,void* arg,int32_t code,const char* msg){
      CallbackBinding* binding = (CallbackBinding*)arg;
      EmApp* me = binding->app;

      me->_options.fail_callback(code,msg);

      delete binding;
    }
    static void OnProgress(unsigned int , void * arg,int progress, int){
      CallbackBinding* binding = (CallbackBinding*)arg;
      EmApp* me = binding->app;


      std::ostringstream message;
      message <<  "loading " << progress << " ......" <<std::endl;
      me->_options.progress_callback(0,message.str());
    }
  };

  static EmApp app;

  static EmApp::WrapperCallback createJavascriptCallback(const val& callback){
    return [=](COLL_INFO* coll){
      val json=val::object();
      json.set("key",val(coll->GetKey()));
      json.set("count",val(coll->BitCount()));
      json.set("elapsed_time",val(coll->ElapsedTime()));
      static_cast<val>(callback)(json);
    };
  }
  static void CreateKeyCollection(const val&keys,std::vector<KEY>& dest){
    unsigned length = keys["length"].as<unsigned>();
    for(int i=0;i<length;i++){
      KEY key=keys[i].as<KEY>();
      dest.push_back(key);
    }
  }
  static void Init(const std::string& api_url,const val& on_progress,const val& on_fail){
    app.InitJavascript(api_url,on_progress,on_fail);
  }
  static void Query(const std::string& index,const std::string& q,const val& callback,const int32_t weight){
    EmApp::WrapperCallback wrapper_callback=createJavascriptCallback(callback);
    app.Query(index,q, wrapper_callback,weight);
  }
  static uint32_t ContainerSize(){
    return app.ContainerSize();
  }
  static void InPlaceAnd(const val& keys,const val& callback){
    std::vector<KEY> key_coll;
    CreateKeyCollection(keys,key_coll);
    EmApp::WrapperCallback wrapper_callback=createJavascriptCallback(callback);
    app.InPlaceAnd(key_coll,wrapper_callback);
  }
  static void InPlaceOr(const val& keys,const val& callback){
    std::vector<KEY> key_coll;
    CreateKeyCollection(keys,key_coll);
    EmApp::WrapperCallback wrapper_callback=createJavascriptCallback(callback);
    app.InPlaceOr(key_coll,wrapper_callback);
  }
  static void AndNot(const val& keys,const val& callback){
    std::vector<KEY> key_coll;
    CreateKeyCollection(keys,key_coll);
    EmApp::WrapperCallback wrapper_callback=createJavascriptCallback(callback);
    app.AndNot(key_coll,wrapper_callback);
  }
  static void InPlaceAndTop(const val& keys,const val& callback,const int32_t min_freq){
    std::vector<KEY> key_coll;
    CreateKeyCollection(keys,key_coll);
    EmApp::WrapperCallback wrapper_callback=createJavascriptCallback(callback);
    app.InPlaceAndTop(key_coll,min_freq,wrapper_callback);
  }
  static void InPlaceAndTopWithPositionMerged(const val& keys,const val& callback,const int32_t min_freq){
    std::vector<KEY> key_coll;
    CreateKeyCollection(keys,key_coll);
    EmApp::WrapperCallback wrapper_callback=createJavascriptCallback(callback);
    app.InPlaceAndTopWithPositionMerged(key_coll,min_freq,wrapper_callback);

  }
  void Top(const IdCategory category,const val& key,const uint32_t topN,const val& callback,const uint32_t offset){
    int32_t len=0;
    uint32_t query_topN = topN + offset;
    val data=val::array();
    std::ostringstream parameter;

    parameter <<"q=";

    //TOP
    COLL_INFO* ci =app.FindWrapper(key.as<KEY>());

    if(ci != NULL) {
      if (ci->IsTopCollection()) {
        RegionTopDoc **docs = ci->TopWrapperTop(query_topN, len);
        printf("top len:%d \n", len);
        for (int i = offset; i < len; i++) {
          TopDoc *top_doc = docs[i]->top_doc;
          val obj = val::object();
          obj.set("id", val(top_doc->doc));
          parameter << top_doc->doc << "@" << docs[i]->region << ",";
          obj.set("count", val(top_doc->freq));
          val p = val::array();
          //js中不能直接保存64bit的对象,拆分成两个int
          for (int j = 0; j < top_doc->position_len; j++) {
            p.call<void>("push",val((uint32_t) (top_doc->position[j] & 0x00000000fffffffL)));
            p.call<void>("push",val((uint32_t) (top_doc->position[j] >> 32)));
                         /*
            p[j * 2] = val((uint32_t) (top_doc->position[j] >> 32));
            p[j * 2 + 1] = val((uint32_t) (top_doc->position[j] & 0x00000000fffffffL));
                          */
          }

          obj.set("p", p);
          //printf("obj id:%d \n", top_doc->doc);

          data.set(i - offset, obj);
        }


        //clear
        for (int i = 0; i < len; i++)
          delete docs[i];
        delete[] docs;
      } else {
        RegionDoc **docs = ci->TopWrapper(query_topN, len);
        printf("sparse len :%d \n", len);
        for (int i = offset; i < len; i++) {
          uint32_t doc = docs[i]->doc;
          uint32_t region = docs[i]->region;
          val obj = val::object();
          obj.set("id", val(doc));
          obj.set("region", val(region));
          parameter << doc << "@" << region << ",";
//        parameter << doc << ",";
          data.set(i - offset, obj);
        }
        //clear
        for (int i = 0; i < len; i++)
          delete docs[i];
        delete[] docs;
      }
    }
    if(ci == NULL) {
      std::ostringstream os;
      os << "collection not found by key " << key.as<KEY>() << std::endl;
      app._options.fail_callback(404,os.str());
    }else if(len > offset) { //查到数据
      switch(category){
        case (IdCategory::Person):
          parameter << "&c=Person";
          break;
        case (IdCategory::Car):
          parameter << "&c=Car";
          break;
        case (IdCategory::Mobile):
          parameter << "&c=Mobile";
          break;
        case (IdCategory::Mac):
          parameter << "&c=Mac";
          break;
        case (IdCategory::QQ):
          parameter << "&c=QQ";
          break;
        case (IdCategory::WeiXin):
          parameter << "&c=WeiXin";
          break;
        default:
          char message[100];
          sprintf(message,"category not found by :%d",category);
          app._options.fail_callback(404,std::string(message));
          return;
      }
      printf(" paramter %s \n",parameter.str().c_str());



      std::string query_api(app._options.api_url);
      query_api.append("/analytics/IdConverterApi");

      app.WebGetTopN(query_api,parameter.str(),callback,key,data);
    }else{
      ((val)callback)(data,key);
    }
  }
  static val GetCollectionProperties(const val& key){
    COLL_INFO* ci = app.FindWrapper(key.as<KEY>());
    val result = val::object();
    if(ci){
      result.set("count",val(ci->BitCount()));
      result.set("key",val(key));
      result.set("is_top",val(false));
    }else{
      std::ostringstream os;
      os << "collection not found by " << key.as<KEY>() << std::endl;
      app._options.fail_callback(404,os.str());
    }
    return result;
  }
  WRAPPER* CreateBitSetWrapper(){
    COLL_INFO& coll_info = app.CreateBitSetWrapper();
    return coll_info.GetOrCreateBitSetWrapper();
  }
  /**
 * 清空所有的集合
 */
  void ClearAllCollection(){
    app.ClearContainer();
  }
  /**
   * 清空某一个集合
   */
  void ClearCollection(const val& key){
    app.RemoveWrapper(key.as<KEY>());
  }
  // Binding code
  EMSCRIPTEN_BINDINGS(analytics2) {
      enum_<IdCategory>("IdCategory")
          .value("Person", IdCategory::Person)
          .value("Mobile", IdCategory::Mobile)
          .value("Mac", IdCategory::Mac)
          .value("QQ", IdCategory::QQ)
          .value("WeiXin", IdCategory::WeiXin)
          .value("Car", IdCategory::Car);

      function("Init", &Init);
      function("query", &Query);
      //function("fullTextQuery", &FullTextQuery);
      function("ContainerSize", &ContainerSize);
      function("inPlaceAnd", &InPlaceAnd);
      function("inPlaceOr", &InPlaceOr);
      function("andNot", &AndNot);
      function("inPlaceAndTop", &InPlaceAndTop);
      function("inPlaceAndTopWithPositionMerged", &InPlaceAndTopWithPositionMerged);
      function("top", &Top);
      function("clearAllCollection", &ClearAllCollection);

      function("clearCollection", &ClearCollection);
      function("getCollectionProperties", &GetCollectionProperties);
      function("createBitSetWrapper", &CreateBitSetWrapper, allow_raw_pointers());


      class_<WRAPPER>("BitSetWrapper")
      .constructor()
//          .function("NewSeg",&monad::WRAPPER::NewSeg)
//          .function("ReadIndice",&monad::WRAPPER::ReadIndice)
//          .function("CreateBit",&monad::WRAPPER::CreateBit)
//          .function("ReadBitBlock",&monad::WRAPPER::ReadBitBlock)
//          .function("ReadNonZero",&monad::WRAPPER::ReadNonZero)
      .function("FastSet", &monad::WRAPPER::FastSet)
      .function("Set", &monad::WRAPPER::Set)
      .function("Commit", &monad::WRAPPER::Commit)
      .function("FastGet", &monad::WRAPPER::FastGet)
      .function("SetWeight", &monad::WRAPPER::SetWeight)
      .function("BitCount", &monad::WRAPPER::BitCount);
      //.class_function("InPlaceAnd",method, allow_raw_pointers());
      //.class_function("InPlaceAnd",select_overload<WRAPPER*(WRAPPER**,size_t)>(&monad::WRAPPER::InPlaceAnd), allow_raw_pointers());
  };
};

#endif //MONAD_EM_BIND_H_

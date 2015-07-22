#ifndef MONAD_EM_BIND_H_
#define MONAD_EM_BIND_H_

#include <emscripten.h>
#include <emscripten/bind.h>
#include <emscripten/val.h>
#include <map>
#include <sstream>


#include "bit_set_wrapper.h"
#include "sparse_bit_set.h"
#include "sparse_bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"


using namespace emscripten;
namespace monad {
  static uint32_t ReadUint32(uint8_t** buffer){
    uint8_t* bb = *buffer;
    uint32_t i = 0;
    i |= bb[0] << 24;
    i |= bb[1] << 16;
    i |= bb[2] << 8;
    i |= bb[3] ;
    *buffer = bb+4;

    return i;
  }
  static uint64_t ReadUint64(uint8_t** buffer){
    uint64_t hi = ReadUint32(buffer);
    uint64_t lo = ReadUint32(buffer);
    return  (hi << 32) | lo;
  }
  static void CallJavascriptFunction(void* args,uint32_t key,SparseBitSetWrapper* wrapper){
    if(args) {
      std::vector<std::string> args_ = *(std::vector<std::string>*)args;
      std::stringstream data;
      data << args_[0] << "(";
      data << "{key:" << key << ",count:" << wrapper->BitCount() << "}";
      data << ")";
      std::string js = data.str();
      //printf("js function:%s \n", js.c_str());
      emscripten_run_script(js.c_str());
      free(args);
    }
  }

  //记录所有的wrapper
  static uint32_t seq(0);
  static std::map<uint32_t,SparseBitSetWrapper*> container;
  static std::string api_url;
  void OnLoadSparseBitSetBuffer(unsigned xx,void* arg,void* buffer,unsigned size){
    uint8_t** bb = (uint8_t**) &buffer;
    uint32_t offset = 0;

    int length = ReadUint32(bb);
    int nonZeroLongCount = ReadUint32(bb);
    SparseBitSetWrapper* wrapper = new SparseBitSetWrapper();
    uint32_t id = seq ++;
    container.insert(std::pair<uint32_t,SparseBitSetWrapper*>(id,wrapper));

    wrapper->NewSeg(1,length);
    wrapper->ReadNonZero(nonZeroLongCount);

    int arrayLength =  ReadUint32(bb);
    for(int i=0;i<arrayLength;i++){
      wrapper->ReadIndice(i,ReadUint64(bb));
    }
    int bitLength = 0;
    for(int i=0;i<arrayLength;i++){
      bitLength = ReadUint32(bb);
      if(bitLength > 0)
        wrapper->CreateBit(i,bitLength);

      for(int j=0;j<bitLength;j++){
        wrapper->ReadBitBlock(i,j,ReadUint64(bb));
      }
    }

    uint64_t ramBytesUsed = ReadUint64(bb);
    //printf("bitCount:%d\n",wrapper->BitCount());

    CallJavascriptFunction(arg,id,wrapper);
  }
  void SetApiUrl(const std::string& api){
    api_url.assign(api);
  }
  void OnFail(unsigned task_id,void* args,int32_t code,const char* msg){
    if(args){
      std::vector<std::string> args_ = *(std::vector<std::string>*)args;
      std::stringstream data;
      data << args_[1] <<"(\" code:" << code <<",msg:"<<msg<<"\")";
      std::string js = data.str();
      //printf("js function:%s \n", js.c_str());
      emscripten_run_script(js.c_str());
      free(args);
    }
  }

  std::vector<std::string> *CreateCallArgs(const std::string &callback, const std::string &on_fail);

  void Query(val parameter,const std::string& callback,const std::string& on_fail){
    std::vector<std::string> *arg = CreateCallArgs(callback, on_fail);

    std::string p;
    p.append("i=").append(parameter["i"].as<std::string>());
    p.append("&");
    p.append("q=").append(parameter["q"].as<std::string>());
    std::string query_api(api_url);
    query_api.append("/analytics/IdSearcher");
    emscripten_async_wget2_data(query_api.c_str(),"POST",p.c_str(),(void*)arg,true,&OnLoadSparseBitSetBuffer,&OnFail,NULL);
  }

  std::vector<std::string> *CreateCallArgs(const std::string &callback, const std::string &on_fail) {
    std::vector<std::string>* arg = new std::vector<std::string>();
    arg->push_back(callback);
    arg->push_back(on_fail);
    return arg;
  };
  uint32_t ContainerSize(){
    return container.size();
  }
  SparseBitSetWrapper* FindWrapper(uint32_t key){
    std::map<uint32_t ,SparseBitSetWrapper*>::iterator it;
    it = container.find(key);
    if(it == container.end()){
      return NULL;
    }else{
      return it->second;
    }
  }
  static SparseBitSetWrapper** CreateWrapperCollection(val v,std::vector<std::string>* args,uint32_t* len){
    unsigned length = v["length"].as<unsigned>();
    SparseBitSetWrapper** collections = new SparseBitSetWrapper*[length];
    //printf("length:%d \n",length);
    SparseBitSetWrapper* wrapper;
    for(unsigned i=0;i<length;i++){
      uint32_t key = v[i].as<uint32_t>();
      wrapper = FindWrapper(key);
      if(wrapper == NULL){
        char message[100];
        sprintf(message,"collection not found by key :%d",key);
        OnFail(0,args,51,message);
        return NULL;
      }
      collections[i]=wrapper;
      //printf("key:%d bitCount:%d \n",key,collections[i]->BitCount());
    }
    *len = length;

    return collections;
  }
  void InPlaceAnd(val v,const std::string& callback,const std::string& on_fail){
    std::vector<std::string> *args = CreateCallArgs(callback, on_fail);
    uint32_t length=0;
    SparseBitSetWrapper** collections = CreateWrapperCollection(v,args,&length);
    if(collections == NULL)
      return;

    uint32_t id = seq ++;
    SparseBitSetWrapper* wrapper= SparseBitSetWrapper::InPlaceAnd(collections,length);
    delete [] collections;
    container.insert(std::pair<uint32_t,SparseBitSetWrapper*>(id,wrapper));
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    CallJavascriptFunction(args,id,wrapper);
  }
  void InPlaceOr(val v,const std::string& callback,const std::string& on_fail){
    std::vector<std::string> *args = CreateCallArgs(callback, on_fail);

    uint32_t length=0;
    SparseBitSetWrapper** collections = CreateWrapperCollection(v,args,&length);
    if(collections == NULL)
      return;

    uint32_t id = seq ++;
    SparseBitSetWrapper* wrapper= SparseBitSetWrapper::InPlaceOr(collections,length);
    container.insert(std::pair<uint32_t,SparseBitSetWrapper*>(id,wrapper));
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    CallJavascriptFunction(args,id,wrapper);
  }
  // Binding code
  EMSCRIPTEN_BINDINGS(analytics) {
      function("SetApiUrl", &SetApiUrl);
      function("query", &Query);
      function("ContainerSize", &ContainerSize);
      function("inPlaceAnd", &InPlaceAnd);
      function("inPlaceOr", &InPlaceAnd);
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

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
  //api url
  static std::string api_url;
  struct ValComp {
    //bool operator() (const char& lhs, const char& rhs) const
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
  //记录SparseBitSetWrapper的容器
  typedef ValComp KEY;
  static std::map<val,SparseBitSetWrapper*,KEY> container;
  static std::map<val,TopBitSetWrapper*,KEY> top_container;

  //操作SparseBitSetWrapper的函数
  typedef SparseBitSetWrapper* (*Action)(SparseBitSetWrapper**,size_t);

  inline static uint32_t ReadUint32(uint8_t** buffer){
    uint8_t* bb = *buffer;
    uint32_t i = 0;
    i |= bb[0] << 24;
    i |= bb[1] << 16;
    i |= bb[2] << 8;
    i |= bb[3] ;
    *buffer = bb+4;

    return i;
  }
  inline static uint64_t ReadUint64(uint8_t** buffer){
    uint64_t hi = ReadUint32(buffer);
    uint64_t lo = ReadUint32(buffer);
    return  (hi << 32) | lo;
  }
  template<typename T>
  inline static T* FindWrapper(std::map<val,T*,KEY>& map, const val& key){
    typename std::map<val ,T*>::iterator it;
    printf("find wrapper container size:%d \n",map.size());
    it = map.find(key);
    if(it == map.end()){
      return NULL;
    }else{
      return it->second;
    }
  }
  template<typename T>
  inline static void CleanrContainer(std::map<val,T*,KEY>& map){
    typename std::map<val ,T*>::iterator it;
    it = map.begin();
    map.erase(it,map.end());
  }
  template<typename T>
  inline static void RemoveWrapper(std::map<val,T*,KEY>& map, const val& key){
    T* wrapper = FindWrapper(map,key);
    if(wrapper){
      map.erase(key);
      delete wrapper;
    }
  }
  template<typename T>
  inline static void CallJavascriptFunction(std::map<val,T*,KEY>& map,void* args,T* wrapper){
    std::vector<val> args_ = *(std::vector<val>*)args;

    val id = args_[0];
    T* old_wrapper = FindWrapper(map,id);
    if(old_wrapper){
      map.erase(id);
      //printf("old wrapper exsists with key :%d! \n",id);
      delete old_wrapper;
    }
    map.insert(std::pair<val,T*>(id,wrapper));
    //printf("insert wrapper id %d \n",id);

    val json=val::object();
    json.set("key",val(id));
    json.set("count",val(wrapper->BitCount()));
    args_[1](json);
    delete (std::vector<val>*)args;
  }
  void OnFail(unsigned task_id,void* args,int32_t code,const char* msg){
    std::vector<val> args_ = *(std::vector<val>*)args;
    std::stringstream message;
    message << " code:" << code <<",msg:"<<msg;
    args_[2](val(message.str()));
    delete (std::vector<val>*)args;
  }
  /**
   * 当加载id的具体数据时候执行的操作
   */
  void OnLoadIdLable(unsigned task_id,void* args,void* buffer,size_t size){
    std::vector<val> args_ = *(std::vector<val>*)args;
    val data=args_[3];
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

    args_[1](data,args_[0]);
  }
  template<typename T>
  static T** CreateWrapperCollection(std::map<val,T*,KEY>& map,const val& keys,std::vector<val>* args,uint32_t* len){
    unsigned length = keys["length"].as<unsigned>();
    T** collections = new T*[length];
    //printf("length:%d \n",length);
    T* wrapper;
    for(unsigned i=0;i<length;i++){
      val key = keys[i];
      wrapper = FindWrapper(map,key);
      if(wrapper == NULL){
        char message[100];
        //sprintf(message,"collection not found by key :%d",key);
        OnFail(0,args,51,message);
        return NULL;
      }
      collections[i]=wrapper;
      //printf("key:%d bitCount:%d \n",key,collections[i]->BitCount());
    }
    *len = length;

    return collections;
  }

  std::vector<val> *CreateCallArgs(const val& key,const val& callback, const val& on_fail) {
    std::vector<val>* arg = new std::vector<val>();
    arg->push_back(key);
    arg->push_back(callback);
    arg->push_back(on_fail);
    return arg;
  };
  void DoOperator(const Action action,const val& keys,const val& new_key,const val& callback,const val& on_fail){
    std::vector<val> *args = CreateCallArgs(new_key,callback, on_fail);

    uint32_t length=0;
    SparseBitSetWrapper** collections = CreateWrapperCollection<SparseBitSetWrapper>(container,keys,args,&length);
    if(collections == NULL)
      return;

    //SparseBitSetWrapper* wrapper= SparseBitSetWrapper::InPlaceOr(collections,length);
    SparseBitSetWrapper* wrapper= action(collections,length);
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    CallJavascriptFunction<SparseBitSetWrapper>(container,args,wrapper);
  }

  void OnLoadSparseBitSetBuffer(unsigned xx,void* arg,void* buffer,unsigned size){
    uint8_t** bb = (uint8_t**) &buffer;
    uint32_t offset = 0;

    int length = ReadUint32(bb);
    int nonZeroLongCount = ReadUint32(bb);
    SparseBitSetWrapper* wrapper = new SparseBitSetWrapper();

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

    CallJavascriptFunction(container,arg,wrapper);
  }
  void SetApiUrl(const std::string& api){
    api_url.assign(api);
  }


  void Query(const val& parameter,const val& new_key,const val& callback,const val& on_fail){
    std::vector<val> *arg = CreateCallArgs(new_key,callback, on_fail);

    std::string p;
    p.append("i=").append(parameter["i"].as<std::string>());
    p.append("&");
    p.append("q=").append(parameter["q"].as<std::string>());
    std::string query_api(api_url);
    query_api.append("/analytics/IdSearcher");
    emscripten_async_wget2_data(query_api.c_str(),"POST",p.c_str(),(void*)arg,true,&OnLoadSparseBitSetBuffer,&OnFail,NULL);
  }

  uint32_t ContainerSize(){
    return container.size();
  }
  void InPlaceAnd(const val& keys,const val& new_key,const val& callback,const val& on_fail){
    DoOperator(&SparseBitSetWrapper::InPlaceAnd,keys,new_key,callback,on_fail);
  }
  void InPlaceOr(const val& keys,const val& new_key,const val& callback,const val& on_fail){
    DoOperator(&SparseBitSetWrapper::InPlaceOr,keys,new_key,callback,on_fail);
  }
  void AndNot(const val& keys,const val& new_key,const val& callback,const val& on_fail){
    DoOperator(&SparseBitSetWrapper::InPlaceNot,keys,new_key,callback,on_fail);
  }
  void InPlaceAndTop(const val& keys,const val& new_key,const val& callback,const int32_t min_freq,const val& on_fail){
    std::vector<val> *args = CreateCallArgs(new_key,callback, on_fail);

    uint32_t length=0;
    SparseBitSetWrapper** collections = CreateWrapperCollection<SparseBitSetWrapper>(container,keys,args,&length);
    if(collections == NULL)
      return;

    TopBitSetWrapper* wrapper= SparseBitSetWrapper::InPlaceAndTop(collections,length,min_freq);
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    CallJavascriptFunction<TopBitSetWrapper>(top_container,args,wrapper);
  }
  void InPlaceAndTopWithPositionMerged(const val& keys,const val& new_key,const val& callback,const int32_t min_freq,const val& on_fail){
    std::vector<val> *args = CreateCallArgs(new_key,callback, on_fail);

    uint32_t length=0;
    TopBitSetWrapper** collections = CreateWrapperCollection<TopBitSetWrapper>(top_container,keys,args,&length);
    if(collections == NULL)
      return;

    TopBitSetWrapper* wrapper= SparseBitSetWrapper::InPlaceAndTopWithPositionMerged(collections,length,min_freq);
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    CallJavascriptFunction<TopBitSetWrapper>(top_container,args,wrapper);
  }
  void Top(const val& key,const uint32_t topN,const val& callback,const uint32_t offset,const val& on_fail) {
    int32_t len=0;
    uint32_t query_topN = topN + offset;
    val data=val::array();
    std::stringstream parameter;

    parameter <<"q=";

    //TOP
    TopBitSetWrapper* wrapper=FindWrapper(top_container,key);

    if(wrapper != NULL) {
      RegionTopDoc** docs = wrapper->Top(query_topN,len);
      printf("top len:%d \n",len);
      for (int i = offset; i < len; i++) {
        TopDoc *top_doc = docs[i]->top_doc;
        val obj = val::object();
        obj.set("id", val(top_doc->doc));
        parameter << top_doc->doc << ",";
        obj.set("count", val(top_doc->freq));
        val p = val::array();
        for (int j = 0; j < top_doc->position_len; j++) {
          p[j * 2] = val((uint32_t) (top_doc->position[j] >> 32));
          p[j * 2 + 1] = val((uint32_t) (top_doc->position[j] & 0x00000000fffffffL));
        }

        obj.set("p", val(top_doc->freq));
        printf("obj id:%d \n", top_doc->doc);

        data.set(i - offset, obj);
      }


      //clear
      for (int i = 0; i < len; i++)
        delete docs[i];
      delete[] docs;
    }

    SparseBitSetWrapper* sparse_wrapper = FindWrapper(container,key);
    if(sparse_wrapper != NULL){
      RegionDoc** docs = sparse_wrapper->Top(query_topN,len);
      printf("sparse len :%d \n",len);
      for (int i = offset; i < len; i++) {
        uint32_t doc = docs[i]->doc;
        val obj = val::object();
        obj.set("id",val(doc));
        parameter << doc << ",";
        data.set(i - offset, obj);
      }
      //clear
      for (int i = 0; i < len; i++)
        delete docs[i];
      delete[] docs;
    }

    if(sparse_wrapper == NULL && wrapper == NULL) {
      char message[100];
      sprintf(message,"collection not found by key :%s",key.as<std::string>().c_str());
      ((val)on_fail)(val(std::string(message)));
    }else if(len > offset) {
      parameter << "&c=Person";
      std::vector<val> *args = new std::vector<val>();
      args->push_back(key);
      args->push_back(callback);
      args->push_back(on_fail);
      args->push_back(data);

      std::string query_api(api_url);
      query_api.append("/analytics/IdConverterApi");
      emscripten_async_wget2_data(query_api.c_str(),
                                  "POST",
                                  parameter.str().c_str(),
                                  (void *) args, true,
                                  &OnLoadIdLable,
                                  &OnFail, NULL);
    }else{
      ((val)callback)(data,key);
    }
  }
  void ClearAllCollection(){
    CleanrContainer(container);
    CleanrContainer(top_container);
  }
  void ClearCollection(const val& key){
    RemoveWrapper(container,key);
    RemoveWrapper(top_container,key);
  }
  val GetCollectionProperties(const val& key){
    SparseBitSetWrapper* wrapper = FindWrapper(container,key);
    val result = val::object();
    if(wrapper){
      result.set("count",val(wrapper->BitCount()));
      result.set("key",val(key));
      result.set("is_top",val(false));
    }
    TopBitSetWrapper* top_wrapper = FindWrapper(top_container,key);
    if(top_wrapper){
      result.set("count",val(top_wrapper->BitCount()));
      result.set("key",val(key));
      result.set("is_top",val(true));
    }
    return result;
  }


  // Binding code
  EMSCRIPTEN_BINDINGS(analytics) {
      function("SetApiUrl", &SetApiUrl);
      function("query", &Query);
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

      /*
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
       */
  };
}

#endif //MONAD_EM_BIND_H_

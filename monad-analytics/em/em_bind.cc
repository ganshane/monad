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
#include "collection_info.h"
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
  //记录BitSetWrapper的容器
  static std::map<val,CollectionInfo*,KEY> container;

  //操作RoaringBitSetWrapper的函数对象
  typedef RoaringBitSetWrapper* (*Action)(RoaringBitSetWrapper**,size_t);
  void ClearCollection(const val& key);

  /**
   * 从buffer中读取一个32bit的整数
   */
  inline static uint32_t ReadUint32(char** buffer){
    char* bb = *buffer;
    uint32_t i = 0;
    i |= bb[0] << 24;
    i |= bb[1] << 16;
    i |= bb[2] << 8;
    i |= bb[3] ;
    *buffer = bb+4;

    return i;
  }
  /**
   * 从buffer重读取一个64bit的数
   */
  inline static uint64_t ReadUint64(char** buffer){
    uint64_t hi = ReadUint32(buffer);
    uint64_t lo = ReadUint32(buffer);
    return  (hi << 32) | lo;
  }
  /**
   * 查找map中的wrapper对象
   */
  template<typename T>
  inline static T* FindWrapper(std::map<val,T*,KEY>& map, const val& key){
    typename std::map<val ,T*>::iterator it;
//    printf("find wrapper container size:%d \n",map.size());
    it = map.find(key);
    if(it == map.end()){
      return NULL;
    }else{
      return it->second;
    }
  }
  /**
   * 清空某一个容器
   */
  template<typename T>
  inline static void ClearContainer(std::map<val,T*,KEY>& map){
    for (typename std::map<val,T*>::iterator it=map.begin(); it!=map.end(); ++it){
      delete it->second;
    }
    typename std::map<val ,T*>::iterator it;
    it = map.begin();
    map.erase(it,map.end());
  }
  /**
   * 通过给定的key,删除某一个wrapper
   */
  template<typename T>
  inline static void RemoveWrapper(std::map<val,T*,KEY>& map, const val& key){
    T* wrapper = FindWrapper(map,key);
    if(wrapper){
      map.erase(key);
      delete wrapper;
    }
  }
  /**
   * 回调javascript中函数
   */
  template<typename T>
  inline static void CallJavascriptFunction(void* args,T* wrapper){
    std::vector<val> args_ = *(std::vector<val>*)args;

    val id = args_[0];
    //先删除同key的集合
    ClearCollection(id);

    CollectionInfo* info = new CollectionInfo(wrapper);
    container.insert(std::pair<val,CollectionInfo*>(id,info));
    //printf("insert wrapper id %d \n",id);

    val json=val::object();
    json.set("key",val(id));
    json.set("count",val(wrapper->BitCount()));
    json.set("elapsed_time",val(wrapper->elapsed_time));
    args_[1](json);
    delete (std::vector<val>*)args;
  }
  void OnFail(unsigned task_id,void* args,int32_t code,const char* msg){
    std::vector<val> args_ = *(std::vector<val>*)args;
    std::stringstream message;
    message << " code:" << code;
    if(msg){
      message << "message:" << msg;
    }
    args_[2](val(message.str()));
    delete (std::vector<val>*)args;
  }
  static void OnProgress(unsigned int , void * args,int progress, int){
    std::vector<val> args_ = *(std::vector<val>*)args;
    std::stringstream message;
    message <<  "loading ..." << progress ;

    args_[3](val(message.str()));
  }
  /**
   * 当加载id的具体数据时候执行的操作
   */
  void OnLoadIdLabel(unsigned task_id,void* args,void* buffer,size_t size){
    std::vector<val> args_ = *(std::vector<val>*)args;
    val data=args_[4];
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
  /**
   * 通过给定的key来构造一个wrapper集合
   */
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
        delete [] collections;
        char message[100];
        std::string type = key.typeof().as<std::string>();
        //printf("type:%s \n",type.c_str());
        if(type == "number")
          sprintf(message,"collection not found by key %d",key.as<int>());
        else
          sprintf(message,"collection not found by key %s",key.as<std::string>().c_str());
        OnFail(0,args,51,message);
        return NULL;
      }
      collections[i]=wrapper;
      //printf("key:%d bitCount:%d \n",key,collections[i]->BitCount());
    }
    *len = length;

    return collections;
  }

  template<typename HEAD>
  void InternalCreateCallArgs(std::vector<HEAD>* data,const HEAD& head) {
    data->push_back(head);
  }
  template<typename HEAD,typename... VAL>
  void InternalCreateCallArgs(std::vector<HEAD>* data,const HEAD& head,VAL& ... args) {
    InternalCreateCallArgs(data,head);
    InternalCreateCallArgs(data,args...);
  }
  /**
   * 创建回调使用的参数,正常情况下
   * 0: key
   * 1: callback
   * 2: onFail
   * 3: onProgress http 请求时刻需要
   */
  template<typename... VAL>
  std::vector<val> *CreateCallArgs(const VAL& ... args) {
    std::vector<val>* arg = new std::vector<val>();
    InternalCreateCallArgs(arg,args...);
    return arg;
  };
  static void ReportProgressOnOperation(const val& on_progress,const std::string& message){
    ((val)on_progress)(val(message));
  }
  void DoOperator(const Action action,const val& keys,const val& new_key,const val& callback,const val& on_fail,const val& on_progress){
    std::vector<val> *args = CreateCallArgs(new_key,callback, on_fail);

    uint32_t length=0;
    ReportProgressOnOperation(on_progress,"creating collection info ...");
    CollectionInfo** collections = CreateWrapperCollection<CollectionInfo>(container,keys,args,&length);
    if(collections == NULL)
      return;

    ReportProgressOnOperation(on_progress,"creating wrapper collection ...");
    printf("length is :%d \n",length);

    RoaringBitSetWrapper** wrappers = new RoaringBitSetWrapper*[length];
    for(int i=0;i<length;i++){
      wrappers[i]= collections[i]->GetOrCreateBitSetWrapper();
    }

    ReportProgressOnOperation(on_progress,"execute ...");
    RoaringBitSetWrapper* wrapper= action(wrappers,length);

    //clear tmp wrapper
    for(int i=0;i<length;i++){
      collections[i]->ClearBitSetWrapper();
    }
    delete[] wrappers;
    delete[] collections;
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    ReportProgressOnOperation(on_progress,"call callback function");
    CallJavascriptFunction<RoaringBitSetWrapper>(args,wrapper);
  }

  /**
   *
   */
  static void OnLoadFullTextQueryResult(unsigned xx,void* args,void*buffer, unsigned size){
    std::vector<val> args_ = *(std::vector<val>*)args;

    args_[1](val(std::string((char*)buffer,size)));
    delete (std::vector<val>*)args;
  }
  /**
   * 从http传输过来的buffer中解析出来RoaringBitSetWrapper
   */
  static void OnLoadRoaringBitSetBuffer(unsigned xx,void* arg,void* buffer,unsigned size){
    char * * bb = (char**) &buffer;
    uint32_t offset = 0;


    RoaringBitSetWrapper* wrapper = new RoaringBitSetWrapper();
    uint32_t seg_len = ReadUint32(bb);
    for(int seg=0;seg<seg_len;seg++) {
      //regionId
      int regionId = ReadUint32(bb);
      *bb = *bb + wrapper->NewSeg(regionId,*bb);
    }

    /*
    SparseBitSetWrapper* wrapper = new SparseBitSetWrapper();
    //read length
    uint32_t seg_len = ReadUint32(bb);
    for(int seg=0;seg<seg_len;seg++) {
      //regionId
      int regionId = ReadUint32(bb);

      int length = ReadUint32(bb);
      int nonZeroLongCount = ReadUint32(bb);

      wrapper->NewSeg(regionId, length);
      wrapper->ReadNonZero(nonZeroLongCount);

      int arrayLength = ReadUint32(bb);
      for (int i = 0; i < arrayLength; i++) {
        wrapper->ReadIndice(i, ReadUint64(bb));
      }
      int bitLength = 0;
//      printf("regionId %d length:%d nonzero:%d bitlength :%d \n",regionId,length,nonZeroLongCount,bitLength);
      for (int i = 0; i < arrayLength; i++) {
        bitLength = ReadUint32(bb);
        if (bitLength > 0)
          wrapper->CreateBit(i, bitLength);

        for (int j = 0; j < bitLength; j++) {
          wrapper->ReadBitBlock(i, j, ReadUint64(bb));
        }
      }

      uint64_t ramBytesUsed = ReadUint64(bb);
//      printf("finish... \n");
    }
     */
    wrapper->Commit();
//    printf("bitCount:%d size:%d\n",wrapper->BitCount(),wrapper->SegCount());
    std::vector<val> args_ = *(std::vector<val>*)arg;
    uint32_t weight = args_[4].as<uint32_t>();
    wrapper->SetWeight(weight);

    CallJavascriptFunction(arg,wrapper);
  }
  /**
   * 设置全局的API_URL
   */
  void SetApiUrl(const std::string& api){
    api_url.assign(api);
  }

  void FullTextQuery(const val& parameter,const val& callback,const val& on_fail,const val& on_progress){
//  void FullTextQuery(const val& parameter,const val& callback,const val& on_fail,const val& on_progress){
    //TODO why first argument must be number
    std::vector<val> *arg = CreateCallArgs(val(1),callback, on_fail,on_progress);
    //std::vector<val> *arg = CreateCallArgs(callback, on_fail,on_progress);
    std::string p;
    p.append("i=").append(parameter["i"].as<std::string>());
    p.append("&");
    p.append("q=").append(parameter["q"].as<std::string>());
    std::string query_api(api_url);
    query_api.append("/search");
    //query_api.append("/analytics/IdSearcher");
    emscripten_async_wget2_data(query_api.c_str(),"POST",p.c_str(),(void*)arg,true,&OnLoadFullTextQueryResult,&OnFail,&OnProgress);
  }
  void Query(const val& parameter,const val& new_key,const val& callback,const val& on_fail,const val& on_progress,const uint32_t weight){
    std::vector<val> *arg = CreateCallArgs(new_key,callback, on_fail,on_progress,val(weight));

    std::string p;
    p.append("i=").append(parameter["i"].as<std::string>());
    p.append("&");
    p.append("q=").append(parameter["q"].as<std::string>());
    std::string query_api(api_url);
    query_api.append("/analytics/IdSearcher");
    emscripten_async_wget2_data(query_api.c_str(),"POST",p.c_str(),(void*)arg,true,&OnLoadRoaringBitSetBuffer,&OnFail,&OnProgress);
  }

  uint32_t ContainerSize(){
    return container.size();
  }
  void InPlaceAnd(const val& keys,const val& new_key,const val& callback,const val& on_fail,const val& on_progress){
    DoOperator(&RoaringBitSetWrapper::InPlaceAnd,keys,new_key,callback,on_fail,on_progress);
  }
  void InPlaceOr(const val& keys,const val& new_key,const val& callback,const val& on_fail,const val& on_progress){
    DoOperator(&RoaringBitSetWrapper::InPlaceOr,keys,new_key,callback,on_fail,on_progress);
  }
  void AndNot(const val& keys,const val& new_key,const val& callback,const val& on_fail,const val& on_progress){
    DoOperator(&RoaringBitSetWrapper::InPlaceNot,keys,new_key,callback,on_fail,on_progress);
  }
  void InPlaceAndTop(const val& keys,const val& new_key,const val& callback,const int32_t min_freq,const val& on_fail,const val& on_progress){
    std::vector<val> *args = CreateCallArgs(new_key,callback, on_fail);

    ReportProgressOnOperation(on_progress,"creating wrapper collection ...");
    uint32_t length=0;
    CollectionInfo** collections = CreateWrapperCollection<CollectionInfo>(container,keys,args,&length);
    if(collections == NULL)
      return;
    RoaringBitSetWrapper** wrappers = new RoaringBitSetWrapper*[length]();
    for(int i=0;i<length;i++)
      wrappers[i]=collections[i]->GetOrCreateBitSetWrapper();

    ReportProgressOnOperation(on_progress,"executing...");
    TopBitSetWrapper* wrapper= RoaringBitSetWrapper::InPlaceAndTop(wrappers,length,min_freq);
    printf("andTop result bitCount:%d \n",wrapper->BitCount());
    for(int i=0;i<length;i++)
      collections[i]->ClearBitSetWrapper();

    delete[] wrappers;
    delete [] collections;
    ReportProgressOnOperation(on_progress,"call callback function...");
    CallJavascriptFunction<TopBitSetWrapper>(args,wrapper);
  }
  void InPlaceAndTopWithPositionMerged(const val& keys,const val& new_key,const val& callback,const int32_t min_freq,const val& on_fail,const val& on_progress){
    std::vector<val> *args = CreateCallArgs(new_key,callback, on_fail,on_progress);

    ReportProgressOnOperation(on_progress,"creating top wrapper collection ...");
    uint32_t length=0;
    CollectionInfo** collections = CreateWrapperCollection<CollectionInfo>(container,keys,args,&length);
    if(collections == NULL)
      return;
    TopBitSetWrapper** wrappers = new TopBitSetWrapper*[length]();
    for(int i=0;i<length;i++) {
      TopBitSetWrapper* tbsw = collections[i]->GetTopBitSetWrapper();
      if(tbsw) wrappers[i]=tbsw;
      else {
        delete[] wrappers;
        return;
      }
    }

    ReportProgressOnOperation(on_progress,"executing...");
    TopBitSetWrapper* wrapper= RoaringBitSetWrapper::InPlaceAndTopWithPositionMerged(wrappers,length,min_freq);

    delete [] wrappers;
    delete[] collections;
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    ReportProgressOnOperation(on_progress,"call callback function...");
    CallJavascriptFunction<TopBitSetWrapper>(args,wrapper);
  }
  void Top(const IdCategory category,const val& key,const uint32_t topN,const val& callback,const uint32_t offset,const val& on_fail,const val& on_progress) {
    int32_t len=0;
    uint32_t query_topN = topN + offset;
    val data=val::array();
    std::stringstream parameter;

    parameter <<"q=";

    //TOP
    CollectionInfo* ci =FindWrapper(container,key);

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
            p[j * 2] = val((uint32_t) (top_doc->position[j] >> 32));
            p[j * 2 + 1] = val((uint32_t) (top_doc->position[j] & 0x00000000fffffffL));
          }

          obj.set("p", val(top_doc->freq));
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
      char message[100];
      std::string type = key.typeof().as<std::string>();
      //printf("type:%s \n",type.c_str());
      if(type == "number")
        sprintf(message,"collection not found by key %d",key.as<int>());
      else
        sprintf(message,"collection not found by key %s",key.as<std::string>().c_str());

      ((val)on_fail)(val(std::string(message)));
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
          ((val)on_fail)(val(std::string(message)));
          return;
      }
      printf(" paramter %s \n",parameter.str().c_str());


      std::vector<val> *args = CreateCallArgs(key,callback,on_fail,on_progress,data);

      std::string query_api(api_url);
      query_api.append("/analytics/IdConverterApi");
      emscripten_async_wget2_data(query_api.c_str(),
                                  "POST",
                                  parameter.str().c_str(),
                                  (void *) args, true,
                                  &OnLoadIdLabel,
                                  &OnFail, &OnProgress);
    }else{
      ((val)callback)(data,key);
    }
  }
  /**
   * 清空所有的集合
   */
  void ClearAllCollection(){
    ClearContainer(container);
  }
  /**
   * 清空某一个集合
   */
  void ClearCollection(const val& key){
    RemoveWrapper(container,key);
  }
  /**
   * 得到某一个集合的属性
   */
  val GetCollectionProperties(const val& key){
    CollectionInfo* ci = FindWrapper(container,key);
    val result = val::object();
    if(ci){
      result.set("count",val(ci->BitCount()));
      result.set("key",val(key));
      result.set("is_top",val(false));
    }
    return result;
  }
  RoaringBitSetWrapper* CreateBitSetWrapper(const val& key){
    //先删除同key的集合
    ClearCollection(key);
    RoaringBitSetWrapper* wrapper = new RoaringBitSetWrapper();
    CollectionInfo* ci = new CollectionInfo(wrapper);
    container.insert(std::pair<val,CollectionInfo*>(key,ci));
    return wrapper;
  }


  // Binding code
  EMSCRIPTEN_BINDINGS(analytics) {
      enum_<IdCategory>("IdCategory")
          .value("Person", IdCategory::Person)
          .value("Mobile",IdCategory::Mobile)
          .value("Mac",IdCategory::Mac)
          .value("QQ",IdCategory::QQ)
          .value("WeiXin",IdCategory::WeiXin)
          .value("Car", IdCategory::Car);
      function("SetApiUrl", &SetApiUrl);
      function("query", &Query);
      function("fullTextQuery", &FullTextQuery);
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
      function("createBitSetWrapper", &CreateBitSetWrapper,allow_raw_pointers());


      class_<RoaringBitSetWrapper>("BitSetWrapper")
          .constructor()
//          .function("NewSeg",&monad::RoaringBitSetWrapper::NewSeg)
//          .function("ReadIndice",&monad::RoaringBitSetWrapper::ReadIndice)
//          .function("CreateBit",&monad::RoaringBitSetWrapper::CreateBit)
//          .function("ReadBitBlock",&monad::RoaringBitSetWrapper::ReadBitBlock)
//          .function("ReadNonZero",&monad::RoaringBitSetWrapper::ReadNonZero)
          .function("FastSet",&monad::RoaringBitSetWrapper::FastSet)
          .function("Set",&monad::RoaringBitSetWrapper::Set)
          .function("Commit", &monad::RoaringBitSetWrapper::Commit)
          .function("FastGet", &monad::RoaringBitSetWrapper::FastGet)
          .function("SetWeight", &monad::RoaringBitSetWrapper::SetWeight)
          .function("BitCount", &monad::RoaringBitSetWrapper::BitCount);
          //.class_function("InPlaceAnd",method, allow_raw_pointers());
          //.class_function("InPlaceAnd",select_overload<RoaringBitSetWrapper*(RoaringBitSetWrapper**,size_t)>(&monad::RoaringBitSetWrapper::InPlaceAnd), allow_raw_pointers());

  };
}

#endif //MONAD_EM_BIND_H_

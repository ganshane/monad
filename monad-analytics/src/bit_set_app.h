#ifndef BIT_SET_APP_H_
#define BIT_SET_APP_H_

#include <map>
#include <string>
#include <sstream>

#include "bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"

namespace monad {
  enum IdCategory {
    Person =1,
    Car =2 ,
    Mobile =3 ,
    Mac = 4,
    QQ = 5,
    WeiXin =6
  };
  template <typename K,typename WRAPPER>
  class CollectionInfo{
  private:
    K _key;
    WRAPPER* _wrapper;
    TopBitSetWrapper* _top_wrapper;
    //此标记注明_wrapper是否为临时创建的wrapper
    bool _tmp_wrapper_flag;
  public:
    CollectionInfo(const K& k,WRAPPER* wrapper):
        _key(k),_wrapper(wrapper),_top_wrapper(NULL),_tmp_wrapper_flag(false){}
    CollectionInfo(const K& k,TopBitSetWrapper* top_wrapper):
        _key(k),_wrapper(NULL),_top_wrapper(top_wrapper),_tmp_wrapper_flag(true){}
    virtual ~CollectionInfo(){
      if(_wrapper){
        delete _wrapper;
        _wrapper = NULL;
      }
      if(_top_wrapper){
        delete _top_wrapper;
        _top_wrapper = NULL;
      }
    }
    WRAPPER* GetOrCreateBitSetWrapper(){
      if(!_wrapper)
        _wrapper = WRAPPER::FromTopBitSetWrapper(_top_wrapper);

      return _wrapper;
    }
    TopBitSetWrapper* GetTopBitSetWrapper(){
      return _top_wrapper;
    }
    void ClearBitSetWrapper(){
      if(_tmp_wrapper_flag && _wrapper) {
        delete _wrapper;
        _wrapper = NULL;
      }
    }
    bool IsTopCollection(){
      return _tmp_wrapper_flag;
    }
    RegionDoc** TopWrapper(int32_t n,int32_t& data_len){
      if(_wrapper)
        return _wrapper->Top(n,data_len);
      else
        return NULL;
    }
    RegionTopDoc** TopWrapperTop(int32_t n,int32_t& data_len){
      if(_top_wrapper)
        return _top_wrapper ->Top(n,data_len);
      else
        return NULL;
    }
    int32_t BitCount(){
      if(IsTopCollection()) return _top_wrapper->BitCount();
      else return _wrapper->BitCount();
    }
    int32_t ElapsedTime(){
      if(IsTopCollection()) return (int32_t) _top_wrapper->elapsed_time;
      else return _wrapper->elapsed_time;
    }
    K& GetKey(){
      return _key;
    }
  };

  typedef void (*MessageCallback)(const int32_t code,const char* message);
  void NilMessageCallback(const int32_t,const char*){}
  struct BitSetAppOptions{
      char* api_url;
      MessageCallback progress_callback;
      MessageCallback fail_callback;
  public:
    BitSetAppOptions(){
      api_url = NULL;
      progress_callback = NilMessageCallback;
      fail_callback = NilMessageCallback;
    }
  };

  template <typename K,typename COMPARATOR,typename WRAPPER>
  class BitSetApp{
  public:
    typedef CollectionInfo<K,WRAPPER> COLL_INFO;
    typedef typename std::map<K,COLL_INFO*>::iterator CONTAINER_IT;
    typedef void (*WrapperCallback)(COLL_INFO* coll);
    typedef WRAPPER* (*WrapperAction)(WRAPPER**,size_t);

    BitSetApp(BitSetAppOptions& options){
      assert(options.api_url);
      size_t len = strlen(options.api_url);
      _options.api_url = (char*)malloc(len + 1);
      memset(_options.api_url,0,len+1);
      memcpy(_options.api_url,options.api_url,len);

      _options.progress_callback = options.progress_callback;
      _options.fail_callback = options.fail_callback;
    }
    virtual ~BitSetApp(){
      free(_options.api_url);
    }

    //virtual void SetApiUrl(const std::string& api_url);
    //virtual void Query(const std::string& index,const std::string& q)=0;
    virtual void FullTextQuery(const std::string& index,const std::string& q,WrapperCallback callback);
    virtual size_t ContainerSize(){return _container.size();};
    virtual void InPlaceAnd(std::vector<K>& keys,const WrapperCallback callback){
      DoOperator(&WRAPPER::InPlaceAnd,keys,NewKey(),callback);
    };
    virtual void InPlaceOr(std::vector<K>& keys,const WrapperCallback callback){
      DoOperator(&WRAPPER::InPlaceOr,keys,NewKey(),callback);
    }
    virtual void AndNot(std::vector<K>& keys,const WrapperCallback callback){
      DoOperator(&WRAPPER::InPlaceNot,keys,NewKey(),callback);
    }
    virtual void InPlaceAndTop(std::vector<K>& keys,const int32_t min_freq,const WrapperCallback callback);
    virtual void InPlaceAndTopWithPositionMerged(std::vector<K>& keys,const int32_t min_freq,const WrapperCallback callback);
    virtual void ClearAllCollection(){ClearContainer();}
    virtual void ClearCollection(const K& key){  RemoveWrapper(key); }
    virtual void GetCollectionProperties(const K& key,const WrapperCallback callback){
      COLL_INFO* coll_info = FindWrapper(key);
      if(coll_info){
        callback(coll_info);
      }else{
        _options.fail_callback(404,"collection not found");
      }
    }
    COLL_INFO& CreateBitSetWrapper(const K& k);

  protected:
    virtual K& NewKey()=0;
    virtual void WebGet(const std::string url,const std::string parameter,WrapperCallback callback)=0;

    void DoOperator(const WrapperAction action,const std::vector<K>& keys,const K& new_key,const WrapperCallback callback);
    /**
    * 查找map中的wrapper对象
    */
    COLL_INFO* FindWrapper(const K& key){
//    printf("find wrapper _container size:%d \n",map.size());
      CONTAINER_IT it = _container.find(key);
      if(it == _container.end()){
        return NULL;
      }else{
        return it->second;
      }
    }
    /**
     * 清空某一个容器
     */
    inline void ClearContainer(){
      for (CONTAINER_IT it=_container.begin(); it!=_container.end(); ++it){
        delete it->second;
      }
      CONTAINER_IT it;
      it = _container.begin();
      _container.erase(it,_container.end());
    }
    /**
     * 通过给定的key,删除某一个wrapper
     */
    inline void RemoveWrapper(const K& key){
      COLL_INFO* wrapper = FindWrapper(key);
      if(wrapper){
        _container.erase(key);
        delete wrapper;
      }
    }
    void AddWrapper(COLL_INFO& info){
      _container.insert(std::pair<K,COLL_INFO*>(info.GetKey(),&info));
    }
    COLL_INFO** CreateWrapperCollection(const std::vector<K> keys){
      size_t length = keys.size();
      COLL_INFO** collections = new COLL_INFO*[length];
      //printf("length:%d \n",length);
      COLL_INFO* wrapper;
      for(unsigned i=0;i<length;i++){
        K key = keys[i];
        wrapper = FindWrapper(key);
        if(wrapper == NULL){
          delete [] collections;
          std::ostringstream os;
          os << "collection not found by key " << key << std::endl;
          _options.fail_callback(51,os.str().c_str());
          return NULL;
        }
        collections[i]=wrapper;
        //printf("key:%d bitCount:%d \n",key,collections[i]->BitCount());
      }
      return collections;
    }

    BitSetAppOptions _options;
    //记录BitSetWrapper的容器
    std::map<K,COLL_INFO*,COMPARATOR> _container;
  };


  template <typename K,typename COMPARATOR,typename WRAPPER>
  void BitSetApp<K,COMPARATOR,WRAPPER>::FullTextQuery(const std::string &index, const std::string &q, WrapperCallback callback) {
    _options.progress_callback(1,"build query parameter");
    std::string parameter;
    parameter.append("i=").append(index);
    parameter.append("&");
    parameter.append("q=").append(q);

    std::string query_api(_options.api_url);
    query_api.append("/search");

    _options.progress_callback(2,"post data to api url ");
    WebGet(query_api,parameter,callback);
  };
  template <typename K,typename COMPARATOR,typename WRAPPER>
  CollectionInfo<K,WRAPPER>& BitSetApp<K,COMPARATOR,WRAPPER>::CreateBitSetWrapper(const K &key) {
    //先删除同key的集合
    ClearCollection(key);
    WRAPPER* wrapper = new WRAPPER();
    COLL_INFO* ci = new COLL_INFO(key,wrapper);
    AddWrapper(*ci);
    return *ci;
  }
  template <typename K,typename COMPARATOR,typename WRAPPER>
  void BitSetApp<K,COMPARATOR,WRAPPER>::DoOperator(const WrapperAction action,const std::vector<K>& keys,const K& new_key,const WrapperCallback callback){

    uint32_t length=keys.size();
    _options.progress_callback(1,"creating collection info ...");
    COLL_INFO** collections = CreateWrapperCollection(keys);
    if(collections == NULL || length == 0)
      return;

    _options.progress_callback(2,"creating wrapper collection ...");
    //printf("length is :%d \n",length);

    WRAPPER** wrappers = new WRAPPER*[length];
    for(int i=0;i<length;i++){
      wrappers[i]= collections[i]->GetOrCreateBitSetWrapper();
    }

    _options.progress_callback(3,"execute ...");
    WRAPPER* wrapper= action(wrappers,length);

    //clear tmp wrapper
    for(int i=0;i<length;i++){
      collections[i]->ClearBitSetWrapper();
    }
    delete[] wrappers;
    delete[] collections;
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    _options.progress_callback(4,"call callback function");
    COLL_INFO* coll_info = new COLL_INFO(new_key,wrapper);
    AddWrapper(*coll_info);
    callback(coll_info);
  }
  template <typename K,typename COMPARATOR,typename WRAPPER>
  void BitSetApp<K,COMPARATOR,WRAPPER>::InPlaceAndTop(std::vector<K> &keys, const int32_t min_freq, const WrapperCallback callback) {
    uint32_t length=keys.size();
    _options.progress_callback(1,"creating collection info ...");
    COLL_INFO** collections = CreateWrapperCollection(keys);
    if(collections == NULL)
      return;

    _options.progress_callback(2,"creating wrapper collection ...");
    //printf("length is :%d \n",length);

    WRAPPER** wrappers = new WRAPPER*[length];
    for(int i=0;i<length;i++){
      wrappers[i]= collections[i]->GetOrCreateBitSetWrapper();
    }

    _options.progress_callback(3,"execute ...");
    TopBitSetWrapper* wrapper= WRAPPER::InPlaceAndTop(wrappers,length,min_freq);

    //clear tmp wrapper
    for(int i=0;i<length;i++){
      collections[i]->ClearBitSetWrapper();
    }
    delete[] wrappers;
    delete[] collections;
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    _options.progress_callback(4,"call callback function");
    COLL_INFO* coll_info = new COLL_INFO(NewKey(),wrapper);
    AddWrapper(*coll_info);
    callback(coll_info);
  }

  template <typename K,typename COMPARATOR,typename WRAPPER>
  void BitSetApp<K,COMPARATOR,WRAPPER>::InPlaceAndTopWithPositionMerged(std::vector<K> &keys, const int32_t min_freq,
                                                  const WrapperCallback callback) {

    uint32_t length=keys.size();
    _options.progress_callback(1,"creating collection info ...");
    COLL_INFO** collections = CreateWrapperCollection(keys);
    if(collections == NULL)
      return;

    _options.progress_callback(2,"creating wrapper collection ...");
    //printf("length is :%d \n",length);

    TopBitSetWrapper** wrappers = new TopBitSetWrapper*[length];
    for(int i=0;i<length;i++){
      TopBitSetWrapper* tbsw = collections[i]->GetTopBitSetWrapper();
      if(tbsw) wrappers[i]=tbsw;
      else {
        delete[] wrappers;
        delete[] collections;
        std::ostringstream os;
        K key = keys[i];
        os << "collection is not top wrapper, key is " << key;
        _options.fail_callback(52,os.str().c_str());
        return;
      }
    }

    _options.progress_callback(3,"execute ...");
    TopBitSetWrapper* wrapper= WRAPPER::InPlaceAndTopWithPositionMerged(wrappers,length,min_freq);

    delete[] wrappers;
    delete[] collections;
    //printf("or result bitCount:%d \n",wrapper->BitCount());
    _options.progress_callback(4,"call callback function");
    COLL_INFO* coll_info = new COLL_INFO(NewKey(),wrapper);
    AddWrapper(*coll_info);
    callback(coll_info);
  }
}
#endif //BIT_SET_APP_H_

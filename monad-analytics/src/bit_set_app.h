#ifndef BIT_SET_APP_H_
#define BIT_SET_APP_H_

#include <map>
#include <string>

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

  template <typename K,typename COMPARATOR,typename WRAPPER>
  class BitSetApp{
  public:
    typedef CollectionInfo<K,WRAPPER> COLL_INFO;
    typedef typename std::map<K,COLL_INFO*>::iterator CONTAINER_IT;
    typedef void (*WrapperCallback)(COLL_INFO* coll);

    virtual void SetApiUrl(const std::string& api_url);
    //virtual void Query(const std::string& index,const std::string& q)=0;
    virtual void FullTextQuery(const std::string& index,const std::string& q,WrapperCallback callback);
    virtual size_t ContainerSize(){return _container.size();};
    /*
    virtual void InPlaceAnd()=0;
    virtual void InPlaceOr()=0;
    virtual void AndNot()=0;
    virtual void InPlaceAndTop()=0;
    virtual void InPlaceAndTopWithPositionMerged()=0;
    virtual void Top()=0;
    virtual void ClearAllCollection()=0;
     */
    virtual void ClearCollection(const K& key){  RemoveWrapper(key); }
    //virtual void GetCollectionProperties()=0;
    COLL_INFO& CreateBitSetWrapper(const K& k);

  protected:
    virtual void WebGet(const std::string url,const std::string parameter,WrapperCallback callback)=0;
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

    std::string _api_url;
    //记录BitSetWrapper的容器
    std::map<K,COLL_INFO*,COMPARATOR> _container;
  };

  template <typename K,typename COMPARATOR,typename WRAPPER>
  void BitSetApp<K,COMPARATOR,WRAPPER>::SetApiUrl(const std::string& api_url) {_api_url.assign(api_url);};

  template <typename K,typename COMPARATOR,typename WRAPPER>
  void BitSetApp<K,COMPARATOR,WRAPPER>::FullTextQuery(const std::string &index, const std::string &q, WrapperCallback callback) {

    std::string parameter;
    parameter.append("i=").append(index);
    parameter.append("&");
    parameter.append("q=").append(q);

    std::string query_api(_api_url);
    query_api.append("/search");

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
}
#endif //BIT_SET_APP_H_

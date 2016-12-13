#ifndef MONAD_COLLECTION_INFO_H_
#define MONAD_COLLECTION_INFO_H_

#include <map>
#include <sstream>

#include "bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"

//using namespace emscripten;
namespace monad {
  enum class IdCategory {
    Person =1,
    Car =2 ,
    Mobile =3 ,
    Mac = 4,
    QQ = 5,
    WeiXin =6
  };
  template <typename WRAPPER>
  class CollectionInfo{
  private:
      WRAPPER* _wrapper;
      TopBitSetWrapper* _top_wrapper;
    //此标记注明_wrapper是否为临时创建的wrapper
      bool _tmp_wrapper_flag;
  public:
    CollectionInfo(WRAPPER* wrapper):
      _wrapper(wrapper),_top_wrapper(NULL),_tmp_wrapper_flag(false){}
    CollectionInfo(TopBitSetWrapper* top_wrapper):
      _wrapper(NULL),_top_wrapper(top_wrapper),_tmp_wrapper_flag(true){}
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
      if(IsTopCollection()) return _top_wrapper->elapsed_time;
      else return _wrapper->elapsed_time;
    }
  };



  template <typename K,typename COMPARATOR,typename WRAPPER>
  class CollectionContainer{
  private:
    //记录BitSetWrapper的容器
    std::map<K,CollectionInfo<WRAPPER>*,COMPARATOR> _container;
    typedef typename std::map<K,CollectionInfo<WRAPPER>*>::iterator CONTAINER_IT;
  public:
   /**
   * 查找map中的wrapper对象
   */
   inline CollectionInfo<WRAPPER>* FindWrapper(const K& key){
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
      CollectionInfo<WRAPPER>* wrapper = FindWrapper(key);
      if(wrapper){
        _container.erase(key);
        delete wrapper;
      }
    }
    void AddWrapper(const K& key,CollectionInfo<WRAPPER>& info){
      _container.insert(std::pair<K,CollectionInfo<WRAPPER>*>(key,&info));
    }
    int32_t Size(){
      return _container.size();
    }
  };
}
#endif //MONAD_COLLECTION_INFO_H_

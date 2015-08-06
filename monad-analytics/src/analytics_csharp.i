#ifdef SWIG
%module CMonad
%{

#include "bit_set_wrapper_holder.h"
#include "open_bit_set.h"
#include "open_bit_set_wrapper.h"
#include "top_bit_set.h"
#include "top_bit_set_wrapper.h"
using namespace monad;

%}
%include <stdint.i>
%include <typemaps.i>
%include <carrays.i>
%array_class(RegionDoc,RegionDocArray);
%array_class(RegionTopDoc,RegionTopDocArray);
%array_class(uint64_t,Uint64Array);
%typemap(ctype)   monad::RegionDoc** "void*"
%typemap(cstype)  monad::RegionDoc** "RegionDocArray"
%typemap(csout)     monad::RegionDoc** {
  return new RegionDocArray($imcall,true);
}
%typemap(out)     monad::RegionDoc** {
  if($1){
    //数据copy
    int32_t len = *arg3;
    RegionDoc* $1_tmp = new RegionDoc[len];
    for(int i=0;i<len;i++){
      memcpy(&$1_tmp[i],$1[i],sizeof(RegionDoc));
      delete $1[i];
    }
    delete[] $1;

    jresult = $1_tmp;
  }
}

%typemap(ctype)   monad::RegionTopDoc** "void*"
%typemap(cstype)  monad::RegionTopDoc** "RegionTopDocArray"
%typemap(csout)     monad::RegionTopDoc** {
  return new RegionTopDocArray($imcall,true);
}
%typemap(out)     monad::RegionTopDoc** {
  if($1){
    //数据copy
    int32_t len = *arg3;
    RegionTopDoc* $1_tmp = new RegionTopDoc[len];
    for(int i=0;i<len;i++){
      memcpy(&$1_tmp[i],$1[i],sizeof(RegionTopDoc));
      delete $1[i];
    }
    delete[] $1;

    jresult = $1_tmp;
  }
}
%csmethodmodifiers position "private";
%typemap(cscode) TopDoc %{
  public Uint64Array GetPosition() {
    IntPtr cPtr = CMonadPINVOKE.TopDoc_position_get(swigCPtr);
    Uint64Array ret = (cPtr == IntPtr.Zero) ? null : new Uint64Array(cPtr,true);
    return ret;
  }
%}

//忽略一些在Java中不用的方法
%ignore monad::OpenBitSetWrapper::Iterator();
%ignore monad::TopBitSetWrapper::Iterator();
%ignore ReadLong;
%ignore TopDoc();
%ignore InPlaceAnd(OpenBitSetWrapper**,size_t);
%ignore InPlaceAndTop(OpenBitSetWrapper**,size_t,int32_t);
%ignore InPlaceAndTopWithPositionMerged(TopBitSetWrapper**,size_t,int32_t);
%ignore InPlaceOr(OpenBitSetWrapper**,size_t);
%ignore InPlaceNot(OpenBitSetWrapper**,size_t);

 // struct TopDoc;
  /**
   * 分区Doc的对象
   */
  struct RegionDoc{
    uint32_t doc; //文档号
    uint32_t region;//分区
  };
  
  /**
   * 记录的TopDoc值
   */
  struct RegionTopDoc{
    TopDoc* top_doc; //TopDoc
    uint32_t region;//分区值
  };

  struct TopDoc {
    uint32_t doc;//Doc
    uint32_t freq;//频率
    uint64_t* position;//位置信息
    uint32_t  position_len;//位置信息的长度
};
/*
// 去掉数据长度参数
%typemap(in,numinputs=0,noblock=1) int32_t& data_len {
   int temp_len=0;
   $1 =  &temp_len;
}
*/
%apply int32_t &OUTPUT{ int32_t &data_len};

%include "open_bit_set_wrapper.h"
%include "top_bit_set_wrapper.h"

%include "bit_set_wrapper_holder.h"
%template(OpenBitSetWrapperHolder) monad::BitSetWrapperHolder<monad::OpenBitSetWrapper>;
%template(TopBitSetWrapperHolder) monad::BitSetWrapperHolder<monad::TopBitSetWrapper>;
#endif

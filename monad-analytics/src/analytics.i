#ifdef SWIG
%module CMonad
%{

#include "bit_set_wrapper_holder.h"
#include "open_bit_set.h"
#include "sparse_bit_set.h"
#include "open_bit_set_wrapper.h"
#include "sparse_bit_set_wrapper.h"
#include "top_bit_set.h"
#include "top_bit_set_wrapper.h"

using namespace monad;

%}
%include <stdint.i>
//忽略一些在Java中不用的方法
%ignore monad::OpenBitSetWrapper::Iterator();
%ignore monad::SparseBitSetWrapper::Iterator();
%ignore monad::TopBitSetWrapper::Iterator();

%ignore InPlaceAnd(BitSetWrapperHolder<OpenBitSetWrapper>& holder);
%ignore InPlaceAndTop(BitSetWrapperHolder<OpenBitSetWrapper>& holder, int32_t min_freq);
%ignore InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq);
%ignore InPlaceOr(BitSetWrapperHolder<OpenBitSetWrapper>& holder);
%ignore InPlaceNot(BitSetWrapperHolder<OpenBitSetWrapper>& holder);

%ignore InPlaceAnd(BitSetWrapperHolder<SparseBitSetWrapper>& holder);
%ignore InPlaceAndTop(BitSetWrapperHolder<SparseBitSetWrapper>& holder, int32_t min_freq);
%ignore InPlaceAndTopWithPositionMerged(BitSetWrapperHolder<TopBitSetWrapper>& holder, int32_t min_freq);
%ignore InPlaceOr(BitSetWrapperHolder<SparseBitSetWrapper>& holder);
%ignore InPlaceNot(BitSetWrapperHolder<SparseBitSetWrapper>& holder);

        
//所有的swigCMemOwn均为true,方便进行删除操作
%typemap(javabody) SWIGTYPE %{
  private long swigCPtr;
  protected boolean swigCMemOwn;

  $javaclassname(long cPtr, boolean cMemoryOwn) {
    swigCPtr = cPtr;
    //swigCMemOwn = cMemoryOwn;
    swigCMemOwn = true;
  }

  static long getCPtr($javaclassname obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }
%}
//在Java中加入转换使用的函数
%pragma(java) modulecode=%{
  static long[] convertBitSetAsLongArray(OpenBitSetWrapper wrappers[]){
    long[] pointerArray = new long[wrappers.length];
    for(int i=0;i<wrappers.length;i++){
      pointerArray[i]=OpenBitSetWrapper.getCPtr(wrappers[i]);
    }
    return pointerArray;
  }
  static long[] convertBitSetAsLongArray(TopBitSetWrapper wrappers[]){
    long[] pointerArray = new long[wrappers.length];
    for(int i=0;i<wrappers.length;i++){
      pointerArray[i]=TopBitSetWrapper.getCPtr(wrappers[i]);
    }
    return pointerArray;
  }
  static long[] convertBitSetAsLongArray(SparseBitSetWrapper wrappers[]){
    long[] pointerArray = new long[wrappers.length];
    for(int i=0;i<wrappers.length;i++){
      pointerArray[i]=SparseBitSetWrapper.getCPtr(wrappers[i]);
    }
    return pointerArray;
  }
%}
//--------------- (Wrapper** WRAPPER,size_t LENGTH) begin --
//定义OpenBitSetWrapper** 的映射
%typemap(jni)     (monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH) "jlongArray"
%typemap(jtype)   (monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH) "long[]"
%typemap(jstype)   (monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH) "OpenBitSetWrapper[]"
%typemap(javain)  (monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH) "$module.convertBitSetAsLongArray($javainput)"
%typemap(in,numinputs=1)  (monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH) {
  jlong* long_arr = JCALL2(GetLongArrayElements, jenv, $input,0);
  $2 = JCALL1(GetArrayLength, jenv, $input);
  $1 = new OpenBitSetWrapper*[$2]();
  for(uint32_t i=0;i<$2;i++)
    $1[i]= reinterpret_cast<monad::OpenBitSetWrapper*>(long_arr[i]);
  if ($input) JCALL3(ReleaseLongArrayElements, jenv, $input, long_arr, 0);
}
%typemap(freearg) (monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH){
  if($1)
    delete[] $1;
}
%apply(monad::OpenBitSetWrapper** WRAPPER, size_t LENGTH) { (monad::OpenBitSetWrapper**  wrappers, size_t len) };
//--------------- (Wrapper** WRAPPER,size_t LENGTH) end --
//--------------- (SparseBitSetWrapper** WRAPPER,size_t LENGTH) begin --
//定义OpenBitSetWrapper** 的映射
%typemap(jni)     (monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH) "jlongArray"
%typemap(jtype)   (monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH) "long[]"
%typemap(jstype)   (monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH) "SparseBitSetWrapper[]"
%typemap(javain)  (monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH) "$module.convertBitSetAsLongArray($javainput)"
%typemap(in,numinputs=1)  (monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH) {
  jlong* long_arr = JCALL2(GetLongArrayElements, jenv, $input,0);
  $2 = JCALL1(GetArrayLength, jenv, $input);
  $1 = new SparseBitSetWrapper*[$2]();
  for(uint32_t i=0;i<$2;i++)
    $1[i]= reinterpret_cast<monad::SparseBitSetWrapper*>(long_arr[i]);
  if ($input) JCALL3(ReleaseLongArrayElements, jenv, $input, long_arr, 0);
}
%typemap(freearg) (monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH){
  if($1)
    delete[] $1;
}
%apply(monad::SparseBitSetWrapper** WRAPPER, size_t LENGTH) { (monad::SparseBitSetWrapper**  wrappers, size_t len) };
//--------------- (TopWrapper** WRAPPER,size_t LENGTH) end --
%typemap(jni)     (monad::TopBitSetWrapper** WRAPPER, size_t LENGTH) "jlongArray"
%typemap(jtype)   (monad::TopBitSetWrapper** WRAPPER, size_t LENGTH) "long[]"
%typemap(jstype)   (monad::TopBitSetWrapper** WRAPPER, size_t LENGTH) "TopBitSetWrapper[]"
%typemap(javain)  (monad::TopBitSetWrapper** WRAPPER, size_t LENGTH) "$module.convertBitSetAsLongArray($javainput)"
%typemap(in,numinputs=1)  (monad::TopBitSetWrapper** WRAPPER, size_t LENGTH) {
  jlong* long_arr = JCALL2(GetLongArrayElements, jenv, $input,0);
  $2 = JCALL1(GetArrayLength, jenv, $input);
  $1 = new TopBitSetWrapper*[$2]();
  for(uint32_t i=0;i<$2;i++)
    $1[i]= reinterpret_cast<monad::TopBitSetWrapper*>(long_arr[i]);
  if ($input) JCALL3(ReleaseLongArrayElements, jenv, $input, long_arr, 0);
}
%typemap(freearg) (monad::TopBitSetWrapper** WRAPPER, size_t LENGTH){
  if($1)
    delete[] $1;
}
%apply(monad::TopBitSetWrapper** WRAPPER, size_t LENGTH) { (monad::TopBitSetWrapper**  wrappers, size_t len) };
//--------------- (TopWrapper** WRAPPER,size_t LENGTH) end --


//------------------ java_long_byte_data begin ------
%typemap(jstype) int8_t* long_byte_data "byte[]"
%typemap(jtype) int8_t* long_byte_data "byte[]"
%typemap(jni)   int8_t* "jbyteArray"
%typemap(javain) int8_t* %{ $javainput %}
%typemap(in) int8_t* %{ 
  $1 = jenv->GetByteArrayElements($input,0);
%}
%typemap(freearg) int8_t* {
  if($1)
    JCALL3(ReleaseByteArrayElements, jenv, $input, (jbyte*)$1, JNI_ABORT);
}


//------------------ java_long_byte_data end ------


//------------------- monad::OpenBitSetWrapper::Top begin -------
// 去掉数据长度参数
%typemap(in,numinputs=0,noblock=1) int32_t& data_len {
   int temp_len=0;
   $1 =  &temp_len;
}
//申明返回类型
%typemap(jstype) monad::RegionDoc** monad::OpenBitSetWrapper::Top "monad.analytics.model.RegionDoc[]"
%typemap(jtype)  monad::RegionDoc** monad::OpenBitSetWrapper::Top "monad.analytics.model.RegionDoc[]"
//设置JNI操作时候的数据类型
%typemap(jni)   monad::RegionDoc** monad::OpenBitSetWrapper::Top "jobjectArray"
%typemap(javaout) monad::RegionDoc** monad::OpenBitSetWrapper::Top {
  return $jnicall;
}
//设置C调用过程中的转换
%typemap(out)   monad::RegionDoc** monad::OpenBitSetWrapper::Top {
  if($1){
    //生成Java中的数组
    jclass region_doc_class = JCALL1(FindClass,jenv,"monad/analytics/model/RegionDoc");
    $result = JCALL3(NewObjectArray, jenv, temp_len,region_doc_class,NULL);
    jmethodID cid = JCALL3(GetMethodID,jenv,region_doc_class, "<init>", "(II)V");
    for(int i=0;i<temp_len;i++){
      //printf("i:%d doc:%u region:%u \n",i,$1[i]->doc,$1[i]->region);
        jobject obj = JCALL4(NewObject,jenv,
                region_doc_class,
                cid,
                static_cast<jint>($1[i]->doc),
                static_cast<jint>($1[i]->region) );
        JCALL3(SetObjectArrayElement,jenv,$result, i, obj);
        JCALL1(DeleteLocalRef,jenv,obj);
    }
    //清空内存
    for(int i=0;i<temp_len;i++){
      delete $1[i];
    }
    delete[] $1;
  }else{
    $result = NULL;
  }
}
//------------------- monad::OpenBitSetWrapper::Top end -------
//------------------- monad::SparseBitSetWrapper::Top begin -------
// 去掉数据长度参数
%typemap(in,numinputs=0,noblock=1) int32_t& data_len {
   int temp_len=0;
   $1 =  &temp_len;
}
//申明返回类型
%typemap(jstype) monad::RegionDoc** monad::SparseBitSetWrapper::Top "monad.analytics.model.RegionDoc[]"
%typemap(jtype)  monad::RegionDoc** monad::SparseBitSetWrapper::Top "monad.analytics.model.RegionDoc[]"
//设置JNI操作时候的数据类型
%typemap(jni)   monad::RegionDoc** monad::SparseBitSetWrapper::Top "jobjectArray"
%typemap(javaout) monad::RegionDoc** monad::SparseBitSetWrapper::Top {
  return $jnicall;
}
//设置C调用过程中的转换
%typemap(out)   monad::RegionDoc** monad::SparseBitSetWrapper::Top {
  if($1){
    //生成Java中的数组
    jclass region_doc_class = JCALL1(FindClass,jenv,"monad/analytics/model/RegionDoc");
    $result = JCALL3(NewObjectArray, jenv, temp_len,region_doc_class,NULL);
    jmethodID cid = JCALL3(GetMethodID,jenv,region_doc_class, "<init>", "(II)V");
    for(int i=0;i<temp_len;i++){
      //printf("i:%d doc:%u region:%u \n",i,$1[i]->doc,$1[i]->region);
        jobject obj = JCALL4(NewObject,jenv,
                region_doc_class, 
                cid,
                static_cast<jint>($1[i]->doc), 
                static_cast<jint>($1[i]->region) );
        JCALL3(SetObjectArrayElement,jenv,$result, i, obj);
        JCALL1(DeleteLocalRef,jenv,obj);
    }
    //清空内存
    for(int i=0;i<temp_len;i++){
      delete $1[i];
    }
    delete[] $1;
  }else{
    $result = NULL;
  }
}
//------------------- monad::SparseBitSetWrapper::Top end -------

//------------------- monad::TopBitSetWrapper::Top begin -------
//申明返回类型
%typemap(jstype) monad::RegionTopDoc** monad::TopBitSetWrapper::Top "monad.analytics.model.RegionTopDoc[]"
%typemap(jtype)  monad::RegionTopDoc** monad::TopBitSetWrapper::Top "monad.analytics.model.RegionTopDoc[]"
//设置JNI操作时候的数据类型
%typemap(jni)   monad::RegionTopDoc** monad::TopBitSetWrapper::Top "jobjectArray"
%typemap(javaout) monad::RegionTopDoc** monad::TopBitSetWrapper::Top {
  return $jnicall;
}
//设置C调用过程中的转换
%typemap(out)  monad::RegionTopDoc** monad::TopBitSetWrapper::Top {
  if($1){
    jclass region_top_doc_class = JCALL1(FindClass,jenv,"monad/analytics/model/RegionTopDoc");
    $result = JCALL3(NewObjectArray, jenv, temp_len,region_top_doc_class,NULL);
    jmethodID cid = JCALL3(GetMethodID,jenv,region_top_doc_class, "<init>", "(II[JI)V");
    for(int i=0;i<temp_len;i++){
      //printf("p:%llx \n",$1[i]->top_doc->position);
      //声明jni对象
      uint32_t position_len = $1[i]->top_doc->position_len;
      jlongArray long_buffer = jenv->NewLongArray(position_len);
      //copy数据
      jlong* elements = jenv->GetLongArrayElements(long_buffer, NULL);
      /*
      for(uint32_t j=0;j<position_len;j++)
        elements[j]= static_cast<jlong>($1[i]->top_doc->position[j]);
      */
      memcpy(elements,$1[i]->top_doc->position,$1[i]->top_doc->position_len*sizeof(uint64_t));
      jenv->ReleaseLongArrayElements(long_buffer, elements,0);

      jobject obj = JCALL6(NewObject,jenv,
          region_top_doc_class, 
          cid,
          static_cast<jint>($1[i]->top_doc->doc),
          static_cast<jint>($1[i]->top_doc->freq),
          long_buffer,
          static_cast<jint>($1[i]->region));

      JCALL3(SetObjectArrayElement,jenv,$result, i, obj);
      JCALL1(DeleteLocalRef,jenv,obj);
    }
    //释放内存
    TopBitSet::FreeRegionTopDocArray($1,temp_len);
  }else{
    $result = NULL;
  }
}
//------------------- monad::TopBitSetWrapper::Top end -------

%include "open_bit_set_wrapper.h"
%include "sparse_bit_set_wrapper.h"
%include "top_bit_set_wrapper.h"
/*
%include "bit_set_wrapper_holder.h"
%template(OpenBitSetWrapperHolder) monad::BitSetWrapperHolder<monad::OpenBitSetWrapper>;
%template(TopBitSetWrapperHolder) monad::BitSetWrapperHolder<monad::TopBitSetWrapper>;
*/
#endif

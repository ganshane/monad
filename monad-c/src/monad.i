#ifdef SWIG
%module CMonad
%{
  #include "monad_config.h"
  #include "monad.h"
  #include "nosql_kv_def.h"
  #include "nosql_support.h"
  #include "sync_nosql.h"
  #include <memory>
  using namespace monad;
%}


%include "stdint.i"
%apply int64_t {uint64_t}
%apply int32_t {uint32_t}


// ---------  leveldb::Slice& begin ------------
%typemap(jni) leveldb::Slice& "jbyteArray"
%typemap(jtype) leveldb::Slice& "byte[]"
%typemap(jstype) leveldb::Slice& "byte[]"
%typemap(javain) leveldb::Slice& %{ $javainput %}

%typemap(in) leveldb::Slice& %{
  if($input == NULL){
    $1 = new leveldb::Slice();
  }else{
    char* c_data_$input = (char *) jenv->GetByteArrayElements($input, 0);
    int len_$input = (int)jenv->GetArrayLength($input);
    $1 = new leveldb::Slice(c_data_$input,len_$input);
  }
  %}
%typemap(freearg) leveldb::Slice& {
  if($input)
  JCALL3(ReleaseByteArrayElements, jenv, $input, (jbyte*)$1->data(), JNI_ABORT);
  if($1)
  delete $1;
}
// ---------  leveldb::Slice& end ------------
// ---------  std::string begin --------------
%typemap(jni) std::string "jbyteArray"
%typemap(jtype) std::string "byte[]"
%typemap(jstype) std::string "byte[]"
%typemap(javaout) std::string {
  return $jnicall;
}
%typemap(out) std::string {
  //TODO 采用DirectByteBuffer进行优化
  size_t sz = $1.size();
  $result = JCALL1(NewByteArray,jenv,sz);
  if($result) {
    //set data
    JCALL4(SetByteArrayRegion,jenv,$result, 0, sz, (jbyte*) $1.data());
  }
}
// ---------  std::string end --------------
// ---------  std::string* begin --------------
%typemap(jni) std::string* "jbyteArray"
%typemap(jtype) std::string* "byte[]"
%typemap(jstype) std::string* "byte[]"
%typemap(javaout) std::string* {
  return $jnicall;
}
%typemap(out) std::string* {
  if($1){
    //TODO 采用DirectByteBuffer进行优化
    size_t sz = $1->size();
    $result = JCALL1(NewByteArray,jenv,sz);
    if($result) {
      //set data
      JCALL4(SetByteArrayRegion,jenv,$result, 0, sz, (jbyte*) $1->data());
    }
    delete $1;
  }
}
// ---------  std::string* end --------------


///////////////// ignore some method ///////////
%ignore doInIterator;
%ignore RawGet;
%ignore monad::BaseBufferSupport::ToString;
%ignore monad::Uncopyable;
%ignore monad::MonadStatus::FromLeveldbStatus;
%ignore monad::IteratorSeeker::IteratorSeeker;
%ignore monad::IteratorCallback;

//%ignore BaseBufferSupport;
///////////////// end ignore ////////////
%typemap(throws, throws="monad.jni.services.StatusException") monad::MonadStatus {
  jclass excep = jenv->FindClass("monad/jni/services/StatusException");
  if (excep)
  jenv->ThrowNew(excep,$1.ToString().c_str());//.ToString().c_str());
  return $null;
}

%newobject monad::SlaveNoSQLSupport::FindNextBinlog;

%include "monad_types.h"
%include "status.h"
%include "nosql_kv_def.h"
%include "nosql_support.h"
%include "sync_nosql.h"

%extend monad::NoSQLSupport{
  std::string* Get(const leveldb::Slice& key) throw(monad::MonadStatus){
    std::string* val = new std::string();
    MonadStatus status = self-> RawGet(key,val);
    if(status.ok()){
      return val;
    }else if(status.code() == kNotFound){
      delete val;
      return NULL;
    }
    delete val;
    throw status;
  };
  std::string* Get(BaseBufferSupport& key) throw(monad::MonadStatus){
    return monad_NoSQLSupport_Get__SWIG_0(self,key.ToString());
  };
}
%extend monad::BaseBufferSupport{
  std::string GetValue(){
    return self->ToString();
  };
}
//logger
namespace monad{
  void OpenLogger(const char* filename,const LoggerLevel level);
}


#endif

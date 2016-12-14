// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <bit_set_app.h>

#include "bit_set_wrapper.h"
#include "roaring_bit_set_wrapper.h"
using namespace monad;
class BitSetAppTest: public ::testing::Test {
  protected:
  BitSetAppTest() {
    }
  virtual ~BitSetAppTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};

struct Int32Comp {
    bool operator()(const int32_t &lhs, const int32_t &rhs) const {
      return lhs < rhs;
    }
};
void OnProgress(int32_t,char*){}
void OnFail(int32_t,char*){}

class MyApp:public BitSetApp<int32_t,Int32Comp,RoaringBitSetWrapper> {
public:
  MyApp(BitSetAppOptions& options):BitSetApp(options),_seq(0){
  }
  static void MyCallback(COLL_INFO* coll_info){

  }
protected:
  int32_t _seq;
  void WebGet(const std::string url,const std::string parameter,WrapperCallback callback){
    _seq ++;
    COLL_INFO& info = CreateBitSetWrapper(_seq);
    RoaringBitSetWrapper* wrapper = info.GetOrCreateBitSetWrapper();
    wrapper->NewSeg(1,12);
    wrapper->FastSet(108);
    wrapper->Commit();

    callback(&info);
  }
};

TEST_F(BitSetAppTest, TestApp) {
  BitSetAppOptions options;
  options.api_url= (char *) "http://localhost:8080/api";
  options.progress_callback = OnProgress;
  options.fail_callback= OnFail;
  MyApp app(options);
  app.FullTextQuery("trace","i=x",MyApp::MyCallback);
  ASSERT_EQ(1,1);
}

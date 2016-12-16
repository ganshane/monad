// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <bit_set_app.h>

#include "bit_set_wrapper.h"
#include "roaring_bit_set_wrapper.h"
using namespace monad;
using namespace std::placeholders;

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
void OnProgress(const int32_t code,const std::string& message){
  std::cout << "progress [" << code << "] " << message << std::endl;
}
void OnFail(const int32_t code,const std::string& message){
  std::cout << "fail " << code << message << std::endl;
}

class MyApp:public BitSetApp<int32_t,Int32Comp,RoaringBitSetWrapper> {
public:
  MyApp(BitSetAppOptions& options):BitSetApp(options),_seq(0){
  }
  static void MyCallback(COLL_INFO* coll_info){
    std::cout<<"["<<coll_info->GetKey() << "] "<<"bit count:" << coll_info->BitCount() << " time:" << coll_info->ElapsedTime() << std::endl;
  }
protected:
  int32_t _seq;

  int32_t& NewKey(){
    ++_seq;
    return _seq;
  }
  void WebGet(const std::string& url,const std::string& parameter,WrapperCallback callback,int32_t weight){
    int32_t key = NewKey();
    COLL_INFO& info = CreateBitSetWrapper(key);
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
  options.progress_callback = OnProgress;//(MessageCallback)std::bind(OnProgress,_1,_2);
  options.fail_callback=  OnFail;//(MessageCallback)std::bind(OnFail,_1,_2);
  MyApp app(options);
  app.Query("trace","i=x",MyApp::MyCallback,1);
  std::vector<int32_t> keys;
  keys.push_back(1);
  keys.push_back(1);
  app.InPlaceAnd(keys,MyApp::MyCallback);
  app.InPlaceOr(keys,MyApp::MyCallback);
  app.AndNot(keys,MyApp::MyCallback);
  app.InPlaceAndTop(keys,1,MyApp::MyCallback);
  keys.clear();
  keys.push_back(5);
  keys.push_back(5);
  app.InPlaceAndTopWithPositionMerged(keys,1,MyApp::MyCallback);
  ASSERT_EQ(1,1);
}

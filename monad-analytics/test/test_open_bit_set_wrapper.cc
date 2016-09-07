// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>

#include "bit_set_wrapper_holder.h"
#include "open_bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"

using namespace monad;
class OpenBitSetWrapperTest: public ::testing::Test {
  protected:
  OpenBitSetWrapperTest() {
    }
  virtual ~OpenBitSetWrapperTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(OpenBitSetWrapperTest, TestInPlaceAndTopWithEmptyCollection) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 40,0);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceAndTop(holder,1);
  ASSERT_EQ(1,bit_set->Size());

  int32_t data_len=0;
  RegionTopDoc** docs = bit_set->Top(10,data_len);
  RegionTopDoc* top_doc = docs[0];
  ASSERT_EQ(2,top_doc->region);
  ASSERT_EQ(40,top_doc->top_doc->doc);
  ASSERT_EQ(1,top_doc->top_doc->freq);
  ASSERT_EQ(2,top_doc->top_doc->position[0]);

  for(int i=0;i<data_len;i++)
    delete docs[i];
  delete []docs;
  delete bit_set;
}
TEST_F(OpenBitSetWrapperTest, TestEmpty) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.Commit();

  TopBitSetWrapper* topWrapper = OpenBitSetWrapper::InPlaceAndTop(holder,2);

  BitSetWrapperHolder<TopBitSetWrapper> holder2;
  holder2.AddWrapper(topWrapper);
  OpenBitSetWrapper::InPlaceAndTopWithPositionMerged(holder2,1);
  if(topWrapper)
    delete topWrapper;
}
TEST_F(OpenBitSetWrapperTest, TestInPlaceNot) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);
  wrapper.NewSeg(3,3);
  wrapper.ReadLong(1LL << 38,1);
  wrapper.NewSeg(2,2);
  wrapper.ReadLong(1LL << 40,0);
  wrapper.ReadLong(1LL << 42,1);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 40,0);
  wrapper2.Commit();

  OpenBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceNot(holder);
  
  uint32_t index = 38;
  ASSERT_TRUE(bit_set->FastGet(index));
  index =  64+40;
  ASSERT_FALSE(bit_set->FastGet(index));
  index = 64+64+42;
  ASSERT_TRUE(bit_set->FastGet(index));
  delete bit_set;
}
TEST_F(OpenBitSetWrapperTest, TestInPlaceOr) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;

  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);
  wrapper.NewSeg(3,3);
  wrapper.ReadLong(1LL << 38,1);
  wrapper.NewSeg(2,2);
  wrapper.ReadLong(1LL << 40,0);
  wrapper.ReadLong(1LL << 42,1);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 50,0);
  wrapper2.Commit();

  OpenBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceOr(holder);
  
  uint32_t index = 38;
  ASSERT_TRUE(bit_set->FastGet(index));
  index =  64+40;
  ASSERT_TRUE(bit_set->FastGet(index));
  index = 64 + 50;
  ASSERT_TRUE(bit_set->FastGet(index));

  int32_t data_len = 0;
  RegionDoc** docs = bit_set->Top(10,data_len);
  RegionDoc* doc = docs[0];
  ASSERT_EQ(38,doc->doc);
  ASSERT_EQ(1,doc->region);
  doc = docs[2];
  ASSERT_EQ(50,doc->doc);
  ASSERT_EQ(2,doc->region);

  for(int i=0;i<data_len;i++){
    delete docs[i];
  }
  delete [] docs;
  delete bit_set;
};
TEST_F(OpenBitSetWrapperTest, TestInPlaceAndTopWithPositionMerged) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);
  wrapper.NewSeg(3,3);
  wrapper.ReadLong(1LL << 38,1);
  wrapper.NewSeg(2,2);
  wrapper.ReadLong(1LL << 40,0);
  wrapper.ReadLong(1LL << 42,1);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 40,0);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceAndTop(holder,1);
  BitSetWrapperHolder<OpenBitSetWrapper> holder_;
  wrapper2 = OpenBitSetWrapper();
  holder_.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 40,0);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set2 = OpenBitSetWrapper::InPlaceAndTop(holder_,1);
  ASSERT_EQ(3,bit_set->Size());
  ASSERT_EQ(1,bit_set2->Size());

  BitSetWrapperHolder<TopBitSetWrapper> holder2;
  holder2.AddWrapper(bit_set);
  holder2.AddWrapper(bit_set2);
  TopBitSetWrapper* bit_set3 = OpenBitSetWrapper::InPlaceAndTopWithPositionMerged(holder2,1);
  ASSERT_EQ(3,bit_set3->Size());

  int32_t data_len = 0;
  RegionTopDoc** docs = bit_set3->Top(10,data_len);
  RegionTopDoc* top_doc = docs[0];
  ASSERT_EQ(2,top_doc->region);
  ASSERT_EQ(40,top_doc->top_doc->doc);
  ASSERT_EQ(2,top_doc->top_doc->freq);
  ASSERT_EQ(3,top_doc->top_doc->position[0]);

  for(int i=0;i<data_len;i++)
    delete docs[i];
  delete []docs;

  delete bit_set;
  delete bit_set2;
  delete bit_set3;
}
TEST_F(OpenBitSetWrapperTest, TestInPlaceAndTopMore) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  for(int i=0;i<33;i++){
    OpenBitSetWrapper* wrapper = new OpenBitSetWrapper();
    holder.AddWrapper(wrapper);
    wrapper->NewSeg(1,2);
    wrapper->ReadLong(1LL << 40,0);
    wrapper->ReadLong(1LL << 41,1);
    wrapper->Commit();
  }


  TopBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceAndTop(holder,1);
  ASSERT_EQ(1,bit_set->Size());

  int32_t data_len=0;
  RegionTopDoc** docs = bit_set->Top(10,data_len);
  RegionTopDoc* top_doc = docs[0];
  ASSERT_EQ(1,top_doc->region);
  ASSERT_EQ(40,top_doc->top_doc->doc);
  ASSERT_EQ(33,top_doc->top_doc->freq);
  ASSERT_EQ(0x1ffffffff,top_doc->top_doc->position[0]);
  top_doc = docs[1];
  ASSERT_EQ(1,top_doc->region);
  ASSERT_EQ(105,top_doc->top_doc->doc);
  ASSERT_EQ(33,top_doc->top_doc->freq);

  for(int i=0;i<data_len;i++)
    delete docs[i];
  delete []docs;
  delete bit_set;
  for(int i=0;i<33;i++){
    delete holder.Get(i);
  }
}
TEST_F(OpenBitSetWrapperTest, TestInPlaceAndTop) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);
  wrapper.NewSeg(3,3);
  wrapper.ReadLong(1LL << 38,1);
  wrapper.NewSeg(2,2);
  wrapper.ReadLong(1LL << 40,0);
  wrapper.ReadLong(1LL << 42,1);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 40,0);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceAndTop(holder,1);
  ASSERT_EQ(3,bit_set->Size());

  int32_t data_len=0;
  RegionTopDoc** docs = bit_set->Top(10,data_len);
  RegionTopDoc::Debug(docs,data_len);
  RegionTopDoc* top_doc = docs[0];
  ASSERT_EQ(2,top_doc->region);
  ASSERT_EQ(40,top_doc->top_doc->doc);
  ASSERT_EQ(2,top_doc->top_doc->freq);
  ASSERT_EQ(3,top_doc->top_doc->position[0]);
  top_doc = docs[1];
  ASSERT_EQ(1,top_doc->region);
  ASSERT_EQ(38,top_doc->top_doc->doc);
  ASSERT_EQ(1,top_doc->top_doc->freq);

  for(int i=0;i<data_len;i++)
    delete docs[i];
  delete []docs;
  delete bit_set;
}
TEST_F(OpenBitSetWrapperTest, TestInPlaceAnd) {
  BitSetWrapperHolder<OpenBitSetWrapper> holder;
  OpenBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);
  wrapper.NewSeg(3,3);
  wrapper.ReadLong(1LL << 38,1);
  wrapper.NewSeg(2,2);
  wrapper.ReadLong(1LL << 40,0);
  wrapper.ReadLong(1LL << 42,1);
  wrapper.Commit();

  OpenBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2);
  wrapper2.ReadLong(1LL << 40,0);
  wrapper2.Commit();

  OpenBitSetWrapper* bit_set = OpenBitSetWrapper::InPlaceAnd(holder);
  
  uint32_t index = 38;
  ASSERT_FALSE(bit_set->FastGet(index));
  //因为第一个集合合并消失了，所以从40开始有数据
  index =  40;
  ASSERT_TRUE(bit_set->FastGet(index));
  delete bit_set;
}
TEST_F(OpenBitSetWrapperTest, TestGetSet) {
  OpenBitSetWrapper wrapper;
  wrapper.NewSeg(1,1);
  wrapper.ReadLong(1LL << 38,0);

  wrapper.NewSeg(3,3);
  wrapper.ReadLong(1LL << 38,1);

  wrapper.NewSeg(2,2);
  wrapper.ReadLong(1LL << 40,0);
  wrapper.ReadLong(1LL << 42,1);

  wrapper.Commit();

  uint32_t index = (1*64)+ 40;
  ASSERT_TRUE(wrapper.FastGet(index));
  index = 64 + (64 + 42);
  ASSERT_TRUE(wrapper.FastGet(index));
  index = 64 + (64*2)+ 64 + 38;
  ASSERT_TRUE(wrapper.FastGet(index));
  ASSERT_EQ(4,wrapper.BitCount());

  wrapper = OpenBitSetWrapper();
  wrapper.NewSeg(1, 10);
  wrapper.ReadLong(1LL << 20, 2);
  wrapper.Commit();
  ASSERT_TRUE(wrapper.FastGet(64*2 + 20));
  int32_t data_len = 0;
  RegionDoc** docs = wrapper.Top(10,data_len);
  ASSERT_EQ(1,data_len);
  for(int i=0;i<data_len;i++){
    printf("doc:%u,region:%u \n",docs[i]->doc,docs[i]->region);
  }

  for(int i=0;i<data_len;i++)
    delete docs[i];
  delete []docs;
}

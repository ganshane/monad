#include "gtest/gtest.h"
#include <vector>

#include "bit_set_wrapper_holder.h"
#include "sparse_bit_set_wrapper.h"
#include "top_bit_set_wrapper.h"

using namespace monad;
class SparseBitSetWrapperTest: public ::testing::Test {
  protected:
  SparseBitSetWrapperTest() {
    }
  virtual ~SparseBitSetWrapperTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};

TEST_F(SparseBitSetWrapperTest, TestBitCount) {

  uint32_t length;
  monad::SparseBitSetWrapper wrapper;
  length = 100;
  wrapper.NewSeg(1,length);
  printf("c:%d \n",wrapper.BitCount());
  for(int i=0;i<length;i++){
    wrapper.FastSet(i);
    //printf("c:%d \n",wrapper.BitCount());
  }
  printf("c:%d \n",wrapper.BitCount());
  int32_t len=0;
  RegionDoc** topDocs = wrapper.Top(10,len);
  for(uint32_t i=0;i<len;i++){
    ASSERT_EQ(i,topDocs[i]->doc);
    delete topDocs[i];
  }
  delete[] topDocs;
}

TEST_F(SparseBitSetWrapperTest, TestRead) {
  /**
   val bitSet = new SparseFixedBitSet(10000)
   bitSet.set(1000)
   bitSet.set(3000)
   bitSet.set(5000)
   */
  SparseBitSetWrapper wrapper;
  wrapper.NewSeg(2,10000);
  wrapper.ReadNonZero(3);
  wrapper.ReadIndice(0,70368744210432);
  wrapper.ReadIndice(1,16384);

  wrapper.CreateBit(0, 3);
  wrapper.ReadBitBlock(0,0,1099511627776);
  wrapper.ReadBitBlock(0,1,72057594037927936);
  wrapper.ReadBitBlock(0,2,0);

  wrapper.CreateBit(1, 1);
  wrapper.ReadBitBlock(1,0,256);
  wrapper.FastSet(40);
  wrapper.Commit();

  ASSERT_TRUE(wrapper.FastGet(40));
  ASSERT_TRUE(wrapper.FastGet(1000));
  ASSERT_FALSE(wrapper.FastGet(2000));
  ASSERT_TRUE(wrapper.FastGet(3000));
  ASSERT_TRUE(wrapper.FastGet(5000));
}
TEST_F(SparseBitSetWrapperTest, TestInPlaceAndTopWithEmptyCollection) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  //wrapper2.NewSeg(2,2);
  wrapper2.NewSeg(2,2 * 64);
  //wrapper2.ReadLong(1LL << 40,0);
  wrapper2.FastSet(40);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceAndTop(holder,1);
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
TEST_F(SparseBitSetWrapperTest, TestEmpty) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  //wrapper.NewSeg(1,1);
  wrapper.NewSeg(1,1 * 64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.Commit();

  TopBitSetWrapper* topWrapper = SparseBitSetWrapper::InPlaceAndTop(holder,2);

  BitSetWrapperHolder<TopBitSetWrapper> holder2;
  holder2.AddWrapper(topWrapper);
  SparseBitSetWrapper::InPlaceAndTopWithPositionMerged(holder2,1);
  if(topWrapper)
    delete topWrapper;
}
TEST_F(SparseBitSetWrapperTest, TestInPlaceNot) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1*64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);
  wrapper.NewSeg(3,3*64);
  //wrapper.ReadLong(1LL << 38,1);
  wrapper.FastSet(38+1*64);
  wrapper.NewSeg(2,2*64);
  //wrapper.ReadLong(1LL << 40,0);
  wrapper.FastSet(40);
  //wrapper.ReadLong(1LL << 42,1);
  wrapper.FastSet(42+1*64);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2*64);
  //wrapper2.ReadLong(1LL << 40,0);
  wrapper2.FastSet(40);
  wrapper2.Commit();

  SparseBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceNot(holder);
  
  uint32_t index = 38;
  ASSERT_TRUE(bit_set->FastGet(index));
  index =  64+40;
  ASSERT_FALSE(bit_set->FastGet(index));
  index = 64+64+42;
  ASSERT_TRUE(bit_set->FastGet(index));
  delete bit_set;
}
TEST_F(SparseBitSetWrapperTest, TestInPlaceOr) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;

  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1*64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);
  wrapper.NewSeg(3,3*64);
  //wrapper.ReadLong(1LL << 38,1);
  wrapper.FastSet(38 + 1*64);
  wrapper.NewSeg(2,2*64);
  //wrapper.ReadLong(1LL << 40,0);
  wrapper.FastSet(40);
  //wrapper.ReadLong(1LL << 42,1);
  wrapper.FastSet(42+1*64);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2*64);
  //wrapper2.ReadLong(1LL << 50,0);
  wrapper2.FastSet(50);
  wrapper2.Commit();

  SparseBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceOr(holder);
  
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
TEST_F(SparseBitSetWrapperTest, TestInPlaceAndTopWithPositionMerged) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1*64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);
  wrapper.NewSeg(3,3*64);
  //wrapper.ReadLong(1LL << 38,1);
  wrapper.FastSet(38+1*64);
  wrapper.NewSeg(2,2*64);
  //wrapper.ReadLong(1LL << 40,0);
  wrapper.FastSet(40);
  //wrapper.ReadLong(1LL << 42,1);
  wrapper.FastSet(42+1*64);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2*64);
  //wrapper2.ReadLong(1LL << 40,0);
  wrapper2.FastSet(40);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceAndTop(holder,1);
  BitSetWrapperHolder<SparseBitSetWrapper> holder_;
  wrapper2 = SparseBitSetWrapper();
  holder_.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2*64);
  //wrapper2.ReadLong(1LL << 40,0);
  wrapper2.FastSet(40);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set2 = SparseBitSetWrapper::InPlaceAndTop(holder_,1);
  ASSERT_EQ(3,bit_set->Size());
  ASSERT_EQ(1,bit_set2->Size());

  BitSetWrapperHolder<TopBitSetWrapper> holder2;
  holder2.AddWrapper(bit_set);
  holder2.AddWrapper(bit_set2);
  TopBitSetWrapper* bit_set3 = SparseBitSetWrapper::InPlaceAndTopWithPositionMerged(holder2,1);
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
TEST_F(SparseBitSetWrapperTest, TestInPlaceAndTopMore) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  for(int i=0;i<33;i++){
    SparseBitSetWrapper* wrapper = new SparseBitSetWrapper();
    holder.AddWrapper(wrapper);
    wrapper->NewSeg(1,2*64);
    //wrapper->ReadLong(1LL << 40,0);
    wrapper->FastSet(40);
    //wrapper->ReadLong(1LL << 41,1);
    wrapper->FastSet(41+1*64);
    wrapper->Commit();
  }


  TopBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceAndTop(holder,1);
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
TEST_F(SparseBitSetWrapperTest, TestInPlaceAndTop) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1*64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);
  wrapper.NewSeg(3,3*64);
  //wrapper.ReadLong(1LL << 38,1);
  wrapper.FastSet(38+1*64);
  wrapper.NewSeg(2,2*64);
  //wrapper.ReadLong(1LL << 40,0);
  wrapper.FastSet(40);
  //wrapper.ReadLong(1LL << 42,1);
  wrapper.FastSet(42+1*64);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2*64);
  //wrapper2.ReadLong(1LL << 40,0);
  wrapper2.FastSet(40);
  wrapper2.Commit();

  TopBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceAndTop(holder,1);
  ASSERT_EQ(3,bit_set->Size());

  int32_t data_len=0;
  RegionTopDoc** docs = bit_set->Top(10,data_len);
  RegionTopDoc* top_doc = docs[0];
  ASSERT_EQ(2,top_doc->region);
  ASSERT_EQ(40,top_doc->top_doc->doc);
  ASSERT_EQ(2,top_doc->top_doc->freq);
  ASSERT_EQ(3,top_doc->top_doc->position[0]);
  top_doc = docs[1];
  ASSERT_EQ(2,top_doc->region);
  ASSERT_EQ(106,top_doc->top_doc->doc);
  ASSERT_EQ(1,top_doc->top_doc->freq);

  for(int i=0;i<data_len;i++)
    delete docs[i];
  delete []docs;
  delete bit_set;
}
TEST_F(SparseBitSetWrapperTest, TestInPlaceAnd) {
  BitSetWrapperHolder<SparseBitSetWrapper> holder;
  SparseBitSetWrapper wrapper;
  holder.AddWrapper(&wrapper);
  wrapper.NewSeg(1,1*64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);
  wrapper.NewSeg(3,3*64);
  //wrapper.ReadLong(1LL << 38,1);
  wrapper.FastSet(38+1*64);
  wrapper.NewSeg(2,2*64);
  //wrapper.ReadLong(1LL << 40,0);
  wrapper.FastSet(40);
  //wrapper.ReadLong(1LL << 42,1);
  wrapper.FastSet(42+1*64);
  wrapper.Commit();

  SparseBitSetWrapper wrapper2;
  holder.AddWrapper(&wrapper2);
  wrapper2.NewSeg(2,2*64);
  //wrapper2.ReadLong(1LL << 40,0);
  wrapper2.FastSet(40);
  wrapper2.Commit();

  SparseBitSetWrapper* bit_set = SparseBitSetWrapper::InPlaceAnd(holder);
  
  uint32_t index = 38;
  ASSERT_FALSE(bit_set->FastGet(index));
  //因为第一个集合合并消失了，所以从40开始有数据
  index =  40;
  ASSERT_TRUE(bit_set->FastGet(index));
  delete bit_set;
}
TEST_F(SparseBitSetWrapperTest, TestGetSet) {
  SparseBitSetWrapper wrapper;
  wrapper.NewSeg(1,1*64);
  //wrapper.ReadLong(1LL << 38,0);
  wrapper.FastSet(38);

  wrapper.NewSeg(3,3*64);
  //wrapper.ReadLong(1LL << 38,1);
  wrapper.FastSet(38+1*64);

  wrapper.NewSeg(2,2*64);
  //wrapper.ReadLong(1LL << 40,0);
  wrapper.FastSet(40);
  //wrapper.ReadLong(1LL << 42,1);
  wrapper.FastSet(42+1*64);

  wrapper.Commit();

  uint32_t index = (1*64)+ 40;
  ASSERT_TRUE(wrapper.FastGet(index));
  index = 64 + (64 + 42);
  ASSERT_TRUE(wrapper.FastGet(index));
  index = 64 + (64*2)+ 64 + 38;
  ASSERT_TRUE(wrapper.FastGet(index));
  ASSERT_EQ(4,wrapper.BitCount());

  wrapper = SparseBitSetWrapper();
  wrapper.NewSeg(1, 10*64);
  //wrapper.ReadLong(1LL << 20, 2);
  wrapper.FastSet(20+ 2*64);
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

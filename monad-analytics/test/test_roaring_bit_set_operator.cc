// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <sparse_bit_set.h>

#include "bit_set_operator.h"
#include "open_bit_set.h"
#include "roaring_bit_set_iterator.h"
#include "top_bit_set.h"
#include "top_bit_set_iterator.h"
using namespace monad;
class RoaringBitSetOperatorTest: public ::testing::Test {
  protected:
  RoaringBitSetOperatorTest() {
    }
  virtual ~RoaringBitSetOperatorTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(RoaringBitSetOperatorTest, TestInPlaceOr) {
  RoaringBitSet bit_set;
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10 + 64);
  //bit_set.ReadLong(1<< 10,2);
  bit_set.Set(10 + (2*64));

  RoaringBitSet bit_set1;
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10 +(1*64));
  //bit_set1.ReadLong(1<< 11,1);
  bit_set1.Set(11 + (1*64));



  RoaringBitSet* coll[] ={&bit_set,&bit_set1};

  RoaringBitSet* result = RoaringBitSetOperator::InPlaceOr(coll,2);
  //ASSERT_EQ(3,result->GetNumWords());
  /*
  RoaringBitSetIterator it(*result);
  while(it.NextDoc() != RoaringBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_TRUE(result->Get(index));
  index += 64;
  ASSERT_TRUE(result->Get(index));
  index += 1;
  ASSERT_TRUE(result->Get(index));
  
  delete result;
}
TEST_F(RoaringBitSetOperatorTest, TestNot) {
  RoaringBitSet bit_set;
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10+ 1 * 64);

  RoaringBitSet bit_set1;
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10 + 1 * 64);



  RoaringBitSet* coll[] ={&bit_set,&bit_set1};

  RoaringBitSet* result = RoaringBitSetOperator::InPlaceNot(coll,2);
  //ASSERT_EQ(10,result->GetNumWords());
  /*
  RoaringBitSetIterator it(*result);
  while(it.NextDoc() != RoaringBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_TRUE(result->Get(index));
  index += 64;
  ASSERT_FALSE(result->Get(index));

  
  delete result;
}
TEST_F(RoaringBitSetOperatorTest, TestAnd) {
  RoaringBitSet bit_set;
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(10,1);
  bit_set.Set(10+64);

  RoaringBitSet bit_set1;
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10+64);



  RoaringBitSet* coll[] ={&bit_set,&bit_set1};

  RoaringBitSet* result = RoaringBitSetOperator::InPlaceAnd(coll,2);
  //ASSERT_EQ(2,result->GetNumWords());
  /*
  RoaringBitSetIterator it(*result);
  while(it.NextDoc() != RoaringBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_FALSE(result->Get(index));
  index += 64;
  ASSERT_TRUE(result->Get(index));

  
  delete result;
}
TEST_F(RoaringBitSetOperatorTest, TestInPlaceAndTopPerformance) {
  uint32_t num_words = 100000000 / 64; //mod 64
  RoaringBitSet bit_set;
  printf("num words:%d \n",num_words);
  for(int i=0;i<num_words;i++){
	  //bit_set.ReadLong((1LLU<<63)-1 ,i);
    for(int j=0;j<63;j++){
       bit_set.Set(j+ i*64);
    }
  }

  RoaringBitSet* coll[] ={&bit_set};

  TopBitSet* result = RoaringBitSetOperator::InPlaceAndTop(coll,1,1);
  delete result;
}
TEST_F(RoaringBitSetOperatorTest, TestInPlaceAndTop) {
  RoaringBitSet bit_set;
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10+ 1 * 64);

  RoaringBitSet bit_set1;
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10+ 1 * 64);



  RoaringBitSet* coll[] ={&bit_set,&bit_set1};

  TopBitSet* result = RoaringBitSetOperator::InPlaceAndTop(coll,2,1);
  TopBitSetIterator it = result->Iterator();
  while(it.NextDoc() != RoaringBitSetIterator::NO_MORE_DOCS){
    TopDoc* doc = it.Doc();
    printf("doc_id :%u freq:%u position:%lld \n",doc->doc,doc->freq,doc->position[0]);
  }
  it = result->Iterator();

  ASSERT_EQ(10,it.NextDoc());
  TopDoc* doc = it.Doc();
  ASSERT_EQ(1,doc->freq);
  ASSERT_EQ(1,doc->position[0]);

  ASSERT_EQ(74,it.NextDoc());
  doc = it.Doc();
  ASSERT_EQ(2,doc->freq);
  ASSERT_EQ(3,doc->position[0]);

  ASSERT_EQ(RoaringBitSetIterator::NO_MORE_DOCS,it.NextDoc());
  delete result;
}
TEST_F(RoaringBitSetOperatorTest, TestInPlaceAndTopWithPositionMerged) {
  RoaringBitSet bit_set;
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10 +1 *64);

  RoaringBitSet bit_set1;
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10 +1 *64);

  RoaringBitSet bit_set2;
  //bit_set2.ReadLong(1LLU << 47,2);
  bit_set2.Set(47 +2 *64);



  RoaringBitSet* coll[] ={&bit_set,&bit_set1};

  TopBitSet* result = RoaringBitSetOperator::InPlaceAndTop(coll,2,1);
  RoaringBitSet* coll_1[] = {&bit_set,&bit_set1,&bit_set2};
  TopBitSet* result2 = RoaringBitSetOperator::InPlaceAndTop(coll_1,3,1);

  TopBitSet* coll_2[]={result,result2};
  TopBitSet* result3 = RoaringBitSetOperator::InPlaceAndTopWithPositionMerged(coll_2,2,1);
  TopBitSetIterator it = result3->Iterator();
  while(it.NextDoc() != RoaringBitSetIterator::NO_MORE_DOCS){
    TopDoc* doc = it.Doc();
    printf("doc_id :%u freq:%u position:%lld \n",doc->doc,doc->freq,doc->position[0]);
  }

  it = result3->Iterator();

  ASSERT_EQ(10,it.NextDoc());
  TopDoc* doc = it.Doc();
  ASSERT_EQ(2,doc->freq);
  ASSERT_EQ(1,doc->position[0]);

  ASSERT_EQ(74,it.NextDoc());
  doc = it.Doc();
  ASSERT_EQ(2,doc->freq);
  ASSERT_EQ(3,doc->position[0]);

  ASSERT_EQ(175,it.NextDoc());
  doc = it.Doc();
  ASSERT_EQ(1,doc->freq);
  ASSERT_EQ(4,doc->position[0]);

  ASSERT_EQ(RoaringBitSetIterator::NO_MORE_DOCS,it.NextDoc());
  delete result;
  delete result2;
  delete result3;
}

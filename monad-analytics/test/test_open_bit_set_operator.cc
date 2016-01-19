// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>

#include "bit_set_operator.h"
#include "open_bit_set.h"
#include "open_bit_set_iterator.h"
#include "top_bit_set.h"
#include "top_bit_set_iterator.h"
using namespace monad;
class OpenBitSetOperatorTest: public ::testing::Test {
  protected:
  OpenBitSetOperatorTest() {
    }
  virtual ~OpenBitSetOperatorTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(OpenBitSetOperatorTest, TestInPlaceOr) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);
  bit_set.ReadLong(1<< 10,2);
  bit_set.TrimTrailingZeros();

  OpenBitSet bit_set1(2);
  bit_set1.ReadLong(1<< 10,1);
  bit_set1.ReadLong(1<< 11,1);



  OpenBitSet* coll[] ={&bit_set,&bit_set1};

  OpenBitSet* result = OpenBitSetOperator::InPlaceOr(coll,2);
  ASSERT_EQ(3,result->GetNumWords());
  /*
  OpenBitSetIterator it(*result);
  while(it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_TRUE(result->FastGet(index));
  index += 64;
  ASSERT_TRUE(result->FastGet(index));
  index += 1;
  ASSERT_TRUE(result->FastGet(index));
  
  delete result;
}
TEST_F(OpenBitSetOperatorTest, TestNot) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);

  OpenBitSet bit_set1(2);
  bit_set1.ReadLong(1<< 10,1);



  OpenBitSet* coll[] ={&bit_set,&bit_set1};

  OpenBitSet* result = OpenBitSetOperator::InPlaceNot(coll,2);
  ASSERT_EQ(10,result->GetNumWords());
  /*
  OpenBitSetIterator it(*result);
  while(it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_TRUE(result->FastGet(index));
  index += 64;
  ASSERT_FALSE(result->FastGet(index));

  
  delete result;
}
TEST_F(OpenBitSetOperatorTest, TestAnd) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);

  OpenBitSet bit_set1(2);
  bit_set1.ReadLong(1<< 10,1);



  OpenBitSet* coll[] ={&bit_set,&bit_set1};

  OpenBitSet* result = OpenBitSetOperator::InPlaceAnd(coll,2);
  ASSERT_EQ(2,result->GetNumWords());
  /*
  OpenBitSetIterator it(*result);
  while(it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_FALSE(result->FastGet(index));
  index += 64;
  ASSERT_TRUE(result->FastGet(index));

  
  delete result;
}
TEST_F(OpenBitSetOperatorTest, TestInPlaceAndTopPerformance) {
  uint32_t num_words = 100000000 / 64; //mod 64
  OpenBitSet bit_set(num_words);
  printf("num words:%d \n",num_words);
  for(int i=0;i<num_words;i++){
	  bit_set.ReadLong((1LLU<<63)-1 ,i);
  }

  OpenBitSet* coll[] ={&bit_set};

  TopBitSet* result = OpenBitSetOperator::InPlaceAndTop(coll,1,1);
  delete result;
}
TEST_F(OpenBitSetOperatorTest, TestInPlaceAndTop) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);

  OpenBitSet bit_set1(2);
  bit_set1.ReadLong(1<< 10,1);



  OpenBitSet* coll[] ={&bit_set,&bit_set1};

  TopBitSet* result = OpenBitSetOperator::InPlaceAndTop(coll,2,1);
  TopBitSetIterator it = result->Iterator();
  while(it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS){
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

  ASSERT_EQ(OpenBitSetIterator::NO_MORE_DOCS,it.NextDoc());
  delete result;
}
TEST_F(OpenBitSetOperatorTest, TestInPlaceAndTopWithPositionMerged) {
  OpenBitSet bit_set(10);
  bit_set.ReadLong(1<< 10,0);
  bit_set.ReadLong(1<< 10,1);

  OpenBitSet bit_set1(2);
  bit_set1.ReadLong(1<< 10,1);

  OpenBitSet bit_set2(3);
  bit_set2.ReadLong(1LLU << 47,2);



  OpenBitSet* coll[] ={&bit_set,&bit_set1};

  TopBitSet* result = OpenBitSetOperator::InPlaceAndTop(coll,2,1);
  OpenBitSet* coll_1[] = {&bit_set,&bit_set1,&bit_set2};
  TopBitSet* result2 = OpenBitSetOperator::InPlaceAndTop(coll_1,3,1);

  TopBitSet* coll_2[]={result,result2};
  TopBitSet* result3 = OpenBitSetOperator::InPlaceAndTopWithPositionMerged(coll_2,2,1);
  TopBitSetIterator it = result3->Iterator();
  while(it.NextDoc() != OpenBitSetIterator::NO_MORE_DOCS){
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

  ASSERT_EQ(OpenBitSetIterator::NO_MORE_DOCS,it.NextDoc());
  delete result;
  delete result2;
  delete result3;
}

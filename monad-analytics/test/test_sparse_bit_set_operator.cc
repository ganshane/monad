#include "gtest/gtest.h"
#include <vector>
#include <sparse_bit_set.h>

#include "open_bit_set.h"
#include "open_bit_set_operator.h"
#include "sparse_bit_set_iterator.h"
#include "sparse_bit_set_operator.h"
#include "top_bit_set.h"
#include "top_bit_set_iterator.h"
using namespace monad;
class SparseBitSetOperatorTest: public ::testing::Test {
  protected:
  SparseBitSetOperatorTest() {
    }
  virtual ~SparseBitSetOperatorTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
TEST_F(SparseBitSetOperatorTest, TestInPlaceOr) {
  SparseBitSet bit_set(10*64);
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10 + 64);
  //bit_set.ReadLong(1<< 10,2);
  bit_set.Set(10 + (2*64));

  SparseBitSet bit_set1(2*64);
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10 +(1*64));
  //bit_set1.ReadLong(1<< 11,1);
  bit_set1.Set(11 + (1*64));



  SparseBitSet* coll[] ={&bit_set,&bit_set1};

  SparseBitSet* result = SparseBitSetOperator::InPlaceOr(coll,2);
  //ASSERT_EQ(3,result->GetNumWords());
  /*
  SparseBitSetIterator it(*result);
  while(it.NextDoc() != SparseBitSetIterator::NO_MORE_DOCS){
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
TEST_F(SparseBitSetOperatorTest, TestNot) {
  SparseBitSet bit_set(10*64);
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10+ 1 * 64);

  SparseBitSet bit_set1(2*64);
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10 + 1 * 64);



  SparseBitSet* coll[] ={&bit_set,&bit_set1};

  SparseBitSet* result = SparseBitSetOperator::InPlaceNot(coll,2);
  //ASSERT_EQ(10,result->GetNumWords());
  /*
  SparseBitSetIterator it(*result);
  while(it.NextDoc() != SparseBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_TRUE(result->FastGet(index));
  index += 64;
  ASSERT_FALSE(result->FastGet(index));

  
  delete result;
}
TEST_F(SparseBitSetOperatorTest, TestAnd) {
  SparseBitSet bit_set(10*64);
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(10,1);
  bit_set.Set(10+64);

  SparseBitSet bit_set1(2*64);
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10+64);



  SparseBitSet* coll[] ={&bit_set,&bit_set1};

  SparseBitSet* result = SparseBitSetOperator::InPlaceAnd(coll,2);
  //ASSERT_EQ(2,result->GetNumWords());
  /*
  SparseBitSetIterator it(*result);
  while(it.NextDoc() != SparseBitSetIterator::NO_MORE_DOCS){
    printf("doc %d\n",it.DocId());
  }
   */
  uint32_t index = 10;
  ASSERT_FALSE(result->FastGet(index));
  index += 64;
  ASSERT_TRUE(result->FastGet(index));

  
  delete result;
}
TEST_F(SparseBitSetOperatorTest, TestInPlaceAndTopPerformance) {
  uint32_t num_words = 100000000 / 64; //mod 64
  SparseBitSet bit_set((num_words+1)*64);
  printf("num words:%d \n",num_words);
  for(int i=0;i<num_words;i++){
	  //bit_set.ReadLong((1LLU<<63)-1 ,i);
    for(int j=0;j<63;j++){
       bit_set.Set(j+ i*64);
    }
  }

  SparseBitSet* coll[] ={&bit_set};

  TopBitSet* result = SparseBitSetOperator::InPlaceAndTop(coll,1,1);
  delete result;
}
TEST_F(SparseBitSetOperatorTest, TestInPlaceAndTop) {
  SparseBitSet bit_set(10*64);
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10+ 1 * 64);

  SparseBitSet bit_set1(2*64);
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10+ 1 * 64);



  SparseBitSet* coll[] ={&bit_set,&bit_set1};

  TopBitSet* result = SparseBitSetOperator::InPlaceAndTop(coll,2,1);
  TopBitSetIterator it = result->Iterator();
  while(it.NextDoc() != SparseBitSetIterator::NO_MORE_DOCS){
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

  ASSERT_EQ(SparseBitSetIterator::NO_MORE_DOCS,it.NextDoc());
  delete result;
}
TEST_F(SparseBitSetOperatorTest, TestInPlaceAndTopWithPositionMerged) {
  SparseBitSet bit_set(10 * 64);
  //bit_set.ReadLong(1<< 10,0);
  bit_set.Set(10);
  //bit_set.ReadLong(1<< 10,1);
  bit_set.Set(10 +1 *64);

  SparseBitSet bit_set1(2 * 64);
  //bit_set1.ReadLong(1<< 10,1);
  bit_set1.Set(10 +1 *64);

  SparseBitSet bit_set2(3 * 64);
  //bit_set2.ReadLong(1LLU << 47,2);
  bit_set2.Set(47 +2 *64);



  SparseBitSet* coll[] ={&bit_set,&bit_set1};

  TopBitSet* result = SparseBitSetOperator::InPlaceAndTop(coll,2,1);
  SparseBitSet* coll_1[] = {&bit_set,&bit_set1,&bit_set2};
  TopBitSet* result2 = SparseBitSetOperator::InPlaceAndTop(coll_1,3,1);

  TopBitSet* coll_2[]={result,result2};
  TopBitSet* result3 = SparseBitSetOperator::InPlaceAndTopWithPositionMerged(coll_2,2,1);
  TopBitSetIterator it = result3->Iterator();
  while(it.NextDoc() != SparseBitSetIterator::NO_MORE_DOCS){
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

  ASSERT_EQ(SparseBitSetIterator::NO_MORE_DOCS,it.NextDoc());
  delete result;
  delete result2;
  delete result3;
}

#include "gtest/gtest.h"
#include <vector>

#include "priority_queue.h"

using namespace monad;
class PriorityQueueTest: public ::testing::Test {
  protected:
  PriorityQueueTest() {
    }
  virtual ~PriorityQueueTest() {
    }
  virtual void SetUp() {
    }

  virtual void TearDown() {
    }
};
bool CompareInt32(int32_t* a,int32_t*b){
    return ((*a)<(*b));
};
TEST_F(PriorityQueueTest, TestFunction) {
  PriorityQueue<int32_t> queue(10,CompareInt32);
  int32_t a=12;
  queue.InsertWithOverflow(&a);
  int32_t b=13;
  queue.InsertWithOverflow(&b);

  ASSERT_EQ(2,queue.Size());
  ASSERT_EQ(12,*queue.Pop());
  ASSERT_EQ(13,*queue.Pop());
}
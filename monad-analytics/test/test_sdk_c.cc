// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <sdk/monad_sdk_impl.h>
#include <sdk/monad_sdk.h>
#include <fstream>
#include <dirent.h>
#include <sys/fcntl.h>


using namespace monad;
const char path[100]= "test.db";
class MonadSDKTest: public ::testing::Test {
protected:
  void* sdk;
  MonadSDKTest() {
    monad_coll_create(&sdk,path,50 * 1024 * 1024);
  }
  virtual ~MonadSDKTest() {
    monad_coll_release(sdk);
    MonadSDK::Destroy(path);
  }
  virtual void SetUp() {
  }

  virtual void TearDown() {
  }
  void EncodeFixed32(char* buf, uint32_t value) {
    buf[0] = value & 0xff;
    buf[1] = (value >> 8) & 0xff;
    buf[2] = (value >> 16) & 0xff;
    buf[3] = (value >> 24) & 0xff;
  }
};
TEST_F(MonadSDKTest, TestRead) {
  std::string id_("110101201012167368");
  
  monad_coll_put_id(sdk,id_.c_str(),id_.size());
  std::string id("413028199009121514");
  monad_coll_put_id(sdk,id.c_str(),id.size());
  
  ASSERT_TRUE(monad_coll_contain_id(sdk,id.c_str(),id.size()));
  std::string id2("413028199009121524");
  ASSERT_FALSE(monad_coll_contain_id(sdk,id2.c_str(),id2.size()));
}
TEST_F(MonadSDKTest,TestPerformance){
  const char path[50]="/tmp/monad";
  char  childpath[512];

  DIR* pDir=opendir(path);
  struct dirent    *ent  ;
  while((ent=readdir(pDir))!=NULL)
  {
    if(ent->d_type & DT_DIR)
    {
      if(strcmp(ent->d_name,".")==0 || strcmp(ent->d_name,"..")==0)
        continue;
      sprintf(childpath,"%s/%s",path,ent->d_name);
      printf("path:%s/n",childpath);
    }
    else
    {
//      std::cout<<ent->d_name<<std::endl;
      uint32_t region_id = std::stoi(ent->d_name);
      sprintf(childpath,"%s/%s",path,ent->d_name);
      FILE* fp = fopen(childpath,"rb");
      fseek(fp, 0L, SEEK_END);
      uint32_t sz = ftell(fp);
      fseek(fp, 0L, SEEK_SET);
      char* buffer = (char *) malloc(sz + 4);
      EncodeFixed32(buffer,region_id);
      fread(buffer+4, sz, 1, fp);
      fclose(fp);
      monad_coll_put_seg(sdk,buffer,sz);
      free(buffer);
    }
  }

  clock_t   start,   finish;
  start   =   clock();

  std::ifstream fin("/Users/jcai/Downloads/sfzh-1.txt", std::ios::in);

  char line[1024]={0};
  std::string id;
  auto i = 0;
  int32_t true_int = 0;
  int32_t false_int = 0;
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    bool flag =  monad_coll_contain_id(sdk,id.c_str(),id.size());
    if(flag) true_int++;else false_int++;
    i++;
    if(i >1000000)
      break;
  }
  fin.close();
  finish = clock();
  double duration =    (double)(finish   -   start)/CLOCKS_PER_SEC ;


  std::cout << "time::" << duration << " true:::" << true_int << " false::"<< false_int << std::endl;
}

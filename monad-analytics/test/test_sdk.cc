// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#include "gtest/gtest.h"
#include <vector>
#include <sdk/monad_sdk_impl.h>
#include <fstream>
#include <dirent.h>
#include <sys/fcntl.h>


using namespace monad;
const char path[100]= "test.db";
class MonadSDKTest: public ::testing::Test {
protected:
  MonadSDK* sdk;
  MonadSDKTest() {
    sdk = new MonadSDK(path);
  }
  virtual ~MonadSDKTest() {
    delete sdk;
    MonadSDK::Destroy(path);
  }
  virtual void SetUp() {
  }

  virtual void TearDown() {
  }
};
TEST_F(MonadSDKTest, TestRead) {
  std::string id_("110101201012167368");
  sdk->PutId(id_.c_str(),id_.size());
  std::string id("413028199009121514");
  sdk->PutId(id.c_str(),id.size());
  ASSERT_TRUE(sdk->ContainId(id.c_str(),id.size()));
  std::string id2("413028199009121524");
  ASSERT_FALSE(sdk->ContainId(id2.c_str(),id2.size()));
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
      char* buffer = (char *) malloc(sz);
      fread(buffer, sz, 1, fp);
      fclose(fp);
      sdk->PutCollection(region_id,buffer,sz);
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
    bool flag =  sdk->ContainId(id.c_str(),id.size());
    if(flag) true_int++;else false_int++;
    i++;
    /*
    if(i >1000000)
      break;
      */
  }
  fin.close();
  finish = clock();
  double duration =    (double)(finish   -   start)/CLOCKS_PER_SEC ;


  std::cout << "time::" << duration << " true:::" << true_int << " false::"<< false_int << std::endl;
}
/*
TEST_F(MonadSDKTest,TestKVPerformance){
  std::ifstream fin("/Users/jcai/Downloads/sfzh.txt", std::ios::in);

  char line[1024]={0};
  std::string id;
  auto i = 0;
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    sdk->PutKV(id,id);
    i++;
//    std::cout << i << " --> "<< id << std::endl;
    //if(i >100000)
    // break;
  }
  fin.clear();
  fin.close();
}

TEST_F(MonadSDKTest,TestPerformance){
  std::ifstream fin("/Users/jcai/Downloads/sfzh.txt", std::ios::in);

  char line[1024]={0};
  std::string id;
  auto i = 0;
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    sdk->PutId(id.c_str(),id.size());
    i++;
//    std::cout << i << " --> "<< id << std::endl;
    //if(i >100000)
     // break;
  }
  fin.clear();
  fin.close();
}
 */


#include <dirent.h>
#include <fstream>
#include <iostream>
#include <string>
#include <stdlib.h>
#include <stdio.h>
#include <sys/stat.h>
#include <time.h>
#include <snappy.h>

#include "monad_sdk.h"
#include "monad_sdk_impl.h"


void* sdk=NULL;
void execute_match(const char* prefix,const char* sfzh_path){
  clock_t   start,   finish,seg_start,seg_finish;
  start   =   clock();
  seg_start = clock();
  double duration,total_duration;

  uint32_t mask = (1 << 21) -1;

  std::ifstream fin(sfzh_path, std::ios::in);
  char line[1024]={0};
  std::string id;
  int i = 0;
  int32_t true_int = 0;
  int32_t false_int = 0;
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    bool flag =  monad_coll_contain_id(sdk,id.c_str(),id.size());
    if(flag) true_int++;
    else {
      false_int++;
    }
    i++;
    if((i & mask) == 0){
      seg_finish = clock();
      duration = (double)(seg_finish-seg_start)/CLOCKS_PER_SEC;
      total_duration = (double)(seg_finish-start)/CLOCKS_PER_SEC;
      std::cout << prefix << i << " line processed " <<" seg time:" << duration <<" total time:" << total_duration <<std::endl;
      seg_start = clock();
    }
  }
  fin.close();
  finish = clock();
  duration =    (double)(finish   -   start)/CLOCKS_PER_SEC ;
  std::cout << prefix<<"total:" << i <<" time::" << duration << " true:::" << true_int << " false::"<< false_int << std::endl;
}
void performance(const char* path,const char* sfzh_path){
//  const char path[50]="/tmp/monad";
  char  childpath[512];

  DIR* pDir=opendir(path);
  struct dirent    *ent  ;
  struct stat s;
//  int before=0;
//  int after = 0;
  while((ent=readdir(pDir))!=NULL)
  {
    memset(&s, 0,sizeof(struct stat));
    stat(ent->d_name, &s);
    if (s.st_mode & S_IFDIR)
    {
      if(strcmp(ent->d_name,".")==0 || strcmp(ent->d_name,"..")==0)
        continue;
//      sprintf(childpath,"%s/%s",path,ent->d_name);
//      printf("path:%s/n",childpath);
    }
    else
    {
//      std::cout<<ent->d_name<<std::endl;
      uint32_t region_id = atoi(ent->d_name);
      sprintf(childpath,"%s/%s",path,ent->d_name);
      FILE* fp = fopen(childpath,"rb");
      fseek(fp, 0L, SEEK_END);
      uint32_t sz = ftell(fp);
      fseek(fp, 0L, SEEK_SET);
      char* buffer = (char *) malloc(sz + 4);
      monad::MonadSDK::EncodeFixed32WithBigEndian(buffer,region_id);
      fread(buffer+4, sz, 1, fp);
      fclose(fp);

      /*
      size_t input_length = sz + 4;
      char* output = new char[snappy::MaxCompressedLength(input_length)];
      size_t output_length;
      snappy::RawCompress(buffer, input_length, output, &output_length);
      delete [] output;

      before += input_length;
      after += output_length;
       */

      monad_coll_put_seg(sdk,buffer,sz+4);
      free(buffer);
    }
  }
//  std::cout << " beofore size :" << before << " compressed size:"<<after<< std::endl;

  execute_match("first--> ",sfzh_path);
  execute_match("second--> ",sfzh_path);
}
void performance2(char* sfzh_path){
  clock_t   start,   finish;
  start   =   clock();

  char line[1024]={0};
  std::string id;
  int i = 0;
  double duration = 0;
  int mask = (1 << 20 ) - 1;
  /*
  std::ifstream fin(sfzh_path, std::ios::in);

  char data[12] = {0};
  while(fin.getline(line, sizeof(line))){
    id.assign(line);
    monad_coll_put_kv(sdk,id.c_str(),id.size(),data,sizeof(data));
    i++;
    if((i&mask) == 0)
      std::cout << "i " <<i << std::endl;
  }
  fin.close();
  finish = clock();
  duration =    (double)(finish   -   start)/CLOCKS_PER_SEC ;
  std::cout << "write:" << i <<" time::" << duration << std::endl;
   */

  start = clock();
  std::ifstream fin2(sfzh_path, std::ios::in);
  int32_t true_int = 0;
  int32_t false_int = 0;
  char* value;
  size_t size;
  while(fin2.getline(line, sizeof(line))){
    id.assign(line);
    MONAD_CODE ret= monad_coll_get_kv(sdk,id.c_str(),id.size(),&value,&size);
    if(ret == MONAD_OK){
      free(value);
      true_int += 1;
    }else false_int += 1;
    i++;
    if((i&mask) == 0)
      std::cout << "i " <<i << std::endl;
    if(i > 1000000)
      break;
  }
  fin2.close();
  finish = clock();
  duration =    (double)(finish   -   start)/CLOCKS_PER_SEC;
  std::cout << "total:" << i <<" time::" << duration << " true:::" << true_int << " false::"<< false_int << std::endl;


}
int main(int argc, char *argv[]){
  const char path[100]= "test.db";
  monad_coll_create(&sdk,path,50 * 1024 * 1024);
  std::string id_("110101201012167368");

  monad_coll_put_id(sdk,id_.c_str(),id_.size());
  std::string id("413028199009121514");
  monad_coll_put_id(sdk,id.c_str(),id.size());

  bool r = monad_coll_contain_id(sdk,id.c_str(),id.size());
  std::cout << "expected:" << "1 real:" << r << std::endl;
  std::string id2("413028199009121524");
  r = monad_coll_contain_id(sdk,id2.c_str(),id2.size());
  std::cout << "expected:" << "0 real:"<< r <<std::endl;


  if(argc == 3){
    std::cout << "db path:" << argv[1] << " sfzh path: " << argv[2] <<std::endl;
    performance(argv[1],argv[2]);
//    performance2(argv[2]);
  }
  monad_coll_release(sdk);

  return 0;
}

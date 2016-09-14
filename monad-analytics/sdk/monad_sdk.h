// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com

#ifndef MONAD_SDK_H_
#define MONAD_SDK_H_

#include "monad_sdk_code.h"
#ifdef __cplusplus
extern "C" {
#endif

/**
 * 根据给定的handle以及文件路径创建
 * @param handle 对象的handle
 * @param file_path 数据写入的文件路径
 */
MONAD_CODE monad_coll_create(void ** handle,char* file_path);
/**
 * 放入对象ID到容器中
 * @param handle 操作的handle对象
 * @param id_card 待放入的对象ID
 * @param size 对象ID的长度
 */
MONAD_CODE monad_coll_put_id(void* handle,char* id_card ,size_t size);
/**
 * 是否包含给定的对象ID
 * @param handle 操作的handle对象
 * @param id_card 待操作的对象ID
 * @param size 对象
 */
bool monad_coll_contain_id(void* handle,char* id_card,size_t size);

/**
 * 设置某一区域的的值,通常用于测试
 */
MONAD_CODE monad_coll_set(void* handle,uint32_t region_id,int32_t i);
/**
 * 判断某一区域的值存在否,通常用户测试
 */
bool monad_coll_contain(void* handle,uint32_t region_id,int32_t i);

/**
 * 释放内存空间,关闭数据库
 */
MONAD_CODE monad_coll_release(void* handle);

#ifdef __cplusplus
}
#endif

#endif //MONAD_SDK_H_

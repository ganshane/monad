// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com

#ifndef MONAD_SDK_H_
#define MONAD_SDK_H_

#include <stdint.h>
#include <stddef.h>
#include "monad_sdk_code.h"

#ifdef DLL_EXPORT
#     define MONAD_EXPORT __declspec(dllexport)
#else
#     define MONAD_EXPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 根据给定的handle以及文件路径创建
 * @param handle 对象的handle
 * @param db_path 数据写入的文件路径
 * @param cache_ram 支持的缓存内存大小
 */
MONAD_EXPORT MONAD_CODE monad_coll_create(void ** handle,const char* db_path,const uint32_t cache_ram);
/**
 * 放入某一数据,此数据通常是分割后的,方便从网络进行下载
 * @param handle 操作对象
 * @param data 分段数据
 * @param size 分段数据的大小
 */
MONAD_EXPORT void monad_coll_put_seg(void* handle,const char* data,const size_t size);
/**
 * 是否包含给定的对象ID
 * @param handle 操作的handle对象
 * @param id_card 待操作的对象ID
 * @param size 对象
 */
MONAD_EXPORT bool monad_coll_contain_id(void* handle,const char* id_card,const size_t size);

/**
 * 放入对象ID到容器中
 * @param handle 操作的handle对象
 * @param id_card 待放入的对象ID
 * @param size 对象ID的长度
 */
MONAD_EXPORT MONAD_CODE monad_coll_put_id(void* handle,const char* id_card ,const size_t size);

/**
 * 释放内存空间,关闭数据库
 */
MONAD_EXPORT void monad_coll_release(void* handle);

#ifdef __cplusplus
}
#endif

#endif //MONAD_SDK_H_

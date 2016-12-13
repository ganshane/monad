#ifndef MONAD_COLLECTION_HELPER_H_
#define MONAD_COLLECTION_HELPER_H_
namespace monad {
  /**
   * 从buffer中读取一个32bit的整数
   */
  inline static uint32_t ReadUint32(char** buffer){
    char* bb = *buffer;
    uint32_t i = 0;
    i |= bb[0] << 24;
    i |= bb[1] << 16;
    i |= bb[2] << 8;
    i |= bb[3] ;
    *buffer = bb+4;

    return i;
  }
  /**
   * 从buffer重读取一个64bit的数
   */
  inline static uint64_t ReadUint64(char** buffer){
    uint64_t hi = ReadUint32(buffer);
    uint64_t lo = ReadUint32(buffer);
    return  (hi << 32) | lo;
  }
}
#endif //MONAD_COLLECTION_HELPER_H_

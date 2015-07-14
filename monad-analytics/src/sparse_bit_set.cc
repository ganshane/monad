// Copyright (c) 2015 Jun Tsai. All rights reserved.

#include <assert.h>
#include <string.h>
#include "sparse_bit_set.h"

namespace monad{
  const uint32_t NO_MORE_DOCS = UINT32_MAX;
  static uint32_t numberOfTrailingZeros(uint64_t i) {
    // HD, Figure 5-14
    uint32_t x, y;
    if (i == 0) return 64;
    int n = 63;
    y = (int)i; if (y != 0) { n = n -32; x = y; } else x = (int)(i>>32);
    y = x <<16; if (y != 0) { n = n -16; x = y; }
    y = x << 8; if (y != 0) { n = n - 8; x = y; }
    y = x << 4; if (y != 0) { n = n - 4; x = y; }
    y = x << 2; if (y != 0) { n = n - 2; x = y; }
    return n - ((x << 1) >> 31);
  }
  static uint32_t numberOfLeadingZeros(uint64_t i) {
    // HD, Figure 5-6
    if (i == 0)
      return 64;
    uint32_t n = 1;
    uint32_t x = (uint32_t)(i >> 32);
    if (x == 0) { n += 32; x = (uint32_t)i; }
    if (x >> 16 == 0) { n += 16; x <<= 16; }
    if (x >> 24 == 0) { n +=  8; x <<=  8; }
    if (x >> 28 == 0) { n +=  4; x <<=  4; }
    if (x >> 30 == 0) { n +=  2; x <<=  2; }
    n -= x >> 31;
    return n;
  }
  static uint32_t bitCount(uint64_t x)
  {
    x = x - ((x >> 1ULL) & 0x5555555555555555ULL);
    x = (x & 0x3333333333333333ULL) + ((x >>2ULL) & 0x3333333333333333ULL);
    x = (x + (x >> 4ULL)) & 0x0f0f0f0f0f0f0f0fULL;
    x = x + (x >> 8ULL);
    x = x + (x >> 16ULL);
    x = x + (x >> 32ULL);
    return (uint32_t)x & 0x7f;
  }
  static uint32_t calBlockCount(uint32_t length) {
    uint32_t blockCount = length >> 12;
    if ((blockCount << 12) < length) {
      ++blockCount;
    }
    assert((blockCount << 12) >= length);
    return blockCount;
  }
  static uint32_t oversize(uint32_t s) {
    uint32_t newSize = s + (s >> 1);
    if (newSize > 50) {
      newSize = 64;
    }
    return newSize;
  }
  SparseBitSet::SparseBitSet(uint32_t length) {
    if (length < 1) {
      throw "length needs to be >= 1";
    }
    _blockCount = calBlockCount(length);
    _length = length;
    _indices = new uint64_t[_blockCount];
    memset(_indices,0,sizeof(uint64_t)*_blockCount);
    _bits = new Uint64Array*[_blockCount];
    memset(_bits,0,sizeof(Uint64Array*)*_blockCount);
    _nonZeroLongCount = 0;
  }
  SparseBitSet::~SparseBitSet() {
    delete[] _indices;
    for (int i=0;i<_blockCount;i++) {
      if(_bits[i])
        delete _bits[i];
    }
    delete[] _bits;
  }

  bool SparseBitSet::consistent(uint32_t index) {
    assert(index >= 0 && index < _length);
    return true;
  }
  uint32_t SparseBitSet::Cardinality() {
    uint32_t cardinality = 0;
    for(int i=0;i< _blockCount;i++){
      if(_bits[i]){
        for(int j=0;j<_bits[i]->_length;j++){
          cardinality += bitCount(_bits[i]->_data[j]);
        }
      }
    }
    return cardinality;
  }

  void SparseBitSet::insertBlock(uint32_t i4096, uint32_t i64, uint32_t i) {
    _indices[i4096] = (uint64_t) (1ULL << i64); // shifts are mod 64 in java
    assert(_bits[i4096] == NULL);
    _bits[i4096] = new Uint64Array(1);
    _bits[i4096]->Set(0,(uint64_t) (1ULL << i)); // shifts are mod 64 in java

    ++_nonZeroLongCount;
  }
  void SparseBitSet::insertLong(uint32_t i4096, uint32_t i64, uint32_t i, uint64_t index) {
    _indices[i4096] |= 1ULL << i64; // shifts are mod 64 in java
    // we count the number of bits that are set on the right of i64
    // this gives us the index at which to perform the insertion
    uint32_t o = bitCount(index & ((1ULL << i64) - 1));
    Uint64Array* bitArray = _bits[i4096];
    uint64_t* data = bitArray->_data;
      if (data[bitArray->_length - 1] == 0) {
        // since we only store non-zero longs, if the last value is 0, it means
        // that we alreay have extra space, make use of it
        memcpy(data+o+1,data+o,(bitArray->_length -o - 1)*sizeof(uint64_t));
        //System.arraycopy(bitArray, o, bitArray, o + 1, bitArray.length - o - 1);
        data[o] = (uint64_t) (1ULL << i);
      } else {
        // we don't have extra space so we need to resize to insert the new long
        uint32_t new_size = oversize(bitArray->_length + 1);
        uint64_t *new_data = new uint64_t[new_size];
        memset(new_data,0, sizeof(uint64_t)*new_size);
        memcpy(new_data, data, o*sizeof(uint64_t));
        new_data[o] = (uint64_t) (1ULL << i);
        memcpy(new_data, data + o, (bitArray->_length - o)*sizeof(uint64_t));
        bitArray->_length = new_size;
        bitArray->_data = new_data;
        delete[] data;
        ++_nonZeroLongCount;
      }
  }
  void SparseBitSet::Set(uint32_t i) {
    assert(consistent(i));
    uint32_t i4096 = i >> 12;
    uint64_t index = _indices[i4096];
    uint32_t i64 = i >> 6;
    if ((index & (1ULL << i64)) != 0) {
      // in that case the sub 64-bits block we are interested in already exists,
      // we just need to set a bit in an existing long: the number of ones on
      // the right of i64 gives us the index of the long we need to update
      _bits[i4096]->_data[bitCount(index & ((1ULL << i64) - 1))] |= 1ULL << i; // shifts are mod 64 in java
    } else if (index == 0) {
      // if the index is 0, it means that we just found a block of 4096 bits
      // that has no bit that is set yet. So let's initialize a new block:
      insertBlock(i4096, i64, i);
    } else {
      // in that case we found a block of 4096 bits that has some values, but
      // the sub-block of 64 bits that we are interested in has no value yet,
      // so we need to insert a new long
      insertLong(i4096, i64, i, index);
    }
  }
  bool SparseBitSet::Get(uint32_t i) {
    assert(consistent(i));
    uint32_t i4096 = i >> 12;
    uint64_t index = _indices[i4096];
    uint32_t i64 = i >> 6;
    // first check the index, if the i64-th bit is not set, then i is not set
    // note: this relies on the fact that shifts are mod 64 in java
    if ((index & (1ULL << i64)) == 0) {
      return false;
    }

    // if it is set, then we count the number of bits that are set on the right
    // of i64, and that gives us the index of the long that stores the bits we
    // are interested in
    uint64_t bits = _bits[i4096]->_data[bitCount(index & ((1ULL << i64) - 1))];
    return (bits & (1ULL << i)) != 0;
  }

  void SparseBitSet::Or(uint32_t i4096, uint64_t index, Uint64Array* bits, uint32_t nonZeroLongCount){
    assert(bitCount(index) == nonZeroLongCount);
    uint64_t currentIndex = _indices[i4096];
    if (currentIndex == 0) {
      // fast path: if we currently have nothing in the block, just copy the data
      // this especially happens all the time if you call OR on an empty set
      _indices[i4096] = index;
      uint32_t new_len = nonZeroLongCount;
      if(new_len>bits->_length)
        new_len = bits->_length;

      Uint64Array* new_data = new Uint64Array(new_len);
      memcpy(new_data->_data,bits->_data,new_len*sizeof(uint64_t));
      _nonZeroLongCount += nonZeroLongCount;
      return;
    }
    Uint64Array* current_bits = _bits[i4096];
    Uint64Array* new_bits;
    Uint64Array* old_bits = NULL;
    uint64_t newIndex = currentIndex | index;
    uint32_t requiredCapacity = bitCount(newIndex);

    if (current_bits->_length>= requiredCapacity) {
      new_bits = current_bits;
    } else {
      new_bits = new Uint64Array(oversize(requiredCapacity));
      old_bits = current_bits;
    }
    // we iterate backwards in order to not override data we might need on the next iteration if the
    // array is reused
    for (int i = numberOfLeadingZeros(newIndex), newO = bitCount(newIndex) - 1;
         i < 64;
         i += 1 + numberOfLeadingZeros(newIndex << (i + 1)), newO -= 1) {
      // bitIndex is the index of a bit which is set in newIndex and newO is the number of 1 bits on its right
      uint32_t bitIndex = 63 - i;
      assert(newO == bitCount(newIndex & ((1ULL << bitIndex) - 1)));
      new_bits->Set(newO,longBits(currentIndex, current_bits, bitIndex) | longBits(index, bits, bitIndex));
    }
    _indices[i4096] = newIndex;
    _bits[i4096] = new_bits;
    _nonZeroLongCount += nonZeroLongCount - bitCount(currentIndex & index);
    if(old_bits)
      delete old_bits;
  }
  /** Return the long bits at the given <code>i64</code> index. */
  uint64_t SparseBitSet::longBits(uint64_t index, Uint64Array* bits, uint32_t i64) {
    if ((index & (1ULL << i64)) == 0) {
      return 0L;
    } else {
      return bits->_data[bitCount(index & ((1ULL << i64) - 1))];
    }
  }
  void SparseBitSet::operator+=(const SparseBitSet &other) {
    for (int i = 0; i < other._blockCount; ++i) {
      uint64_t index = other._indices[i];
      if (index != 0) {
        Or(i, index, other._bits[i], bitCount(index));
      }
    }
  }
  void SparseBitSet::operator&=(const SparseBitSet &other) {
  }
  /** Return the first document that occurs on or after the provided block index. */
  uint32_t SparseBitSet::firstDoc(uint32_t i4096) {
    uint64_t index = 0;
    while (i4096 < _blockCount) {
      index = _indices[i4096];
      if (index != 0) {
        uint32_t i64 = numberOfTrailingZeros(index);
        return (i4096 << 12) | (i64 << 6) | numberOfTrailingZeros(_bits[i4096]->_data[0]);
      }
      i4096 += 1;
    }
    return NO_MORE_DOCS;
  }
  uint32_t SparseBitSet::NextSetBit(uint32_t i) {
    assert(i < _length);
    int32_t i4096 = i >> 12;
    uint64_t index = _indices[i4096];
    Uint64Array* bitArray = _bits[i4096];
    uint32_t i64 = i >> 6;
    uint32_t o = bitCount(index & ((1ULL << i64) - 1));
    if ((index & (1ULL << i64)) != 0) {
      // There is at least one bit that is set in the current uint64_t, check if
      // one of them is after i
      uint64_t bits = bitArray->_data[o] >> i; // shifts are mod 64
      if (bits != 0) {
        return i + numberOfTrailingZeros(bits);
      }
      o += 1;
    }
    uint64_t indexBits = index >> i64 >> 1;
    if (indexBits == 0) {
      // no more bits are set in the current block of 4096 bits, go to the next one
      return firstDoc(i4096 + 1);
    }
    // there are still set bits
    i64 += 1 + numberOfTrailingZeros(indexBits);
    uint64_t bits = bitArray->_data[o];
    return (i64 << 6) | numberOfTrailingZeros(bits);
  }
  uint32_t SparseBitSet::lastDoc(uint32_t i4096) {
    int32_t i = i4096;
    uint64_t index;
    while (i >= 0) {
      index = _indices[i4096];
      if (index != 0) {
        uint32_t i64 = 63 - numberOfLeadingZeros(index);
        uint64_t bits = _bits[i4096]->_data[bitCount(index) - 1];
        return (i4096 << 12) | (i64 << 6) | (63 - numberOfLeadingZeros(bits));
      }
      i -= 1;
    }
    return NO_MORE_DOCS;
  }
  uint32_t SparseBitSet::PreSetBit(uint32_t i) {
    assert(i >= 0);
    uint32_t i4096 = i >> 12;
    uint64_t index = _indices[i4096];
    Uint64Array* bitArray = _bits[i4096];
    uint32_t i64 = i >> 6;
    uint64_t indexBits = index & ((1L << i64) - 1);
    uint32_t o = bitCount(indexBits);
    if ((index & (1L << i64)) != 0) {
      // There is at least one bit that is set in the same long, check if there
      // is one bit that is set that is lower than i
      uint64_t bits = bitArray->_data[o] & ((1L << i << 1) - 1);
      if (bits != 0) {
        return (i64 << 6) | (63 - numberOfLeadingZeros(bits));
      }
    }
    if (indexBits == 0) {
      // no more bits are set in this block, go find the last bit in the
      // previous block
      return lastDoc(i4096 - 1);
    }
    // go to the previous long
    i64 = 63 - numberOfLeadingZeros(indexBits);
    uint64_t bits = bitArray->_data[o - 1];
    return (i4096 << 12) | (i64 << 6) | (63 - numberOfLeadingZeros(bits));
  }
}

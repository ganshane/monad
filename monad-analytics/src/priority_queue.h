// Copyright 2016 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
#ifndef _MONAD_PRIORITY_QUEUE_H
#define _MONAD_PRIORITY_QUEUE_H

#include <stdint.h>
#include <stddef.h>
#include <string.h>

#include "config.h"

namespace monad {

  template<typename T>
  class PriorityQueue {
  private:
    uint32_t size, max_size;
    T* *heap;
    typedef bool (*LessThanFunction)(T*,T*);
    bool LessThan(T* a, T* b);
    T* Add(T* e);
    void Clear();
    void UpHeap();
    void DownHeap();
  public:

    PriorityQueue(unsigned int maxLength,LessThanFunction fun,bool prepopulate=false) {
      this->Initialize(maxLength,prepopulate);
      this->fun = fun;
    }
    ~PriorityQueue();
    T* InsertWithOverflow(T* element);
    T* Pop();
    unsigned int Size();
    T* UpdateTop();
    /** Returns the least element of the PriorityQueue in constant time. */
    T* Top() {
      // We don't need to check size here: if maxSize is 0,
      // then heap is length 2 array with both entries null.
      // If size is 0 then heap[1] is already null.
      return heap[1];
    }

  protected:
    LessThanFunction fun;
    PriorityQueue(){}
    virtual void Initialize(unsigned int max_size,bool prepopulate);
    virtual T* getSentinelObject() {
      return new T();
    }
  };

  template<typename T>
  PriorityQueue<T>::~PriorityQueue() {
    for(uint32_t i=0;i<=size;i++){ //heap元素实际是1到size
      if(heap[i])
        delete heap[i];

    }
    delete[] heap;
  }

  /** Determines the ordering of objects in this priority queue.  Subclasses
   *  must define this one method.
   *  @return <code>true</code> iff parameter <tt>a</tt> is less than parameter <tt>b</tt>.
   */
  template<typename T>
  bool PriorityQueue<T>::LessThan(T* a, T* b) {
    return fun(a, b);
  }

  template<typename T>
  void PriorityQueue<T>::Initialize(uint32_t maxSize,bool prepopulate) {
    size = 0;
    uint32_t heapSize = 0;
    if (0 == maxSize)
      // We allocate 1 extra to avoid if statement in top()
      heapSize = 2;
    else {
      if (maxSize == UINT32_MAX) {
        // Don't wrap heapSize to -1, in this case, which
        // causes a confusing NegativeArraySizeException.
        // Note that very likely this will simply then hit
        // an OOME, but at least that's more indicative to
        // caller that this values is too big.  We don't +1
        // in this case, but it's very unlikely in practice
        // one will actually insert this many objects into
        // the PQ:
        heapSize = UINT32_MAX;
      } else {
        // NOTE: we add +1 because all access to heap is
        // 1-based not 0-based.  heap[0] is unused.
        heapSize = maxSize + 1;
      }
    }
    heap = new T*[heapSize]; // T is unbounded type, so this unchecked cast works always
    memset(heap,0,heapSize*sizeof(T*));

    if(prepopulate){
      for(int i=0;i<heapSize;i++)
        heap[i] = getSentinelObject();

      size = maxSize;
    }

    this->max_size = maxSize;
  }

  /**
   * Adds an Object to a PriorityQueue in log(size) time. If one tries to add
   * more objects than maxSize from initialize an
   * {@link ArrayIndexOutOfBoundsException} is thrown.
   * 
   * @return the new 'top' element in the queue.
   */
  template<typename T>
  T* PriorityQueue<T>::Add(T* element) {
    size++;
    heap[size] = element;
    UpHeap();
    return heap[1];
  }
  ;

  /**
   * Adds an Object to a PriorityQueue in log(size) time.
   * It returns the object (if any) that was
   * dropped off the heap because it was full. This can be
   * the given parameter (in case it is smaller than the
   * full heap's minimum, and couldn't be added), or another
   * object that was previously the smallest value in the
   * heap and now has been replaced by a larger one, or null
   * if the queue wasn't yet full with maxSize elements.
   */
  template<typename T>
  T* PriorityQueue<T>::InsertWithOverflow(T* element) {
    if (size < max_size) {
      Add(element);
      return NULL;
    } else if (size > 0 && !LessThan(element, heap[1])) {
      T* ret = heap[1];
      heap[1] = element;
      UpdateTop();
      return ret;
    } else {
      return element;
    }
  }

  /** Removes and returns the least element of the PriorityQueue in log(size)
   time. */
  template<typename T>
  T* PriorityQueue<T>::Pop() {
    if (size > 0) {
      T* result = heap[1]; // save first value
      heap[1] = heap[size]; // move last to first
      heap[size] = NULL; // permit GC of objects
      size--;
      DownHeap(); // adjust heap
      return result;
    } else
      return NULL;
  }

  /**
   * Should be called when the Object at top changes values. Still log(n) worst
   * case, but it's at least twice as fast to
   * 
   * <pre>
   * pq.top().change();
   * pq.updateTop();
   * </pre>
   * 
   * instead of
   * 
   * <pre>
   * o = pq.pop();
   * o.change();
   * pq.push(o);
   * </pre>
   * 
   * @return the new 'top' element.
   */
  template<typename T>
  T* PriorityQueue<T>::UpdateTop() {
    DownHeap();
    return heap[1];
  }

  /** Returns the number of elements currently stored in the PriorityQueue. */
  template<typename T>
  uint32_t PriorityQueue<T>::Size() {
    return this->size;
  }
  ;

  /** Removes all entries from the PriorityQueue. */
  template<typename T>
  void PriorityQueue<T>::Clear() {
    uint32_t i = 0;
    for (i = 0; i <= size; i++) {
      heap[i] = NULL;
    }
    size = 0;
  }

  template<typename T>
  void PriorityQueue<T>::UpHeap() {
    uint32_t i = size;
    T* node = heap[i]; // save bottom node
    uint32_t j = i >> 1;
    while (j > 0 && LessThan(node, heap[j])) {
      heap[i] = heap[j]; // shift parents down
      i = j;
      j = j >> 1;
    }
    heap[i] = node; // install saved node
  }

  template<typename T>
  void PriorityQueue<T>::DownHeap() {
    uint32_t i = 1;
    T* node = heap[i]; // save top node
    uint32_t j = i << 1; // find smaller child
    uint32_t k = j + 1;
    if (k <= size && LessThan(heap[k], heap[j])) {
      j = k;
    }
    while (j <= size && LessThan(heap[j], node)) {
      heap[i] = heap[j]; // shift up child
      i = j;
      j = i << 1;
      k = j + 1;
      if (k <= size && LessThan(heap[k], heap[j])) {
        j = k;
      }
    }
    heap[i] = node; // install saved node
  }
}//namespace monad
#endif


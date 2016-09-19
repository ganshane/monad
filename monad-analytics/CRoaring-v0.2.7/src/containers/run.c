#include <stdio.h>
#include <stdlib.h>

#include <roaring/containers/run.h>
#include <roaring/portability.h>
#ifdef IS_X64
#include <x86intrin.h>
#endif

extern inline int32_t interleavedBinarySearch(const rle16_t *array,
                                      int32_t lenarray, uint16_t ikey);
extern inline bool run_container_contains(const run_container_t *run,
                                           uint16_t pos);
extern bool run_container_is_full(const run_container_t *run);
extern bool run_container_nonzero_cardinality(const run_container_t *r);
extern void run_container_clear(run_container_t *run);
extern int32_t run_container_serialized_size_in_bytes(int32_t num_runs);
extern run_container_t *run_container_create_range(uint32_t start,
                                                   uint32_t stop);

bool run_container_add(run_container_t *run, uint16_t pos) {
    int32_t index = interleavedBinarySearch(run->runs, run->n_runs, pos);
    if (index >= 0) return false;  // already there
    index = -index - 2;            // points to preceding value, possibly -1
    if (index >= 0) {              // possible match
        int32_t offset = pos - run->runs[index].value;
        int32_t le = run->runs[index].length;
        if (offset <= le) return false;  // already there
        if (offset == le + 1) {
            // we may need to fuse
            if (index + 1 < run->n_runs) {
                if (run->runs[index + 1].value == pos + 1) {
                    // indeed fusion is needed
                    run->runs[index].length = run->runs[index + 1].value +
                                              run->runs[index + 1].length -
                                              run->runs[index].value;
                    recoverRoomAtIndex(run, index + 1);
                    return true;
                }
            }
            run->runs[index].length++;
            return true;
        }
        if (index + 1 < run->n_runs) {
            // we may need to fuse
            if (run->runs[index + 1].value == pos + 1) {
                // indeed fusion is needed
                run->runs[index + 1].value = pos;
                run->runs[index + 1].length = run->runs[index + 1].length + 1;
                return true;
            }
        }
    }
    if (index == -1) {
        // we may need to extend the first run
        if (0 < run->n_runs) {
            if (run->runs[0].value == pos + 1) {
                run->runs[0].length++;
                run->runs[0].value--;
                return true;
            }
        }
    }
    makeRoomAtIndex(run, index + 1);
    run->runs[index + 1].value = pos;
    run->runs[index + 1].length = 0;
    return true;
}

/* Create a new run container. Return NULL in case of failure. */
run_container_t *run_container_create_given_capacity(int32_t size) {
    run_container_t *run;
    /* Allocate the run container itself. */
    if ((run = (run_container_t *)malloc(sizeof(run_container_t))) == NULL) {
        return NULL;
    }
    if ((run->runs = (rle16_t *)malloc(sizeof(rle16_t) * size)) == NULL) {
        free(run);
        return NULL;
    }
    run->capacity = size;
    run->n_runs = 0;
    return run;
}

/* Create a new run container. Return NULL in case of failure. */
run_container_t *run_container_create(void) {
    return run_container_create_given_capacity(RUN_DEFAULT_INIT_SIZE);
}

run_container_t *run_container_clone(const run_container_t *src) {
    run_container_t *run = run_container_create_given_capacity(src->capacity);
    if (run == NULL) return NULL;
    run->capacity = src->capacity;
    run->n_runs = src->n_runs;
    memcpy(run->runs, src->runs, src->n_runs * sizeof(rle16_t));
    return run;
}

/* Free memory. */
void run_container_free(run_container_t *run) {
    free(run->runs);
    run->runs = NULL;  // pedantic
    free(run);
}

#ifdef USEAVX

/* Get the cardinality of `run'. Requires an actual computation. */
int run_container_cardinality(const run_container_t *run) {
    const int32_t n_runs = run->n_runs;
    const rle16_t *runs = run->runs;

    /* by initializing with n_runs, we omit counting the +1 for each pair. */
    int sum = n_runs;
    int32_t k = 0;
    const int32_t step = sizeof(__m256i) / sizeof(rle16_t);
    if (n_runs > step) {
        __m256i total = _mm256_setzero_si256();
        for (; k + step <= n_runs; k += step) {
            __m256i ymm1 = _mm256_lddqu_si256((const __m256i *)(runs + k));
            __m256i justlengths = _mm256_srli_epi32(ymm1, 16);
            total = _mm256_add_epi32(total, justlengths);
        }
        // a store might be faster than extract?
        uint32_t buffer[sizeof(__m256i) / sizeof(rle16_t)];
        _mm256_store_si256((__m256i *)buffer, total);
        sum += (buffer[0] + buffer[1]) + (buffer[2] + buffer[3]) +
               (buffer[4] + buffer[5]) + (buffer[6] + buffer[7]);
    }
    for (; k < n_runs; ++k) {
        sum += runs[k].length;
    }

    return sum;
}

#else

/* Get the cardinality of `run'. Requires an actual computation. */
int run_container_cardinality(const run_container_t *run) {
    const int32_t n_runs = run->n_runs;
    const rle16_t *runs = run->runs;

    /* by initializing with n_runs, we omit counting the +1 for each pair. */
    int sum = n_runs;
    for (int k = 0; k < n_runs; ++k) {
        sum += runs[k].length;
    }

    return sum;
}
#endif

void run_container_grow(run_container_t *run, int32_t min, bool copy) {
    int32_t newCapacity =
        (run->capacity == 0)
            ? RUN_DEFAULT_INIT_SIZE
            : run->capacity < 64 ? run->capacity * 2
                                 : run->capacity < 1024 ? run->capacity * 3 / 2
                                                        : run->capacity * 5 / 4;
    if (newCapacity < min) newCapacity = min;
    run->capacity = newCapacity;
    assert(run->capacity >= min);
    if (copy) {
        rle16_t *oldruns = run->runs;
        run->runs =
            (rle16_t *)realloc(oldruns, run->capacity * sizeof(rle16_t));
        if (run->runs == NULL) free(oldruns);
    } else {
        free(run->runs);
        run->runs = (rle16_t *)malloc(run->capacity * sizeof(rle16_t));
    }
    // TODO: handle the case where realloc fails
    assert(run->runs != NULL);
}

/* copy one container into another */
void run_container_copy(const run_container_t *src, run_container_t *dst) {
    const int32_t n_runs = src->n_runs;
    if (src->n_runs > dst->capacity) {
        run_container_grow(dst, n_runs, false);
    }
    dst->n_runs = n_runs;
    memcpy(dst->runs, src->runs, sizeof(rle16_t) * n_runs);
}

/* Compute the union of `src_1' and `src_2' and write the result to `dst'
 * It is assumed that `dst' is distinct from both `src_1' and `src_2'. */
void run_container_union(const run_container_t *src_1,
                         const run_container_t *src_2, run_container_t *dst) {
    // TODO: this could be a lot more efficient

    // we start out with inexpensive checks
    const bool if1 = run_container_is_full(src_1);
    const bool if2 = run_container_is_full(src_2);
    if (if1 || if2) {
        if (if1) {
            run_container_copy(src_1, dst);
            return;
        }
        if (if2) {
            run_container_copy(src_2, dst);
            return;
        }
    }
    const int32_t neededcapacity = src_1->n_runs + src_2->n_runs;
    if (dst->capacity < neededcapacity)
        run_container_grow(dst, neededcapacity, false);
    dst->n_runs = 0;
    int32_t rlepos = 0;
    int32_t xrlepos = 0;

    rle16_t previousrle;
    if (src_1->runs[rlepos].value <= src_2->runs[xrlepos].value) {
        previousrle = run_container_append_first(dst, src_1->runs[rlepos]);
        rlepos++;
    } else {
        previousrle = run_container_append_first(dst, src_2->runs[xrlepos]);
        xrlepos++;
    }

    while ((xrlepos < src_2->n_runs) && (rlepos < src_1->n_runs)) {
        rle16_t newrl;
        if (src_1->runs[rlepos].value <= src_2->runs[xrlepos].value) {
            newrl = src_1->runs[rlepos];
            rlepos++;
        } else {
            newrl = src_2->runs[xrlepos];
            xrlepos++;
        }
        run_container_append(dst, newrl, &previousrle);
    }
    while (xrlepos < src_2->n_runs) {
        run_container_append(dst, src_2->runs[xrlepos], &previousrle);
        xrlepos++;
    }
    while (rlepos < src_1->n_runs) {
        run_container_append(dst, src_1->runs[rlepos], &previousrle);
        rlepos++;
    }
}

/* Compute the union of `src_1' and `src_2' and write the result to `src_1'
 */
void run_container_union_inplace(run_container_t *src_1,
                                 const run_container_t *src_2) {
    // TODO: this could be a lot more efficient

    // we start out with inexpensive checks
    const bool if1 = run_container_is_full(src_1);
    const bool if2 = run_container_is_full(src_2);
    if (if1 || if2) {
        if (if1) {
            return;
        }
        if (if2) {
            run_container_copy(src_2, src_1);
            return;
        }
    }
    // we move the data to the end of the current array
    const int32_t maxoutput = src_1->n_runs + src_2->n_runs;
    const int32_t neededcapacity = maxoutput + src_1->n_runs;
    if (src_1->capacity < neededcapacity)
        run_container_grow(src_1, neededcapacity, true);
    memmove(src_1->runs + maxoutput, src_1->runs,
            src_1->n_runs * sizeof(rle16_t));
    rle16_t *inputsrc1 = src_1->runs + maxoutput;
    const int32_t input1nruns = src_1->n_runs;
    src_1->n_runs = 0;
    int32_t rlepos = 0;
    int32_t xrlepos = 0;

    rle16_t previousrle;
    if (inputsrc1[rlepos].value <= src_2->runs[xrlepos].value) {
        previousrle = run_container_append_first(src_1, inputsrc1[rlepos]);
        rlepos++;
    } else {
        previousrle = run_container_append_first(src_1, src_2->runs[xrlepos]);
        xrlepos++;
    }
    while ((xrlepos < src_2->n_runs) && (rlepos < input1nruns)) {
        rle16_t newrl;
        if (inputsrc1[rlepos].value <= src_2->runs[xrlepos].value) {
            newrl = inputsrc1[rlepos];
            rlepos++;
        } else {
            newrl = src_2->runs[xrlepos];
            xrlepos++;
        }
        run_container_append(src_1, newrl, &previousrle);
    }
    while (xrlepos < src_2->n_runs) {
        run_container_append(src_1, src_2->runs[xrlepos], &previousrle);
        xrlepos++;
    }
    while (rlepos < input1nruns) {
        run_container_append(src_1, inputsrc1[rlepos], &previousrle);
        rlepos++;
    }
}

/* Compute the symmetric difference of `src_1' and `src_2' and write the result
 * to `dst'
 * It is assumed that `dst' is distinct from both `src_1' and `src_2'. */
void run_container_xor(const run_container_t *src_1,
                       const run_container_t *src_2, run_container_t *dst) {
    // don't bother to convert xor with full range into negation
    // since negation is implemented similarly

    const int32_t neededcapacity = src_1->n_runs + src_2->n_runs;
    if (dst->capacity < neededcapacity)
        run_container_grow(dst, neededcapacity, false);

    int32_t pos1 = 0;
    int32_t pos2 = 0;
    dst->n_runs = 0;

    while ((pos1 < src_1->n_runs) && (pos2 < src_2->n_runs)) {
        if (src_1->runs[pos1].value <= src_2->runs[pos2].value) {
            run_container_smart_append_exclusive(dst, src_1->runs[pos1].value,
                                                 src_1->runs[pos1].length);
            pos1++;
        } else {
            run_container_smart_append_exclusive(dst, src_2->runs[pos2].value,
                                                 src_2->runs[pos2].length);
            pos2++;
        }
    }
    while (pos1 < src_1->n_runs) {
        run_container_smart_append_exclusive(dst, src_1->runs[pos1].value,
                                             src_1->runs[pos1].length);
        pos1++;
    }

    while (pos2 < src_2->n_runs) {
        run_container_smart_append_exclusive(dst, src_2->runs[pos2].value,
                                             src_2->runs[pos2].length);
        pos2++;
    }
}

/* Compute the intersection of src_1 and src_2 and write the result to
 * dst. It is assumed that dst is distinct from both src_1 and src_2. */
void run_container_intersection(const run_container_t *src_1,
                                const run_container_t *src_2,
                                run_container_t *dst) {
    const bool if1 = run_container_is_full(src_1);
    const bool if2 = run_container_is_full(src_2);
    if (if1 || if2) {
        if (if1) {
            run_container_copy(src_2, dst);
            return;
        }
        if (if2) {
            run_container_copy(src_1, dst);
            return;
        }
    }
    // TODO: this could be a lot more efficient, could use SIMD optimizations
    const int32_t neededcapacity = src_1->n_runs + src_2->n_runs;
    if (dst->capacity < neededcapacity)
        run_container_grow(dst, neededcapacity, false);
    dst->n_runs = 0;
    int32_t rlepos = 0;
    int32_t xrlepos = 0;
    int32_t start = src_1->runs[rlepos].value;
    int32_t end = start + src_1->runs[rlepos].length + 1;
    int32_t xstart = src_2->runs[xrlepos].value;
    int32_t xend = xstart + src_2->runs[xrlepos].length + 1;
    while ((rlepos < src_1->n_runs) && (xrlepos < src_2->n_runs)) {
        if (end <= xstart) {
            ++rlepos;
            if (rlepos < src_1->n_runs) {
                start = src_1->runs[rlepos].value;
                end = start + src_1->runs[rlepos].length + 1;
            }
        } else if (xend <= start) {
            ++xrlepos;
            if (xrlepos < src_2->n_runs) {
                xstart = src_2->runs[xrlepos].value;
                xend = xstart + src_2->runs[xrlepos].length + 1;
            }
        } else {  // they overlap
            const int32_t lateststart = start > xstart ? start : xstart;
            int32_t earliestend;
            if (end == xend) {  // improbable
                earliestend = end;
                rlepos++;
                xrlepos++;
                if (rlepos < src_1->n_runs) {
                    start = src_1->runs[rlepos].value;
                    end = start + src_1->runs[rlepos].length + 1;
                }
                if (xrlepos < src_2->n_runs) {
                    xstart = src_2->runs[xrlepos].value;
                    xend = xstart + src_2->runs[xrlepos].length + 1;
                }
            } else if (end < xend) {
                earliestend = end;
                rlepos++;
                if (rlepos < src_1->n_runs) {
                    start = src_1->runs[rlepos].value;
                    end = start + src_1->runs[rlepos].length + 1;
                }

            } else {  // end > xend
                earliestend = xend;
                xrlepos++;
                if (xrlepos < src_2->n_runs) {
                    xstart = src_2->runs[xrlepos].value;
                    xend = xstart + src_2->runs[xrlepos].length + 1;
                }
            }
            dst->runs[dst->n_runs].value = lateststart;
            dst->runs[dst->n_runs].length = (earliestend - lateststart - 1);
            dst->n_runs++;
        }
    }
}

/* Compute the difference of src_1 and src_2 and write the result to
 * dst. It is assumed that dst is distinct from both src_1 and src_2. */
void run_container_andnot(const run_container_t *src_1,
                          const run_container_t *src_2, run_container_t *dst) {
    // following Java implementation as of June 2016

    if (dst->capacity < src_1->n_runs + src_2->n_runs)
        run_container_grow(dst, src_1->n_runs + src_2->n_runs, false);

    dst->n_runs = 0;

    int rlepos1 = 0;
    int rlepos2 = 0;
    int32_t start = src_1->runs[rlepos1].value;
    int32_t end = start + src_1->runs[rlepos1].length + 1;
    int32_t start2 = src_2->runs[rlepos2].value;
    int32_t end2 = start2 + src_2->runs[rlepos2].length + 1;

    while ((rlepos1 < src_1->n_runs) && (rlepos2 < src_2->n_runs)) {
        if (end <= start2) {
            // output the first run
            dst->runs[dst->n_runs++] =
                (rle16_t){.value = (uint16_t)start,
                          .length = (uint16_t)(end - start - 1)};
            rlepos1++;
            if (rlepos1 < src_1->n_runs) {
                start = src_1->runs[rlepos1].value;
                end = start + src_1->runs[rlepos1].length + 1;
            }
        } else if (end2 <= start) {
            // exit the second run
            rlepos2++;
            if (rlepos2 < src_2->n_runs) {
                start2 = src_2->runs[rlepos2].value;
                end2 = start2 + src_2->runs[rlepos2].length + 1;
            }
        } else {
            if (start < start2) {
                dst->runs[dst->n_runs++] =
                    (rle16_t){.value = (uint16_t)start,
                              .length = (uint16_t)(start2 - start - 1)};
            }
            if (end2 < end) {
                start = end2;
            } else {
                rlepos1++;
                if (rlepos1 < src_1->n_runs) {
                    start = src_1->runs[rlepos1].value;
                    end = start + src_1->runs[rlepos1].length + 1;
                }
            }
        }
    }
    if (rlepos1 < src_1->n_runs) {
        dst->runs[dst->n_runs++] = (rle16_t){
            .value = (uint16_t)start, .length = (uint16_t)(end - start - 1)};
        rlepos1++;
        if (rlepos1 < src_1->n_runs) {
            memcpy(dst->runs + dst->n_runs, src_1->runs + rlepos1,
                   sizeof(rle16_t) * (src_1->n_runs - rlepos1));
            dst->n_runs += src_1->n_runs - rlepos1;
        }
    }
}

int run_container_to_uint32_array(void *vout, const run_container_t *cont,
                                  uint32_t base) {
    int outpos = 0;
    uint32_t * out = (uint32_t *) vout;
    for (int i = 0; i < cont->n_runs; ++i) {
        uint32_t run_start = base + cont->runs[i].value;
        uint16_t le = cont->runs[i].length;
        for (int j = 0; j <= le; ++j) {
          uint32_t val = run_start + j;
          memcpy(out + outpos, &val, sizeof(uint32_t)); // should be compiled as a MOV on x64
          outpos++;
        }
    }
    return outpos;
}

/*
 * Print this container using printf (useful for debugging).
 */
void run_container_printf(const run_container_t *cont) {
    for (int i = 0; i < cont->n_runs; ++i) {
        uint16_t run_start = cont->runs[i].value;
        uint16_t le = cont->runs[i].length;
        printf("[%d,%d]", run_start, run_start + le);
    }
}

/*
 * Print this container using printf as a comma-separated list of 32-bit
 * integers starting at base.
 */
void run_container_printf_as_uint32_array(const run_container_t *cont,
                                          uint32_t base) {
    if (cont->n_runs == 0) return;
    {
        uint32_t run_start = base + cont->runs[0].value;
        uint16_t le = cont->runs[0].length;
        printf("%d", run_start);
        for (uint32_t j = 1; j <= le; ++j) printf(",%d", run_start + j);
    }
    for (int32_t i = 1; i < cont->n_runs; ++i) {
        uint32_t run_start = base + cont->runs[i].value;
        uint16_t le = cont->runs[i].length;
        for (uint32_t j = 0; j <= le; ++j) printf(",%d", run_start + j);
    }
}

int32_t run_container_serialize(run_container_t *container, char *buf) {
    int32_t l, off;

    memcpy(buf, &container->n_runs, off = sizeof(container->n_runs));
    memcpy(&buf[off], &container->capacity, sizeof(container->capacity));
    off += sizeof(container->capacity);

    l = sizeof(rle16_t) * container->n_runs;
    memcpy(&buf[off], container->runs, l);
    return (off + l);
}

int32_t run_container_write(const run_container_t *container, char *buf) {
    memcpy(buf, &container->n_runs, sizeof(uint16_t));
    memcpy(buf + sizeof(uint16_t), container->runs,
               container->n_runs * sizeof(rle16_t));
    return run_container_size_in_bytes(container);
}

int32_t run_container_read(int32_t cardinality, run_container_t *container,
                           const char *buf) {
    (void)cardinality;
    memcpy(&container->n_runs, buf, sizeof(uint16_t));
    if (container->n_runs > container->capacity)
        run_container_grow(container, container->n_runs, false);
    memcpy(container->runs, buf + sizeof(uint16_t),
           container->n_runs * sizeof(rle16_t));
    return run_container_size_in_bytes(container);
}

uint32_t run_container_serialization_len(run_container_t *container) {
    return (sizeof(container->n_runs) + sizeof(container->capacity) +
            sizeof(rle16_t) * container->n_runs);
}

void *run_container_deserialize(const char *buf, size_t buf_len) {
    run_container_t *ptr;

    if (buf_len < 8 /* n_runs + capacity */)
        return (NULL);
    else
        buf_len -= 8;

    if ((ptr = (run_container_t *)malloc(sizeof(run_container_t))) != NULL) {
        size_t len;
        int32_t off;

        memcpy(&ptr->n_runs, buf, off = 4);
        memcpy(&ptr->capacity, &buf[off], 4);
        off += 4;

        len = sizeof(rle16_t) * ptr->n_runs;

        if (len != buf_len) {
            free(ptr);
            return (NULL);
        }

        if ((ptr->runs = (rle16_t *)malloc(len)) == NULL) {
            free(ptr);
            return (NULL);
        }

        memcpy(ptr->runs, &buf[off], len);

        /* Check if returned values are monotonically increasing */
        for (int32_t i = 0, j = 0; i < ptr->n_runs; i++) {
            if (ptr->runs[i].value < j) {
                free(ptr->runs);
                free(ptr);
                return (NULL);
            } else
                j = ptr->runs[i].value;
        }
    }

    return (ptr);
}

bool run_container_iterate(const run_container_t *cont, uint32_t base,
                           roaring_iterator iterator, void *ptr) {
    for (int i = 0; i < cont->n_runs; ++i) {
        uint32_t run_start = base + cont->runs[i].value;
        uint16_t le = cont->runs[i].length;

        for (int j = 0; j <= le; ++j)
            if (!iterator(run_start + j, ptr)) return false;
    }
    return true;
}

bool run_container_equals(run_container_t *container1,
                          run_container_t *container2) {
    if (container1->n_runs != container2->n_runs) {
        return false;
    }
    for (int32_t i = 0; i < container1->n_runs; ++i) {
        if ((container1->runs[i].value != container2->runs[i].value) ||
            (container1->runs[i].length != container2->runs[i].length))
            return false;
    }
    return true;
}

// TODO: write smart_append_exclusive version to match the overloaded 1 param
// Java version (or  is it even used?)

// follows the Java implementation closely
// length is the rle-value.  Ie, run [10,12) uses a length value 1.
void run_container_smart_append_exclusive(run_container_t *src,
                                          const uint16_t start,
                                          const uint16_t length) {
    int old_end;
    rle16_t *last_run = src->n_runs ? src->runs + (src->n_runs - 1) : NULL;
    rle16_t *appended_last_run = src->runs + src->n_runs;

    if (!src->n_runs ||
        (start > (old_end = last_run->value + last_run->length + 1))) {
        *appended_last_run = (rle16_t){.value = start, .length = length};
        src->n_runs++;
        return;
    }
    if (old_end == start) {
        // we merge
        last_run->length += (length + 1);
        return;
    }
    int new_end = start + length + 1;

    if (start == last_run->value) {
        // wipe out previous
        if (new_end < old_end) {
            *last_run = (rle16_t){.value = (uint16_t)new_end,
                                  .length = (uint16_t)(old_end - new_end - 1)};
            return;
        } else if (new_end > old_end) {
            *last_run = (rle16_t){.value = (uint16_t)old_end,
                                  .length = (uint16_t)(new_end - old_end - 1)};
            return;
        } else {
            src->n_runs--;
            return;
        }
    }
    last_run->length = start - last_run->value - 1;
    if (new_end < old_end) {
        *appended_last_run =
            (rle16_t){.value = (uint16_t)new_end,
                      .length = (uint16_t)(old_end - new_end - 1)};
        src->n_runs++;
    } else if (new_end > old_end) {
        *appended_last_run =
            (rle16_t){.value = (uint16_t)old_end,
                      .length = (uint16_t)(new_end - old_end - 1)};
        src->n_runs++;
    }
}

bool run_container_select(const run_container_t *container,
                          uint32_t *start_rank, uint32_t rank,
                          uint32_t *element) {
    for (int i = 0; i < container->n_runs; i++) {
        uint16_t length = container->runs[i].length;
        if (rank <= *start_rank + length) {
            uint16_t value = container->runs[i].value;
            *element = value + rank - (*start_rank);
            return true;
        } else
            *start_rank += length + 1;
    }
    return false;
}

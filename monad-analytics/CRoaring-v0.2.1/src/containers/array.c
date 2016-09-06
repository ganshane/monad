/*
 * array.c
 *
 */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <roaring/containers/array.h>

extern inline bool array_container_contains(const array_container_t *arr,
                                             uint16_t pos);
extern int array_container_cardinality(const array_container_t *array);
extern bool array_container_nonzero_cardinality(const array_container_t *array);
extern void array_container_clear(array_container_t *array);
extern int32_t array_container_serialized_size_in_bytes(int32_t card);
extern bool array_container_empty(const array_container_t *array);
extern bool array_container_full(const array_container_t *array);

/* Create a new array with capacity size. Return NULL in case of failure. */
array_container_t *array_container_create_given_capacity(int32_t size) {
    array_container_t *container;

    if ((container = (array_container_t *)malloc(sizeof(array_container_t))) ==
        NULL) {
        return NULL;
    }

    if ((container->array = (uint16_t *)malloc(sizeof(uint16_t) * size)) ==
        NULL) {
        free(container);
        return NULL;
    }

    container->capacity = size;
    container->cardinality = 0;

    return container;
}

/* Create a new array. Return NULL in case of failure. */
array_container_t *array_container_create() {
    return array_container_create_given_capacity(ARRAY_DEFAULT_INIT_SIZE);
}

/* Duplicate container */
array_container_t *array_container_clone(const array_container_t *src) {
    array_container_t *newcontainer =
        array_container_create_given_capacity(src->capacity);
    if (newcontainer == NULL) return NULL;

    newcontainer->cardinality = src->cardinality;

    memcpy(newcontainer->array, src->array,
           src->cardinality * sizeof(uint16_t));

    return newcontainer;
}

/* Free memory. */
void array_container_free(array_container_t *arr) {
    free(arr->array);
    arr->array = NULL;
    free(arr);
}

static inline int32_t grow_capacity(int32_t capacity) {
    return (capacity <= 0) ? ARRAY_DEFAULT_INIT_SIZE
                           : capacity < 64 ? capacity * 2
                                           : capacity < 1024 ? capacity * 3 / 2
                                                             : capacity * 5 / 4;
}

static inline int32_t clamp(int32_t val, int32_t min, int32_t max) {
    return ((val < min) ? min : (val > max) ? max : val);
}

/**
 * increase capacity to at least min, and to no more than max. Whether the
 * existing data needs to be copied over depends on the "preserve" parameter. If
 * preserve is false,
 * then the new content will be uninitialized, otherwise the old content is
 * copie.
 */
void array_container_grow(array_container_t *container, int32_t min,
                          int32_t max, bool preserve) {
    int32_t new_capacity = clamp(grow_capacity(container->capacity), min, max);

    // currently uses set max to INT32_MAX.  The next statement is not so useful
    // then.
    // if we are within 1/16th of the max, go to max
    if (new_capacity > max - max / 16) new_capacity = max;

    container->capacity = new_capacity;
    uint16_t *array = container->array;

    if (preserve) {
        container->array =
            (uint16_t *)realloc(array, new_capacity * sizeof(uint16_t));
        if (container->array == NULL) free(array);
    } else {
        free(array);
        container->array = (uint16_t *)malloc(new_capacity * sizeof(uint16_t));
    }

    // TODO: handle the case where realloc fails
    assert(container->array != NULL);
}

/* Copy one container into another. We assume that they are distinct. */
void array_container_copy(const array_container_t *src,
                          array_container_t *dst) {
    const int32_t cardinality = src->cardinality;
    if (cardinality > dst->capacity) {
        array_container_grow(dst, cardinality, INT32_MAX, false);
    }

    dst->cardinality = cardinality;
    memcpy(dst->array, src->array, cardinality * sizeof(uint16_t));
}

void array_container_add_from_range(array_container_t *arr, uint32_t min,
                                    uint32_t max, uint16_t step) {
    for (uint32_t value = min; value < max; value += step) {
        array_container_append(arr, value);
    }
}

/* Computes the union of array1 and array2 and write the result to arrayout.
 * It is assumed that arrayout is distinct from both array1 and array2.
 */
void array_container_union(const array_container_t *array_1,
                           const array_container_t *array_2,
                           array_container_t *out) {
    const int32_t card_1 = array_1->cardinality, card_2 = array_2->cardinality;
    const int32_t max_cardinality = card_1 + card_2;

    if (out->capacity < max_cardinality)
        array_container_grow(out, max_cardinality, INT32_MAX, false);
#ifdef ROARING_VECTOR_UNION_ENABLED
    // compute union with smallest array first
    if (card_1 < card_2) {
        out->cardinality = union_vector16(array_1->array, card_1,
                                          array_2->array, card_2, out->array);
    } else {
        out->cardinality = union_vector16(array_2->array, card_2,
                                          array_1->array, card_1, out->array);
    }
#else
    // compute union with smallest array first
    if (card_1 < card_2) {
        out->cardinality = union_uint16(array_1->array, card_1, array_2->array,
                                        card_2, out->array);
    } else {
        out->cardinality = union_uint16(array_2->array, card_2, array_1->array,
                                        card_1, out->array);
    }
#endif
}

/* helper. a_out must be a valid array container with adequate capacity.
 * and may be same as a1.
 * Returns the cardinality of the output container. Based on Java
 * implementation Util.unsignedDifference
 */

static int array_array_array_subtract(const array_container_t *a1,
                                      const array_container_t *a2,
                                      array_container_t *a_out) {
    int out_card = 0;
    int k1 = 0, k2 = 0;
    int length1 = a1->cardinality, length2 = a2->cardinality;

    if (length1 == 0) return 0;

    if (length2 == 0) {
        if (a1 != a_out)
            memcpy(a_out->array, a1->array, sizeof(uint16_t) * length1);
        return length1;
    }

    uint16_t s1 = a1->array[k1];
    uint16_t s2 = a2->array[k2];

    while (true) {
        if (s1 < s2) {
            a_out->array[out_card++] = s1;
            ++k1;
            if (k1 >= length1) {
                break;
            }
            s1 = a1->array[k1];
        } else if (s1 == s2) {
            ++k1;
            ++k2;
            if (k1 >= length1) {
                break;
            }
            if (k2 >= length2) {
                memmove(a_out->array + out_card, a1->array + k1,
                        sizeof(uint16_t) * (length1 - k1));
                return out_card + length1 - k1;
            }
            s1 = a1->array[k1];
            s2 = a2->array[k2];
        } else {  // if (val1>val2)
            ++k2;
            if (k2 >= length2) {
                memmove(a_out->array + out_card, a1->array + k1,
                        sizeof(uint16_t) * (length1 - k1));
                return out_card + length1 - k1;
            }
            s2 = a2->array[k2];
        }
    }
    return out_card;
}

/* Computes the  difference of array1 and array2 and write the result
 * to array out.
 * Array out does not need to be distinct from array_1
 */
void array_container_andnot(const array_container_t *array_1,
                            const array_container_t *array_2,
                            array_container_t *out) {
    if (out->capacity < array_1->cardinality)
        array_container_grow(out, array_1->cardinality, INT32_MAX, false);
    out->cardinality = array_array_array_subtract(array_1, array_2, out);
}

/* Computes the symmetric difference of array1 and array2 and write the
 * result
 * to arrayout.
 * It is assumed that arrayout is distinct from both array1 and array2.
 */
void array_container_xor(const array_container_t *array_1,
                         const array_container_t *array_2,
                         array_container_t *out) {
    const int32_t card_1 = array_1->cardinality, card_2 = array_2->cardinality;
    const int32_t max_cardinality = card_1 + card_2;

    if (out->capacity < max_cardinality)
        array_container_grow(out, max_cardinality, INT32_MAX, false);

    // TODO something clever like the AVX union in array_util.c
    // except where *both* occurrences of a duplicate in a sorted sequence
    // are removed.

    // just a merge for now (see TODO)
    int pos1 = 0, pos2 = 0, pos_out = 0;
    while (pos1 < card_1 && pos2 < card_2) {
        const uint16_t v1 = array_1->array[pos1];
        const uint16_t v2 = array_2->array[pos2];
        if (v1 == v2) {
            ++pos1;
            ++pos2;
            continue;
        }
        if (v1 < v2) {
            out->array[pos_out++] = v1;
            ++pos1;
        } else {
            out->array[pos_out++] = v2;
            ++pos2;
        }
    }
    // todo: memcpys instead
    while (pos1 < card_1) out->array[pos_out++] = array_1->array[pos1++];
    while (pos2 < card_2) out->array[pos_out++] = array_2->array[pos2++];

    out->cardinality = pos_out;
}

static inline int32_t minimum_int32(int32_t a, int32_t b) {
    return (a < b) ? a : b;
}

/* computes the intersection of array1 and array2 and write the result to
 * arrayout.
 * It is assumed that arrayout is distinct from both array1 and array2.
 * */
void array_container_intersection(const array_container_t *array1,
                                  const array_container_t *array2,
                                  array_container_t *out) {
    int32_t card_1 = array1->cardinality, card_2 = array2->cardinality,
            min_card = minimum_int32(card_1, card_2);
    const int threshold = 64;  // subject to tuning
#ifdef USEAVX
    min_card += sizeof(__m128i) / sizeof(uint16_t);
#endif
    if (out->capacity < min_card)
        array_container_grow(out, min_card, INT32_MAX, false);
    if (card_1 * threshold < card_2) {
        out->cardinality = intersect_skewed_uint16(
            array1->array, card_1, array2->array, card_2, out->array);
    } else if (card_2 * threshold < card_1) {
        out->cardinality = intersect_skewed_uint16(
            array2->array, card_2, array1->array, card_1, out->array);
    } else {
#ifdef USEAVX
        out->cardinality = intersect_vector16(
            array1->array, card_1, array2->array, card_2, out->array);
#else
        out->cardinality = intersect_uint16(array1->array, card_1,
                                            array2->array, card_2, out->array);
#endif
    }
}

/* computes the intersection of array1 and array2 and write the result to
 * array1.
 * */
void array_container_intersection_inplace(array_container_t *src_1,
                                          const array_container_t *src_2) {
    // todo: can any of this be vectorized?
    int32_t card_1 = src_1->cardinality, card_2 = src_2->cardinality;
    const int threshold = 64;  // subject to tuning
    if (card_1 * threshold < card_2) {
        src_1->cardinality = intersect_skewed_uint16(
            src_1->array, card_1, src_2->array, card_2, src_1->array);
    } else if (card_2 * threshold < card_1) {
        src_1->cardinality = intersect_skewed_uint16(
            src_2->array, card_2, src_1->array, card_1, src_1->array);
    } else {
        src_1->cardinality = intersect_uint16(
            src_1->array, card_1, src_2->array, card_2, src_1->array);
    }
}

int array_container_to_uint32_array(void *vout,
                                    const array_container_t *cont,
                                    uint32_t base) {
    int outpos = 0;
    uint32_t * out = (uint32_t *) vout;
    for (int i = 0; i < cont->cardinality; ++i) {
        const uint32_t val = base + cont->array[i];
        memcpy(out + outpos, &val, sizeof(uint32_t)); // should be compiled as a MOV on x64
        outpos ++;
    }
    return outpos;
}

void array_container_printf(const array_container_t *v) {
    if (v->cardinality == 0) {
        printf("{}");
        return;
    }
    printf("{");
    printf("%d", v->array[0]);
    for (int i = 1; i < v->cardinality; ++i) {
        printf(",%d", v->array[i]);
    }
    printf("}");
}

void array_container_printf_as_uint32_array(const array_container_t *v,
                                            uint32_t base) {
    if (v->cardinality == 0) {
        return;
    }
    printf("%d", v->array[0] + base);
    for (int i = 1; i < v->cardinality; ++i) {
        printf(",%d", v->array[i] + base);
    }
}

/* Compute the number of runs */
int32_t array_container_number_of_runs(const array_container_t *a) {
    // Can SIMD work here?
    int32_t nr_runs = 0;
    int32_t prev = -2;
    for (const uint16_t *p = a->array; p != a->array + a->cardinality; ++p) {
        if (*p != prev + 1) nr_runs++;
        prev = *p;
    }
    return nr_runs;
}

int32_t array_container_serialize(array_container_t *container, char *buf) {
    int32_t l, off;
    uint16_t cardinality = (uint16_t)container->cardinality;

    memcpy(buf, &cardinality, off = sizeof(cardinality));
    l = sizeof(uint16_t) * container->cardinality;
    if (l) memcpy(&buf[off], container->array, l);

    return (off + l);
}

/**
 * Writes the underlying array to buf, outputs how many bytes were written.
 * The number of bytes written should be
 * array_container_size_in_bytes(container).
 *
 */
int32_t array_container_write(const array_container_t *container, char *buf) {
    if (IS_BIG_ENDIAN) {
        // forcing little endian (could be faster)
        for (int32_t i = 0; i < container->cardinality; i++) {
            uint16_t val = container->array[i];
            buf[2 * i] = (uint8_t)val;
            buf[2 * i + 1] = (uint8_t)(val >> 8);
        }
    } else {
        memcpy(buf, container->array,
               container->cardinality * sizeof(uint16_t));
    }
    return array_container_size_in_bytes(container);
}

bool array_container_equals(array_container_t *container1,
                            array_container_t *container2) {
    if (container1->cardinality != container2->cardinality) {
        return false;
    }
    // could be vectorized:
    for (int32_t i = 0; i < container1->cardinality; ++i) {
        if (container1->array[i] != container2->array[i]) return false;
    }
    return true;
}

int32_t array_container_read(int32_t cardinality, array_container_t *container,
                             const char *buf) {
    if (container->capacity < cardinality) {
        array_container_grow(container, cardinality, DEFAULT_MAX_SIZE, false);
    }
    container->cardinality = cardinality;
    assert(!IS_BIG_ENDIAN);  // TODO: Implement

    memcpy(container->array, buf, container->cardinality * sizeof(uint16_t));

    return array_container_size_in_bytes(container);
}

uint32_t array_container_serialization_len(array_container_t *container) {
    return (sizeof(uint16_t) /* container->cardinality converted to 16 bit */ +
            (sizeof(uint16_t) * container->cardinality));
}

void *array_container_deserialize(const char *buf, size_t buf_len) {
    array_container_t *ptr;

    if (buf_len < 2) /* capacity converted to 16 bit */
        return (NULL);
    else
        buf_len -= 2;

    if ((ptr = (array_container_t *)malloc(sizeof(array_container_t))) !=
        NULL) {
        size_t len;
        int32_t off;
        uint16_t cardinality;

        memcpy(&cardinality, buf, off = sizeof(cardinality));

        ptr->capacity = ptr->cardinality = (uint32_t)cardinality;
        len = sizeof(uint16_t) * ptr->cardinality;

        if (len != buf_len) {
            free(ptr);
            return (NULL);
        }

        if ((ptr->array = (uint16_t *)malloc(sizeof(uint16_t) *
                                             ptr->capacity)) == NULL) {
            free(ptr);
            return (NULL);
        }

        if (len) memcpy(ptr->array, &buf[off], len);

        /* Check if returned values are monotonically increasing */
        for (int32_t i = 0, j = 0; i < ptr->cardinality; i++) {
            if (ptr->array[i] < j) {
                free(ptr->array);
                free(ptr);
                return (NULL);
            } else
                j = ptr->array[i];
        }
    }

    return (ptr);
}

bool array_container_iterate(const array_container_t *cont, uint32_t base,
                             roaring_iterator iterator, void *ptr) {
    for (int i = 0; i < cont->cardinality; i++)
        if (!iterator(cont->array[i] + base, ptr)) return false;
    return true;
}

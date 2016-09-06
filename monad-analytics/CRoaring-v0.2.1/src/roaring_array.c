#include <assert.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <roaring/containers/bitset.h>
#include <roaring/containers/containers.h>
#include <roaring/roaring_array.h>

// ported from RoaringArray.java
// Todo: optimization (eg branchless binary search)
// Go version has copy-on-write, has combo binary/sequential search
// AND: fast SIMD and on key sets; containerwise AND; SIMD partial sum
//    with +1 for nonempty containers, 0 for empty containers
//    then use this to pack the arrays for the result.

// Convention: [0,ra->size) all elements are initialized
//  [ra->size, ra->allocation_size) is junk and contains nothing needing freeing

extern inline int32_t ra_get_size(roaring_array_t *ra);
extern inline int32_t ra_get_index(roaring_array_t *ra, uint16_t x);
extern inline void *ra_get_container_at_index(roaring_array_t *ra,
      uint16_t i, uint8_t *typecode);
extern inline void ra_unshare_container_at_index(roaring_array_t *ra, uint16_t i);
#define INITIAL_CAPACITY 4

roaring_array_t *ra_create_with_capacity(uint32_t cap) {
    roaring_array_t *new_ra =
        (roaring_array_t *)malloc(sizeof(roaring_array_t));
    if (!new_ra) return NULL;
    new_ra->keys = NULL;
    new_ra->containers = NULL;
    new_ra->typecodes = NULL;

    new_ra->allocation_size = cap;
    new_ra->keys = (uint16_t *)malloc(cap * sizeof(uint16_t));
    new_ra->containers = (void **)malloc(cap * sizeof(void *));
    new_ra->typecodes = (uint8_t *)malloc(cap * sizeof(uint8_t));
    if (!new_ra->keys || !new_ra->containers || !new_ra->typecodes) {
        free(new_ra);
        free(new_ra->keys);
        free(new_ra->containers);
        free(new_ra->typecodes);
        return NULL;
    }
    new_ra->size = 0;

    return new_ra;
}

roaring_array_t *ra_create() {
    return ra_create_with_capacity(INITIAL_CAPACITY);
}

roaring_array_t *ra_copy(roaring_array_t *r, bool copy_on_write) {
    roaring_array_t *new_ra =
        (roaring_array_t *)malloc(sizeof(roaring_array_t));
    if (!new_ra) return NULL;
    new_ra->keys = NULL;
    new_ra->containers = NULL;
    new_ra->typecodes = NULL;

    const int32_t allocsize = r->allocation_size;
    new_ra->allocation_size = allocsize;
    new_ra->keys = (uint16_t *)malloc(allocsize * sizeof(uint16_t));
    new_ra->containers =
        (void **)calloc(allocsize, sizeof(void *));  // setting pointers to zero
    new_ra->typecodes = (uint8_t *)malloc(allocsize * sizeof(uint8_t));
    if (!new_ra->keys || !new_ra->containers || !new_ra->typecodes) {
        free(new_ra);
        free(new_ra->keys);
        free(new_ra->containers);
        free(new_ra->typecodes);
        return NULL;
    }
    int32_t s = r->size;
    new_ra->size = s;
    memcpy(new_ra->keys, r->keys, s * sizeof(uint16_t));
    // we go through the containers, turning them into shared containers...
    if (copy_on_write) {
        for (int32_t i = 0; i < s; ++i) {
            r->containers[i] = get_copy_of_container(
                r->containers[i], &r->typecodes[i], copy_on_write);
        }
        // we do a shallow copy to the other bitmap
        memcpy(new_ra->containers, r->containers, s * sizeof(void *));
        memcpy(new_ra->typecodes, r->typecodes, s * sizeof(uint8_t));
    } else {
        memcpy(new_ra->typecodes, r->typecodes, s * sizeof(uint8_t));
        for (int32_t i = 0; i < s; i++) {
            new_ra->containers[i] =
                container_clone(r->containers[i], r->typecodes[i]);
            if (new_ra->containers[i] == NULL) {
                for (int32_t j = 0; j < i; j++) {
                    container_free(r->containers[j], r->typecodes[j]);
                }
                free(new_ra);
                free(new_ra->keys);
                free(new_ra->containers);
                free(new_ra->typecodes);
                return NULL;
            }
        }
    }
    return new_ra;
}

static void ra_clear(roaring_array_t *ra) {
    free(ra->keys);
    ra->keys = NULL;  // paranoid
    for (int i = 0; i < ra->size; ++i) {
        container_free(ra->containers[i], ra->typecodes[i]);
    }
    free(ra->containers);
    ra->containers = NULL;  // paranoid
    free(ra->typecodes);
    ra->typecodes = NULL;  // paranoid
}

static void ra_clear_without_containers(roaring_array_t *ra) {
    free(ra->keys);
    ra->keys = NULL;  // paranoid
    free(ra->containers);
    ra->containers = NULL;  // paranoid
    free(ra->typecodes);
    ra->typecodes = NULL;  // paranoid
}

void ra_free(roaring_array_t *ra) {
    ra_clear(ra);
    free(ra);
}

void ra_free_without_containers(roaring_array_t *ra) {
    ra_clear_without_containers(ra);
    free(ra);
}

void extend_array(roaring_array_t *ra, uint32_t k) {
    // corresponding Java code uses >= ??
    int desired_size = ra->size + (int)k;
    if (desired_size > ra->allocation_size) {
        int new_capacity =
            (ra->size < 1024) ? 2 * desired_size : 5 * desired_size / 4;

        ra->keys =
            (uint16_t *)realloc(ra->keys, sizeof(uint16_t) * new_capacity);
        ra->containers =
            (void **)realloc(ra->containers, sizeof(void *) * new_capacity);
        ra->typecodes =
            (uint8_t *)realloc(ra->typecodes, sizeof(uint8_t) * new_capacity);
        if (!ra->keys || !ra->containers || !ra->typecodes) {
            fprintf(stderr, "[%s] %s\n", __FILE__, __func__);
            perror(0);
        }
        ra->allocation_size = new_capacity;
    }
}

void ra_append(roaring_array_t *ra, uint16_t key, void *container,
               uint8_t typecode) {
    extend_array(ra, 1);
    const int32_t pos = ra->size;

    ra->keys[pos] = key;
    ra->containers[pos] = container;
    ra->typecodes[pos] = typecode;
    ra->size++;
}

void ra_append_copy(roaring_array_t *ra, roaring_array_t *sa, uint16_t index,
                    bool copy_on_write) {
    extend_array(ra, 1);
    const int32_t pos = ra->size;

    // old contents is junk not needing freeing
    ra->keys[pos] = sa->keys[index];
    // the shared container will be in two bitmaps
    if (copy_on_write) {
        sa->containers[index] = get_copy_of_container(
            sa->containers[index], &sa->typecodes[index], copy_on_write);
        ra->containers[pos] = sa->containers[index];
        ra->typecodes[pos] = sa->typecodes[index];
    } else {
        ra->containers[pos] =
            container_clone(sa->containers[index], sa->typecodes[index]);
        ra->typecodes[pos] = sa->typecodes[index];
    }
    ra->size++;
}

void ra_append_copies_until(roaring_array_t *ra, roaring_array_t *sa,
                            uint16_t stopping_key, bool copy_on_write) {
    for (uint16_t i = 0; i < sa->size; ++i) {
        if (sa->keys[i] >= stopping_key) break;
        ra_append_copy(ra, sa, i, copy_on_write);
    }
}

void ra_append_copy_range(roaring_array_t *ra, roaring_array_t *sa,
                          uint16_t start_index, uint16_t end_index,
                          bool copy_on_write) {
    extend_array(ra, end_index - start_index);

    for (uint16_t i = start_index; i < end_index; ++i) {
        const int32_t pos = ra->size;
        ra->keys[pos] = sa->keys[i];
        if (copy_on_write) {
            sa->containers[i] = get_copy_of_container(
                sa->containers[i], &sa->typecodes[i], copy_on_write);
            ra->containers[pos] = sa->containers[i];
            ra->typecodes[pos] = sa->typecodes[i];
        } else {
            ra->containers[pos] =
                container_clone(sa->containers[i], sa->typecodes[i]);
            ra->typecodes[pos] = sa->typecodes[i];
        }
        ra->size++;
    }
}

void ra_append_copies_after(roaring_array_t *ra, roaring_array_t *sa,
                            uint16_t before_start, bool copy_on_write) {
    int start_location = ra_get_index(sa, before_start);
    if (start_location >= 0)
        ++start_location;
    else
        start_location = -start_location - 1;
    ra_append_copy_range(ra, sa, start_location, sa->size, copy_on_write);
}

void ra_append_move_range(roaring_array_t *ra, roaring_array_t *sa,
                          uint16_t start_index, uint16_t end_index) {
    extend_array(ra, end_index - start_index);

    for (uint16_t i = start_index; i < end_index; ++i) {
        const int32_t pos = ra->size;

        ra->keys[pos] = sa->keys[i];
        ra->containers[pos] = sa->containers[i];
        ra->typecodes[pos] = sa->typecodes[i];
        ra->size++;
    }
}

void ra_append_range(roaring_array_t *ra, roaring_array_t *sa,
                     uint16_t start_index, uint16_t end_index,
                     bool copy_on_write) {
    extend_array(ra, end_index - start_index);

    for (uint16_t i = start_index; i < end_index; ++i) {
        const int32_t pos = ra->size;
        ra->keys[pos] = sa->keys[i];
        if (copy_on_write) {
            sa->containers[i] = get_copy_of_container(
                sa->containers[i], &sa->typecodes[i], copy_on_write);
            ra->containers[pos] = sa->containers[i];
            ra->typecodes[pos] = sa->typecodes[i];
        } else {
            ra->containers[pos] =
                container_clone(sa->containers[i], sa->typecodes[i]);
            ra->typecodes[pos] = sa->typecodes[i];
        }
        ra->size++;
    }
}

void *ra_get_container(roaring_array_t *ra, uint16_t x, uint8_t *typecode) {
    int i = binarySearch(ra->keys, (int32_t)ra->size, x);
    if (i < 0) return NULL;
    *typecode = ra->typecodes[i];
    return ra->containers[i];
}

extern void *ra_get_container_at_index(roaring_array_t *ra, uint16_t i,
                                       uint8_t *typecode);

void *ra_get_writable_container(roaring_array_t *ra, uint16_t x,
                                uint8_t *typecode) {
    int i = binarySearch(ra->keys, (int32_t)ra->size, x);
    if (i < 0) return NULL;
    *typecode = ra->typecodes[i];
    return get_writable_copy_if_shared(ra->containers[i], typecode);
}

void *ra_get_writable_container_at_index(roaring_array_t *ra, uint16_t i,
                                         uint8_t *typecode) {
    assert(i < ra->size);
    *typecode = ra->typecodes[i];
    return get_writable_copy_if_shared(ra->containers[i], typecode);
}

uint16_t ra_get_key_at_index(roaring_array_t *ra, uint16_t i) {
    return ra->keys[i];
}

extern int32_t ra_get_index(roaring_array_t *ra, uint16_t x);

extern int32_t ra_advance_until(roaring_array_t *ra, uint16_t x, int32_t pos);

// everything skipped over is freed
int32_t ra_advance_until_freeing(roaring_array_t *ra, uint16_t x, int32_t pos) {
    while (pos < ra->size && ra->keys[pos] < x) {
        container_free(ra->containers[pos], ra->typecodes[pos]);
        ++pos;
    }
    return pos;
}

void ra_insert_new_key_value_at(roaring_array_t *ra, int32_t i, uint16_t key,
                                void *container, uint8_t typecode) {
    extend_array(ra, 1);
    // May be an optimization opportunity with DIY memmove
    memmove(&(ra->keys[i + 1]), &(ra->keys[i]),
            sizeof(uint16_t) * (ra->size - i));
    memmove(&(ra->containers[i + 1]), &(ra->containers[i]),
            sizeof(void *) * (ra->size - i));
    memmove(&(ra->typecodes[i + 1]), &(ra->typecodes[i]),
            sizeof(uint8_t) * (ra->size - i));
    ra->keys[i] = key;
    ra->containers[i] = container;
    ra->typecodes[i] = typecode;
    ra->size++;
}

// note: Java routine set things to 0, enabling GC.
// Java called it "resize" but it was always used to downsize.
// Allowing upsize would break the conventions about
// valid containers below ra->size.

void ra_downsize(roaring_array_t *ra, int32_t new_length) {
    assert(new_length <= ra->size);
    ra->size = new_length;
}

void ra_remove_at_index(roaring_array_t *ra, int32_t i) {
    memmove(&(ra->containers[i]), &(ra->containers[i + 1]),
            sizeof(void *) * (ra->size - i - 1));
    memmove(&(ra->keys[i]), &(ra->keys[i + 1]),
            sizeof(uint16_t) * (ra->size - i - 1));
    memmove(&(ra->typecodes[i]), &(ra->typecodes[i + 1]),
            sizeof(uint8_t) * (ra->size - i - 1));
    ra->size--;
}

void ra_remove_at_index_and_free(roaring_array_t *ra, int32_t i) {
    container_free(ra->containers[i], ra->typecodes[i]);
    ra_remove_at_index(ra, i);
}

// used in inplace andNot only, to slide left the containers from
// the mutated RoaringBitmap that are after the largest container of
// the argument RoaringBitmap.  In use it should be followed by a call to
// downsize.
//
void ra_copy_range(roaring_array_t *ra, uint32_t begin, uint32_t end,
                   uint32_t new_begin) {
    assert(begin <= end);
    assert(new_begin < begin);

    const int range = end - begin;

    // We ensure to previously have freed overwritten containers
    // that are not copied elsewhere

    memmove(&(ra->containers[new_begin]), &(ra->containers[begin]),
            sizeof(void *) * range);
    memmove(&(ra->keys[new_begin]), &(ra->keys[begin]),
            sizeof(uint16_t) * range);
    memmove(&(ra->typecodes[new_begin]), &(ra->typecodes[begin]),
            sizeof(uint8_t) * range);
}

void ra_set_container_at_index(roaring_array_t *ra, int32_t i, void *c,
                               uint8_t typecode) {
    assert(i < ra->size);
    ra->containers[i] = c;
    ra->typecodes[i] = typecode;
}

void ra_replace_key_and_container_at_index(roaring_array_t *ra, int32_t i,
                                           uint16_t key, void *c,
                                           uint8_t typecode) {
    assert(i < ra->size);

    ra->keys[i] = key;
    ra->containers[i] = c;
    ra->typecodes[i] = typecode;
}

// just for debugging use
void show_structure(roaring_array_t *ra) {
    for (int i = 0; i < ra->size; ++i) {
        printf(" i=%d\n", i);
        fflush(stdout);

        printf("Container %d has key %d and its type is %s  of card %d\n", i,
               (int)ra->keys[i],
               get_full_container_name(ra->containers[i], ra->typecodes[i]),
               container_get_cardinality(ra->containers[i], ra->typecodes[i]));
    }
}


size_t ra_size_in_bytes(roaring_array_t *ra) {
    size_t cardinality = 0;
    size_t
        tot_len =
            1 /* initial byte type */ + 4 /* tot_len */ +
            sizeof(roaring_array_t) +
            ra->size * (sizeof(uint16_t) + sizeof(void *) + sizeof(uint8_t));
    for (int32_t i = 0; i < ra->size; i++) {
        tot_len += (container_serialization_len(ra->containers[i], ra->typecodes[i]) + sizeof(uint16_t));
        cardinality +=
            container_get_cardinality(ra->containers[i], ra->typecodes[i]);
    }

    if ((cardinality * sizeof(uint32_t) + sizeof(uint32_t)) < tot_len) {
        return cardinality * sizeof(uint32_t) + 1 + sizeof(uint32_t);
    }
    return tot_len;
}



void ra_to_uint32_array(roaring_array_t *ra, uint32_t *ans) {
    size_t ctr = 0;
    for (int i = 0; i < ra->size; ++i) {
        int num_added = container_to_uint32_array(
            ans + ctr, ra->containers[i],
            ra->typecodes[i],
            ((uint32_t)ra->keys[i]) << 16);
        ctr += num_added;
    }
}

size_t ra_serialize(roaring_array_t *ra, char *buf) {
    size_t off, l,
        cardinality = 0,
        tot_len =
            1 /* initial byte type */ + 4 /* tot_len */ +
            sizeof(roaring_array_t) +
            ra->size * (sizeof(uint16_t) + sizeof(void *) + sizeof(uint8_t));
    uint16_t *lens;
    /* [ 32 bit length ] [ serialization bytes ] */
    if ((lens = (uint16_t *)malloc(sizeof(int16_t) * ra->size)) == NULL) {
        fprintf(stderr, "Failed to allocate memory early on. Bailing out.\n");
        return 0;
    }

    for (int32_t i = 0; i < ra->size; i++) {
        lens[i] =
            container_serialization_len(ra->containers[i], ra->typecodes[i]);

        assert(lens[i] != 0);
        tot_len += (lens[i] + sizeof(lens[i]));
        cardinality +=
            container_get_cardinality(ra->containers[i], ra->typecodes[i]);
    }

    if ((cardinality * sizeof(uint32_t) + sizeof(uint32_t)) < tot_len) {
        free(lens);
        buf[0] = SERIALIZATION_ARRAY_UINT32;
        memcpy(buf + 1, &cardinality, sizeof(uint32_t));
        ra_to_uint32_array(ra, (uint32_t *)(buf + 1 + sizeof(uint32_t)));
        return 1 + sizeof(uint32_t) + cardinality * sizeof(uint32_t);
    }

    /* Leave room for the first byte */
    buf[0] = SERIALIZATION_CONTAINER, off = 1;

    /* Total lenght (first 4 bytes of the serialization) */
    memcpy(buf + off, &tot_len, 4), off += 4;

    l = sizeof(roaring_array_t);
    uint32_t saved_allocation_size = ra->allocation_size;

    ra->allocation_size = ra->size;
    memcpy(&buf[off], ra, l);
    ra->allocation_size = saved_allocation_size;
    off += l;

    l = ra->size * sizeof(uint16_t);
    memcpy(&buf[off], ra->keys, l);
    off += l;

    l = ra->size * sizeof(void *);
    memcpy(&buf[off], ra->containers, l);
    off += l;

    l = ra->size * sizeof(uint8_t);
    memcpy(&buf[off], ra->typecodes, l);
    off += l;

    for (int32_t i = 0; i < ra->size; i++) {
        int32_t serialized_bytes;

        memcpy(&buf[off], &lens[i], sizeof(lens[i]));
        off += sizeof(lens[i]);
        serialized_bytes =
            container_serialize(ra->containers[i], ra->typecodes[i], &buf[off]);
        off += serialized_bytes;
    }

    if (tot_len != off) {
        assert(tot_len != off);
    }

    free(lens);

    return tot_len;
}

// assumes that it is not an array (see roaring_bitmap_deserialize)
roaring_array_t *ra_deserialize(const void *buf) {
    int32_t size;
    const char *bufaschar = (const char *)buf;
    memcpy(&size, bufaschar, sizeof(int32_t));
    roaring_array_t *ra_copy;
    uint32_t off, l;


    if ((ra_copy = (roaring_array_t *)malloc(sizeof(roaring_array_t))) == NULL)
        return (NULL);

    memcpy(ra_copy, bufaschar, off = sizeof(roaring_array_t));

    if ((ra_copy->keys = (uint16_t *)malloc(size * sizeof(uint16_t))) == NULL) {
        free(ra_copy);
        return (NULL);
    }

    if ((ra_copy->containers = (void **)malloc(size * sizeof(void *))) ==
        NULL) {
        free(ra_copy->keys);
        free(ra_copy);
        return (NULL);
    }

    if ((ra_copy->typecodes = (uint8_t *)malloc(size * sizeof(uint8_t))) ==
        NULL) {
        free(ra_copy->containers);
        free(ra_copy->keys);
        free(ra_copy);
        return (NULL);
    }

    l = size * sizeof(uint16_t);
    memcpy(ra_copy->keys, &bufaschar[off], l);
    off += l;

    l = size * sizeof(void *);
    memcpy(ra_copy->containers, &bufaschar[off], l);
    off += l;

    l = size * sizeof(uint8_t);
    memcpy(ra_copy->typecodes, &bufaschar[off], l);
    off += l;

    for (int32_t i = 0; i < size; i++) {
        uint16_t len;

        memcpy(&len, &bufaschar[off], sizeof(len));
        off += sizeof(len);

        ra_copy->containers[i] =
            container_deserialize(ra_copy->typecodes[i], &bufaschar[off], len);

        if (ra_copy->containers[i] == NULL) {
            for (int32_t j = 0; j < i; j++)
                container_free(ra_copy->containers[j], ra_copy->typecodes[j]);

            free(ra_copy->containers);
            free(ra_copy->keys);
            free(ra_copy);
            return (NULL);
        }

        off += len;
    }

    return (ra_copy);
}

bool ra_has_run_container(roaring_array_t *ra) {
    for (int32_t k = 0; k < ra->size; ++k) {
        if (get_container_type(ra->containers[k], ra->typecodes[k]) ==
            RUN_CONTAINER_TYPE_CODE)
            return true;
    }
    return false;
}

uint32_t ra_portable_header_size(roaring_array_t *ra) {
    if (ra_has_run_container(ra)) {
        if (ra->size <
            NO_OFFSET_THRESHOLD) {  // for small bitmaps, we omit the offsets
            return 4 + (ra->size + 7) / 8 + 4 * ra->size;
        }
        return 4 + (ra->size + 7) / 8 +
               8 * ra->size;  // - 4 because we pack the size with the cookie
    } else {
        return 4 + 4 + 8 * ra->size;
    }
}

size_t ra_portable_size_in_bytes(roaring_array_t *ra) {
    size_t count = ra_portable_header_size(ra);

    for (int32_t k = 0; k < ra->size; ++k) {
        count += container_size_in_bytes(ra->containers[k], ra->typecodes[k]);
    }
    return count;
}

size_t ra_portable_serialize(roaring_array_t *ra, char *buf) {
    assert(!IS_BIG_ENDIAN);  // not implemented
    char *initbuf = buf;
    uint32_t startOffset = 0;
    bool hasrun = ra_has_run_container(ra);
    if (hasrun) {
        uint32_t cookie = SERIAL_COOKIE | ((ra->size - 1) << 16);
        memcpy(buf, &cookie, sizeof(cookie));
        buf += sizeof(cookie);
        uint32_t s = (ra->size + 7) / 8;
        uint8_t *bitmapOfRunContainers = (uint8_t *)calloc(s, 1);
        assert(bitmapOfRunContainers != NULL);  // todo: handle
        for (int32_t i = 0; i < ra->size; ++i) {
            if (get_container_type(ra->containers[i], ra->typecodes[i]) ==
                RUN_CONTAINER_TYPE_CODE) {
                bitmapOfRunContainers[i / 8] |= (1 << (i % 8));
            }
        }
        memcpy(buf, bitmapOfRunContainers, s);
        buf += s;
        free(bitmapOfRunContainers);
        if (ra->size < NO_OFFSET_THRESHOLD) {
            startOffset = 4 + 4 * ra->size + s;
        } else {
            startOffset = 4 + 8 * ra->size + s;
        }
    } else {  // backwards compatibility
        uint32_t cookie = SERIAL_COOKIE_NO_RUNCONTAINER;

        memcpy(buf, &cookie, sizeof(cookie));
        buf += sizeof(cookie);
        memcpy(buf, &ra->size, sizeof(ra->size));
        buf += sizeof(ra->size);

        startOffset = 4 + 4 + 4 * ra->size + 4 * ra->size;
    }
    for (int32_t k = 0; k < ra->size; ++k) {
        memcpy(buf, &ra->keys[k], sizeof(ra->keys[k]));
        buf += sizeof(ra->keys[k]);

        uint16_t card =
            container_get_cardinality(ra->containers[k], ra->typecodes[k]) - 1;
        memcpy(buf, &card, sizeof(card));
        buf += sizeof(card);
    }
    if ((!hasrun) || (ra->size >= NO_OFFSET_THRESHOLD)) {
        // writing the containers offsets
        for (int32_t k = 0; k < ra->size; k++) {
            memcpy(buf, &startOffset, sizeof(startOffset));
            buf += sizeof(startOffset);
            startOffset =
                startOffset +
                container_size_in_bytes(ra->containers[k], ra->typecodes[k]);
        }
    }
    for (int32_t k = 0; k < ra->size; ++k) {
        buf += container_write(ra->containers[k], ra->typecodes[k], buf);
    }
    return buf - initbuf;
}

roaring_array_t *ra_portable_deserialize(const char *buf) {
    assert(!IS_BIG_ENDIAN);  // not implemented
    uint32_t cookie;
    memcpy(&cookie, buf, sizeof(int32_t));
    buf += sizeof(uint32_t);
    if ((cookie & 0xFFFF) != SERIAL_COOKIE &&
        cookie != SERIAL_COOKIE_NO_RUNCONTAINER) {
        fprintf(stderr, "I failed to find one of the right cookies. Found %d\n",
                cookie);
        return NULL;
    }
    int32_t size;

    if ((cookie & 0xFFFF) == SERIAL_COOKIE)
        size = (cookie >> 16) + 1;
    else {
        memcpy(&size, buf, sizeof(int32_t));
        buf += sizeof(uint32_t);
    }
    roaring_array_t *answer = ra_create_with_capacity(size);
    if (answer == NULL) {
        fprintf(stderr, "Failed to allocate memory early on. Bailing out.\n");
        return answer;
    }
    answer->size = size;
    char *bitmapOfRunContainers = NULL;
    bool hasrun = (cookie & 0xFFFF) == SERIAL_COOKIE;
    if (hasrun) {
        int32_t s = (size + 7) / 8;
        bitmapOfRunContainers = (char *)malloc((size + 7) / 8);
        assert(bitmapOfRunContainers != NULL);  // todo: handle
        memcpy(bitmapOfRunContainers, buf, s);
        buf += s;
    }
    uint16_t *keys = answer->keys;
    int32_t *cardinalities = (int32_t *)malloc(size * sizeof(int32_t));
    assert(cardinalities != NULL);  // todo: handle
    bool *isBitmap = (bool *)malloc(size * sizeof(bool));
    assert(isBitmap != NULL);  // todo: handle
    uint16_t tmp;
    for (int32_t k = 0; k < size; ++k) {
        memcpy(&keys[k], buf, sizeof(keys[k]));
        buf += sizeof(keys[k]);
        memcpy(&tmp, buf, sizeof(tmp));
        buf += sizeof(tmp);
        cardinalities[k] = 1 + tmp;
        isBitmap[k] = cardinalities[k] > DEFAULT_MAX_SIZE;
        if (bitmapOfRunContainers != NULL &&
            (bitmapOfRunContainers[k / 8] & (1 << (k % 8))) != 0) {
            isBitmap[k] = false;
        }
    }
    if ((!hasrun) || (size >= NO_OFFSET_THRESHOLD)) {
        // skipping the offsets
        buf += size * 4;
    }
    // Reading the containers
    for (int32_t k = 0; k < size; ++k) {
        if (isBitmap[k]) {
            bitset_container_t *c = bitset_container_create();
            assert(c != NULL);  // todo: handle
            buf += bitset_container_read(cardinalities[k], c, buf);
            answer->containers[k] = c;
            answer->typecodes[k] = BITSET_CONTAINER_TYPE_CODE;
        } else if (bitmapOfRunContainers != NULL &&
                   ((bitmapOfRunContainers[k / 8] & (1 << (k % 8))) != 0)) {
            run_container_t *c = run_container_create();
            assert(c != NULL);  // todo: handle
            buf += run_container_read(cardinalities[k], c, buf);
            answer->containers[k] = c;
            answer->typecodes[k] = RUN_CONTAINER_TYPE_CODE;
        } else {
            array_container_t *c =
                array_container_create_given_capacity(cardinalities[k]);
            assert(c != NULL);  // todo: handle
            buf += array_container_read(cardinalities[k], c, buf);
            answer->containers[k] = c;
            answer->typecodes[k] = ARRAY_CONTAINER_TYPE_CODE;
        }
    }
    free(bitmapOfRunContainers);
    free(cardinalities);
    free(isBitmap);
    return answer;
}


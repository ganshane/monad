# Copyright 2016 the original author or authors. All rights reserved.
# site: http://www.ganshane.com
SET(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -pthread " )

MACRO(ANAYTICS_TEST name)
	ADD_EXECUTABLE(${name} ${name}.cc)
	TARGET_LINK_LIBRARIES(${name} ${GTEST_BOTH_LIBRARIES} analytics pthread)
	ADD_TEST(${name} ${name})
ENDMACRO(ANAYTICS_TEST name)

ANAYTICS_TEST(test_open_bit_set)
ANAYTICS_TEST(test_open_bit_set_iterator)
ANAYTICS_TEST(test_open_bit_set_operator)
ANAYTICS_TEST(test_open_bit_set_wrapper)
ANAYTICS_TEST(test_priority_queue)
ANAYTICS_TEST(test_simple9)
ANAYTICS_TEST(test_sparse_bit_set)
ANAYTICS_TEST(test_sparse_bit_set_operator)
ANAYTICS_TEST(test_sparse_bit_set_wrapper)
ANAYTICS_TEST(test_variable_byte)

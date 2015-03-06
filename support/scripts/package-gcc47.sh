#!/bin/bash -e
export CC=/opt/centos/devtoolset-1.1/root/usr/bin/gcc
export CPP=/opt/centos/devtoolset-1.1/root/usr/bin/cpp
export CXX=/opt/centos/devtoolset-1.1/root/usr/bin/c++
ROOT=$(pwd)
mkdir monad-c/build
cd monad-c/build
rm -rf *
CC=/opt/centos/devtoolset-1.1/root/usr/bin/cc cmake -DCMAKE_CXX_FLAGS="-I/opt/centos/devtoolset-1.1/root/usr/include -std=c++11"  -DENABLE_ROCKSDB=on -DSTATIC_LINK=on -DCMAKE_BUILD_TYPE=Release ..
make
mkdir -p ${ROOT}/monad-jni/src/main/resources/META-INF/native/linux64
cp src/*.so ${ROOT}/monad-jni/src/main/resources/META-INF/native/linux64
mkdir -p ${ROOT}/monad-jni/src/main/java/monad/jni/services/gen
cp src/javaapi/* ${ROOT}/monad-jni/src/main/java/monad/jni/services/gen
cd $ROOT

#BUILD_NUMBER=r`git log -n1 --pretty=format:"%ad" --date=iso|awk -F'[-: ]' '{ print $1$2$3$4$5$6 }'`
BUILD_NUMBER=r`git log -n1 --pretty=format:%at | awk '{print strftime("%y%m%d%H%M",$1)}'`
echo MVN_OPTS="-Xmx1g" mvn -Dmaven.test.skip=true -DskipTests=true -Darguments="-DskipTests" -DBUILD_ID=`date +%Y%m%d_%H%M%S` -DBUILD_NUMBER=$BUILD_NUMBER -P production clean package
MVN_OPTS="-Xmx1g" mvn -Dmaven.test.skip=true -DskipTests=true -Darguments="-DskipTests" -DBUILD_ID=`date +%Y%m%d_%H%M%S` -DBUILD_NUMBER=$BUILD_NUMBER -P production clean package

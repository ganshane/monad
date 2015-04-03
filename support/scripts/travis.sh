#!/bin/bash

set -e

ROOT_DIR=$(pwd)
#create jni directory
mkdir -p ${ROOT_DIR}/monad-jni/src/main/java/monad/jni/services/gen
mkdir -p ${ROOT_DIR}/support/dll

sudo apt-get -yy purge mingw32 mingw32-binutils
sudo apt-get install -yy mingw-w64 g++-mingw-w64 swig

LIB_DIR=$(pwd)/_tmp
mkdir -p $LIB_DIR
mkdir -p ${LIB_DIR}/mingw
#compile leveldb
cd $LIB_DIR
git clone --depth=1 --branch=bitcoin-fork https://github.com/bitcoin/leveldb.git

#compile dll using mingw
compile_mingw() {
#  gcc-mingw-w64 g++-mingw-w64-x86-64 g++-mingw-w64-i686 gcc-mingw-w64-i686 gcc-mingw-w64-x86-64 \
#  binutils-mingw-w64-i686 binutils-mingw-w64-x86-64
  mkdir -p ${LIB_DIR}/${ARCH}

  #compile leveldb
  cd $LIB_DIR/leveldb
  CC=/usr/bin/${CROSSPREFIX}gcc CXX=${CROSSPREFIX}g++ AR=${CROSSPREFIX}ar TARGET_OS=OS_WINDOWS_CROSSCOMPILE make clean all
  cp libleveldb.a ${LIB_DIR}/${ARCH}
  cp -rp include/leveldb ${LIB_DIR}/mingw/

  #compile 
  cd ${ROOT_DIR}/monad-c
  mkdir build-$ARCH
  cd build-$ARCH
  LDFLAGS="-L/${LIB_DIR}/${ARCH}" CXXFLAGS="-I${LIB_DIR}/mingw"   \
    cmake -DCMAKE_TOOLCHAIN_FILE=${LIB_DIR}/../support/docker/compile-mingw/Toolchain-cross-mingw32-linux.cmake \
    -DHOST=$HOST  -DCMAKE_BUILD_TYPE=Release -DARCH=$ARCH \
    -DSWIG_DIR=/usr/share/swig2.0 \
    -DJAVA_AWT_LIBRARY=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/amd64/libjawt.so \
    -DJAVA_INCLUDE_PATH=/usr/lib/jvm/java-7-openjdk-amd64/include \
    -DJAVA_AWT_INCLUDE_PATH=/usr/lib/jvm/java-7-openjdk-amd64/include \
    -DJAVA_INCLUDE_PATH2=/usr/lib/jvm/java-7-openjdk-amd64/include/linux \
    -DJAVA_JVM_LIBRARY=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/amd64/libjava.so  ..
  make
  mkdir -p ${ROOT_DIR}/monad-jni/src/main/resources/META-INF/native/${JNI_DIR}
  cp src/libmonad4j.dll ${ROOT_DIR}/monad-jni/src/main/resources/META-INF/native/${JNI_DIR}/monad4j.dll
}
#win64  dll
HOST=x86_64-w64-mingw32 ARCH=w64 JNI_DIR=windows64 CROSSPREFIX=x86_64-w64-mingw32-  compile_mingw
#win32  dll
HOST=i686-w64-mingw32 ARCH=w32 JNI_DIR=windows32 CROSSPREFIX=i686-w64-mingw32- compile_mingw

#compile linux version
sudo apt-get -yy install libleveldb-dev swig libsnappy-dev
cd ${ROOT_DIR}/monad-c
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
make

#compile jni files
cp src/javaapi/*.java ${ROOT_DIR}/monad-jni/src/main/java/monad/jni/services/gen/
mkdir -p ${ROOT_DIR}/monad-jni/src/main/resources/META-INF/native/linux64
cp src/libmonad4j.so ${ROOT_DIR}/monad-jni/src/main/resources/META-INF/native/linux64/

#install protoc
mkdir -p $ROOT_DIR/target

#wget https://protobuf.googlecode.com/files/protobuf-2.5.0.tar.gz
#tar xfz protobuf-2.5.0.tar.gz
#cd protobuf-2.5.0
#CC=gcc CXX=g++ ./configure
#CC=gcc CXX=g++ make -j2
#sudo make install

wget https://github.com/google/protobuf/releases/download/v2.6.1/protobuf-2.6.1.tar.gz
tar xfvz protobuf-2.6.1.tar.gz
cd protobuf-2.6.1
CC=gcc CXX=g++ ./autogen.sh
CC=gcc CXX=g++ ./configure
make
sudo make install


sudo ldconfig


#building main scala project
#gpg http://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/
cd $ROOT_DIR
MAVEN_OPTS="-Xmx1g -Xms1g" mvn -V -B -P deploy \
  -Dgpg.defaultKeyring=false -Dgpg.passphrase=$GPG_PASS -Dgpg.keyname=$GPG_KEYID \
  clean deploy  --settings support/settings.xml


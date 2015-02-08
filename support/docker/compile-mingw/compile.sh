# Exit on first error
set -e

save_and_shutdown() {
  # save built for host result
  # force clean shutdown
  halt -f
}

# make sure we shut down cleanly
trap save_and_shutdown EXIT

git clone --branch=develop --depth=1 https://github.com/ganshane/monad.git .
#export VERSION=`cat pom.xml|grep -e '<version>\(.*\)</version>'|head -n 1|sed  's/^.*<version>\(.*\)<\/version>/\1/g'`
#export VERSION=`git log -n1 --pretty=format:%at | awk '{print strftime("%y%m%d%H%M",$1)}'`
export VERSION=`git log -n1 --pretty=format:%ai|cut -c1-10`

cd monad-c

#build =============> 32bit
export HOST=i686-w64-mingw32
export ARCH=w32
export CROSSPREFIX=i686-w64-mingw32-
mkdir build-$ARCH
cd build-$ARCH
LDFLAGS="-L/build/${ARCH}" CXXFLAGS="-I/build/mingw"   \
  cmake -DCMAKE_TOOLCHAIN_FILE=/build/Toolchain-cross-mingw32-linux.cmake \
  -DHOST=$HOST  -DCMAKE_BUILD_TYPE=Release -DARCH=$ARCH \
  -DSWIG_DIR=/usr/share/swig2.0 \
  -DJAVA_AWT_LIBRARY=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/amd64/libjawt.so \
  -DJAVA_INCLUDE_PATH=/usr/lib/jvm/java-7-openjdk-amd64/include \
  -DJAVA_AWT_INCLUDE_PATH=/usr/lib/jvm/java-7-openjdk-amd64/include \
  -DJAVA_INCLUDE_PATH2=/usr/lib/jvm/java-7-openjdk-amd64/include/linux \
  -DJAVA_JVM_LIBRARY=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/amd64/libjava.so  ..

make
cd -

scp build-$ARCH/src/*.dll jcai@dev.egfit.com:/opt/app/sites/lichen/nirvana/dll/$ARCH/monad4j.dll

#build =============> 64bit
export HOST=x86_64-w64-mingw32
export ARCH=w64
export CROSSPREFIX=x86_64-w64-mingw32-
mkdir build-$ARCH
cd build-$ARCH
LDFLAGS="-L/build/${ARCH}" CXXFLAGS="-I/build/mingw"   \
  cmake -DCMAKE_TOOLCHAIN_FILE=/build/Toolchain-cross-mingw32-linux.cmake \
  -DHOST=$HOST  -DCMAKE_BUILD_TYPE=Release -DARCH=$ARCH \
  -DSWIG_DIR=/usr/share/swig2.0 \
  -DJAVA_AWT_LIBRARY=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/amd64/libjawt.so \
  -DJAVA_INCLUDE_PATH=/usr/lib/jvm/java-7-openjdk-amd64/include \
  -DJAVA_AWT_INCLUDE_PATH=/usr/lib/jvm/java-7-openjdk-amd64/include \
  -DJAVA_INCLUDE_PATH2=/usr/lib/jvm/java-7-openjdk-amd64/include/linux \
  -DJAVA_JVM_LIBRARY=/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/amd64/libjava.so  ..
make
cd -

scp build-$ARCH/src/*.dll jcai@dev.egfit.com:/opt/app/sites/lichen/nirvana/dll/$ARCH/monad4j.dll

echo "[hit enter key to exit] or run 'docker stop <container>'"
read cmd
save_and_shutdown


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
export VERSION=`git log -n1 --pretty=format:%at | awk '{print strftime("%y%m%d%H%M",$1)}'`
export VERSION=`git log -n1 --pretty=format:%ai|cut -c1-10`

cd monad-c

#build =============> 32bit
export HOST=i686-w64-mingw32
export ARCH=w32
export CROSSPREFIX=i686-w64-mingw32-
mkdir build-$ARCH
cd build-$ARCH
cmake -DNIRVANA_SOURCE_VERSION=$VERSION -DARCH=$ARCH -DHOST=$HOST -DCMAKE_TOOLCHAIN_FILE=/build/Toolchain-cross-mingw32-linux.cmake   -DCMAKE_BUILD_TYPE=Release ..
make
cd -

scp build-$ARCH/src/*.dll jcai@dev.egfit.com:/opt/app/sites/lichen/nirvana/dll/

#build =============> 64bit
export HOST=x86_64-w64-mingw32
export ARCH=w64
export CROSSPREFIX=x86_64-w64-mingw32-
mkdir build-$ARCH
cd build-$ARCH
cmake -DNIRVANA_SOURCE_VERSION=$VERSION  -DARCH=$ARCH -DHOST=$HOST -DCMAKE_TOOLCHAIN_FILE=/build/Toolchain-cross-mingw32-linux.cmake   -DCMAKE_BUILD_TYPE=Release ..
make
cd -

scp build-$ARCH/src/*.dll jcai@dev.egfit.com:/opt/app/sites/lichen/nirvana/dll/

echo "[hit enter key to exit] or run 'docker stop <container>'"
read cmd
save_and_shutdown


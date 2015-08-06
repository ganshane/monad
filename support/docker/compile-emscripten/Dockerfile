#构建monad的编译环境
FROM ubuntu:trusty
MAINTAINER Jun Tsai - jcai@ganshane.com

WORKDIR /build
#mirror
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list
#install build library
RUN { \
  apt-get update &&\
  apt-get install -y build-essential curl git cmake python &&\
  apt-get clean;\
}
#install nodejs
RUN { \
  curl -sL https://deb.nodesource.com/setup | bash - && apt-get install -y nodejs &&\
  apt-get clean;\
}
#install emscripten
RUN { \
  curl https://s3.amazonaws.com/mozilla-games/emscripten/releases/emsdk-portable.tar.gz > emsdk-portable.tar.gz && \
  tar xzf emsdk-portable.tar.gz && \
  cd emsdk_portable && \
  ./emsdk update && ./emsdk install latest && \
  ./emsdk activate latest && \
  find . -name "*.o" -exec rm {} \; && \
  find . -name "*.a" -exec rm {} \; && \
  rm -rf ./clang/fastcomp/src && \
  rm -rf ./emscripten/master/.git && \
  apt-get -y autoremove && apt-get clean;\
}
#link 
RUN { \
  for prog in em++ em-config emar emcc emconfigure emmake emranlib emrun emscons; do \
    ln -sf ./emscripten/master/$prog /usr/local/bin; done ;\
}

   
#ssh_config
RUN echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config
RUN echo "UserKnownHostsFile /dev/null" >> /etc/ssh/ssh_config

#Add compile script
ADD compile.sh /compile.sh

#compile
VOLUME /dist
CMD ["sh","/compile.sh"]



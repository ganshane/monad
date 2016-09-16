FROM ubuntu:xenial
MAINTAINER Jun Tsai <jcai@ganshane.com>

ENV TEMPDIR   /tmp
#RUN dpkg --add-architecture i386
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com\/ubuntu\/ubuntu/g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y mingw-w64 g++-mingw-w64 git-core make cmake

#ssh_config
RUN echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config
RUN echo "UserKnownHostsFile /dev/null" >> /etc/ssh/ssh_config

WORKDIR /build

ADD Toolchain-cross-mingw32-linux.cmake /build/Toolchain-cross-mingw32-linux.cmake

RUN mkdir /build/w32
RUN mkdir /build/w64


VOLUME /dist
WORKDIR /build/monad
ADD compile.sh /compile.sh
CMD ["sh","/compile.sh"]

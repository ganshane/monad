#构建monad的编译环境
FROM centos:centos5
MAINTAINER Jun Tsai - jcai@ganshane.com

#Install the EPEL repo
RUN yum update -y
RUN rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/x86_64/epel-release-5-4.noarch.rpm

#Install package
RUN yum install -y  git java-1.7.0-openjdk-devel.x86_64 which openssh-clients.x86_64 wget autoconf automake libtool -y


#Install gcc-4.7
RUN cd /etc/yum.repos.d/ && \
  wget http://people.centos.org/tru/devtools-1.1/devtools-1.1.repo && \
  yum --enablerepo=testing-1.1-devtools-5 install devtoolset-1.1-gcc \
    devtoolset-1.1-gcc-c++ devtoolset-1.1-binutils make -y


#Install cmake
RUN export CC=/opt/centos/devtoolset-1.1/root/usr/bin/gcc && \
    export CPP=/opt/centos/devtoolset-1.1/root/usr/bin/cpp && \
    export CXX=/opt/centos/devtoolset-1.1/root/usr/bin/c++ && \
    mkdir /tmp/cmake && cd /tmp/cmake && \
    wget http://www.cmake.org/files/v2.8/cmake-2.8.12.2.tar.gz && \
    tar xfvz cmake-2.8.12.2.tar.gz && \
    cd cmake* && ./configure && \
    make && make install && \
    rm -rf /tmp/cmake

#Install maven
RUN mkdir /tmp/maven && cd /tmp/maven && \
    wget http://www.us.apache.org/dist/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
RUN mkdir -p /tmp/maven && cd /tmp/maven && \
    tar xfvz apache-maven-3.2.5-bin.tar.gz && \
    mv apache-maven-3.2.5 /opt/ && \
    ln -s /opt/apache-maven-3.2.5/bin/* /usr/local/bin/ && \
    echo "export M3_HOME=/opt/apache-maven-3.2.5" > /etc/profile.d/maven.sh && \
    echo "export M3=$M3_HOME/bin" >> /etc/profile.d/maven.sh && \
    echo "export PATH=$M3:$PATH" >> /etc/profile.d/maven.sh && \
    sh /etc/profile.d/maven.sh && \
    rm -rf /tmp/maven 

#Install which

RUN mvn --version


#Install swig
RUN mkdir /tmp/swig && cd /tmp/swig && \
    wget http://prdownloads.sourceforge.net/swig/swig-3.0.2.tar.gz && \
    yum install pcre-devel.x86_64 -y && \
    export CC=/opt/centos/devtoolset-1.1/root/usr/bin/gcc && \
    export CPP=/opt/centos/devtoolset-1.1/root/usr/bin/cpp && \
    export CXX=/opt/centos/devtoolset-1.1/root/usr/bin/c++ && \
    mkdir -p /tmp/swig && cd /tmp/swig && \
    tar xfvz swig-3.0.2.tar.gz && \
    cd swig-3.0.2 && \
    ./configure && \
    make && make install && \
    rm -rf /tmp/swig

WORKDIR /build
ENV CC /opt/centos/devtoolset-1.1/root/usr/bin/gcc
ENV CPP /opt/centos/devtoolset-1.1/root/usr/bin/cpp
ENV CXX /opt/centos/devtoolset-1.1/root/usr/bin/c++
#Install snappy,rocksdb,nirvana-kernel
RUN { \
    wget http://lichen.egfit.com/nirvana/gz/libsnappy.a -O /usr/local/lib/libsnappy.a \
    && wget http://lichen.egfit.com/nirvana/gz/snappy.h -O /usr/local/include/snappy.h \
    && wget https://github.com/google/protobuf/releases/download/v2.6.1/protobuf-2.6.1.tar.gz \
    && tar xfvz protobuf-2.6.1.tar.gz \
    && cd protobuf-2.6.1 \
    && ./autogen.sh \

    #&& wget http://lichen.egfit.com/nirvana/gz/protobuf-2.5.0.tar.gz \
    #&& tar xfvz protobuf-2.5.0.tar.gz \
    #&& cd protobuf-2.5.0 \
    && ./configure && make && make install \
    && cd - && rm -rf protobuf* \
    && git clone --depth=1 --branch=rocksdb-3.7 https://github.com/facebook/rocksdb.git \
    && cd rocksdb \
    && CXXFLAGS="-fPIC" make static_lib \
    && cp -r include/* /usr/local/include/ && cp librocksdb.a /usr/local/lib/ \
    && cd - && rm -rf rocksdb* \
    && ldconfig ; \
}
#clear yum cache
RUN yum clean all
#ssh_config
RUN echo "StrictHostKeyChecking no" >> /etc/ssh/ssh_config
RUN echo "UserKnownHostsFile /dev/null" >> /etc/ssh/ssh_config

#Add compile script
ADD compile.sh /compile.sh

#compile
VOLUME /dist
WORKDIR /build/monad
CMD ["sh","/compile.sh"]






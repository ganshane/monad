import sbt._

object Versions {
  lazy val app = "dev-SNAPSHOT"
  lazy val jetty = "8.1.16.v20140903"
  lazy val tapestry = "5.3.8"
  lazy val metrics ="3.1.0"
  lazy val lucene = "5.2.1"
  lazy val proguard = "5.1"
  lazy val slf4j = "1.6.1"
}

object Deps {
  lazy val httpClient = "org.apache.httpcomponents" % "httpclient" % "4.3.5"
  lazy val servlet = "javax.servlet" % "javax.servlet-api" % "3.0.1"
  lazy val tapestryCore = "org.apache.tapestry" % "tapestry-core" % Versions.tapestry
  lazy val tapestryJson = "org.apache.tapestry" % "tapestry-json" % Versions.tapestry
  lazy val tapestryIoc = "org.apache.tapestry" % "tapestry-ioc" % Versions.tapestry exclude("org.testng","testng")
  lazy val commonsIo = "commons-io" % "commons-io" % "2.4"
  lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.5"
  lazy val spymemcached = "net.spy" % "spymemcached" % "2.11.4"
  lazy val javassist = "org.javassist" % "javassist" % "3.18.1-GA"
  lazy val disruptor = "com.lmax" % "disruptor" % "3.2.1"
  lazy val protobuf = "com.google.protobuf" % "protobuf-java" % "2.6.1"
  lazy val metricsCore = "io.dropwizard.metrics" % "metrics-core" % Versions.metrics
  lazy val metricsJvm = "io.dropwizard.metrics" % "metrics-jvm" % Versions.metrics
  lazy val metricsJetty = "io.dropwizard.metrics" % "metrics-jetty8" % Versions.metrics
  lazy val jna = "net.java.dev.jna" % "jna" % "4.1.0"
  lazy val netty = "io.netty" % "netty" % "3.10.3.Final"
  lazy val rhino = "org.mozilla" % "rhino" % "1.7R5"
  lazy val h2 = "com.h2database" % "h2" % "1.3.176"
  lazy val jettyServer = "org.eclipse.jetty" % "jetty-server" % Versions.jetty
  lazy val jettyServlet = "org.eclipse.jetty" % "jetty-servlet" % Versions.jetty
  lazy val jettyWebapp= "org.eclipse.jetty" % "jetty-webapp" % Versions.jetty
  lazy val jettyJsp= "org.eclipse.jetty" % "jetty-jsp" % Versions.jetty
  lazy val concurrentlinkedhashmap= "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.2"
  lazy val jansi = "org.fusesource.jansi" % "jansi" % "1.11"
  lazy val slf4jApi = "org.slf4j" % "slf4j-api" % Versions.slf4j
  lazy val slf4jLog4j= "org.slf4j" % "slf4j-log4j12" % Versions.slf4j
  lazy val slf4jJul= "org.slf4j" % "jul-to-slf4j" % Versions.slf4j
  lazy val zookeeper = "org.apache.zookeeper" % "zookeeper" % "3.4.5"  exclude ("log4j","log4j") exclude ("org.jboss.netty","netty")
  lazy val curator = "org.apache.curator" % "curator-framework" % "2.6.0"
  lazy val gson = "org.google.code.gson" % "gson" % "2.3.1"
  lazy val luceneCore = "org.apache.lucene" % "lucene-core" % Versions.lucene
  lazy val luceneFacet= "org.apache.lucene" % "lucene-facet" % Versions.lucene
  lazy val luceneAnalyzersCommon= "org.apache.lucene" % "lucene-analyzers-common" % Versions.lucene
  lazy val luceneAnalyzersSmartcn= "org.apache.lucene" % "lucene-analyzers-smartcn" % Versions.lucene
  lazy val luceneQueryparser= "org.apache.lucene" % "lucene-queryparser" % Versions.lucene
  lazy val luceneMisc= "org.apache.lucene" % "lucene-misc" % Versions.lucene
  lazy val luceneHighlighter= "org.apache.lucene" % "lucene-highlighter" % Versions.lucene
  lazy val junit = "junit" % "junit" % "4.8.2" % "test"
  lazy val mockito = "org.mockito" % "mockito-all" % "1.10.19" % "test"
}

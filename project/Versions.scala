import sbt._

object Versions {
  lazy val app = "dev-SNAPSHOT"
  lazy val jetty = "8.1.16.v20140903"
  lazy val tapestry = "5.3.8"
}

object Deps {
  lazy val httpClient = "org.apache.httpcomponents" % "httpclient" % "4.3.5"
  lazy val servlet = "javax.servlet" % "servlet-api" % "2.5"
  lazy val jettyServer = "org.eclipse.jetty" % "jetty-server" % Versions.jetty
  lazy val jettyServlet = "org.eclipse.jetty" % "jetty-servlet" % Versions.jetty
  lazy val tapestryCore = "org.apache.tapestry" % "tapestry-core" % Versions.tapestry % "provided"
  lazy val curatorFramework = "org.apache.curator" % "curator-framework" % "2.6.0" % "provided"
  lazy val commonsIo = "commons-io" % "commons-io" % "2.4" % "provided"
  lazy val junit = "junit" % "junit" % "4.8.2" % "test"
}

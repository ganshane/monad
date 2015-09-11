import sbt.Keys._
import sbt._

object MonadBuild extends Build {

  object sonatype extends PublishToSonatype(MonadBuild) {
    def projectUrl    = "https://github.com/ganshane/monad"
    def developerId   = "jcai"
    def developerName = "Jun Tsai"
  }

  lazy val mainSettings = sonatype.settings ++ Seq(
      version := Versions.app,
      organization := "com.ganshane",
      scalaVersion := "2.11.2",
      crossScalaVersions := Seq("2.11.2","2.10.4"),
      dependencyOverrides += "log4j" % "log4j" % "1.2.16" force(),
      dependencyOverrides += "org.slf4j" % "slf4j-api" % "1.6.6" force(),
      resolvers += Resolver.mavenLocal,

      credentials += Credentials(new File("../.credentials"))
  )
  lazy val root = Project("monad-project", file("."),settings = mainSettings).aggregate(supportProject)
  lazy val supportProject = Project("monad-support", file("monad-support"),settings = mainSettings).settings(
    libraryDependencies ++= Seq(
      Deps.httpClient,
      Deps.servlet,
      Deps.jettyServer,
      Deps.jettyServlet,
      Deps.tapestryCore,
      Deps.curatorFramework,
      Deps.commonsIo,
      Deps.junit)
  )

  /**
   * Source:  https://github.com/paulp/scala-improving/blob/master/project/Publishing.scala
   * License: https://github.com/paulp/scala-improving/blob/master/LICENSE.txt
   */
  abstract class PublishToSonatype(build: Build) {

    val ossSnapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
    val ossStaging   = "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

    def projectUrl: String
    def developerId: String
    def developerName: String

    def licenseName         = "Apache"
    def licenseUrl          = "http://www.apache.org/licenses/LICENSE-2.0"
    def licenseDistribution = "repo"
    def scmUrl              = projectUrl
    def scmConnection       = "scm:git:" + scmUrl

    def generatePomExtra(scalaVersion: String): xml.NodeSeq = {
      <url>{ projectUrl }</url>
        <licenses>
          <license>
            <name>{ licenseName }</name>
            <url>{ licenseUrl }</url>
            <distribution>{ licenseDistribution }</distribution>
          </license>
        </licenses>
        <scm>
          <url>{ scmUrl }</url>
          <connection>{ scmConnection }</connection>
        </scm>
        <developers>
          <developer>
            <id>{ developerId }</id>
            <name>{ developerName }</name>
          </developer>
        </developers>
    }

    def settings: Seq[Setting[_]] = Seq(
      publishMavenStyle := true,
      publishTo <<= version((v: String) => Some( if (v.trim endsWith "SNAPSHOT") ossSnapshots else ossStaging)),
      publishArtifact in Test := false,
      pomIncludeRepository := (_ => false),
      pomExtra <<= scalaVersion(generatePomExtra)
    )
  }
}


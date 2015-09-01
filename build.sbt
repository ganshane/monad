import sbtprotobuf.{ProtobufPlugin => PB}

resolvers += Resolver.url(
    "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"


Seq(PB.protobufSettings: _*)

//pgpPassphrase := Some(Array())

//pgpSecretRing := file("../secring.gpg")

//pgpPublicRing := file("../pubring.gpg")

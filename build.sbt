resolvers += Resolver.url(
    "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"

//pgpPassphrase := Some(Array())

//pgpSecretRing := file("../secring.gpg")

//pgpPublicRing := file("../pubring.gpg")

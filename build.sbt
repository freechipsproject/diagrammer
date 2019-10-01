name := "firrtl-diagrammer"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)
organization := "edu.berkeley.cs"
version := "1.1-SNAPSHOT"
autoAPIMappings := true
scalaVersion := "2.12.7"
crossScalaVersions := Seq("2.12.7", "2.11.12")
scalacOptions := Seq("-deprecation", "-feature") ++ scalacOptionsVersion(scalaVersion.value)

def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // If we're building with Scala > 2.11, enable the compile option
    //  switch to support our anonymous Bundle definitions:
    //  https://github.com/scala/bug/issues/10047
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 => Seq()
      case _ => Seq("-Xsource:2.11")
    }
  }
}

def javacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // Scala 2.12 requires Java 8. We continue to generate
    //  Java 7 compatible code for Scala 2.11
    //  for compatibility with old clients.
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 =>
        Seq("-source", "1.7", "-target", "1.7")
      case _ =>
        Seq("-source", "1.8", "-target", "1.8")
    }
  }
}


publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { x => false }
// Don't add 'scm' elements if we have a git.remoteRepo definition,
//  but since we don't (with the removal of ghpages), add them in below.
pomExtra := <url>http://chisel.eecs.berkeley.edu/</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/freechipsproject/diagrammer.git</url>
    <connection>scm:git:github.com/freechipsproject/diagrammer.git</connection>
  </scm>
  <developers>
    <developer>
      <id>chick</id>
      <name>Chick Markley</name>
      <url>https://github.com/chick</url>
    </developer>
    <developer>
      <id>mgnica</id>
      <name>Monica Kumaran</name>
      <url>https://github.com/mgnica</url>
    </developer>
  </developers>

publishTo := {
  val v = version.value
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  }
  else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "chisel3" -> "3.2-SNAPSHOT"
)

libraryDependencies ++= Seq("chisel3").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) }

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "com.github.scopt" %% "scopt" % "3.7.1"
)

scalacOptions ++= scalacOptionsVersion(scalaVersion.value)

javacOptions ++= javacOptionsVersion(scalaVersion.value)


// Assembly

assemblyJarName in assembly := "diagrammer.jar"

test in assembly := {} // Should there be tests?

assemblyOutputPath in assembly := file("./utils/bin/diagrammer.jar")


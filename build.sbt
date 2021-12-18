// SPDX-License-Identifier: Apache-2.0

name := "firrtl-diagrammer"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)
organization := "edu.berkeley.cs"
version := "1.5.0-RC2"
autoAPIMappings := true
scalaVersion := "2.12.14"
crossScalaVersions := Seq("2.12.15", "2.13.6")

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
    Some("snapshots".at(nexus + "content/repositories/snapshots"))
  } else {
    Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  }
}

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "chisel3" -> "3.5.0-RC2"
)

libraryDependencies ++= Seq("chisel3").map { dep: String =>
  "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep))
}

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.9" % "test",
)

scalacOptions in Compile ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:reflectiveCalls",
  "-language:existentials",
  "-language:implicitConversions"
)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Assembly

assemblyJarName in assembly := "diagrammer.jar"

test in assembly := {} // Should there be tests?

assemblyOutputPath in assembly := file("./utils/bin/diagrammer.jar")

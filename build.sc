import ammonite.ops._
import ammonite.ops.ImplicitWd._
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.eval.Evaluator

import $file.CommonBuild

// An sbt layout with src in the top directory.
trait CrossUnRootedSbtModule extends CrossSbtModule {
  override def millSourcePath = super.millSourcePath / ammonite.ops.up
}

trait CommonModule extends CrossUnRootedSbtModule with PublishModule {
  def publishVersion = "1.1-SNAPSHOT"

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "edu.berkeley.cs",
    url = "https://github.com/freechipsproject/diagrammer.git",
    licenses = Seq(License.`BSD-3-Clause`),
    versionControl = VersionControl.github("freechipsproject", "diagrammer"),
    developers = Seq(
      Developer("chick",    "Charles Markley",     "https://github.com/chick"),
      Developer("mgnica",   "Monica Kumaran",      "https://github.com/mgnica")
    )
  )

  override def scalacOptions = Seq(
    "-deprecation",
    "-explaintypes",
    "-feature", "-language:reflectiveCalls",
    "-unchecked",
    "-Xcheckinit",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator"
  ) ++ CommonBuild.scalacOptionsVersion(crossScalaVersion)

  override def javacOptions = CommonBuild.javacOptionsVersion(crossScalaVersion)
}

val crossVersions = Seq("2.12.7", "2.11.12")

// Make this available to external tools.
object diagrammer extends Cross[DiagrammerModule](crossVersions: _*) {
  def defaultVersion(ev: Evaluator) = T.command{
    println(crossVersions.head)
  }

  def compile = T{
    diagrammer(crossVersions.head).compile()
  }

  def jar = T{
    diagrammer(crossVersions.head).jar()
  }

  def test = T{
    diagrammer(crossVersions.head).test.test()
  }

  def publishLocal = T{
    diagrammer(crossVersions.head).publishLocal()
  }

  def docJar = T{
    diagrammer(crossVersions.head).docJar()
  }

  def assembly = T{
    diagrammer(crossVersions.head).assembly()
  }
}

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map("chisel3" -> "3.2-SNAPSHOT")

def getVersion(dep: String, org: String = "edu.berkeley.cs") = {
  val version = sys.env.getOrElse(dep + "Version", defaultVersions(dep))
  ivy"$org::$dep:$version"
}

class DiagrammerModule(val crossScalaVersion: String) extends CommonModule {
  override def artifactName = "firrtl-diagrammer"

  def chiselDeps = Agg("chisel3").map { d => getVersion(d) }

  override def ivyDeps = Agg(
    ivy"org.scala-lang.modules:scala-jline:2.12.1",
    ivy"org.json4s::json4s-native:3.5.3"
  ) ++ chiselDeps

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.8",
      ivy"org.scalacheck::scalacheck:1.14.0"
    )
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  def mainClass = Some("dotvisualizer.FirrtlDiagrammer")
}


import mill._, scalalib._

object diagrammer extends SbtModule {
  def scalaVersion = "2.12.6"

  def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.2-SNAPSHOT"
  )

  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.4")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  // src/ folders etc are directly in this folder as opposed to
  // e.g. diagrammer/src
  def millSourcePath = os.pwd

  def mainClass = Some("dotvisualizer.FirrtlDiagrammer")
}


// See LICENSE for license details.

package example

import chisel3._
import dotvisualizer.{TopOfVisualizer, VisualizerAnnotator}
import firrtl.annotations.Annotation
import org.scalatest.{FreeSpec, Matchers}

class MyManyDynamicElementVecFir(length: Int) extends Module with VisualizerAnnotator {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
    val consts = Input(Vec(length, UInt(8.W)))
  })

  val taps = Seq(io.in) ++ Seq.fill(io.consts.length - 1)(RegInit(0.U(8.W)))
  taps.zip(taps.tail).foreach { case (a, b) => b := a }

  io.out := taps.zip(io.consts).map { case (a, b) => a * b }.reduce(_ + _)
  visualize(this, depth = 1)
  // setDotProgram("fdp")
  setDotProgram("dot")
}



class FirExampleSpec extends FreeSpec with Matchers {
  def findAnno(as: Seq[Annotation], name: String): Option[Annotation] = {
    as.find { a => a.targetString == name }
  }

  """
    |Visualizer is an example of a module that has two sub-modules A and B who both instantiate their
    |own instances of module C.  This highlights the difference between specific and general
    |annotation scopes
  """.stripMargin - {

    """
      |annotations are not resolved at after circuit elaboration,
      |that happens only after emit has been called on circuit""".stripMargin in {

      Driver.execute(Array("--target-dir", "test_run_dir", "--compiler", "low"), () => new MyManyDynamicElementVecFir(10)) match {
        case ChiselExecutionSuccess(Some(_), emitted, _) =>
          println(s"done!")
        case _ =>
          throw new Exception("bad parse")
      }
    }
  }
}
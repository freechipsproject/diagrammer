// See LICENSE for license details.

package example

import chisel3._
import dotvisualizer.VisualizerAnnotator
import org.scalatest.{FreeSpec, Matchers}

class MyManyDynamicElementVecFir(length: Int) extends Module with VisualizerAnnotator {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
    val consts = Input(Vec(length, UInt(8.W)))
  })

  val taps: Seq[UInt] = Seq(io.in) ++ Seq.fill(io.consts.length - 1)(RegInit(0.U(8.W)))
  taps.zip(taps.tail).foreach { case (a, b) => b := a }

  io.out := taps.zip(io.consts).map { case (a, b) => a * b }.reduce(_ + _)
  visualize(this, depth = 1)
  // setDotProgram("fdp")
  setDotProgram("dot")
}



class FirExampleSpec extends FreeSpec with Matchers {
//  def findAnno(as: Seq[Annotation], name: String): Option[Annotation] = {
//    as.find { a => a.targetString == name }
//  }

  """
    |This is an example of an FIR circuit which has a lot of elements in a single module
  """.stripMargin  in {

    Driver.execute(
      Array("--target-dir", "test_run_dir/fir_example", "--top-name", "fir_example", "--compiler", "low"),
      () => new MyManyDynamicElementVecFir(10)
    ) match {
      case ChiselExecutionSuccess(Some(_), _, _) =>
      case _ =>
        throw new Exception("bad parse")
    }
  }
}
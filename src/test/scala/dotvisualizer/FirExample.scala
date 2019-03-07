// See LICENSE for license details.

package dotvisualizer

import chisel3._
import org.scalatest.{FreeSpec, Matchers}

class MyManyDynamicElementVecFir(length: Int) extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
    val consts = Input(Vec(length, UInt(8.W)))
  })

  val taps: Seq[UInt] = Seq(io.in) ++ Seq.fill(io.consts.length - 1)(RegInit(0.U(8.W)))
  taps.zip(taps.tail).foreach { case (a, b) => b := a }

  io.out := taps.zip(io.consts).map { case (a, b) => a * b }.reduce(_ + _)
}

class FirExampleSpec extends FreeSpec with Matchers {

  """This is an example of an FIR circuit which has a lot of elements in a single module""" in {
    val circuit = chisel3.Driver.elaborate(() => new MyManyDynamicElementVecFir(10))
    val firrtl = chisel3.Driver.emit(circuit)
    val config = Config(
      targetDir = "test_run_dir/fir_example/", firrtlSource = firrtl, rankDir = "TB", useRanking = true
    )
    FirrtlDiagrammer.run(config)
  }
}
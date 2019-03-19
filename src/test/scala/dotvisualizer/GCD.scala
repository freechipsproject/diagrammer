// See LICENSE for license details.

package dotvisualizer

import chisel3._
import org.scalatest.{FreeSpec, Matchers}

class GCD extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val a  = Input(UInt(16.W))
    val b  = Input(UInt(16.W))
    val e  = Input(Bool())
    val z  = Output(UInt(16.W))
    val v  = Output(Bool())
  })

  val x  = Reg(UInt())
  val y  = Reg(UInt())

  when (x > y) { x := x - y }
    .otherwise { y := y - x }

  when (io.e) { x := io.a; y := io.b }
  io.z := x
  io.v := y === 0.U
}

class GCDTester extends FreeSpec with Matchers {
  "GCD circuit to visualize" in {
    val circuit = chisel3.Driver.elaborate(() => new GCD)
    val firrtl = chisel3.Driver.emit(circuit)
    val config = Config(targetDir = "test_run_dir/gcd/", firrtlSource = firrtl, useRanking = true)
    FirrtlDiagrammer.run(config)
  }
}


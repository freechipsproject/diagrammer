// SPDX-License-Identifier: Apache-2.0

package dotvisualizer

import java.io.{ByteArrayOutputStream, File, PrintStream}

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import dotvisualizer.stage.DiagrammerStage
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import scala.language.reflectiveCalls

class GCD extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val a = Input(UInt(16.W))
    val b = Input(UInt(16.W))
    val e = Input(Bool())
    val z = Output(UInt(16.W))
    val v = Output(Bool())
  })

  val x = Reg(UInt())
  val y = Reg(UInt())

  when(x > y) { x := x - y }.otherwise { y := y - x }

  when(io.e) { x := io.a; y := io.b }
  io.z := x
  io.v := y === 0.U
}

class GCDTester extends AnyFreeSpec with Matchers {
  "GCD circuit to visualize" in {
    val targetDir = "test_run_dir/gcd"
    val targetFile = new File(s"$targetDir/GCD.dot.svg")
    if (targetFile.exists()) {
      targetFile.delete()
    }

    val outputBuf = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(outputBuf)) {
      (new DiagrammerStage).execute(
        Array("--target-dir", targetDir, "--rank-elements", "--open-command", ""),
        Seq(ChiselGeneratorAnnotation(() => new GCD))
      )
    }
    val output = outputBuf.toString

    // confirm user gets message
    output should include("creating dot file test_run_dir/gcd/GCD.dot")
    // confirm we have turned off opening file in browser
    output should include("There is no program identified which will render the svg files")
    // confirm we have turned off opening file in browser
    output should include("There is no program identified which will render the svg files")

    targetFile.exists() should be(true)
  }
}

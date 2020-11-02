// SPDX-License-Identifier: Apache-2.0

package dotvisualizer

import java.io.{ByteArrayOutputStream, File, PrintStream}

import chisel3._
import chisel3.stage.ChiselStage
import dotvisualizer.stage.{DiagrammerStage, OpenCommandAnnotation, RankDirAnnotation, RankElementsAnnotation}
import firrtl.options.TargetDirAnnotation
import firrtl.stage.FirrtlSourceAnnotation
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import scala.language.reflectiveCalls

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

class FirExampleSpec extends AnyFreeSpec with Matchers {

  """This is an example of an FIR circuit which has a lot of elements in a single module""" in {
    val targetDir = "test_run_dir/fir_example"
    val targetFile = new File(s"$targetDir/MyManyDynamicElementVecFir_hierarchy.dot.svg")
    if (targetFile.exists()) {
      targetFile.delete()
    }

    val outputBuf = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(outputBuf)) {
      val firrtl = (new ChiselStage).emitFirrtl(
        new MyManyDynamicElementVecFir(length = 10),
        annotations = Seq(TargetDirAnnotation(targetDir))
      )

      val annos = Seq(
        TargetDirAnnotation(targetDir),
        FirrtlSourceAnnotation(firrtl),
        RankElementsAnnotation,
        RankDirAnnotation("TB"),
        OpenCommandAnnotation("")
      )
      (new DiagrammerStage).transform(annos)
    }
    val output = outputBuf.toString

    output should include("creating dot file test_run_dir/fir_example/MyManyDynamicElementVecFir.dot")
    // confirm we have turned off opening file in browser
    output should include("There is no program identified which will render the svg files")

    targetFile.exists() should be(true)
  }
}

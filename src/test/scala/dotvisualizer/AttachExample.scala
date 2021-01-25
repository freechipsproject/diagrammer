// SPDX-License-Identifier: Apache-2.0

package dotvisualizer

import java.io.{ByteArrayOutputStream, PrintStream}

import dotvisualizer.stage.{DiagrammerStage, OpenCommandAnnotation}
import firrtl.FileUtils
import firrtl.options.TargetDirAnnotation
import firrtl.stage.FirrtlSourceAnnotation
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

/** Checks that attaches are double headed arrows connected to each node
  */
class AttachExample extends AnyFreeSpec with Matchers {

  """This example contains Analog Connects""" in {
    val dir = "test_run_dir/attach"
    FileUtils.makeDirectory(dir)

    val firrtl = s"""
                    |circuit AttachTest :
                    |  module AttachTest :
                    |    input clock : Clock
                    |    input reset : UInt<1>
                    |
                    |    output io : {in : Analog<1>, out : Analog<1>}
                    |    output io2 : {in : Analog<1>, out1 : Analog<1>, out2 : Analog<1>, out3 : Analog<1>}
                    |
                    |    attach (io.in, io.out) @[cmd8.sc 6:9]
                    |
                    |    attach (io2.in, io2.out1, io2.out2, io2.out3) @[cmd8.sc 6:9]
                    |
       """.stripMargin

    val outputBuf = new ByteArrayOutputStream()
    Console.withOut(new PrintStream(outputBuf)) {
      val annos = Seq(TargetDirAnnotation(dir), FirrtlSourceAnnotation(firrtl), OpenCommandAnnotation(""))
      (new DiagrammerStage).execute(Array.empty, annos)
    }
    val output = outputBuf.toString

    // confirm user gets message
    output should include("creating dot file test_run_dir/attach/AttachTest.dot")
    // confirm we have turned off opening file in browser
    output should include("There is no program identified which will render the svg files")

    val lines = Source.fromFile(s"$dir/AttachTest.dot").getLines()

    val targets = Seq(
      s"""cluster_AttachTest_io_out -> cluster_AttachTest_io_in [dir = "both"]""",
      s"""cluster_AttachTest_io2_out1 -> cluster_AttachTest_io2_in [dir = "both"]""",
      s"""cluster_AttachTest_io2_out2 -> cluster_AttachTest_io2_in [dir = "both"]""",
      s"""cluster_AttachTest_io2_out3 -> cluster_AttachTest_io2_in [dir = "both"]"""
    )

    targets.foreach { target =>
      lines.exists { line => line.contains(target) } should be(true)
    }
  }
}

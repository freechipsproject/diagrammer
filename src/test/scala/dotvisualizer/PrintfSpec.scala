// SPDX-License-Identifier: Apache-2.0

package dotvisualizer

import java.io.{ByteArrayOutputStream, PrintStream}

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import dotvisualizer.stage._
import firrtl.FileUtils
import firrtl.annotations.Annotation
import firrtl.options.TargetDirAnnotation
import firrtl.stage.FirrtlSourceAnnotation
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class HasPrintf extends MultiIOModule {
  val in = IO(Input(Bool()))
  val out = IO(Output(Bool()))
  out := in
  printf("in %d, out %d\n", in, out)
}
class PrintfSpec extends AnyFreeSpec with Matchers {

  "printfs can now be rendered" - {

    def makeDotFile(showPrintfs: Boolean): String = {
      val targetDir = s"test_run_dir/has_printf_$showPrintfs"
      val annos = Seq(
        TargetDirAnnotation(targetDir),
        ChiselGeneratorAnnotation(() => new HasPrintf),
        RankElementsAnnotation,
        RankDirAnnotation("TB"),
        OpenCommandAnnotation("")
      ) ++ (if (showPrintfs) { Seq(ShowPrintfsAnnotation) }
            else { Seq.empty[Annotation] })
      val outputBuf = new ByteArrayOutputStream()
      Console.withOut(new PrintStream(outputBuf)) {
        (new DiagrammerStage).transform(annos)
      }
      val output = outputBuf.toString
      output should include(s"creating dot file test_run_dir/has_printf_$showPrintfs/HasPrintf.dot")

      FileUtils.getText(s"$targetDir/HasPrintf.dot")
    }

    "showPrintfs=true will render printfs in dot file" in {
      val dotText = makeDotFile(showPrintfs = true)

      dotText should include("struct_cluster_HasPrintf_printf_")
      dotText should include("""printf("in %d, out %d\n")""")
    }

    "default behavior will not render printfs in dot file" in {
      val dotText = makeDotFile(showPrintfs = false)

      dotText.contains("struct_cluster_HasPrintf_printf_") should be(false)
      dotText.contains("""printf("in %d, out %d\n")""") should be(false)
    }
  }
}

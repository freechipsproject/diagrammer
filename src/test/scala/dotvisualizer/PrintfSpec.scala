// See README.md for license details.

package dotvisualizer

import org.scalatest.{FreeSpec, Matchers}
import chisel3._
import firrtl.FileUtils

class HasPrintf extends MultiIOModule {
  val in = IO(Input(Bool()))
  val out = IO(Output(Bool()))
  out := in
  printf("in %d, out %d\n", in, out)
}
class PrintfSpec extends FreeSpec with Matchers {

  "printfs can now be rendered" - {
    val dirName = "test_run_dir/has_printf_on/"

    def makeDotFile(showPrintfs: Boolean): String = {
      val circuit = chisel3.Driver.elaborate(() => new HasPrintf)
      val firrtl = chisel3.Driver.emit(circuit)
      val config = Config(
        targetDir = dirName,
        firrtlSource = firrtl,
        rankDir = "TB",
        useRanking = true,
        showPrintfs = showPrintfs,
        openProgram = ""
      )
      FirrtlDiagrammer.run(config)

      FileUtils.getText(s"${dirName}HasPrintf.dot")
    }

    "showPrintfs=true will render printfs in dot file" in {
      val dotText = makeDotFile(showPrintfs = true)

      dotText.contains("struct_cluster_HasPrintf_printf_1") should be(true)
      dotText.contains("""printf("in %d, out %d\n")""") should be(true)
    }

    "default behavior will not render printfs in dot file" in {
      val dotText = makeDotFile(showPrintfs = false)

      dotText.contains("struct_cluster_HasPrintf_printf_1") should be(false)
      dotText.contains("""printf("in %d, out %d\n")""") should be(false)
    }
  }
}

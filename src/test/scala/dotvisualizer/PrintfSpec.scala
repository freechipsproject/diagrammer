/*
Copyright 2020 The Regents of the University of California (Regents)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package dotvisualizer

import chisel3._
import firrtl.FileUtils
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

      dotText should include ("struct_cluster_HasPrintf_printf_")
      dotText should include ("""printf("in %d, out %d\n")""")
    }

    "default behavior will not render printfs in dot file" in {
      val dotText = makeDotFile(showPrintfs = false)

      dotText.contains("struct_cluster_HasPrintf_printf_") should be(false)
      dotText.contains("""printf("in %d, out %d\n")""") should be(false)
    }
  }
}

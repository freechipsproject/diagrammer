// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage

import chisel3.stage.ChiselCli
import firrtl.AnnotationSeq
import firrtl.options.{Shell, Stage}
import firrtl.stage.FirrtlCli

/** DiagrammerState creates a diagram of a firrtl circuit. The firrtl circuit can be in any one of the following
  * forms
  * ChiselGeneratorAnnotation(() => new DUT())  a chisel DUT that is used to generate firrtl
  * FirrtlFileAnnotation(fileName)              a file name that contains firrtl source
  * FirrtlSourceAnnotation                      in-line firrtl source
  */
class DiagrammerStage extends Stage {
  val shell: Shell = new Shell("chisel") with DiagrammerCli with ChiselCli with FirrtlCli

  override protected def run(annotations: AnnotationSeq): AnnotationSeq = {
    (new DiagrammerPhase).transform(annotations)
  }
}

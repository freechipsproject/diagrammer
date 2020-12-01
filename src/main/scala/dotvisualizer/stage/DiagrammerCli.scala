// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage

import firrtl.options.Shell

trait DiagrammerCli {
  this: Shell =>

  parser.note("Diagrammer Front End Options")

  Seq(
    StartModuleNameAnnotation,
    OpenCommandAnnotation,
    SetRenderProgramAnnotation,
    JustTopLevelAnnotation,
    RankDirAnnotation,
    RankElementsAnnotation,
    ShowPrintfsAnnotation,
    DotTimeoutSecondsAnnotation
  ).foreach(_.addOptions(parser))
}

// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage

import firrtl.annotations.{Annotation, NoTargetAnnotation}
import firrtl.options.{HasShellOptions, ShellOption, Unserializable}

sealed trait DiagrammerAnnotation {
  this: Annotation =>
}

case class StartModuleNameAnnotation(name: String)
    extends DiagrammerAnnotation
    with NoTargetAnnotation
    with Unserializable

object StartModuleNameAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[String](
      longOption = "module-name",
      toAnnotationSeq = (a: String) => Seq(StartModuleNameAnnotation(a)),
      helpText = "the module in the hierarchy to start, default is the circuit top"
    )
  )
}

case class OpenCommandAnnotation(name: String) extends DiagrammerAnnotation with NoTargetAnnotation with Unserializable

object OpenCommandAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[String](
      longOption = "open-command",
      toAnnotationSeq = (a: String) => Seq(OpenCommandAnnotation(a)),
      helpText = "The name of the program to open svg file in browser"
    )
  )
}

case class SetRenderProgramAnnotation(name: String)
    extends DiagrammerAnnotation
    with NoTargetAnnotation
    with Unserializable

object SetRenderProgramAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[String](
      longOption = "render-command",
      toAnnotationSeq = (a: String) => Seq(SetRenderProgramAnnotation(a)),
      helpText = "The name of the program to render svg, default is dot"
    )
  )
}

case object JustTopLevelAnnotation
    extends NoTargetAnnotation
    with DiagrammerAnnotation
    with HasShellOptions
    with Unserializable {
  val options = Seq(
    new ShellOption[Unit](
      longOption = "just-top-level",
      toAnnotationSeq = _ => Seq(JustTopLevelAnnotation),
      helpText = "The name of the program to open svg file in browser"
    )
  )
}

case class RankDirAnnotation(name: String) extends DiagrammerAnnotation with NoTargetAnnotation with Unserializable

object RankDirAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[String](
      longOption = "rank-dir",
      toAnnotationSeq = (a: String) => Seq(RankDirAnnotation(a)),
      helpText = "use to set ranking direction, default is LR, TB is good alternative"
    )
  )
}

case object RankElementsAnnotation
    extends NoTargetAnnotation
    with DiagrammerAnnotation
    with HasShellOptions
    with Unserializable {
  val options = Seq(
    new ShellOption[Unit](
      longOption = "rank-elements",
      toAnnotationSeq = _ => Seq(RankElementsAnnotation),
      helpText = "tries to rank elements by depth from inputs"
    )
  )
}

case object ShowPrintfsAnnotation
    extends NoTargetAnnotation
    with DiagrammerAnnotation
    with HasShellOptions
    with Unserializable {
  val options = Seq(
    new ShellOption[Unit](
      longOption = "show-printfs",
      toAnnotationSeq = _ => Seq(ShowPrintfsAnnotation),
      helpText = "render printfs showing arguments"
    )
  )
}

case class DotTimeoutSecondsAnnotation(timeout: Int)
    extends DiagrammerAnnotation
    with NoTargetAnnotation
    with Unserializable

object DotTimeoutSecondsAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[Int](
      longOption = "dot-timeout-seconds",
      toAnnotationSeq = (a: Int) => Seq(DotTimeoutSecondsAnnotation(a)),
      helpText = "gives up trying to diagram a module after 7 seconds, this increases that time"
    )
  )
}

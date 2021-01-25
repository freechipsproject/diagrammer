// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage.phase

import java.io.{File, PrintWriter}

import dotvisualizer.RenderSvg
import dotvisualizer.stage.{
  DotTimeoutSecondsAnnotation,
  JustTopLevelAnnotation,
  OpenCommandAnnotation,
  SetRenderProgramAnnotation
}
import dotvisualizer.transforms.{MakeDiagramGroup, ModuleLevelDiagrammer}
import firrtl.options.{Dependency, Phase, TargetDirAnnotation}
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.{AnnotationSeq, CircuitState}

import scala.sys.process._

class GenerateDotFilePhase extends Phase {
  override def prerequisites = Seq(Dependency[OptionallyBuildTargetDirPhase])

  override def optionalPrerequisites = Seq.empty

  override def optionalPrerequisiteOf = Seq.empty

  override def invalidates(a: Phase) = false

  /** Make a simple css file that controls highlighting
    *
    * @param targetDir where to put the css
    */
  def addCss(targetDir: String): Unit = {
    val file = new File(s"$targetDir/styles.css")
    val printWriter = new PrintWriter(file)
    printWriter.println(
      """
        |.edge:hover * {
        |  stroke: #ff0000;
        |}
        |.edge:hover polygon {
        |  fill: #ff0000;
        |}
      """.stripMargin
    )
    printWriter.close()
  }

  /** Open an svg file using the open program
    *
    * @param fileName    file to be opened
    * @param openProgram program to use
    */
  def show(fileName: String, openProgram: String): Unit = {
    if (openProgram.nonEmpty && openProgram != "none") {
      val openProcessString = s"$openProgram $fileName.svg"
      openProcessString.!!
    } else {
      println(s"""There is no program identified which will render the svg files.""")
      println(s"""The file to start with is $fileName.svg, open it in the appropriate viewer""")
      println(s"""Specific module views should be in the same directory as $fileName.svg""")
    }
  }

  override def transform(annotationSeq: AnnotationSeq): AnnotationSeq = {
    val targetDir = annotationSeq.collectFirst { case TargetDirAnnotation(dir) => dir }.get
    val firrtlCircuit = annotationSeq.collectFirst { case FirrtlCircuitAnnotation(circuit) => circuit }.get
    val circuitState = CircuitState(firrtlCircuit, annotationSeq)

    addCss(targetDir)

    val dotProgram = annotationSeq.collectFirst { case SetRenderProgramAnnotation(program) =>
      program
    }.getOrElse("dot")

    val dotTimeOut = annotationSeq.collectFirst { case DotTimeoutSecondsAnnotation(secs) =>
      secs
    }.getOrElse(7)

    val renderer = new RenderSvg(dotProgram, dotTimeOut)

    if (annotationSeq.exists { case JustTopLevelAnnotation => true; case _ => false }) {
      val justTopLevelTransform = new ModuleLevelDiagrammer(renderer)
      justTopLevelTransform.execute(circuitState)
    } else {
      val transform = new MakeDiagramGroup(renderer)
      transform.execute(circuitState)
    }

    val fileName = s"$targetDir/${circuitState.circuit.main}_hierarchy.dot"
    val openProgram = annotationSeq.collectFirst { case OpenCommandAnnotation(program) =>
      program
    }.getOrElse("open")

    show(fileName, openProgram)

    annotationSeq
  }
}

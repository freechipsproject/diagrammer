// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage.phase

import java.io.File

import dotvisualizer.stage.DiagrammerException
import firrtl.options.{Dependency, Phase, TargetDirAnnotation}
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.{AnnotationSeq, FileUtils}

class OptionallyBuildTargetDirPhase extends Phase {
  override def prerequisites = Seq(Dependency[GetFirrtlCircuitPhase])

  override def optionalPrerequisites = Seq.empty

  override def optionalPrerequisiteOf = Seq.empty

  override def invalidates(a: Phase) = false

  override def transform(annotationSeq: AnnotationSeq): AnnotationSeq = {
    val newAnnotations: AnnotationSeq = if (annotationSeq.exists(_.isInstanceOf[TargetDirAnnotation])) {
      annotationSeq
    } else {
      val circuit = annotationSeq.collectFirst { case FirrtlCircuitAnnotation(circuit) => circuit }.get
      val targetDir = s"test_run_dir/${circuit.main}"
      annotationSeq :+ TargetDirAnnotation(targetDir)
    }

    newAnnotations.foreach {
      case TargetDirAnnotation(targetDir) =>
        val targetDirFile = new File(targetDir)
        if (targetDirFile.exists()) {
          if (!targetDirFile.isDirectory) {
            throw new DiagrammerException(s"Error: Target dir ${targetDir} exists and is not a directory")
          }
        } else {
          FileUtils.makeDirectory(targetDir)
          if (!targetDirFile.exists()) {
            throw new DiagrammerException(s"Error: Target dir ${targetDir} exists and is not a directory")
          }
        }
      case _ =>
    }
    newAnnotations
  }
}

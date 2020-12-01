// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage.phase

import chisel3.stage.ChiselGeneratorAnnotation
import dotvisualizer.stage.DiagrammerException
import firrtl.AnnotationSeq
import firrtl.options.{Phase, TargetDirAnnotation}
import firrtl.stage.{FirrtlFileAnnotation, FirrtlSourceAnnotation}

class CheckPhase extends Phase {

  override def transform(annotationSeq: AnnotationSeq): AnnotationSeq = {
    val sourceCount = annotationSeq.count {
      case _: FirrtlSourceAnnotation    => true
      case _: FirrtlFileAnnotation      => true
      case _: ChiselGeneratorAnnotation => true
      case _ => false
    }

    if (sourceCount > 1) {
      throw new DiagrammerException(s"Error: Only one source of firrtl should be preset, but found $sourceCount")
    } else if (sourceCount < 1) {
      throw new DiagrammerException(s"Error: Could not find firrtl source to diagram, perhaps you need -i <file name>")
    }

    val targetCount = annotationSeq.count(_.isInstanceOf[TargetDirAnnotation])
    if (targetCount > 1) {
      throw new DiagrammerException(s"Error: There can only be 1 TargetDir, $targetCount found")
    }
    annotationSeq
  }
}

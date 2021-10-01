// SPDX-License-Identifier: Apache-2.0

package dotvisualizer

import firrtl.CompilerUtils.getLoweringTransforms
import firrtl.Mappers._
import firrtl.PrimOps.Dshl
import firrtl._
import firrtl.ir._
import firrtl.options.{Dependency, Phase}
import firrtl.stage.{FirrtlCircuitAnnotation, Forms}
import firrtl.transforms.BlackBoxSourceHelper

/**
  * Use these lowering transforms to prepare circuit for compiling
  */
class ToLoFirrtl extends Phase {
  private val targets = Forms.LowFormOptimized ++ Seq(
    Dependency(passes.RemoveValidIf),
    Dependency(passes.memlib.VerilogMemDelays),
    Dependency(passes.SplitExpressions),
    Dependency[firrtl.transforms.LegalizeAndReductionsTransform],
    Dependency[firrtl.transforms.ConstantPropagation],
    Dependency[firrtl.transforms.CombineCats],
    Dependency(passes.CommonSubexpressionElimination),
    Dependency[firrtl.transforms.DeadCodeElimination]
  )

  private def compiler = new firrtl.stage.transforms.Compiler(targets, currentState = Nil)
  private val transforms = compiler.flattenedTransformOrder

  override def transform(annotationSeq: AnnotationSeq): AnnotationSeq = {

    annotationSeq.flatMap {
      case FirrtlCircuitAnnotation(circuit) =>
        val state = CircuitState(circuit, annotationSeq)
        val newState = transforms.foldLeft(state) {
          case (prevState, transform) => transform.runTransform(prevState)
        }
        Some(FirrtlCircuitAnnotation(newState.circuit))
      case other =>
        Some(other)
    }
  }
}

/**
  *  Workaround for https://github.com/freechipsproject/firrtl/issues/498 from @jackkoenig
  */
class FixupOps extends Transform with DependencyAPIMigration {
  override def prerequisites = Seq.empty

  override def optionalPrerequisites = Seq.empty

  override def optionalPrerequisiteOf = Seq.empty

  override def invalidates(a: Transform) = false

  private def onExpr(expr: Expression): Expression =
    expr.map(onExpr) match {
      case prim @ DoPrim(Dshlw, _, _, _) => prim.copy(op = Dshl)
      case other                         => other
    }
  private def onStmt(stmt: Statement): Statement = stmt.map(onStmt).map(onExpr)
  private def onMod(mod:   DefModule): DefModule = mod.map(onStmt)
  def execute(state: CircuitState): CircuitState = {
    state.copy(circuit = state.circuit.map(onMod))
  }
}

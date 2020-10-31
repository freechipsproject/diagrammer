// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.transforms

import dotvisualizer.ToLoFirrtl
import dotvisualizer.stage.DiagrammerException
import firrtl.ir._
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.{
  CircuitForm,
  CircuitState,
  DependencyAPIMigration,
  FileUtils,
  LowForm,
  Parser,
  Transform,
  WRef,
  WSubField,
  WSubIndex
}

import scala.collection.mutable

class RemoveTempWires extends Transform with DependencyAPIMigration {
  override def prerequisites = Seq.empty

  override def optionalPrerequisites = Seq.empty

  override def optionalPrerequisiteOf = Seq.empty

  override def invalidates(a: Transform) = false

  /**
    * Foreach Module in a firrtl circuit
    *   Find all the DefNodes with temp names and render their expression
    *   Remove all the found dev nodes
    *   recursively replace their references with their associated expression
    * @param state to be altered
    * @return
    */

  def execute(state: CircuitState): CircuitState = {

    val c = state.circuit

    /**
      * removes all references to temp wires in module
      * @param module the module to be altered
      * @return
      */
    def removeTempWiresFromModule(module: Module): Module = {

      val toRemove = new mutable.HashMap[String, Expression]()

      /**
        * Saves reference to the expression associated
        * with a temp wire associated with a Node statement
        * @param s statement to be checked
        */
      def collectTempExpressions(s: Statement): Unit = s match {
        case block: Block =>
          block.stmts.foreach { substatement =>
            collectTempExpressions(substatement)
          }

        case node: DefNode =>
          if (node.name.startsWith(RemoveTempWires.GenPrefix) || node.name.startsWith(RemoveTempWires.TempPrefix)) {

            if (toRemove.contains(node.name)) {
              println(s"Houston we have a problem, ${node.name} already marked for removal")
            }
            toRemove(node.name) = node.value
            None
          }
        case _ => //do nothing
      }

      /**
        * recursively find any references to temp wires in the expression and replace the
        * references with the associated expression
        * @param e expression to be altered
        * @return
        */
      def removeGen(e: Expression): Expression = {
        e match {
          case wire: WRef =>
            if (
              (wire.name.startsWith(RemoveTempWires.GenPrefix) ||
              wire.name.startsWith(RemoveTempWires.TempPrefix)) && toRemove.contains(wire.name)
            ) {
              val new_node = toRemove(wire.name)
              removeGen(new_node)
            } else {
              wire
            }
          case wire: WSubField =>
            if (
              (wire.name.startsWith(RemoveTempWires.GenPrefix) ||
              wire.name.startsWith(RemoveTempWires.TempPrefix)) && toRemove.contains(wire.name)
            ) {
              val new_node = toRemove(wire.name)
              removeGen(new_node)
            } else {
              wire
            }
          case wire: WSubIndex =>
            wire.mapExpr(removeGen)
          case ee => ee.mapExpr(removeGen)
        }
      }

      /**
        * Removes node definition statements for temp wires
        * @param s statement to be altered
        * @return
        */
      def removeGenStatement(s: Statement): Option[Statement] = {
        s match {
          case block: Block =>
            val result = Some(Block(block.stmts.flatMap { substatement =>
              removeGenStatement(substatement)
            }))
            result
          case node: DefNode =>
            if (node.name.startsWith(RemoveTempWires.GenPrefix) || node.name.startsWith(RemoveTempWires.TempPrefix)) {
              None
            } else {
              Some(node.mapExpr(removeGen))
            }
          case other: Statement =>
            Some(other.mapExpr(removeGen))
          case _ => Some(s) //do nothing
        }
      }

      collectTempExpressions(module.body)
      val moduleWithTempExpressionsRemmoved = removeGenStatement(module.body)
      module.copy(body = moduleWithTempExpressionsRemmoved.get)
    }

    val newModules = c.modules.map {
      case m: Module => removeTempWiresFromModule(m)
      case otherMod => otherMod
    }

    state.copy(circuit = Circuit(c.info, newModules, c.main))
  }
}

object RemoveTempWires {
  val GenPrefix = "_GEN_"
  val TempPrefix = "_T_"

  def main(args: Array[String]): Unit = {
    args.headOption match {
      case Some(fileName) =>
        val firrtlSource = FileUtils.getLines(fileName).mkString("\n")
        val annos = (new ToLoFirrtl).transform(Seq(FirrtlCircuitAnnotation(Parser.parse(firrtlSource))))
        val firrtl = annos.collectFirst { case FirrtlCircuitAnnotation(c) => c }.getOrElse {
          throw new DiagrammerException("Error: could not process supplied firrtl")
        }

        val grapher = new RemoveTempWires

        val newState = grapher.execute(CircuitState(firrtl, Seq.empty))
        println(s"${newState.circuit.serialize}")

      case _ =>
    }
  }
}

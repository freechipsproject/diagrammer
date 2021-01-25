// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.transforms

import dotvisualizer._
import dotvisualizer.stage.StartModuleNameAnnotation
import firrtl.options.TargetDirAnnotation
import firrtl.{CircuitForm, CircuitState, DependencyAPIMigration, LowForm, Transform}

import scala.collection.mutable

class MakeDiagramGroup(renderSvg: RenderSvg) extends Transform with DependencyAPIMigration {
  override def prerequisites = Seq.empty

  override def optionalPrerequisites = Seq.empty

  override def optionalPrerequisiteOf = Seq.empty

  override def invalidates(a: Transform) = false

  /** Creates a series of diagrams starting with the startModule and continuing
    * through all descendant sub-modules.
    * @param state the state to be diagrammed
    * @return
    */

  override def execute(state: CircuitState): CircuitState = {

    val targetDir = state.annotations.collectFirst { case TargetDirAnnotation(dir) => dir }.getOrElse {
      s"test_run_dir/${state.circuit.main}/"
    }

    val startModule = state.annotations.collectFirst { case StartModuleNameAnnotation(moduleName) =>
      moduleName
    }.getOrElse(state.circuit.main)

    val queue = new mutable.Queue[String]()
    val modulesSeen = new mutable.HashSet[String]()

    val pass_remove_gen = new RemoveTempWires()
    var cleanedState = pass_remove_gen.execute(state)

    val pass_top_level = new ModuleLevelDiagrammer(renderSvg)
    pass_top_level.execute(cleanedState)

    queue += startModule // set top level of diagram tree

    while (queue.nonEmpty) {
      val moduleName = queue.dequeue()
      if (!modulesSeen.contains(moduleName)) {

        val updatedAnnotations = {
          state.annotations.filterNot { x =>
            x.isInstanceOf[StartModuleNameAnnotation]
          } :+ StartModuleNameAnnotation(moduleName)
        }
        val stateToDiagram = CircuitState(cleanedState.circuit, state.form, updatedAnnotations)

        val pass = new MakeOneDiagram(renderSvg)
        pass.execute(stateToDiagram)

        queue ++= pass.subModulesFound.map(module => module.name)
        renderSvg.render(s"$targetDir/$moduleName.dot")
      }
      modulesSeen += moduleName
    }

    // we return the original state, all transform work is just in the interest of diagramming
    state
  }
}

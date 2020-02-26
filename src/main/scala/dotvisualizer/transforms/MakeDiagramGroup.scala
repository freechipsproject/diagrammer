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

package dotvisualizer.transforms

import dotvisualizer._
import firrtl.{CircuitForm, CircuitState, LowForm, Transform}

import scala.collection.mutable

import scala.sys.process._

class MakeDiagramGroup extends Transform {
  override def inputForm: CircuitForm = LowForm
  override def outputForm: CircuitForm = LowForm

  /**
    * Creates a series of diagrams starting with the startModule and continuing
    * through all descendant sub-modules.
    * @param state the state to be diagrammed
    * @return
    */
  //scalastyle:off method.length cyclomatic.complexity
  override def execute(state: CircuitState): CircuitState = {

    val targetDir = FirrtlDiagrammer.getTargetDir(state.annotations)

    FirrtlDiagrammer.addCss(targetDir)

    val dotProgram = state.annotations.collectFirst {
      case SetRenderProgram(program) => program
    }.getOrElse("dot")

    val openProgram = state.annotations.collectFirst {
      case SetOpenProgram(program) => program
    }.getOrElse("open")

    val startModule = state.annotations.collectFirst {
      case StartModule(moduleName) => moduleName
    }.getOrElse(state.circuit.main)

    val queue = new mutable.Queue[String]()
    val modulesSeen = new mutable.HashSet[String]()

    val pass_remove_gen = new RemoveTempWires()
    var cleanedState = pass_remove_gen.execute(state)

    val pass_top_level = new ModuleLevelDiagrammer
    pass_top_level.execute(cleanedState)

    queue += startModule // set top level of diagram tree

    while(queue.nonEmpty) {
      val moduleName = queue.dequeue()
      if (!modulesSeen.contains(moduleName)) {

        val updatedAnnotations = {
          state.annotations.filterNot { x => x.isInstanceOf[StartModule] } :+ StartModule(moduleName)
        }
        val stateToDiagram = CircuitState(cleanedState.circuit, state.form, updatedAnnotations)

        val pass = new MakeOneDiagram
        pass.execute(stateToDiagram)

        queue ++= pass.subModulesFound.map(module => module.name)
        FirrtlDiagrammer.render(s"$targetDir$moduleName.dot", dotProgram)
      }
      modulesSeen += moduleName
    }

    // we return the original state, all transform work is just in the interest of diagramming
    state
  }
}


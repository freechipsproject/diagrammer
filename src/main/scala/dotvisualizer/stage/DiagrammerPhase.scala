// SPDX-License-Identifier: Apache-2.0

package dotvisualizer.stage

import dotvisualizer.stage.phase.{
  CheckPhase,
  GenerateDotFilePhase,
  GetFirrtlCircuitPhase,
  OptionallyBuildTargetDirPhase
}
import firrtl.options.phases.DeletedWrapper
import firrtl.options.{Dependency, Phase, PhaseManager}

class DiagrammerPhase extends PhaseManager(DiagrammerPhase.targets) {

  override val wrappers = Seq((a: Phase) => DeletedWrapper(a))

}

object DiagrammerPhase {

  val targets: Seq[PhaseManager.PhaseDependency] = Seq(
    Dependency[CheckPhase],
    Dependency[GetFirrtlCircuitPhase],
    Dependency[OptionallyBuildTargetDirPhase],
    Dependency[GenerateDotFilePhase]
  )
}

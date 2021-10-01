// SPDX-License-Identifier: Apache-2.0

package dotvisualizer

import dotvisualizer.stage.DiagrammerStage
import firrtl.FileUtils.isCommandAvailable
import firrtl._

/**
  * This library implements a graphviz dot file render.
  */
object FirrtlDiagrammer {
  def main(args: Array[String]): Unit = {
    (new DiagrammerStage).execute(args, Seq.empty)
  }

  private val MacPattern = """.*mac.*""".r
  private val LinuxPattern = """.*n[iu]x.*""".r
  private val WindowsPattern = """.*win.*""".r

  def getOpenForOs: String = {
    System.getProperty("os.name").toLowerCase match {
      case MacPattern()                                                    => "open"
      case LinuxPattern() if isCommandAvailable(Seq("xdg-open", "--help")) => "xdg-open"
      case WindowsPattern()                                                => "" // no clear agreement here.
      case _                                                               => "" // punt
    }
  }

}

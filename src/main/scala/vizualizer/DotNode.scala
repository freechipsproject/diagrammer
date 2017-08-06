// See LICENSE for license details.

package vizualizer

import scala.collection.mutable.ArrayBuffer

trait DotNode {
  def render: String
  def name: String
  def parentOpt: Option[DotNode]
  def absoluteName: String = {
    parentOpt match {
      case Some(parent) => s"${parent.absoluteName}_$name"
      case _ => name
    }
  }
  val children: ArrayBuffer[DotNode] = new ArrayBuffer[DotNode]
}

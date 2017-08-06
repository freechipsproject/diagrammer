// See LICENSE for license details.

package vizualizer

import scala.collection.mutable.ArrayBuffer

trait DotNode {
  def render: String
  def children: ArrayBuffer[DotNode] = new ArrayBuffer[DotNode]
}

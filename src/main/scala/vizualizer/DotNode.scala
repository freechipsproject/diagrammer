// See LICENSE for license details.

package vizualizer

trait DotNode {
  def render: String
  def children: Seq[DotNode] = Seq.empty
}

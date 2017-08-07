// See LICENSE for license details.

package vizualizer

case class NodeNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def render: String = s"""$absoluteName [label = "$name" shape="rectangle"]; """
}

// See LICENSE for license details.

package dotvisualizer.dotnodes

case class NodeNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def render: String = s"""$absoluteName [label = "$name" shape="rectangle"]; """
}

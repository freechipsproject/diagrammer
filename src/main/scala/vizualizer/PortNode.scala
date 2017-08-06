// See LICENSE for license details.

package vizualizer

case class PortNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def render: String = {
    s"""$absoluteName [shape = circle label="$name"]
     """.stripMargin
  }
}

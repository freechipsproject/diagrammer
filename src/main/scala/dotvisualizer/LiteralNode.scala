// See LICENSE for license details.

package dotvisualizer

case class LiteralNode(name: String, value: BigInt, parentOpt: Option[DotNode]) extends DotNode {
  def render: String = {
    s"""$absoluteName [shape = circle label="$value"]
     """.stripMargin
  }
}

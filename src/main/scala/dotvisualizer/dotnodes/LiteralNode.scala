// See LICENSE for license details.

package dotvisualizer.dotnodes

case class LiteralNode(name: String, value: BigInt, parentOpt: Option[DotNode]) extends DotNode {
  def render: String = {
    s"""$absoluteName [shape="circle" style="filled" BGCOLOR="#C0C0C0" label="$value"]
     """.stripMargin
  }
}

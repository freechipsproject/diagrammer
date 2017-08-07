// See LICENSE for license details.

package vizualizer

case class PrimOpNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  override def render: String = s"""$absoluteName [label = "$name"]"""

  override def asRhs: String = absoluteName

  println(s"here I am in primops")
}

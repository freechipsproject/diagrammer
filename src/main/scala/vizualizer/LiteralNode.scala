// See LICENSE for license details.

package vizualizer

case class LiteralNode(name: String, value: BigInt) extends DotNode {
  def render: String = {
    s"""$name [shape = circle label="$value"]
     """.stripMargin
  }
}

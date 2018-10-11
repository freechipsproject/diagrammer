// See LICENSE for license details.

package dotvisualizer.dotnodes

case class PortNode(name: String, parentOpt: Option[DotNode], rank: Int=10, isInput: Boolean = false) extends DotNode {
  def render: String = {
    val color = if(! isInput) { s"#E0FFFF" } else { s"#CCCCCC"}
    s"""$absoluteName [shape = "rectangle" style="filled" fillcolor="$color" label="$name" rank="$rank"]
     """.stripMargin
  }
}

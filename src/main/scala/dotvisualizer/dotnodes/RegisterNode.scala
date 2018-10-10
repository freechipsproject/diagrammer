// See LICENSE for license details.

package dotvisualizer.dotnodes

case class RegisterNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  override val in: String = s"struct_$absoluteName:in"
  override val out: String = s"struct_$absoluteName:out"
  override val asRhs: String = s"struct_$absoluteName:out"

  def render: String = {
    s"""struct_$absoluteName [shape="plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#FFE4B5">
       |  <TR>
       |    <TD PORT="in">${PrimOpNode.BlackDot}</TD>
       |    <TD>$name</TD>
       |    <TD PORT="out">${PrimOpNode.BlackDot}</TD>
       |  </TR>
       |</TABLE>>];""".stripMargin
  }
}

// See LICENSE for license details.

package vizualizer

case class RegisterNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  override def in: String = s"struct_$absoluteName:in"
  override def out: String = s"struct_$absoluteName:out"
  override def asRhs: String = s"struct_$absoluteName:out"

  def render: String = {
    s"""struct_$absoluteName [shape="plaintext" label=<
      |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
      |  <TR>
      |    <TD PORT="in"> </TD>
      |     <TD>$name</TD>
      |    <TD PORT="out"> </TD>
      |  </TR>
      |</TABLE>>];""".stripMargin
  }
}

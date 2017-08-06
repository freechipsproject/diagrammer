// See LICENSE for license details.

package vizualizer

case class RegisterNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def in: String = s"$name:in"
  def out: String = s"$name:out"
  def render: String = {
    s"""$absoluteName [label=<
      |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
      |  <TR>
      |    <TD PORT="in"> </TD>
      |    <TD PORT="out"> </TD>
      |  </TR>
      |</TABLE>>];""".stripMargin
  }
}

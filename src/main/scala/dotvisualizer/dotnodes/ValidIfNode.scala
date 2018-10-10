// See LICENSE for license details.

package dotvisualizer.dotnodes

case class ValidIfNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  val select: String = s"$name:select"
  val in1: String = s"$name:in1"
  override val asRhs: String = s"$name:out"

  def render: String = {
    s"""
       |$name [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#FA8072">
       |  <TR>
       |    <TD PORT="in1">&#x25cf;</TD>
       |    <TD PORT="select">validIf?</TD>
       |    <TD PORT="out">&#x25cf;</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }

}

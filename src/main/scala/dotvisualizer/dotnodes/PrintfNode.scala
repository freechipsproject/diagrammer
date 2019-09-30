// See LICENSE for license details.

package dotvisualizer.dotnodes

import firrtl.WRef
import firrtl.ir.Print

import scala.collection.mutable

case class PrintfNode(name: String, formatString: String, parentOpt: Option[DotNode]) extends DotNode {

  val text = new mutable.StringBuilder()

  override def absoluteName: String = "struct_" + super.absoluteName

  text.append(
    s"""
      |$absoluteName [shape="plaintext" label=<
      |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#EA3076">
      |  <TR>
      |    <TD>printf("$formatString") </TD>
      |  </TR>
    """.stripMargin)

  def addArgument(displayName: String, connectTarget: String, connect: String): PrintfArgument = {
    val port = PrintfArgument(displayName, connect, connectTarget)
    text.append(s"      ${port.render}")
    port
  }

  def finish() {
    text.append(
      """
        |</TABLE>>];
    """.stripMargin)
  }

  def render: String = text.toString()
}

case class PrintfArgument(name: String, override val absoluteName: String, connectTarget: String) extends DotNode {
  val parentOpt : Option[DotNode] = None // doesn't need to know parent
  def render: String = {
    s"""
      |<TR><TD PORT="$connectTarget">$name</TD></TR>
    """.stripMargin
  }
}

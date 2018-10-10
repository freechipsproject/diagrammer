// See LICENSE for license details.

package dotvisualizer.dotnodes

import firrtl.ir.DefMemory

import scala.collection.mutable

case class MemNode(name: String, parentOpt: Option[DotNode],
                   firrtlName: String,
                   defMem: DefMemory, nameToNode: mutable.HashMap[String, DotNode]) extends DotNode {

  val text = new mutable.StringBuilder()

  text.append(
    s"""
      |struct_$absoluteName [shape="plaintext" label=<
      |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#DA70D6">
      |  <TR>
      |    <TD>Mem $name </TD>
      |  </TR>
    """.stripMargin)

  def addPort(memPortName: String, firrtlMemPortName: String, portName: String): Unit = {
    val firrtlName = s"$firrtlMemPortName.$portName"

    val port = MemoryPort(portName, s"struct_${this.absoluteName}:${memPortName}_$portName", memPortName)
    nameToNode(firrtlName) = port
    text.append(s"      ${port.render}")
  }

  def addMemoryPort(kind: String, memPortName:String, fields: Seq[String]): Unit = {
    val firrtlMemPortName = s"$firrtlName.$memPortName"
    text.append(
      s"""
        |<TR><TD>$kind $memPortName</TD></TR>
      """.stripMargin)

    fields.foreach { portName => addPort(memPortName, firrtlMemPortName, portName)}
  }


  defMem.readers.foreach { readerName =>
    addMemoryPort("read port", readerName, Seq("en", "addr", "data", "clk"))
  }
  defMem.writers.foreach { readerName =>
    addMemoryPort("write port", readerName, Seq("en", "addr", "data", "mask", "clk"))
  }
  defMem.readwriters.foreach { readerName =>
    addMemoryPort("write port", readerName, Seq("en", "addr", "wdata", "wmask", "wmode", "clk"))
  }

  text.append(
    """
      |</TABLE>>];
    """.stripMargin)

  def render: String = text.toString()
}

case class MemoryPort(name: String, override val absoluteName: String, memPortName: String) extends DotNode {
  val parentOpt : Option[DotNode] = None // doesn't need to know parent
  def render: String = {
    s"""
      |<TR><TD PORT="${memPortName}_$name">$name</TD></TR>
    """.stripMargin
  }
}

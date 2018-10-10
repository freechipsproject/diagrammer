// See LICENSE for license details.

package dotvisualizer.dotnodes

import java.io.{File, PrintWriter}

case class MuxNode(
  name: String,
  parentOpt: Option[DotNode],
  arg0ValueOpt: Option[String] = None,
  arg1ValueOpt: Option[String] = None
) extends DotNode {

  val select: String = s"$name:select"
  val in1: String = s"$name:in1"
  val in2: String = s"$name:in2"
  override val asRhs: String = s"$name:out"

  def render: String = {
    s"""
       |$name [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#AFEEEE">
       |  <TR>
       |    <TD PORT="in1">${arg0ValueOpt.getOrElse(PrimOpNode.BlackDot)}</TD>
       |    <TD ROWSPAN="2" PORT="select">a?</TD>
       |    <TD ROWSPAN="2" PORT="out">&#x25cf;</TD>
       |  </TR>
       |  <TR>
       |    <TD PORT="in2">${arg1ValueOpt.getOrElse(PrimOpNode.BlackDot)}</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }

}

import scala.sys.process._

object MuxNode {
  //noinspection ScalaStyle
  def main(args: Array[String]): Unit = {
    val fox = LiteralNode("fox", BigInt(1), None)
    val dog = LiteralNode("dog", BigInt(5), None)
    val cat = LiteralNode("cat", BigInt(2), None)
    val reg1 = RegisterNode("reg1", None)
    val mux1 = MuxNode("struct1", None)
    val mux2 = MuxNode("struct2", None)
    val writer = new PrintWriter(new File("mux1.dot"))
    writer.println(s"digraph structs {")
    writer.println(s"graph [splines=ortho]")
    writer.println(s"node [shape=plaintext]")
    writer.println(fox.render)
    writer.println(dog.render)
    writer.println(cat.render)
    writer.println(reg1.render)
    writer.write(mux1.render)
    writer.write(mux2.render)

    writer.println(s"${fox.name} -> ${mux1.select};")
    writer.println(s"${dog.name} -> ${mux1.in1};")
    writer.println(s"${cat.name} -> ${mux1.in2};")
    writer.println(s"\n${mux1.out} -> ${mux2.in1}")
    writer.println(s"\n${mux2.out} -> ${reg1.in}")
    writer.println(s"\n${reg1.out} -> ${mux2.in2}")


    writer.println(s"}")

    writer.close()

    "dot -Tpng -O mux1.dot".!!
    "open mux1.dot.png".!!
  }
}

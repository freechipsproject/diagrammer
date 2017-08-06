// See LICENSE for license details.

package vizualizer

import java.io.{File, PrintWriter}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ModuleNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  val inputs: ArrayBuffer[DotNode] = new ArrayBuffer()
  val outputs: ArrayBuffer[DotNode] = new ArrayBuffer()
  val localConnections: mutable.HashMap[String, String] = new mutable.HashMap()

  def render: String = {
    s"""
       |subgraph cluster_$absoluteName {
       |  label="$name"
       |  ${inputs.map(_.render).mkString("\n")}
       |  ${outputs.map(_.render).mkString("\n")}
       |  ${children.map(_.render).mkString("\n")}
       |
       |  ${localConnections.map { case (k, v) => s"$v -> $k"}.mkString("\n")}
       |}
     """.stripMargin
  }

  override def absoluteName: String = {
    val baseName = parentOpt match {
      case Some(parent) => s"${parent.absoluteName}_$name"
      case _ => name
    }
    s"cluster_$baseName"
  }

}

import sys.process._

object ModuleNode {
  def main(args: Array[String]): Unit = {
    val topModule = ModuleNode("top", parentOpt = None)

    val fox = LiteralNode("fox", BigInt(1), Some(topModule))
    val dog = LiteralNode("dog", BigInt(5), Some(topModule))
    val cat = LiteralNode("cat", BigInt(2), Some(topModule))

    val reg1 = RegisterNode("reg1", Some(topModule))
    val mux1 = MuxNode("mux1", Some(topModule))
    val mux2 = MuxNode("mux2", Some(topModule))

    topModule.inputs += PortNode("in1", Some(topModule))
    topModule.inputs += PortNode("in2", Some(topModule))

    val subModule = ModuleNode("child", Some(topModule))

    subModule.inputs += PortNode("cluster_in1", Some(topModule))
    subModule.inputs += PortNode("in2", Some(topModule))

    topModule.children += fox
    topModule.children += dog
    topModule.children += mux1
    topModule.children += mux2
    topModule.children += subModule

    topModule.localConnections(mux1.in1) = fox.absoluteName
    topModule.localConnections(mux2.in1) = dog.absoluteName

    topModule.localConnections(s"cluster_top_cluster_in1") = topModule.inputs(0).absoluteName

    val writer = new PrintWriter(new File("module1.dot"))
    writer.println(s"digraph structs {")
//    writer.println(s"graph [splines=ortho]")
    writer.println(s"node [shape=plaintext]")
    writer.println(topModule.render)


    writer.println(s"}")

    writer.close()

    "dot -Tpng -O module1.dot".!!
    "open module1.dot.png".!!
  }
}




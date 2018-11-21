// See LICENSE for license details.

package dotvisualizer.dotnodes

import java.io.{File, PrintWriter}

import dotvisualizer.transforms.MakeOneDiagram

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ModuleNode(
  name: String,
  parentOpt: Option[DotNode],
  var url_string: Option[String]= None,
  subModuleDepth: Int = 0
) extends DotNode {

  val inputs: ArrayBuffer[DotNode] = new ArrayBuffer()
  val outputs: ArrayBuffer[DotNode] = new ArrayBuffer()
  val namedNodes: mutable.HashMap[String, DotNode] = new mutable.HashMap()
  val connections: mutable.HashMap[String, String] = new mutable.HashMap()
  private val analogConnections = new mutable.HashMap[String, ArrayBuffer[String]]() {
    override def default(key: String): ArrayBuffer[String] = {
      this(key) = new ArrayBuffer[String]()
      this(key)
    }
  }
  val localConnections: mutable.HashMap[String, String] = new mutable.HashMap()

  val backgroundColorIndex: Int = subModuleDepth % MakeOneDiagram.subModuleColorsByDepth.length
  val backgroundColor: String = MakeOneDiagram.subModuleColorsByDepth(backgroundColorIndex)

  def render: String = {
    def expandBiConnects(target: String, sources: ArrayBuffer[String]): String = {
      sources.map { vv => s"""$vv -> $target [dir = "both"]"""  }.mkString("\n")
    }

    val s = s"""
       |subgraph $absoluteName {
       |  label="$name"
       |  URL="${url_string.getOrElse("")}"
       |  bgcolor="$backgroundColor"
       |  ${inputs.map(_.render).mkString("\n")}
       |  ${outputs.map(_.render).mkString("\n")}
       |  ${children.map(_.render).mkString("\n")}
       |
       |  ${connections.map { case (k, v) => s"$v -> $k"}.mkString("\n")}
       |  ${analogConnections.map { case (k, v) => expandBiConnects(k, v) }.mkString("\n")}
       |}
     """.stripMargin
    s
  }

  override def absoluteName: String = {
    parentOpt match {
      case Some(parent) => s"${parent.absoluteName}_$name"
      case _ => s"cluster_$name"
    }
  }

  def connect(destination: DotNode, source: DotNode): Unit = {
    connect(destination.absoluteName, source.absoluteName)
  }

  def connect(destinationName: String, source: DotNode): Unit = {
    connect(destinationName, source.absoluteName)
  }

  def connect(destination: DotNode, sourceName: String): Unit = {
    connect(destination.absoluteName, sourceName)
  }

  def connect(destination: String, source: String, edgeLabel: String = ""): Unit = {
    connections(destination) = source
  }

  def analogConnect(destination: String, source: String, edgeLabel: String = ""): Unit = {
    analogConnections(destination) += source
  }

  //scalastyle:off method.name
  def += (childNode: DotNode): Unit = {
    namedNodes(childNode.name) = childNode
    children += childNode
  }
}

import scala.sys.process._

//noinspection ScalaStyle
object ModuleNode {
  //noinspection ScalaStyle
  def main(args: Array[String]): Unit = {
    val topModule = ModuleNode("top", parentOpt = None)

    val fox = LiteralNode("fox", BigInt(1), Some(topModule))
    val dog = LiteralNode("dog", BigInt(5), Some(topModule))

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
    writer.println(s"graph [splines=ortho]")
    writer.println(s"node [shape=plaintext]")
    writer.println(topModule.render)


    writer.println(s"}")

    writer.close()

    "fdp -Tpng -O module1.dot".!!
    "open module1.dot.png".!!
  }
}




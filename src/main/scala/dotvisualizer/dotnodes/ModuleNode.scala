// See LICENSE for license details.

package dotvisualizer.dotnodes

import dotvisualizer.transforms.MakeOneDiagram
import firrtl.graph.DiGraph

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ModuleNode(
  name: String,
  parentOpt: Option[DotNode],
  var url_string: Option[String]= None,
  subModuleDepth: Int = 0
) extends DotNode {

  var renderWithRank: Boolean = false

  val namedNodes: mutable.HashMap[String, DotNode] = new mutable.HashMap()
  val subModuleNames: mutable.HashSet[String] = new mutable.HashSet[String]()

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

  //scalastyle:off method.length cyclomatic.complexity
  def constructRankDirectives: String = {
    val inputNames = children.collect { case p: PortNode if p.isInput => p }.map(_.absoluteName)
    val outputPorts = children.collect { case p: PortNode if ! p.isInput => p }.map(_.absoluteName)

    val diGraph = {
      val linkedHashMap = new mutable.LinkedHashMap[String, mutable.LinkedHashSet[String]] {
        override def default(key: String): mutable.LinkedHashSet[String] = {
          this(key) = new mutable.LinkedHashSet[String]
          this(key)
        }
      }

      val connectionTargetNames = connections.values.map(_.split(":").head).toSet

      connections.foreach { case (rhs, lhs) =>
        val source = lhs.split(":").head
        val target = rhs.split(":").head

        if(target.nonEmpty && connectionTargetNames.contains(target)) {
          linkedHashMap(source) += target
          linkedHashMap(target)
        }
      }
      DiGraph(linkedHashMap)
    }

    val sources = diGraph.findSources.filter(inputNames.contains).toSeq

    def getRankedNodes: mutable.ArrayBuffer[Seq[String]] = {
      val alreadyVisited = new mutable.HashSet[String]()
      val rankNodes = new mutable.ArrayBuffer[Seq[String]]()

      def walkByRank(nodes: Seq[String], rankNumber: Int = 0): Unit = {
        rankNodes.append(nodes)

        alreadyVisited ++= nodes

        val nextNodes = nodes.flatMap { node =>
          diGraph.getEdges(node)
        }.filterNot(alreadyVisited.contains).distinct

        if(nextNodes.nonEmpty) {
          walkByRank(nextNodes, rankNumber + 1)
        }
      }

      walkByRank(sources)
      rankNodes
    }

    val rankedNodes = getRankedNodes

    val rankInfo = rankedNodes.map {
      nodesAtRank => s"""{ rank=same; ${nodesAtRank.mkString(" ")} };"""
    }.mkString("", "\n  ", "")

    rankInfo + "\n  " + s"""{ rank=same; ${outputPorts.mkString(" ")} };"""
  }

  /**
    * Renders this node
    * @return
    */
  def render: String = {
    def expandBiConnects(target: String, sources: ArrayBuffer[String]): String = {
      sources.map { vv => s"""$vv -> $target [dir = "both"]"""  }.mkString("\n")
    }

    val rankInfo = if(renderWithRank) constructRankDirectives else ""

    val s = s"""
       |subgraph $absoluteName {
       |  label="$name"
       |  URL="${url_string.getOrElse("")}"
       |  bgcolor="$backgroundColor"
       |  ${children.map(_.render).mkString("\n")}
       |
       |  ${connections.map { case (k, v) => s"$v -> $k"}.mkString("", "\n  ", "")}
       |  ${analogConnections.map { case (k, v) => expandBiConnects(k, v) }.mkString("", "\n  ", "")}
       |  $rankInfo
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
    namedNodes(childNode.absoluteName) = childNode
    children += childNode
  }
}

// See LICENSE for license details.

package vizualizer

import java.io.PrintWriter

import chisel3.experimental.ChiselAnnotation
import chisel3.internal.InstanceId
import firrtl._
import firrtl.annotations.{Annotation, Named}
import firrtl.ir._
import firrtl.passes.Pass

import scala.collection.mutable

//scalastyle:off magic.number
/**
  * This and the Visualizer transform class are a highly schematic implementation of a
  * library implementation of   (i.e. code outside of firrtl itself)
  */
object VisualizerAnnotation {
  def apply(target: Named, value: String): Annotation = Annotation(target, classOf[VisualizerTransform], value)

  def unapply(a: Annotation): Option[(Named, String)] = a match {
    case Annotation(named, t, value) if t == classOf[VisualizerTransform] => Some((named, value))
    case _ => None
  }
}

//noinspection ScalaStyle
object VizualizerPass extends Pass {
  case class DotNode(firrtlName: String)

  def run (c:Circuit) : Circuit = {
    var indentLevel = 0
    def indent: String = " " * indentLevel
    println(s"We are in ${new java.io.File(".").getAbsolutePath}")
    val printFile = new PrintWriter(new java.io.File("viz1.dot"))
    def pl(s: String): Unit = {
      printFile.println(s.split("\n").mkString(indent, s"\n$indent", ""))
    }

    def code(a: AnyRef): String = {
      s"node${a.hashCode().abs}"
    }

    val connections = new mutable.HashMap[String, String]
    val refsToName = new mutable.HashMap[WRef, String]

    /**
      * finds the specified module name in the circuit
      *
      * @param moduleName name to find
      * @param circuit circuit being analyzed
      * @return the circuit, exception occurs in not found
      */
    def findModule(moduleName: String, circuit: Circuit): DefModule = {
      circuit.modules.find(module => module.name == moduleName) match {
        case Some(module: firrtl.ir.Module) =>
          module
        case Some(externalModule: DefModule) =>
          externalModule
        case _ =>
          throw new Exception(s"Could not find top level module in $moduleName")
      }
    }

    def processModule(modulePrefix: String, myModule: DefModule): Unit = {
      def expand(name: String): String = if(modulePrefix.isEmpty) name else modulePrefix + "_" + name

      indentLevel += 1
      pl(s"subgraph cluster${code(myModule)} {")
      indentLevel += 1
      pl(s"""label = "${myModule.name}";""")

      def renameExpression(expression: firrtl.ir.Expression): String = {
        val result = expression match {
          case mux: firrtl.ir.Mux =>
            val muxName = code(mux)
            val labelA :: labelB :: labelS :: labelC :: Nil = List("A", "B", "S", "C").map(tag => s"$muxName$tag")
            pl(s"""struct${muxName} [label="{<$labelA>} A | <$labelB> B } | <$labelS> A? | <$labelC C"] """)

            //TODO: Chick figure out mux design
            //            connections(labelA) = renameExpression(mux.tval)
            //            connections(labelB) = renameExpression(mux.fval)
            //            connections(labelS) = renameExpression(mux.cond)
            labelC
          case WRef(name, tpe, kind, gender) =>
            expand(name)
          case WSubField(subExpression, name, tpe, gender) =>
            renameExpression(subExpression)
          case WSubIndex(subExpression, value, tpe, gender) =>
            renameExpression(subExpression)
          case ValidIf(condition, value, tpe) =>
            ""
          case DoPrim(op, args, const, tpe) =>
            ""
          case c: UIntLiteral =>
            s"${c.value}"
          case c: SIntLiteral =>
            s"${c.value}"
          case _ =>
            throw new Exception(s"renameExpression:error: unhandled expression $expression")
        }
        result
      }

      def processPorts(module: DefModule): Unit = {
        def makeName(s: String): String = if(modulePrefix.isEmpty) s else s"${modulePrefix}_${s}"

        def showPorts(isInput: Boolean): Unit = {
          val (dir, subGraphName) = if(isInput) {
            (firrtl.ir.Input, "io_in")
          }
          else {
            (firrtl.ir.Output, "io_out")
          }

          val ports = module.ports.flatMap {
            case port if port.direction == dir =>
              Some(makeName(port.name))
            case _ => None
          }
          if(ports.nonEmpty) {
            indentLevel += 1
            pl(s"subgraph cluster${myModule.name}.inputs {")
            indentLevel += 1
            ports.foreach { port =>
              pl(s"""${code(port)} [label = "$port"]""")
            }
            indentLevel -= 1
            pl(s"}")
            indentLevel -= 1
          }
        }

        showPorts(isInput = true)
        showPorts(isInput = false)
      }

      def processStatement(modulePrefix: String, s: Statement): Statement = {
        s match {
          case block: Block =>
            block.stmts.map { subStatement =>
              processStatement(modulePrefix, subStatement)
            }
            block
          case con: Connect =>
            con.loc match {
              case WRef(name, _, _, _) =>
                connections(code(name)) = code(renameExpression(con.expr))
              case (_: WSubField | _: WSubIndex) =>
                connections(expand(con.loc.serialize)) = renameExpression(con.expr)
            }
            c
          case WDefInstance(info, instanceName, moduleName, _) =>
            val subModule = findModule(moduleName, c)
            val newPrefix = if(modulePrefix.isEmpty) instanceName else modulePrefix + "_" + instanceName
            // log(s"declaration:WDefInstance:$instanceName:$moduleName prefix now $newPrefix")
            processModule(newPrefix, subModule)
            s

          case DefNode(info, name, expression) =>
            val expandedName = expand(name)
          case _ =>
          // let everything else slide
        }
        s
      }

      myModule match {
        case module: firrtl.ir.Module =>
          processPorts(myModule)
          processStatement(s"${modulePrefix}_${module.name}", module.body)
        case extModule: ExtModule =>
        case a =>
          println(s"got a $a")
      }
      indentLevel -= 1
      pl("}")
      indentLevel -= 1
    }

    c.modules.find(_.name == c.main) match {
      case Some(topModule) =>
        pl(s"digraph ${topModule.name} {")
        processModule("", topModule)
        connections.foreach { case (lhs, rhs) =>
          pl(s"$lhs -> $rhs")
        }
        pl("}")
      case _ =>
        println(s"could not find top module ${c.main}")
    }

    printFile.close()
    c
  }
}

class VisualizerTransform extends Transform {
  override def inputForm: CircuitForm = LowForm

  override def outputForm: CircuitForm = LowForm

  override def execute(state: CircuitState): CircuitState = {
    getMyAnnotations(state) match {
      case Nil => state
      case myAnnotations =>
        println(s"in transform $state")
        VizualizerPass.run(state.circuit)
        state
    }
  }
}

trait VisualizerAnnotator {
  self: chisel3.Module =>
  def visualize(component: InstanceId, value: String): Unit = {
    annotate(ChiselAnnotation(component, classOf[VisualizerTransform], value))
  }
}

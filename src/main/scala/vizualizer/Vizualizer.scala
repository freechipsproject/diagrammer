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

import sys.process._

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
  def run (c:Circuit) : Circuit = {
    var indentLevel = 0
    def indent: String = " " * indentLevel
    println(s"We are in ${new java.io.File(".").getAbsolutePath}")
    val printFile = new PrintWriter(new java.io.File(s"${c.main}.dot"))
    def pl(s: String): Unit = {
      printFile.println(s.split("\n").mkString(indent, s"\n$indent", ""))
    }

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

    def processModule(modulePrefix: String, myModule: DefModule, moduleNode: ModuleNode): DotNode = {
      def expand(name: String): String = {
        s"${moduleNode.absoluteName}_$name".replaceAll("""\.""", "_")
      }

      def processExpression(expression: firrtl.ir.Expression): String = {
        val result = expression match {
          case mux: firrtl.ir.Mux =>
            val muxNode = MuxNode(s"mux_${mux.hashCode()}", Some(moduleNode))
            moduleNode += muxNode
            moduleNode.connect(muxNode.select, processExpression(mux.cond))
            moduleNode.connect(muxNode.in1, processExpression(mux.tval))
            moduleNode.connect(muxNode.in2, processExpression(mux.fval))
            muxNode.out
          case WRef(name, tpe, kind, gender) =>
            s"${moduleNode.absoluteName}_$name"
          case subfield: WSubField =>
            expand(subfield.serialize)
          case subindex: WSubIndex =>
            expand(subindex.serialize)
          case ValidIf(condition, value, tpe) =>
            ""
          case DoPrim(op, args, const, tpe) =>
            ""
          case c: UIntLiteral =>
            val uInt = LiteralNode(s"${c.hashCode}", c.value, Some(moduleNode))
            moduleNode += uInt
            uInt.absoluteName
          case c: SIntLiteral =>
            val uInt = LiteralNode(s"${c.hashCode}", c.value, Some(moduleNode))
            moduleNode += uInt
            uInt.absoluteName
          case _ =>
            throw new Exception(s"renameExpression:error: unhandled expression $expression")
        }
        result
      }

      def processPorts(module: DefModule): Unit = {
        def makeName(s: String): String = s"${moduleNode.absoluteName}_$s"

        def showPorts(isInput: Boolean): Unit = {
          val (dir, subGraphName) = if(isInput) {
            (firrtl.ir.Input, "io_in")
          }
          else {
            (firrtl.ir.Output, "io_out")
          }

          val ports = module.ports.flatMap {
            case port if port.direction == dir =>
              Some(PortNode(port.name, Some(moduleNode)))
            case _ => None
          }
          ports.foreach { port =>
            moduleNode += port
          }
        }

        showPorts(isInput = true)
        showPorts(isInput = false)
      }

      def processStatement(s: Statement): Unit = {
        s match {
          case block: Block =>
            block.stmts.foreach { subStatement =>
              println(s"processing substatement $subStatement")
              processStatement(subStatement)
            }
          case con: Connect =>
            con.loc match {
              case WRef(name, _, _, _) =>
                moduleNode.connect(expand(name), processExpression(con.expr))
              case (_: WSubField | _: WSubIndex) =>
                moduleNode.connect(expand(con.loc.serialize), processExpression(con.expr))
            }
          case WDefInstance(info, instanceName, moduleName, _) =>
            val subModule = findModule(moduleName, c)
            val newPrefix = if(modulePrefix.isEmpty) instanceName else modulePrefix + "_" + instanceName
            val subModuleNode = ModuleNode(instanceName, Some(moduleNode))
            moduleNode += subModuleNode
            // log(s"declaration:WDefInstance:$instanceName:$moduleName prefix now $newPrefix")
            processModule(newPrefix, subModule, subModuleNode)

          case DefNode(info, name, expression) =>
            val expandedName = expand(name)
          case reg: DefRegister =>
            val regNode = RegisterNode(reg.name, Some(moduleNode))
            moduleNode += regNode
          case _ =>
          // let everything else slide
        }
      }

      myModule match {
        case module: firrtl.ir.Module =>
          processPorts(myModule)
          processStatement(module.body)
        case extModule: ExtModule =>
        case a =>
          println(s"got a $a")
      }

      moduleNode
    }

    c.modules.find(_.name == c.main) match {
      case Some(topModule) =>
        pl(s"digraph ${topModule.name} {")
        val topModuleNode = ModuleNode(c.main, parentOpt = None)
        processModule("", topModule, topModuleNode)
        pl(topModuleNode.render)
        pl("}")
      case _ =>
        println(s"could not find top module ${c.main}")
    }

    printFile.close()

    s"dot -Tpng -O ${c.main}.dot".!!
    s"open ${c.main}.dot.png".!!
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

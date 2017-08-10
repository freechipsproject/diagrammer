// See LICENSE for license details.

package dotvisualizer

import java.io.PrintWriter

import chisel3.experimental.ChiselAnnotation
import chisel3.internal.InstanceId
import firrtl.PrimOps._
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
class VisualizerPass(annotations: Seq[Annotation]) extends Pass {
  def run (c:Circuit) : Circuit = {
    val nameToNode: mutable.HashMap[String, DotNode] = new mutable.HashMap()

    val printFile = new PrintWriter(new java.io.File(s"${c.main}.dot"))
    def pl(s: String): Unit = {
      printFile.println(s.split("\n").mkString("\n"))
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
      def firrtlName(name: String): String = {
        if(modulePrefix.isEmpty) name else modulePrefix + "." + name
      }

      def expand(name: String): String = {
        s"${moduleNode.absoluteName}_$name".replaceAll("""\.""", "_")
      }

      def processPrimOp(primOp: DoPrim): String = {
        def addBinOpNode(symbol: String): String = {
          val opNode = BinaryOpNode(symbol, Some(moduleNode))
          moduleNode += opNode
          moduleNode.connect(opNode.in1, processExpression(primOp.args.head))
          moduleNode.connect(opNode.in2, processExpression(primOp.args.tail.head))
          opNode.asRhs
        }

        def addUnaryOpNode(symbol: String): String = {
          val opNode = UnaryOpNode(symbol, Some(moduleNode))
          moduleNode += opNode
          moduleNode.connect(opNode.in1, processExpression(primOp.args.head))
          opNode.asRhs
        }

        def addOneArgOneParamOpNode(symbol: String): String = {
          val opNode = OneArgOneParamOpNode(symbol, Some(moduleNode), primOp.consts.head)
          moduleNode += opNode
          moduleNode.connect(opNode.in1, processExpression(primOp.args.head))
          opNode.asRhs
        }

        primOp.op match {
          case Add => addBinOpNode("add")
          case Sub => addBinOpNode("sub")
          case Mul => addBinOpNode("mul")
          case Div => addBinOpNode("div")
          case Rem => addBinOpNode("rem")

          case Eq  => addBinOpNode("eq")
          case Neq => addBinOpNode("neq")
          case Lt  => addBinOpNode("lt")
          case Leq => addBinOpNode("lte")
          case Gt  => addBinOpNode("gt")
          case Geq => addBinOpNode("gte")

          case Pad => addUnaryOpNode("pad")

          case AsUInt => addUnaryOpNode("asUInt")
          case AsSInt => addUnaryOpNode("asSInt")

          case Shl => addOneArgOneParamOpNode("shl")
          case Shr => addOneArgOneParamOpNode("shr")

          case Dshl => addBinOpNode("dshl")
          case Dshr => addBinOpNode("dshr")

          case Cvt => addUnaryOpNode("cvt")
          case Neg => addUnaryOpNode("neg")
          case Not => addUnaryOpNode("not")

          case And => addBinOpNode("and")
          case Or  => addBinOpNode("or")
          case Xor => addBinOpNode("xor")

          case Andr => addUnaryOpNode("andr")
          case Orr  => addUnaryOpNode("orr")
          case Xorr => addUnaryOpNode("xorr")

          case Cat => addBinOpNode("cat")

          case Bits =>
            val opNode = OneArgTwoParamOpNode("bits", Some(moduleNode), primOp.consts.head, primOp.consts.tail.head)
            moduleNode += opNode
            moduleNode.connect(opNode.in1, processExpression(primOp.args.head))
            opNode.asRhs


          case Head => addOneArgOneParamOpNode("head")
          case Tail => addOneArgOneParamOpNode("tail")

          case _ =>
            "dummy"
        }
      }

      def processExpression(expression: firrtl.ir.Expression): String = {
        def resolveRef(firrtlName: String, dotName: String): String = {
          nameToNode.get(firrtlName) match {
            case Some(node) =>
              node.asRhs
            case _ => dotName
          }
        }
        val result = expression match {
          case mux: firrtl.ir.Mux =>
            val muxNode = MuxNode(s"mux_${mux.hashCode().abs}", Some(moduleNode))
            moduleNode += muxNode
            moduleNode.connect(muxNode.select, processExpression(mux.cond))
            moduleNode.connect(muxNode.in1, processExpression(mux.tval))
            moduleNode.connect(muxNode.in2, processExpression(mux.fval))
            muxNode.asRhs
          case WRef(name, _, _, _) =>
            resolveRef(firrtlName(name), expand(name))
          case subfield: WSubField =>
            resolveRef(firrtlName(subfield.serialize), expand(subfield.serialize))
          case subindex: WSubIndex =>
            resolveRef(firrtlName(subindex.serialize), expand(subindex.serialize))
          case validIf : ValidIf =>
            val validIfNode = ValidIfNode(s"validif_${validIf.hashCode().abs}", Some(moduleNode))
            moduleNode += validIfNode
            moduleNode.connect(validIfNode.select, processExpression(validIf.cond))
            moduleNode.connect(validIfNode.in1, processExpression(validIf.value))
            validIfNode.asRhs
          case primOp: DoPrim =>
            processPrimOp(primOp)
          case c: UIntLiteral =>
            val uInt = LiteralNode(s"lit${PrimOpNode.hash}", c.value, Some(moduleNode))
            moduleNode += uInt
            uInt.absoluteName
          case c: SIntLiteral =>
            val uInt = LiteralNode(s"lit${PrimOpNode.hash}", c.value, Some(moduleNode))
            moduleNode += uInt
            uInt.absoluteName
          case _ =>
            throw new Exception(s"renameExpression:error: unhandled expression $expression")
        }
        result
      }

      def processPorts(module: DefModule): Unit = {
        def showPorts(dir: firrtl.ir.Direction): Unit = {
          module.ports.foreach {
            case port if port.direction == dir =>
              val portNode = PortNode(port.name, Some(moduleNode))
              nameToNode(firrtlName(port.name)) = portNode
              moduleNode += portNode
            case _ => None
          }

        }

        showPorts(firrtl.ir.Input)
        showPorts(firrtl.ir.Output)
      }

      def processMemory(memory: DefMemory): Unit = {
        val fname = firrtlName(memory.name)
        val dotName = expand(memory.name)

        val memNode = MemNode(memory.name, Some(moduleNode), fname, memory, nameToNode)
        moduleNode += memNode
      }

      def processStatement(s: Statement): Unit = {
        s match {
          case block: Block =>
            block.stmts.foreach { subStatement =>
              println(s"processing substatement $subStatement")
              processStatement(subStatement)
            }
          case con: Connect =>
            val (fName, dotName) = con.loc match {
              case WRef(name, _, _, _) => (firrtlName(name), expand(name))
              case s: WSubField =>
                (firrtlName(s.serialize), expand(s.serialize))
              case s: WSubIndex => (firrtlName(s.serialize), expand(s.serialize))
              case _ => ("badName","badName")
            }
            val lhsName = nameToNode.get(fName) match {
              case Some(regNode: RegisterNode) => regNode.in
              case Some(memPort: MemoryPort) => memPort.absoluteName
              case _ => dotName
            }
            moduleNode.connect(lhsName, processExpression(con.expr))
          case WDefInstance(_, instanceName, moduleName, _) =>
            val subModule = findModule(moduleName, c)
            val newPrefix = if(modulePrefix.isEmpty) instanceName else modulePrefix + "." + instanceName
            val subModuleNode = ModuleNode(instanceName, Some(moduleNode))
            moduleNode += subModuleNode
            processModule(newPrefix, subModule, subModuleNode)

          case DefNode(_, name, expression) =>
            val fName = firrtlName(name)
            val nodeNode = NodeNode(name, Some(moduleNode))
            moduleNode += nodeNode
            nameToNode(fName) = nodeNode
            moduleNode.connect(expand(name), processExpression(expression))
          case DefWire(_, name, _) =>
            val fName = firrtlName(name)
            val nodeNode = NodeNode(name, Some(moduleNode))
            nameToNode(fName) = nodeNode
          case reg: DefRegister =>
            val regNode = RegisterNode(reg.name, Some(moduleNode))
            nameToNode(firrtlName(reg.name)) = regNode
            moduleNode += regNode
          case memory: DefMemory =>
            processMemory(memory)
          case _ =>
          // let everything else slide
        }
      }

      myModule match {
        case module: firrtl.ir.Module =>
          processPorts(myModule)
          processStatement(module.body)
        case extModule: ExtModule =>
          processPorts(extModule)
        case a =>
          println(s"got a $a")
      }

      moduleNode
    }

    c.modules.find(_.name == c.main) match {
      case Some(topModule) =>
        pl(s"digraph ${topModule.name} {")
//        pl(s"graph [splines=ortho];")
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
        new VisualizerPass(myAnnotations).run(state.circuit)
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

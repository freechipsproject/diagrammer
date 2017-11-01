// See LICENSE for license details.

package dotvisualizer

import java.io.PrintWriter

import chisel3.experimental.ChiselAnnotation
import firrtl.PrimOps._
import firrtl._
import firrtl.annotations._
import firrtl.ir._
import firrtl.passes.Pass

import scala.collection.mutable
import sys.process._

//TODO: Chick: Allow specifying where to write dot file
//TODO: Chick: Allow way to specify renderer dot and fdp have been tested
//TODO: Chick: Allow way to specify a render depth, i.e. only render so many levels of sub modules beneath start module
//TODO: Chick: Allow way to suppress or minimize display of intermediate _T nodes
//TODO: Chick: Consider mergining constants in to muxes and primops, rather than wiring in a node.
//TODO: Chick: Must be more than above

//scalastyle:off magic.number
/**
  * This library implements a graphviz dot file render.  The annotation can specify at what module to
  * start the rendering process.  value will eventually be modified to allow some options in rendering
  */
object VisualizerAnnotation {
  def apply(target: Named, depth: Int = 0): Annotation = {
    Annotation(target, classOf[VisualizerTransform], s"${Visualizer.DepthString}=$depth")
  }
  def setDotProgram(program: String): Annotation = {
    Annotation(CircuitTopName, classOf[VisualizerTransform], s"${Visualizer.DotProgramString}=$program")
  }
  def setOpenProgram(program: String): Annotation = {
    Annotation(CircuitTopName, classOf[VisualizerTransform], s"${Visualizer.OpenProgramString}=$program")
  }

  def unapply(a: Annotation): Option[(Named, String)] = a match {
    case Annotation(named, t, value) if t == classOf[VisualizerTransform] => Some((named, value))
    case _ => None
  }
}

/**
  * Add this trait to a module to allow user to specify that the module or a submodule should be
  * rendered
  */
trait VisualizerAnnotator {
  self: chisel3.Module =>
  def visualize(component: chisel3.Module, depth: Int = 0): Unit = {
    annotate(ChiselAnnotation(component, classOf[VisualizerTransform], s"${Visualizer.DepthString}=$depth"))
  }
  def setDotProgram(program: String): Unit = {
    annotate(ChiselAnnotation(self, classOf[VisualizerTransform], s"${Visualizer.DotProgramString}=$program"))
  }
  def setOpenProgram(program: String): Unit = {
    annotate(ChiselAnnotation(self, classOf[VisualizerTransform], s"${Visualizer.OpenProgramString}=$program"))
  }
}

/**
  * Annotations specify where to start rendering.  Currently the first encountered module that matches an annotation
  * will start the rendering, rendering continues through all submodules.  It would be nice to allow a depth
  * specification. This pass is intermixed with other low to low transforms, it is not treated as a separate
  * emit, so if so annotated it will run with every firrtl compilation.
  *
  * @param annotations  where to start rendering
  */
//noinspection ScalaStyle
class VisualizerPass(val annotations: Seq[Annotation]) extends Pass {
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

    var isInScope = false

    /**
      * If rendering started, construct a graph inside moduleNode
      * @param modulePrefix the path to this node
      * @param myModule     the firrtl module currently being parsed
      * @param moduleNode   a node renderable to dot notation constructed from myModule
      * @return
      */
    def processModule(modulePrefix: String, myModule: DefModule, moduleNode: ModuleNode): DotNode = {
      /**
        * Half the battle here is matching references between firrtl full name for an element and
        * dot's reference to a connectable module
        * Following functions compute the two kinds of name
        */

      /** get firrtl's version, usually has dot's as separators
        * @param name components name
        * @return
        */
      def getFirrtlName(name: String): String = {
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
            resolveRef(getFirrtlName(name), expand(name))
          case Reference(name, _) =>
            resolveRef(getFirrtlName(name), expand(name))
          case subfield: WSubField =>
            resolveRef(getFirrtlName(subfield.serialize), expand(subfield.serialize))
          case subindex: WSubIndex =>
            resolveRef(getFirrtlName(subindex.serialize), expand(subindex.serialize))
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
          case other =>
            // throw new Exception(s"renameExpression:error: unhandled expression $expression")
            other.getClass.getName
            ""
        }
        result
      }

      def processPorts(module: DefModule): Unit = {
        def showPorts(dir: firrtl.ir.Direction): Unit = {
          module.ports.foreach {
            case port if port.direction == dir =>
              val portNode = PortNode(port.name, Some(moduleNode))
              nameToNode(getFirrtlName(port.name)) = portNode
              moduleNode += portNode
            case _ => None
          }

        }

        if(isInScope) {
          showPorts(firrtl.ir.Input)
          showPorts(firrtl.ir.Output)
        }
      }

      def processMemory(memory: DefMemory): Unit = {
        val fname = getFirrtlName(memory.name)
        val memNode = MemNode(memory.name, Some(moduleNode), fname, memory, nameToNode)
        moduleNode += memNode
      }

      def processStatement(s: Statement): Unit = {
        s match {
          case block: Block =>
            block.stmts.foreach { subStatement =>
              processStatement(subStatement)
            }
          case con: Connect if isInScope =>
            val (fName, dotName) = con.loc match {
              case WRef(name, _, _, _) => (getFirrtlName(name), expand(name))
              case Reference(name, _) => (getFirrtlName(name), expand(name))
              case subfield: WSubField =>
                (getFirrtlName(subfield.serialize), expand(subfield.serialize))
              case subfield: SubField =>
                (getFirrtlName(s.serialize), expand(subfield.serialize))
              case s: WSubIndex => (getFirrtlName(s.serialize), expand(s.serialize))
              case other =>
                println(s"Found bad connect arg $other")
                ("badName","badName")
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

          case DefNode(_, name, expression) if isInScope =>
            val fName = getFirrtlName(name)
            val nodeNode = NodeNode(name, Some(moduleNode))
            moduleNode += nodeNode
            nameToNode(fName) = nodeNode
            moduleNode.connect(expand(name), processExpression(expression))
          case DefWire(_, name, _) if isInScope =>
            val fName = getFirrtlName(name)
            val nodeNode = NodeNode(name, Some(moduleNode))
            nameToNode(fName) = nodeNode
          case reg: DefRegister if isInScope =>
            val regNode = RegisterNode(reg.name, Some(moduleNode))
            nameToNode(getFirrtlName(reg.name)) = regNode
            moduleNode += regNode
          case memory: DefMemory if isInScope =>
            processMemory(memory)
          case _ =>
          // let everything else slide
        }
      }

      def checkScope(): Unit = {
        val found = annotations.exists { annotation =>
          annotation.target match {
            case ModuleName(moduleName, _) =>
              moduleName == myModule.name
            case CircuitTopName =>
              true
            case _ =>
              false
          }
        }
        if(found) {
          isInScope = true
        }
      }

      val saveScope = isInScope
      checkScope()

      myModule match {
        case module: firrtl.ir.Module =>
          processPorts(myModule)
          processStatement(module.body)
        case extModule: ExtModule =>
          processPorts(extModule)
        case a =>
          println(s"got a $a")
      }
      isInScope = saveScope

      moduleNode
    }

    findModule(c.main, c) match {
      case topModule: DefModule =>
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

//    //TODO (Chick) this makes the dot file pop up on OS X, not sure what happens elsewhere
////    s"dot -Tpng -O ${c.main}.dot".!!
//    s"fdp -Tpng -O ${c.main}.dot".!!
//    s"open ${c.main}.dot.png".!!
//
    c
  }
}

class VisualizerTransform extends Transform {
  override def inputForm: CircuitForm = LowForm

  override def outputForm: CircuitForm = LowForm

  def show(fileName: String, dotProgram: String = "dot", openProgram: String = "open"): Unit = {
    if(dotProgram != "none") {
      val dotProcessString = s"$dotProgram -Tpng -O $fileName"
      dotProcessString.!!

      if(openProgram != "none") {
        val openProcessString = s"$openProgram $fileName.png"
        openProcessString.!!
      }
    }
  }

  override def execute(state: CircuitState): CircuitState = {
    var dotProgram = "dot"
    var openProgram = "open"

    getMyAnnotations(state) match {
      case Nil => state
      case myAnnotations =>
        val filteredAnnotations = myAnnotations.flatMap {
          case annotation@VisualizerAnnotation(_, value) =>
            if(value.startsWith(Visualizer.DotProgramString)) {
              dotProgram = value.split("=", 2).last.trim
              None
            }
            else if(value.startsWith(Visualizer.OpenProgramString)) {
              openProgram = value.split("=", 2).last.trim
              None
            }
            else {
              Some(annotation)
            }
        }
        new VisualizerPass(filteredAnnotations).run(state.circuit)

        val fileName = s"${state.circuit.main}.dot"

        show(fileName, dotProgram, openProgram)

        state
    }
  }
}

object Visualizer {
  val DepthString       = "Depth"
  val DotProgramString  = "DotProgram"
  val OpenProgramString = "OpenProgram"

  def run(fileName : String, dotProgram : String = "dot", openProgram: String = "open"): Unit = {
    val sourceFirttl = io.Source.fromFile(fileName).getLines().mkString("\n")

    val ast = Parser.parse(sourceFirttl)
    val annotations = AnnotationMap(
      Seq(
        VisualizerAnnotation(CircuitTopName),
        VisualizerAnnotation.setDotProgram(dotProgram),
        VisualizerAnnotation.setOpenProgram(openProgram)
      )
    )
    val circuitState = CircuitState(ast, LowForm, Some(annotations))

    val transform = new VisualizerTransform

    transform.execute(circuitState)
  }

  //scalastyle:off regex
  def main(args: Array[String]): Unit = {
    args.toList match {
      case fileName :: dotProgram :: openProgram :: Nil =>
        run(fileName, dotProgram, openProgram)
      case fileName :: dotProgram :: Nil =>
        run(fileName, dotProgram)
      case fileName :: Nil =>
        run(fileName)
      case _ =>
        println("Usage: Visualizer <lo-firrtl-file> <dot-program> <open-program>")
        println("       <dot-program> must be one of dot family circo, dot, fdp, neato, osage, sfdp, twopi")
        println("                     default is dot, use none to not produce png")
        println("       <open-program> default is open, this works on os-x, use none to not open")
    }
  }
}

// See LICENSE for license details.

package dotvisualizer.transforms

import java.io.PrintWriter

import dotvisualizer._
import dotvisualizer.dotnodes._
import firrtl.PrimOps._
import firrtl.ir._
import firrtl.{CircuitForm, CircuitState, LowForm, Transform, WDefInstance, WRef, WSubField, WSubIndex}

import scala.collection.mutable

/**
  * Annotations specify where to start rendering.  Currently the first encountered module that matches an annotation
  * will start the rendering, rendering continues per the depth specified in the annotation.
  * This pass is intermixed with other low to low transforms, it is not treated as a separate
  * emit, so if so annotated it will run with every firrtl compilation.
  */
//noinspection ScalaStyle
class MakeOneDiagram extends Transform {
  override def inputForm: CircuitForm = LowForm
  override def outputForm: CircuitForm = LowForm

  val subModulesFound: mutable.HashSet[DefModule] = new mutable.HashSet[DefModule]()

  def execute(state: CircuitState) : CircuitState = {
    val nameToNode: mutable.HashMap[String, DotNode] = new mutable.HashMap()

    val c = state.circuit
    val targetDir = FirrtlDiagrammer.getTargetDir(state.annotations)

    val startModuleName = state.annotations.collectFirst {
      case StartModule(moduleName) => moduleName
    }.getOrElse(state.circuit.main)

    var linesPrintedSinceFlush = 0
    var totalLinesPrinted = 0

    val useRanking = state.annotations.collectFirst { case UseRankAnnotation => UseRankAnnotation}.isDefined

    val rankDir = state.annotations.collectFirst { case RankDirAnnotation(dir) => dir}.getOrElse("LR")

    val showPrintfs = state.annotations.collectFirst { case ShowPrintfsAnnotation => ShowPrintfsAnnotation}.isDefined

    val printFileName = s"$targetDir$startModuleName.dot"
    println(s"creating dot file $printFileName")
    val printFile = new PrintWriter(new java.io.File(printFileName))
    def pl(s: String): Unit = {
      printFile.println(s)
      val lines = s.count(_ == '\n')
      totalLinesPrinted += lines
      linesPrintedSinceFlush += lines
      if(linesPrintedSinceFlush > 1000) {
        printFile.flush()
        linesPrintedSinceFlush = 0
      }
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

    /**
      * If rendering started, construct a graph inside moduleNode
      * @param modulePrefix the path to this node
      * @param myModule     the firrtl module currently being parsed
      * @param moduleNode   a node renderable to dot notation constructed from myModule
      * @return
      */
    def processModule(
      modulePrefix: String,
      myModule: DefModule,
      moduleNode: ModuleNode,
      scope: Scope = Scope(),
      subModuleDepth: Int = 0
    ): DotNode = {
      /**
        * Half the battle here is matching references between firrtl full name for an element and
        * dot's reference to a connect-able module
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

      def reducedLongLiteral(s: String): String = {
        if(s.length > 32) { s.take(16) + "..." + s.takeRight(16) } else { s }
      }

      def getLiteralValue(expression: Expression): Option[String] = {
        expression match {
          case UIntLiteral(x, _) => Some(reducedLongLiteral(x.toString))
          case SIntLiteral(x, _) => Some(reducedLongLiteral(x.toString))
          case _                 => None
        }
      }

      def processPrimOp(primOp: DoPrim): String = {
        def addBinOpNode(symbol: String): String = {
          val arg0ValueOpt = getLiteralValue(primOp.args.head)
          val arg1ValueOpt = getLiteralValue(primOp.args.tail.head)

          val opNode = BinaryOpNode(symbol, Some(moduleNode), arg0ValueOpt, arg1ValueOpt)
          moduleNode += opNode
          if(arg0ValueOpt.isEmpty) moduleNode.connect(opNode.in1, processExpression(primOp.args.head))
          if(arg1ValueOpt.isEmpty) moduleNode.connect(opNode.in2, processExpression(primOp.args.tail.head))
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
            val arg0ValueOpt = getLiteralValue(mux.tval)
            val arg1ValueOpt = getLiteralValue(mux.fval)

            val muxNode = MuxNode(s"mux_${mux.hashCode().abs}", Some(moduleNode), arg0ValueOpt, arg1ValueOpt)
            moduleNode += muxNode
            moduleNode.connect(muxNode.select, processExpression(mux.cond))
            if(arg0ValueOpt.isEmpty) moduleNode.connect(muxNode.in1, processExpression(mux.tval))
            if(arg1ValueOpt.isEmpty) moduleNode.connect(muxNode.in2, processExpression(mux.fval))
            muxNode.asRhs
          case WRef(name, _, _, _) =>
            resolveRef(getFirrtlName(name), expand(name))
          case Reference(name, _) =>
            resolveRef(getFirrtlName(name), expand(name))
          case subfield: WSubField =>
            resolveRef(getFirrtlName(subfield.serialize), expand(subfield.serialize))
          case subIndex: WSubIndex =>
            resolveRef(getFirrtlName(subIndex.serialize), expand(subIndex.serialize))
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
              val portNode = PortNode(
                port.name, Some(moduleNode),
                rank = if(port.direction == firrtl.ir.Input) 0 else 1000,
                port.direction == firrtl.ir.Input
              )
              nameToNode(getFirrtlName(port.name)) = portNode
              moduleNode += portNode
            case _ => None
          }

        }

        if(scope.doPorts()) {
          showPorts(firrtl.ir.Input)
          showPorts(firrtl.ir.Output)
        }
      }

      def processMemory(memory: DefMemory): Unit = {
        val fName = getFirrtlName(memory.name)
        val memNode = MemNode(memory.name, Some(moduleNode), fName, memory, nameToNode)
        moduleNode += memNode
      }

      def processPrintf(printf: Print): Unit = {
        val nodeName = s"printf_${printf.hashCode().abs}"
        val printfNode = PrintfNode(nodeName, printf.string.serialize, Some(moduleNode))

        printf.args.foreach { arg =>

          val displayName = arg.serialize
            .replaceAll("[<]", "&lt;")
            .replaceAll("[>]", "&gt;")
            .replaceAll("[(]", "&#40;")
            .replaceAll("[)]", "&#41;")
          val connectCode = s"${displayName.hashCode.abs}"
          val connectTarget = s"${printfNode.absoluteName}:$connectCode"
          val processedArg = processExpression(arg)

          val port = printfNode.addArgument(displayName, connectCode, processedArg)
          nameToNode(connectTarget) = port

          moduleNode.connect(connectTarget, processedArg)
        }
        printfNode.finish()


        moduleNode += printfNode
      }

      def getConnectInfo(expression: Expression): String = {
        val (fName, dotName) = expression match {
          case WRef(name, _, _, _) => (getFirrtlName(name), expand(name))
          case Reference(name, _) => (getFirrtlName(name), expand(name))
          case subfield: WSubField =>
            (getFirrtlName(subfield.serialize), expand(subfield.serialize))
          case subfield: SubField =>
            (getFirrtlName(subfield.serialize), expand(subfield.serialize))
          case s: WSubIndex => (getFirrtlName(s.serialize), expand(s.serialize))
          case other =>
            println(s"Found bad connect arg $other")
            ("badName", "badName")
        }
        val lhsName = nameToNode.get(fName) match {
          case Some(regNode: RegisterNode) => regNode.in
          case Some(memPort: MemoryPort) => memPort.absoluteName
          case _ => dotName
        }
        lhsName
      }


      def processStatement(s: Statement): Unit = {
        s match {
          case block: Block =>
            block.stmts.foreach { subStatement =>
              processStatement(subStatement)
            }
          case con: Connect if scope.doComponents() =>
            val lhsName = getConnectInfo(con.loc)
            moduleNode.connect(lhsName, processExpression(con.expr))

          case Attach(_, exprs) if scope.doComponents() =>
            exprs.toList match {
              case lhs :: tail =>
                val lhsName = getConnectInfo(lhs)
                tail.foreach { rhs =>
                  moduleNode.analogConnect(lhsName, processExpression(rhs))
                }
              case _ =>
            }

          case WDefInstance(_, instanceName, moduleName, _) =>

            val subModule = findModule(moduleName, c)
            val newPrefix = if(modulePrefix.isEmpty) instanceName else modulePrefix + "." + instanceName
            //val url_string = "file:///Users/monica/ChiselProjects/visualizer/src/test/scala/dotvisualizer/" +
            //  info.toString.drop(3).split(" ").head //name of source code file
            //val line_num = info.toString().drop(3).split(" ").tail(0).split(":").head //line # in source code file
            //val subModuleNode = ModuleNode(instanceName, Some(moduleNode), Some(url_string + "#line" + line_num))
            val moduleNameParsed = moduleName.split("/").last
            val subModuleNode = ModuleNode(instanceName, Some(moduleNode), Some(moduleNameParsed + ".dot.svg"), subModuleDepth + 1)
            moduleNode += subModuleNode

            subModulesFound += subModule

            processModule(newPrefix, subModule, subModuleNode, scope.descend, subModuleDepth + 1)

            moduleNode.subModuleNames += subModuleNode.absoluteName

          case DefNode(_, name, expression) if scope.doComponents() =>
            val fName = getFirrtlName(name)
            val nodeNode = NodeNode(name, Some(moduleNode))
            moduleNode += nodeNode
            nameToNode(fName) = nodeNode
            moduleNode.connect(expand(name), processExpression(expression))
          case DefWire(_, name, _) if scope.doComponents() =>
            val fName = getFirrtlName(name)
            val nodeNode = NodeNode(name, Some(moduleNode))
            nameToNode(fName) = nodeNode
          case reg: DefRegister if scope.doComponents() =>
            val regNode = RegisterNode(reg.name, Some(moduleNode))
            nameToNode(getFirrtlName(reg.name)) = regNode
            moduleNode += regNode
          case printf: Print if showPrintfs =>
            processPrintf(printf)
          case memory: DefMemory if scope.doComponents() =>
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

    findModule(startModuleName, c) match {
      case topModule: DefModule =>
        pl(s"digraph ${topModule.name} {")
        pl(s"""stylesheet = "styles.css"""")
        pl(s"""rankdir="$rankDir" """)
        //TODO: make this an option -- pl(s"graph [splines=ortho];")
        val topModuleNode = ModuleNode(startModuleName, parentOpt = None)
        if(useRanking) topModuleNode.renderWithRank = true
        processModule("", topModule, topModuleNode, Scope(0, 1))

        pl(topModuleNode.render)

        pl("}")
      case _ =>
        println(s"could not find top module $startModuleName")
    }

    printFile.close()
    println(s"print file closed $totalLinesPrinted lines printed")

    state
  }
}

object MakeOneDiagram {
  val subModuleColorsByDepth = Array(
    "#FFF8DC", // Cornsilk
    "#ADD8E6", // Light Blue
    "#FFB6C1"  // Light Pink
  )
}



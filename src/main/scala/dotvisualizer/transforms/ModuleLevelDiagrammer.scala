// See LICENSE for license details.

package dotvisualizer.transforms

import java.io.PrintWriter

import dotvisualizer.{FirrtlDiagrammer, StartModule}
import firrtl.Mappers._
import firrtl._
import firrtl.analyses.InstanceGraph
import firrtl.ir.{Block, DefInstance, Statement}

import scala.collection.mutable

/**
  * Represents a module instance in the graph of instances in a circuit.
  *
  * @param graphName    How `dot` will refer to this node
  * @param instanceName The instance name within the parent module
  * @param moduleName   The Module Name
  */
class ModuleDotNode private (val graphName: String, val instanceName: String, val moduleName: String) {
  val children = new mutable.ArrayBuffer[ModuleDotNode]()

  /**
    * Render this node as a small HTML table with the module name at the top
    * and each child instance as a row
    * children are sorted by moduleName then instanceName
    * @return
    */
  def render: String = {
    val s = new mutable.StringBuilder()
    s.append(s"""$graphName [shape= "plaintext" href="$moduleName.dot.svg" """)
    s.append(s"""label=<\n""")
    s.append(
      """
        |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" >
      """.stripMargin)

    s.append(
      s"""
         |  <TR >
         |    <TD BGCOLOR="#FFDEAD" > $moduleName </TD>
         |  </TR>
        """.stripMargin)

    children.sortBy { child => s"${child.moduleName} ${child.instanceName}" }.foreach { child =>
      s.append(
        s"""
           |  <TR>
           |    <TD PORT="${child.graphName}" BGCOLOR="#FFF8DC" >${child.instanceName}</TD>
           |  </TR>
        """.stripMargin)
    }

    s.append(
      s"""
         |</TABLE>>];
         |
      """.stripMargin)
    s.toString
  }
}

/**
  * Is the factory for ModuleDotNode creation
  */
object ModuleDotNode {
  var nodeCounter: Int = 0

  /**
    * This factor created a new unique dot graph node name for this instance
    * @param instanceName The instance
    * @param moduleName   The module
    * @return
    */
  def apply(instanceName: String, moduleName: String): ModuleDotNode = {
    nodeCounter += 1
    new ModuleDotNode(s"module_$nodeCounter", instanceName, moduleName)
  }
}

/**
  * Creates a high level diagram that shows the instances in the circuit and their Module names
  */
//
//TODO: Make even more links from these graph nodes back to the other generated graphs
//
class ModuleLevelDiagrammer extends Transform {
  override def inputForm: CircuitForm = LowForm
  override def outputForm: CircuitForm = LowForm

  //scalastyle:off cyclomatic.complexity method.length
  def execute(circuitState: CircuitState) : CircuitState = {
    // (targetDir: String, backFileName: String)

    val c = circuitState.circuit
    val targetDir = FirrtlDiagrammer.getTargetDir(circuitState.annotations)

    val startModule = circuitState.annotations.collectFirst {
      case StartModule(moduleName) => moduleName
    }.getOrElse(circuitState.circuit.main)

    val TopLevel = startModule + "_hierarchy"

    val moduleNodes = new mutable.ArrayBuffer[ModuleDotNode]()
    val connections = new mutable.ArrayBuffer[(String, String)]()

    val outputFileName = new java.io.File(s"$targetDir$TopLevel.dot")
    val printFile = new PrintWriter(outputFileName)

    printFile.write("digraph " + TopLevel + " { rankdir=\"TB\" \n node [shape=\"rectangle\"]; \n")
    printFile.write("rankdir=\"LR\" \n")
    printFile.println("""stylesheet = "styles.css"""")

    //statements that have modules, have a list of module names,

    val top = WDefInstance(startModule, startModule)

    /**
      * Find the given instance in the Module Hierarchy
      * This is almost certainly inefficient. Should not have to map the modules every time
      * @param instance instance being searched for
      * @return
      */
    def findModuleByName(instance: WDefInstance): Set[WDefInstance] = {
      c.modules.find(m => m.name == instance.module) match {
        case Some(module: firrtl.ir.Module) =>
          val set = new mutable.HashSet[WDefInstance]()
          def onStmt(s: Statement): Statement = s match {
            case b: Block => b map onStmt
            case i: WDefInstance => set += i; i
            case other => other
          }
          onStmt(module.body)
          set.toSet

        case other => Set.empty[WDefInstance]
      }
    }

    /**
      * Walk through the instance graph adding ModuleDotNodes and a record of their connections
      * @param wDefInstance start instance
      * @param path         underscore separated path to the start instance
      * @return
      */
    def walk(wDefInstance: WDefInstance, path: String = ""): ModuleDotNode = {
      def expand(name: String): String = {
        if(path.isEmpty) name else path + "_" + name
      }

      val dotNode = ModuleDotNode(wDefInstance.name, wDefInstance.module)
      moduleNodes += dotNode

      val children = findModuleByName(wDefInstance)
      children.foreach { child =>
        val childNode = walk(child, expand(wDefInstance.name))
        connections.append((dotNode.graphName + ":" + childNode.graphName, childNode.graphName))
        dotNode.children += childNode
      }

      dotNode
    }

    // Start walk from the top of the graph
    walk(top)

    // Render all the collected nodes
    for(mod <- moduleNodes) {
      printFile.append(mod.render)
    }

    // Render their connections
    for((parent, child) <- connections) {
      printFile.write(s"$parent -> $child\n")
    }

    // close the block
    printFile.write("}")

    // Finish writing the file
    printFile.close()
    FirrtlDiagrammer.render(s"$targetDir$TopLevel.dot")

    circuitState
  }
}

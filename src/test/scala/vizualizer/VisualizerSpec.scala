// See LICENSE for license details.

package vizualizer

import chisel3._
import chisel3.testers.BasicTester
import firrtl.annotations.Annotation
import firrtl.ir.{Input => _, Module => _, Output => _}
import firrtl.{Driver => _}
import org.scalatest._

/** Circuit Top instantiates A and B and both A and B instantiate C
  * Illustrations of annotations of various components and modules in both
  * relative and absolute cases
  *
  * This is currently not much of a test, read the printout to see what annotations look like
  */
/**
  * This class has parameterizable widths, it will generate different hardware
  * @param widthC io width
  */
class VizModC(widthC: Int) extends Module with VisualizerAnnotator {
  val io = IO(new Bundle {
    val in = Input(UInt(widthC.W))
    val out = Output(UInt(widthC.W))
  })
  io.out := io.in

  visualize(this, s"VizModC($widthC)")

  visualize(io.out, s"VizModC(ignore param)")
}

/**
  * instantiates a C of a particular size, VizModA does not generate different hardware
  * based on it's parameter
  * @param annoParam  parameter is only used in annotation not in circuit
  */
class VizModA(annoParam: Int) extends Module with VisualizerAnnotator {
  val io = IO(new Bundle {
    val in = Input(UInt())
    val out = Output(UInt())
  })
  val modC = Module(new VizModC(16))
  modC.io.in := io.in
  io.out := modC.io.out

  visualize(this, s"VizModA(ignore param)")

  visualize(io.out, s"VizModA.io.out($annoParam)")
  visualize(io.out, s"VizModA.io.out(ignore_param)")
}

class VizModB(widthB: Int) extends Module with VisualizerAnnotator{
  val io = IO(new Bundle {
    val in = Input(UInt(widthB.W))
    val out = Output(UInt(widthB.W))
  })
  val modC = Module(new VizModC(widthB))
  modC.io.in := io.in
  io.out := modC.io.out

  visualize(io.in, s"modB.io.in annotated from inside modB")
}

class TopOfVisualizer extends Module with VisualizerAnnotator {
  val io = IO(new Bundle {
    val in   = Input(UInt(32.W))
    val out  = Output(UInt(32.W))
  })
  val x = Reg(UInt(32.W))
  val y = Reg(UInt(32.W))

  val modA = Module(new VizModA(64))
  val modB = Module(new VizModB(32))

  x := io.in
  modA.io.in := x
  modB.io.in := x

  y := modA.io.out + modB.io.out
  io.out := y

  visualize(this, s"TopOfVisualizer\nWith\nSome new lines")

  visualize(modB.io.in, s"modB.io.in annotated from outside modB")
}

class VisualizerTester extends BasicTester {
  val dut = Module(new TopOfVisualizer)

  stop()
}

class AnnotatingVisualizerSpec extends FreeSpec with Matchers {
  def findAnno(as: Seq[Annotation], name: String): Option[Annotation] = {
    as.find { a => a.targetString == name }
  }

  """
    |Visualizer is an example of a module that has two sub-modules A and B who both instantiate their
    |own instances of module C.  This highlights the difference between specific and general
    |annotation scopes
  """.stripMargin - {

    """
      |annotations are not resolved at after circuit elaboration,
      |that happens only after emit has been called on circuit""".stripMargin in {

      Driver.execute(Array("--target-dir", "test_run_dir"), () => new TopOfVisualizer) match {
        case ChiselExecutionSuccess(Some(circuit), emitted, _) =>
          println(s"done!")
        case _ =>
          assert(false)
      }
    }
  }
}
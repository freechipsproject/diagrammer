// See LICENSE for license details.

package dotvisualizer

import chisel3._
import chisel3.testers.BasicTester
import firrtl.annotations.Annotation
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

  visualize(this, s"depth=2")
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

    visualize(this, s"depth=3")
}

class VizModB(widthB: Int) extends Module with VisualizerAnnotator{
  val io = IO(new Bundle {
    val in = Input(UInt(widthB.W))
    val out = Output(UInt(widthB.W))
  })
  val modC = Module(new VizModC(widthB))
  modC.io.in := io.in
  io.out := modC.io.out
}

class TopOfVisualizer extends Module with VisualizerAnnotator {
  val io = IO(new Bundle {
    val in1    = Input(UInt(32.W))
    val in2    = Input(UInt(32.W))
    val select = Input(Bool())
    val out    = Output(UInt(32.W))
    val memOut = Output(UInt(32.W))
  })
  val x = Reg(UInt(32.W))
  val y = Reg(UInt(32.W))

  val myMem = Mem(16, UInt(32.W))

  val modA = Module(new VizModA(64))
//  val modB = Module(new VizModB(32))



  when(io.select) {
    x := io.in1
    myMem(io.in1) := io.in2
  }
  .otherwise {
    x := io.in2
    io.memOut := myMem(io.in1)
  }

  modA.io.in := x

  y := modA.io.out + io.in2
  io.out := y

  visualize(this, s"TopOfVisualizer\nWith\nSome new lines")
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

      Driver.execute(Array("--target-dir", "test_run_dir", "--compiler", "low"), () => new TopOfVisualizer) match {
        case ChiselExecutionSuccess(Some(circuit), emitted, _) =>
          println(s"done!")
        case _ =>
          throw new Exception("bad parse")
      }
    }
  }
}
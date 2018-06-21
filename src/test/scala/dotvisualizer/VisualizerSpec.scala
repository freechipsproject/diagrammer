// See LICENSE for license details.

package dotvisualizer

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest._

/** Circuit Top instantiates A and B and both A and B instantiate C
  * Illustrations of annotations of various components and modules in both
  * relative and absolute cases
  *
  * This is currently not much of a test, read the printout to see what annotations look like
  */

//scalastyle:off magic.number

/**
  * This class has parameterizable widths, it will generate different hardware
  *
  * @param widthC io width
  */
//noinspection TypeAnnotation
class VizModC(widthC: Int) extends Module with VisualizerAnnotator {
  val io = IO(new Bundle {
    val in = Input(UInt(widthC.W))
    val out = Output(UInt(widthC.W))
  })
  io.out := io.in

  // visualize(this, depth = 2)
}

/**
  * instantiates a C of a particular size, VizModA does not generate different hardware
  * based on it's parameter
  * @param annoParam  parameter is only used in annotation not in circuit
  */
class VizModA(annoParam: Int) extends Module with VisualizerAnnotator {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt())
    val out = Output(UInt())
  })
  val modC = Module(new VizModC(16))
  modC.io.in := io.in
  io.out := modC.io.out

    visualize(this, depth = 1)
}

class VizModB(widthB: Int) extends Module with VisualizerAnnotator{
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt(widthB.W))
    val out = Output(UInt(widthB.W))
  })
  val modC = Module(new VizModC(widthB))
  modC.io.in := io.in
  io.out := modC.io.out
}

class TopOfVisualizer extends Module with VisualizerAnnotator {
  //noinspection TypeAnnotation
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

  io.memOut := DontCare

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

  /**
    * Play with the depth over the range 0 to 3, to see how it affects rendering
    */
  visualize(this, depth = 3)
  VisualizerAnnotation.setVisualizeAll("somestring")
}

class VisualizerTester(c: TopOfVisualizer) extends PeekPokeTester(c) {
}

class AnnotatingVisualizerSpec extends FreeSpec with Matchers {
//  def findAnno(as: Seq[Annotation], name: String): Option[Annotation] = {
//    as.find { a => a.targetString == name }
//  }

  """
    |Visualizer is an example of a module that has two sub-modules A and B who both instantiate their
    |own instances of module C.  This highlights the difference between specific and general
    |annotation scopes
  """.stripMargin - {

    """
      |annotations are not resolved at after circuit elaboration,
      |that happens only after emit has been called on circuit""".stripMargin in {

      iotesters.Driver.execute(Array("--compiler", "low"), () => new TopOfVisualizer) { c =>
        new VisualizerTester(c)
      }
    }
  }
}
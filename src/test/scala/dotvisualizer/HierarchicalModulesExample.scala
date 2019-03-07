// See LICENSE for license details.

package dotvisualizer

import chisel3._
import org.scalatest._

/**
  * Circuit Top instantiates A and B and both A and B instantiate C
  */

//scalastyle:off magic.number

/**
  * This class has parameterizable widths, it will generate different hardware
  *
  * @param widthC io width
  */
//noinspection TypeAnnotation
class VizModC(widthC: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(widthC.W))
    val out = Output(UInt(widthC.W))
  })
  io.out := io.in
}

/**
  * instantiates a C of a particular size, VizModA does not generate different hardware
  * based on it's parameter
  * @param annoParam  parameter is only used in annotation not in circuit
  */
class VizModA(annoParam: Int) extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt())
    val out = Output(UInt())
  })
  val modC = Module(new VizModC(16))
  val modB = Module(new VizModB(16))
  val modB2 = Module(new VizModB(16))

  modC.io.in := io.in
  modB.io.in := io.in
  modB2.io.in := io.in
  io.out := modC.io.out + modB.io.out + modB2.io.out
}

class VizModB(widthB: Int) extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt(widthB.W))
    val out = Output(UInt(widthB.W))
  })
  val modC = Module(new VizModC(widthB))
  modC.io.in := io.in
  io.out := modC.io.out
}

class TopOfVisualizer extends Module {
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
  val modB = Module(new VizModB(32))
  val modC = Module(new VizModC(48))

  when(io.select) {
    x := io.in1
    myMem(io.in1) := io.in2
  }
  .otherwise {
    x := io.in2
    io.memOut := myMem(io.in1)
  }

  modA.io.in := x

  y := modA.io.out + io.in2 + modB.io.out + modC.io.out
  io.out := y

  modB.io.in := x
  modC.io.in := x
}

class HierarchicalModulesExample extends FreeSpec with Matchers {

  """This is an example of a module with hierarchical submodules """  in {
    val circuit = chisel3.Driver.elaborate(() => new TopOfVisualizer)
    val firrtl = chisel3.Driver.emit(circuit)
    val config = Config(targetDir = "test_run_dir/visualizer/", firrtlSource = firrtl)
    FirrtlDiagrammer.run(config)
  }
}
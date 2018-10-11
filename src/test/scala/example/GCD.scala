// See LICENSE for license details.

package example

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}
import dotvisualizer.VisualizerAnnotator

class GCD extends Module with VisualizerAnnotator {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val a  = Input(UInt(16.W))
    val b  = Input(UInt(16.W))
    val e  = Input(Bool())
    val z  = Output(UInt(16.W))
    val v  = Output(Bool())
  })

  val x  = Reg(UInt())
  val y  = Reg(UInt())

  when (x > y) { x := x - y }
    .otherwise { y := y - x }

  when (io.e) { x := io.a; y := io.b }
  io.z := x
  io.v := y === 0.U

//  visualize(this, depth = 2)
//  setDotProgram("dot")
}

class GCDUnitTester(c: GCD) extends PeekPokeTester(c) {
  /**
    * compute the gcd and the number of steps it should take to do it
    *
    * @param a positive integer
    * @param b positive integer
    * @return the GCD of a and b
    */
  def computeGcd(a: Int, b: Int): (Int, Int) = {
    var x = a
    var y = b
    var depth = 1
    while(y > 0 ) {
      if (x > y) {
        x -= y
      }
      else {
        y -= x
      }
      depth += 1
    }
    (x, depth)
  }

  private val gcd = c

  for(i <- 1 to 40 by 3) {
    for (j <- 1 to 40 by 7) {

      poke(gcd.io.a, i)
      poke(gcd.io.b, j)
      poke(gcd.io.e, 1)
      step(1)
      poke(gcd.io.e, 0)

      val (expected_gcd, steps) = computeGcd(i, j)

      step(steps - 1) // -1 is because we step(1) already to toggle the enable
      expect(gcd.io.z, expected_gcd)
      expect(gcd.io.v, 1)

    }
  }
}

class GCDTester extends ChiselFlatSpec {
  "GCD" should s"is used here to demonstrate dot visualization" in {
    iotesters.Driver.execute(Array.empty, () => new GCD) {
      c => new GCDUnitTester(c)
    } should be (true)
  }
}


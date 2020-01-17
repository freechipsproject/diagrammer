/*
Copyright 2020 The Regents of the University of California (Regents)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package dotvisualizer

import firrtl.FileUtils
import org.scalatest.{FreeSpec, Matchers}

import scala.io.Source

/**
  * Checks that attaches are double headed arrows connected to each node
  */
class AttachExample extends FreeSpec with Matchers {

  """This example contains Analog Connects""" in {
    val dir = "test_run_dir/attach"
    FileUtils.makeDirectory(dir)

    val firrtl = s"""
         |circuit AttachTest :
         |  module AttachTest :
         |    input clock : Clock
         |    input reset : UInt<1>
         |
         |    output io : {in : Analog<1>, out : Analog<1>}
         |    output io2 : {in : Analog<1>, out1 : Analog<1>, out2 : Analog<1>, out3 : Analog<1>}
         |
         |    attach (io.in, io.out) @[cmd8.sc 6:9]
         |
         |    attach (io2.in, io2.out1, io2.out2, io2.out3) @[cmd8.sc 6:9]
         |
       """.stripMargin

    val config = Config(targetDir = s"$dir/", firrtlSource = firrtl)
    FirrtlDiagrammer.run(config)

    val lines = Source.fromFile(s"$dir/AttachTest.dot").getLines()

    val targets = Seq(
      s"""cluster_AttachTest_io_out -> cluster_AttachTest_io_in [dir = "both"]""",
      s"""cluster_AttachTest_io2_out1 -> cluster_AttachTest_io2_in [dir = "both"]""",
      s"""cluster_AttachTest_io2_out2 -> cluster_AttachTest_io2_in [dir = "both"]""",
      s"""cluster_AttachTest_io2_out3 -> cluster_AttachTest_io2_in [dir = "both"]"""
    )

    targets.foreach { target =>
      lines.exists { line => line.contains(target) } should be (true)
    }
  }
}

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

package dotvisualizer.dotnodes

import firrtl.WRef
import firrtl.ir.Print

import scala.collection.mutable

case class PrintfNode(name: String, formatString: String, parentOpt: Option[DotNode]) extends DotNode {

  val text = new mutable.StringBuilder()

  override def absoluteName: String = "struct_" + super.absoluteName

  text.append(
    s"""
      |$absoluteName [shape="plaintext" label=<
      |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#EA3076">
      |  <TR>
      |    <TD>printf("$formatString") </TD>
      |  </TR>
    """.stripMargin)

  def addArgument(displayName: String, connectTarget: String, connect: String): PrintfArgument = {
    val port = PrintfArgument(displayName, connect, connectTarget)
    text.append(s"      ${port.render}")
    port
  }

  def finish() {
    text.append(
      """
        |</TABLE>>];
    """.stripMargin)
  }

  def render: String = text.toString()
}

case class PrintfArgument(name: String, override val absoluteName: String, connectTarget: String) extends DotNode {
  val parentOpt : Option[DotNode] = None // doesn't need to know parent
  def render: String = {
    s"""
      |<TR><TD PORT="$connectTarget">$name</TD></TR>
    """.stripMargin
  }
}

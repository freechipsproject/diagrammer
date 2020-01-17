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

case class ValidIfNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  val select: String = s"$name:select"
  val in1: String = s"$name:in1"
  override val asRhs: String = s"$name:out"

  def render: String = {
    s"""
       |$name [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4" BGCOLOR="#FA8072">
       |  <TR>
       |    <TD PORT="in1">&#x25cf;</TD>
       |    <TD PORT="select">validIf?</TD>
       |    <TD PORT="out">&#x25cf;</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }

}

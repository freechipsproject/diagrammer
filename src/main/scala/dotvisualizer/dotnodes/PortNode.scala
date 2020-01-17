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

case class PortNode(name: String, parentOpt: Option[DotNode], rank: Int=10, isInput: Boolean = false) extends DotNode {
  def render: String = {
    val color = if(! isInput) { s"#E0FFFF" } else { s"#CCCCCC"}
    s"""$absoluteName [shape = "rectangle" style="filled" fillcolor="$color" label="$name" rank="$rank"]
     """.stripMargin
  }
}

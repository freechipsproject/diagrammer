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

case class Scope(depth: Int = -1, maxDepth: Int = -1) {
  def doComponents(): Boolean = {
    val result = depth >= 0 && (maxDepth == -1 || depth < maxDepth)
    result
  }
  def doPorts(): Boolean = depth >= 0 && (maxDepth == -1 || depth <= maxDepth)

  def descend: Scope = {
    if(depth == -1) {
      Scope()
    }
    else {
      val newDepth = if(depth == maxDepth) -1 else depth + 1
      Scope(newDepth, maxDepth)
    }
  }

  override def toString: String = {
    val s = (depth, maxDepth) match {
        case (-1, -1) => "out"
        case (_, -1)  => "in, no depth limit"
        case (_, _)   => s"in, do ports ${doPorts()}, do components ${doComponents()}"
    }
    s"Scope($depth, $maxDepth): $s"
  }
}

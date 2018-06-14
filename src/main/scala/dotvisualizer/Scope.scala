// See LICENSE for license details.

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

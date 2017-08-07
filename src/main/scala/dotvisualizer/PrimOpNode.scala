// See LICENSE for license details.

package dotvisualizer

object PrimOpNode {
  var psuedoHash: Long = 0
  def hash: Long = {
    psuedoHash += 1
    psuedoHash
  }
}

case class BinaryOpNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def in1: String = s"$absoluteName:in1"
  def in2: String = s"$absoluteName:in2"
  override val absoluteName: String = s"op_${name}_${PrimOpNode.hash}"
  override val asRhs: String = s"$absoluteName:out"

  def render: String ={
    s"""
       |$absoluteName [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
       |  <TR>
       |    <TD PORT="in1">&#x25cf;</TD>
       |    <TD ROWSPAN="2" > $name </TD>
       |    <TD ROWSPAN="2" PORT="out">&#x25cf;</TD>
       |  </TR>
       |  <TR>
       |    <TD PORT="in2">&#x25cf;</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }
}

case class UnaryOpNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def in1: String = s"$absoluteName:in1"
  override val absoluteName: String = s"op_${name}_${PrimOpNode.hash}"
  override val asRhs: String = s"$absoluteName:out"

  def render: String ={
    s"""
       |$absoluteName [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
       |  <TR>
       |    <TD PORT="in1">&#x25cf;</TD>
       |    <TD > $name </TD>
       |    <TD PORT="out">&#x25cf;</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }
}

case class OneArgOneParamOpNode(name: String, parentOpt: Option[DotNode], value: BigInt) extends DotNode {
  def in1: String = s"$absoluteName:in1"
  override val absoluteName: String = s"op_${name}_${PrimOpNode.hash}"
  override val asRhs: String = s"$absoluteName:out"

  def render: String ={
    s"""
       |$absoluteName [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
       |  <TR>
       |    <TD PORT="in1">&#x25cf;</TD>
       |    <TD ROWSPAN="2" > $name </TD>
       |    <TD ROWSPAN="2" PORT="out">&#x25cf;</TD>
       |  </TR>
       |  <TR>
       |    <TD>$value</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }
}
case class OneArgTwoParamOpNode(
                                 name: String, parentOpt: Option[DotNode],
                                 value1: BigInt, value2: BigInt) extends DotNode {
  def in1: String = s"$absoluteName:in1"
  override val absoluteName: String = s"op_${name}_${PrimOpNode.hash}"
  override val asRhs: String = s"$absoluteName:out"

  def render: String ={
    s"""
       |$absoluteName [shape = "plaintext" label=<
       |<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
       |  <TR>
       |    <TD PORT="in1">&#x25cf;</TD>
       |    <TD ROWSPAN="2" > $name </TD>
       |    <TD ROWSPAN="2" PORT="out">&#x25cf;</TD>
       |  </TR>
       |  <TR>
       |    <TD>($value1, $value2)</TD>
       |  </TR>
       |</TABLE>>];
       """.stripMargin
  }
}

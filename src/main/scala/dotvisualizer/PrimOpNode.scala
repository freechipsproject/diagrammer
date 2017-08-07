// See LICENSE for license details.

package dotvisualizer

case class PrimOpNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  override def render: String = s"""$absoluteName [label = "$name"]"""

  override def asRhs: String = absoluteName
}

case class BinaryOpNode(name: String, parentOpt: Option[DotNode]) extends DotNode {
  def in1: String = s"$absoluteName:in1"
  def in2: String = s"$absoluteName:in2"
  override def absoluteName: String = s"op_${name}_${hashCode().abs}"
  override def asRhs: String = s"$absoluteName:out"

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
  override def absoluteName: String = s"op_${name}_${hashCode().abs}"
  override def asRhs: String = s"$absoluteName:out"

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
  override def absoluteName: String = s"op_${name}_${hashCode().abs}"
  override def asRhs: String = s"$absoluteName:out"

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

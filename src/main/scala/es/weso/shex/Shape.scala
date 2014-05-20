package es.weso.shex

import es.weso.rdfgraph.nodes._
import es.weso.rdfgraph._

import es.weso.parser.PrefixMap
import scala.util.parsing.input.Positional

/**
 * The following definitions follow: http://www.w3.org/2013/ShEx/Definition
 * */
object ShapeSyntax {
  
case class ShEx(rules:Seq[Shape], start: Option[Label]) 
	 extends Positional // Positional helps Parser Combinators to show positions 
	 {
 
  def findShape(label:Label): Option[Shape] = {
    val rs = rules.filter(r => r.label == label)
    if (rs.size == 1) Some(rs.head)
    else None
  }
}

case class Shape(label: Label, rule: Rule)
	 extends Positional


sealed trait Rule extends Positional  

case class ArcRule(id: Option[Label], n: NameClass, v: ValueClass) extends Rule
case class AndRule(r1: Rule, r2: Rule) extends Rule
case class OrRule(r1: Rule, r2: Rule) extends Rule
case class OneOrMore(r: Rule) extends Rule
case class ActionRule(r: Rule, a: Seq[Action]) extends Rule
case object NoRule extends Rule

sealed trait Label {
  def getIRI():IRI
}

case class IRILabel(iri: IRI) extends Label {
  override def getIRI = iri
}

case class BNodeLabel(bnodeId: Int) extends Label {
  override def getIRI = ???
}

sealed trait NameClass
case class NameTerm(t: IRI) extends NameClass
case class NameAny(excl: Set[IRIStem]) extends NameClass
case class NameStem(s: IRIStem) extends NameClass

sealed trait ValueClass
case class ValueType(v: RDFNode) extends ValueClass
case class ValueSet(s: Seq[RDFNode]) extends ValueClass
case class ValueAny(excl: Set[IRIStem]) extends ValueClass
case class ValueStem(s: IRI) extends ValueClass
case class ValueReference(l: Label) extends ValueClass

case class Action(label: Label, code: String)

// TODO: We could safely remove these definitions
/*
case class Cardinality(min: Integer,max: Either[Integer,Unbound])
case class Unbound()

lazy val unbound : Unbound = Unbound()
lazy val Default = Cardinality(min = 1, max=Left(1))
lazy val Plus = Cardinality(min = 1, max=Right(unbound))
lazy val Star = Cardinality(min = 0, max=Right(unbound))
lazy val Opt  = Cardinality(min = 0, max=Left(1))

def range(m: Integer, n: Integer): Cardinality = {
  require(n > m)
  Cardinality(min = m, max = Left(n))
}
*/

//----------
def option(r: Rule): Rule = {
 OrRule(r,NoRule)  
}

def star(r: Rule): Rule = {
 OrRule(OneOrMore(r),NoRule)  
}

def range(m:Int,n:Int,r:Rule):Rule = {
  require(m > 0, "range: m must be positive")
  require(n >= m,"range: n(" + n + ") must be bigger than m (" + m + ")")
  if (m == 0) {
    if (n == 0) NoRule
    else OrRule(r,range(m,n - 1,r))
  } else {
    AndRule(r,range(m-1,n,r))
  }   
}

lazy val NoActions : Seq[Action] = Seq()
// lazy val NoId : Label = IRILabel(iri = IRI(""))


case class IRIStem(iri: IRI, isStem: Boolean) {

  def matchStem(other: RDFNode): Boolean = {
    other.isIRI && 
    other.toIRI.str.startsWith(iri.str)
  }

}

lazy val foaf = "http://xmlns.com/foaf/0.1/"
lazy val xsd  = "http://www.w3.org/2001/XMLSchema#"
lazy val shex = "http://www.w3.org/2013/ShEx/ns#"
lazy val shex_IRI = IRI(shex + "IRI")
lazy val shex_Literal = IRI(shex + "Literal")
lazy val shex_NonLiteral = IRI(shex + "NonLiteral")
lazy val shex_BNode = IRI(shex + "BNode")
lazy val xsd_string = IRI(xsd + "string")

lazy val typeShexLiteral  	= ValueType(v = shex_Literal)
lazy val typeShexIRI  		= ValueType(v = shex_IRI)
lazy val typeShexBNode  	= ValueType(v = shex_BNode)
lazy val typeShexNonLiteral	= ValueType(v = shex_NonLiteral)
lazy val typeXsdString		= ValueType(v = xsd_string)

def matchStems(stems:Set[IRIStem], node:RDFNode): Boolean = {
  stems.exists(_.matchStem(node)) 
}

}
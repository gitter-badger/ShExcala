package es.weso.shex

import es.weso.rdfNode.IRI
import es.weso.parser.PrefixMap

/**
 * The following definitions follow: http://www.w3.org/2013/ShEx/Definition
 * */

object ShapeSyntax {
  
case class ShEx(rules:Seq[Shape], start: Option[Label])

case class Shape(label: Label, rule: Rule)

sealed trait Rule

case class ArcRule(
    id: Label,
    n: NameClass,
    v: ValueClass,
    c: Cardinality,
    a: Seq[Action]
    ) extends Rule
    
case class AndRule(conjoints: Seq[Rule]) extends Rule
case class OrRule(disjoints: Seq[Rule]) extends Rule
case class GroupRule(rule: Rule, opt: Boolean, a: Seq[Action]) extends Rule

sealed trait Label
case class IRILabel(iri: IRI) extends Label
case class BNodeLabel(bnodeId: Int) extends Label

case class IRIStem(iri: IRI, isStem: Boolean)

sealed trait NameClass
case class NameTerm(t: IRI) extends NameClass
case class NameAny(excl: Set[IRIStem]) extends NameClass
case class NameStem(s: IRI) extends NameClass

sealed trait ValueClass
case class ValueType(vtype: IRI) extends ValueClass
case class ValueSet(s: Seq[IRI]) extends ValueClass
case class ValueAny(stem: IRIStem) extends ValueClass
case class ValueStem(s: IRI) extends ValueClass
case class ValueReference(l: Label) extends ValueClass

case class Action(label: Label, code: String)



// Utility definitions 

case class Cardinality(min: Integer,max: Either[Integer,Unbound])
case class Unbound()

lazy val unbound : Unbound = Unbound()
lazy val Default = Cardinality(min = 1, max=Left(1))
lazy val Plus = Cardinality(min = 1, max=Right(unbound))
lazy val Star = Cardinality(min = 0, max=Right(unbound))
lazy val Opt  = Cardinality(min = 0, max=Left(1))

lazy val NoActions : Seq[Action] = Seq()
lazy val NoId : Label = IRILabel(iri = IRI(""))

def range(m: Integer, n: Integer): Cardinality = {
  require(n > m)
  Cardinality(min = m, max = Left(n))
}

lazy val foaf = "http://xmlns.com/foaf/0.1/"
lazy val xsd  = "http://www.w3.org/2001/XMLSchema#"
lazy val shex = "http://www.w3.org/2013/ShEx/ns#"
lazy val typeShexLiteral  	= ValueType(vtype = IRI(shex + "Literal"))
lazy val typeShexIRI  		= ValueType(vtype = IRI(shex + "IRI"))
lazy val typeShexBNode  	= ValueType(vtype = IRI(shex + "BNode"))
lazy val typeShexNonLiteral	= ValueType(vtype = IRI(shex + "NonLiteral"))
lazy val typeXsdString		= ValueType(vtype = IRI(xsd  + "string"))

}
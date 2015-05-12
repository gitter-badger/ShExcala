package es.weso.shacl

import es.weso.rdfgraph.nodes._
import es.weso.rdfgraph._
import scala.collection.immutable.StringOps
import scala.text._
import Document._
import es.weso.rdf._
import arq.iri
import org.slf4j.LoggerFactory
import es.weso.shex.PREFIXES._
import es.weso.shacl.Shacl._

case class ShaclDoc(prefixMap: PrefixMap) {

  def shaclSchemaDoc(s: SHACLSchema): Document = {
    rulesDoc(s.rules) // TODO: start
  }

  def pmDoc(pm: PrefixMap): Document = {
    pm.pm.foldLeft(empty: Document)(
      (d, x) => d :/: text("prefix ") :: x._1 :: text(":") :: space ::
        text("<") :: text(x._2.str) :: text(">")
    )
  }

  def rulesDoc(rules: Set[Rule]): Document = {
    setDocWithSep(rules, "\n", ruleDoc)
  }

  def ruleDoc(rule: Rule): Document = {
    labelDoc(rule.label) ::
      shapeDefinitionDoc(rule.shapeDefinition) ::
      extensionConditionDoc(rule.extensionCondition)
  }

  def shapeDefinitionDoc(shapeDefn: ShapeDefinition): Document = {
    shapeDefn match {
      case OpenShape(id, s, inclSet) =>
        space :: "{" :: space ::
          nest(3,
            group(shapeExprDoc(s))) ::
            space ::
            text("}")
      case ClosedShape(id, s) =>
        space :: "[" :: space ::
          nest(3, group(shapeExprDoc(s))) ::
          space ::
          text("]")
    }
  }

  def labelDoc(label: Label): Document = {
    label match {
      case IRILabel(iri) => iriDoc(iri)
      case BNodeLabel(id) => text(id.toString)
    }
  }

  def shapeExprDoc(s: ShapeExpr): Document = {
    s match {
      case t: TripleConstraintValue => tripleConstraintValueDoc(t)
      case t: TripleConstraintShape => tripleConstraintShapeDoc(t)
      case t: InverseTripleConstraint => inverseTripleConstraintDoc(t)
    }
  }

  def tripleConstraintValueDoc(t: TripleConstraintValue): Document = {
    iriDoc(t.iri) :: space :: valueDoc(t.value) :: cardDoc(t.card)
  }

  def tripleConstraintShapeDoc(t: TripleConstraintShape): Document = {
    iriDoc(t.iri) :: space :: shapeDoc(t.shape) :: cardDoc(t.card)
  }

  def inverseTripleConstraintDoc(t: InverseTripleConstraint): Document = {
    "^" :: iriDoc(t.iri) :: space :: shapeDoc(t.shape) :: cardDoc(t.card)
  }

  def extensionConditionDoc(e: ExtensionCondition): Document = {
    "%" :: labelDoc(e.extLangName) :: "{" :: text(e.extDefinition) :: text("}")
  }

  def valueDoc(v: ValueConstr): Document = {
    v match {
      case LiteralDatatype(v, None) => rdfNodeDoc(v)
      case LiteralDatatype(v, Some(f)) => ???
      case ValueSet(s) => "(" ::
        nest(3, seqDocWithSep(s, " ", valueObjectDoc)) :: text(")")
      case n: NodeKind => text(n.token)
    }
  }

  def shapeDoc(s: ShapeConstr): Document = {
    s match {
      case DisjShapeConstr(shapes) => setDocWithSep(shapes, ",", labelDoc)
      case ConjShapeConstr(shapes) => setDocWithSep(shapes, "|", labelDoc)
      case NotShapeConstr(shape) => "!" :: shapeDoc(s)
    }
  }

  def cardDoc(v: Cardinality): Document = {
    v match {
      case RangeCardinality(m, n) => "{" :: m.toString :: "," :: n.toString :: text("}")
      case UnboundedCardinality(m) =>
        m match {
          case 0 => text("*")
          case 1 => text("+")
          case _ => "{" :: m.toString :: "," :: "unbounded" :: text("}")
        }
    }
  }

  def rdfNodeDoc(n: RDFNode): Document = {
    text(ShaclDoc.rdfNode2String(n)(prefixMap))
  }

  def valueObjectDoc(v: ValueObject): Document = {
    v match {
      case ValueIRI(iri) => iriDoc(iri)
      case ValueLiteral(l) => rdfNodeDoc(l)
      case ValueLang(lang) => text("@") :: text(lang.lang)
    }
  }

  def iriDoc(i: IRI): Document = {
    text(ShaclDoc.iri2String(i)(prefixMap))
  }

  def pairDoc(d1: Document, d2: Document): Document =
    "(" :: d1 :: "," :: d2 :: ")" :: empty

  def space: Document = text(" ")

  def seqDocWithSep[A](s: Seq[A],
    sep: String,
    toDoc: A => Document): Document = {
    if (s.isEmpty) empty
    else
      s.tail.foldLeft(toDoc(s.head))(
        (d: Document, x: A) => d :: sep :/: toDoc(x)
      )
  }

  def setDocWithSep[A](s: Set[A],
    sep: String,
    toDoc: A => Document): Document = {
    if (s.isEmpty) empty
    else
      s.tail.foldLeft(toDoc(s.head))(
        (d: Document, x: A) => d :: sep :/: toDoc(x)
      )
  }

}

object ShaclDoc {

  /**
   * Generic function for pretty printing
   */
  def prettyPrint(d: Document): String = {
    val writer = new java.io.StringWriter
    d.format(1, writer)
    writer.toString
  }

  def rule2String(r: Rule)(implicit pm: PrefixMap): String = {
    prettyPrint(ShaclDoc(pm).ruleDoc(r))
  }

  def schema2String(s: SHACLSchema)(implicit pm: PrefixMap): String = {
    prettyPrint(ShaclDoc(pm).shaclSchemaDoc(s))
  }

  def shape2String(shape: ShapeExpr)(implicit pm: PrefixMap): String =
    prettyPrint(ShaclDoc(pm).shapeExprDoc(shape))

  def rules2String(r: Rule)(implicit pm: PrefixMap): String = {
    prettyPrint(ShaclDoc(pm).ruleDoc(r))
  }

  def rdfNode2String(n: RDFNode)(implicit pm: PrefixMap): String = {
    n match {
      case BNodeId(id) => "_:" + id
      case iri: IRI => iri2String(iri)
      case l: Literal => l.toString
    }
  }

  def iri2String(iri: IRI)(implicit pm: PrefixMap): String = {

    def startsWithPredicate(p: (String, IRI)): Boolean = {
      iri.str.startsWith(p._2.str)
    }

    pm.pm.find(startsWithPredicate) match {
      case None => "<" ++ iri.str ++ ">"
      case Some(p) => p._1 ++ ":" ++ iri.str.stripPrefix(p._2.str)
    }
  }

}


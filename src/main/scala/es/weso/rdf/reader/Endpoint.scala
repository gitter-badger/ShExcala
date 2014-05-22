package es.weso.rdf.reader

import com.hp.hpl.jena.query._
import es.weso.rdfgraph.nodes._
import es.weso.rdfgraph.nodes.RDFNode
import es.weso.rdfgraph.statements.RDFTriple
import scala.collection.JavaConversions._
import scala.collection.immutable.StringOps._
import scala.util.Try
import es.weso.rdfgraph.statements._
import com.hp.hpl.jena.rdf.model.{RDFNode => JenaRDFNode}
import com.hp.hpl.jena.rdf.model.Property
import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.rdf.model.Model
import org.slf4j._ 
import com.hp.hpl.jena.rdf.model.{RDFNode => JenaRDFNode}
import es.weso.rdf.RDF

case class Endpoint(endpoint: String) extends RDF {
  
  val log = LoggerFactory.getLogger("Endpoint")
  
  lazy val findIRIs = QueryFactory.create(
      """|select ?x where {
         | ?x ?p ?y .
         | filter (isIRI(?x))
	     |}
         |""".stripMargin
      )
      
  lazy val findRDFTriples = QueryFactory.create(
      """|construct { ?x ?p ?y } where {
         | ?x ?p ?y .
	     |}
         |""".stripMargin
      )      
     
  def queryTriples() = {
    QueryFactory.create(
     s"""|construct {?x ?p ?y } where {
         |?x ?p ?y .
	     |}
         |""".stripMargin
    )      
  }

  def queryTriplesWithSubject(subj: IRI) = {
    val s = subj.str 
    QueryFactory.create(
     s"""|construct {<${s}> ?p ?y } where {
         |<${s}> ?p ?y .
	     |}
         |""".stripMargin
    )      
  }
  
  def queryTriplesWithObject(obj: IRI) = {
    val s = obj.str 
    QueryFactory.create(
     s"""|construct {?x ?p <${s}> } where {
         | ?x ?p <${s}> .
	     |}
         |""".stripMargin
    )      
  }

  override def parse(cs: CharSequence): Try[RDF] = {
    throw new Exception("Cannot parse into an endpoint. endpoint = " + endpoint)
  }      

  override def serialize(format:String): String = {
    throw new Exception("Cannot serialize an endpoint. endpoint = " + endpoint)
  }      

  override def iris(): Set[IRI] = {
    val resultSet = QueryExecutionFactory.sparqlService(endpoint,findIRIs).execSelect()
    resultSet.map(qs => IRI(qs.get("x").asResource.getURI)).toSet
  }  
  
  
  def rdfTriples(): Set[RDFTriple] = {
    val model = QueryExecutionFactory.sparqlService(endpoint,queryTriples).execConstruct()
    model2triples(model)
  }

  def triplesWithSubject(subj: IRI): Set[RDFTriple] = {
    val model = QueryExecutionFactory.sparqlService(endpoint,queryTriplesWithSubject(subj)).execConstruct()
    model2triples(model)
  }
  
  def triplesWithObject(obj: IRI): Set[RDFTriple] = {
    val model = QueryExecutionFactory.sparqlService(endpoint,queryTriplesWithObject(obj)).execConstruct()
    model2triples(model)
  }

  def model2triples(model: Model): Set[RDFTriple] = {
    model.listStatements().map(st => statement2triple(st)).toSet
  }
  
  def statement2triple(st: Statement): RDFTriple = {
    RDFTriple(
        jena2rdfnode(st.getSubject), 
        property2iri(st.getPredicate),
        jena2rdfnode(st.getObject))
  }
  

  def property2iri(p: Property): IRI = {
    IRI(p.getURI)
  }
  
  def jena2rdfnode(r: JenaRDFNode): RDFNode = {
    if (r.isAnon) {
      BNodeId(r.asNode.getBlankNodeId().hashCode())
    } else 
   if (r.isURIResource) {
      IRI(r.asResource.getURI())
   } else 
   if (r.isLiteral) {
     val lit = r.asLiteral
     if (lit.getDatatypeURI() == null) {
       StringLiteral(lit.getString())
     } else
     lit.getDatatypeURI() match {
       case RDFNode.IntegerDatatypeIRI => IntegerLiteral(lit.getInt)  
       case RDFNode.BooleanDatatypeIRI => BooleanLiteral(lit.getBoolean)
       case RDFNode.DoubleDatatypeIRI => DoubleLiteral(lit.getDouble())
       case RDFNode.LangStringDatatypeIRI => LangLiteral(lit.getLexicalForm, Lang(lit.getLanguage))
       case _ => DatatypeLiteral(lit.getLexicalForm, IRI(lit.getDatatypeURI))       
     }    
   } else 
     throw new Exception("Unknown type of resource")
  }
  

}
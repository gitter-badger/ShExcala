package es.weso.shacl

import es.weso.monads._
import es.weso.rdf._
import es.weso.shacl.Shacl._
import es.weso.rdfgraph.nodes._
import java.lang._
import es.weso.utils.Logging


case class Matcher(
    schema: Schema, 
    rdf: RDFReader, 
    validator: ShaclValidator 
   ) extends Logging {

  val subjects: List[RDFNode] = rdf.subjects.toList

  
/*  val ctx =
    Context(rdf = rdf, 
        schema = schema, 
        Typing.emptyTyping, 
        schema.pm, 
        validateIncoming
    ) */
/*
  def matchIRI_Label(iri: IRI)(lbl: Label): Result[Typing] = {
    log.debug("Matching " + iri + " with label " + lbl)
    try {
      for (
        shape <- ctx.getShape(lbl); 
        ctx1 <- ctx.addTyping(iri, lbl.getNode); 
        t <- validator.matchShape(ctx1, iri, shape)
      ) yield {
        log.debug("Matched with typing: " + t)
        t
      }
    } catch {
      case _: StackOverflowError => Failure("StackOverflow error")
      case e: Exception => Failure("Exception matching iri " + iri + " with label " + lbl + ": " + e.getMessage)
    }
  }

  def matchLabel_IRI(lbl: Label)(iri: IRI): Result[Typing] = {
    matchIRI_Label(iri)(lbl)
  }

  def matchIRI_AllLabels(iri: IRI): Result[Typing] = {
    Result.passSome(schema.getLabels, matchIRI_Label(iri))
  }

  def comb(t1: Typing, t2: Typing): Typing = {
    t1 combine t2
  }

  def matchAllIRIs_Label(lbl: Label): Result[Typing] = {
    Result.combineAll(subjects, matchLabel_IRI(lbl), comb)
  }

  def matchAllIRIs_AllLabels(): Result[Typing] = {
    Result.combineAll(subjects, matchIRI_AllLabels, comb)
  } */ 
} 
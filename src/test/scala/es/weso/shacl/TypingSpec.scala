package es.weso.shacl

import org.scalatest._
import org.scalatest.prop.PropertyChecks
import org.scalatest.prop.Checkers
import es.weso.shacl.Typing._
import es.weso.shacl.Shacl._
import Stream._
import es.weso.rdfgraph.nodes._
import util._
import org.scalatest.TryValues._

class TypingSpec
    extends FunSpec
    with Matchers
    with Checkers {

  describe("Typing") {

    it("Should handle empty typing") {
      val t = emptyTyping
      t.hasShapes(IRI("http://kiko.org")) should be(Set())
    }

    it("Should handle a single typing") {
      val t = for (
        t0 <- emptyTyping.addShape(IRI("x"), IRILabel(IRI("a"))); 
        t1 <- t0.addShape(IRI("x"), IRILabel(IRI("b")))
      ) yield t1
      t.get.hasShapes(IRI("x")) should be(Set(IRILabel(IRI("a")), IRILabel(IRI("b"))))
    }

    it("Should handle a single typing with no matches") {
      val t = for (
        t0 <- emptyTyping.addShape(IRI("x"), IRILabel(IRI("a"))); 
        t1 <- t0.addShape(IRI("x"), IRILabel(IRI("b")))
      ) yield t1
      t.get.hasShapes(IRI("y")) should be(Set())
    }
    
     it("Should be able to add a shape even if there are negShapes") {
      val reason = Reason("x cannot be a")
      val result = for (
        t0 <- emptyTyping.addNegShape(IRI("x"), IRILabel(IRI("a")),reason); 
        t1 <- t0.addShape(IRI("x"), IRILabel(IRI("b")))
      ) yield t1
      println("Result..." + result)
      val expected = 
        Typing(Map(IRI("x") -> ShapeType(shapes = Set(IRILabel(IRI("b"))),
                                  negShapes = Set((IRILabel(IRI("a")),reason))
                                 )))
      result.success.value should be (expected)
    }

     it("Should complain when trying to add a shape and it is in negShapes") {
      val reason = Reason("x cannot be a")
      val result = for (
        t0 <- emptyTyping.addNegShape(IRI("x"), IRILabel(IRI("a")),reason); 
        t1 <- t0.addShape(IRI("x"), IRILabel(IRI("a")))
      ) yield t1
      result.isFailure should be(true) 
    }

  }
}
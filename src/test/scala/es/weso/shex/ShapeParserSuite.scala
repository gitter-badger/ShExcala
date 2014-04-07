package es.weso.shex

import org.scalatest._
import org.scalatest.prop._

import es.weso.rdfNode._
import es.weso.shex.ShapeDoc._
import es.weso.shex.ShapeSyntax._
import scala.Either._

class ShapeParserSuite extends ShapeParser 
    with FunSpecLike 
	with Matchers 
	with Checkers {

 describe("Shape Parser") {

   describe("valueObject") {

	   it ("Should parse a value Object made by <>") {
       val str = "<>"
       
       val state : ShapeParserState = ShapeParserState.initial
       val result = ShapeParser.parse(ShapeParser.valueObject(state),str)
       result.get._1 should be(IRI(""))
       result.get._2 should be(state)
     }  

	it ("Should parse a value Object made by an IRI") {
	   val iri = "http://www.example.org/"
       val str = "<" + iri + ">"
       
       val state : ShapeParserState = ShapeParserState.initial
       val result = ShapeParser.parse(ShapeParser.valueObject(state),str)
       result.get._1 should be(IRI(iri))
       result.get._2 should be(state)
     }  

     it ("Should parse a value Object made by blank node") {
       val str = "_:a"
       val state : ShapeParserState = ShapeParserState.initial
       val (id1,state1) = state.newBNode("a")
       val result = ShapeParser.parse(ShapeParser.valueObject(state),str)
       result.get._1 should be (id1)
       result.get._2 should be (state1)
     }  

     it ("Should parse a value Object made by a qualified name") {
       val prefix = "ex"
       val localname = "a"
       val example = "http://example.org/"
       val strTest = prefix + ":" + localname
       val state : ShapeParserState = ShapeParserState.initial
       val state1 = state.addPrefix(prefix, IRI(example))

       val result = ShapeParser.parse(ShapeParser.valueObject(state1),strTest)
       result.get._1 should be (IRI(example + localname))
       result.get._2 should be (state1)
     }  
   

     it ("Should parse a value Object made by a string") {
       val str = "Hi"
       val strTest = "\"" + str + "\""
       val state : ShapeParserState = ShapeParserState.initial

       val result = ShapeParser.parse(ShapeParser.valueObject(state),strTest)
       result.get._1 should be (StringLiteral(str))
       result.get._2 should be (state)
     }  
   }

 describe("Prefix directives") {

   it("should parse prefixes") {
     val state = ShapeParserState.initial
     val str = "prefix a: <http://example.org/a/> " 
     val result = ShapeParser.parse(ShapeParser.directive(state),str)
     val expected = state.addPrefix("a",IRI("http://example.org/a/"))
     result.get should be (expected)
   }
   
       
   it("should parse statement with prefixes") {
     val state = ShapeParserState.initial
     val str = "prefix a: <http://example.org/a/> \n" +
               "prefix b: <http://example.org/b/> \n" 
     
     val result = ShapeParser.parse(ShapeParser.shExParser(state),str)
     val expected = state.
     				addPrefix("a",IRI("http://example.org/a/")).
     				addPrefix("b",IRI("http://example.org/b/"))
     val noShapes : List[List[Shape]] = List()
     result.get._1 should be (noShapes)
     result.get._2 should be (expected)
      
   }  

   it("Should be able to parse default prefix") {
      val ex = "http://example.org/"
      val str = "PREFIX : <" + ex + ">\n" + 
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                ":s { :a xsd:string }"

      val result = ShapeParser.parse(str)
      
      val shape : ArcRule = ArcRule(
    		  id = None, 
          	  n = NameTerm(IRI(ex + "a")),
          	  v = ValueType(IRI(xsd + "string")),
          	  c = Default,
          	  a = NoActions)
          	  
      val expected : ShEx = ShEx(
           rules = Seq(Shape(label= IRILabel(IRI("http://example.org/s")), 
        		   	         rule = shape
        		   	        )), 
           start = None)
        
      result should be (Left(expected))
     }
       
   }
   
   describe("Shapes") {

     it("Should parse labels") {
       val state = ShapeParserState.initial
       val iri = "Label"
       val str = "<" + iri + ">"
       val result = ShapeParser.parse(ShapeParser.label(state),str)
       result.get should be (IRILabel(IRI(iri)))
     }
     
     it("Should parse qualified labels ") {
       val prefix = "http://example.org/"
       val localName = "a"
       val alias = "ex"
       val str = alias + ":" + localName 
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.label(state),str)
       result.get should be (IRILabel(IRI(prefix + localName)))
     }

     it("Should parse openParen") {
       val str = "("
       val result = ShapeParser.parse(ShapeParser.openParen,str)
       result.get should be ("(")
     }

     it("Should parse fixedValues with qualified value type") {
       val prefix = "http://example.org/"
       val localName = "a"
       val alias = "ex"
       val str = alias + ":" + localName  
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.fixedValues(state),str)
       result.get._1 should be (ValueType(IRI(prefix + localName)))
     }

     it("Should parse fixedValues with a set of one qualified value") {
       val prefix = "http://example.org/"
       val a = "a"
       val alias = "ex"
       val str = "( " + alias + ":" + a + " )"  
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.fixedValues(state),str)
       result.get._1 should be (ValueSet(Seq(IRI(prefix + a))))
     }

     it("Should parse fixedValues with a set of two qualified values ") {
       val prefix = "http://example.org/"
       val a = "a"
       val b = "b"
       val alias = "ex"
       val str = "( " + alias + ":" + a + " " + alias + ":" + b + " )"  
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.fixedValues(state),str)
       result.get._1 should be (ValueSet(Seq(IRI(prefix + a), IRI(prefix + b))))
     }

     it("Should parse fixedValues with a reference ") {
       val label = "http://example.org/a"
       val str = "@<" + label + ">"   
       val state = ShapeParserState.initial
       val result = ShapeParser.parse(ShapeParser.fixedValues(state),str)
       result.get._1 should be (ValueReference(IRILabel(IRI(label))))
     }

     it("Should parse nameClassAndValue - single iri ") {
       val prefix = "http://example.org/"
       val a = "a"
       val b = "b"
       val alias = "ex"
       val str = alias + ":" + a + " " + alias + ":" + b    
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.nameClassAndValue(state),str)
       result.get._1._1 should be (NameTerm(IRI(prefix + a)))
       result.get._1._2 should be (ValueType(IRI(prefix + b)))
     }

     it("Should parse arc - single iris ") {
       val prefix = "http://example.org/"
       val a = "a"
       val b = "b"
       val alias = "ex"
       val str = alias + ":" + a + " " + alias + ":" + b    
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.arc(state),str)
       val expected : ArcRule = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + a)),
                           v  = ValueType(IRI(prefix + b)),
                           c  = Default,
                           a  = NoActions
                          )
       result.get._1 should be (expected)
     }

     it("Should parse ruleSpec single") {
       val prefix = "http://example.org/"
       val a = "a"
       val b = "b"
       val alias = "ex"
       val str = "{ " + alias + ":" + a + " " + alias + ":" + b + " }"    
       val state  = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.typeSpec(state),str)
       val expected : ArcRule = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + a)),
                           v  = ValueType(IRI(prefix + b)),
                           c  = Default,
                           a  = NoActions
                          )
       result.get._1 should be (expected)
     }

     it("Should parse a single shape") {
       val prefix = "http://example.org/"
       val a = "a"
       val b = "b"
       val c = "c"
       val alias = "ex"
       val str = alias + ":" + a + " { " + alias + ":" + b + " " + alias + ":" + c + " }"    
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.shape(state),str)
       val expected : Shape = 
         Shape(label = IRILabel(IRI(prefix + a)),
               rule  = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + b)),
                           v  = ValueType(IRI(prefix + c)),
                           c  = Default,
                           a  = NoActions
                          )
              )
       result.get._1 should be (expected)
     }
   
     it("Should parse several shapes") {
       val prefix = "http://example.org/"
       val a = "a"
       val b = "b"
       val c = "c"
       val alias = "ex"
       val str = alias + ":" + a + " { " + alias + ":" + b + " " + alias + ":" + c + " }\n" +
                 alias + ":" + b + " { " + alias + ":" + b + " " + alias + ":" + c + " } "
                 
       val state = ShapeParserState.initial.addPrefix(alias,IRI(prefix))
       val result = ShapeParser.parse(ShapeParser.shExParser(state),str)
       val labelA = IRILabel(IRI(prefix + a))
       val labelB = IRILabel(IRI(prefix + b))
       val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + b)),
                           v  = ValueType(IRI(prefix + c)),
                           c  = Default,
                           a  = NoActions
                          )
       val shape1 : Shape = Shape(label = labelA, rule = ruleBC )
       val shape2 : Shape = Shape(label = labelB, rule = ruleBC )
       val expected : List[Shape] = List(shape1, shape2)
       result.get._1 should be (expected)
     }

    it("Should parse directive with shape") {
      val prefix = "http://example.org/"
      val xsd = "http://www.w3.org/2001/XMLSchema#"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { :b :c }"
      val state = ShapeParserState.initial
      val result = ShapeParser.parse(ShapeParser.shExParser(state),str)
      val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "c")),
                           c  = Default,
                           a  = NoActions
                          )
       val labelA = IRILabel(IRI(prefix + "a"))
       val shape1 : Shape = Shape(label = labelA, rule = ruleBC )
       result.get._1 should be(List(shape1))
    }

     it("Should parse empty rule") {
      val prefix = "http://example.org/"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { }"
      val state = ShapeParserState.initial
      val result = ShapeParser.parse(ShapeParser.shExParser(state),str)
      val rule = NoRule
      val labelA = IRILabel(IRI(prefix + "a"))
      val shape1 : Shape = Shape(label = labelA, rule = rule )
      result.get._1 should be(List(shape1))
    }

    
    it("Should parse or simple") {
      val prefix = "http://example.org/"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { :b :c | :b :d }"
      val state = ShapeParserState.initial
      val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "c")),
                           c  = Default,
                           a  = NoActions
                          )
      val ruleBD = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "d")),
                           c  = Default,
                           a  = NoActions
                          )

      val rule = OrRule(ruleBC,ruleBD)
      val labelA = IRILabel(IRI(prefix + "a"))
      val shape1 : Shape = Shape(label = labelA, rule = rule )
      shouldParseIgnoreState(shExParser,state, str,List(shape1))
    }

    it("Should parse and simple") {
      val prefix = "http://example.org/"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { :b :c , :b :d }"
      val state = ShapeParserState.initial
      val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "c")),
                           c  = Default,
                           a  = NoActions
                          )
      val ruleBD = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "d")),
                           c  = Default,
                           a  = NoActions
                          )

      val rule = AndRule(ruleBC,ruleBD)
      val labelA = IRILabel(IRI(prefix + "a"))
      val shape1 : Shape = Shape(label = labelA, rule = rule )
      shouldParseIgnoreState(shExParser, state, str,List(shape1))
    }
   }
   
    it("Should parse and/or simple") {
      val prefix = "http://example.org/"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { :b :c , (:b :d | :b :e) }"
      val state = ShapeParserState.initial
      val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "c")),
                           c  = Default,
                           a  = NoActions
                          )
      val ruleBD = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "d")),
                           c  = Default,
                           a  = NoActions
                          )
      val ruleBE = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "e")),
                           c  = Default,
                           a  = NoActions
                          )

      val rule = AndRule(ruleBC,OrRule(ruleBD,ruleBE))
      val labelA = IRILabel(IRI(prefix + "a"))
      val shape : Shape = Shape(label = labelA, rule = rule )
      shouldParseIgnoreState(shExParser, state, str,List(shape))
    }
 
    it("Should parse and/or simple withour paren") {
      val prefix = "http://example.org/"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { :b :c , :b :d | :b :e }"
      val state = ShapeParserState.initial
      val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "c")),
                           c  = Default,
                           a  = NoActions
                          )
      val ruleBD = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "d")),
                           c  = Default,
                           a  = NoActions
                          )
      val ruleBE = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "e")),
                           c  = Default,
                           a  = NoActions
                          )

      val rule = OrRule(AndRule(ruleBC,ruleBD),ruleBE)
      val labelA = IRILabel(IRI(prefix + "a"))
      val shape : Shape = Shape(label = labelA, rule = rule )
      shouldParseIgnoreState(shExParser, state, str,List(shape))
    }

    it("Should parse arc with star") {
      val prefix = "http://example.org/"
      val str = "PREFIX : <" + prefix + ">\n" + 
                ":a { :b :c* }"
      val state = ShapeParserState.initial
      val ruleBC = ArcRule(id = None,
                           n  = NameTerm(IRI(prefix + "b")),
                           v  = ValueType(IRI(prefix + "c")),
                           c  = Star,
                           a  = NoActions)
      val rule = ruleBC
      val labelA = IRILabel(IRI(prefix + "a"))
      val shape : Shape = Shape(label = labelA, rule = rule )
      shouldParseIgnoreState(shExParser, state, str,List(shape))
    }
 
 
 }   

def shouldParse[A](p:Parser[A], s : String, a : A) {
      val result = parseAll(p,s) match {
        case Success(x,_) => x 
        case NoSuccess(msg,_) => fail(msg)
      }
      result should be(a)
   }

  def shouldParseState[A,State](p:State => Parser[(A,State)], 
      initial: State, 
      s : String, 
      expected : (A,State)) {
      val result = parseAll(p(initial),s) match {
        case Success(x,_) => x 
        case NoSuccess(msg,_) => fail(msg)
      }
      result should be(expected)
   }
  
  def shouldParseIgnoreState[A,State](p:State => Parser[(A,State)],
      initial: State, 
      s : String,
      expected : A) {
      val result = parseAll(p(initial),s) match {
        case Success(x,_) => x._1 
        case NoSuccess(msg,_) => fail(msg)
      }
      result should be(expected)
   }

}
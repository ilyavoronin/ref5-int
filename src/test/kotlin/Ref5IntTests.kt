import org.junit.jupiter.api.Test
import refal5.interp.IntSuccess
import refal5.interp.Refal5interpreter
import refal5.tree.*
import kotlin.test.assertEquals

class Ref5IntTests {
    @Test
    fun simpleTest() {
        val case = """
            Go {
                = <IsSymbol A> <IsSymbol A B>;
            }
            IsSymbol {
                s.1 = True;
                e.1 = False;
            }
        """.trimIndent()

        val res = Refal5interpreter().eval(case) as IntSuccess

        assertEquals(
            RMultExpr(RIdent("True"), RIdent("False")),
            res.res
        )
    }

    @Test
    fun palindromeTest() {
        val case = """
            Go {
                = <Pal A> <Pal 123 456 123> <Pal A B A C A B A> <Pal A B A B B A> <Pal Q W E E W Q> <Pal 'abacaba'>;
            }
            Pal { = True;
                 s.1 = True;
                 s.1 e.2 s.1 = <Pal e.2>;
                 e.1 = False;  
            }
        """.trimIndent()

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RIdent("True"), RIdent("True"), RIdent("True"), RIdent("False"), RIdent("True"), RIdent("True")),
            res
        )
    }

    @Test
    fun translatorTest() {
        val case = """
            Go {
               = <Ital-Engl 'cane'> <Ital-Engl 'gatto'> <Ital-Engl 'abacaba'> ;
            }
            
            Table { = (('cane') 'dog')
                (('gatto') 'cat');
            }
            
            Trans { 
                 (e.Word) e.1 ((e.Word) e.Trans) e.2 = e.Trans;
                 (e.Word) e.1  =  '***';  
            }
            
            Ital-Engl { e.W = <Trans (e.W) <Table>>; }
        """

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RString("dog"), RString("cat"), RString("***")),
            res
        )
    }

    @Test
    fun tripletTest() {
        val case = """
            Go {
               = <IsTriplet ABC ABC ABC> <IsTriplet 215.2 215.2 215.2> <IsTriplet 23 23 23> <IsTriplet A A A A> <IsTriplet A A A A A A> <IsTriplet A B A>;
            }
            
            IsTriplet {
               e.1 e.1 e.1 = True;
               e.2 = False;
            }
        """

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RIdent("True"), RIdent("True"), RIdent("True"), RIdent("False"), RIdent("True"), RIdent("False")),
            res
        )
    }

    @Test
    fun reverseTest() {
        val case = """
            Go {
                = <Reverse A B C> <Reverse '1' ABC 123>;
            }
            
            Reverse {
                            = ;
                s.1         = s.1 ;
                s.1 e.2 s.3 = s.3 <Reverse e.2> s.1 ;
            }
        """.trimIndent()

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RIdent("C"), RIdent("B"), RIdent("A"), RNum("123"), RIdent("ABC"), RString("1")),
            res
        )
    }

    @Test
    fun fibonacciTest() {
        val case = """
            Go {
                = <Fib 3> <Fib 4> <Fib 6> <Fib 17>;
            }
            
            Fib {
                0 = 1 ;
                1 = 1 ;
                s.Num = <+ <Fib <- s.Num 1>> <Fib <- s.Num 2> > > ;
            }
        """.trimIndent()

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RNum("3"), RNum("5"), RNum("13"), RNum("2584")),
            res
        )
    }

    @Test
    fun testMu() {
        val case = """
            Go {
                = <Call F1 1> <Call F1 2> <Call F2> ;
            }
            
            Call {
                 s1 e.1 = <Mu s1 e.1> ;
            }
            
            F1 {
                 1 = 'Hello';
                 2 = 'World';
            }
            
            F2 {
                 = AAAAAAAA ;
            }
        """.trimIndent()

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RString("Hello"), RString("World"), RIdent("AAAAAAAA")),
            res
        )
    }

    @Test
    fun testCorrectBraces() {
        val case = """
            Go {
                = <DumbCheckBr  A A B A A B A B B A A B B B A B A A B B > <DumbCheckBr  A A B B B A>;
            }
            
            DumbCheckBr {
                      = True;
                  A e1 B e2 & <DumbCheckBr e1> : True & <DumbCheckBr e2> : True = True;
                  e.3 = False;
            }
        """.trimIndent()

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RIdent("True"), RIdent("False")),
            res
        )
    }

    @Test
    fun testCannibalesMove() {
        val case = """
            
         Go {
             = <Try 1 L('MMMCCC')()> <Try 5 R('MC')('MC')>;
         }
         Try {
              * Boat on left bank
              * MM crossing
                 1 L ( 'MM' e1 ) ( e2 ) = R(e1)('MM'e2);
              * CC crossing
                 2 L(e1'CC')(e2) = R(e1)(e2'CC');
              * MC crossing
                 3 L(e1'MC'e2)(e3) = R(e1 e2)('M'e3'C');
              * M crossing
                 4 L('M'e1)(e2) = R(e1)('M'e2);
              * C crossing
                 5 L(e1'C')(e2) = R(e1)(e2'C');
              * Boat on right bank
              * MM crossing
                 1 R(e1)('MM'e2) = L('MM'e1)(e2);
              * CC crossing
                 2 R(e1)(e2'CC') = L(e1'CC')(e2);
              * MC crossing
                 3 R(e1)('M'e2'C') = L('M'e1'C')(e2);
              * M crossing
                 4 R(e1)('M'e2) = L('M'e1)(e2);
              * C crossing
                 5 R(e1)(e2'C') = L(e1'C')(e2);
              * Otherwise move impossible
                 s.Mv eS = Imposs;
          }
        """.trimIndent()

        val res = (Refal5interpreter().eval(case) as IntSuccess).res

        assertEquals(
            RMultExpr(RIdent("R"), RBraced(RString("MCCC")), RBraced(RString("MM")),
                RIdent("L"), RBraced(RString("MCC")), RBraced(RString("M"))),
            res
        )
    }
}
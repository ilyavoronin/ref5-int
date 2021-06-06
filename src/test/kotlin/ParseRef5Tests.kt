import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import parser.*
import refal5.parser.*
import refal5.tree.*
import kotlin.test.assertEquals

class ParseRef5Tests {
    @Test
    fun testParseIdent() {
        assertEquals(RIdent("Abacaba123_-"), ref5_ident[Input(" Abacaba123_- ")])
        assertEquals("Failed to parse  caused by [Symbol is not capital letter at 0:0] at 0:0", ref5_ident("abacaba".wrap()).toString())
        assertEquals(RIdent("Abacab"), ref5_ident["Abacab&".wrap()])
    }

    @Test
    fun testParse() {
        assertEquals(RString("abacaba "), ref5_string[" 'abacaba ' ".wrap()])
    }

    @Test
    fun testNum() {
        assertEquals(RNum("1234"), ref5_num[" 1234 ".wrap()])
    }

    @Test
    fun testFloat() {
        assertEquals(RFloat("215.73"), ref5_float[" 215.73 ".wrap()])
        assertEquals(RFloat("-18E+15"), ref5_float["-18E+15".wrap()])
        assertEquals(RFloat("0.003E-7"), ref5_float["0.003E-7".wrap()])
    }

    @Test
    fun testSymb() {
        assertEquals(RFloat("215.73"), ref5_symb[" 215.73 ".wrap()])
        assertEquals(RNum("1234"), ref5_symb[" 1234 ".wrap()])
        assertEquals(RString("abacaba "), ref5_symb[" 'abacaba ' ".wrap()])
        assertEquals(RIdent("Abacab"), ref5_symb["Abacab&".wrap()])
    }

    @Test
    fun testExpr() {
        assertEquals(
                RIdent("A")
            ,
            ref5_expr[" A ".wrap()]
        )

        assertEquals(
            RMultExpr(listOf(
                RBraced(RMultExpr(listOf(RIdent("A"), RString("+"), RIdent("B")))),
                RString("*"),
                RBraced(RMultExpr(listOf(RIdent("C"), RString("-"), RIdent("D")))),
            )),
            ref5_expr["  (A'+'B)'*'(C'-'D)".wrap()]
        )

        assertEquals(
            RMultExpr(listOf(
                RIdent("Begin"),
                RBraced(RMultExpr(listOf(RIdent("Ho-ho-ho"), RString("("), RBraced(RString("A joke"))))),
                RIdent("End")
            ))
            ,
            ref5_expr["   Begin (Ho-ho-ho '(' ('A joke')) End ".wrap()]
        )
    }

    @Test
    fun testPattern() {
        assertEquals(
            RMultPattern(listOf(REvar("1"), REvar("Data1"), REvar("data1"), RSvar("X"), RSvar("Free-var"), RTvar("1"), RTvar("25"))),
            ref5_pattern["e.1  e.Data1  e.data1  s.X  s.Free-var  t.1  t.25".wrap()]
        )

        assertEquals(
            RMultPattern(listOf(REvar("1"), RString("+"), REvar("2"))),
            ref5_pattern["e.1 '+' e.2 ".wrap()]
        )

        assertEquals(
            RMultPattern(listOf(
                RPatternBraced(RMultPattern(listOf(RIdent("A"), RString("+"), RIdent("B")))),
                RString("*"),
                RPatternBraced(RMultPattern(listOf(RIdent("C"), RString("-"), RIdent("D")))),
            )),
            ref5_pattern["  (A'+'B)'*'(C'-'D)".wrap()]
        )

        assertEquals(
            RMultPattern(listOf(
                RPatternBraced(RMultPattern(listOf(RIdent("A"), RString("+"), RTvar("B")))),
                RString("*"),
                RPatternBraced(RMultPattern(listOf(RIdent("C"), RString("-"), REvar("D")))),
            )),
            ref5_pattern["  (A'+'t.B)'*'(C'-'e.D)".wrap()]
        )
    }


    @Test
    fun testPatternMatching() {
        assertEquals(
            RPatMatching(RMultExpr(listOf()), RMultPattern(listOf(REvar("1"), RString("+"), REvar("2")))),
            ref5_pat_matching[":e.1 '+' e.2 ".wrap()]
        )

        assertEquals(
            RPatMatching(
                RMultExpr(listOf(RIdent("A"), RString("+"), RBraced(RMultExpr(listOf(RIdent("C"), RIdent("D")))))),
                RMultPattern(listOf(REvar("1"), RString("+"), REvar("2")))
            ),
            ref5_pat_matching[" A '+' ( C D ):e.1 '+' e.2 ".wrap()]
        )
    }
}
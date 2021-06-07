import org.junit.jupiter.api.Test
import refal5.interp.IntSuccess
import refal5.interp.Refal5interpreter
import refal5.tree.RIdent
import refal5.tree.RMultExpr
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
}
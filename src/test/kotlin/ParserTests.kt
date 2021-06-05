import org.junit.jupiter.api.Test
import parser.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTests {

    @Test
    fun altTest1() {
        val parseCar = const("car")
        val parseCat = const("cat")
        val parseBat = const("bat")

        val parseCarOrCat = parseBat * parseCar * parseCat

        assertEquals("car", parseCarOrCat.parse(Input("car")).unwrap())
        assertEquals("cat", parseCarOrCat.parse(Input("cat")).unwrap())
        assertEquals("bat", parseCarOrCat.parse(Input("bat")).unwrap())

        val err = parseCarOrCat.parse(Input("cap"))
        assertTrue { err.isError() }
    }

    @Test
    fun testCombineParseFun() {
        val funParser = combine("function") { it ->
            const("fun")[it]
            spaces()[it]
            val name = parseWhile("letter or digit") { it.isLetterOrDigit() }[it]
            const("()")[it]
            spaces()[it]
            const("{")[it]
            spaces()[it]
            const("}")[it]
            Ok(name)
        }

        val case1 = """
            fun abacaba() {}
        """.trimIndent()

        assertEquals("abacaba", funParser[Input(case1)])


        val case2 = """
            fun () {}
        """.trimIndent()

        assertEquals(
            "Failed to parse function caused by [Symbol is not letter or digit at 0:4] at 0:0",
            (funParser(Input(case2)) as ParseError).toString()
        )

        val case3 = """
            fun abacaba() {
            
            )
        """.trimIndent()

        assertEquals(
            "Failed to parse function caused by [Excpected '}' at 2:0] at 0:0",
            (funParser(Input(case3)) as ParseError).toString()
        )
    }
}
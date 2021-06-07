import refal5.interp.IntEvalError
import refal5.interp.IntParsingError
import refal5.interp.IntSuccess
import refal5.interp.Refal5interpreter
import java.io.File

fun main(args: Array<String>) {
    val filename = args[0]

    val progText = File(filename).readText()
    val interp = Refal5interpreter()

    val res = interp.eval(progText)

    when (res) {
        is IntSuccess -> println("Program finished successfully: ${res.res.nodeToString()}")
        is IntParsingError -> println("Syntax error: ${res.error}")
        is IntEvalError -> println("Program finished with error: ${res.message}")
    }
}
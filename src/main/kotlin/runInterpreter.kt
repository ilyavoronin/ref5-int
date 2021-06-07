import refal5.interp.Refal5interpreter
import java.io.File

fun main(args: Array<String>) {
    val filename = args[0]

    val progText = File(filename).readText()
    val interp = Refal5interpreter()

    println(interp.eval(progText))
}
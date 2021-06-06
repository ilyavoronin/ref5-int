package parser

class SimpleParseError<T>(
    val msg: String,
    line: Int,
    col: Int
): ParseError<T>(line, col) {
    override fun toString(): String {
        return "$msg at ${line}:${col}"
    }
}
package parser

data class SimpleParseError<T>(
    val msg: String,
    val line: Int,
    val col: Int
): ParseError<T>(line, col) {
    override fun toString(): String {
        return "$msg at ${line}:${col}"
    }
}
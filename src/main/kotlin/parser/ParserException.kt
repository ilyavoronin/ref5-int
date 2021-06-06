package parser

data class ParserException(val parseError: ParseError<*>): Throwable()

fun <T> fail(message: String, line: Int, col: Int): Nothing {
    throw ParserException(SimpleParseError<T>(message, line, col))
}
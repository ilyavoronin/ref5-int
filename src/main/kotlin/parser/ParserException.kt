package parser

data class ParserException(val parseError: ParseError<*>): Throwable()
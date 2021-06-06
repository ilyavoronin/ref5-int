package parser

interface Parser<out T> {
    fun parse(input: Input): ParseResult<out T>
}
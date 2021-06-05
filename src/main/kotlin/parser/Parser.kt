package parser

interface Parser<T> {
    fun parse(input: Input): ParseResult<T>
}
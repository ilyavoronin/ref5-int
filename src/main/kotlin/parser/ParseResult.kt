package parser

sealed class ParseResult<T> {
    abstract fun isError(): Boolean

    abstract fun unwrap(): T
}

data class Ok<T>(val res: T): ParseResult<T>() {
    override fun isError(): Boolean {
        return false
    }

    override fun unwrap(): T {
        return res
    }
}

abstract class  ParseError<T>(line: Int, col: Int): ParseResult<T>() {

    override fun isError(): Boolean {
        return true
    }

    override fun unwrap(): Nothing {
        throw ParserException(this)
    }

    abstract override fun toString(): String
}
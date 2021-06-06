package parser

sealed class ParseResult<out T> {
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

abstract class ParseError<out T>(val line: Int, val col: Int): ParseResult<T>() {

    override fun isError(): Boolean {
        return true
    }

    override fun unwrap(): Nothing {
        throw ParserException(this)
    }

    abstract override fun toString(): String
}
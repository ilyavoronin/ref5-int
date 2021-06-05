package parser

fun <T> parser(parseFunc: (Input) -> ParseResult<T>): Parser<T> {
    return object : Parser<T> {
        override fun parse(input: Input): ParseResult<T> {
            return parseFunc(input)
        }
    }
}

operator fun <T> Parser<T>.invoke(input: Input): ParseResult<T> {
    return this.parse(input)
}

fun <T> Parser<T>.lose(): Parser<Unit> {
    return parser {
        val res = this.parse(it)
        when (res) {
            is ParseError -> SimpleParseError("", it.getLine(), it.getColumn())
            is Ok -> Ok(Unit)
        }
    }
}

fun <T> const(str: String, transform: (String) -> T, error: (Input) -> ParseError<T>): Parser<T> {
    return parser { input ->
        input.beginTx()
        var i = 0
        while (!input.eof() && i < str.length && input.peek() == str[i]) {
            i += 1
            input.incr()
        }
        if (i != str.length) {
            input.rollbackTx()
            error(input)
        } else {
            input.finishTx()
            Ok(transform(str))
        }
    }
}

fun const(str: String): Parser<String> {
    return const(str, {it}, {SimpleParseError("Excpected '$str'", it.getLine(), it.getColumn())})
}

class AltParseError<T>(val parserErrors: List<ParseError<*>>, line: Int, col: Int) : ParseError<T>(line, col) {
    override fun toString(): String {
        return "All parsing alternatives failed: {\n${parserErrors.joinToString(separator = "\n") { it.toString() }}\n}"
    }

    constructor(err1: ParseError<T>, err2: ParseError<T>, line: Int, col: Int): this(join(err1, err2), line, col)

    companion object {

        fun <T> join(err1: ParseError<T>, err2: ParseError<T>): List<ParseError<*>> {
            return when(err1) {
                is AltParseError<*> -> err1.parserErrors + err2
                else -> listOf(err1, err2)
            }
        }
    }
}

//alternative
operator fun <T> Parser<T>.times(other: Parser<T>): Parser<T> {
    return parser {
        when (val res1 = this.parse(it)) {
            is ParseError -> {
                val res2 = other.parse(it)
                when (res2) {
                    is ParseError -> AltParseError(res1, res2, it.getLine(), it.getColumn())
                    else -> res2
                }
            }
            else -> res1
        }
    }
}

fun <T> combine(name: String = "", combineFun: (Input) -> ParseResult<T>): Parser<T> {
    return parser { input ->
        try {
            input.beginTx()
            combineFun(input).also {
                when (it) {
                    is ParseError -> input.rollbackTx()
                    is Ok -> input.finishTx()
                }
            }
        } catch (e: ParserException) {
            input.rollbackTx()
            SimpleParseError("Failed to parse $name caused by [${e.parseError}]", input.getLine(), input.getColumn())
        }
    }
}

operator fun <T> Parser<T>.get(input: Input): T {
    return this.parse(input).unwrap()
}

fun space(): Parser<Unit> {
    return (const(" ") * const("\n") * const("\t")).lose()
}

fun spaces(): Parser<Unit> {
    return (+space()).lose()
}

fun parseWhile(name: String, cond: (Char) -> Boolean): Parser<String> {
    return parser {
        var res = ""
        while (cond(it.peek())) {
            res += it.next()
        }
        if (res.isEmpty()) {
            SimpleParseError("Symbol is not $name", it.getLine(), it.getColumn())
        } else {
            Ok(res)
        }
    }
}

fun <T> Parser<T>.many(): Parser<List<T>> {
    return parser {
        val res = mutableListOf<T>()
        while (true) {
            val currRes = this.parse(it)
            if (currRes is ParseError) {
                break
            }
        }
        Ok(res)
    }
}

operator fun <T> Parser<T>.unaryPlus(): Parser<List<T>> {
    return this.many()
}
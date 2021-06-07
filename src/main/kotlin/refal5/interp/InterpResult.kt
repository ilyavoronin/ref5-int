package refal5.interp

import parser.ParseError
import refal5.tree.RExpr

sealed class InterpResult

data class IntSuccess(val res: RExpr): InterpResult()

data class IntParsingError(val error: ParseError<*>): InterpResult()

data class IntEvalError(val message: String): InterpResult()
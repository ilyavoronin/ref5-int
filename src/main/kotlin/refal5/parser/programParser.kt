package refal5.parser

import parser.*
import refal5.tree.*

val ref5_func_match = combine("pattern match") {
    spaces()[it]
    val pattern = ref5_pattern[it]
    spaces()[it]
    val whereMatches = ((const("&") * const(",")) - ref5_pat_matching).many(false, "where struct")[it]
    spaces()[it]
    const("=")[it]
    spaces()[it]
    val exp = ref5_expr[it]
    spaces()[it]
    const(";")[it]
    Ok(RPatMatching(exp, pattern, whereMatches))
}

val ref5_fun = combine("function") {
    spaces()[it]
    val ident = ref5_ident.map {it.str}[it]
    spaces()[it]
    const("{")[it]
    spaces()[it]
    val patMatches = (ref5_func_match.many(true, "pattern match"))[it]
    spaces()[it]
    const("}")[it]
    spaces()[it]
    Ok(RFunc(ident, patMatches))
}

val ref5_prog = (ref5_fun.untilEof(true, "function")).map {RProgram(it)}
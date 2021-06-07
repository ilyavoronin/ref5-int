package refal5.parser

import parser.*
import refal5.tree.*

val ref5_ident = combine("ident") { it ->
    spaces()[it]
    val firstSymb = symb("capital letter") { it.isUpperCase() }[it]
    val name = parseWhile("symbol for ident", true) { it.isLetterOrDigit() || it == '-' || it == '_' }[it]
    spaces()[it]
    Ok(RIdent(firstSymb + name))
}

val ref5_var_ident = combine("var ident") { it ->
    spaces()[it]
    val firstSymb = symb("letter") { it.isLetter() }[it]
    val name = parseWhile("symbol for ident", true) { it.isLetterOrDigit() || it == '-' || it == '_' }[it]
    spaces()[it]
    Ok(firstSymb + name)
}

val ref5_string = combine("string") {
    spaces()[it]
    const("'")[it]
    val str = parseWhile("", true) {it != '\''}[it]
    const("'")[it]
    spaces()[it]
    Ok(RString(str))
}

val ref5_num = combine("number") {
    spaces()[it]
    val num = number[it]
    spaces()[it]
    Ok(RNum(num))
}

val ref5_float = combine("float") {
    spaces()[it]
    val num = const("-").orEmpty()[it] + number[it]
    val rest = (
            (const(".") + digitSeq).orEmpty() +
                    (const("E") + (const("+") * const("-")) + number).orEmpty()
            )[it]
    if (rest.isEmpty()) {
        fail<String>("Not a float", it.getLine(), it.getColumn())
    }
    Ok(RFloat(num + rest))
}

val ref5_fcall = combine("function call") {
    spaces()[it]
    const("<")[it]
    val ident = (ref5_ident.map {it.str} * const("+") * const("-") * const("*") * (const("/")))[it]
    spaces()[it]
    val exp = ref5_expr[it]
    spaces()[it]
    const(">")[it]
    spaces()[it]
    Ok(RFCall(ident, exp))
}

val ref5_evar = (const("e.") - (ref5_var_ident * ref5_num.map {it.str})).map {REvar(it)}
val ref5_svar = (const("s.") - (ref5_var_ident * ref5_num.map {it.str})).map {RSvar(it)}
val ref5_tvar = (const("t.") - (ref5_var_ident * ref5_num.map {it.str})).map {RTvar(it)}

val ref5_symb: Parser<RSymb> = ref5_string * ref5_fcall * ref5_evar * ref5_svar * ref5_tvar * ref5_ident * ref5_float * ref5_num

val ref5_term = ref5_symb * combine("term") {
    spaces()[it]
    const("(")[it]
    spaces()[it]
    val res = ref5_expr[it]
    spaces()[it]
    const(")")[it]
    spaces()[it]
    Ok(RBraced(res))
}

val ref5_expr: Parser<RExpr> = (+ref5_term).map {
    if (it.size == 1) {
        it[0]
    } else {
        RMultExpr(it)
    }
}

val ref5_pat_elem: Parser<RPatternElem> = ref5_evar * ref5_svar * ref5_tvar * ref5_string * ref5_ident * ref5_float * ref5_num

val ref5_pat_term = ref5_pat_elem * combine {
    spaces()[it]
    const("(")[it]
    spaces()[it]
    val res = ref5_pattern[it]
    spaces()[it]
    const(")")[it]
    spaces()[it]
    Ok(RPatternBraced(res))
}

val ref5_pattern: Parser<RPattern> = (+ref5_pat_term).map {
    if (it.size == 1) {
        it[0]
    } else {
        RMultPattern(it)
    }
}

val ref5_pat_matching = combine {
    spaces()[it]
    val expr = ref5_expr[it]
    spaces()[it]
    const(":")[it]
    spaces()[it]
    val pattern = ref5_pattern[it]
    Ok(RPatMatching(expr, pattern))
}
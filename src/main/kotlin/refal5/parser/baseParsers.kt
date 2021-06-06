package refal5.parser

import parser.*
import refal5.tree.*

val ref5_ident = combine { it ->
    spaces()[it]
    val firstSymb = symb("capital letter") { it.isUpperCase() }[it]
    val name = parseWhile("symbol for ident", true) { it.isLetterOrDigit() || it == '-' || it == '_' }[it]
    spaces()[it]
    Ok(RIdent(firstSymb + name))
}

val ref5_var_ident = combine { it ->
    spaces()[it]
    val firstSymb = symb("letter") { it.isLetter() }[it]
    val name = parseWhile("symbol for ident", true) { it.isLetterOrDigit() || it == '-' || it == '_' }[it]
    spaces()[it]
    Ok(firstSymb + name)
}

val ref5_string = combine {
    spaces()[it]
    const("'")[it]
    val str = parseWhile("", true) {it != '\''}[it]
    const("'")[it]
    spaces()[it]
    Ok(RString(str))
}

val ref5_num = combine {
    spaces()[it]
    val num = number[it]
    spaces()[it]
    Ok(RNum(num))
}

val ref5_float = combine {
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

val ref5_symb: Parser<RSymb> = ref5_string * ref5_ident * ref5_float * ref5_num

val ref5_term = ref5_symb * combine {
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

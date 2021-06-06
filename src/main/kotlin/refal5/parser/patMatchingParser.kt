package refal5.parser

import parser.*
import refal5.tree.*

val ref5_evar = (const("e.") - (ref5_var_ident * ref5_num.map {it.str})).map {REvar(it)}
val ref5_svar = (const("s.") - (ref5_var_ident * ref5_num.map {it.str})).map {RSvar(it)}
val ref5_tvar = (const("t.") - (ref5_var_ident * ref5_num.map {it.str})).map {RTvar(it)}

val ref5_pat_elem = ref5_evar * ref5_svar * ref5_tvar * ref5_symb

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
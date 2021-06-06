package refal5.parser

import parser.*

val not0digit = symb("non 0 digit") {it.isDigit() && it != '0'}
val digitSeqEmpt = parseWhile("digit", true) {it.isDigit()}
val digitSeq = parseWhile("digit") {it.isDigit()}
val number = combine {
    val fn = not0digit[it]
    val rest = digitSeqEmpt[it]
    Ok(fn + rest)
} * const("0")
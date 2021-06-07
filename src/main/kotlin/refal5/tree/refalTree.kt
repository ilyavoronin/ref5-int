package refal5.tree

interface RNode

interface RExpr: RNode

interface RTerm: RExpr, RNode

data class RMultExpr(val terms: List<RTerm>): RExpr {
    constructor(vararg terms: RTerm): this(terms.toList())
}

data class RBraced(val expr: RExpr): RTerm

interface RSymb: RTerm

data class RIdent(val str: String): RSymb, RPatternElem

data class RString(val str: String): RSymb, RPatternElem

data class RNum(val str: String): RSymb, RPatternElem

data class RFloat(val str: String): RSymb, RPatternElem

data class RFCall(val fname: String, val exp: RExpr): RSymb


interface RPattern: RNode

interface RPatternTerm: RPattern

data class RPatternBraced(val pattern: RPattern): RPatternTerm

interface RPatternElem: RPattern, RPatternTerm

data class REvar(val str: String): RPatternElem, RSymb
data class RSvar(val str: String): RPatternElem, RSymb
data class RTvar(val str: String): RPatternElem, RSymb

data class RMultPattern(val patTerms: List<RPatternTerm>) : RPattern {
    constructor(vararg patTerms: RPatternTerm) : this(patTerms.toList())
}


data class RPatMatching(val expr: RExpr, val pattern: RPattern): RNode

data class RFunc(val name: String, val patternMatches: List<RPatMatching>): RNode {
    constructor(name: String, vararg patternMatches: RPatMatching) : this(name, patternMatches.toList())
}

data class RProgram(val funcs: List<RFunc>): RNode {
    constructor(vararg funcs: RFunc): this(funcs.toList())
}
package refal5.tree

sealed class RExpr

data class RMultExpr(val terms: List<RTerm>): RExpr()

sealed class RTerm: RExpr()

data class RBraced(val expr: RExpr): RTerm()

sealed class RSymb: RTerm(), RPatternElem {
    abstract val str: String
}

data class RIdent(override val str: String): RSymb()

data class RString(override val str: String): RSymb()

data class RNum(override val str: String): RSymb()

data class RFloat(override val str: String): RSymb()


interface RPattern

interface RPatternTerm: RPattern

data class RPatternBraced(val pattern: RPattern): RPatternTerm

interface RPatternElem: RPattern, RPatternTerm

data class REvar(val str: String): RPatternElem
data class RSvar(val str: String): RPatternElem
data class RTvar(val str: String): RPatternElem

data class RMultPattern(val patTerms: List<RPatternTerm>) : RPattern


data class RPatMatching(val expr: RExpr, val pattern: RPattern)
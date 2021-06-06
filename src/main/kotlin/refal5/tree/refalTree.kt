package refal5.tree

sealed class RExpr

data class RMultExpr(val terms: List<RTerm>): RExpr()

sealed class RTerm: RExpr()

data class RBraced(val expr: RExpr): RTerm()

sealed class RSymb: RTerm() {
    abstract val str: String
}

data class RIdent(override val str: String): RSymb()

data class RString(override val str: String): RSymb()

data class RNum(override val str: String): RSymb()

data class RFloat(override val str: String): RSymb()
package refal5.interp

import refal5.tree.RExpr
import refal5.tree.RFunc

interface BuildInFunc {
    fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult
}
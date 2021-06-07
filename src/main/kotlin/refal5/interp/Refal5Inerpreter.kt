package refal5.interp

import parser.*
import refal5.parser.ref5_prog
import refal5.tree.*
import java.lang.IllegalStateException

class Refal5interpreter {
    fun eval(code: String): InterpResult {
        val input = Input(code)
        val prog = ref5_prog(input)
        if (prog is ParseError) {
            return IntParsingError(prog)
        }

        val uprog = prog.unwrap()

        val funcs = mutableMapOf<String, RFunc>()

        uprog.funcs.forEach {
            if (funcs.containsKey(it.name)) {
                return IntEvalError("Multiple functions with name ${it.name}")
            }
            funcs[it.name] = it
        }

        val go = funcs["Go"] ?: return IntEvalError("Program should contain entry function 'Go'")
        val state = State()

        return evalFunc(go, RMultExpr(), state, funcs)
    }

    private fun evalFunc(func: RFunc, arg: RExpr, state: State, funcs: Map<String, RFunc>): InterpResult {
        func.patternMatches.forEach { pat ->
            val res = tryMatch(arg, pat.pattern)
            if (res != null) {
                val applyRes = apply(res, pat.expr)
                return@evalFunc when (applyRes) {
                    is ApplyRes.ApplyError -> IntEvalError(applyRes.msg)
                    is ApplyRes.ApplySucc -> evalInner(applyRes.expr, state, funcs)
                }
            }
        }
        return IntEvalError("No suutable pattern in function ${func.name} for expression $arg")
    }

    private fun tryMatch(exp: RExpr, pattern: RPattern): Map<RSymb, RExpr>? {
        fun matchMutlPatternWithSingleExpr(pattern: RMultPattern, expr: RExpr): Map<RSymb, RExpr>? {
            if (pattern.patTerms.isEmpty()) {
                return null
            }
            val evars = pattern.patTerms.map {it as? REvar}.filterNotNull()
            val res = mutableMapOf<RSymb, RExpr>()
            if (evars.size == pattern.patTerms.size) {
                evars.dropLast(1).forEach {
                    res[it] = RMultExpr()
                }
                res[evars.last()] = expr
                return res
            }
            if (evars.size + 1 == pattern.patTerms.size) {
                val notEvar = pattern.patTerms.find {it !is REvar}!!
                val res1 = tryMatch(expr, notEvar) ?: return null
                res.putAll(res1)
                evars.forEach {
                    res[it] = RMultExpr()
                }
                return res
            }

            return null
        }

        val res: Map<RSymb, RExpr>? = when (exp) {
            is RIdent -> {
                when(pattern) {
                    is RIdent -> if (exp.str == pattern.str) mapOf() else null
                    is REvar -> mapOf(Pair(pattern, exp))
                    is RTvar -> mapOf(Pair(pattern, exp))
                    is RSvar -> mapOf(Pair(pattern, exp))
                    is RMultPattern -> matchMutlPatternWithSingleExpr(pattern, exp)
                    else -> null
                }
            }
            is RNum -> {
                when(pattern) {
                    is RNum -> if (exp.str == pattern.str) mapOf() else null
                    is REvar -> mapOf(Pair(pattern, exp))
                    is RTvar -> mapOf(Pair(pattern, exp))
                    is RSvar -> mapOf(Pair(pattern, exp))
                    is RMultPattern -> matchMutlPatternWithSingleExpr(pattern, exp)
                    else -> null
                }
            }
            is RString -> {
                when(pattern) {
                    is RString -> if (exp.str == pattern.str) mapOf() else null
                    is REvar -> mapOf(Pair(pattern, exp))
                    is RTvar -> mapOf(Pair(pattern, exp))
                    is RSvar -> mapOf(Pair(pattern, exp))
                    is RMultPattern -> matchMutlPatternWithSingleExpr(pattern, exp)
                    else -> null
                }
            }
            is RFloat -> {
                when(pattern) {
                    is RFloat -> if (exp.str == pattern.str) mapOf() else null
                    is REvar -> mapOf(Pair(pattern, exp))
                    is RTvar -> mapOf(Pair(pattern, exp))
                    is RSvar -> mapOf(Pair(pattern, exp))
                    is RMultPattern -> matchMutlPatternWithSingleExpr(pattern, exp)
                    else -> null
                }
            }
            is RBraced -> {
                when (pattern) {
                    is RPatternBraced -> tryMatch(exp.expr, pattern.pattern)
                    is RTvar -> mapOf(Pair(pattern, exp))
                    is REvar -> mapOf(Pair(pattern, exp))
                    is RMultPattern -> matchMutlPatternWithSingleExpr(pattern, exp)
                    else -> null
                }
            }
            is RMultExpr -> {
                when (pattern) {
                    is RMultPattern -> {
                        if (pattern.patTerms.isEmpty()) {
                            if (exp.terms.isEmpty()) {
                                mapOf()
                            }
                            else {
                                null
                            }
                        } else {
                            val firstPattern = pattern.patTerms.first()
                            if (firstPattern is REvar) {
                                val currTerms = mutableListOf<RTerm>()
                                var iexp = 0
                                var res: Map<RSymb, RExpr>? = null
                                do {
                                    val currMap = mapOf<RSymb, RExpr>(Pair(firstPattern, RMultExpr(currTerms).simplify()))
                                    val restMap =
                                        tryMatch(RMultExpr(exp.terms.drop(currTerms.size)).simplify(), apply(currMap, RMultPattern(pattern.patTerms.drop(1)).simplify()))
                                    if (restMap != null) {
                                        val res1 = restMap.toMutableMap()
                                        res1.putAll(currMap)
                                        res = res1
                                        break
                                    }
                                    if (iexp < exp.terms.size) {
                                        currTerms.add(exp.terms[iexp])
                                    }
                                } while (iexp++ < exp.terms.size)
                                res
                            } else if (exp.terms.isNotEmpty()) {
                                run<Refal5interpreter, Map<RSymb, RExpr>?> {
                                    val resMap = tryMatch(exp.terms.first(), firstPattern) ?: return@run null
                                    val restMap =
                                        tryMatch(RMultExpr(exp.terms.drop(1)).simplify(), apply(resMap, RMultPattern(pattern.patTerms.drop(1)).simplify())) ?: return@run null
                                    val res = restMap.toMutableMap()
                                    res.putAll(resMap)
                                    res
                                }
                            } else null
                        }
                    }
                    is REvar -> mapOf(Pair(pattern, exp))
                    else -> null
                }
            }
            else -> null
        }
        return res
    }

    private fun apply(values: Map<RSymb, RExpr>, pattern: RPattern): RPattern {
        val res = when (pattern) {
            is REvar -> if (values.containsKey(pattern)) values[pattern]!!.toPattern() else pattern
            is RSvar -> if (values.containsKey(pattern)) values[pattern]!!.toPattern() else pattern
            is RTvar -> if (values.containsKey(pattern)) values[pattern]!!.toPattern() else pattern
            is RPatternBraced -> RPatternBraced(apply(values, pattern.pattern))
            is RMultPattern -> {
                val nterms = pattern.patTerms.map {apply(values, it)}.flatMap { if (it is RMultPattern) it.patTerms else listOf(it as RPatternTerm) }
                return RMultPattern(nterms).simplify()
            }
            else -> pattern
        }
        return res
    }

    sealed class ApplyRes {
        data class ApplySucc(val expr: RExpr) : ApplyRes()
        data class ApplyError(val msg: String): ApplyRes()
    }


    private fun apply(values: Map<RSymb, RExpr>, expr: RExpr): ApplyRes {
        val res = when (expr) {
            is REvar -> if (values.containsKey(expr)) ApplyRes.ApplySucc(values[expr]!!) else ApplyRes.ApplyError("No matching for $expr")
            is RSvar -> if (values.containsKey(expr)) ApplyRes.ApplySucc(values[expr]!!) else ApplyRes.ApplyError("No matching for $expr")
            is RTvar -> if (values.containsKey(expr)) ApplyRes.ApplySucc(values[expr]!!) else ApplyRes.ApplyError("No matching for $expr")
            is RBraced -> {
                val res = apply(values, expr.expr)
                when (res) {
                    is ApplyRes.ApplySucc -> ApplyRes.ApplySucc(RBraced(res.expr))
                    is ApplyRes.ApplyError -> res
                }
            }
            is RMultExpr -> {
                val nterms = expr.terms
                    .map {apply(values, it)}.flatMap {
                        if (it is ApplyRes.ApplyError) return@apply it
                        val nexpr = (it as ApplyRes.ApplySucc).expr
                        if (nexpr is RMultExpr) nexpr.terms else listOf(nexpr as RTerm)
                    }
                ApplyRes.ApplySucc(RMultExpr(nterms).simplify())
            }
            is RFCall -> {
                when (val fres = apply(values, expr.exp)) {
                    is ApplyRes.ApplySucc -> ApplyRes.ApplySucc(RFCall(expr.fname, fres.expr))
                    is ApplyRes.ApplyError -> fres
                }
            }
            else -> ApplyRes.ApplySucc(expr)
        }
        return res
    }

    private fun evalInner(node: RNode, state: State, funcs: Map<String, RFunc>): InterpResult {
        val res = when (node) {
            is RIdent -> IntSuccess(node)
            is RString -> IntSuccess(node)
            is RNum -> IntSuccess(node)
            is RFloat -> IntSuccess(node)
            is RMultExpr -> {
                val evalExps = node.terms.map { evalInner(it, state, funcs) }.flatMap {
                    if (it is IntSuccess) {
                        when(it.res) {
                            is RBraced -> listOf(it.res)
                            is RSymb -> listOf(it.res)
                            is RMultExpr -> it.res.terms
                            else -> listOf()
                        }
                    } else {
                        return it
                    }
                }
                IntSuccess(RMultExpr(evalExps).simplify())
            }

            is RBraced -> {
                val eexp = evalInner(node.expr, state, funcs)
                if (eexp is IntSuccess) {
                    IntSuccess(RBraced(eexp.res))
                } else {
                    eexp
                }
            }

            is RFCall -> {
                val fname = node.fname
                if (!funcs.containsKey(fname)) {
                    IntEvalError("No such func: $fname")
                } else {
                    val func = funcs[fname]!!
                    val evalExpr = evalInner(node.exp, state, funcs)
                    if (evalExpr is IntSuccess) {
                        evalFunc(func, evalExpr.res , state, funcs)
                    } else {
                        evalExpr
                    }
                }
            }

            else -> throw IllegalStateException("No rule to process node ${node}")
        }
        return res
    }

    private fun RExpr.toPattern(): RPattern {
        return if (this is RMultExpr) {
            RMultPattern(this.terms.map {it.toPattern() as RPatternTerm})
        } else {
            this as RPattern
        }
    }
}
package refal5.interp

import parser.*
import refal5.parser.ref5_expr
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

        return evalFunc(go, RMultExpr(), funcs)
    }

    companion object {

        private fun evalFunc(func: RFunc, arg: RExpr, funcs: Map<String, RFunc>): InterpResult {
            func.patternMatches.forEach { pat ->
                val res = tryMatch2(arg, pat.pattern, pat.whereBlocks, funcs)
                if (res != null) {
                    val applyRes = apply(res, pat.expr, true)
                    return@evalFunc when (applyRes) {
                        is ApplyRes.ApplyError -> IntEvalError(applyRes.msg)
                        is ApplyRes.ApplySucc -> evalInner(applyRes.expr, funcs)
                    }
                }
            }
            return IntEvalError("No suitable pattern in function ${func.name} for expression $arg")
        }


        private fun tryMatch2(exp: RExpr, pattern: RPattern, whereStructs: List<RPatMatching>, funcs: Map<String, RFunc>): Map<RSymb, RExpr>? {


            fun applyToWhere(mapping: Map<RSymb, RExpr>, whereStructs: List<RPatMatching>): List<RPatMatching>? {
                val res = whereStructs.map {
                    when (val newWithExp = apply(mapping, it.expr, false)) {
                        is ApplyRes.ApplySucc -> RPatMatching(newWithExp.expr, apply(mapping, it.pattern))
                        else -> null
                    }
                }
                return if (res.any {it == null}) {
                    null
                } else {
                    res.filterNotNull()
                }
            }

            fun Map<RSymb, RExpr>?.applyWith(): Map<RSymb, RExpr>? {
                if (this == null) {
                    return null
                }
                if (whereStructs.isEmpty()) {
                    return this
                }
                return when (val res = apply(this, whereStructs.first().expr, true)) {
                    is ApplyRes.ApplySucc -> {
                        when (val evalRes = evalInner(res.expr, funcs)) {
                            is IntSuccess -> {
                                applyToWhere(this@applyWith ,whereStructs.drop(1)) ?.let { nwhere ->
                                    tryMatch2(
                                        evalRes.res,
                                        whereStructs.first().pattern,
                                        nwhere,
                                        funcs
                                    )?.toMutableMap()?.apply {
                                        putAll(this@applyWith)
                                    }
                                }
                            }
                            else -> throw IllegalStateException(evalRes.toString())
                        }
                    }
                    is ApplyRes.ApplyError -> null
                }
            }

            fun emptyMap(): Map<RSymb, RExpr>? {
                return mapOf<RSymb, RExpr>()
            }

            fun mapOf(vararg vals: Pair<RSymb, RExpr>): Map<RSymb, RExpr> {
                return kotlin.collections.mapOf(*vals)
            }

            val res: Map<RSymb, RExpr>? = when(pattern) {
                is RIdent -> {
                    when (exp) {
                        is RIdent -> if (exp.str == pattern.str) emptyMap() else null
                        else -> null
                    }.applyWith()
                }
                is RNum -> {
                    when (exp) {
                        is RNum -> if (exp.str == pattern.str)  emptyMap() else null
                        else -> null
                    }.applyWith()
                }
                is RFloat -> {
                    when (exp) {
                        is RFloat -> if (exp.str == pattern.str) emptyMap() else null
                        else -> null
                    }.applyWith()
                }
                is RString -> {
                    when (exp) {
                        is RString -> if (exp.str == pattern.str) emptyMap() else null
                        else -> null
                    }.applyWith()
                }
                is RSvar -> {
                    when  {
                        (exp is RFloat) || (exp is RString && exp.str.length == 1) || (exp is RIdent) || (exp is RNum) -> mapOf(Pair(pattern, exp))
                        else -> null
                    }.applyWith()
                }
                is RTvar -> {
                    when {
                        (exp is RBraced) || (exp is RFloat) || (exp is RString && exp.str.length == 1) || (exp is RIdent) || (exp is RNum) -> mapOf(Pair(pattern, exp))
                        else -> null
                    }.applyWith()
                }

                is REvar -> {
                    mapOf(Pair(pattern, exp)).applyWith()
                }

                is RPatternBraced -> {
                    when (exp) {
                        is RBraced -> tryMatch2(exp.expr, pattern.pattern, emptyList(), funcs)
                        else -> null
                    }.applyWith()
                }

                is RMultPattern -> {
                    val exps = when (exp) {
                        is RMultExpr -> exp.terms
                        else -> listOf(exp as RTerm)
                    }.splitStrings()
                    if (pattern.patTerms.isEmpty()) {
                        if (exps.isEmpty()) {
                            mapOf()
                        } else {
                            null
                        }.applyWith()
                    } else {
                        val patTerms = pattern.patTerms.splitStrings()
                        val firstPattern = patTerms.first()
                        if (firstPattern is REvar) {
                            val currTerms = mutableListOf<RTerm>()
                            var iexp = 0
                            var res: Map<RSymb, RExpr>? = null
                            do {
                                val currMap =
                                    mapOf<RSymb, RExpr>(Pair(firstPattern, RMultExpr(currTerms.joinStrings()).simplify()))

                                var successfulApp = true
                                val newWhereStructs = whereStructs.map {
                                    when (val newWithExp = apply(currMap, it.expr, false)) {
                                        is ApplyRes.ApplySucc -> RPatMatching(newWithExp.expr, apply(currMap, it.pattern))
                                        else -> {successfulApp = false; null}
                                    }
                                }.filterNotNull()
                                if (!successfulApp) {
                                    continue
                                }
                                val restMap =
                                    tryMatch2(
                                        RMultExpr(exps.drop(currTerms.size).joinStrings()).simplify(),
                                        apply(currMap, RMultPattern(patTerms.drop(1).joinStrings()).simplify()),
                                        newWhereStructs,
                                        funcs
                                    )
                                if (restMap != null) {
                                    val res1 = restMap.toMutableMap()
                                    res1.putAll(currMap)
                                    res = res1
                                    break
                                }
                                if (iexp < exps.size) {
                                    currTerms.add(exps[iexp])
                                }
                            } while (iexp++ < exps.size)
                            res
                        } else if (exps.isNotEmpty()) {
                            run {
                                val resMap = tryMatch2(exps.first(), firstPattern, emptyList(), funcs) ?: return@run null
                                var successfulApp = true
                                val newWithStructs = whereStructs.map {
                                    when (val newWithExp = apply(resMap, it.expr, false)) {
                                        is ApplyRes.ApplySucc -> RPatMatching(newWithExp.expr, apply(resMap, it.pattern))
                                        else -> {successfulApp = false; null}
                                    }
                                }.filterNotNull()
                                if (!successfulApp) {
                                    null
                                } else {
                                    val restMap =
                                        tryMatch2(
                                            RMultExpr(exps.drop(1).joinStrings()).simplify(),
                                            apply(resMap, RMultPattern(patTerms.drop(1).joinStrings()).simplify()),
                                            newWithStructs,
                                            funcs
                                        ) ?: return@run null
                                    val res = restMap.toMutableMap()
                                    res.putAll(resMap)
                                    res
                                }
                            }
                        } else null
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
                    val nterms = pattern.patTerms.map { apply(values, it) }
                        .flatMap { if (it is RMultPattern) it.patTerms else listOf(it as RPatternTerm) }
                    return RMultPattern(nterms.joinStrings()).simplify()
                }
                else -> pattern
            }
            return res
        }

        sealed class ApplyRes {
            data class ApplySucc(val expr: RExpr) : ApplyRes()
            data class ApplyError(val msg: String) : ApplyRes()
        }

        private fun List<RTerm>.splitStrings(): List<RTerm> {
            return this.flatMap { if (it is RString) it.str.map { RString(it.toString()) } else listOf(it) }
        }

        private fun List<RTerm>.joinStrings(): List<RTerm> {
            val res = mutableListOf<RTerm>()
            this.forEach {
                if (res.isEmpty()) {
                    res.add(it)
                } else {
                    if (res.last() is RString && it is RString) {
                        res.add(RString((res.removeLast() as RString).str + it.str))
                    } else {
                        res.add(it)
                    }
                }
            }
            return res
        }

        @JvmName("splitStringsRPatternTerm")
        private fun List<RPatternTerm>.splitStrings(): List<RPatternTerm> {
            return this.flatMap { if (it is RString) it.str.map { RString(it.toString()) } else listOf(it) }
        }

        @JvmName("joinStringsRPatternTerm")
        private fun List<RPatternTerm>.joinStrings(): List<RPatternTerm> {
            val res = mutableListOf<RPatternTerm>()
            this.forEach {
                if (res.isEmpty()) {
                    res.add(it)
                } else {
                    if (res.last() is RString && it is RString) {
                        res.add(RString((res.removeLast() as RString).str + it.str))
                    } else {
                        res.add(it)
                    }
                }
            }
            return res
        }


        private fun apply(values: Map<RSymb, RExpr>, expr: RExpr, fillAll: Boolean): ApplyRes {
            val res = when (expr) {
                is REvar -> if (values.containsKey(expr)) ApplyRes.ApplySucc(values[expr]!!) else if (!fillAll) ApplyRes.ApplySucc(expr) else ApplyRes.ApplyError("No matching for $expr")
                is RSvar -> if (values.containsKey(expr)) ApplyRes.ApplySucc(values[expr]!!) else if (!fillAll) ApplyRes.ApplySucc(expr) else ApplyRes.ApplyError("No matching for $expr")
                is RTvar -> if (values.containsKey(expr)) ApplyRes.ApplySucc(values[expr]!!) else if (!fillAll) ApplyRes.ApplySucc(expr) else ApplyRes.ApplyError("No matching for $expr")
                is RBraced -> {
                    val res = apply(values, expr.expr, fillAll)
                    when (res) {
                        is ApplyRes.ApplySucc -> ApplyRes.ApplySucc(RBraced(res.expr))
                        is ApplyRes.ApplyError -> res
                    }
                }
                is RMultExpr -> {
                    val nterms = expr.terms
                        .map { apply(values, it, fillAll) }.flatMap {
                            if (it is ApplyRes.ApplyError) return@apply it
                            val nexpr = (it as ApplyRes.ApplySucc).expr
                            if (nexpr is RMultExpr) nexpr.terms else listOf(nexpr as RTerm)
                        }
                    ApplyRes.ApplySucc(RMultExpr(nterms.joinStrings()).simplify())
                }
                is RFCall -> {
                    when (val fres = apply(values, expr.exp, fillAll)) {
                        is ApplyRes.ApplySucc -> ApplyRes.ApplySucc(RFCall(expr.fname, fres.expr))
                        is ApplyRes.ApplyError -> fres
                    }
                }
                else -> ApplyRes.ApplySucc(expr)
            }
            return res
        }

        private fun evalInner(node: RNode, funcs: Map<String, RFunc>): InterpResult {
            val res = when (node) {
                is RIdent -> IntSuccess(node)
                is RString -> IntSuccess(node)
                is RNum -> IntSuccess(node)
                is RFloat -> IntSuccess(node)
                is RMultExpr -> {
                    val evalExps = node.terms.map { evalInner(it, funcs).also { if (it !is IntSuccess) return it } }.flatMap {
                        if (it is IntSuccess) {
                            when (it.res) {
                                is RBraced -> listOf(it.res)
                                is RSymb -> listOf(it.res)
                                is RMultExpr -> it.res.terms
                                else -> listOf()
                            }
                        } else {
                            return it
                        }
                    }
                    IntSuccess(RMultExpr(evalExps.joinStrings()).simplify())
                }

                is RBraced -> {
                    val eexp = evalInner(node.expr, funcs)
                    if (eexp is IntSuccess) {
                        IntSuccess(RBraced(eexp.res))
                    } else {
                        eexp
                    }
                }

                is RFCall -> {
                    val fname = node.fname
                    callFunction(fname, funcs, node.exp)
                }

                else -> throw IllegalStateException("No rule to process node ${node}")
            }
            return res
        }

        private fun RExpr.toPattern(): RPattern {
            return if (this is RMultExpr) {
                RMultPattern(this.terms.map { it.toPattern() as RPatternTerm })
            } else if (this is RBraced) {
                RPatternBraced(this.expr.toPattern())
            }else {
                this as RPattern
            }
        }

        private fun callFunction(fname: String, funcs: Map<String, RFunc>, exp: RExpr): InterpResult {
           return if (!funcs.containsKey(fname) && !ourBuildInFunc.containsKey(fname)) {
                IntEvalError("No such func: $fname")
            } else {
                val evalExpr = evalInner(exp, funcs)

                if (evalExpr is IntSuccess) {
                    val func = funcs[fname]
                    if (func != null) {
                        evalFunc(func, evalExpr.res, funcs)
                    } else {
                        ourBuildInFunc[fname]!!.eval(evalExpr.res, funcs)
                    }
                } else {
                    evalExpr
                }
            }
        }

        // Built in functions

        private val plus = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                fun getError(): IntEvalError {
                    return IntEvalError("Add function takes two arguments of type Num.  But ${expr} was given")
                }

                if (expr !is RMultExpr || expr.terms.size != 2) {
                    return getError()
                }
                val arg1 = expr.terms[0]
                val arg2 = expr.terms[1]

                if (arg1 !is RNum || arg2 !is RNum) {
                    return getError()
                }

                return IntSuccess(RNum((arg1.str.toLong() + arg2.str.toLong()).toString()))
            }

        }

        private val minus = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                fun getError(): IntEvalError {
                    return IntEvalError("Sub function takes two arguments of type Num.  But ${expr} was given")
                }

                if (expr !is RMultExpr || expr.terms.size != 2) {
                    return getError()
                }
                val arg1 = expr.terms[0]
                val arg2 = expr.terms[1]

                if (arg1 !is RNum || arg2 !is RNum) {
                    return getError()
                }

                return IntSuccess(RNum((arg1.str.toLong() - arg2.str.toLong()).toString()))
            }

        }

        private val mult = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                fun getError(): IntEvalError {
                    return IntEvalError("Mult function takes two arguments of type Num.  But ${expr} was given")
                }

                if (expr !is RMultExpr || expr.terms.size != 2) {
                    return getError()
                }
                val arg1 = expr.terms[0]
                val arg2 = expr.terms[1]

                if (arg1 !is RNum || arg2 !is RNum) {
                    return getError()
                }

                return IntSuccess(RNum((arg1.str.toLong() * arg2.str.toLong()).toString()))
            }

        }

        private val div = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                fun getError(): IntEvalError {
                    return IntEvalError("Div function takes two arguments of type Num.  But ${expr} was given")
                }

                if (expr !is RMultExpr || expr.terms.size != 2) {
                    return getError()
                }
                val arg1 = expr.terms[0]
                val arg2 = expr.terms[1]

                if (arg1 !is RNum || arg2 !is RNum) {
                    return getError()
                }

                return IntSuccess(RNum((arg1.str.toLong() / arg2.str.toLong()).toString()))
            }
        }

        private val input = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                val res = readLine() ?: return IntEvalError("Card failed. Failed to read line")

                val parsed = ref5_expr.full()(Input(res))
                if (parsed is ParseError) {
                    return IntEvalError("Card failed. Not correct expression: $parsed")
                }

                return IntSuccess(parsed.unwrap())
            }
        }

        private val output = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                println(expr.nodeToString())

                return IntSuccess(RMultExpr())
            }
        }

        private val mu = object : BuildInFunc {
            override fun eval(expr: RExpr, funcs: Map<String, RFunc>): InterpResult {
                val nameExpr = if (expr is RMultExpr) {
                    if (expr.terms.isEmpty()) {
                        return IntEvalError("Mu function espects at least one argument(name of the function). But none were given")
                    } else {
                        expr.terms.first()
                    }
                } else {
                    expr
                }
                if (nameExpr !is RIdent) {
                    return IntEvalError("Mu function expects Ident as a first argument. But ${nameExpr} was given")
                }

                val args = if (expr is RMultExpr) {
                    RMultExpr(expr.terms.drop(1)).simplify()
                } else {
                    RMultExpr()
                }
                return callFunction(nameExpr.str, funcs, args)
            }
        }


        private val ourBuildInFunc = mapOf(
            Pair("Card", input),
            Pair("Print", output),
            Pair("Prout", output),
            Pair("Mu", mu),
            Pair("Add", plus),
            Pair("+", plus),
            Pair("Sub", minus),
            Pair("-", minus),
            Pair("Div", div),
            Pair("/", div),
            Pair("Mul", mult),
            Pair("*", mult)
        )

    }
}
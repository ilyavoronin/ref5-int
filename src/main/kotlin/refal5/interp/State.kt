package refal5.interp

import refal5.parser.ref5_var_ident
import refal5.tree.*
import java.lang.IllegalStateException

class State {
    private class Vars {
        private val mySVars = mutableMapOf<String, RExpr>()
        private val myTVars = mutableMapOf<String, RExpr>()
        private val myEVars = mutableMapOf<String, RExpr>()

        fun put(va: RPatternElem, value: RExpr) {
            when(va) {
                is REvar -> myEVars[va.str] = value
                is RTvar -> myTVars[va.str] = value
                is RSvar -> mySVars[va.str] = value
                else -> throw IllegalStateException("Variable should be svar, tvar or evar")
            }
        }

        fun get(va: RPatternElem): RExpr? {
            return when(va) {
                is REvar -> myEVars[va.str]
                is RTvar -> myTVars[va.str]
                is RSvar -> mySVars[va.str]
                else -> throw IllegalStateException("Variable should be svar, tvar or evar")
            }
        }
    }

    private val myStateStack = mutableListOf<Vars>()

    fun enter() {
        val newVars = Vars()
        myStateStack.add(newVars)
    }

    fun exit() {
        myStateStack.removeLast()
    }

    fun set(svar: RSvar, expr: RExpr) {
        myStateStack.last().put(svar, expr)
    }

    fun set(tvar: RTvar, expr: RExpr) {
        myStateStack.last().put(tvar, expr)
    }

    fun set(evar: REvar, expr: RExpr) {
        myStateStack.last().put(evar, expr)
    }

    fun get(svar: RSvar): RExpr? {
        return myStateStack.last().get(svar)
    }

    fun get(tvar: RTvar): RExpr? {
        return myStateStack.last().get(tvar)
    }


    fun get(evar: REvar): RExpr? {
        return myStateStack.last().get(evar)
    }
}
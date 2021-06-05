package parser

data class Input(
    private val str: String,
    private var pos: Int = 0
) {
    private var myCurrLine = 0
    private var myCurrColumn = 0

    private val myStates = mutableListOf<Input>()

    fun beginTx() {
        val inpCopy = Input(str, pos)
        inpCopy.myCurrLine = myCurrLine
        inpCopy.myCurrColumn = myCurrColumn
        myStates.add(inpCopy)
    }

    fun rollbackTx() {
        val last = myStates.removeLast()
        myCurrColumn = last.myCurrColumn
        myCurrLine = last.myCurrLine
        pos = last.pos
    }

    fun finishTx() {
        myStates.removeLast()
    }

    fun next(): Char {
        if (str[pos] == '\n') {
            myCurrColumn = 0
            myCurrLine += 1
        } else {
            myCurrColumn += 1
        }
        return str[pos++]
    }

    fun peek(): Char {
        return str[pos]
    }

    fun incr() {
        if (str[pos] == '\n') {
            myCurrColumn = 0
            myCurrLine += 1
        } else {
            myCurrColumn += 1
        }
        pos++
    }

    fun eof(): Boolean {
        return pos >= str.length
    }

    fun getColumn(): Int {
        return myCurrColumn
    }

    fun getLine(): Int {
        return myCurrLine
    }
}
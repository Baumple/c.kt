import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import java.io.File
import kotlin.system.exitProcess


enum class TokenType {
    TOKEN_NAME,
    TOKEN_OPAREN,
    TOKEN_CPAREN,
    TOKEN_OCURLY,
    TOKEN_CCURLY,
    TOKEN_NUMBER,
    TOKEN_STRING,
    TOKEN_RETURN,
}

data class Loc(val fileName: String, val row: Int, val col: Int) {
    override fun toString(): String = "$fileName:$row:$col"
}

data class Token(val type: TokenType, val text: String, val loc: Loc)

class Lexer(val sourceName: String, val source: String) {
    var cur = 0
    var bol = 0
    var row = 0

    fun hasNext() = cur < source.length

    fun chopChar() {
        if (hasNext()) {
            val x = source[cur]
            cur += 1

            if (x == '\n') {
                this.bol = cur
                this.row += 1

            }
        }
    }

    fun trimLeft() {
        while (this.hasNext() && this.source[cur].isWhitespace()) {
            this.chopChar()
        }
    }

    fun curLoc(): Loc {
        return Loc(this.sourceName, this.cur, this.row)
    }

    fun nextToken(): Token? {
        this.trimLeft()
        while (this.hasNext() && this.source[cur] == '#') {
            this.dropLine()
            this.trimLeft()
        }

        if (!this.hasNext()) {
            return null
        }

        if (this.source[cur].isAlpha()) {
            val index = cur
            while (this.hasNext() and this.source[cur].isAlpha()) {
                this.chopChar()
            }

            return Token(TokenType.TOKEN_NAME, source.slice(index until cur), curLoc())
        }

        // todo("nextToken")
        return null
    }

    private fun dropLine() {
        while (this.hasNext() && this.source[cur] != '\n') {
            this.chopChar()
        }

        if (!this.hasNext()) this.chopChar()
    }

    companion object Builder {
        fun fromFile(fileName: String) = Lexer(fileName, File(fileName).readText())
    }
}

fun test() {
    exitProcess(0)
}

fun main() {
    // test()
    val fileName = "hello.c"
    val lexer = Lexer.fromFile(fileName)

    println("Size of file: ${lexer.source.length}")

    var token = lexer.nextToken()

    while (token != null) {
        println(token)
        println(token.loc)
        token = lexer.nextToken()
    }

}

fun todo(what: String) {
    error("TODO: $what not yet implemented")
}

fun Char.isAlpha(): Boolean {
    return !(this !in 'A'..'Z' && this !in 'a'..'z' && this !in '0'..'9')
}
fun<T> dbg(x: T): T {
    println(x)
    return x
}
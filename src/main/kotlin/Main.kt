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
    TOKEN_SCOLON,
}

data class Loc(val fileName: String, val row: Int, val col: Int) {
    override fun toString(): String = "$fileName:${row + 1}:${col + 1}"
}

data class Token(val type: TokenType, val value: String, val loc: Loc)

class Lexer(val sourceName: String, val source: String) {
    private var cur = 0
    private var bol = 0
    private var row = 0
    
    fun hasNext() = cur < source.length
    
    private fun chopChar() {
        if (hasNext()) {
            val c = source[cur]
            cur += 1
            
            if (c == '\n') {
                bol = cur
                row += 1
            }
        }
    }
    
    private fun trimLeft() {
        while (hasNext() && source[cur].isWhitespace()) {
            chopChar()
        }
    }
    
    fun curLoc(): Loc {
        return Loc(sourceName, row, cur - bol)
    }
    
    fun nextToken(): Token? {
        trimLeft()
        while (hasNext() && source[cur] == '#') {
            dropLine()
            trimLeft()
        }
        
        if (!hasNext()) {
            return null
        }
        
        val loc = curLoc()
        val firstc = source[cur]
        
        if (firstc.isLetter()) {
            val index = cur
            while (hasNext() && source[cur].isLetterOrDigit()) {
                chopChar()
            }
            return Token(TokenType.TOKEN_NAME, source.slice(index until cur), loc)
        }
        
        val type = when (firstc) {
            '(' -> TokenType.TOKEN_OPAREN
            ')' -> TokenType.TOKEN_CPAREN
            '{' -> TokenType.TOKEN_OCURLY
            '}' -> TokenType.TOKEN_CCURLY
            ';' -> TokenType.TOKEN_SCOLON
            else -> null
        }
        if (type != null) {
            chopChar()
            return Token(type, firstc.toString(), loc)
        }
        
        if (firstc == '"') {
            chopChar()
            val start = cur
            while (hasNext() && source[cur] != '"') {
                chopChar()
            }
            if (hasNext()) {
                val text = source.slice(start until cur)
                chopChar()
                return Token(TokenType.TOKEN_STRING, text, loc)
            }
            
            println("ERROR: Unclosed String literal at ${Loc(sourceName, row, start - bol)}")
            return null
        }
        
        if (firstc.isDigit()) {
            val start = cur
            
            chopChar()
            while (hasNext() && source[cur].isDigit()) {
                chopChar()
            }
            
            val text = source.slice(start until cur)
            chopChar()
            
            return Token(TokenType.TOKEN_NUMBER, text, loc)
        }
        
        TODO("nextToken: $loc")
        return null
    }
    
    private fun dropLine() {
        while (hasNext() && source[cur] != '\n') {
            chopChar()
        }
        
        if (!hasNext()) chopChar()
    }
    
    companion object Builder {
        fun fromFile(fileName: String) = Lexer(fileName, File(fileName).readText())
        fun fromFile(file: File) = Lexer(file.name, file.readText())
    }
}

fun test() {
    exitProcess(0)
}

enum class StatementType {
    STMT_FUNCALL,
    STMT_RETURN
}


class FuncallStmt : Statement {

}

class RetStmt : Statement {

}

interface Statement {

}

class Func(val name: String, val body: List<Statement>) {

}

enum class Type {
    TYPE_INT,
}

fun expect_token(lexer: Lexer, tokenType: TokenType): Token? {
    val token = lexer.nextToken()
    
    if (token == null) {
        println("ERROR: Expected $tokenType but got EOF: ${lexer.curLoc()}")
        
        return null
    }
    
    if (token.type != tokenType) {
        println("ERROR: Expected '$tokenType' but got EOF: ${lexer.curLoc()}")
    }
    
    return token
}

fun parseType(lexer: Lexer): Type? {
    val returnType = expect_token(lexer, TokenType.TOKEN_NAME) ?: return null
    
    if (returnType.value != "int") {
        println("Unexpected type ${returnType.value} at ${lexer.curLoc()}")
        return null
    }
    
    return Type.TYPE_INT
}

fun parse_function(lexer: Lexer): Token? {
    val returnType = parseType(lexer)
    
    return null
}

fun main() {
    // test()
    val fileName = "hello.c"
    val lexer = Lexer.fromFile(fileName)
    
    val func = parse_function(lexer)
}


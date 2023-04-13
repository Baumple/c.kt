import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

enum class TokenType {
    TOKEN_NAME, TOKEN_OPAREN, TOKEN_CPAREN, TOKEN_OCURLY, TOKEN_CCURLY, TOKEN_NUMBER, TOKEN_STRING, TOKEN_RETURN, TOKEN_SCOLON, TOKEN_COMMA, ;
    
}

data class Loc(val fileName: String, val row: Int, val col: Int) {
    override fun toString(): String = "$fileName:${row + 1}:${col + 1}"
}

data class Token(val type: TokenType, val value: String, val loc: Loc) {
    /// Turns type into corrected formatted string
    fun valueToString(): String {
        return when (this.type) {
            TokenType.TOKEN_NUMBER -> this.value
            TokenType.TOKEN_STRING -> "\"${this.value}\""
            else -> error("")
        }
    }
}

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
            val text = source.slice(index until cur)
            if (text == "return") return Token(TokenType.TOKEN_RETURN, text, loc)
            
            return Token(TokenType.TOKEN_NAME, text, loc)
        }
        
        val type = when (firstc) {
            '(' -> TokenType.TOKEN_OPAREN
            ')' -> TokenType.TOKEN_CPAREN
            '{' -> TokenType.TOKEN_OCURLY
            '}' -> TokenType.TOKEN_CCURLY
            ';' -> TokenType.TOKEN_SCOLON
            ',' -> TokenType.TOKEN_COMMA
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
            
            return Token(TokenType.TOKEN_NUMBER, text, loc)
        }
        
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

interface Statement {
    val type: StatementType
    override fun toString(): String
}

enum class StatementType {
    Funcall, Return
}

class FuncallStmt(val name: Token, val args: List<Token>) : Statement {
    override val type: StatementType = StatementType.Funcall
    override fun toString(): String = "name: '$name', number_of_args: ${args.size}"
}

class RetStmt(val expr: Token) : Statement {
    override val type: StatementType = StatementType.Return
    override fun toString(): String = "return statement: $expr "
}

class Func(val name: String, val body: List<Statement>) {
    override fun toString(): String = "name='$name', number_of_statements: ${body.size}"
}

enum class Type {
    TYPE_INT,
}

fun expectToken(lexer: Lexer, vararg expectedTokens: TokenType): Token? {
    val token = lexer.nextToken()
    
    if (token == null) {
        println("ERROR: Expected one of the following tokens: ${expectedTokens.map { it.toString() }} but got EOF ${lexer.curLoc()}")
        return null
    }
    
    if (token.type !in expectedTokens) {
        println("${token.loc} ERROR: Expected '${expectedTokens.map { it.toString() }}' but got ${token.type}:'${token.value}'")
    }
    
    return token
}

fun parseType(lexer: Lexer): Type? {
    val returnType = expectToken(lexer, TokenType.TOKEN_NAME) ?: return null
    
    if (returnType.value != "int") {
        println("Unexpected type '${returnType.value}' at ${lexer.curLoc()}")
        return null
    }
    
    return Type.TYPE_INT
}


fun parseFunction(lexer: Lexer): Func? {
    val returnType = parseType(lexer) ?: return null
    assert(returnType == Type.TYPE_INT)
    
    val name = expectToken(lexer, TokenType.TOKEN_NAME) ?: return null
    
    expectToken(lexer, TokenType.TOKEN_OPAREN) ?: return null
    expectToken(lexer, TokenType.TOKEN_CPAREN) ?: return null
    
    val body = parseBody(lexer) ?: return null
    return Func(name.value, body)
}

fun parseBody(lexer: Lexer): List<Statement>? {
    expectToken(lexer, TokenType.TOKEN_OCURLY) ?: return null
    
    val block = mutableListOf<Statement>()
    
    while (true) {
        val name = expectToken(
            lexer,
            TokenType.TOKEN_RETURN,
            TokenType.TOKEN_NAME,
            TokenType.TOKEN_CCURLY,
        ) ?: return null
        
        when (name.type) {
            TokenType.TOKEN_CCURLY -> {
                break
            }
            
            TokenType.TOKEN_RETURN -> {
                val token = expectToken(lexer, TokenType.TOKEN_NUMBER) ?: return null
                block.add(RetStmt(token))
            }
            
            TokenType.TOKEN_NAME -> {
                val arglist = parseArgList(lexer) ?: return null
                block.add(FuncallStmt(name, arglist))
            }
            
            else -> exitProcess(69)
        }
        expectToken(lexer, TokenType.TOKEN_SCOLON) ?: return null
        
    }
    return block
}

fun parseArgList(lexer: Lexer): List<Token>? {
    expectToken(lexer, TokenType.TOKEN_OPAREN) ?: return null
    val funArgs = mutableListOf<Token>()
    
    while (true) {
        val token = expectToken(
            lexer,
            TokenType.TOKEN_NUMBER,
            TokenType.TOKEN_STRING,
            TokenType.TOKEN_NAME,
            TokenType.TOKEN_CPAREN,
            TokenType.TOKEN_COMMA
        ) ?: return null
        
        when (token.type) {
            TokenType.TOKEN_CPAREN -> break
            TokenType.TOKEN_COMMA -> continue
            else -> funArgs.add(token)
        }
    }
    return funArgs
}

fun main() {
    val fileName = "hello.c"
    val lexer = Lexer.fromFile(fileName)
    
    val func = parseFunction(lexer) ?: error("Failed to compile $fileName")
    
    val compiledScript = mutableListOf<String>()
    for (stmt in func.body) {
        when (stmt.type) {
            StatementType.Funcall -> {
                val stmt = stmt as FuncallStmt
                if (stmt.name.value == "printf") {
                    compiledScript.add("print(${stmt.args.joinToString { it.valueToString() }})")
                } else {
                    println("${stmt.name.loc} ERROR: Call to unknown function: '${stmt.name.value}'")
                    exitProcess(69)
                }
            }
            
            StatementType.Return -> {
            
            }
        }
    }
    File("output.py").also { it.createNewFile() }.bufferedWriter().use { writer ->
        writer.write(compiledScript.joinToString(separator = "\n") { it })
    }
    
}

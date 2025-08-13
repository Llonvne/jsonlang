package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.source.SourceLocationBuilder
import cn.llonvne.jsonlang.core.errors.CompilerError
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.ErrorSeverity
import cn.llonvne.jsonlang.core.errors.ErrorCategory

/**
 * 词法分析器接口
 * 定义了词法分析器的基本功能
 */
interface Lexer {
    /**
     * 获取下一个 Token
     * @return 下一个 Token，如果到达文件末尾则返回 EOF Token
     */
    fun nextToken(): Token
    
    /**
     * 查看下一个 Token 但不消费它
     * @return 下一个 Token
     */
    fun peekToken(): Token
    
    /**
     * 查看指定偏移量的 Token 但不消费它
     * @param offset 偏移量，0 表示当前 Token，1 表示下一个 Token
     * @return 指定位置的 Token
     */
    fun peekToken(offset: Int): Token
    
    /**
     * 检查是否已到达文件末尾
     * @return 如果到达文件末尾则返回 true
     */
    fun isAtEnd(): Boolean
    
    /**
     * 获取当前位置信息
     * @return 当前源码位置
     */
    fun getCurrentLocation(): SourceLocation
    
    /**
     * 获取错误报告器
     * @return 错误报告器实例
     */
    fun getErrorReporter(): ErrorReporter
}

/**
 * JSONLang 词法分析器实现
 * 支持 UTF-8 和中文字符，提供位置信息跟踪和错误恢复机制
 */
class JSONLangLexer(
    private val source: String,
    private val filename: String = "<unknown>",
    private val errorReporter: ErrorReporter
) : Lexer {
    
    private var current = 0
    private var line = 1
    private var column = 1
    private var locationBuilder = SourceLocationBuilder(filename)
    
    // Token 缓存，用于 peek 操作
    private val tokenCache = mutableListOf<Token>()
    
    // 关键字映射表
    private val keywords = mapOf(
        // 英文关键字
        "metadata" to TokenType.METADATA,
        "imports" to TokenType.IMPORTS,
        "exports" to TokenType.EXPORTS,
        "types" to TokenType.TYPES,
        "constants" to TokenType.CONSTANTS,
        "functions" to TokenType.FUNCTIONS,
        "macros" to TokenType.MACROS,
        "dsl" to TokenType.DSL,
        "String" to TokenType.STRING,
        "Int" to TokenType.INT,
        "Long" to TokenType.LONG,
        "Float" to TokenType.FLOAT,
        "Double" to TokenType.DOUBLE,
        "Boolean" to TokenType.BOOLEAN,
        "Unit" to TokenType.UNIT,
        "Any" to TokenType.ANY,
        "Nothing" to TokenType.NOTHING,
        "null" to TokenType.NULL,
        "if" to TokenType.IF,
        "else" to TokenType.ELSE,
        "when" to TokenType.WHEN,
        "for" to TokenType.FOR,
        "while" to TokenType.WHILE,
        "do" to TokenType.DO,
        "break" to TokenType.BREAK,
        "continue" to TokenType.CONTINUE,
        "return" to TokenType.RETURN,
        "try" to TokenType.TRY,
        "catch" to TokenType.CATCH,
        "finally" to TokenType.FINALLY,
        "throw" to TokenType.THROW,
        "fun" to TokenType.FUN,
        "lambda" to TokenType.LAMBDA,
        "async" to TokenType.ASYNC,
        "await" to TokenType.AWAIT,
        "suspend" to TokenType.SUSPEND,
        "inline" to TokenType.INLINE,
        "public" to TokenType.PUBLIC,
        "private" to TokenType.PRIVATE,
        "protected" to TokenType.PROTECTED,
        "internal" to TokenType.INTERNAL,
        "abstract" to TokenType.ABSTRACT,
        "final" to TokenType.FINAL,
        "open" to TokenType.OPEN,
        "override" to TokenType.OVERRIDE,
        "virtual" to TokenType.VIRTUAL,
        "static" to TokenType.STATIC,
        "struct" to TokenType.STRUCT,
        "enum" to TokenType.ENUM,
        "union" to TokenType.UNION,
        "alias" to TokenType.ALIAS,
        "interface" to TokenType.INTERFACE,
        "class" to TokenType.CLASS,
        "import" to TokenType.IMPORT,
        "export" to TokenType.EXPORT,
        "from" to TokenType.FROM,
        "as" to TokenType.AS,
        "macro" to TokenType.MACRO,
        "quote" to TokenType.QUOTE,
        "unquote" to TokenType.UNQUOTE,
        "splice" to TokenType.SPLICE,
        "eval" to TokenType.EVAL,
        "true" to TokenType.BOOLEAN_LITERAL,
        "false" to TokenType.BOOLEAN_LITERAL,
        
        // 中文关键字
        "元数据" to TokenType.元数据,
        "导入" to TokenType.导入,
        "导出" to TokenType.导出,
        "类型" to TokenType.类型,
        "常量" to TokenType.常量,
        "函数" to TokenType.函数,
        "宏" to TokenType.宏,
        "领域语言" to TokenType.领域语言,
        "如果" to TokenType.如果,
        "否则" to TokenType.否则,
        "当" to TokenType.当,
        "循环" to TokenType.循环,
        "当循环" to TokenType.当循环,
        "执行" to TokenType.执行,
        "跳出" to TokenType.跳出,
        "继续" to TokenType.继续,
        "返回" to TokenType.返回,
        "尝试" to TokenType.尝试,
        "捕获" to TokenType.捕获,
        "最终" to TokenType.最终,
        "抛出" to TokenType.抛出
    )
    
    override fun nextToken(): Token {
        if (tokenCache.isNotEmpty()) {
            return tokenCache.removeAt(0)
        }
        
        return scanToken()
    }
    
    override fun peekToken(): Token {
        return peekToken(0)
    }
    
    override fun peekToken(offset: Int): Token {
        // 确保缓存中有足够的 Token
        while (tokenCache.size <= offset) {
            val token = scanToken()
            tokenCache.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        return if (offset < tokenCache.size) {
            tokenCache[offset]
        } else {
            tokenCache.lastOrNull() ?: TokenFactory.eof(getCurrentLocation())
        }
    }
    
    override fun isAtEnd(): Boolean {
        return current >= source.length
    }
    
    override fun getCurrentLocation(): SourceLocation {
        return SourceLocation.point(filename, line, column, current)
    }
    
    override fun getErrorReporter(): ErrorReporter {
        return errorReporter
    }
    
    /**
     * 扫描下一个 Token
     */
    private fun scanToken(): Token {
        skipWhitespace()
        
        if (isAtEnd()) {
            return TokenFactory.eof(getCurrentLocation())
        }
        
        val start = current
        val startLine = line
        val startColumn = column
        val c = advance()
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        return when (c) {
            // JSON 基础字符
            '{' -> TokenFactory.operator(TokenType.LEFT_BRACE, "{", location)
            '}' -> TokenFactory.operator(TokenType.RIGHT_BRACE, "}", location)
            '[' -> TokenFactory.operator(TokenType.LEFT_BRACKET, "[", location)
            ']' -> TokenFactory.operator(TokenType.RIGHT_BRACKET, "]", location)
            ',' -> TokenFactory.operator(TokenType.COMMA, ",", location)
            ':' -> {
                if (match(':')) {
                    TokenFactory.operator(TokenType.DOUBLE_COLON, "::", location)
                } else {
                    TokenFactory.operator(TokenType.COLON, ":", location)
                }
            }
            ';' -> TokenFactory.operator(TokenType.SEMICOLON, ";", location)
            '.' -> TokenFactory.operator(TokenType.DOT, ".", location)
            '?' -> TokenFactory.operator(TokenType.QUESTION, "?", location)
            
            // 算术操作符
            '+' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.PLUS_ASSIGN, "+=", location)
                    else -> TokenFactory.operator(TokenType.PLUS, "+", location)
                }
            }
            '-' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.MINUS_ASSIGN, "-=", location)
                    match('>') -> TokenFactory.operator(TokenType.ARROW, "->", location)
                    else -> TokenFactory.operator(TokenType.MINUS, "-", location)
                }
            }
            '*' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.MULTIPLY_ASSIGN, "*=", location)
                    match('*') -> TokenFactory.operator(TokenType.POWER, "**", location)
                    else -> TokenFactory.operator(TokenType.MULTIPLY, "*", location)
                }
            }
            '/' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.DIVIDE_ASSIGN, "/=", location)
                    match('/') -> {
                        // 单行注释
                        skipLineComment()
                        scanToken() // 递归获取下一个 Token
                    }
                    match('*') -> {
                        // 多行注释
                        skipBlockComment()
                        scanToken() // 递归获取下一个 Token
                    }
                    else -> TokenFactory.operator(TokenType.DIVIDE, "/", location)
                }
            }
            '%' -> TokenFactory.operator(TokenType.MODULO, "%", location)
            
            // 比较操作符
            '=' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.EQUAL, "==", location)
                    match('>') -> TokenFactory.operator(TokenType.FAT_ARROW, "=>", location)
                    else -> TokenFactory.operator(TokenType.ASSIGN, "=", location)
                }
            }
            '!' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.NOT_EQUAL, "!=", location)
                    else -> TokenFactory.operator(TokenType.EXCLAMATION, "!", location)
                }
            }
            '<' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.LESS_EQUAL, "<=", location)
                    match('<') -> TokenFactory.operator(TokenType.LEFT_SHIFT, "<<", location)
                    else -> TokenFactory.operator(TokenType.LESS_THAN, "<", location)
                }
            }
            '>' -> {
                when {
                    match('=') -> TokenFactory.operator(TokenType.GREATER_EQUAL, ">=", location)
                    match('>') -> TokenFactory.operator(TokenType.RIGHT_SHIFT, ">>", location)
                    else -> TokenFactory.operator(TokenType.GREATER_THAN, ">", location)
                }
            }
            
            // 逻辑操作符
            '&' -> {
                when {
                    match('&') -> TokenFactory.operator(TokenType.AND, "&&", location)
                    else -> TokenFactory.operator(TokenType.BIT_AND, "&", location)
                }
            }
            '|' -> {
                when {
                    match('|') -> TokenFactory.operator(TokenType.OR, "||", location)
                    else -> TokenFactory.operator(TokenType.BIT_OR, "|", location)
                }
            }
            '^' -> TokenFactory.operator(TokenType.BIT_XOR, "^", location)
            '~' -> TokenFactory.operator(TokenType.BIT_NOT, "~", location)
            
            // 字符串字面量
            '"' -> scanString()
            '\'' -> scanString('\'')
            
            // 数字字面量
            in '0'..'9' -> scanNumber()
            
            // 标识符和关键字
            else -> {
                if (isAlpha(c) || c == '_' || isChineseCharacter(c)) {
                    scanIdentifier()
                } else {
                    reportError("意外的字符: '$c'", location)
                    TokenFactory.error(c.toString(), location)
                }
            }
        }
    }
    
    /**
     * 扫描字符串字面量
     */
    private fun scanString(quote: Char = '"'): Token {
        val start = current - 1
        val startLine = line
        val startColumn = column - 1
        val value = StringBuilder()
        
        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\n') {
                line++
                column = 1
            } else if (peek() == '\\') {
                advance() // 消费反斜杠
                if (!isAtEnd()) {
                    val escaped = advance()
                    when (escaped) {
                        'n' -> value.append('\n')
                        't' -> value.append('\t')
                        'r' -> value.append('\r')
                        '\\' -> value.append('\\')
                        '"' -> value.append('"')
                        '\'' -> value.append('\'')
                        'u' -> {
                            // Unicode 转义序列
                            val unicode = scanUnicodeEscape()
                            if (unicode != null) {
                                value.append(unicode)
                            } else {
                                value.append("\\u")
                            }
                        }
                        else -> {
                            value.append('\\')
                            value.append(escaped)
                        }
                    }
                }
            } else {
                value.append(advance())
            }
        }
        
        if (isAtEnd()) {
            val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
            reportError("未终止的字符串", location)
            return TokenFactory.error(source.substring(start, current), location)
        }
        
        // 消费结束引号
        advance()
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        return TokenFactory.stringLiteral(lexeme, value.toString(), location)
    }
    
    /**
     * 扫描 Unicode 转义序列
     */
    private fun scanUnicodeEscape(): Char? {
        val digits = StringBuilder()
        repeat(4) {
            if (!isAtEnd() && isHexDigit(peek())) {
                digits.append(advance())
            } else {
                return null
            }
        }
        
        return try {
            digits.toString().toInt(16).toChar()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * 扫描数字字面量
     */
    private fun scanNumber(): Token {
        val start = current - 1
        val startLine = line
        val startColumn = column - 1
        
        // 扫描整数部分
        while (isDigit(peek())) {
            advance()
        }
        
        var isFloat = false
        
        // 检查小数点
        if (peek() == '.' && isDigit(peekNext())) {
            isFloat = true
            advance() // 消费小数点
            
            // 扫描小数部分
            while (isDigit(peek())) {
                advance()
            }
        }
        
        // 检查科学计数法
        if (peek() == 'e' || peek() == 'E') {
            isFloat = true
            advance() // 消费 e/E
            
            if (peek() == '+' || peek() == '-') {
                advance() // 消费符号
            }
            
            if (!isDigit(peek())) {
                val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
                reportError("无效的数字格式", location)
                return TokenFactory.error(source.substring(start, current), location)
            }
            
            while (isDigit(peek())) {
                advance()
            }
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        
        return try {
            if (isFloat) {
                val value = lexeme.toDouble()
                TokenFactory.floatLiteral(lexeme, value, location)
            } else {
                val value = lexeme.toLong()
                TokenFactory.integerLiteral(lexeme, value, location)
            }
        } catch (e: NumberFormatException) {
            reportError("无效的数字格式: $lexeme", location)
            TokenFactory.error(lexeme, location)
        }
    }
    
    /**
     * 扫描标识符或关键字
     */
    private fun scanIdentifier(): Token {
        val start = current - 1
        val startLine = line
        val startColumn = column - 1
        
        while (isAlphaNumeric(peek()) || peek() == '_' || isChineseCharacter(peek())) {
            advance()
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        
        // 检查是否为关键字
        val tokenType = keywords[lexeme]
        
        return if (tokenType != null) {
            if (tokenType == TokenType.BOOLEAN_LITERAL) {
                val value = lexeme == "true"
                TokenFactory.booleanLiteral(lexeme, value, location)
            } else {
                TokenFactory.keyword(tokenType, lexeme, location)
            }
        } else {
            TokenFactory.identifier(lexeme, location)
        }
    }
    
    /**
     * 跳过空白字符
     */
    private fun skipWhitespace() {
        while (!isAtEnd()) {
            when (peek()) {
                ' ', '\r', '\t' -> advance()
                '\n' -> {
                    line++
                    column = 1
                    advance()
                }
                else -> break
            }
        }
    }
    
    /**
     * 跳过单行注释
     */
    private fun skipLineComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance()
        }
    }
    
    /**
     * 跳过多行注释
     */
    private fun skipBlockComment() {
        var depth = 1
        
        while (!isAtEnd() && depth > 0) {
            if (peek() == '/' && peekNext() == '*') {
                advance()
                advance()
                depth++
            } else if (peek() == '*' && peekNext() == '/') {
                advance()
                advance()
                depth--
            } else {
                if (peek() == '\n') {
                    line++
                    column = 1
                }
                advance()
            }
        }
        
        if (depth > 0) {
            reportError("未终止的块注释", getCurrentLocation())
        }
    }
    
    /**
     * 前进一个字符
     */
    private fun advance(): Char {
        if (isAtEnd()) return '\u0000'
        column++
        return source[current++]
    }
    
    /**
     * 查看当前字符但不消费
     */
    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return source[current]
    }
    
    /**
     * 查看下一个字符但不消费
     */
    private fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }
    
    /**
     * 匹配指定字符，如果匹配则消费
     */
    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current++
        column++
        return true
    }
    
    /**
     * 检查字符是否为字母
     */
    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z'
    }
    
    /**
     * 检查字符是否为数字
     */
    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }
    
    /**
     * 检查字符是否为十六进制数字
     */
    private fun isHexDigit(c: Char): Boolean {
        return c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
    }
    
    /**
     * 检查字符是否为字母或数字
     */
    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }
    
    /**
     * 检查字符是否为中文字符
     */
    private fun isChineseCharacter(c: Char): Boolean {
        val codePoint = c.code
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF) ||  // CJK 统一汉字
               (codePoint >= 0x3400 && codePoint <= 0x4DBF) ||  // CJK 扩展 A
               (codePoint >= 0x20000 && codePoint <= 0x2A6DF) || // CJK 扩展 B
               (codePoint >= 0x2A700 && codePoint <= 0x2B73F) || // CJK 扩展 C
               (codePoint >= 0x2B740 && codePoint <= 0x2B81F) || // CJK 扩展 D
               (codePoint >= 0x2B820 && codePoint <= 0x2CEAF) || // CJK 扩展 E
               (codePoint >= 0x2CEB0 && codePoint <= 0x2EBEF)    // CJK 扩展 F
    }
    
    /**
     * 报告错误
     */
    private fun reportError(message: String, location: SourceLocation) {
        val error = CompilerError(
            code = "LEXER_ERROR",
            message = message,
            severity = ErrorSeverity.ERROR,
            category = ErrorCategory.SYNTAX,
            location = location
        )
        errorReporter.report(error)
    }
}
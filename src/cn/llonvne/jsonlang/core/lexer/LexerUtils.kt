package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 词法分析器工具类
 * 提供词法分析过程中的辅助功能
 */
object LexerUtils {
    
    /**
     * 检查字符是否为有效的标识符起始字符
     */
    fun isIdentifierStart(c: Char): Boolean {
        return c.isLetter() || c == '_' || isChineseCharacter(c)
    }
    
    /**
     * 检查字符是否为有效的标识符字符
     */
    fun isIdentifierPart(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || isChineseCharacter(c)
    }
    
    /**
     * 检查字符是否为中文字符
     */
    fun isChineseCharacter(c: Char): Boolean {
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
     * 检查字符是否为空白字符
     */
    fun isWhitespace(c: Char): Boolean {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n'
    }
    
    /**
     * 检查字符是否为换行符
     */
    fun isNewline(c: Char): Boolean {
        return c == '\n' || c == '\r'
    }
    
    /**
     * 检查字符是否为数字
     */
    fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }
    
    /**
     * 检查字符是否为十六进制数字
     */
    fun isHexDigit(c: Char): Boolean {
        return c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
    }
    
    /**
     * 检查字符是否为八进制数字
     */
    fun isOctalDigit(c: Char): Boolean {
        return c in '0'..'7'
    }
    
    /**
     * 检查字符是否为二进制数字
     */
    fun isBinaryDigit(c: Char): Boolean {
        return c == '0' || c == '1'
    }
    
    /**
     * 转义字符映射表
     */
    val escapeCharacters = mapOf(
        'n' to '\n',
        't' to '\t',
        'r' to '\r',
        'b' to '\b',
        'f' to '\u000C',
        '\\' to '\\',
        '"' to '"',
        '\'' to '\'',
        '0' to '\u0000'
    )
    
    /**
     * 解析转义字符
     */
    fun parseEscapeCharacter(c: Char): Char? {
        return escapeCharacters[c]
    }
    
    /**
     * 解析 Unicode 转义序列
     */
    fun parseUnicodeEscape(digits: String): Char? {
        if (digits.length != 4) return null
        
        return try {
            digits.toInt(16).toChar()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * 检查字符串是否为有效的数字
     */
    fun isValidNumber(str: String): Boolean {
        if (str.isEmpty()) return false
        
        var i = 0
        var hasDigits = false
        var hasDot = false
        var hasExponent = false
        
        // 检查符号
        if (str[i] == '+' || str[i] == '-') {
            i++
        }
        
        // 检查数字部分
        while (i < str.length) {
            val c = str[i]
            when {
                c.isDigit() -> {
                    hasDigits = true
                    i++
                }
                c == '.' && !hasDot && !hasExponent -> {
                    hasDot = true
                    i++
                }
                (c == 'e' || c == 'E') && !hasExponent && hasDigits -> {
                    hasExponent = true
                    i++
                    // 检查指数符号
                    if (i < str.length && (str[i] == '+' || str[i] == '-')) {
                        i++
                    }
                    // 指数部分必须有数字
                    if (i >= str.length || !str[i].isDigit()) {
                        return false
                    }
                }
                else -> return false
            }
        }
        
        return hasDigits
    }
    
    /**
     * 获取操作符的字符串表示
     */
    fun getOperatorString(type: TokenType): String? {
        return when (type) {
            TokenType.PLUS -> "+"
            TokenType.MINUS -> "-"
            TokenType.MULTIPLY -> "*"
            TokenType.DIVIDE -> "/"
            TokenType.MODULO -> "%"
            TokenType.POWER -> "**"
            TokenType.EQUAL -> "=="
            TokenType.NOT_EQUAL -> "!="
            TokenType.LESS_THAN -> "<"
            TokenType.LESS_EQUAL -> "<="
            TokenType.GREATER_THAN -> ">"
            TokenType.GREATER_EQUAL -> ">="
            TokenType.AND -> "&&"
            TokenType.OR -> "||"
            TokenType.NOT -> "!"
            TokenType.ASSIGN -> "="
            TokenType.PLUS_ASSIGN -> "+="
            TokenType.MINUS_ASSIGN -> "-="
            TokenType.MULTIPLY_ASSIGN -> "*="
            TokenType.DIVIDE_ASSIGN -> "/="
            TokenType.BIT_AND -> "&"
            TokenType.BIT_OR -> "|"
            TokenType.BIT_XOR -> "^"
            TokenType.BIT_NOT -> "~"
            TokenType.LEFT_SHIFT -> "<<"
            TokenType.RIGHT_SHIFT -> ">>"
            TokenType.ARROW -> "->"
            TokenType.FAT_ARROW -> "=>"
            TokenType.DOUBLE_COLON -> "::"
            else -> null
        }
    }
    
    /**
     * 检查 Token 类型是否为二元操作符
     */
    fun isBinaryOperator(type: TokenType): Boolean {
        return when (type) {
            TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE,
            TokenType.MODULO, TokenType.POWER, TokenType.EQUAL, TokenType.NOT_EQUAL,
            TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN,
            TokenType.GREATER_EQUAL, TokenType.AND, TokenType.OR,
            TokenType.BIT_AND, TokenType.BIT_OR, TokenType.BIT_XOR,
            TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT -> true
            else -> false
        }
    }
    
    /**
     * 检查 Token 类型是否为一元操作符
     */
    fun isUnaryOperator(type: TokenType): Boolean {
        return when (type) {
            TokenType.PLUS, TokenType.MINUS, TokenType.NOT, TokenType.BIT_NOT -> true
            else -> false
        }
    }
    
    /**
     * 检查 Token 类型是否为赋值操作符
     */
    fun isAssignmentOperator(type: TokenType): Boolean {
        return when (type) {
            TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
            TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN -> true
            else -> false
        }
    }
}

/**
 * 词法分析器错误恢复策略
 */
class LexerErrorRecovery {
    
    /**
     * 同步点 Token 类型
     * 这些 Token 通常标志着语句或声明的开始，可以作为错误恢复的同步点
     */
    private val synchronizationTokens = setOf(
        TokenType.METADATA,
        TokenType.IMPORTS,
        TokenType.EXPORTS,
        TokenType.TYPES,
        TokenType.CONSTANTS,
        TokenType.FUNCTIONS,
        TokenType.MACROS,
        TokenType.DSL,
        TokenType.IF,
        TokenType.WHILE,
        TokenType.FOR,
        TokenType.TRY,
        TokenType.RETURN,
        TokenType.LEFT_BRACE,
        TokenType.RIGHT_BRACE,
        TokenType.SEMICOLON
    )
    
    /**
     * 检查 Token 是否为同步点
     */
    fun isSynchronizationToken(type: TokenType): Boolean {
        return type in synchronizationTokens
    }
    
    /**
     * 跳过 Token 直到找到同步点
     */
    fun skipToSynchronizationPoint(lexer: Lexer): Token {
        var token = lexer.nextToken()
        
        while (token.type != TokenType.EOF && !isSynchronizationToken(token.type)) {
            token = lexer.nextToken()
        }
        
        return token
    }
    
    /**
     * 尝试从词法错误中恢复
     */
    fun recoverFromError(lexer: Lexer, errorLocation: SourceLocation): Token {
        // 跳过当前错误字符
        var token = lexer.nextToken()
        
        // 如果下一个 Token 也是错误，继续跳过
        while (token.type == TokenType.ERROR && token.type != TokenType.EOF) {
            token = lexer.nextToken()
        }
        
        return token
    }
    
    /**
     * 检查是否可能是不完整的 Token
     */
    fun isIncompleteToken(lexeme: String, location: SourceLocation): Boolean {
        return when {
            lexeme.startsWith("\"") && !lexeme.endsWith("\"") -> true
            lexeme.startsWith("'") && !lexeme.endsWith("'") -> true
            lexeme.startsWith("/*") && !lexeme.endsWith("*/") -> true
            lexeme.matches(Regex("\\d+\\.")) -> true // 不完整的浮点数
            lexeme.matches(Regex("\\d+[eE][+-]?")) -> true // 不完整的科学计数法
            else -> false
        }
    }
    
    /**
     * 建议可能的修复方案
     */
    fun suggestFix(lexeme: String, location: SourceLocation): String? {
        return when {
            lexeme.startsWith("\"") && !lexeme.endsWith("\"") -> 
                "可能缺少结束引号 \""
            lexeme.startsWith("'") && !lexeme.endsWith("'") -> 
                "可能缺少结束引号 '"
            lexeme.startsWith("/*") && !lexeme.endsWith("*/") -> 
                "可能缺少注释结束标记 */"
            lexeme.matches(Regex("\\d+\\.")) -> 
                "不完整的浮点数，请添加小数部分"
            lexeme.matches(Regex("\\d+[eE][+-]?")) -> 
                "不完整的科学计数法，请添加指数部分"
            else -> null
        }
    }
}
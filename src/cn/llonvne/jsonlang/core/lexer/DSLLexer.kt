package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.ErrorSeverity
import cn.llonvne.jsonlang.core.errors.ErrorCategory
import cn.llonvne.jsonlang.core.errors.CompilerError

/**
 * DSL 词法分析器
 * 支持多种领域特定语言的语法解析
 */
class DSLLexer(
    private val dslType: DSLType,
    private val source: String,
    private val filename: String = "<unknown>",
    private val errorReporter: ErrorReporter
) {
    
    private var current = 0
    private var line = 1
    private var column = 1
    
    /**
     * DSL 类型枚举
     */
    enum class DSLType {
        SQL,
        HTML,
        CSS,
        REGEX,
        JSON_SCHEMA,
        GRAPHQL,
        MARKDOWN
    }
    
    /**
     * 扫描 DSL Token
     */
    fun scanDSLTokens(): List<Token> {
        val tokens = mutableListOf<Token>()
        
        while (!isAtEnd()) {
            val token = scanDSLToken()
            if (token != null) {
                tokens.add(token)
            }
        }
        
        return tokens
    }
    
    /**
     * 扫描单个 DSL Token
     */
    private fun scanDSLToken(): Token? {
        skipWhitespace()
        
        if (isAtEnd()) return null
        
        return when (dslType) {
            DSLType.SQL -> scanSQLToken()
            DSLType.HTML -> scanHTMLToken()
            DSLType.CSS -> scanCSSToken()
            DSLType.REGEX -> scanRegexToken()
            DSLType.JSON_SCHEMA -> scanJSONSchemaToken()
            DSLType.GRAPHQL -> scanGraphQLToken()
            DSLType.MARKDOWN -> scanMarkdownToken()
        }
    }
    
    /**
     * 扫描 SQL Token
     */
    private fun scanSQLToken(): Token? {
        val start = current
        val startLine = line
        val startColumn = column
        
        val c = peek()
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        return when {
            c.isLetter() -> {
                val identifier = scanIdentifier()
                val upperIdentifier = identifier.uppercase()
                
                when (upperIdentifier) {
                    "SELECT" -> TokenFactory.keyword(TokenType.SQL_SELECT, identifier, location)
                    "FROM" -> TokenFactory.keyword(TokenType.SQL_FROM, identifier, location)
                    "WHERE" -> TokenFactory.keyword(TokenType.SQL_WHERE, identifier, location)
                    "JOIN", "INNER", "LEFT", "RIGHT", "OUTER" -> 
                        TokenFactory.keyword(TokenType.IDENTIFIER, identifier, location)
                    "GROUP", "BY", "ORDER", "HAVING" -> 
                        TokenFactory.keyword(TokenType.IDENTIFIER, identifier, location)
                    "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER" -> 
                        TokenFactory.keyword(TokenType.IDENTIFIER, identifier, location)
                    else -> TokenFactory.identifier(identifier, location)
                }
            }
            c == '\'' -> scanSQLString()
            c == '"' -> scanSQLString('"')
            c.isDigit() -> scanNumber()
            c == '(' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACKET, "(", location)
            }
            c == ')' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACKET, ")", location)
            }
            c == ',' -> {
                advance()
                TokenFactory.operator(TokenType.COMMA, ",", location)
            }
            c == ';' -> {
                advance()
                TokenFactory.operator(TokenType.SEMICOLON, ";", location)
            }
            c == '=' -> {
                advance()
                TokenFactory.operator(TokenType.EQUAL, "=", location)
            }
            c == '<' -> {
                advance()
                if (peek() == '=') {
                    advance()
                    TokenFactory.operator(TokenType.LESS_EQUAL, "<=", location)
                } else {
                    TokenFactory.operator(TokenType.LESS_THAN, "<", location)
                }
            }
            c == '>' -> {
                advance()
                if (peek() == '=') {
                    advance()
                    TokenFactory.operator(TokenType.GREATER_EQUAL, ">=", location)
                } else {
                    TokenFactory.operator(TokenType.GREATER_THAN, ">", location)
                }
            }
            else -> {
                advance()
                TokenFactory.error(c.toString(), location)
            }
        }
    }
    
    /**
     * 扫描 HTML Token
     */
    private fun scanHTMLToken(): Token? {
        val start = current
        val startLine = line
        val startColumn = column
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        val c = peek()
        
        return when {
            c == '<' -> scanHTMLTag()
            c == '>' -> {
                advance()
                TokenFactory.operator(TokenType.GREATER_THAN, ">", location)
            }
            c == '/' -> {
                advance()
                TokenFactory.operator(TokenType.DIVIDE, "/", location)
            }
            c == '=' -> {
                advance()
                TokenFactory.operator(TokenType.ASSIGN, "=", location)
            }
            c == '"' -> scanHTMLString()
            c == '\'' -> scanHTMLString('\'')
            c.isLetter() -> {
                val identifier = scanIdentifier()
                if (isHTMLTag(identifier)) {
                    TokenFactory.keyword(TokenType.HTML_TAG, identifier, location)
                } else {
                    TokenFactory.identifier(identifier, location)
                }
            }
            else -> {
                advance()
                TokenFactory.identifier(c.toString(), location)
            }
        }
    }
    
    /**
     * 扫描 CSS Token
     */
    private fun scanCSSToken(): Token? {
        val start = current
        val startLine = line
        val startColumn = column
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        val c = peek()
        
        return when {
            c == '{' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACE, "{", location)
            }
            c == '}' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACE, "}", location)
            }
            c == ':' -> {
                advance()
                TokenFactory.operator(TokenType.COLON, ":", location)
            }
            c == ';' -> {
                advance()
                TokenFactory.operator(TokenType.SEMICOLON, ";", location)
            }
            c == '.' -> {
                advance()
                TokenFactory.operator(TokenType.DOT, ".", location)
            }
            c == '#' -> {
                advance()
                TokenFactory.operator(TokenType.IDENTIFIER, "#", location)
            }
            c.isLetter() -> {
                val identifier = scanIdentifier()
                if (isCSSProperty(identifier)) {
                    TokenFactory.keyword(TokenType.CSS_PROPERTY, identifier, location)
                } else {
                    TokenFactory.identifier(identifier, location)
                }
            }
            c.isDigit() -> scanCSSValue()
            c == '"' -> scanCSSString()
            c == '\'' -> scanCSSString('\'')
            else -> {
                advance()
                TokenFactory.identifier(c.toString(), location)
            }
        }
    }
    
    /**
     * 扫描正则表达式 Token
     */
    private fun scanRegexToken(): Token? {
        val start = current
        val startLine = line
        val startColumn = column
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        val c = peek()
        
        return when {
            c == '/' -> scanRegexPattern()
            c == '^' -> {
                advance()
                TokenFactory.operator(TokenType.BIT_XOR, "^", location)
            }
            c == '$' -> {
                advance()
                TokenFactory.identifier("$", location)
            }
            c == '.' -> {
                advance()
                TokenFactory.operator(TokenType.DOT, ".", location)
            }
            c == '*' -> {
                advance()
                TokenFactory.operator(TokenType.MULTIPLY, "*", location)
            }
            c == '+' -> {
                advance()
                TokenFactory.operator(TokenType.PLUS, "+", location)
            }
            c == '?' -> {
                advance()
                TokenFactory.operator(TokenType.QUESTION, "?", location)
            }
            c == '[' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACKET, "[", location)
            }
            c == ']' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACKET, "]", location)
            }
            c == '(' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACKET, "(", location)
            }
            c == ')' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACKET, ")", location)
            }
            c == '|' -> {
                advance()
                TokenFactory.operator(TokenType.BIT_OR, "|", location)
            }
            c == '\\' -> scanRegexEscape()
            else -> {
                advance()
                TokenFactory.identifier(c.toString(), location)
            }
        }
    }
    
    /**
     * 扫描 JSON Schema Token
     */
    private fun scanJSONSchemaToken(): Token? {
        // JSON Schema 基本上是 JSON，但有特殊的关键字
        val start = current
        val startLine = line
        val startColumn = column
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        val c = peek()
        
        return when {
            c.isLetter() -> {
                val identifier = scanIdentifier()
                when (identifier) {
                    "type", "properties", "required", "items", "enum", 
                    "minimum", "maximum", "pattern", "format" -> 
                        TokenFactory.keyword(TokenType.IDENTIFIER, identifier, location)
                    else -> TokenFactory.identifier(identifier, location)
                }
            }
            else -> null // 使用基础 JSON 词法分析器
        }
    }
    
    /**
     * 扫描 GraphQL Token
     */
    private fun scanGraphQLToken(): Token? {
        val start = current
        val startLine = line
        val startColumn = column
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        val c = peek()
        
        return when {
            c.isLetter() -> {
                val identifier = scanIdentifier()
                when (identifier) {
                    "query", "mutation", "subscription", "fragment", "on",
                    "type", "interface", "union", "enum", "input", "schema",
                    "extend", "scalar", "directive" -> 
                        TokenFactory.keyword(TokenType.IDENTIFIER, identifier, location)
                    else -> TokenFactory.identifier(identifier, location)
                }
            }
            c == '{' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACE, "{", location)
            }
            c == '}' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACE, "}", location)
            }
            c == '(' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACKET, "(", location)
            }
            c == ')' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACKET, ")", location)
            }
            c == '[' -> {
                advance()
                TokenFactory.operator(TokenType.LEFT_BRACKET, "[", location)
            }
            c == ']' -> {
                advance()
                TokenFactory.operator(TokenType.RIGHT_BRACKET, "]", location)
            }
            c == ':' -> {
                advance()
                TokenFactory.operator(TokenType.COLON, ":", location)
            }
            c == '!' -> {
                advance()
                TokenFactory.operator(TokenType.EXCLAMATION, "!", location)
            }
            c == '@' -> {
                advance()
                TokenFactory.identifier("@", location)
            }
            else -> null
        }
    }
    
    /**
     * 扫描 Markdown Token
     */
    private fun scanMarkdownToken(): Token? {
        val start = current
        val startLine = line
        val startColumn = column
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        val c = peek()
        
        return when {
            c == '#' -> scanMarkdownHeading()
            c == '*' -> scanMarkdownEmphasis()
            c == '_' -> scanMarkdownEmphasis('_')
            c == '`' -> scanMarkdownCode()
            c == '[' -> scanMarkdownLink()
            c == '!' -> scanMarkdownImage()
            else -> null
        }
    }
    
    // 辅助方法
    
    private fun scanIdentifier(): String {
        val start = current
        
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_' || peek() == '-')) {
            advance()
        }
        
        return source.substring(start, current)
    }
    
    private fun scanNumber(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        while (!isAtEnd() && peek().isDigit()) {
            advance()
        }
        
        var isFloat = false
        if (!isAtEnd() && peek() == '.' && peekNext().isDigit()) {
            isFloat = true
            advance() // 消费 .
            while (!isAtEnd() && peek().isDigit()) {
                advance()
            }
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        
        return if (isFloat) {
            TokenFactory.floatLiteral(lexeme, lexeme.toDouble(), location)
        } else {
            TokenFactory.integerLiteral(lexeme, lexeme.toLong(), location)
        }
    }
    
    private fun scanSQLString(quote: Char = '\''): Token {
        val start = current - 1
        val startLine = line
        val startColumn = column - 1
        val value = StringBuilder()
        
        while (!isAtEnd() && peek() != quote) {
            if (peek() == '\\') {
                advance()
                if (!isAtEnd()) {
                    val escaped = advance()
                    when (escaped) {
                        'n' -> value.append('\n')
                        't' -> value.append('\t')
                        'r' -> value.append('\r')
                        '\\' -> value.append('\\')
                        '\'' -> value.append('\'')
                        '"' -> value.append('"')
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
        
        if (!isAtEnd()) {
            advance() // 消费结束引号
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        return TokenFactory.stringLiteral(lexeme, value.toString(), location)
    }
    
    private fun scanHTMLString(quote: Char = '"'): Token {
        return scanSQLString(quote) // HTML 字符串处理类似 SQL
    }
    
    private fun scanCSSString(quote: Char = '"'): Token {
        return scanSQLString(quote) // CSS 字符串处理类似 SQL
    }
    
    private fun scanHTMLTag(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        advance() // 消费 <
        
        val tagName = StringBuilder()
        while (!isAtEnd() && peek() != '>' && peek() != ' ' && peek() != '\t' && peek() != '\n') {
            tagName.append(advance())
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        return TokenFactory.keyword(TokenType.HTML_TAG, "<${tagName}", location)
    }
    
    private fun scanCSSValue(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        while (!isAtEnd() && (peek().isDigit() || peek() == '.' || peek().isLetter() || peek() == '%')) {
            advance()
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        return TokenFactory.identifier(lexeme, location)
    }
    
    private fun scanRegexPattern(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        advance() // 消费开始的 /
        
        val pattern = StringBuilder()
        while (!isAtEnd() && peek() != '/') {
            if (peek() == '\\') {
                pattern.append(advance())
                if (!isAtEnd()) {
                    pattern.append(advance())
                }
            } else {
                pattern.append(advance())
            }
        }
        
        if (!isAtEnd()) {
            advance() // 消费结束的 /
        }
        
        // 扫描标志
        val flags = StringBuilder()
        while (!isAtEnd() && peek().isLetter()) {
            flags.append(advance())
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = "/${pattern}/${flags}"
        return TokenFactory.keyword(TokenType.REGEX_PATTERN, lexeme, location)
    }
    
    private fun scanRegexEscape(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        advance() // 消费 \
        if (!isAtEnd()) {
            advance() // 消费转义字符
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = source.substring(start, current)
        return TokenFactory.identifier(lexeme, location)
    }
    
    private fun scanMarkdownHeading(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        var level = 0
        while (!isAtEnd() && peek() == '#') {
            level++
            advance()
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = "#".repeat(level)
        return TokenFactory.identifier(lexeme, location)
    }
    
    private fun scanMarkdownEmphasis(char: Char = '*'): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        var count = 0
        while (!isAtEnd() && peek() == char) {
            count++
            advance()
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = char.toString().repeat(count)
        return TokenFactory.identifier(lexeme, location)
    }
    
    private fun scanMarkdownCode(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        var count = 0
        while (!isAtEnd() && peek() == '`') {
            count++
            advance()
        }
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        val lexeme = "`".repeat(count)
        return TokenFactory.identifier(lexeme, location)
    }
    
    private fun scanMarkdownLink(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        advance() // 消费 [
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        return TokenFactory.operator(TokenType.LEFT_BRACKET, "[", location)
    }
    
    private fun scanMarkdownImage(): Token {
        val start = current
        val startLine = line
        val startColumn = column
        
        advance() // 消费 !
        
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        return TokenFactory.operator(TokenType.EXCLAMATION, "!", location)
    }
    
    private fun isHTMLTag(identifier: String): Boolean {
        val htmlTags = setOf(
            "html", "head", "body", "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6",
            "a", "img", "ul", "ol", "li", "table", "tr", "td", "th", "form", "input",
            "button", "script", "style", "link", "meta", "title", "section", "article",
            "header", "footer", "nav", "aside", "main"
        )
        return identifier.lowercase() in htmlTags
    }
    
    private fun isCSSProperty(identifier: String): Boolean {
        val cssProperties = setOf(
            "color", "background", "font", "margin", "padding", "border",
            "width", "height", "display", "position", "top", "left", "right", "bottom",
            "flex", "grid", "text-align", "font-size", "line-height", "opacity"
        )
        return identifier.lowercase() in cssProperties
    }
    
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
    
    private fun advance(): Char {
        if (isAtEnd()) return '\u0000'
        column++
        return source[current++]
    }
    
    private fun peek(): Char {
        if (isAtEnd()) return '\u0000'
        return source[current]
    }
    
    private fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }
    
    private fun isAtEnd(): Boolean {
        return current >= source.length
    }
    
    private fun reportError(message: String, location: SourceLocation) {
        val error = CompilerError(
            code = "DSL_LEXER_ERROR",
            message = message,
            severity = ErrorSeverity.ERROR,
            category = ErrorCategory.SYNTAX,
            location = location
        )
        errorReporter.report(error)
    }
}
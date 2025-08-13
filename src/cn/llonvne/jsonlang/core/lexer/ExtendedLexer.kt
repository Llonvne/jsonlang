package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.ErrorSeverity
import cn.llonvne.jsonlang.core.errors.ErrorCategory
import cn.llonvne.jsonlang.core.errors.CompilerError

/**
 * JSONLang 扩展词法分析器
 * 支持 JSONLang 特有的语法糖、宏语法和 DSL 语法
 */
class ExtendedJSONLangLexer(
    private val source: String,
    private val filename: String = "<unknown>",
    private val errorReporter: ErrorReporter
) : Lexer {
    
    private val baseLexer = JSONLangLexer(source, filename, errorReporter)
    private var current = 0
    private var line = 1
    private var column = 1
    
    // 扩展语法状态
    private var inMacroContext = false
    private var inDSLContext = false
    private var dslContextName: String? = null
    private var macroNestingLevel = 0
    private var templateNestingLevel = 0
    
    // Token 缓存用于复杂的前瞻分析
    private val tokenBuffer = mutableListOf<Token>()
    private var bufferIndex = 0
    
    override fun nextToken(): Token {
        if (bufferIndex < tokenBuffer.size) {
            return tokenBuffer[bufferIndex++]
        }
        
        val token = scanExtendedToken()
        tokenBuffer.add(token)
        bufferIndex++
        return token
    }
    
    override fun peekToken(): Token {
        return peekToken(0)
    }
    
    override fun peekToken(offset: Int): Token {
        // 确保缓存中有足够的 Token
        while (tokenBuffer.size - bufferIndex <= offset) {
            val token = scanExtendedToken()
            tokenBuffer.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val index = bufferIndex + offset
        return if (index < tokenBuffer.size) {
            tokenBuffer[index]
        } else {
            tokenBuffer.lastOrNull() ?: TokenFactory.eof(getCurrentLocation())
        }
    }
    
    override fun isAtEnd(): Boolean {
        return baseLexer.isAtEnd()
    }
    
    override fun getCurrentLocation(): SourceLocation {
        return baseLexer.getCurrentLocation()
    }
    
    override fun getErrorReporter(): ErrorReporter {
        return errorReporter
    }
    
    /**
     * 扫描扩展语法 Token
     */
    private fun scanExtendedToken(): Token {
        val baseToken = baseLexer.nextToken()
        
        // 处理宏语法
        if (isMacroSyntax(baseToken)) {
            return handleMacroSyntax(baseToken)
        }
        
        // 处理 DSL 语法
        if (isDSLSyntax(baseToken)) {
            return handleDSLSyntax(baseToken)
        }
        
        // 处理模板语法
        if (isTemplateSyntax(baseToken)) {
            return handleTemplateSyntax(baseToken)
        }
        
        // 处理语法糖
        if (isSyntaxSugar(baseToken)) {
            return handleSyntaxSugar(baseToken)
        }
        
        return baseToken
    }
    
    /**
     * 检查是否为宏语法
     */
    private fun isMacroSyntax(token: Token): Boolean {
        return when {
            // 宏定义开始: @macro
            token.type == TokenType.IDENTIFIER && token.lexeme == "@macro" -> true
            // 宏调用: @identifier
            token.type == TokenType.IDENTIFIER && token.lexeme.startsWith("@") -> true
            // 宏引用: #identifier
            token.type == TokenType.IDENTIFIER && token.lexeme.startsWith("#") -> true
            // 宏展开标记: ${...}
            token.type == TokenType.IDENTIFIER && token.lexeme == "$" -> {
                val next = baseLexer.peekToken()
                next.type == TokenType.LEFT_BRACE
            }
            else -> false
        }
    }
    
    /**
     * 处理宏语法
     */
    private fun handleMacroSyntax(token: Token): Token {
        return when {
            token.lexeme == "@macro" -> {
                inMacroContext = true
                TokenFactory.keyword(TokenType.MACRO_START, "@macro", token.location)
            }
            token.lexeme.startsWith("@") -> {
                // 宏调用
                val macroName = token.lexeme.substring(1)
                Token(TokenType.IDENTIFIER, macroName, macroName, token.location)
            }
            token.lexeme.startsWith("#") -> {
                // 宏引用
                val refName = token.lexeme.substring(1)
                Token(TokenType.IDENTIFIER, refName, refName, token.location)
            }
            token.lexeme == "$" -> {
                // 模板展开开始
                val nextToken = baseLexer.nextToken()
                if (nextToken.type == TokenType.LEFT_BRACE) {
                    templateNestingLevel++
                    TokenFactory.operator(TokenType.TEMPLATE_START, "\${", token.location)
                } else {
                    // 回退 Token
                    token
                }
            }
            else -> token
        }
    }
    
    /**
     * 检查是否为 DSL 语法
     */
    private fun isDSLSyntax(token: Token): Boolean {
        return when {
            // DSL 定义开始: dsl "name" { ... }
            token.type == TokenType.DSL -> true
            // DSL 块开始标记
            inDSLContext && token.type == TokenType.LEFT_BRACE -> true
            // DSL 特殊语法标记
            token.type == TokenType.IDENTIFIER && isDSLKeyword(token.lexeme) -> true
            else -> false
        }
    }
    
    /**
     * 处理 DSL 语法
     */
    private fun handleDSLSyntax(token: Token): Token {
        return when {
            token.type == TokenType.DSL -> {
                // 检查 DSL 名称
                val nameToken = baseLexer.peekToken()
                if (nameToken.type == TokenType.STRING_LITERAL) {
                    dslContextName = nameToken.literal as? String
                    inDSLContext = true
                }
                TokenFactory.keyword(TokenType.DSL_START, "dsl", token.location)
            }
            inDSLContext && token.type == TokenType.LEFT_BRACE -> {
                TokenFactory.operator(TokenType.DSL_START, "{", token.location)
            }
            inDSLContext && token.type == TokenType.RIGHT_BRACE -> {
                inDSLContext = false
                dslContextName = null
                TokenFactory.operator(TokenType.DSL_END, "}", token.location)
            }
            isDSLKeyword(token.lexeme) -> {
                // 转换 DSL 关键字
                val dslTokenType = getDSLTokenType(token.lexeme)
                TokenFactory.keyword(dslTokenType, token.lexeme, token.location)
            }
            else -> token
        }
    }
    
    /**
     * 检查是否为模板语法
     */
    private fun isTemplateSyntax(token: Token): Boolean {
        return when {
            templateNestingLevel > 0 && token.type == TokenType.RIGHT_BRACE -> true
            token.type == TokenType.TEMPLATE_START -> true
            else -> false
        }
    }
    
    /**
     * 处理模板语法
     */
    private fun handleTemplateSyntax(token: Token): Token {
        return when {
            token.type == TokenType.RIGHT_BRACE && templateNestingLevel > 0 -> {
                templateNestingLevel--
                TokenFactory.operator(TokenType.TEMPLATE_END, "}", token.location)
            }
            else -> token
        }
    }
    
    /**
     * 检查是否为语法糖
     */
    private fun isSyntaxSugar(token: Token): Boolean {
        return when {
            // 函数简写语法: fn => 
            token.type == TokenType.IDENTIFIER && token.lexeme == "fn" -> {
                val next = baseLexer.peekToken()
                next.type == TokenType.FAT_ARROW
            }
            // 可选链操作符: ?.
            token.type == TokenType.QUESTION -> {
                val next = baseLexer.peekToken()
                next.type == TokenType.DOT
            }
            // 空合并操作符: ??
            token.type == TokenType.QUESTION -> {
                val next = baseLexer.peekToken()
                next.type == TokenType.QUESTION
            }
            // 管道操作符: |>
            token.type == TokenType.BIT_OR -> {
                val next = baseLexer.peekToken()
                next.type == TokenType.GREATER_THAN
            }
            // 范围操作符: ..
            token.type == TokenType.DOT -> {
                val next = baseLexer.peekToken()
                next.type == TokenType.DOT
            }
            else -> false
        }
    }
    
    /**
     * 处理语法糖
     */
    private fun handleSyntaxSugar(token: Token): Token {
        return when {
            token.lexeme == "fn" -> {
                val next = baseLexer.peekToken()
                if (next.type == TokenType.FAT_ARROW) {
                    // 消费 =>
                    baseLexer.nextToken()
                    TokenFactory.keyword(TokenType.LAMBDA, "fn =>", token.location)
                } else {
                    token
                }
            }
            token.type == TokenType.QUESTION -> {
                val next = baseLexer.peekToken()
                when (next.type) {
                    TokenType.DOT -> {
                        // 可选链操作符 ?.
                        baseLexer.nextToken()
                        Token(TokenType.QUESTION, "?.", null, token.location)
                    }
                    TokenType.QUESTION -> {
                        // 空合并操作符 ??
                        baseLexer.nextToken()
                        Token(TokenType.QUESTION, "??", null, token.location)
                    }
                    else -> token
                }
            }
            token.type == TokenType.BIT_OR -> {
                val next = baseLexer.peekToken()
                if (next.type == TokenType.GREATER_THAN) {
                    // 管道操作符 |>
                    baseLexer.nextToken()
                    Token(TokenType.BIT_OR, "|>", null, token.location)
                } else {
                    token
                }
            }
            token.type == TokenType.DOT -> {
                val next = baseLexer.peekToken()
                if (next.type == TokenType.DOT) {
                    // 范围操作符 ..
                    baseLexer.nextToken()
                    Token(TokenType.DOT, "..", null, token.location)
                } else {
                    token
                }
            }
            else -> token
        }
    }
    
    /**
     * 检查是否为 DSL 关键字
     */
    private fun isDSLKeyword(lexeme: String): Boolean {
        return when (dslContextName) {
            "sql" -> isSQLKeyword(lexeme)
            "html" -> isHTMLKeyword(lexeme)
            "css" -> isCSSKeyword(lexeme)
            "regex" -> isRegexKeyword(lexeme)
            else -> false
        }
    }
    
    /**
     * 获取 DSL Token 类型
     */
    private fun getDSLTokenType(lexeme: String): TokenType {
        return when (dslContextName) {
            "sql" -> getSQLTokenType(lexeme)
            "html" -> getHTMLTokenType(lexeme)
            "css" -> getCSSTokenType(lexeme)
            "regex" -> getRegexTokenType(lexeme)
            else -> TokenType.IDENTIFIER
        }
    }
    
    /**
     * SQL DSL 关键字检查
     */
    private fun isSQLKeyword(lexeme: String): Boolean {
        val sqlKeywords = setOf(
            "select", "from", "where", "join", "inner", "left", "right", "outer",
            "on", "group", "by", "having", "order", "limit", "offset",
            "insert", "into", "values", "update", "set", "delete",
            "create", "table", "drop", "alter", "index"
        )
        return lexeme.lowercase() in sqlKeywords
    }
    
    /**
     * 获取 SQL Token 类型
     */
    private fun getSQLTokenType(lexeme: String): TokenType {
        return when (lexeme.lowercase()) {
            "select" -> TokenType.IDENTIFIER // 可以扩展为专门的 SQL Token 类型
            "from" -> TokenType.FROM
            "where" -> TokenType.IDENTIFIER
            else -> TokenType.IDENTIFIER
        }
    }
    
    /**
     * HTML DSL 关键字检查
     */
    private fun isHTMLKeyword(lexeme: String): Boolean {
        val htmlTags = setOf(
            "html", "head", "body", "div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6",
            "a", "img", "ul", "ol", "li", "table", "tr", "td", "th", "form", "input",
            "button", "script", "style", "link", "meta", "title"
        )
        return lexeme.lowercase() in htmlTags
    }
    
    /**
     * 获取 HTML Token 类型
     */
    private fun getHTMLTokenType(lexeme: String): TokenType {
        return TokenType.IDENTIFIER // 可以扩展为专门的 HTML Token 类型
    }
    
    /**
     * CSS DSL 关键字检查
     */
    private fun isCSSKeyword(lexeme: String): Boolean {
        val cssProperties = setOf(
            "color", "background", "font", "margin", "padding", "border",
            "width", "height", "display", "position", "top", "left", "right", "bottom"
        )
        return lexeme.lowercase() in cssProperties
    }
    
    /**
     * 获取 CSS Token 类型
     */
    private fun getCSSTokenType(lexeme: String): TokenType {
        return TokenType.IDENTIFIER // 可以扩展为专门的 CSS Token 类型
    }
    
    /**
     * Regex DSL 关键字检查
     */
    private fun isRegexKeyword(lexeme: String): Boolean {
        val regexKeywords = setOf(
            "match", "replace", "split", "test", "flags", "global", "ignoreCase", "multiline"
        )
        return lexeme.lowercase() in regexKeywords
    }
    
    /**
     * 获取 Regex Token 类型
     */
    private fun getRegexTokenType(lexeme: String): TokenType {
        return TokenType.IDENTIFIER // 可以扩展为专门的 Regex Token 类型
    }
    
    /**
     * 扫描字符串插值表达式
     */
    private fun scanStringInterpolation(baseString: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val chars = baseString.toCharArray()
        
        while (i < chars.size) {
            if (i < chars.size - 1 && chars[i] == '$' && chars[i + 1] == '{') {
                // 找到插值表达式开始
                val start = i
                i += 2 // 跳过 ${
                var braceCount = 1
                val exprStart = i
                
                // 找到匹配的结束括号
                while (i < chars.size && braceCount > 0) {
                    when (chars[i]) {
                        '{' -> braceCount++
                        '}' -> braceCount--
                    }
                    i++
                }
                
                if (braceCount == 0) {
                    // 提取插值表达式
                    val expression = baseString.substring(exprStart, i - 1)
                    
                    // 创建插值 Token
                    val location = getCurrentLocation()
                    tokens.add(TokenFactory.operator(TokenType.TEMPLATE_START, "\${", location))
                    
                    // 递归解析插值表达式
                    val exprLexer = ExtendedJSONLangLexer(expression, filename, errorReporter)
                    var exprToken = exprLexer.nextToken()
                    while (exprToken.type != TokenType.EOF) {
                        tokens.add(exprToken)
                        exprToken = exprLexer.nextToken()
                    }
                    
                    tokens.add(TokenFactory.operator(TokenType.TEMPLATE_END, "}", location))
                } else {
                    // 未匹配的括号，报告错误
                    reportError("未匹配的插值表达式括号", getCurrentLocation())
                }
            } else {
                i++
            }
        }
        
        return tokens
    }
    
    /**
     * 报告错误
     */
    private fun reportError(message: String, location: SourceLocation) {
        val error = CompilerError(
            code = "EXTENDED_LEXER_ERROR",
            message = message,
            severity = ErrorSeverity.ERROR,
            category = ErrorCategory.SYNTAX,
            location = location
        )
        errorReporter.report(error)
    }
}
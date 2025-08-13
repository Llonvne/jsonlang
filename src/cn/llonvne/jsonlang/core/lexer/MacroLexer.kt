package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.ErrorSeverity
import cn.llonvne.jsonlang.core.errors.ErrorCategory
import cn.llonvne.jsonlang.core.errors.CompilerError

/**
 * 宏语法词法分析器
 * 专门处理 JSONLang 的宏定义和宏展开语法
 */
class MacroLexer(
    private val source: String,
    private val filename: String = "<unknown>",
    private val errorReporter: ErrorReporter
) {
    
    private var current = 0
    private var line = 1
    private var column = 1
    
    // 宏上下文状态
    private var inMacroDefinition = false
    private var inMacroExpansion = false
    private var macroNestingLevel = 0
    private var quoteLevel = 0
    
    /**
     * 扫描宏相关的 Token
     */
    fun scanMacroTokens(): List<Token> {
        val tokens = mutableListOf<Token>()
        
        while (!isAtEnd()) {
            val token = scanMacroToken()
            if (token != null) {
                tokens.add(token)
            }
        }
        
        return tokens
    }
    
    /**
     * 扫描单个宏 Token
     */
    private fun scanMacroToken(): Token? {
        skipWhitespace()
        
        if (isAtEnd()) return null
        
        val start = current
        val startLine = line
        val startColumn = column
        
        val c = advance()
        val location = SourceLocation(filename, startLine, startColumn, line, column, start, current)
        
        return when (c) {
            '@' -> scanMacroDirective(location)
            '#' -> scanMacroReference(location)
            '$' -> scanMacroExpansion(location)
            '`' -> scanQuotedExpression(location)
            '~' -> scanUnquoteExpression(location)
            ',' -> {
                if (peek() == '@') {
                    advance()
                    TokenFactory.operator(TokenType.SPLICE, ",@", location)
                } else {
                    TokenFactory.operator(TokenType.COMMA, ",", location)
                }
            }
            else -> {
                // 回退并使用基础词法分析器
                current--
                column--
                null
            }
        }
    }
    
    /**
     * 扫描宏指令 (@macro, @define, @expand 等)
     */
    private fun scanMacroDirective(location: SourceLocation): Token {
        val directive = scanIdentifier()
        
        return when (directive) {
            "macro" -> {
                inMacroDefinition = true
                TokenFactory.keyword(TokenType.MACRO_START, "@macro", location)
            }
            "define" -> {
                TokenFactory.keyword(TokenType.MACRO, "@define", location)
            }
            "expand" -> {
                TokenFactory.keyword(TokenType.MACRO, "@expand", location)
            }
            "quote" -> {
                quoteLevel++
                TokenFactory.keyword(TokenType.QUOTE, "@quote", location)
            }
            "unquote" -> {
                if (quoteLevel > 0) {
                    quoteLevel--
                }
                TokenFactory.keyword(TokenType.UNQUOTE, "@unquote", location)
            }
            "splice" -> {
                TokenFactory.keyword(TokenType.SPLICE, "@splice", location)
            }
            "eval" -> {
                TokenFactory.keyword(TokenType.EVAL, "@eval", location)
            }
            else -> {
                // 宏调用
                TokenFactory.identifier("@$directive", location)
            }
        }
    }
    
    /**
     * 扫描宏引用 (#symbol)
     */
    private fun scanMacroReference(location: SourceLocation): Token {
        val symbol = scanIdentifier()
        return TokenFactory.identifier("#$symbol", location)
    }
    
    /**
     * 扫描宏展开 (${expression})
     */
    private fun scanMacroExpansion(location: SourceLocation): Token {
        if (peek() == '{') {
            advance() // 消费 {
            macroNestingLevel++
            inMacroExpansion = true
            return TokenFactory.operator(TokenType.TEMPLATE_START, "\${", location)
        } else {
            // 普通的 $ 符号
            return TokenFactory.identifier("$", location)
        }
    }
    
    /**
     * 扫描引用表达式 (`expression`)
     */
    private fun scanQuotedExpression(location: SourceLocation): Token {
        val expression = StringBuilder()
        
        while (!isAtEnd() && peek() != '`') {
            if (peek() == '\\') {
                advance() // 消费转义符
                if (!isAtEnd()) {
                    val escaped = advance()
                    when (escaped) {
                        'n' -> expression.append('\n')
                        't' -> expression.append('\t')
                        'r' -> expression.append('\r')
                        '\\' -> expression.append('\\')
                        '`' -> expression.append('`')
                        else -> {
                            expression.append('\\')
                            expression.append(escaped)
                        }
                    }
                }
            } else {
                expression.append(advance())
            }
        }
        
        if (isAtEnd()) {
            reportError("未终止的引用表达式", location)
            return TokenFactory.error("`${expression}", location)
        }
        
        advance() // 消费结束的 `
        
        return TokenFactory.stringLiteral("`${expression}`", expression.toString(), location)
    }
    
    /**
     * 扫描反引用表达式 (~expression)
     */
    private fun scanUnquoteExpression(location: SourceLocation): Token {
        if (quoteLevel > 0) {
            quoteLevel--
            return TokenFactory.keyword(TokenType.UNQUOTE, "~", location)
        } else {
            // 在非引用上下文中，~ 是位运算符
            return TokenFactory.operator(TokenType.BIT_NOT, "~", location)
        }
    }
    
    /**
     * 扫描标识符
     */
    private fun scanIdentifier(): String {
        val start = current
        
        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            advance()
        }
        
        return source.substring(start, current)
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
     * 检查是否到达文件末尾
     */
    private fun isAtEnd(): Boolean {
        return current >= source.length
    }
    
    /**
     * 报告错误
     */
    private fun reportError(message: String, location: SourceLocation) {
        val error = CompilerError(
            code = "MACRO_LEXER_ERROR",
            message = message,
            severity = ErrorSeverity.ERROR,
            category = ErrorCategory.SYNTAX,
            location = location
        )
        errorReporter.report(error)
    }
}

/**
 * 宏展开器
 * 处理宏的展开和替换
 */
class MacroExpander(
    private val errorReporter: ErrorReporter
) {
    
    // 宏定义存储
    private val macroDefinitions = mutableMapOf<String, MacroDefinition>()
    
    /**
     * 宏定义数据类
     */
    data class MacroDefinition(
        val name: String,
        val parameters: List<String>,
        val body: List<Token>,
        val location: SourceLocation
    )
    
    /**
     * 注册宏定义
     */
    fun registerMacro(definition: MacroDefinition) {
        macroDefinitions[definition.name] = definition
    }
    
    /**
     * 展开宏调用
     */
    fun expandMacro(name: String, arguments: List<List<Token>>, location: SourceLocation): List<Token> {
        val definition = macroDefinitions[name]
        
        if (definition == null) {
            reportError("未定义的宏: $name", location)
            return emptyList()
        }
        
        if (arguments.size != definition.parameters.size) {
            reportError("宏参数数量不匹配: 期望 ${definition.parameters.size}，实际 ${arguments.size}", location)
            return emptyList()
        }
        
        // 创建参数替换映射
        val substitutions = definition.parameters.zip(arguments).toMap()
        
        // 展开宏体
        return expandTokens(definition.body, substitutions)
    }
    
    /**
     * 展开 Token 列表中的宏引用
     */
    private fun expandTokens(tokens: List<Token>, substitutions: Map<String, List<Token>>): List<Token> {
        val result = mutableListOf<Token>()
        
        for (token in tokens) {
            when {
                token.type == TokenType.IDENTIFIER && token.lexeme.startsWith("#") -> {
                    // 宏参数引用
                    val paramName = token.lexeme.substring(1)
                    val replacement = substitutions[paramName]
                    if (replacement != null) {
                        result.addAll(replacement)
                    } else {
                        reportError("未定义的宏参数: $paramName", token.location)
                        result.add(token)
                    }
                }
                token.type == TokenType.TEMPLATE_START -> {
                    // 模板展开
                    result.add(token)
                    // 这里需要更复杂的模板展开逻辑
                }
                else -> {
                    result.add(token)
                }
            }
        }
        
        return result
    }
    
    /**
     * 报告错误
     */
    private fun reportError(message: String, location: SourceLocation) {
        val error = CompilerError(
            code = "MACRO_EXPANSION_ERROR",
            message = message,
            severity = ErrorSeverity.ERROR,
            category = ErrorCategory.SYNTAX,
            location = location
        )
        errorReporter.report(error)
    }
}
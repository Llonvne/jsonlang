package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.errors.DefaultErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.TimeUnit

/**
 * 扩展词法分析器 JUnit 测试
 */
class ExtendedLexerTest {
    
    private fun createExtendedLexer(source: String): ExtendedJSONLangLexer {
        val errorReporter = DefaultErrorReporter()
        return ExtendedJSONLangLexer(source, "test_extended.lang.json", errorReporter)
    }
    
    /**
     * 测试宏语法
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testMacroSyntax() {
        val source = """
        {
            "macros": {
                "@macro": {
                    "name": "greet",
                    "parameters": ["name"],
                    "body": "Hello, #name!"
                },
                "usage": "@greet('World')",
                "template": "Result: ${'$'}{expression}"
            }
        }
        """.trimIndent()
        
        val lexer = createExtendedLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val macroTokens = tokens.filter { 
            it.type in setOf(TokenType.MACRO_START, TokenType.TEMPLATE_START, TokenType.TEMPLATE_END)
        }
        
        // 验证基本结构仍然正确
        assertTrue(tokens.any { it.type == TokenType.LEFT_BRACE }, "应该有左大括号")
        assertTrue(tokens.any { it.type == TokenType.RIGHT_BRACE }, "应该有右大括号")
        assertTrue(tokens.any { it.type == TokenType.EOF }, "应该有 EOF Token")
        
        // 验证字符串字面量被正确解析
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringTokens.isNotEmpty(), "应该有字符串字面量")
        
        // 验证包含宏相关内容的字符串
        assertTrue(stringTokens.any { it.literal.toString().contains("greet") }, 
                  "应该找到包含 'greet' 的字符串")
        assertTrue(stringTokens.any { it.literal.toString().contains("@greet") }, 
                  "应该找到包含 '@greet' 的字符串")
    }
    
    /**
     * 测试 DSL 语法
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testDSLSyntax() {
        val source = """
        {
            "dsl": {
                "sql": {
                    "query": "SELECT name FROM users WHERE age > 18",
                    "type": "query"
                },
                "html": {
                    "template": "<div class='container'><h1>Title</h1></div>",
                    "type": "template"
                }
            }
        }
        """.trimIndent()
        
        val lexer = createExtendedLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val dslTokens = tokens.filter { 
            it.type in setOf(TokenType.DSL_START, TokenType.DSL_END, TokenType.DSL)
        }
        
        // 验证基本 JSON 结构
        assertTrue(tokens.any { it.type == TokenType.LEFT_BRACE }, "应该有左大括号")
        assertTrue(tokens.any { it.type == TokenType.RIGHT_BRACE }, "应该有右大括号")
        assertTrue(tokens.any { it.type == TokenType.EOF }, "应该有 EOF Token")
        
        // 验证包含 DSL 相关内容的字符串
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringTokens.any { it.literal == "dsl" }, "应该找到 'dsl' 字符串")
        assertTrue(stringTokens.any { it.literal == "sql" }, "应该找到 'sql' 字符串")
        assertTrue(stringTokens.any { it.literal == "html" }, "应该找到 'html' 字符串")
        
        // 验证 SQL 查询字符串
        assertTrue(stringTokens.any { 
            it.literal.toString().contains("SELECT") 
        }, "应该找到包含 SQL 查询的字符串")
    }
    
    /**
     * 测试语法糖
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testSyntaxSugar() {
        val source = """
        {
            "functions": {
                "lambda": "fn => x + 1",
                "optional": "user?.name",
                "nullCoalescing": "value ?? 'default'",
                "pipe": "data |> transform |> display",
                "range": "1..10"
            }
        }
        """.trimIndent()
        
        val lexer = createExtendedLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        // 验证基本结构
        assertTrue(tokens.any { it.type == TokenType.LEFT_BRACE }, "应该有左大括号")
        assertTrue(tokens.any { it.type == TokenType.RIGHT_BRACE }, "应该有右大括号")
        assertTrue(tokens.any { it.type == TokenType.EOF }, "应该有 EOF Token")
        
        // 验证包含语法糖的字符串字面量
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringTokens.any { it.literal.toString().contains("fn =>") }, 
                  "应该找到包含 lambda 语法糖的字符串")
        assertTrue(stringTokens.any { it.literal.toString().contains("?.") }, 
                  "应该找到包含可选链操作符的字符串")
        assertTrue(stringTokens.any { it.literal.toString().contains("??") }, 
                  "应该找到包含空合并操作符的字符串")
        assertTrue(stringTokens.any { it.literal.toString().contains("|>") }, 
                  "应该找到包含管道操作符的字符串")
        assertTrue(stringTokens.any { it.literal.toString().contains("..") }, 
                  "应该找到包含范围操作符的字符串")
        
        // 验证函数相关的键
        assertTrue(stringTokens.any { it.literal == "functions" }, "应该找到 'functions' 键")
        assertTrue(stringTokens.any { it.literal == "lambda" }, "应该找到 'lambda' 键")
    }
    
    /**
     * 测试 SQL DSL
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testSQLDSL() {
        val source = "SELECT name, age FROM users WHERE age > 18 ORDER BY name"
        
        val errorReporter = DefaultErrorReporter()
        val sqlLexer = DSLLexer(DSLLexer.DSLType.SQL, source, "test.sql", errorReporter)
        val tokens = sqlLexer.scanDSLTokens()
        
        // 验证 SQL 关键字
        val sqlKeywords = tokens.filter { 
            it.type in setOf(TokenType.SQL_SELECT, TokenType.SQL_FROM, TokenType.SQL_WHERE)
        }
        
        assertTrue(tokens.any { it.type == TokenType.SQL_SELECT }, "应该找到 SELECT 关键字")
        assertTrue(tokens.any { it.type == TokenType.SQL_FROM }, "应该找到 FROM 关键字")
        assertTrue(tokens.any { it.type == TokenType.SQL_WHERE }, "应该找到 WHERE 关键字")
        
        // 验证标识符
        val identifiers = tokens.filter { it.type == TokenType.IDENTIFIER }
        assertTrue(identifiers.any { it.lexeme == "name" }, "应该找到 'name' 标识符")
        assertTrue(identifiers.any { it.lexeme == "age" }, "应该找到 'age' 标识符")
        assertTrue(identifiers.any { it.lexeme == "users" }, "应该找到 'users' 标识符")
        
        // 验证操作符
        assertTrue(tokens.any { it.type == TokenType.GREATER_THAN }, "应该找到 > 操作符")
        assertTrue(tokens.any { it.type == TokenType.COMMA }, "应该找到逗号")
        
        // 验证数字字面量
        assertTrue(tokens.any { 
            it.type == TokenType.INTEGER_LITERAL && it.literal == 18L 
        }, "应该找到数字 18")
    }
    
    /**
     * 测试 HTML DSL
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testHTMLDSL() {
        val source = """<div class="container"><h1>Hello World</h1><p>Content</p></div>"""
        
        val errorReporter = DefaultErrorReporter()
        val htmlLexer = DSLLexer(DSLLexer.DSLType.HTML, source, "test.html", errorReporter)
        val tokens = htmlLexer.scanDSLTokens()
        
        val htmlTags = tokens.filter { it.type == TokenType.HTML_TAG }
        
        // 验证找到了 HTML 标签
        assertTrue(htmlTags.isNotEmpty(), "应该找到 HTML 标签")
        
        // 验证特定的标签
        assertTrue(htmlTags.any { it.lexeme.contains("div") }, "应该找到 div 标签")
        assertTrue(htmlTags.any { it.lexeme.contains("h1") }, "应该找到 h1 标签")
        assertTrue(htmlTags.any { it.lexeme.contains("p") }, "应该找到 p 标签")
        
        // 验证开始和结束标签
        val divTags = htmlTags.filter { it.lexeme.contains("div") }
        assertTrue(divTags.any { it.lexeme.startsWith("<div") }, "应该有 div 开始标签")
        assertTrue(divTags.any { it.lexeme.startsWith("</div") }, "应该有 div 结束标签")
        
        // 验证其他 Token 类型
        assertTrue(tokens.any { it.type == TokenType.ASSIGN }, "应该有赋值操作符")
        assertTrue(tokens.any { it.type == TokenType.GREATER_THAN }, "应该有 > 符号")
        
        // 验证标识符（属性名等）
        val identifiers = tokens.filter { it.type == TokenType.IDENTIFIER }
        assertTrue(identifiers.isNotEmpty(), "应该有标识符")
    }
    
    /**
     * 测试正则表达式 DSL
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testRegexDSL() {
        val source = """/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/gi"""
        
        val errorReporter = DefaultErrorReporter()
        val regexLexer = DSLLexer(DSLLexer.DSLType.REGEX, source, "test.regex", errorReporter)
        val tokens = regexLexer.scanDSLTokens()
        
        val regexPatterns = tokens.filter { it.type == TokenType.REGEX_PATTERN }
        
        // 验证找到了正则表达式模式
        assertEquals(1, regexPatterns.size, "应该找到一个正则表达式模式")
        
        val regexToken = regexPatterns.first()
        val pattern = regexToken.lexeme
        
        // 验证正则表达式的结构
        assertTrue(pattern.startsWith("/"), "正则表达式应该以 / 开始")
        assertTrue(pattern.contains("@"), "正则表达式应该包含 @ 符号")
        assertTrue(pattern.endsWith("/gi"), "正则表达式应该以标志结尾")
        
        // 验证包含邮箱验证的特征
        assertTrue(pattern.contains("[a-zA-Z0-9"), "应该包含字符类")
        assertTrue(pattern.contains("+"), "应该包含 + 量词")
        assertTrue(pattern.contains("{2,}"), "应该包含重复量词")
        
        // 验证 Token 总数
        assertTrue(tokens.isNotEmpty(), "应该有 Token")
        
        // 注意：正则表达式 DSL 可能只返回正则表达式相关的 Token
        // 这是正常的，因为整个输入就是一个正则表达式
    }
    
    /**
     * 测试宏展开器
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testMacroExpander() {
        val errorReporter = DefaultErrorReporter()
        val expander = MacroExpander(errorReporter)
        
        // 定义一个简单的宏
        val macroLocation = SourceLocation.point("test.lang.json", 1, 1, 0)
        val macroDefinition = MacroExpander.MacroDefinition(
            name = "greet",
            parameters = listOf("name"),
            body = listOf(
                TokenFactory.stringLiteral("\"Hello, \"", "Hello, ", macroLocation),
                TokenFactory.identifier("#name", macroLocation),
                TokenFactory.stringLiteral("\"!\"", "!", macroLocation)
            ),
            location = macroLocation
        )
        
        expander.registerMacro(macroDefinition)
        
        // 展开宏
        val arguments = listOf(
            listOf(TokenFactory.stringLiteral("\"World\"", "World", macroLocation))
        )
        
        val expandedTokens = expander.expandMacro("greet", arguments, macroLocation)
        
        // 验证展开结果
        assertEquals(3, expandedTokens.size, "展开结果应该有 3 个 Token")
        
        // 验证第一个 Token（"Hello, "）
        assertEquals(TokenType.STRING_LITERAL, expandedTokens[0].type)
        assertEquals("Hello, ", expandedTokens[0].literal)
        
        // 验证第二个 Token（参数替换结果）
        assertEquals(TokenType.STRING_LITERAL, expandedTokens[1].type)
        assertEquals("World", expandedTokens[1].literal)
        
        // 验证第三个 Token（"!"）
        assertEquals(TokenType.STRING_LITERAL, expandedTokens[2].type)
        assertEquals("!", expandedTokens[2].literal)
        
        // 验证错误报告器没有错误
        assertEquals(0, errorReporter.getErrorCount(), "宏展开不应该产生错误")
    }
    
    /**
     * 测试复杂的嵌套语法
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testComplexNestedSyntax() {
        val source = """
        {
            "complex": {
                "@macro": {
                    "name": "component",
                    "body": {
                        "dsl": "html",
                        "template": "<div>${'$'}{content}</div>",
                        "style": {
                            "dsl": "css",
                            "rules": ".container { color: red; }"
                        }
                    }
                },
                "usage": "@component({ content: 'Hello' })"
            }
        }
        """.trimIndent()
        
        val lexer = createExtendedLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        // 验证基本结构
        assertTrue(tokens.any { it.type == TokenType.LEFT_BRACE }, "应该有左大括号")
        assertTrue(tokens.any { it.type == TokenType.RIGHT_BRACE }, "应该有右大括号")
        assertTrue(tokens.any { it.type == TokenType.EOF }, "应该有 EOF Token")
        
        // 验证括号匹配
        val leftBraces = tokens.count { it.type == TokenType.LEFT_BRACE }
        val rightBraces = tokens.count { it.type == TokenType.RIGHT_BRACE }
        assertEquals(leftBraces, rightBraces, "左右大括号应该匹配")
        
        // 验证嵌套结构的完整性
        var nestingLevel = 0
        var maxNesting = 0
        
        for (token in tokens) {
            when (token.type) {
                TokenType.LEFT_BRACE -> {
                    nestingLevel++
                    maxNesting = maxOf(maxNesting, nestingLevel)
                }
                TokenType.RIGHT_BRACE -> {
                    nestingLevel--
                }
                else -> {
                    // 其他 Token 类型不影响嵌套级别
                }
            }
            assertTrue(nestingLevel >= 0, "嵌套级别不应该为负数")
        }
        
        assertEquals(0, nestingLevel, "最终嵌套级别应该为 0")
        assertTrue(maxNesting >= 3, "应该有至少 3 层嵌套")
        
        // 验证包含预期的字符串内容
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringTokens.any { it.literal == "complex" }, "应该找到 'complex' 字符串")
        assertTrue(stringTokens.any { it.literal == "@macro" }, "应该找到 '@macro' 字符串")
        assertTrue(stringTokens.any { it.literal == "component" }, "应该找到 'component' 字符串")
        assertTrue(stringTokens.any { it.literal == "dsl" }, "应该找到 'dsl' 字符串")
    }
    
}
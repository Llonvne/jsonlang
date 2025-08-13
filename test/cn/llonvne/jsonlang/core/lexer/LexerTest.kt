package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.errors.DefaultErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.TimeUnit

/**
 * JSONLang 词法分析器 JUnit 测试
 */
class LexerTest {
    
    private fun createLexer(source: String): JSONLangLexer {
        val errorReporter = DefaultErrorReporter()
        return JSONLangLexer(source, "test.lang.json", errorReporter)
    }
    
    /**
     * 测试基础 JSON Token
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testBasicJsonTokens() {
        val source = """{ "key": "value", "array": [1, 2, 3] }"""
        val lexer = createLexer(source)
        
        val expectedTypes = listOf(
            TokenType.LEFT_BRACE,
            TokenType.STRING_LITERAL,
            TokenType.COLON,
            TokenType.STRING_LITERAL,
            TokenType.COMMA,
            TokenType.STRING_LITERAL,
            TokenType.COLON,
            TokenType.LEFT_BRACKET,
            TokenType.INTEGER_LITERAL,
            TokenType.COMMA,
            TokenType.INTEGER_LITERAL,
            TokenType.COMMA,
            TokenType.INTEGER_LITERAL,
            TokenType.RIGHT_BRACKET,
            TokenType.RIGHT_BRACE,
            TokenType.EOF
        )
        
        val tokens = mutableListOf<Token>()
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        // 验证 Token 数量
        assertEquals(expectedTypes.size, tokens.size, "Token 数量不匹配")
        
        // 验证每个 Token 类型
        tokens.forEachIndexed { index, token ->
            val expected = expectedTypes[index]
            assertEquals(expected, token.type, "Token $index 类型不匹配: 期望 $expected，实际 ${token.type}")
        }
        
        // 验证特定的字面量值
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertEquals("key", stringTokens[0].literal)
        assertEquals("value", stringTokens[1].literal)
        assertEquals("array", stringTokens[2].literal)
        
        val integerTokens = tokens.filter { it.type == TokenType.INTEGER_LITERAL }
        assertEquals(1L, integerTokens[0].literal)
        assertEquals(2L, integerTokens[1].literal)
        assertEquals(3L, integerTokens[2].literal)
    }
    
    /**
     * 测试 JSONLang 关键字
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testKeywords() {
        val source = """
        {
            "metadata": {},
            "imports": {},
            "functions": {
                "test": {
                    "if": true,
                    "else": false,
                    "return": null
                }
            }
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val keywordTokens = tokens.filter { it.isKeyword() }
        
        // 验证找到了预期的关键字
        assertTrue(keywordTokens.any { it.type == TokenType.NULL }, "应该找到 null 关键字")
        
        // 验证关键字的词素
        val nullToken = keywordTokens.find { it.type == TokenType.NULL }
        assertNotNull(nullToken, "null 关键字不应为空")
        assertEquals("null", nullToken!!.lexeme, "null 关键字的词素应为 'null'")
        
        // 验证关键字数量大于 0
        assertTrue(keywordTokens.isNotEmpty(), "应该找到至少一个关键字")
    }
    
    /**
     * 测试操作符
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testOperators() {
        val source = """
        {
            "test": a + b - c * d / e % f,
            "comparison": x == y != z < w <= v > u >= t,
            "logical": p && q || !r,
            "assignment": s = t += u -= v *= w /= x,
            "bitwise": a & b | c ^ ~d << e >> f,
            "special": g -> h => i :: j?
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val operatorTokens = tokens.filter { it.isOperator() }
        
        // 验证找到了预期的操作符
        val expectedOperators = setOf(
            TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE,
            TokenType.MODULO, TokenType.POWER, TokenType.EQUAL, TokenType.NOT_EQUAL,
            TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN,
            TokenType.GREATER_EQUAL, TokenType.AND, TokenType.OR, TokenType.ASSIGN,
            TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.MULTIPLY_ASSIGN,
            TokenType.DIVIDE_ASSIGN, TokenType.BIT_AND, TokenType.BIT_OR,
            TokenType.BIT_XOR, TokenType.BIT_NOT, TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT,
            TokenType.ARROW, TokenType.FAT_ARROW, TokenType.DOUBLE_COLON
        )
        
        val foundOperatorTypes = operatorTokens.map { it.type }.toSet()
        val commonOperators = expectedOperators.intersect(foundOperatorTypes)
        
        assertTrue(commonOperators.isNotEmpty(), "应该找到一些预期的操作符")
        
        // 验证操作符优先级
        operatorTokens.forEach { token ->
            val precedence = token.getPrecedence()
            assertTrue(precedence >= 0, "操作符 ${token.type} 的优先级应该 >= 0")
        }
    }
    
    /**
     * 测试字面量
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testLiterals() {
        val source = """
        {
            "string": "Hello, 世界!",
            "integer": 42,
            "float": 3.14159,
            "scientific": 1.23e-4,
            "boolean_true": true,
            "boolean_false": false,
            "null_value": null
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val literalTokens = tokens.filter { it.isLiteral() }
        
        // 验证字符串字面量
        val stringLiterals = literalTokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringLiterals.any { it.literal == "Hello, 世界!" }, "应该找到中文字符串字面量")
        
        // 验证整数字面量
        val integerLiterals = literalTokens.filter { it.type == TokenType.INTEGER_LITERAL }
        assertTrue(integerLiterals.any { it.literal == 42L }, "应该找到整数字面量 42")
        
        // 验证浮点数字面量
        val floatLiterals = literalTokens.filter { it.type == TokenType.FLOAT_LITERAL }
        assertTrue(floatLiterals.any { (it.literal as Double) == 3.14159 }, "应该找到浮点数字面量 3.14159")
        assertTrue(floatLiterals.any { (it.literal as Double) == 1.23E-4 }, "应该找到科学计数法字面量")
        
        // 验证布尔字面量
        val booleanLiterals = literalTokens.filter { it.type == TokenType.BOOLEAN_LITERAL }
        assertTrue(booleanLiterals.any { it.literal == true }, "应该找到 true 字面量")
        assertTrue(booleanLiterals.any { it.literal == false }, "应该找到 false 字面量")
        
        // 验证字面量总数
        assertTrue(literalTokens.size >= 10, "应该找到至少 10 个字面量")
    }
    
    /**
     * 测试中文支持
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testChineseSupport() {
        val source = """
        {
            "元数据": {
                "名称": "测试模块",
                "版本": "1.0.0"
            },
            "函数": {
                "问候": {
                    "参数": ["姓名"],
                    "返回": "String",
                    "如果": true,
                    "返回": "你好，世界！"
                }
            }
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val chineseTokens = tokens.filter { token ->
            token.lexeme.any { LexerUtils.isChineseCharacter(it) }
        }
        
        // 验证找到了中文 Token
        assertTrue(chineseTokens.isNotEmpty(), "应该找到包含中文字符的 Token")
        
        // 验证特定的中文字符串
        val expectedChineseStrings = setOf("元数据", "名称", "测试模块", "版本", "函数", "你好，世界！")
        val foundChineseStrings = chineseTokens
            .filter { it.type == TokenType.STRING_LITERAL }
            .mapNotNull { it.literal as? String }
            .toSet()
        
        val commonChineseStrings = expectedChineseStrings.intersect(foundChineseStrings)
        assertTrue(commonChineseStrings.isNotEmpty(), "应该找到预期的中文字符串")
        
        // 验证中文字符识别功能
        assertTrue(LexerUtils.isChineseCharacter('中'), "'中' 应该被识别为中文字符")
        assertTrue(LexerUtils.isChineseCharacter('文'), "'文' 应该被识别为中文字符")
        assertFalse(LexerUtils.isChineseCharacter('a'), "'a' 不应该被识别为中文字符")
    }
    
    /**
     * 测试注释处理
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testComments() {
        val source = """
        {
            // 这是单行注释
            "key1": "value1",
            /* 这是
               多行注释 */
            "key2": "value2"
            /* 嵌套 /* 注释 */ 测试 */
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        // 注释应该被跳过，不出现在 Token 流中
        val commentTokens = tokens.filter { it.type == TokenType.COMMENT }
        assertEquals(0, commentTokens.size, "注释应该被跳过，不应该出现在 Token 流中")
        
        // 验证正常的 Token 仍然存在
        assertTrue(tokens.any { it.type == TokenType.STRING_LITERAL && it.literal == "key1" }, 
                  "应该找到 key1 字符串")
        assertTrue(tokens.any { it.type == TokenType.STRING_LITERAL && it.literal == "value1" }, 
                  "应该找到 value1 字符串")
        assertTrue(tokens.any { it.type == TokenType.STRING_LITERAL && it.literal == "key2" }, 
                  "应该找到 key2 字符串")
        assertTrue(tokens.any { it.type == TokenType.STRING_LITERAL && it.literal == "value2" }, 
                  "应该找到 value2 字符串")
        
        // 验证结构完整性
        assertTrue(tokens.any { it.type == TokenType.LEFT_BRACE }, "应该有左大括号")
        assertTrue(tokens.any { it.type == TokenType.RIGHT_BRACE }, "应该有右大括号")
        assertTrue(tokens.any { it.type == TokenType.EOF }, "应该有 EOF Token")
    }
    

    
    /**
     * 测试位置信息跟踪
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testLocationTracking() {
        val source = """
        {
            "line1": "value1",
            "line2": "value2"
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        // 验证所有 Token 都有有效的位置信息
        tokens.forEach { token ->
            assertNotNull(token.location, "Token ${token.type} 应该有位置信息")
            assertTrue(token.location.isValid(), "Token ${token.type} 的位置信息应该有效")
            assertEquals("test.lang.json", token.location.file, "文件名应该正确")
        }
        
        // 验证第一个 Token 的位置
        val firstToken = tokens.first()
        assertEquals(1, firstToken.location.startLine, "第一个 Token 应该在第 1 行")
        assertEquals(1, firstToken.location.startColumn, "第一个 Token 应该在第 1 列")
        
        // 验证位置信息的递增性（行号和列号应该合理递增）
        var previousLocation = tokens.first().location
        for (i in 1 until tokens.size) {
            val currentLocation = tokens[i].location
            assertTrue(
                currentLocation.startLine >= previousLocation.startLine,
                "Token 位置应该按顺序递增"
            )
            if (currentLocation.startLine == previousLocation.startLine) {
                assertTrue(
                    currentLocation.startColumn >= previousLocation.endColumn,
                    "同一行的 Token 列号应该递增"
                )
            }
            previousLocation = currentLocation
        }
    }
    
    /**
     * 测试错误处理
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun testErrorHandling() {
        val source = """
        {
            "valid_key": "valid_value",
            "invalid_char": @#$%,
            "another_key": "another_value"
        }
        """.trimIndent()
        
        val errorReporter = DefaultErrorReporter()
        val lexer = JSONLangLexer(source, "error_test.lang.json", errorReporter)
        val tokens = mutableListOf<Token>()
        
        // 收集所有 Token，包括错误 Token，但限制数量防止无限循环
        var tokenCount = 0
        val maxTokens = 100
        
        while (tokenCount < maxTokens) {
            val token = lexer.nextToken()
            tokens.add(token)
            tokenCount++
            if (token.type == TokenType.EOF) break
        }
        
        // 验证基本结构仍然可以解析
        assertTrue(tokens.any { it.type == TokenType.LEFT_BRACE }, "应该能解析左大括号")
        assertTrue(tokens.any { it.type == TokenType.RIGHT_BRACE }, "应该能解析右大括号")
        
        // 验证找到了有效的字符串
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringTokens.any { it.literal == "valid_key" }, "应该找到有效的键")
        assertTrue(stringTokens.any { it.literal == "valid_value" }, "应该找到有效的值")
        
        // 验证到达了文件末尾
        assertTrue(tokens.any { it.type == TokenType.EOF }, "应该到达文件末尾")
    }
    
    /**
     * 测试数字解析的边界情况
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testNumberParsing() {
        val source = """
        {
            "integers": [0, 123, -456],
            "floats": [0.0, 123.456, -789.012],
            "scientific": [1e5, 2.5e-3, -1.23E+10]
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val integerTokens = tokens.filter { it.type == TokenType.INTEGER_LITERAL }
        val floatTokens = tokens.filter { it.type == TokenType.FLOAT_LITERAL }
        
        // 验证整数解析
        assertTrue(integerTokens.any { it.literal == 0L }, "应该解析整数 0")
        assertTrue(integerTokens.any { it.literal == 123L }, "应该解析整数 123")
        
        // 验证浮点数解析
        assertTrue(floatTokens.any { (it.literal as Double) == 0.0 }, "应该解析浮点数 0.0")
        assertTrue(floatTokens.any { (it.literal as Double) == 123.456 }, "应该解析浮点数 123.456")
        
        // 验证科学计数法
        assertTrue(floatTokens.any { (it.literal as Double) == 1e5 }, "应该解析科学计数法 1e5")
        assertTrue(floatTokens.any { (it.literal as Double) == 2.5e-3 }, "应该解析科学计数法 2.5e-3")
    }
    
    /**
     * 测试字符串转义序列
     */
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    fun testStringEscaping() {
        val source = """
        {
            "escaped": "Hello\nWorld\t\"Quote\"\\"
        }
        """.trimIndent()
        
        val lexer = createLexer(source)
        val tokens = mutableListOf<Token>()
        
        while (true) {
            val token = lexer.nextToken()
            tokens.add(token)
            if (token.type == TokenType.EOF) break
        }
        
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        val escapedString = stringTokens.find { it.literal.toString().contains("Hello") }
        
        assertNotNull(escapedString, "应该找到包含转义序列的字符串")
        val value = escapedString!!.literal as String
        assertTrue(value.contains("\n"), "应该包含换行符")
        assertTrue(value.contains("\t"), "应该包含制表符")
        assertTrue(value.contains("\""), "应该包含引号")
        assertTrue(value.contains("\\"), "应该包含反斜杠")
    }
}


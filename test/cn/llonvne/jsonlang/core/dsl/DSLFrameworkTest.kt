package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.CompilerError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * DSL 框架测试类
 */
class DSLFrameworkTest {
    
    private lateinit var errorReporter: TestErrorReporter
    private lateinit var dslRegistry: DSLRegistry
    private lateinit var dslToolkit: DSLToolkit
    
    @BeforeEach
    fun setUp() {
        errorReporter = TestErrorReporter()
        dslRegistry = DSLRegistry(errorReporter)
        dslToolkit = DSLToolkit()
    }
    
    @Test
    fun `test DSL registration and basic functionality`() {
        // 创建一个简单的 DSL
        val sqlDSL = createSQLDSL()
        
        // 注册 DSL
        assertTrue(dslRegistry.registerDSL(sqlDSL))
        assertTrue(dslRegistry.isDSLRegistered("SQL"))
        
        // 验证注册的 DSL
        val registeredDSL = dslRegistry.getDSL("SQL")
        assertNotNull(registeredDSL)
        assertEquals("SQL", registeredDSL!!.name)
    }
    
    @Test
    fun `test DSL syntax parsing`() {
        val sqlDSL = createSQLDSL()
        dslRegistry.registerDSL(sqlDSL)
        
        val location = SourceLocation("test", 1, 1, 1, 1, 0, 0)
        
        // 测试 SELECT 语句解析
        val selectInput = "SELECT id, name FROM users"
        val selectNode = dslRegistry.parseDSL("SQL", selectInput, location)
        

        
        assertNotNull(selectNode)
        assertEquals("SQL", selectNode!!.dslName)
        assertEquals("select", selectNode.patternName)
        assertEquals("id, name", selectNode.fields["fields"])
        assertEquals("users", selectNode.fields["table"])
    }
    
    @Test
    fun `test pattern matching directly`() {
        // 直接测试模式匹配
        val pattern = cn.llonvne.jsonlang.core.ast.DSLPattern("SELECT \${fields} FROM \${table}", listOf("fields", "table"))
        val compiledPattern = CompiledDSLPattern("test", pattern)
        
        val input = "SELECT id, name FROM users"
        val result = compiledPattern.match(input)
        
        assertTrue(compiledPattern.matches(input))
        assertNotNull(result)
        assertEquals("id, name", result!!["fields"])
        assertEquals("users", result["table"])
    }
    
    @Test
    fun `test DSL transformation`() {
        val sqlDSL = createSQLDSL()
        dslRegistry.registerDSL(sqlDSL)
        
        val location = SourceLocation("test", 1, 1, 1, 1, 0, 0)
        val selectInput = "SELECT id, name FROM users"
        val selectNode = dslRegistry.parseDSL("SQL", selectInput, location)
        
        assertNotNull(selectNode)
        
        // 转换 DSL 节点
        val astNode = dslRegistry.transformDSL(selectNode!!)
        assertNotNull(astNode)
        assertTrue(astNode is CallExpression)
        
        val callExpr = astNode as CallExpression
        assertTrue(callExpr.function is IdentifierExpression)
        assertEquals("sqlQuery", (callExpr.function as IdentifierExpression).name)
        assertEquals(2, callExpr.arguments.size)
    }
    
    @Test
    fun `test DSL syntax extension`() {
        val syntax = DSLSyntaxBuilder("TestDSL")
            .pattern("simple", "hello \${name}", "name")
            .pattern("complex", "greet \${name} with \${message}", "name", "message")
            .build()
        
        val extension = DSLSyntaxExtension("TestDSL", syntax)
        val location = SourceLocation("test", 1, 1, 1, 1, 0, 0)
        
        // 测试简单模式
        val simpleNode = extension.parse("hello world", location)
        assertNotNull(simpleNode)
        assertEquals("simple", simpleNode!!.patternName)
        assertEquals("world", simpleNode.fields["name"])
        
        // 测试复杂模式
        val complexNode = extension.parse("greet Alice with Good morning", location)
        assertNotNull(complexNode)
        assertEquals("complex", complexNode!!.patternName)
        assertEquals("Alice", complexNode.fields["name"])
        assertEquals("Good morning", complexNode.fields["message"])
    }
    
    @Test
    fun `test DSL transformation rule`() {
        val transformation = DSLTransformation(
            target = "kotlin",
            template = "println(\"\${message}\")"
        )
        
        val rule = DSLTransformationRule("TestDSL", transformation)
        val location = SourceLocation("test", 1, 1, 1, 1, 0, 0)
        
        val dslNode = DSLNode(
            dslName = "TestDSL",
            patternName = "print",
            fields = mapOf("message" to "Hello World"),
            location = location
        )
        
        val result = rule.transform(dslNode)
        assertNotNull(result)
        assertTrue(result is cn.llonvne.jsonlang.core.ast.CodeLiteralExpression)
        
        val codeExpr = result as cn.llonvne.jsonlang.core.ast.CodeLiteralExpression
        assertEquals("kotlin", codeExpr.language)
        assertEquals("println(\"Hello World\")", codeExpr.code)
    }
    
    @Test
    fun `test DSL template engine`() {
        val engine = DSLTemplateEngine()
        
        // 测试基本字段替换
        val template1 = "Hello \${name}!"
        val fields1 = mapOf("name" to "Alice")
        val result1 = engine.expand(template1, fields1)
        assertEquals("Hello Alice!", result1)
        
        // 测试条件模板
        val template2 = "{% if name %}Hello \${name}!{% endif %}"
        val fields2 = mapOf("name" to "Bob")
        val result2 = engine.expand(template2, fields2)
        assertEquals("Hello Bob!", result2)
        
        // 测试空条件
        val fields3 = mapOf<String, String>()
        val result3 = engine.expand(template2, fields3)
        assertEquals("", result3)
    }
    
    @Test
    fun `test DSL toolkit examples`() {
        val examples = dslToolkit.createExamples()
        
        assertTrue(examples.isNotEmpty())
        assertTrue(examples.any { it.name == "SQL" })
        assertTrue(examples.any { it.name == "HTML" })
        assertTrue(examples.any { it.name == "Config" })
        assertTrue(examples.any { it.name == "Test" })
        assertTrue(examples.any { it.name == "Validation" })
        
        // 验证每个示例都有完整的定义
        examples.forEach { dsl ->
            assertNotNull(dsl.syntax)
            assertNotNull(dsl.semantics)
            assertNotNull(dsl.transformation)
            assertTrue(dsl.syntax.patterns.isNotEmpty())
            assertTrue(dsl.semantics.rules.isNotEmpty())
        }
    }
    
    @Test
    fun `test DSL builder fluent API`() {
        val dsl = DSLBuilder("TestDSL")
            .syntax(
                DSLSyntaxBuilder("TestDSL")
                    .pattern("greet", "hello \${name}", "name")
                    .build()
            )
            .semantics(
                DSLSemantics(
                    mapOf(
                        "greet" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "greet",
                                "arguments" to listOf("\${name}")
                            )
                        )
                    )
                )
            )
            .transformation(
                DSLTransformationBuilder("TestDSL")
                    .target("kotlin")
                    .template("greet(\"\${name}\")")
                    .build()
            )
            .annotation("Description", mapOf("value" to "测试 DSL"))
            .build()
        
        assertEquals("TestDSL", dsl.name)
        assertEquals(1, dsl.syntax.patterns.size)
        assertEquals(1, dsl.semantics.rules.size)
        assertEquals("kotlin", dsl.transformation.target)
        assertEquals(1, dsl.annotations.size)
    }
    
    @Test
    fun `test DSL validation`() {
        val validator = DSLValidator()
        
        // 测试有效的 DSL
        val validDSL = createSQLDSL()
        val validErrors = validator.validate(validDSL)
        assertTrue(validErrors.isEmpty())
        
        // 测试无效的 DSL（空名称）
        val invalidDSL = validDSL.copy(name = "")
        val invalidErrors = validator.validate(invalidDSL)
        assertTrue(invalidErrors.isNotEmpty())
        assertTrue(invalidErrors.any { it.message.contains("名称不能为空") })
    }
    
    @Test
    fun `test DSL syntax validator`() {
        val validator = DSLSyntaxValidator()
        
        // 测试有效语法
        val validSyntax = DSLSyntax(
            mapOf(
                "test" to DSLPattern("hello \${name}", listOf("name"))
            )
        )
        val validErrors = validator.validate(validSyntax)
        assertTrue(validErrors.isEmpty())
        
        // 测试无效语法（字段未使用）
        val invalidSyntax = DSLSyntax(
            mapOf(
                "test" to DSLPattern("hello world", listOf("name"))
            )
        )
        val invalidErrors = validator.validate(invalidSyntax)
        assertTrue(invalidErrors.isNotEmpty())
        assertTrue(invalidErrors.any { it.message.contains("未使用") })
    }
    
    @Test
    fun `test DSL transformation validator`() {
        val validator = DSLTransformationValidator()
        
        // 测试有效转换
        val validTransformation = DSLTransformation("kotlin", "println(\"\${message}\")")
        val validErrors = validator.validate(validTransformation)
        assertTrue(validErrors.isEmpty())
        
        // 测试无效转换（不支持的目标）
        val invalidTransformation = DSLTransformation("unsupported", "code")
        val invalidErrors = validator.validate(invalidTransformation)
        assertTrue(invalidErrors.isNotEmpty())
        assertTrue(invalidErrors.any { it.message.contains("不支持的目标后端") })
    }
    
    @Test
    fun `test DSL registry statistics`() {
        val sqlDSL = createSQLDSL()
        val htmlDSL = createHTMLDSL()
        
        dslRegistry.registerDSL(sqlDSL)
        dslRegistry.registerDSL(htmlDSL)
        
        val stats = dslRegistry.getStatistics()
        assertEquals(2, stats.totalDSLs)
        assertTrue(stats.totalPatterns > 0)
        assertTrue(stats.totalSemanticRules > 0)
        assertTrue(stats.dslsByTarget.containsKey("kotlin"))
        assertTrue(stats.dslsByTarget.containsKey("javascript"))
    }
    
    @Test
    fun `test DSL example generator`() {
        val generator = DSLExampleGenerator()
        val sqlDSL = createSQLDSL()
        
        val examples = generator.generateExamples(sqlDSL)
        assertTrue(examples.isNotEmpty())
        
        examples.forEach { example ->
            assertNotNull(example.name)
            assertNotNull(example.description)
            assertNotNull(example.input)
            assertNotNull(example.expectedOutput)
        }
    }
    
    @Test
    fun `test DSL documentation generator`() {
        val generator = DSLDocumentationGenerator()
        val sqlDSL = createSQLDSL()
        
        val documentation = generator.generateDocumentation(sqlDSL)
        assertEquals("SQL", documentation.name)
        assertTrue(documentation.syntax.contains("语法模式"))
        assertTrue(documentation.semantics.contains("语义规则"))
        assertTrue(documentation.transformation.contains("转换规则"))
        assertTrue(documentation.examples.isNotEmpty())
    }
    
    // 辅助方法
    private fun createSQLDSL(): DSLDeclaration {
        val syntax = DSLSyntax(
            mapOf(
                "select" to DSLPattern("SELECT \${fields} FROM \${table}", listOf("fields", "table")),
                "insert" to DSLPattern("INSERT INTO \${table} VALUES \${values}", listOf("table", "values"))
            )
        )
        
        val semantics = DSLSemantics(
            mapOf(
                "select" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlQuery",
                        "arguments" to listOf("\${fields}", "\${table}")
                    )
                ),
                "insert" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlInsert",
                        "arguments" to listOf("\${table}", "\${values}")
                    )
                )
            )
        )
        
        val transformation = DSLTransformation("kotlin", "sql.\${function}(\${arguments})")
        
        return DSLDeclaration(
            name = "SQL",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("test", 1, 1, 1, 1, 0, 0),
            annotations = listOf(
                Annotation("Description", mapOf("value" to cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue("SQL DSL for database queries")), location = SourceLocation("test", 1, 1, 1, 1, 0, 0))
            )
        )
    }
    
    private fun createHTMLDSL(): DSLDeclaration {
        val syntax = DSLSyntax(
            mapOf(
                "element" to DSLPattern("<\${tag}>\${content}</\${tag}>", listOf("tag", "content"))
            )
        )
        
        val semantics = DSLSemantics(
            mapOf(
                "element" to DSLSemanticRule(
                    type = "object",
                    mapping = mapOf(
                        "properties" to mapOf(
                            "tag" to "\${tag}",
                            "content" to "\${content}"
                        )
                    )
                )
            )
        )
        
        val transformation = DSLTransformation("javascript", "createElement('\${tag}', \${content})")
        
        return DSLDeclaration(
            name = "HTML",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("test", 1, 1, 1, 1, 0, 0)
        )
    }
    
    // 测试用的错误报告器
    private class TestErrorReporter : ErrorReporter {
        private val errorList = mutableListOf<CompilerError>()
        
        override fun report(error: CompilerError) {
            errorList.add(error)
        }
        
        override fun hasErrors(): Boolean = errorList.isNotEmpty()
        
        override fun hasWarnings(): Boolean = errorList.any { it.isWarning() }
        
        override fun getErrors(): List<CompilerError> = errorList.toList()
        
        override fun getErrorCount(): Int = errorList.count { it.isFatal() }
        
        override fun getWarningCount(): Int = errorList.count { it.isWarning() }
        
        override fun clear() {
            errorList.clear()
        }
    }
}
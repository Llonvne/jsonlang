package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.CompilerError

/**
 * DSL 集成示例
 * 展示如何使用 DSL 框架创建和使用自定义 DSL
 */
class DSLIntegrationExample {
    
    private val errorReporter = SimpleErrorReporter()
    private val dslRegistry = DSLRegistry(errorReporter)
    private val dslToolkit = DSLToolkit()
    
    /**
     * 运行完整的 DSL 示例
     */
    fun runExample() {
        println("=== JSONLang DSL 框架示例 ===\n")
        
        // 1. 创建和注册预定义的 DSL
        registerPredefinedDSLs()
        
        // 2. 创建自定义 DSL
        createCustomDSL()
        
        // 3. 演示 DSL 使用
        demonstrateDSLUsage()
        
        // 4. 展示 DSL 文档生成
        generateDocumentation()
        
        // 5. 显示统计信息
        showStatistics()
    }
    
    /**
     * 注册预定义的 DSL
     */
    private fun registerPredefinedDSLs() {
        println("1. 注册预定义的 DSL...")
        
        val examples = dslToolkit.createExamples()
        val results = dslRegistry.registerDSLs(examples)
        
        results.forEach { (name, success) ->
            if (success) {
                println("   ✓ 成功注册 DSL: $name")
            } else {
                println("   ✗ 注册失败 DSL: $name")
            }
        }
        println()
    }
    
    /**
     * 创建自定义 DSL
     */
    private fun createCustomDSL() {
        println("2. 创建自定义 DSL...")
        
        // 创建一个简单的数学表达式 DSL
        val mathDSL = DSLBuilder("Math")
            .syntax { 
                DSLSyntaxBuilder("Math")
                    .pattern("add", "\${a} + \${b}", "a", "b")
                    .pattern("subtract", "\${a} - \${b}", "a", "b")
                    .pattern("multiply", "\${a} * \${b}", "a", "b")
                    .pattern("divide", "\${a} / \${b}", "a", "b")
                    .pattern("power", "\${a} ^ \${b}", "a", "b")
                    .build()
            }
            .semantics(
                DSLSemantics(
                    mapOf(
                        "add" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "add",
                                "arguments" to listOf("\${a}", "\${b}")
                            )
                        ),
                        "subtract" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "subtract",
                                "arguments" to listOf("\${a}", "\${b}")
                            )
                        ),
                        "multiply" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "multiply",
                                "arguments" to listOf("\${a}", "\${b}")
                            )
                        ),
                        "divide" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "divide",
                                "arguments" to listOf("\${a}", "\${b}")
                            )
                        ),
                        "power" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "power",
                                "arguments" to listOf("\${a}", "\${b}")
                            )
                        )
                    )
                )
            )
            .transformation { 
                DSLTransformationBuilder("Math")
                    .target("kotlin")
                    .template("Math.\${function}(\${a}, \${b})")
                    .build()
            }
            .annotation("Description", mapOf("value" to "数学表达式 DSL"))
            .annotation("Version", mapOf("value" to "1.0.0"))
            .build()
        
        val success = dslRegistry.registerDSL(mathDSL)
        if (success) {
            println("   ✓ 成功创建并注册自定义数学 DSL")
        } else {
            println("   ✗ 创建自定义数学 DSL 失败")
        }
        println()
    }
    
    /**
     * 演示 DSL 使用
     */
    private fun demonstrateDSLUsage() {
        println("3. 演示 DSL 使用...")
        
        val location = SourceLocation("example", 1, 1, 1, 1, 0, 0)
        
        // SQL DSL 示例
        demonstrateSQLDSL(location)
        
        // HTML DSL 示例
        demonstrateHTMLDSL(location)
        
        // 数学 DSL 示例
        demonstrateMathDSL(location)
        
        // 配置 DSL 示例
        demonstrateConfigDSL(location)
        
        println()
    }
    
    /**
     * 演示 SQL DSL
     */
    private fun demonstrateSQLDSL(location: SourceLocation) {
        println("   SQL DSL 示例:")
        
        val sqlInputs = listOf(
            "SELECT id, name FROM users",
            "SELECT * FROM products WHERE price > 100"
        )
        
        sqlInputs.forEach { input ->
            val node = dslRegistry.parseDSL("SQL", input, location)
            if (node != null) {
                val ast = dslRegistry.transformDSL(node)
                println("     输入: $input")
                println("     解析: ${node.patternName} - ${node.fields}")
                println("     转换: ${ast?.nodeType}")
                println()
            }
        }
    }
    
    /**
     * 演示 HTML DSL
     */
    private fun demonstrateHTMLDSL(location: SourceLocation) {
        println("   HTML DSL 示例:")
        
        val htmlInputs = listOf(
            "<div>Hello World</div>",
            "<h1>标题</h1>",
            "<p>这是一个段落</p>"
        )
        
        htmlInputs.forEach { input ->
            val node = dslRegistry.parseDSL("HTML", input, location)
            if (node != null) {
                val ast = dslRegistry.transformDSL(node)
                println("     输入: $input")
                println("     解析: ${node.patternName} - ${node.fields}")
                println("     转换: ${ast?.nodeType}")
                println()
            }
        }
    }
    
    /**
     * 演示数学 DSL
     */
    private fun demonstrateMathDSL(location: SourceLocation) {
        println("   数学 DSL 示例:")
        
        val mathInputs = listOf(
            "5 + 3",
            "10 - 4",
            "6 * 7",
            "15 / 3",
            "2 ^ 8"
        )
        
        mathInputs.forEach { input ->
            val node = dslRegistry.parseDSL("Math", input, location)
            if (node != null) {
                val ast = dslRegistry.transformDSL(node)
                println("     输入: $input")
                println("     解析: ${node.patternName} - ${node.fields}")
                println("     转换: ${ast?.nodeType}")
                println()
            }
        }
    }
    
    /**
     * 演示配置 DSL
     */
    private fun demonstrateConfigDSL(location: SourceLocation) {
        println("   配置 DSL 示例:")
        
        val configInputs = listOf(
            "database.host = localhost",
            "server.port = 8080",
            "[database]",
            "include config.properties"
        )
        
        configInputs.forEach { input ->
            val node = dslRegistry.parseDSL("Config", input, location)
            if (node != null) {
                val ast = dslRegistry.transformDSL(node)
                println("     输入: $input")
                println("     解析: ${node.patternName} - ${node.fields}")
                println("     转换: ${ast?.nodeType}")
                println()
            }
        }
    }
    
    /**
     * 生成文档
     */
    private fun generateDocumentation() {
        println("4. 生成 DSL 文档...")
        
        val generator = DSLDocumentationGenerator()
        val registeredDSLs = dslRegistry.getAllDSLs()
        
        registeredDSLs.forEach { (name, dsl) ->
            val documentation = generator.generateDocumentation(dsl)
            println("   DSL: $name")
            println("   描述: ${documentation.description}")
            println("   模式数量: ${dsl.syntax.patterns.size}")
            println("   语义规则数量: ${dsl.semantics.rules.size}")
            println("   目标后端: ${dsl.transformation.target}")
            println("   示例数量: ${documentation.examples.size}")
            println()
        }
    }
    
    /**
     * 显示统计信息
     */
    private fun showStatistics() {
        println("5. DSL 统计信息:")
        
        val stats = dslRegistry.getStatistics()
        println("   总 DSL 数量: ${stats.totalDSLs}")
        println("   总模式数量: ${stats.totalPatterns}")
        println("   总语义规则数量: ${stats.totalSemanticRules}")
        println("   按目标后端分组:")
        stats.dslsByTarget.forEach { (target, count) ->
            println("     $target: $count 个 DSL")
        }
        
        if (errorReporter.hasErrors()) {
            println("\n   错误信息:")
            errorReporter.getErrors().forEach { error ->
                println("     ${error.severity}: ${error.message}")
            }
        }
    }
    
    /**
     * 创建复杂的 DSL 示例
     */
    fun createComplexDSLExample() {
        println("\n=== 复杂 DSL 示例：REST API DSL ===\n")
        
        val apiDSL = DSLBuilder("RestAPI")
            .syntax {
                DSLSyntaxBuilder("RestAPI")
                    .pattern("get", "GET \${path}", "path")
                    .pattern("post", "POST \${path} {\${body}}", "path", "body")
                    .pattern("put", "PUT \${path} {\${body}}", "path", "body")
                    .pattern("delete", "DELETE \${path}", "path")
                    .pattern("route", "route \${path} {\${handlers}}", "path", "handlers")
                    .pattern("middleware", "use \${middleware}", "middleware")
                    .build()
            }
            .semantics(
                DSLSemantics(
                    mapOf(
                        "get" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "get",
                                "arguments" to listOf("\${path}")
                            )
                        ),
                        "post" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "post",
                                "arguments" to listOf("\${path}", "\${body}")
                            )
                        ),
                        "put" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "put",
                                "arguments" to listOf("\${path}", "\${body}")
                            )
                        ),
                        "delete" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "delete",
                                "arguments" to listOf("\${path}")
                            )
                        ),
                        "route" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "route",
                                "arguments" to listOf("\${path}", "\${handlers}")
                            )
                        ),
                        "middleware" to DSLSemanticRule(
                            type = "function_call",
                            mapping = mapOf(
                                "function" to "use",
                                "arguments" to listOf("\${middleware}")
                            )
                        )
                    )
                )
            )
            .transformation(
                DSLTransformation(
                    target = "kotlin",
                    template = "app.\${function}(\${arguments})"
                )
            )
            .annotation("Description", mapOf("value" to "REST API 路由 DSL"))
            .annotation("Version", mapOf("value" to "1.0.0"))
            .annotation("Author", mapOf("value" to "JSONLang Team"))
            .build()
        
        val success = dslRegistry.registerDSL(apiDSL)
        if (success) {
            println("✓ 成功创建 REST API DSL")
            
            // 演示使用
            val location = SourceLocation("api_example", 1, 1, 1, 1, 0, 0)
            val apiExamples = listOf(
                "GET /users",
                "POST /users {user_data}",
                "PUT /users/123 {updated_data}",
                "DELETE /users/123",
                "use authentication"
            )
            
            println("\nAPI DSL 使用示例:")
            apiExamples.forEach { input ->
                val node = dslRegistry.parseDSL("RestAPI", input, location)
                if (node != null) {
                    println("  输入: $input")
                    println("  解析: ${node.patternName} - ${node.fields}")
                    println()
                }
            }
        } else {
            println("✗ 创建 REST API DSL 失败")
        }
    }
}

/**
 * 简单的错误报告器实现
 */
private class SimpleErrorReporter : ErrorReporter {
    private val errors = mutableListOf<CompilerError>()
    
    override fun report(error: CompilerError) {
        errors.add(error)
    }
    
    override fun hasErrors(): Boolean = errors.isNotEmpty()
    
    override fun hasWarnings(): Boolean = errors.any { it.isWarning() }
    
    override fun getErrors(): List<CompilerError> = errors.toList()
    
    override fun getErrorCount(): Int = errors.count { it.isFatal() }
    
    override fun getWarningCount(): Int = errors.count { it.isWarning() }
    
    override fun clear() {
        errors.clear()
    }
}

/**
 * 主函数 - 运行 DSL 示例
 */
fun main() {
    val example = DSLIntegrationExample()
    example.runExample()
    example.createComplexDSLExample()
}
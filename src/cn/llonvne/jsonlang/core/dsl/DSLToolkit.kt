package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation

/**
 * DSL 构建工具包
 * 提供便捷的 API 来创建和管理 DSL
 */
class DSLToolkit {
    private val dslBuilders = mutableMapOf<String, DSLBuilder>()
    
    /**
     * 创建新的 DSL 构建器
     */
    fun createDSL(name: String): DSLBuilder {
        val builder = DSLBuilder(name)
        dslBuilders[name] = builder
        return builder
    }
    
    /**
     * 获取 DSL 构建器
     */
    fun getDSLBuilder(name: String): DSLBuilder? = dslBuilders[name]
    
    /**
     * 构建所有 DSL 声明
     */
    fun buildAll(): List<DSLDeclaration> {
        return dslBuilders.values.map { it.build() }
    }
    
    /**
     * 创建预定义的 DSL 示例
     */
    fun createExamples(): List<DSLDeclaration> {
        return listOf(
            createSQLDSL(),
            createHTMLDSL(),
            createConfigDSL(),
            createTestDSL(),
            createValidationDSL()
        )
    }
    
    /**
     * 创建 SQL 查询 DSL
     */
    private fun createSQLDSL(): DSLDeclaration {
        val syntax = DSLSyntaxBuilder("SQL")
            .pattern("select", "SELECT \${fields} FROM \${table}", "fields", "table")
            .pattern("select_where", "SELECT \${fields} FROM \${table} WHERE \${condition}", "fields", "table", "condition")
            .pattern("insert", "INSERT INTO \${table} (\${columns}) VALUES (\${values})", "table", "columns", "values")
            .pattern("update", "UPDATE \${table} SET \${assignments} WHERE \${condition}", "table", "assignments", "condition")
            .pattern("delete", "DELETE FROM \${table} WHERE \${condition}", "table", "condition")
            .build()
        
        val semantics = DSLSemantics(
            mapOf(
                "select" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlQuery",
                        "arguments" to listOf("\${fields}", "\${table}")
                    )
                ),
                "select_where" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlQuery",
                        "arguments" to listOf("\${fields}", "\${table}", "\${condition}")
                    )
                ),
                "insert" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlInsert",
                        "arguments" to listOf("\${table}", "\${columns}", "\${values}")
                    )
                ),
                "update" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlUpdate",
                        "arguments" to listOf("\${table}", "\${assignments}", "\${condition}")
                    )
                ),
                "delete" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "sqlDelete",
                        "arguments" to listOf("\${table}", "\${condition}")
                    )
                )
            )
        )
        
        val transformation = DSLTransformation(
            target = "kotlin",
            template = "sqlExecutor.\${function}(\${arguments})"
        )
        
        return DSLDeclaration(
            name = "SQL",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0),
            annotations = listOf(
                Annotation("Description", mapOf("value" to cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue("SQL 查询 DSL")), location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0))
            )
        )
    }
    
    /**
     * 创建 HTML 模板 DSL
     */
    private fun createHTMLDSL(): DSLDeclaration {
        val syntax = DSLSyntaxBuilder("HTML")
            .pattern("element", "<\${tag}>\${content}</\${tag}>", "tag", "content")
            .pattern("element_with_attrs", "<\${tag} \${attributes}>\${content}</\${tag}>", "tag", "attributes", "content")
            .pattern("self_closing", "<\${tag} \${attributes}/>", "tag", "attributes")
            .pattern("text", "\${text}", "text")
            .build()
        
        val semantics = DSLSemantics(
            mapOf(
                "element" to DSLSemanticRule(
                    type = "object",
                    mapping = mapOf(
                        "properties" to mapOf(
                            "tag" to "\${tag}",
                            "content" to "\${content}",
                            "attributes" to "{}"
                        )
                    )
                ),
                "element_with_attrs" to DSLSemanticRule(
                    type = "object",
                    mapping = mapOf(
                        "properties" to mapOf(
                            "tag" to "\${tag}",
                            "content" to "\${content}",
                            "attributes" to "\${attributes}"
                        )
                    )
                ),
                "self_closing" to DSLSemanticRule(
                    type = "object",
                    mapping = mapOf(
                        "properties" to mapOf(
                            "tag" to "\${tag}",
                            "content" to "null",
                            "attributes" to "\${attributes}"
                        )
                    )
                ),
                "text" to DSLSemanticRule(
                    type = "object",
                    mapping = mapOf(
                        "properties" to mapOf(
                            "type" to "text",
                            "content" to "\${text}"
                        )
                    )
                )
            )
        )
        
        val transformation = DSLTransformation(
            target = "javascript",
            template = "createElement('\${tag}', \${attributes}, \${content})"
        )
        
        return DSLDeclaration(
            name = "HTML",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0),
            annotations = listOf(
                Annotation("Description", mapOf("value" to cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue("HTML 模板 DSL")), location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0))
            )
        )
    }
    
    /**
     * 创建配置 DSL
     */
    private fun createConfigDSL(): DSLDeclaration {
        val syntax = DSLSyntaxBuilder("Config")
            .pattern("setting", "\${key} = \${value}", "key", "value")
            .pattern("section", "[\${section}]", "section")
            .pattern("comment", "# \${comment}", "comment")
            .pattern("include", "include \${file}", "file")
            .build()
        
        val semantics = DSLSemantics(
            mapOf(
                "setting" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "setSetting",
                        "arguments" to listOf("\${key}", "\${value}")
                    )
                ),
                "section" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "enterSection",
                        "arguments" to listOf("\${section}")
                    )
                ),
                "include" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "includeConfig",
                        "arguments" to listOf("\${file}")
                    )
                )
            )
        )
        
        val transformation = DSLTransformation(
            target = "kotlin",
            template = "config.\${function}(\${arguments})"
        )
        
        return DSLDeclaration(
            name = "Config",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0),
            annotations = listOf(
                Annotation("Description", mapOf("value" to cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue("配置文件 DSL")), location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0))
            )
        )
    }
    
    /**
     * 创建测试 DSL
     */
    private fun createTestDSL(): DSLDeclaration {
        val syntax = DSLSyntaxBuilder("Test")
            .pattern("describe", "describe \"\${description}\"", "description")
            .pattern("it", "it \"\${description}\"", "description")
            .pattern("expect", "expect(\${actual}).to(\${matcher})", "actual", "matcher")
            .pattern("before_each", "beforeEach", "")
            .pattern("after_each", "afterEach", "")
            .build()
        
        val semantics = DSLSemantics(
            mapOf(
                "describe" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "describe",
                        "arguments" to listOf("\${description}")
                    )
                ),
                "it" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "it",
                        "arguments" to listOf("\${description}")
                    )
                ),
                "expect" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "expect",
                        "arguments" to listOf("\${actual}", "\${matcher}")
                    )
                )
            )
        )
        
        val transformation = DSLTransformation(
            target = "kotlin",
            template = "testFramework.\${function}(\${arguments})"
        )
        
        return DSLDeclaration(
            name = "Test",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0),
            annotations = listOf(
                Annotation("Description", mapOf("value" to cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue("测试框架 DSL")), location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0))
            )
        )
    }
    
    /**
     * 创建验证 DSL
     */
    private fun createValidationDSL(): DSLDeclaration {
        val syntax = DSLSyntaxBuilder("Validation")
            .pattern("required", "\${field} is required", "field")
            .pattern("min_length", "\${field} min length \${length}", "field", "length")
            .pattern("max_length", "\${field} max length \${length}", "field", "length")
            .pattern("pattern", "\${field} matches \${pattern}", "field", "pattern")
            .pattern("range", "\${field} between \${min} and \${max}", "field", "min", "max")
            .build()
        
        val semantics = DSLSemantics(
            mapOf(
                "required" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "validateRequired",
                        "arguments" to listOf("\${field}")
                    )
                ),
                "min_length" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "validateMinLength",
                        "arguments" to listOf("\${field}", "\${length}")
                    )
                ),
                "max_length" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "validateMaxLength",
                        "arguments" to listOf("\${field}", "\${length}")
                    )
                ),
                "pattern" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "validatePattern",
                        "arguments" to listOf("\${field}", "\${pattern}")
                    )
                ),
                "range" to DSLSemanticRule(
                    type = "function_call",
                    mapping = mapOf(
                        "function" to "validateRange",
                        "arguments" to listOf("\${field}", "\${min}", "\${max}")
                    )
                )
            )
        )
        
        val transformation = DSLTransformation(
            target = "kotlin",
            template = "validator.\${function}(\${arguments})"
        )
        
        return DSLDeclaration(
            name = "Validation",
            syntax = syntax,
            semantics = semantics,
            transformation = transformation,
            location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0),
            annotations = listOf(
                Annotation("Description", mapOf("value" to cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue("数据验证 DSL")), location = SourceLocation("dsl_toolkit", 1, 1, 1, 1, 0, 0))
            )
        )
    }
}

/**
 * DSL 构建器
 * 提供流式 API 来构建完整的 DSL 定义
 */
class DSLBuilder(private val name: String) {
    private var syntax: DSLSyntax? = null
    private var semantics: DSLSemantics? = null
    private var transformation: DSLTransformation? = null
    private var visibility: Visibility = Visibility.PUBLIC
    private var location: SourceLocation = SourceLocation("builder", 1, 1, 1, 1, 0, 0)
    private var annotations: List<Annotation> = emptyList()
    
    /**
     * 设置语法定义
     */
    fun syntax(syntaxBuilder: DSLSyntaxBuilder.() -> DSLSyntax): DSLBuilder {
        val builder = DSLSyntaxBuilder(name)
        syntax = builder.syntaxBuilder()
        return this
    }
    
    /**
     * 设置语法定义
     */
    fun syntax(syntax: DSLSyntax): DSLBuilder {
        this.syntax = syntax
        return this
    }
    
    /**
     * 设置语义定义
     */
    fun semantics(semantics: DSLSemantics): DSLBuilder {
        this.semantics = semantics
        return this
    }
    
    /**
     * 设置转换定义
     */
    fun transformation(transformationBuilder: DSLTransformationBuilder.() -> DSLTransformation): DSLBuilder {
        val builder = DSLTransformationBuilder(name)
        transformation = builder.transformationBuilder()
        return this
    }
    
    /**
     * 设置转换定义
     */
    fun transformation(transformation: DSLTransformation): DSLBuilder {
        this.transformation = transformation
        return this
    }
    
    /**
     * 设置可见性
     */
    fun visibility(visibility: Visibility): DSLBuilder {
        this.visibility = visibility
        return this
    }
    
    /**
     * 设置位置信息
     */
    fun location(location: SourceLocation): DSLBuilder {
        this.location = location
        return this
    }
    
    /**
     * 添加注解
     */
    fun annotation(annotation: Annotation): DSLBuilder {
        annotations = annotations + annotation
        return this
    }
    
    /**
     * 添加注解
     */
    fun annotation(name: String, parameters: Map<String, Any> = emptyMap()): DSLBuilder {
        val annotationParams = parameters.mapValues { (_, value) ->
            when (value) {
                is String -> cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue(value)
                is Int -> cn.llonvne.jsonlang.core.annotations.AnnotationValue.IntValue(value.toLong())
                is Long -> cn.llonvne.jsonlang.core.annotations.AnnotationValue.IntValue(value)
                is Boolean -> cn.llonvne.jsonlang.core.annotations.AnnotationValue.BooleanValue(value)
                else -> cn.llonvne.jsonlang.core.annotations.AnnotationValue.StringValue(value.toString())
            }
        }
        return annotation(Annotation(name, annotationParams, location = SourceLocation("builder", 1, 1, 1, 1, 0, 0)))
    }
    
    /**
     * 构建 DSL 声明
     */
    fun build(): DSLDeclaration {
        require(syntax != null) { "DSL 语法定义不能为空" }
        require(semantics != null) { "DSL 语义定义不能为空" }
        require(transformation != null) { "DSL 转换定义不能为空" }
        
        return DSLDeclaration(
            name = name,
            syntax = syntax!!,
            semantics = semantics!!,
            transformation = transformation!!,
            visibility = visibility,
            location = location,
            annotations = annotations
        )
    }
}

/**
 * DSL 示例生成器
 */
class DSLExampleGenerator {
    /**
     * 生成 DSL 使用示例
     */
    fun generateExamples(dsl: DSLDeclaration): List<DSLExample> {
        val examples = mutableListOf<DSLExample>()
        
        for ((patternName, pattern) in dsl.syntax.patterns) {
            val example = generatePatternExample(patternName, pattern)
            examples.add(example)
        }
        
        return examples
    }
    
    /**
     * 生成模式示例
     */
    private fun generatePatternExample(patternName: String, pattern: DSLPattern): DSLExample {
        var exampleInput = pattern.pattern
        
        // 为每个字段生成示例值
        for (field in pattern.fields) {
            val exampleValue = generateExampleValue(field)
            exampleInput = exampleInput.replace("\${$field}", exampleValue)
        }
        
        return DSLExample(
            name = patternName,
            description = "示例：$patternName 模式的使用",
            input = exampleInput,
            expectedOutput = "// 转换后的代码将在这里显示"
        )
    }
    
    /**
     * 生成字段示例值
     */
    private fun generateExampleValue(fieldName: String): String {
        return when (fieldName.lowercase()) {
            "table" -> "users"
            "fields" -> "id, name, email"
            "condition" -> "age > 18"
            "tag" -> "div"
            "content" -> "Hello World"
            "attributes" -> "class=\"container\""
            "key" -> "database.host"
            "value" -> "localhost"
            "section" -> "database"
            "file" -> "config.properties"
            "description" -> "测试用例描述"
            "actual" -> "result"
            "matcher" -> "equal(expected)"
            "field" -> "username"
            "length" -> "8"
            "pattern" -> "/^[a-zA-Z0-9]+$/"
            "min" -> "1"
            "max" -> "100"
            else -> "example_$fieldName"
        }
    }
}

/**
 * DSL 示例数据类
 */
data class DSLExample(
    val name: String,
    val description: String,
    val input: String,
    val expectedOutput: String
)

/**
 * DSL 文档生成器
 */
class DSLDocumentationGenerator {
    /**
     * 生成 DSL 文档
     */
    fun generateDocumentation(dsl: DSLDeclaration): DSLDocumentation {
        val examples = DSLExampleGenerator().generateExamples(dsl)
        
        return DSLDocumentation(
            name = dsl.name,
            description = getDescription(dsl),
            syntax = generateSyntaxDocumentation(dsl.syntax),
            semantics = generateSemanticsDocumentation(dsl.semantics),
            transformation = generateTransformationDocumentation(dsl.transformation),
            examples = examples
        )
    }
    
    /**
     * 获取 DSL 描述
     */
    private fun getDescription(dsl: DSLDeclaration): String {
        return dsl.annotations.find { it.name == "Description" }
            ?.parameters?.get("value")?.toString()
            ?: "DSL: ${dsl.name}"
    }
    
    /**
     * 生成语法文档
     */
    private fun generateSyntaxDocumentation(syntax: DSLSyntax): String {
        val sb = StringBuilder()
        sb.appendLine("## 语法模式")
        
        for ((patternName, pattern) in syntax.patterns) {
            sb.appendLine("### $patternName")
            sb.appendLine("模式: `${pattern.pattern}`")
            sb.appendLine("字段: ${pattern.fields.joinToString(", ")}")
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * 生成语义文档
     */
    private fun generateSemanticsDocumentation(semantics: DSLSemantics): String {
        val sb = StringBuilder()
        sb.appendLine("## 语义规则")
        
        for ((ruleName, rule) in semantics.rules) {
            sb.appendLine("### $ruleName")
            sb.appendLine("类型: ${rule.type}")
            sb.appendLine("映射: ${rule.mapping}")
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * 生成转换文档
     */
    private fun generateTransformationDocumentation(transformation: DSLTransformation): String {
        val sb = StringBuilder()
        sb.appendLine("## 转换规则")
        sb.appendLine("目标: ${transformation.target}")
        sb.appendLine("模板:")
        sb.appendLine("```")
        sb.appendLine(transformation.template)
        sb.appendLine("```")
        
        return sb.toString()
    }
}

/**
 * DSL 文档数据类
 */
data class DSLDocumentation(
    val name: String,
    val description: String,
    val syntax: String,
    val semantics: String,
    val transformation: String,
    val examples: List<DSLExample>
)
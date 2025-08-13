package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.Type

/**
 * 代码生成器接口
 * 定义了将 AST 转换为目标语言代码的通用接口
 */
interface CodeGenerator {
    /**
     * 生成代码
     */
    fun generate(node: ASTNode): String
    
    /**
     * 获取目标语言名称
     */
    fun getTargetLanguage(): String
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String
}

/**
 * 抽象代码生成器基类
 * 提供通用的代码生成功能
 */
abstract class BaseCodeGenerator : CodeGenerator {
    protected val templateManager = TemplateManager()
    protected var indentLevel = 0
    protected val indentSize = 4
    
    /**
     * 生成缩进
     */
    protected fun indent(): String = " ".repeat(indentLevel * indentSize)
    
    /**
     * 增加缩进级别
     */
    protected fun increaseIndent() {
        indentLevel++
    }
    
    /**
     * 减少缩进级别
     */
    protected fun decreaseIndent() {
        if (indentLevel > 0) {
            indentLevel--
        }
    }
    
    /**
     * 在指定缩进级别下执行代码块
     */
    protected inline fun withIndent(block: () -> String): String {
        increaseIndent()
        val result = block()
        decreaseIndent()
        return result
    }
    
    /**
     * 生成表达式代码
     */
    protected abstract fun generateExpression(expr: Expression): String
    
    /**
     * 生成语句代码
     */
    protected abstract fun generateStatement(stmt: Statement): String
    
    /**
     * 生成声明代码
     */
    protected abstract fun generateDeclaration(decl: Declaration): String
    
    /**
     * 生成类型代码
     */
    protected abstract fun generateType(type: Type): String
    
    override fun generate(node: ASTNode): String {
        return when (node) {
            is Program -> generateProgram(node)
            is Expression -> generateExpression(node)
            is Statement -> generateStatement(node)
            is Declaration -> generateDeclaration(node)
            else -> throw UnsupportedOperationException("Unsupported node type: ${node::class.simpleName}")
        }
    }
    
    /**
     * 生成程序代码
     */
    protected open fun generateProgram(program: Program): String {
        val parts = mutableListOf<String>()
        
        // 生成导入
        if (program.imports.isNotEmpty()) {
            parts.add(program.imports.joinToString("\n") { generateDeclaration(it) })
            parts.add("")
        }
        
        // 生成类型声明
        if (program.types.isNotEmpty()) {
            parts.add(program.types.joinToString("\n\n") { generateDeclaration(it) })
            parts.add("")
        }
        
        // 生成常量声明
        if (program.constants.isNotEmpty()) {
            parts.add(program.constants.joinToString("\n") { generateDeclaration(it) })
            parts.add("")
        }
        
        // 生成函数声明
        if (program.functions.isNotEmpty()) {
            parts.add(program.functions.joinToString("\n\n") { generateDeclaration(it) })
        }
        
        return parts.joinToString("\n")
    }
}

/**
 * 通用代码生成器
 * 生成类似 JSONLang 语法的代码
 */
class GenericCodeGenerator : BaseCodeGenerator() {
    
    override fun getTargetLanguage(): String = "JSONLang"
    override fun getFileExtension(): String = "lang.json"
    
    override fun generateExpression(expr: Expression): String {
        return when (expr) {
            is LiteralExpression.StringLiteral -> "\"${expr.value}\""
            is LiteralExpression.IntLiteral -> expr.value.toString()
            is LiteralExpression.FloatLiteral -> expr.value.toString()
            is LiteralExpression.BooleanLiteral -> expr.value.toString()
            is LiteralExpression.NullLiteral -> "null"
            is IdentifierExpression -> expr.name
            is BinaryExpression -> "${generateExpression(expr.left)} ${expr.operator.symbol} ${generateExpression(expr.right)}"
            is UnaryExpression -> "${expr.operator.symbol}${generateExpression(expr.operand)}"
            is CodeLiteralExpression -> expr.code
            is CallExpression -> {
                val function = generateExpression(expr.function)
                val args = expr.arguments.joinToString(", ") { arg ->
                    if (arg.name != null) {
                        "${arg.name}: ${generateExpression(arg.value)}"
                    } else {
                        generateExpression(arg.value)
                    }
                }
                "$function($args)"
            }
            is MemberExpression -> {
                val target = generateExpression(expr.target)
                if (expr.computed) {
                    "$target[${expr.property}]"
                } else {
                    "$target.${expr.property}"
                }
            }
            is ArrayExpression -> {
                val elements = expr.elements.joinToString(", ") { generateExpression(it) }
                "[$elements]"
            }
            is ObjectExpression -> {
                val properties = expr.properties.joinToString(", ") { prop ->
                    val key = if (prop.computed) {
                        "[${generateExpression(prop.key)}]"
                    } else {
                        generateExpression(prop.key)
                    }
                    "$key: ${generateExpression(prop.value)}"
                }
                "{$properties}"
            }
            is LambdaExpression -> {
                val params = expr.parameters.joinToString(", ") { "${it.name}: ${generateType(it.type)}" }
                val body = generateExpression(expr.body)
                "($params) => $body"
            }
            is ConditionalExpression -> {
                "${generateExpression(expr.condition)} ? ${generateExpression(expr.thenExpression)} : ${generateExpression(expr.elseExpression)}"
            }
            is CastExpression -> {
                val operator = if (expr.safe) "as?" else "as"
                "${generateExpression(expr.expression)} $operator ${generateType(expr.targetType)}"
            }
            is TypeCheckExpression -> {
                "${generateExpression(expr.expression)} is ${generateType(expr.type)}"
            }
            is MacroCall -> {
                val args = expr.arguments.joinToString(", ") { generate(it) }
                "${expr.name}!($args)"
            }
            is MacroParameterPlaceholder -> "\${${expr.parameterName}}"
        }
    }
    
    override fun generateStatement(stmt: Statement): String {
        // 这里需要根据实际的 Statement 类型来实现
        // 由于 Statement 类还没有完全定义，这里先返回空字符串
        return ""
    }
    
    override fun generateDeclaration(decl: Declaration): String {
        return when (decl) {
            is ImportDeclaration -> {
                val items = if (decl.items.isEmpty()) "*" else decl.items.joinToString(", ")
                "import $items from \"${decl.source}\""
            }
            is ExportDeclaration -> {
                "export ${decl.exportType.name.lowercase()} ${decl.name}"
            }
            is TypeDeclaration -> {
                "type ${decl.name} = ${generateTypeDefinition(decl.definition)}"
            }
            is ConstantDeclaration -> {
                "const ${decl.name}: ${generateType(decl.type)} = ${generateExpression(decl.value)}"
            }
            is FunctionDeclaration -> {
                val modifiers = generateFunctionModifiers(decl.modifiers)
                val params = decl.parameters.joinToString(", ") { param ->
                    val optional = if (param.optional) "?" else ""
                    val default = if (param.defaultValue != null) " = ${generateExpression(param.defaultValue)}" else ""
                    "${param.name}$optional: ${generateType(param.type)}$default"
                }
                val returnType = generateType(decl.returnType)
                val body = decl.body.joinToString("\n${indent()}") { generateStatement(it) }
                
                "$modifiers function ${decl.name}($params): $returnType {\n${withIndent { body }}\n}"
            }
            is MacroDeclaration -> {
                val params = decl.parameters.joinToString(", ")
                val expansion = when (decl.expansion) {
                    is MacroExpansion.TemplateExpansion -> generate(decl.expansion.template)
                    is MacroExpansion.FunctionExpansion -> decl.expansion.function
                }
                "macro ${decl.name}($params) = $expansion"
            }
            is DSLDeclaration -> {
                "dsl ${decl.name} { /* DSL definition */ }"
            }
        }
    }
    
    override fun generateType(type: Type): String {
        // 这里需要根据实际的 Type 类型来实现
        // 由于 Type 类还没有完全定义，这里先返回简单的字符串表示
        return type.toString()
    }
    
    private fun generateTypeDefinition(definition: TypeDefinition): String {
        return when (definition) {
            is TypeDefinition.StructDefinition -> {
                val fields = definition.fields.entries.joinToString(", ") { (name, field) ->
                    val optional = if (field.optional) "?" else ""
                    "$name$optional: ${generateType(field.type)}"
                }
                "{ $fields }"
            }
            is TypeDefinition.EnumDefinition -> {
                val variants = definition.variants.entries.joinToString(" | ") { (name, type) ->
                    if (type != null) "$name(${generateType(type)})" else name
                }
                variants
            }
            is TypeDefinition.UnionDefinition -> {
                definition.types.joinToString(" | ") { generateType(it) }
            }
            is TypeDefinition.AliasDefinition -> {
                generateType(definition.aliasedType)
            }
            is TypeDefinition.GenericDefinition -> {
                val params = definition.parameters.joinToString(", ") { it.name }
                "${generateType(definition.baseType)}<$params>"
            }
        }
    }
    
    private fun generateFunctionModifiers(modifiers: FunctionModifiers): String {
        val parts = mutableListOf<String>()
        if (modifiers.async) parts.add("async")
        if (modifiers.inline) parts.add("inline")
        if (modifiers.pure) parts.add("pure")
        if (modifiers.external) parts.add("external")
        return if (parts.isNotEmpty()) parts.joinToString(" ") + " " else ""
    }
}

/**
 * 代码生成器工厂
 */
object CodeGeneratorFactory {
    private val generators = mutableMapOf<String, () -> CodeGenerator>()
    
    init {
        // 注册内置生成器
        register("generic") { GenericCodeGenerator() }
        register("jsonlang") { GenericCodeGenerator() }
        // Kotlin backend is registered separately in BackendRegistry
    }
    
    /**
     * 注册代码生成器
     */
    fun register(name: String, factory: () -> CodeGenerator) {
        generators[name] = factory
    }
    
    /**
     * 创建代码生成器
     */
    fun create(name: String): CodeGenerator? {
        return generators[name]?.invoke()
    }
    
    /**
     * 获取所有可用的生成器名称
     */
    fun getAvailableGenerators(): List<String> {
        return generators.keys.toList()
    }
}

/**
 * 代码生成上下文
 */
data class CodeGenerationContext(
    val targetLanguage: String,
    val options: Map<String, Any> = emptyMap(),
    val templates: Map<String, CodeTemplate> = emptyMap()
) {
    fun getOption(key: String): Any? = options[key]
    
    fun getTemplate(name: String): CodeTemplate? = templates[name]
    
    fun withOption(key: String, value: Any): CodeGenerationContext {
        return copy(options = options + (key to value))
    }
    
    fun withTemplate(name: String, template: CodeTemplate): CodeGenerationContext {
        return copy(templates = templates + (name to template))
    }
}

/**
 * 代码生成结果
 */
data class CodeGenerationResult(
    val code: String,
    val language: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    fun withMetadata(key: String, value: Any): CodeGenerationResult {
        return copy(metadata = metadata + (key to value))
    }
}
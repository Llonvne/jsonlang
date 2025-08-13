package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.ASTNode
import cn.llonvne.jsonlang.core.ast.CallExpression
import cn.llonvne.jsonlang.core.ast.Expression
import cn.llonvne.jsonlang.core.ast.IdentifierExpression
import cn.llonvne.jsonlang.core.ast.MacroParameterPlaceholder
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 代码模板系统
 * 支持参数化的代码模板和动态代码生成
 */
class CodeTemplate(
    val name: String,
    val template: String,
    val parameters: List<TemplateParameter> = emptyList(),
    val metadata: TemplateMetadata = TemplateMetadata()
) {
    
    /**
     * 使用给定参数渲染模板
     */
    fun render(context: TemplateContext): String {
        var result = template
        
        // 替换参数占位符
        for ((key, value) in context.variables) {
            val placeholder = "\${$key}"
            result = result.replace(placeholder, value.toString())
        }
        
        // 处理条件块
        result = processConditionalBlocks(result, context)
        
        // 处理循环块
        result = processLoopBlocks(result, context)
        
        // 处理嵌套模板
        result = processNestedTemplates(result, context)
        
        return result
    }
    
    /**
     * 处理条件块 #{if condition}...#{endif}
     */
    private fun processConditionalBlocks(template: String, context: TemplateContext): String {
        val ifPattern = Regex("""#\{if\s+(\w+)\}(.*?)#\{endif\}""", RegexOption.DOT_MATCHES_ALL)
        return ifPattern.replace(template) { matchResult ->
            val condition = matchResult.groupValues[1]
            val content = matchResult.groupValues[2]
            
            val conditionValue = context.variables[condition]
            if (conditionValue != null && conditionValue.toString().toBoolean()) {
                content
            } else {
                ""
            }
        }
    }
    
    /**
     * 处理循环块 #{for item in items}...#{endfor}
     */
    private fun processLoopBlocks(template: String, context: TemplateContext): String {
        val forPattern = Regex("""#\{for\s+(\w+)\s+in\s+(\w+)\}(.*?)#\{endfor\}""", RegexOption.DOT_MATCHES_ALL)
        return forPattern.replace(template) { matchResult ->
            val itemVar = matchResult.groupValues[1]
            val listVar = matchResult.groupValues[2]
            val content = matchResult.groupValues[3]
            
            val list = context.variables[listVar] as? List<*> ?: emptyList<Any>()
            list.joinToString("") { item ->
                val itemContext = context.withVariable(itemVar, item ?: "")
                var itemContent = content
                for ((key, value) in itemContext.variables) {
                    itemContent = itemContent.replace("\${$key}", value.toString())
                }
                itemContent
            }
        }
    }
    
    /**
     * 处理嵌套模板 #{template templateName}
     */
    private fun processNestedTemplates(template: String, context: TemplateContext): String {
        val templatePattern = Regex("""#\{template\s+(\w+)\}""")
        return templatePattern.replace(template) { matchResult ->
            val templateName = matchResult.groupValues[1]
            val nestedTemplate = context.getTemplate(templateName)
            nestedTemplate?.render(context) ?: ""
        }
    }
    
    /**
     * 验证模板参数
     */
    fun validate(context: TemplateContext): List<TemplateValidationError> {
        val errors = mutableListOf<TemplateValidationError>()
        
        // 检查必需参数
        for (param in parameters) {
            if (param.required && !context.variables.containsKey(param.name)) {
                errors.add(TemplateValidationError(
                    "Missing required parameter: ${param.name}",
                    param.name
                ))
            }
        }
        
        // 检查参数类型
        for (param in parameters) {
            val value = context.variables[param.name]
            if (value != null && !isTypeCompatible(value, param.type)) {
                errors.add(TemplateValidationError(
                    "Parameter ${param.name} has wrong type. Expected: ${param.type.simpleName}, Actual: ${value::class.simpleName}",
                    param.name
                ))
            }
        }
        
        return errors
    }
    
    /**
     * 检查值是否与期望类型兼容
     */
    private fun isTypeCompatible(value: Any, expectedType: Class<*>): Boolean {
        return when {
            expectedType.isInstance(value) -> true
            // Handle Kotlin/Java type compatibility
            expectedType == Int::class.java && value is Int -> true
            expectedType == String::class.java && value is String -> true
            expectedType == Boolean::class.java && value is Boolean -> true
            expectedType == Double::class.java && value is Double -> true
            expectedType == Float::class.java && value is Float -> true
            expectedType == Long::class.java && value is Long -> true
            // Handle primitive type boxing
            expectedType == java.lang.Integer::class.java && value is Int -> true
            expectedType == java.lang.String::class.java && value is String -> true
            expectedType == java.lang.Boolean::class.java && value is Boolean -> true
            expectedType == java.lang.Double::class.java && value is Double -> true
            expectedType == java.lang.Float::class.java && value is Float -> true
            expectedType == java.lang.Long::class.java && value is Long -> true
            else -> false
        }
    }
}

/**
 * 模板参数定义
 */
data class TemplateParameter(
    val name: String,
    val type: Class<*>,
    val required: Boolean = true,
    val defaultValue: Any? = null,
    val description: String = ""
)

/**
 * 模板元数据
 */
data class TemplateMetadata(
    val description: String = "",
    val author: String = "",
    val version: String = "1.0.0",
    val tags: List<String> = emptyList()
)

/**
 * 模板上下文
 */
data class TemplateContext(
    val variables: Map<String, Any> = emptyMap(),
    val templates: Map<String, CodeTemplate> = emptyMap(),
    val functions: Map<String, TemplateFunction> = emptyMap()
) {
    fun withVariable(name: String, value: Any): TemplateContext {
        return copy(variables = variables + (name to value))
    }
    
    fun withTemplate(name: String, template: CodeTemplate): TemplateContext {
        return copy(templates = templates + (name to template))
    }
    
    fun withFunction(name: String, function: TemplateFunction): TemplateContext {
        return copy(functions = functions + (name to function))
    }
    
    fun getTemplate(name: String): CodeTemplate? = templates[name]
    
    fun getFunction(name: String): TemplateFunction? = functions[name]
}

/**
 * 模板函数类型
 */
typealias TemplateFunction = (List<Any?>) -> Any?

/**
 * 模板验证错误
 */
data class TemplateValidationError(
    val message: String,
    val parameter: String? = null
)

/**
 * AST 模板
 * 支持基于 AST 的代码模板
 */
class ASTTemplate(
    val name: String,
    val template: ASTNode,
    val parameters: List<String> = emptyList()
) {
    
    /**
     * 使用给定参数展开 AST 模板
     */
    fun expand(bindings: Map<String, ASTNode>): ASTNode {
        return expandNode(template, bindings)
    }
    
    private fun expandNode(node: ASTNode, bindings: Map<String, ASTNode>): ASTNode {
        return when (node) {
            is MacroParameterPlaceholder -> {
                bindings[node.parameterName] ?: node
            }
            is IdentifierExpression -> {
                // 如果标识符匹配参数名，则替换
                bindings[node.name] ?: node
            }
            else -> {
                // 递归处理子节点
                expandNodeRecursively(node, bindings)
            }
        }
    }
    
    private fun expandNodeRecursively(node: ASTNode, bindings: Map<String, ASTNode>): ASTNode {
        // 这里需要根据具体的 AST 节点类型来递归处理
        // 由于 AST 节点类型很多，这里只展示基本框架
        return when (node) {
            is Expression -> expandExpression(node, bindings)
            else -> node
        }
    }
    
    private fun expandExpression(expr: Expression, bindings: Map<String, ASTNode>): Expression {
        return when (expr) {
            is MacroParameterPlaceholder -> {
                val replacement = bindings[expr.parameterName]
                if (replacement is Expression) replacement else expr
            }
            is IdentifierExpression -> {
                val replacement = bindings[expr.name]
                if (replacement is Expression) replacement else expr
            }
            is CallExpression -> {
                val expandedFunction = expandNode(expr.function, bindings) as Expression
                val expandedArguments = expr.arguments.map { arg ->
                    arg.copy(value = expandNode(arg.value, bindings) as Expression)
                }
                expr.copy(function = expandedFunction, arguments = expandedArguments)
            }
            // 可以继续添加其他表达式类型的处理
            else -> expr
        }
    }
}

/**
 * 模板管理器
 */
class TemplateManager {
    private val templates = mutableMapOf<String, CodeTemplate>()
    private val astTemplates = mutableMapOf<String, ASTTemplate>()
    
    /**
     * 注册代码模板
     */
    fun registerTemplate(template: CodeTemplate) {
        templates[template.name] = template
    }
    
    /**
     * 注册 AST 模板
     */
    fun registerASTTemplate(template: ASTTemplate) {
        astTemplates[template.name] = template
    }
    
    /**
     * 获取代码模板
     */
    fun getTemplate(name: String): CodeTemplate? = templates[name]
    
    /**
     * 获取 AST 模板
     */
    fun getASTTemplate(name: String): ASTTemplate? = astTemplates[name]
    
    /**
     * 列出所有模板
     */
    fun listTemplates(): List<String> = templates.keys.toList()
    
    /**
     * 列出所有 AST 模板
     */
    fun listASTTemplates(): List<String> = astTemplates.keys.toList()
    
    /**
     * 从字符串创建模板
     */
    fun createTemplate(
        name: String,
        templateString: String,
        parameters: List<TemplateParameter> = emptyList()
    ): CodeTemplate {
        val template = CodeTemplate(name, templateString, parameters)
        registerTemplate(template)
        return template
    }
    
    /**
     * 从 AST 创建模板
     */
    fun createASTTemplate(
        name: String,
        templateAST: ASTNode,
        parameters: List<String> = emptyList()
    ): ASTTemplate {
        val template = ASTTemplate(name, templateAST, parameters)
        registerASTTemplate(template)
        return template
    }
}

/**
 * 内置模板函数
 */
object BuiltinTemplateFunctions {
    
    val functions: Map<String, TemplateFunction> = mapOf(
        "uppercase" to { args ->
            args.firstOrNull()?.toString()?.uppercase() ?: ""
        },
        "lowercase" to { args ->
            args.firstOrNull()?.toString()?.lowercase() ?: ""
        },
        "capitalize" to { args ->
            args.firstOrNull()?.toString()?.replaceFirstChar { it.uppercase() } ?: ""
        },
        "join" to { args ->
            val list = args.getOrNull(0) as? List<*> ?: emptyList<Any>()
            val separator = args.getOrNull(1)?.toString() ?: ", "
            list.joinToString(separator)
        },
        "length" to { args ->
            when (val value = args.firstOrNull()) {
                is String -> value.length
                is List<*> -> value.size
                is Array<*> -> value.size
                else -> 0
            }
        },
        "default" to { args ->
            val value = args.getOrNull(0)
            val defaultValue = args.getOrNull(1)
            if (value == null || value.toString().isEmpty()) defaultValue ?: "" else value
        }
    )
    
    /**
     * 创建包含内置函数的模板上下文
     */
    fun createContext(): TemplateContext {
        return TemplateContext(functions = functions)
    }
}
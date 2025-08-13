package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlinx.serialization.Serializable

/**
 * DSL 转换规则类
 * 负责将 DSL 节点转换为核心语言 AST 节点
 */
class DSLTransformationRule(
    val dslName: String,
    val transformation: DSLTransformation
) {
    private val templateEngine = DSLTemplateEngine()
    
    /**
     * 转换 DSL 节点为 AST 节点
     */
    fun transform(dslNode: DSLNode): ASTNode {
        return when (transformation.target.lowercase()) {
            "kotlin" -> transformToKotlin(dslNode)
            "go" -> transformToGo(dslNode)
            "javascript" -> transformToJavaScript(dslNode)
            "ast" -> transformToAST(dslNode)
            else -> transformWithTemplate(dslNode)
        }
    }
    
    /**
     * 使用模板转换
     */
    private fun transformWithTemplate(dslNode: DSLNode): ASTNode {
        val expandedTemplate = templateEngine.expand(transformation.template, dslNode.fields)
        return parseTemplateToAST(expandedTemplate, dslNode.location)
    }
    
    /**
     * 转换为 Kotlin 代码
     */
    private fun transformToKotlin(dslNode: DSLNode): ASTNode {
        val kotlinCode = templateEngine.expand(transformation.template, dslNode.fields)
        return cn.llonvne.jsonlang.core.ast.CodeLiteralExpression(kotlinCode, "kotlin", dslNode.location, emptyList())
    }
    
    /**
     * 转换为 Go 代码
     */
    private fun transformToGo(dslNode: DSLNode): ASTNode {
        val goCode = templateEngine.expand(transformation.template, dslNode.fields)
        return cn.llonvne.jsonlang.core.ast.CodeLiteralExpression(goCode, "go", dslNode.location, emptyList())
    }
    
    /**
     * 转换为 JavaScript 代码
     */
    private fun transformToJavaScript(dslNode: DSLNode): ASTNode {
        val jsCode = templateEngine.expand(transformation.template, dslNode.fields)
        return cn.llonvne.jsonlang.core.ast.CodeLiteralExpression(jsCode, "javascript", dslNode.location, emptyList())
    }
    
    /**
     * 转换为 AST 节点
     */
    private fun transformToAST(dslNode: DSLNode): ASTNode {
        // 解析模板为 AST 结构
        return parseTemplateToAST(transformation.template, dslNode.location)
    }
    
    /**
     * 解析模板为 AST 节点
     */
    private fun parseTemplateToAST(template: String, location: SourceLocation): ASTNode {
        // 简化的 AST 解析逻辑
        return when {
            template.contains("function_call") -> {
                // 解析函数调用
                val functionName = extractValue(template, "function")
                val arguments = extractList(template, "arguments")
                
                CallExpression(
                    function = IdentifierExpression(functionName, location, emptyList()),
                    arguments = arguments.map { Argument(value = LiteralExpression.StringLiteral(it, location, emptyList())) },
                    location = location,
                    annotations = emptyList()
                )
            }
            template.contains("object") -> {
                // 解析对象表达式
                val properties = extractMap(template, "properties")
                
                ObjectExpression(
                    properties = properties.map { (key, value) ->
                        ObjectProperty(LiteralExpression.StringLiteral(key, location, emptyList()), LiteralExpression.StringLiteral(value, location, emptyList()))
                    },
                    location = location,
                    annotations = emptyList()
                )
            }
            template.contains("conditional") -> {
                // 解析条件表达式
                val condition = extractValue(template, "condition")
                val thenExpr = extractValue(template, "then")
                val elseExpr = extractValue(template, "else")
                
                ConditionalExpression(
                    condition = LiteralExpression.StringLiteral(condition, location, emptyList()),
                    thenExpression = LiteralExpression.StringLiteral(thenExpr, location, emptyList()),
                    elseExpression = if (elseExpr.isNotEmpty()) LiteralExpression.StringLiteral(elseExpr, location, emptyList()) else LiteralExpression.NullLiteral(location),
                    location = location,
                    annotations = emptyList()
                )
            }
            else -> {
                // 默认为字面量表达式
                LiteralExpression.StringLiteral(template, location, emptyList())
            }
        }
    }
    
    /**
     * 从模板中提取值
     */
    private fun extractValue(template: String, key: String): String {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(template)?.groupValues?.get(1) ?: ""
    }
    
    /**
     * 从模板中提取列表
     */
    private fun extractList(template: String, key: String): List<String> {
        val regex = Regex("\"$key\"\\s*:\\s*\\[([^\\]]+)\\]")
        val match = regex.find(template) ?: return emptyList()
        val listContent = match.groupValues[1]
        return listContent.split(",").map { it.trim().removeSurrounding("\"") }
    }
    
    /**
     * 从模板中提取映射
     */
    private fun extractMap(template: String, key: String): Map<String, String> {
        val regex = Regex("\"$key\"\\s*:\\s*\\{([^}]+)\\}")
        val match = regex.find(template) ?: return emptyMap()
        val mapContent = match.groupValues[1]
        
        val result = mutableMapOf<String, String>()
        val pairRegex = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"")
        pairRegex.findAll(mapContent).forEach { pairMatch ->
            result[pairMatch.groupValues[1]] = pairMatch.groupValues[2]
        }
        
        return result
    }
}

/**
 * DSL 模板引擎
 */
class DSLTemplateEngine {
    /**
     * 展开模板
     */
    fun expand(template: String, fields: Map<String, String>): String {
        var expanded = template
        
        // 替换字段占位符
        for ((fieldName, fieldValue) in fields) {
            val placeholder = "\${$fieldName}"
            expanded = expanded.replace(placeholder, fieldValue)
        }
        
        // 处理条件模板
        expanded = processConditionalTemplates(expanded, fields)
        
        // 处理循环模板
        expanded = processLoopTemplates(expanded, fields)
        
        return expanded
    }
    
    /**
     * 处理条件模板
     */
    private fun processConditionalTemplates(template: String, fields: Map<String, String>): String {
        val conditionalRegex = Regex("\\{%\\s*if\\s+([^%]+)\\s*%\\}(.*?)\\{%\\s*endif\\s*%\\}")
        var processed = template
        
        conditionalRegex.findAll(template).forEach { match ->
            val condition = match.groupValues[1].trim()
            val content = match.groupValues[2]
            val fullMatch = match.value
            
            val shouldInclude = evaluateCondition(condition, fields)
            processed = processed.replace(fullMatch, if (shouldInclude) content else "")
        }
        
        return processed
    }
    
    /**
     * 处理循环模板
     */
    private fun processLoopTemplates(template: String, fields: Map<String, String>): String {
        val loopRegex = Regex("\\{%\\s*for\\s+([^%]+)\\s*%\\}([^{]*)\\{%\\s*endfor\\s*%\\}")
        var processed = template
        
        loopRegex.findAll(template).forEach { match ->
            val loopVar = match.groupValues[1].trim()
            val content = match.groupValues[2]
            val fullMatch = match.value
            
            val loopResult = processLoop(loopVar, content, fields)
            processed = processed.replace(fullMatch, loopResult)
        }
        
        return processed
    }
    
    /**
     * 评估条件
     */
    private fun evaluateCondition(condition: String, fields: Map<String, String>): Boolean {
        // 简化的条件评估
        return when {
            condition.contains("!=") -> {
                val parts = condition.split("!=").map { it.trim() }
                val left = resolveValue(parts[0], fields)
                val right = resolveValue(parts[1], fields)
                left != right
            }
            condition.contains("==") -> {
                val parts = condition.split("==").map { it.trim() }
                val left = resolveValue(parts[0], fields)
                val right = resolveValue(parts[1], fields)
                left == right
            }
            condition.contains("exists") -> {
                val fieldName = condition.replace("exists", "").trim()
                fields.containsKey(fieldName) && fields[fieldName]?.isNotEmpty() == true
            }
            else -> {
                // 默认检查字段是否存在且非空
                fields.containsKey(condition) && fields[condition]?.isNotEmpty() == true
            }
        }
    }
    
    /**
     * 处理循环
     */
    private fun processLoop(loopVar: String, content: String, fields: Map<String, String>): String {
        // 简化的循环处理
        val parts = loopVar.split(" in ").map { it.trim() }
        if (parts.size != 2) return content
        
        val itemVar = parts[0]
        val listVar = parts[1]
        
        val listValue = fields[listVar] ?: return ""
        val items = listValue.split(",").map { it.trim() }
        
        return items.joinToString("") { item ->
            content.replace("\${$itemVar}", item)
        }
    }
    
    /**
     * 解析值
     */
    private fun resolveValue(value: String, fields: Map<String, String>): String {
        return when {
            value.startsWith("\"") && value.endsWith("\"") -> value.substring(1, value.length - 1)
            fields.containsKey(value) -> fields[value] ?: ""
            else -> value
        }
    }
}



/**
 * DSL 转换构建器
 */
class DSLTransformationBuilder(private val dslName: String) {
    private var target: String = "ast"
    private var template: String = ""
    
    /**
     * 设置目标后端
     */
    fun target(targetBackend: String): DSLTransformationBuilder {
        target = targetBackend
        return this
    }
    
    /**
     * 设置转换模板
     */
    fun template(templateStr: String): DSLTransformationBuilder {
        template = templateStr
        return this
    }
    
    /**
     * 构建转换定义
     */
    fun build(): DSLTransformation {
        return DSLTransformation(target, template)
    }
}

/**
 * DSL 转换验证器
 */
class DSLTransformationValidator {
    /**
     * 验证转换定义
     */
    fun validate(transformation: DSLTransformation): List<DSLTransformationError> {
        val errors = mutableListOf<DSLTransformationError>()
        
        // 检查目标后端是否支持
        val supportedTargets = setOf("kotlin", "go", "javascript", "ast")
        if (transformation.target.lowercase() !in supportedTargets) {
            errors.add(DSLTransformationError("不支持的目标后端: ${transformation.target}"))
        }
        
        // 检查模板是否为空
        if (transformation.template.isBlank()) {
            errors.add(DSLTransformationError("转换模板不能为空"))
        }
        
        // 验证模板语法
        try {
            val engine = DSLTemplateEngine()
            engine.expand(transformation.template, emptyMap())
        } catch (e: Exception) {
            errors.add(DSLTransformationError("模板语法错误: ${e.message}"))
        }
        
        return errors
    }
}

/**
 * DSL 转换错误
 */
data class DSLTransformationError(val message: String)
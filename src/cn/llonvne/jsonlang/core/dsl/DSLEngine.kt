package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.lexer.Token
/**
 * DSL 引擎核心类
 * 负责 DSL 的注册、解析、转换和执行
 */
class DSLEngine(
    private val errorReporter: ErrorReporter
) {
    private val registeredDSLs = mutableMapOf<String, DSLDefinition>()
    private val syntaxExtensions = mutableMapOf<String, DSLSyntaxExtension>()
    private val transformationRules = mutableMapOf<String, DSLTransformationRule>()
    
    /**
     * 注册 DSL 定义
     */
    fun registerDSL(declaration: DSLDeclaration) {
        val definition = DSLDefinition(
            name = declaration.name,
            syntax = declaration.syntax,
            semantics = declaration.semantics,
            transformation = declaration.transformation,
            location = declaration.location
        )
        
        registeredDSLs[declaration.name] = definition
        
        // 注册语法扩展
        registerSyntaxExtensions(declaration.name, declaration.syntax)
        
        // 注册转换规则
        registerTransformationRules(declaration.name, declaration.transformation)
    }
    
    /**
     * 解析 DSL 语法
     */
    fun parseDSLSyntax(dslName: String, input: String, location: SourceLocation): DSLNode? {
        val dsl = registeredDSLs[dslName] ?: run {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "DSL_NOT_FOUND",
                    message = "未找到 DSL 定义: $dslName",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.DSL,
                    location = location
                )
            )
            return null
        }
        
        return try {
            val syntaxExtension = syntaxExtensions[dslName]
            if (syntaxExtension != null) {
                syntaxExtension.parse(input, location)
            } else {
                parseWithPatterns(dsl, input, location)
            }
        } catch (e: DSLParseException) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "DSL_PARSE_ERROR",
                    message = "DSL 解析错误: ${e.message}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.DSL,
                    location = location
                )
            )
            null
        }
    }
    
    /**
     * 转换 DSL 节点到核心语言 AST
     */
    fun transformDSLToAST(dslNode: DSLNode): ASTNode? {
        val dsl = registeredDSLs[dslNode.dslName] ?: return null
        
        return try {
            // 优先使用语义规则进行结构化转换
            val semanticRule = dsl.semantics.rules[dslNode.patternName]
            if (semanticRule != null && semanticRule.type in listOf("function_call", "object", "conditional", "loop")) {
                transformWithSemantics(dsl, dslNode)
            } else {
                val transformationRule = transformationRules[dslNode.dslName]
                if (transformationRule != null) {
                    transformationRule.transform(dslNode)
                } else {
                    transformWithSemantics(dsl, dslNode)
                }
            }
        } catch (e: DSLTransformationException) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "DSL_TRANSFORMATION_ERROR",
                    message = "DSL 转换错误: ${e.message}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.DSL,
                    location = dslNode.location
                )
            )
            null
        }
    }
    
    /**
     * 检查 DSL 是否已注册
     */
    fun isDSLRegistered(name: String): Boolean = registeredDSLs.containsKey(name)
    
    /**
     * 获取所有已注册的 DSL
     */
    fun getRegisteredDSLs(): Map<String, DSLDefinition> = registeredDSLs.toMap()
    
    /**
     * 注册语法扩展
     */
    private fun registerSyntaxExtensions(dslName: String, syntax: DSLSyntax) {
        val extension = DSLSyntaxExtension(dslName, syntax)
        syntaxExtensions[dslName] = extension
    }
    
    /**
     * 注册转换规则
     */
    private fun registerTransformationRules(dslName: String, transformation: DSLTransformation) {
        val rule = DSLTransformationRule(dslName, transformation)
        transformationRules[dslName] = rule
    }
    
    /**
     * 使用模式解析 DSL
     */
    private fun parseWithPatterns(dsl: DSLDefinition, input: String, location: SourceLocation): DSLNode {
        for ((patternName, pattern) in dsl.syntax.patterns) {
            val matcher = DSLPatternMatcher(pattern)
            val match = matcher.match(input)
            if (match != null) {
                return DSLNode(
                    dslName = dsl.name,
                    patternName = patternName,
                    fields = match,
                    location = location
                )
            }
        }
        
        throw DSLParseException("无法匹配任何 DSL 模式: $input")
    }
    
    /**
     * 使用语义规则转换 DSL
     */
    private fun transformWithSemantics(dsl: DSLDefinition, dslNode: DSLNode): ASTNode {
        val semanticRule = dsl.semantics.rules[dslNode.patternName]
            ?: throw DSLTransformationException("未找到语义规则: ${dslNode.patternName}")
        
        return when (semanticRule.type) {
            "function_call" -> createFunctionCall(semanticRule, dslNode)
            "object" -> createObjectExpression(semanticRule, dslNode)
            "conditional" -> createConditionalExpression(semanticRule, dslNode)
            "loop" -> createLoopStatement(semanticRule, dslNode)
            else -> throw DSLTransformationException("不支持的语义规则类型: ${semanticRule.type}")
        }
    }
    
    /**
     * 创建函数调用表达式
     */
    private fun createFunctionCall(rule: DSLSemanticRule, dslNode: DSLNode): CallExpression {
        val functionName = rule.mapping["function"] as? String
            ?: throw DSLTransformationException("函数调用缺少函数名")
        
        val arguments = (rule.mapping["arguments"] as? List<*>)?.map { arg ->
            when (arg) {
                is String -> {
                    if (arg.startsWith("\${") && arg.endsWith("}")) {
                        val fieldName = arg.substring(2, arg.length - 1)
                        val fieldValue = dslNode.fields[fieldName]
                            ?: throw DSLTransformationException("未找到字段: $fieldName")
                        LiteralExpression.StringLiteral(fieldValue, dslNode.location, emptyList())
                    } else {
                        LiteralExpression.StringLiteral(arg, dslNode.location, emptyList())
                    }
                }
                else -> LiteralExpression.StringLiteral(arg.toString(), dslNode.location, emptyList())
            }
        } ?: emptyList()
        
        return CallExpression(
            function = IdentifierExpression(functionName, dslNode.location, emptyList()),
            arguments = arguments.map { Argument(value = it) },
            location = dslNode.location,
            annotations = emptyList()
        )
    }
    
    /**
     * 创建对象表达式
     */
    private fun createObjectExpression(rule: DSLSemanticRule, dslNode: DSLNode): ObjectExpression {
        val properties = (rule.mapping["properties"] as? Map<*, *>)?.mapNotNull { (key, value) ->
            val keyStr = key.toString()
            val valueExpr = when (value) {
                is String -> {
                    if (value.startsWith("\${") && value.endsWith("}")) {
                        val fieldName = value.substring(2, value.length - 1)
                        val fieldValue = dslNode.fields[fieldName]
                            ?: return@mapNotNull null
                        LiteralExpression.StringLiteral(fieldValue, dslNode.location, emptyList())
                    } else {
                        LiteralExpression.StringLiteral(value, dslNode.location, emptyList())
                    }
                }
                else -> LiteralExpression.StringLiteral(value.toString(), dslNode.location, emptyList())
            }
            ObjectProperty(LiteralExpression.StringLiteral(keyStr, dslNode.location, emptyList()), valueExpr)
        } ?: emptyList()
        
        return ObjectExpression(properties, dslNode.location, emptyList())
    }
    
    /**
     * 创建条件表达式
     */
    private fun createConditionalExpression(rule: DSLSemanticRule, dslNode: DSLNode): ConditionalExpression {
        val conditionField = rule.mapping["condition"] as? String
            ?: throw DSLTransformationException("条件表达式缺少条件字段")
        
        val thenField = rule.mapping["then"] as? String
            ?: throw DSLTransformationException("条件表达式缺少 then 字段")
        
        val elseField = rule.mapping["else"] as? String
        
        val condition = parseFieldAsExpression(conditionField, dslNode)
        val thenExpr = parseFieldAsExpression(thenField, dslNode)
        val elseExpr = elseField?.let { parseFieldAsExpression(it, dslNode) }
        
        return ConditionalExpression(condition, thenExpr, elseExpr ?: LiteralExpression.NullLiteral(dslNode.location), dslNode.location, emptyList())
    }
    
    /**
     * 创建循环语句
     */
    private fun createLoopStatement(rule: DSLSemanticRule, dslNode: DSLNode): LoopStatement.ForLoop {
        val iteratorField = rule.mapping["iterator"] as? String
            ?: throw DSLTransformationException("循环语句缺少迭代器字段")
        
        val iterableField = rule.mapping["iterable"] as? String
            ?: throw DSLTransformationException("循环语句缺少可迭代对象字段")
        
        val bodyField = rule.mapping["body"] as? String
            ?: throw DSLTransformationException("循环语句缺少循环体字段")
        
        val iterator = dslNode.fields[iteratorField]
            ?: throw DSLTransformationException("未找到迭代器字段: $iteratorField")
        
        val iterable = parseFieldAsExpression(iterableField, dslNode)
        val body = parseFieldAsStatement(bodyField, dslNode)
        
        return LoopStatement.ForLoop(
            iterator = iterator,
            iterable = iterable,
            body = body,
            location = dslNode.location,
            annotations = emptyList()
        )
    }
    
    /**
     * 解析字段为表达式
     */
    private fun parseFieldAsExpression(fieldRef: String, dslNode: DSLNode): Expression {
        if (fieldRef.startsWith("\${") && fieldRef.endsWith("}")) {
            val fieldName = fieldRef.substring(2, fieldRef.length - 1)
            val fieldValue = dslNode.fields[fieldName]
                ?: throw DSLTransformationException("未找到字段: $fieldName")
            return LiteralExpression.StringLiteral(fieldValue, dslNode.location, emptyList())
        } else {
            return LiteralExpression.StringLiteral(fieldRef, dslNode.location, emptyList())
        }
    }
    
    /**
     * 解析字段为语句
     */
    private fun parseFieldAsStatement(fieldRef: String, dslNode: DSLNode): Statement {
        val expr = parseFieldAsExpression(fieldRef, dslNode)
        return ExpressionStatement(expr, dslNode.location, emptyList())
    }
}

/**
 * DSL 定义数据类
 */
data class DSLDefinition(
    val name: String,
    val syntax: DSLSyntax,
    val semantics: DSLSemantics,
    val transformation: DSLTransformation,
    val location: SourceLocation
)

/**
 * DSL 节点
 */
data class DSLNode(
    val dslName: String,
    val patternName: String,
    val fields: Map<String, String>,
    val location: SourceLocation
)

/**
 * DSL 解析异常
 */
class DSLParseException(message: String) : Exception(message)

/**
 * DSL 转换异常
 */
class DSLTransformationException(message: String) : Exception(message)
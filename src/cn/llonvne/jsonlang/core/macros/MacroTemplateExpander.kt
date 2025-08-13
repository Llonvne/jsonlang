package cn.llonvne.jsonlang.core.macros

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 宏模板展开器
 * 负责将宏模板中的参数占位符替换为实际参数
 */
class MacroTemplateExpander(
    private val bindings: Map<String, ASTNode>,
    private val context: MacroExpansionContext
) : BaseASTVisitor<ASTNode>() {
    
    /**
     * 展开模板
     */
    fun expand(template: ASTNode): ASTNode {
        return template.accept(this)
    }
    
    override fun defaultVisit(node: ASTNode): ASTNode = node
    
    /**
     * 处理标识符表达式 - 可能是宏参数
     */
    override fun visitIdentifierExpression(node: IdentifierExpression): ASTNode {
        // 检查是否是宏参数占位符
        if (node.name.startsWith("$")) {
            val paramName = node.name.substring(1)
            return bindings[paramName] ?: node
        }
        
        // 检查是否是普通宏参数
        return bindings[node.name] ?: node
    }
    
    /**
     * 处理字符串字面量 - 可能包含参数插值
     */
    override fun visitStringLiteral(node: LiteralExpression.StringLiteral): ASTNode {
        val expandedValue = expandStringTemplate(node.value)
        return if (expandedValue != node.value) {
            node.copy(value = expandedValue)
        } else {
            node
        }
    }
    
    /**
     * 处理函数声明
     */
    override fun visitFunctionDeclaration(node: FunctionDeclaration): ASTNode {
        val expandedName = expandStringTemplate(node.name)
        val expandedBody = node.body.map { it.accept(this) as Statement }
        val expandedParameters = node.parameters.map { expandParameter(it) }
        
        return node.copy(
            name = expandedName,
            parameters = expandedParameters,
            body = expandedBody
        )
    }
    
    /**
     * 处理二元表达式
     */
    override fun visitBinaryExpression(node: BinaryExpression): ASTNode {
        val expandedLeft = node.left.accept(this) as Expression
        val expandedRight = node.right.accept(this) as Expression
        
        return node.copy(
            left = expandedLeft,
            right = expandedRight
        )
    }
    
    /**
     * 处理一元表达式
     */
    override fun visitUnaryExpression(node: UnaryExpression): ASTNode {
        val expandedOperand = node.operand.accept(this) as Expression
        return node.copy(operand = expandedOperand)
    }
    
    /**
     * 处理函数调用表达式
     */
    override fun visitCallExpression(node: CallExpression): ASTNode {
        val expandedFunction = node.function.accept(this) as Expression
        val expandedArguments = node.arguments.map { arg ->
            arg.copy(value = arg.value.accept(this) as Expression)
        }
        
        return node.copy(
            function = expandedFunction,
            arguments = expandedArguments
        )
    }
    
    /**
     * 处理数组表达式
     */
    override fun visitArrayExpression(node: ArrayExpression): ASTNode {
        val expandedElements = node.elements.map { it.accept(this) as Expression }
        return node.copy(elements = expandedElements)
    }
    
    /**
     * 处理对象表达式
     */
    override fun visitObjectExpression(node: ObjectExpression): ASTNode {
        val expandedProperties = node.properties.map { prop ->
            prop.copy(
                key = prop.key.accept(this) as Expression,
                value = prop.value.accept(this) as Expression
            )
        }
        return node.copy(properties = expandedProperties)
    }
    
    /**
     * 处理条件表达式
     */
    override fun visitConditionalExpression(node: ConditionalExpression): ASTNode {
        val expandedCondition = node.condition.accept(this) as Expression
        val expandedThen = node.thenExpression.accept(this) as Expression
        val expandedElse = node.elseExpression.accept(this) as Expression
        
        return node.copy(
            condition = expandedCondition,
            thenExpression = expandedThen,
            elseExpression = expandedElse
        )
    }
    
    /**
     * 处理 Lambda 表达式
     */
    override fun visitLambdaExpression(node: LambdaExpression): ASTNode {
        val expandedParameters = node.parameters.map { expandParameter(it) }
        val expandedBody = node.body.accept(this) as Expression
        
        return node.copy(
            parameters = expandedParameters,
            body = expandedBody
        )
    }
    
    /**
     * 展开字符串模板
     */
    private fun expandStringTemplate(template: String): String {
        var result = template
        
        // 处理 ${param} 形式的参数插值
        val paramPattern = Regex("""\$\{(\w+)\}""")
        result = paramPattern.replace(result) { matchResult ->
            val paramName = matchResult.groupValues[1]
            val paramValue = bindings[paramName]
            
            when (paramValue) {
                is LiteralExpression.StringLiteral -> paramValue.value
                is LiteralExpression.IntLiteral -> paramValue.value.toString()
                is LiteralExpression.FloatLiteral -> paramValue.value.toString()
                is LiteralExpression.BooleanLiteral -> paramValue.value.toString()
                is IdentifierExpression -> paramValue.name
                else -> matchResult.value // 保持原样
            }
        }
        
        return result
    }
    
    /**
     * 展开参数定义
     */
    private fun expandParameter(param: Parameter): Parameter {
        val expandedName = expandStringTemplate(param.name)
        val expandedDefaultValue = param.defaultValue?.accept(this) as? Expression
        
        return param.copy(
            name = expandedName,
            defaultValue = expandedDefaultValue
        )
    }
}

/**
 * 宏参数替换器
 * 专门用于替换宏参数占位符
 */
class MacroParameterReplacer(
    private val bindings: Map<String, ASTNode>
) {
    
    /**
     * 替换节点中的参数占位符
     */
    fun replace(node: ASTNode): ASTNode {
        return when (node) {
            is IdentifierExpression -> {
                bindings[node.name] ?: node
            }
            is MacroParameterPlaceholder -> {
                bindings[node.parameterName] ?: node
            }
            else -> node
        }
    }
}


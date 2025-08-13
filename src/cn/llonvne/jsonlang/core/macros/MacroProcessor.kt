package cn.llonvne.jsonlang.core.macros

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 宏处理器
 * 负责在编译过程中处理宏展开
 */
class MacroProcessor(
    private val macroSystem: MacroSystem,
    private val errorReporter: ErrorReporter
) {
    private val maxExpansionDepth = 100
    
    /**
     * 处理程序中的所有宏
     */
    fun processProgram(program: Program): Program {
        // 首先注册所有宏定义
        program.macros.forEach { macroSystem.registerMacro(it) }
        
        // 然后展开编译时宏
        val context = MacroExpansionContext(
            currentScope = "global",
            expansionDepth = 0
        )
        
        return expandMacrosInProgram(program, context)
    }
    
    /**
     * 在程序中展开宏
     */
    private fun expandMacrosInProgram(program: Program, context: MacroExpansionContext): Program {
        val expandedFunctions = program.functions.map { function ->
            expandMacrosInFunction(function, context)
        }
        
        val expandedConstants = program.constants.map { constant ->
            expandMacrosInConstant(constant, context)
        }
        
        return program.copy(
            functions = expandedFunctions,
            constants = expandedConstants
        )
    }
    
    /**
     * 在函数中展开宏
     */
    private fun expandMacrosInFunction(function: FunctionDeclaration, context: MacroExpansionContext): FunctionDeclaration {
        val functionContext = context.withScope("function_${function.name}")
        val expandedBody = function.body.map { statement ->
            expandMacrosInStatement(statement, functionContext)
        }
        
        return function.copy(body = expandedBody)
    }
    
    /**
     * 在常量中展开宏
     */
    private fun expandMacrosInConstant(constant: ConstantDeclaration, context: MacroExpansionContext): ConstantDeclaration {
        val expandedValue = expandMacrosInExpression(constant.value, context)
        return constant.copy(value = expandedValue)
    }
    
    /**
     * 在语句中展开宏
     */
    private fun expandMacrosInStatement(statement: Statement, context: MacroExpansionContext): Statement {
        return when (statement) {
            is ExpressionStatement -> {
                val expandedExpression = expandMacrosInExpression(statement.expression, context)
                statement.copy(expression = expandedExpression)
            }
            is VariableDeclaration -> {
                val expandedInitializer = statement.value?.let { 
                    expandMacrosInExpression(it, context) 
                }
                statement.copy(value = expandedInitializer)
            }
            is ConditionalStatement -> {
                val expandedCondition = expandMacrosInExpression(statement.condition, context)
                val expandedThenBranch = expandMacrosInStatement(statement.thenBranch, context)
                val expandedElseBranch = statement.elseBranch?.let { 
                    expandMacrosInStatement(it, context) 
                }
                statement.copy(
                    condition = expandedCondition,
                    thenBranch = expandedThenBranch,
                    elseBranch = expandedElseBranch
                )
            }
            is BlockStatement -> {
                val expandedStatements = statement.statements.map { 
                    expandMacrosInStatement(it, context) 
                }
                statement.copy(statements = expandedStatements)
            }
            is JumpStatement.ReturnStatement -> {
                val expandedValue = statement.value?.let { 
                    expandMacrosInExpression(it, context) 
                }
                statement.copy(value = expandedValue)
            }
            else -> statement
        }
    }
    
    /**
     * 在表达式中展开宏
     */
    private fun expandMacrosInExpression(expression: Expression, context: MacroExpansionContext): Expression {
        return when (expression) {
            is CallExpression -> {
                // 检查是否是宏调用
                if (expression.function is IdentifierExpression) {
                    val functionName = expression.function.name
                    if (macroSystem.hasMacro(functionName)) {
                        // 这是一个宏调用
                        val macroCall = MacroCall(
                            name = functionName,
                            arguments = expression.arguments.map { it.value },
                            location = expression.location
                        )
                        
                        val expanded = macroSystem.expandMacro(macroCall, context)
                        return if (expanded is Expression) {
                            // 递归展开结果中可能包含的其他宏
                            expandMacrosInExpression(expanded, context.withIncrementedDepth())
                        } else {
                            // 如果展开结果不是表达式，报告错误
                            errorReporter.report(
                                cn.llonvne.jsonlang.core.errors.CompilerError(
                                    code = "MACRO_EXPANSION_TYPE_ERROR",
                                    message = "宏展开结果不是表达式: $functionName",
                                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.MACRO,
                                    location = expression.location
                                )
                            )
                            expression
                        }
                    }
                }
                
                // 普通函数调用，展开参数中的宏
                val expandedFunction = expandMacrosInExpression(expression.function, context)
                val expandedArguments = expression.arguments.map { arg ->
                    arg.copy(value = expandMacrosInExpression(arg.value, context))
                }
                expression.copy(
                    function = expandedFunction,
                    arguments = expandedArguments
                )
            }
            is BinaryExpression -> {
                val expandedLeft = expandMacrosInExpression(expression.left, context)
                val expandedRight = expandMacrosInExpression(expression.right, context)
                expression.copy(left = expandedLeft, right = expandedRight)
            }
            is UnaryExpression -> {
                val expandedOperand = expandMacrosInExpression(expression.operand, context)
                expression.copy(operand = expandedOperand)
            }
            is ArrayExpression -> {
                val expandedElements = expression.elements.map { 
                    expandMacrosInExpression(it, context) 
                }
                expression.copy(elements = expandedElements)
            }
            is ObjectExpression -> {
                val expandedProperties = expression.properties.map { prop ->
                    prop.copy(
                        key = expandMacrosInExpression(prop.key, context),
                        value = expandMacrosInExpression(prop.value, context)
                    )
                }
                expression.copy(properties = expandedProperties)
            }
            is ConditionalExpression -> {
                val expandedCondition = expandMacrosInExpression(expression.condition, context)
                val expandedThen = expandMacrosInExpression(expression.thenExpression, context)
                val expandedElse = expandMacrosInExpression(expression.elseExpression, context)
                expression.copy(
                    condition = expandedCondition,
                    thenExpression = expandedThen,
                    elseExpression = expandedElse
                )
            }
            is LambdaExpression -> {
                val expandedBody = expandMacrosInExpression(expression.body, context)
                expression.copy(body = expandedBody)
            }
            is MemberExpression -> {
                val expandedTarget = expandMacrosInExpression(expression.target, context)
                expression.copy(target = expandedTarget)
            }
            else -> expression
        }
    }
    
    /**
     * 检查展开深度
     */
    private fun checkExpansionDepth(context: MacroExpansionContext, location: SourceLocation) {
        if (context.expansionDepth > maxExpansionDepth) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "MACRO_EXPANSION_DEPTH_EXCEEDED",
                    message = "宏展开深度超过限制: ${context.expansionDepth}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.MACRO,
                    location = location
                )
            )
        }
    }
}

/**
 * 运行时宏处理器
 * 处理运行时宏展开
 */
class RuntimeMacroProcessor(
    private val macroSystem: MacroSystem,
    private val errorReporter: ErrorReporter
) {
    
    /**
     * 在运行时展开宏
     */
    fun expandRuntimeMacro(macroCall: MacroCall, context: MacroExpansionContext): ASTNode? {
        return macroSystem.expandMacro(macroCall, context)
    }
    
    /**
     * 评估宏表达式
     */
    fun evaluateMacroExpression(expression: Expression, context: MacroExpansionContext): Any? {
        return when (expression) {
            is LiteralExpression.StringLiteral -> expression.value
            is LiteralExpression.IntLiteral -> expression.value
            is LiteralExpression.FloatLiteral -> expression.value
            is LiteralExpression.BooleanLiteral -> expression.value
            is LiteralExpression.NullLiteral -> null
            is IdentifierExpression -> {
                // 从上下文中获取变量值
                context.variables[expression.name]
            }
            is BinaryExpression -> {
                evaluateBinaryExpression(expression, context)
            }
            is UnaryExpression -> {
                evaluateUnaryExpression(expression, context)
            }
            is CallExpression -> {
                // 检查是否是运行时宏调用
                if (expression.function is IdentifierExpression) {
                    val functionName = expression.function.name
                    if (macroSystem.hasMacro(functionName)) {
                        val macroCall = MacroCall(
                            name = functionName,
                            arguments = expression.arguments.map { it.value },
                            location = expression.location
                        )
                        return expandRuntimeMacro(macroCall, context)
                    }
                }
                null
            }
            else -> null
        }
    }
    
    /**
     * 评估二元表达式
     */
    private fun evaluateBinaryExpression(expression: BinaryExpression, context: MacroExpansionContext): Any? {
        val leftValue = evaluateMacroExpression(expression.left, context)
        val rightValue = evaluateMacroExpression(expression.right, context)
        
        return when (expression.operator) {
            BinaryOperator.PLUS -> {
                when {
                    leftValue is Number && rightValue is Number -> {
                        leftValue.toDouble() + rightValue.toDouble()
                    }
                    leftValue is String || rightValue is String -> {
                        leftValue.toString() + rightValue.toString()
                    }
                    else -> null
                }
            }
            BinaryOperator.MINUS -> {
                if (leftValue is Number && rightValue is Number) {
                    leftValue.toDouble() - rightValue.toDouble()
                } else null
            }
            BinaryOperator.MULTIPLY -> {
                if (leftValue is Number && rightValue is Number) {
                    leftValue.toDouble() * rightValue.toDouble()
                } else null
            }
            BinaryOperator.DIVIDE -> {
                if (leftValue is Number && rightValue is Number && rightValue.toDouble() != 0.0) {
                    leftValue.toDouble() / rightValue.toDouble()
                } else null
            }
            BinaryOperator.EQUAL -> leftValue == rightValue
            BinaryOperator.NOT_EQUAL -> leftValue != rightValue
            BinaryOperator.LOGICAL_AND -> {
                (leftValue as? Boolean ?: false) && (rightValue as? Boolean ?: false)
            }
            BinaryOperator.LOGICAL_OR -> {
                (leftValue as? Boolean ?: false) || (rightValue as? Boolean ?: false)
            }
            else -> null
        }
    }
    
    /**
     * 评估一元表达式
     */
    private fun evaluateUnaryExpression(expression: UnaryExpression, context: MacroExpansionContext): Any? {
        val operandValue = evaluateMacroExpression(expression.operand, context)
        
        return when (expression.operator) {
            UnaryOperator.MINUS -> {
                if (operandValue is Number) {
                    -operandValue.toDouble()
                } else null
            }
            UnaryOperator.PLUS -> {
                if (operandValue is Number) {
                    operandValue.toDouble()
                } else null
            }
            UnaryOperator.LOGICAL_NOT -> {
                !(operandValue as? Boolean ?: false)
            }
            else -> null
        }
    }
}
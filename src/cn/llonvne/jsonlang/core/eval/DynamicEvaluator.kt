package cn.llonvne.jsonlang.core.eval

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.Type
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.CompilerError
import cn.llonvne.jsonlang.core.errors.ErrorSeverity
import cn.llonvne.jsonlang.core.errors.ErrorCategory

/**
 * 动态代码执行器
 * 支持在运行时执行 JSONLang 代码
 */
class DynamicEvaluator(
    private val errorReporter: ErrorReporter
) {
    private val globalScope = EvaluationScope()
    private val callStack = mutableListOf<CallFrame>()
    
    /**
     * 执行 AST 节点
     */
    fun evaluate(node: ASTNode, scope: EvaluationScope = globalScope): EvaluationResult {
        return try {
            when (node) {
                is Expression -> evaluateExpression(node, scope)
                is Statement -> evaluateStatement(node, scope)
                is Declaration -> evaluateDeclaration(node, scope)
                is Program -> evaluateProgram(node, scope)
                else -> EvaluationResult.Error("Unsupported node type: ${node::class.simpleName}")
            }
        } catch (e: EvaluationException) {
            EvaluationResult.Error(e.message ?: "Evaluation error")
        } catch (e: Exception) {
            EvaluationResult.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * 执行表达式
     */
    private fun evaluateExpression(expr: Expression, scope: EvaluationScope): EvaluationResult {
        return when (expr) {
            is LiteralExpression.StringLiteral -> EvaluationResult.Success(expr.value)
            is LiteralExpression.IntLiteral -> EvaluationResult.Success(expr.value)
            is LiteralExpression.FloatLiteral -> EvaluationResult.Success(expr.value)
            is LiteralExpression.BooleanLiteral -> EvaluationResult.Success(expr.value)
            is LiteralExpression.NullLiteral -> EvaluationResult.Success(null)
            is CodeLiteralExpression -> EvaluationResult.Success(expr.code)
            
            is IdentifierExpression -> {
                val value = scope.getValue(expr.name)
                if (value != null) {
                    EvaluationResult.Success(value)
                } else {
                    EvaluationResult.Error("Undefined variable: ${expr.name}")
                }
            }
            
            is BinaryExpression -> evaluateBinaryExpression(expr, scope)
            is UnaryExpression -> evaluateUnaryExpression(expr, scope)
            is CallExpression -> evaluateCallExpression(expr, scope)
            is MemberExpression -> evaluateMemberExpression(expr, scope)
            is ArrayExpression -> evaluateArrayExpression(expr, scope)
            is ObjectExpression -> evaluateObjectExpression(expr, scope)
            is LambdaExpression -> EvaluationResult.Success(LambdaValue(expr, scope))
            is ConditionalExpression -> evaluateConditionalExpression(expr, scope)
            is CastExpression -> evaluateCastExpression(expr, scope)
            is TypeCheckExpression -> evaluateTypeCheckExpression(expr, scope)
            is MacroCall -> evaluateMacroCall(expr, scope)
            is MacroParameterPlaceholder -> EvaluationResult.Error("Macro parameter placeholder should be expanded")
        }
    }
    
    /**
     * 执行二元表达式
     */
    private fun evaluateBinaryExpression(expr: BinaryExpression, scope: EvaluationScope): EvaluationResult {
        val leftResult = evaluateExpression(expr.left, scope)
        if (leftResult is EvaluationResult.Error) return leftResult
        
        val rightResult = evaluateExpression(expr.right, scope)
        if (rightResult is EvaluationResult.Error) return rightResult
        
        val leftValue = (leftResult as EvaluationResult.Success).value
        val rightValue = (rightResult as EvaluationResult.Success).value
        
        return try {
            val result = when (expr.operator) {
                BinaryOperator.PLUS -> when {
                    leftValue is Number && rightValue is Number -> {
                        if (leftValue is Double || rightValue is Double) {
                            leftValue.toDouble() + rightValue.toDouble()
                        } else {
                            leftValue.toLong() + rightValue.toLong()
                        }
                    }
                    leftValue is String || rightValue is String -> {
                        leftValue.toString() + rightValue.toString()
                    }
                    else -> throw EvaluationException("Cannot add ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.MINUS -> when {
                    leftValue is Number && rightValue is Number -> {
                        if (leftValue is Double || rightValue is Double) {
                            leftValue.toDouble() - rightValue.toDouble()
                        } else {
                            leftValue.toLong() - rightValue.toLong()
                        }
                    }
                    else -> throw EvaluationException("Cannot subtract ${rightValue?.javaClass?.simpleName} from ${leftValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.MULTIPLY -> when {
                    leftValue is Number && rightValue is Number -> {
                        if (leftValue is Double || rightValue is Double) {
                            leftValue.toDouble() * rightValue.toDouble()
                        } else {
                            leftValue.toLong() * rightValue.toLong()
                        }
                    }
                    else -> throw EvaluationException("Cannot multiply ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.DIVIDE -> when {
                    leftValue is Number && rightValue is Number -> {
                        val rightNum = rightValue.toDouble()
                        if (rightNum == 0.0) {
                            throw EvaluationException("Division by zero")
                        }
                        leftValue.toDouble() / rightNum
                    }
                    else -> throw EvaluationException("Cannot divide ${leftValue?.javaClass?.simpleName} by ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.MODULO -> when {
                    leftValue is Number && rightValue is Number -> {
                        val rightNum = rightValue.toLong()
                        if (rightNum == 0L) {
                            throw EvaluationException("Modulo by zero")
                        }
                        leftValue.toLong() % rightNum
                    }
                    else -> throw EvaluationException("Cannot compute modulo of ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.EQUAL -> leftValue == rightValue
                BinaryOperator.NOT_EQUAL -> leftValue != rightValue
                
                BinaryOperator.LESS_THAN -> when {
                    leftValue is Number && rightValue is Number -> leftValue.toDouble() < rightValue.toDouble()
                    leftValue is String && rightValue is String -> leftValue < rightValue
                    else -> throw EvaluationException("Cannot compare ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.LESS_EQUAL -> when {
                    leftValue is Number && rightValue is Number -> leftValue.toDouble() <= rightValue.toDouble()
                    leftValue is String && rightValue is String -> leftValue <= rightValue
                    else -> throw EvaluationException("Cannot compare ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.GREATER_THAN -> when {
                    leftValue is Number && rightValue is Number -> leftValue.toDouble() > rightValue.toDouble()
                    leftValue is String && rightValue is String -> leftValue > rightValue
                    else -> throw EvaluationException("Cannot compare ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.GREATER_EQUAL -> when {
                    leftValue is Number && rightValue is Number -> leftValue.toDouble() >= rightValue.toDouble()
                    leftValue is String && rightValue is String -> leftValue >= rightValue
                    else -> throw EvaluationException("Cannot compare ${leftValue?.javaClass?.simpleName} and ${rightValue?.javaClass?.simpleName}")
                }
                
                BinaryOperator.LOGICAL_AND -> {
                    val leftBool = isTruthy(leftValue)
                    if (!leftBool) leftValue else rightValue
                }
                
                BinaryOperator.LOGICAL_OR -> {
                    val leftBool = isTruthy(leftValue)
                    if (leftBool) leftValue else rightValue
                }
                
                else -> throw EvaluationException("Unsupported binary operator: ${expr.operator}")
            }
            
            EvaluationResult.Success(result)
        } catch (e: EvaluationException) {
            EvaluationResult.Error(e.message ?: "Binary operation error")
        }
    }
    
    /**
     * 执行一元表达式
     */
    private fun evaluateUnaryExpression(expr: UnaryExpression, scope: EvaluationScope): EvaluationResult {
        val operandResult = evaluateExpression(expr.operand, scope)
        if (operandResult is EvaluationResult.Error) return operandResult
        
        val operandValue = (operandResult as EvaluationResult.Success).value
        
        return try {
            val result = when (expr.operator) {
                UnaryOperator.PLUS -> when (operandValue) {
                    is Number -> operandValue
                    else -> throw EvaluationException("Cannot apply unary plus to ${operandValue?.javaClass?.simpleName}")
                }
                
                UnaryOperator.MINUS -> when (operandValue) {
                    is Long -> -operandValue
                    is Double -> -operandValue
                    is Int -> -operandValue
                    is Float -> -operandValue
                    else -> throw EvaluationException("Cannot apply unary minus to ${operandValue?.javaClass?.simpleName}")
                }
                
                UnaryOperator.LOGICAL_NOT -> !isTruthy(operandValue)
                
                else -> throw EvaluationException("Unsupported unary operator: ${expr.operator}")
            }
            
            EvaluationResult.Success(result)
        } catch (e: EvaluationException) {
            EvaluationResult.Error(e.message ?: "Unary operation error")
        }
    }
    
    /**
     * 执行函数调用表达式
     */
    private fun evaluateCallExpression(expr: CallExpression, scope: EvaluationScope): EvaluationResult {
        val functionResult = evaluateExpression(expr.function, scope)
        if (functionResult is EvaluationResult.Error) return functionResult
        
        val functionValue = (functionResult as EvaluationResult.Success).value
        
        // 评估参数
        val argumentValues = mutableListOf<Any?>()
        for (arg in expr.arguments) {
            val argResult = evaluateExpression(arg.value, scope)
            if (argResult is EvaluationResult.Error) return argResult
            argumentValues.add((argResult as EvaluationResult.Success).value)
        }
        
        return when (functionValue) {
            is LambdaValue -> callLambda(functionValue, argumentValues, scope)
            is NativeFunctionValue -> callNativeFunction(functionValue, argumentValues)
            else -> EvaluationResult.Error("Value is not callable: ${functionValue?.javaClass?.simpleName}")
        }
    }
    
    /**
     * 执行成员访问表达式
     */
    private fun evaluateMemberExpression(expr: MemberExpression, scope: EvaluationScope): EvaluationResult {
        val targetResult = evaluateExpression(expr.target, scope)
        if (targetResult is EvaluationResult.Error) return targetResult
        
        val targetValue = (targetResult as EvaluationResult.Success).value
        
        return when (targetValue) {
            is Map<*, *> -> {
                val key = if (expr.computed) {
                    // 对于计算属性，需要评估属性表达式
                    // 这里简化处理，直接使用属性名
                    expr.property
                } else {
                    expr.property
                }
                EvaluationResult.Success(targetValue[key])
            }
            is List<*> -> {
                if (expr.computed) {
                    try {
                        val index = expr.property.toInt()
                        if (index >= 0 && index < targetValue.size) {
                            EvaluationResult.Success(targetValue[index])
                        } else {
                            EvaluationResult.Error("Array index out of bounds: $index")
                        }
                    } catch (e: NumberFormatException) {
                        EvaluationResult.Error("Invalid array index: ${expr.property}")
                    }
                } else {
                    // 数组的属性访问（如 length）
                    when (expr.property) {
                        "length" -> EvaluationResult.Success(targetValue.size)
                        else -> EvaluationResult.Error("Unknown array property: ${expr.property}")
                    }
                }
            }
            else -> EvaluationResult.Error("Cannot access property of ${targetValue?.javaClass?.simpleName}")
        }
    }
    
    /**
     * 执行数组表达式
     */
    private fun evaluateArrayExpression(expr: ArrayExpression, scope: EvaluationScope): EvaluationResult {
        val elements = mutableListOf<Any?>()
        
        for (element in expr.elements) {
            val elementResult = evaluateExpression(element, scope)
            if (elementResult is EvaluationResult.Error) return elementResult
            elements.add((elementResult as EvaluationResult.Success).value)
        }
        
        return EvaluationResult.Success(elements)
    }
    
    /**
     * 执行对象表达式
     */
    private fun evaluateObjectExpression(expr: ObjectExpression, scope: EvaluationScope): EvaluationResult {
        val obj = mutableMapOf<String, Any?>()
        
        for (prop in expr.properties) {
            val keyResult = evaluateExpression(prop.key, scope)
            if (keyResult is EvaluationResult.Error) return keyResult
            
            val valueResult = evaluateExpression(prop.value, scope)
            if (valueResult is EvaluationResult.Error) return valueResult
            
            val key = (keyResult as EvaluationResult.Success).value.toString()
            val value = (valueResult as EvaluationResult.Success).value
            
            obj[key] = value
        }
        
        return EvaluationResult.Success(obj)
    }
    
    /**
     * 执行条件表达式
     */
    private fun evaluateConditionalExpression(expr: ConditionalExpression, scope: EvaluationScope): EvaluationResult {
        val conditionResult = evaluateExpression(expr.condition, scope)
        if (conditionResult is EvaluationResult.Error) return conditionResult
        
        val conditionValue = (conditionResult as EvaluationResult.Success).value
        
        return if (isTruthy(conditionValue)) {
            evaluateExpression(expr.thenExpression, scope)
        } else {
            evaluateExpression(expr.elseExpression, scope)
        }
    }
    
    /**
     * 执行类型转换表达式
     */
    private fun evaluateCastExpression(expr: CastExpression, scope: EvaluationScope): EvaluationResult {
        val exprResult = evaluateExpression(expr.expression, scope)
        if (exprResult is EvaluationResult.Error) return exprResult
        
        val value = (exprResult as EvaluationResult.Success).value
        
        // 这里简化处理，实际应该根据目标类型进行转换
        return EvaluationResult.Success(value)
    }
    
    /**
     * 执行类型检查表达式
     */
    private fun evaluateTypeCheckExpression(expr: TypeCheckExpression, scope: EvaluationScope): EvaluationResult {
        val exprResult = evaluateExpression(expr.expression, scope)
        if (exprResult is EvaluationResult.Error) return exprResult
        
        val value = (exprResult as EvaluationResult.Success).value
        
        // 这里简化处理，实际应该根据类型系统进行检查
        val isMatch = when (expr.type.toString()) {
            "String" -> value is String
            "Int" -> value is Long || value is Int
            "Float" -> value is Double || value is Float
            "Boolean" -> value is Boolean
            "Array" -> value is List<*>
            "Object" -> value is Map<*, *>
            else -> false
        }
        
        return EvaluationResult.Success(isMatch)
    }
    
    /**
     * 执行宏调用
     */
    private fun evaluateMacroCall(expr: MacroCall, scope: EvaluationScope): EvaluationResult {
        // 宏调用应该在编译时展开，运行时不应该遇到
        return EvaluationResult.Error("Macro call should be expanded at compile time: ${expr.name}")
    }
    
    /**
     * 执行语句
     */
    private fun evaluateStatement(stmt: Statement, scope: EvaluationScope): EvaluationResult {
        // 这里需要根据实际的 Statement 类型来实现
        // 由于 Statement 类还没有完全定义，这里先返回成功
        return EvaluationResult.Success(Unit)
    }
    
    /**
     * 执行声明
     */
    private fun evaluateDeclaration(decl: Declaration, scope: EvaluationScope): EvaluationResult {
        return when (decl) {
            is ConstantDeclaration -> {
                val valueResult = evaluateExpression(decl.value, scope)
                if (valueResult is EvaluationResult.Error) return valueResult
                
                val value = (valueResult as EvaluationResult.Success).value
                scope.setValue(decl.name, value)
                EvaluationResult.Success(value)
            }
            is FunctionDeclaration -> {
                val functionValue = FunctionDeclarationValue(decl, scope)
                scope.setValue(decl.name, functionValue)
                EvaluationResult.Success(functionValue)
            }
            else -> EvaluationResult.Success(Unit)
        }
    }
    
    /**
     * 执行程序
     */
    private fun evaluateProgram(program: Program, scope: EvaluationScope): EvaluationResult {
        // 执行常量声明
        for (constant in program.constants) {
            val result = evaluateDeclaration(constant, scope)
            if (result is EvaluationResult.Error) return result
        }
        
        // 执行函数声明
        for (function in program.functions) {
            val result = evaluateDeclaration(function, scope)
            if (result is EvaluationResult.Error) return result
        }
        
        return EvaluationResult.Success(Unit)
    }
    
    /**
     * 调用 Lambda 函数
     */
    private fun callLambda(lambda: LambdaValue, arguments: List<Any?>, scope: EvaluationScope): EvaluationResult {
        if (arguments.size != lambda.expression.parameters.size) {
            return EvaluationResult.Error("Argument count mismatch: expected ${lambda.expression.parameters.size}, got ${arguments.size}")
        }
        
        val callScope = EvaluationScope(lambda.capturedScope)
        
        // 绑定参数
        for ((param, arg) in lambda.expression.parameters.zip(arguments)) {
            callScope.setValue(param.name, arg)
        }
        
        return evaluateExpression(lambda.expression.body, callScope)
    }
    
    /**
     * 调用原生函数
     */
    private fun callNativeFunction(function: NativeFunctionValue, arguments: List<Any?>): EvaluationResult {
        return try {
            val result = function.implementation(arguments)
            EvaluationResult.Success(result)
        } catch (e: Exception) {
            EvaluationResult.Error("Native function error: ${e.message}")
        }
    }
    
    /**
     * 判断值的真假性
     */
    private fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            is String -> value.isNotEmpty()
            is List<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> true
        }
    }
}

/**
 * 评估结果
 */
sealed class EvaluationResult {
    data class Success(val value: Any?) : EvaluationResult()
    data class Error(val message: String) : EvaluationResult()
}

/**
 * 评估异常
 */
class EvaluationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 评估作用域
 */
class EvaluationScope(private val parent: EvaluationScope? = null) {
    private val variables = mutableMapOf<String, Any?>()
    
    fun getValue(name: String): Any? {
        return variables[name] ?: parent?.getValue(name)
    }
    
    fun setValue(name: String, value: Any?) {
        variables[name] = value
    }
    
    fun hasValue(name: String): Boolean {
        return variables.containsKey(name) || parent?.hasValue(name) == true
    }
    
    fun createChild(): EvaluationScope {
        return EvaluationScope(this)
    }
}

/**
 * 调用帧
 */
data class CallFrame(
    val functionName: String,
    val arguments: List<Any?>,
    val location: cn.llonvne.jsonlang.core.source.SourceLocation
)

/**
 * 函数值类型
 */
sealed class FunctionValue

/**
 * Lambda 值
 */
data class LambdaValue(
    val expression: LambdaExpression,
    val capturedScope: EvaluationScope
) : FunctionValue()

/**
 * 函数声明值
 */
data class FunctionDeclarationValue(
    val declaration: FunctionDeclaration,
    val capturedScope: EvaluationScope
) : FunctionValue()

/**
 * 原生函数值
 */
data class NativeFunctionValue(
    val name: String,
    val implementation: (List<Any?>) -> Any?
) : FunctionValue()

/**
 * Eval 函数
 * 提供动态代码执行的便捷接口
 */
object Eval {
    private val evaluator = DynamicEvaluator(object : ErrorReporter {
        override fun report(error: CompilerError) {
            // 简单的错误处理
            println("Error: ${error.message}")
        }
        
        override fun getErrors(): List<CompilerError> = emptyList()
        override fun getErrorCount(): Int = 0
        override fun getWarningCount(): Int = 0
        override fun hasErrors(): Boolean = false
        override fun hasWarnings(): Boolean = false
        override fun clear() {}
    })
    
    /**
     * 执行表达式
     */
    fun eval(expression: Expression, scope: EvaluationScope = EvaluationScope()): Any? {
        val result = evaluator.evaluate(expression, scope)
        return when (result) {
            is EvaluationResult.Success -> result.value
            is EvaluationResult.Error -> throw EvaluationException(result.message)
        }
    }
    
    /**
     * 执行程序
     */
    fun evalProgram(program: Program, scope: EvaluationScope = EvaluationScope()): Any? {
        val result = evaluator.evaluate(program, scope)
        return when (result) {
            is EvaluationResult.Success -> result.value
            is EvaluationResult.Error -> throw EvaluationException(result.message)
        }
    }
    
    /**
     * 创建新的作用域
     */
    fun createScope(): EvaluationScope = EvaluationScope()
    
    /**
     * 注册原生函数
     */
    fun registerNativeFunction(name: String, implementation: (List<Any?>) -> Any?, scope: EvaluationScope = EvaluationScope()) {
        scope.setValue(name, NativeFunctionValue(name, implementation))
    }
}
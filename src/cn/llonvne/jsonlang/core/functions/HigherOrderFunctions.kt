package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 高阶函数支持
 * 处理函数作为参数和返回值的情况
 */
class HigherOrderFunctionSupport {
    
    /**
     * 验证高阶函数调用
     */
    fun validateHigherOrderCall(
        call: CallExpression,
        signature: FunctionSignature
    ): HigherOrderValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查函数参数
        for ((index, argument) in call.arguments.withIndex()) {
            if (index < signature.parameters.size) {
                val parameter = signature.parameters[index]
                if (parameter.type is CompositeType.FunctionType) {
                    val validationResult = validateFunctionArgument(argument, parameter.type)
                    if (!validationResult.isValid) {
                        errors.addAll(validationResult.errors)
                    }
                }
            }
        }
        
        return HigherOrderValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * 验证函数参数
     */
    private fun validateFunctionArgument(
        argument: Argument,
        expectedType: CompositeType.FunctionType
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        when (val argValue = argument.value) {
            is LambdaExpression -> {
                val lambdaValidation = validateLambdaExpression(argValue, expectedType)
                if (!lambdaValidation.isValid) {
                    errors.addAll(lambdaValidation.errors)
                }
            }
            is IdentifierExpression -> {
                // 检查标识符是否引用了函数
                val inferredType = argValue.inferredType
                if (inferredType !is CompositeType.FunctionType) {
                    errors.add("参数 '${argValue.name}' 不是函数类型")
                } else if (!expectedType.isAssignableFrom(inferredType)) {
                    errors.add("函数类型不匹配: 期望 ${expectedType.getDisplayName()}，得到 ${inferredType.getDisplayName()}")
                }
            }
            else -> {
                errors.add("无效的函数参数类型")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * 验证 Lambda 表达式
     */
    private fun validateLambdaExpression(
        lambda: LambdaExpression,
        expectedType: CompositeType.FunctionType
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查参数数量
        if (lambda.parameters.size != expectedType.parameterTypes.size) {
            errors.add("Lambda 参数数量不匹配: 期望 ${expectedType.parameterTypes.size}，得到 ${lambda.parameters.size}")
            return ValidationResult(false, errors)
        }
        
        // 检查参数类型
        for ((index, param) in lambda.parameters.withIndex()) {
            val expectedParamType = expectedType.parameterTypes[index]
            if (!expectedParamType.isAssignableFrom(param.type)) {
                errors.add("Lambda 参数 '${param.name}' 类型不匹配: 期望 ${expectedParamType.getDisplayName()}，得到 ${param.type.getDisplayName()}")
            }
        }
        
        // 检查返回类型
        val lambdaReturnType = lambda.body.inferredType ?: lambda.inferredType
        if (lambdaReturnType != null && !expectedType.returnType.isAssignableFrom(lambdaReturnType)) {
            errors.add("Lambda 返回类型不匹配: 期望 ${expectedType.returnType.getDisplayName()}，得到 ${lambdaReturnType.getDisplayName()}")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * 推断高阶函数的返回类型
     */
    fun inferHigherOrderReturnType(
        signature: FunctionSignature,
        arguments: List<Argument>
    ): Type? {
        // 如果返回类型是函数类型，需要根据参数推断具体类型
        return when (val returnType = signature.returnType) {
            is CompositeType.FunctionType -> {
                // 简化实现：直接返回声明的函数类型
                returnType
            }
            is GenericType -> {
                // 根据参数推断泛型类型
                inferGenericReturnType(signature, arguments, returnType)
            }
            else -> returnType
        }
    }
    
    /**
     * 推断泛型返回类型
     */
    private fun inferGenericReturnType(
        signature: FunctionSignature,
        arguments: List<Argument>,
        genericType: GenericType
    ): Type? {
        // 简化实现：查找泛型参数的具体类型
        for ((index, argument) in arguments.withIndex()) {
            if (index < signature.parameters.size) {
                val parameter = signature.parameters[index]
                if (parameter.type is GenericType && parameter.type.name == genericType.name) {
                    return argument.value.inferredType
                }
            }
        }
        return null
    }
}

/**
 * 闭包支持
 * 处理闭包的创建、捕获和作用域管理
 */
class ClosureSupport {
    
    /**
     * 分析闭包捕获
     */
    fun analyzeClosure(lambda: LambdaExpression, context: ClosureContext): ClosureAnalysisResult {
        val capturedVariables = mutableSetOf<String>()
        val errors = mutableListOf<String>()
        
        // 分析 lambda 体中的变量引用
        val variableAnalyzer = VariableAnalyzer(lambda.parameters.map { it.name }.toSet())
        val referencedVariables = variableAnalyzer.analyze(lambda.body)
        
        // 确定需要捕获的变量
        for (variable in referencedVariables) {
            if (context.isVariableInScope(variable)) {
                capturedVariables.add(variable)
            } else {
                errors.add("未定义的变量: $variable")
            }
        }
        
        // 检查捕获的变量是否可变
        val mutableCaptures = capturedVariables.filter { context.isVariableMutable(it) }
        if (mutableCaptures.isNotEmpty()) {
            // 警告：捕获可变变量可能导致意外行为
            errors.add("警告: 捕获了可变变量: ${mutableCaptures.joinToString(", ")}")
        }
        
        return ClosureAnalysisResult(
            isValid = errors.none { !it.startsWith("警告") },
            errors = errors,
            capturedVariables = capturedVariables,
            requiresHeapAllocation = capturedVariables.isNotEmpty()
        )
    }
    
    /**
     * 创建闭包环境
     */
    fun createClosureEnvironment(
        capturedVariables: Set<String>,
        context: ClosureContext
    ): ClosureEnvironment {
        val environment = mutableMapOf<String, ClosureBinding>()
        
        for (variable in capturedVariables) {
            val variableType = context.getVariableType(variable)
            val isMutable = context.isVariableMutable(variable)
            
            environment[variable] = ClosureBinding(
                name = variable,
                type = variableType,
                mutable = isMutable,
                captureMode = if (isMutable) CaptureMode.BY_REFERENCE else CaptureMode.BY_VALUE
            )
        }
        
        return ClosureEnvironment(environment)
    }
    
    /**
     * 验证闭包访问
     */
    fun validateClosureAccess(
        variable: String,
        accessType: AccessType,
        environment: ClosureEnvironment
    ): ValidationResult {
        val binding = environment.getBinding(variable)
            ?: return ValidationResult(false, listOf("变量 '$variable' 未在闭包中捕获"))
        
        if (accessType == AccessType.WRITE && !binding.mutable) {
            return ValidationResult(false, listOf("无法修改不可变的捕获变量 '$variable'"))
        }
        
        return ValidationResult(true, emptyList())
    }
}

/**
 * Lambda 表达式扩展
 * 为 Lambda 表达式添加类型推断和验证功能
 */
class LambdaExpressionAnalyzer {
    
    /**
     * 推断 Lambda 表达式类型
     */
    fun inferLambdaType(lambda: LambdaExpression, context: TypeInferenceContext): CompositeType.FunctionType? {
        val parameterTypes = lambda.parameters.map { it.type }
        val returnType = inferLambdaReturnType(lambda, context)
        
        return if (returnType != null) {
            CompositeType.FunctionType(parameterTypes, returnType)
        } else {
            null
        }
    }
    
    /**
     * 推断 Lambda 返回类型
     */
    private fun inferLambdaReturnType(lambda: LambdaExpression, context: TypeInferenceContext): Type? {
        // 分析 lambda 体的类型
        return when (val body = lambda.body) {
            is LiteralExpression.StringLiteral -> PrimitiveType.StringType
            is LiteralExpression.IntLiteral -> PrimitiveType.IntType
            is LiteralExpression.FloatLiteral -> PrimitiveType.DoubleType
            is LiteralExpression.BooleanLiteral -> PrimitiveType.BooleanType
            is LiteralExpression.NullLiteral -> PrimitiveType.NullType
            is IdentifierExpression -> context.getVariableType(body.name)
            is CallExpression -> body.inferredType
            is BinaryExpression -> inferBinaryExpressionType(body)
            else -> null
        }
    }
    
    /**
     * 推断二元表达式类型
     */
    private fun inferBinaryExpressionType(expr: BinaryExpression): Type? {
        val leftType = expr.left.inferredType
        val rightType = expr.right.inferredType
        
        return when (expr.operator) {
            BinaryOperator.PLUS, BinaryOperator.MINUS, BinaryOperator.MULTIPLY, BinaryOperator.DIVIDE -> {
                when {
                    leftType == PrimitiveType.IntType && rightType == PrimitiveType.IntType -> PrimitiveType.IntType
                    leftType == PrimitiveType.DoubleType || rightType == PrimitiveType.DoubleType -> PrimitiveType.DoubleType
                    leftType == PrimitiveType.StringType && expr.operator == BinaryOperator.PLUS -> PrimitiveType.StringType
                    else -> null
                }
            }
            BinaryOperator.EQUAL, BinaryOperator.NOT_EQUAL, 
            BinaryOperator.LESS_THAN, BinaryOperator.LESS_EQUAL,
            BinaryOperator.GREATER_THAN, BinaryOperator.GREATER_EQUAL -> PrimitiveType.BooleanType
            BinaryOperator.LOGICAL_AND, BinaryOperator.LOGICAL_OR -> PrimitiveType.BooleanType
            else -> null
        }
    }
    
    /**
     * 验证 Lambda 表达式
     */
    fun validateLambda(lambda: LambdaExpression, expectedType: CompositeType.FunctionType?): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查参数名重复
        val paramNames = lambda.parameters.map { it.name }
        val duplicateNames = paramNames.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            errors.add("Lambda 参数名重复: ${duplicateNames.joinToString(", ")}")
        }
        
        // 如果有期望类型，检查兼容性
        if (expectedType != null) {
            if (lambda.parameters.size != expectedType.parameterTypes.size) {
                errors.add("Lambda 参数数量不匹配")
            } else {
                for ((index, param) in lambda.parameters.withIndex()) {
                    val expectedParamType = expectedType.parameterTypes[index]
                    if (!expectedParamType.isAssignableFrom(param.type)) {
                        errors.add("Lambda 参数 '${param.name}' 类型不匹配")
                    }
                }
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
}

/**
 * 变量分析器
 * 分析表达式中引用的变量
 */
class VariableAnalyzer(private val localVariables: Set<String>) {
    
    fun analyze(expression: Expression): Set<String> {
        val referencedVariables = mutableSetOf<String>()
        analyzeExpression(expression, referencedVariables)
        return referencedVariables - localVariables
    }
    
    private fun analyzeExpression(expression: Expression, variables: MutableSet<String>) {
        when (expression) {
            is IdentifierExpression -> {
                variables.add(expression.name)
            }
            is BinaryExpression -> {
                analyzeExpression(expression.left, variables)
                analyzeExpression(expression.right, variables)
            }
            is UnaryExpression -> {
                analyzeExpression(expression.operand, variables)
            }
            is CodeLiteralExpression -> {
                // Code literals don't reference variables in the current scope
            }
            is CallExpression -> {
                analyzeExpression(expression.function, variables)
                expression.arguments.forEach { analyzeExpression(it.value, variables) }
            }
            is MemberExpression -> {
                analyzeExpression(expression.target, variables)
            }
            is ArrayExpression -> {
                expression.elements.forEach { analyzeExpression(it, variables) }
            }
            is ObjectExpression -> {
                expression.properties.forEach { 
                    analyzeExpression(it.key, variables)
                    analyzeExpression(it.value, variables)
                }
            }
            is LambdaExpression -> {
                // 创建新的作用域
                val nestedAnalyzer = VariableAnalyzer(localVariables + expression.parameters.map { it.name })
                variables.addAll(nestedAnalyzer.analyze(expression.body))
            }
            is ConditionalExpression -> {
                analyzeExpression(expression.condition, variables)
                analyzeExpression(expression.thenExpression, variables)
                analyzeExpression(expression.elseExpression, variables)
            }
            is CastExpression -> {
                analyzeExpression(expression.expression, variables)
            }
            is TypeCheckExpression -> {
                analyzeExpression(expression.expression, variables)
            }
            is MacroCall -> {
                expression.arguments.forEach { analyzeExpression(it as Expression, variables) }
            }
            is MacroParameterPlaceholder -> {
                // 宏参数占位符不引用变量
            }
            // 字面量不引用变量
            is LiteralExpression -> {}
        }
    }
}

// 数据类和枚举

/**
 * 高阶函数验证结果
 */
data class HigherOrderValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * 闭包分析结果
 */
data class ClosureAnalysisResult(
    val isValid: Boolean,
    val errors: List<String>,
    val capturedVariables: Set<String>,
    val requiresHeapAllocation: Boolean
)

/**
 * 闭包上下文
 */
interface ClosureContext {
    fun isVariableInScope(name: String): Boolean
    fun isVariableMutable(name: String): Boolean
    fun getVariableType(name: String): Type
}

/**
 * 闭包环境
 */
data class ClosureEnvironment(
    private val bindings: Map<String, ClosureBinding>
) {
    fun getBinding(name: String): ClosureBinding? = bindings[name]
    fun getAllBindings(): Map<String, ClosureBinding> = bindings
}

/**
 * 闭包绑定
 */
data class ClosureBinding(
    val name: String,
    val type: Type,
    val mutable: Boolean,
    val captureMode: CaptureMode
)

/**
 * 捕获模式
 */
enum class CaptureMode {
    BY_VALUE,    // 按值捕获
    BY_REFERENCE // 按引用捕获
}

/**
 * 访问类型
 */
enum class AccessType {
    READ, WRITE
}

/**
 * 类型推断上下文
 */
interface TypeInferenceContext {
    fun getVariableType(name: String): Type?
    fun getFunctionType(name: String): CompositeType.FunctionType?
}
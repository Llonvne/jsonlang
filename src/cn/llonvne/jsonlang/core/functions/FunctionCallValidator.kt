package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*

/**
 * 函数调用验证器
 * 负责验证函数调用的正确性，包括参数匹配和类型检查
 */
class FunctionCallValidator {
    
    /**
     * 验证函数调用
     */
    fun validateCall(
        call: CallExpression,
        signature: FunctionSignature
    ): CallValidationResult {
        val errors = mutableListOf<String>()
        
        // 验证参数数量
        val paramCountResult = validateParameterCount(call, signature)
        if (!paramCountResult.isValid) {
            errors.addAll(paramCountResult.errors)
        }
        
        // 验证参数类型
        val typeResult = validateParameterTypes(call, signature)
        if (!typeResult.isValid) {
            errors.addAll(typeResult.errors)
        }
        
        // 创建参数映射
        val argumentMapping = if (errors.isEmpty()) {
            createArgumentMapping(call, signature)
        } else {
            null
        }
        
        return CallValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            argumentMapping = argumentMapping
        )
    }
    
    /**
     * 验证参数数量
     */
    private fun validateParameterCount(
        call: CallExpression,
        signature: FunctionSignature
    ): ValidationResult {
        val arguments = call.arguments
        val parameters = signature.parameters
        val errors = mutableListOf<String>()
        
        val requiredParamCount = parameters.count { !it.optional && !it.hasDefaultValue }
        val maxParamCount = parameters.size
        
        when {
            arguments.size < requiredParamCount -> {
                errors.add("参数数量不足: 需要至少 $requiredParamCount 个参数，但提供了 ${arguments.size} 个")
            }
            arguments.size > maxParamCount -> {
                errors.add("参数数量过多: 最多接受 $maxParamCount 个参数，但提供了 ${arguments.size} 个")
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * 验证参数类型
     */
    private fun validateParameterTypes(
        call: CallExpression,
        signature: FunctionSignature
    ): ValidationResult {
        val arguments = call.arguments
        val parameters = signature.parameters
        val errors = mutableListOf<String>()
        
        // 创建参数映射以便类型检查
        val argMapping = createArgumentMapping(call, signature)
        
        for ((paramName, argExpr) in argMapping) {
            val parameter = parameters.find { it.name == paramName }
            if (parameter != null) {
                val argType = argExpr.inferredType
                if (argType != null && !parameter.type.isAssignableFrom(argType)) {
                    errors.add(
                        "参数 '$paramName' 类型不匹配: " +
                        "期望 ${parameter.type.getDisplayName()}，" +
                        "但得到 ${argType.getDisplayName()}"
                    )
                }
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * 创建参数映射
     */
    private fun createArgumentMapping(
        call: CallExpression,
        signature: FunctionSignature
    ): Map<String, Expression> {
        val arguments = call.arguments
        val parameters = signature.parameters
        val mapping = mutableMapOf<String, Expression>()
        
        // 处理位置参数
        val positionalArgs = arguments.filter { it.name == null }
        for (i in positionalArgs.indices) {
            if (i < parameters.size) {
                mapping[parameters[i].name] = positionalArgs[i].value
            }
        }
        
        // 处理命名参数
        val namedArgs = arguments.filter { it.name != null }
        for (namedArg in namedArgs) {
            val paramName = namedArg.name!!
            if (parameters.any { it.name == paramName }) {
                mapping[paramName] = namedArg.value
            }
        }
        
        // 添加默认值参数
        for (parameter in parameters) {
            if (!mapping.containsKey(parameter.name) && parameter.hasDefaultValue) {
                mapping[parameter.name] = parameter.defaultValue!!
            }
        }
        
        return mapping
    }
    
    /**
     * 验证函数修饰符兼容性
     */
    fun validateModifierCompatibility(
        call: CallExpression,
        signature: FunctionSignature,
        context: CallContext
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查异步函数调用
        if (signature.modifiers.async && !context.allowAsync) {
            errors.add("异步函数 '${signature.name}' 只能在异步上下文中调用")
        }
        
        // 检查内联函数调用
        if (signature.modifiers.inline && !context.allowInline) {
            errors.add("内联函数 '${signature.name}' 在当前上下文中不能被调用")
        }
        
        // 检查纯函数调用
        if (signature.modifiers.pure && context.requiresPurity && !isPureCall(call)) {
            errors.add("在纯函数上下文中调用了非纯函数 '${signature.name}'")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * 检查是否为纯函数调用
     */
    private fun isPureCall(call: CallExpression): Boolean {
        // 简化实现：检查调用是否有副作用
        // 实际实现需要更复杂的分析
        return call.arguments.all { arg ->
            isPureExpression(arg.value)
        }
    }
    
    /**
     * 检查表达式是否为纯表达式
     */
    private fun isPureExpression(expr: Expression): Boolean {
        return when (expr) {
            is LiteralExpression -> true
            is IdentifierExpression -> true
            is BinaryExpression -> isPureExpression(expr.left) && isPureExpression(expr.right)
            is UnaryExpression -> isPureExpression(expr.operand)
            is CallExpression -> false  // 简化：假设所有函数调用都不纯
            else -> false
        }
    }
}

/**
 * 调用验证结果
 */
data class CallValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val argumentMapping: Map<String, Expression>?
)

/**
 * 调用上下文
 */
data class CallContext(
    val allowAsync: Boolean = true,
    val allowInline: Boolean = true,
    val requiresPurity: Boolean = false,
    val currentFunction: FunctionSignature? = null
)
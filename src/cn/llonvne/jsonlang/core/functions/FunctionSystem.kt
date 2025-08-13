package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation

/**
 * 函数系统核心类
 * 负责函数定义、调用、重载解析和类型检查
 */
class FunctionSystem {
    private val functions = mutableMapOf<String, MutableList<FunctionSignature>>()
    private val overloadResolver = OverloadResolver()
    private val callValidator = FunctionCallValidator()
    
    /**
     * 注册函数定义
     */
    fun registerFunction(declaration: FunctionDeclaration): FunctionRegistrationResult {
        val signature = FunctionSignature.fromDeclaration(declaration)
        
        // 检查函数签名有效性
        val validationResult = validateFunctionSignature(signature)
        if (!validationResult.isValid) {
            return FunctionRegistrationResult.Error(validationResult.errors)
        }
        
        // 检查重载冲突
        val existingFunctions = functions[declaration.name] ?: mutableListOf()
        val overloadResult = overloadResolver.checkOverloadConflict(signature, existingFunctions)
        if (!overloadResult.isValid) {
            return FunctionRegistrationResult.Error(overloadResult.errors)
        }
        
        // 注册函数
        existingFunctions.add(signature)
        functions[declaration.name] = existingFunctions
        
        return FunctionRegistrationResult.Success(signature)
    }
    
    /**
     * 解析函数调用
     */
    fun resolveCall(call: CallExpression): FunctionCallResult {
        // 获取函数名
        val functionName = when (val func = call.function) {
            is IdentifierExpression -> func.name
            else -> return FunctionCallResult.Error(listOf("不支持的函数调用形式"))
        }
        
        // 获取候选函数
        val candidates = functions[functionName] 
            ?: return FunctionCallResult.Error(listOf("未找到函数: $functionName"))
        
        // 解析重载
        val overloadResult = overloadResolver.resolveOverload(call, candidates)
        if (!overloadResult.isValid) {
            return FunctionCallResult.Error(overloadResult.errors)
        }
        
        val selectedFunction = overloadResult.selectedFunction!!
        
        // 验证调用
        val callResult = callValidator.validateCall(call, selectedFunction)
        if (!callResult.isValid) {
            return FunctionCallResult.Error(callResult.errors)
        }
        
        return FunctionCallResult.Success(selectedFunction, callResult.argumentMapping!!)
    }
    
    /**
     * 获取函数签名
     */
    fun getFunctionSignatures(name: String): List<FunctionSignature> {
        return functions[name] ?: emptyList()
    }
    
    /**
     * 检查函数是否存在
     */
    fun hasFunction(name: String): Boolean {
        return functions.containsKey(name)
    }
    
    /**
     * 验证函数签名
     */
    private fun validateFunctionSignature(signature: FunctionSignature): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查参数名重复
        val paramNames = signature.parameters.map { it.name }
        val duplicateNames = paramNames.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            errors.add("参数名重复: ${duplicateNames.joinToString(", ")}")
        }
        
        // 检查默认参数顺序
        var hasDefaultParam = false
        for (param in signature.parameters) {
            if (param.hasDefaultValue) {
                hasDefaultParam = true
            } else if (hasDefaultParam && !param.optional) {
                errors.add("必需参数不能出现在有默认值的参数之后: ${param.name}")
            }
        }
        
        // 检查泛型参数约束
        for (generic in signature.generics) {
            for (constraint in generic.constraints) {
                if (!isValidTypeConstraint(constraint)) {
                    errors.add("无效的泛型约束: ${constraint.getDisplayName()}")
                }
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    private fun isValidTypeConstraint(type: Type): Boolean {
        // 简化实现：检查约束类型是否有效
        return when (type) {
            is PrimitiveType.NothingType -> false
            is PrimitiveType.NullType -> false
            else -> true
        }
    }
}

/**
 * 函数签名
 */
data class FunctionSignature(
    val name: String,
    val modifiers: FunctionModifiers,
    val generics: List<GenericParameter>,
    val parameters: List<FunctionParameter>,
    val returnType: Type,
    val visibility: Visibility,
    val annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>,
    val location: SourceLocation
) {
    companion object {
        fun fromDeclaration(declaration: FunctionDeclaration): FunctionSignature {
            return FunctionSignature(
                name = declaration.name,
                modifiers = declaration.modifiers,
                generics = declaration.generics,
                parameters = declaration.parameters.map { FunctionParameter.fromParameter(it) },
                returnType = declaration.returnType,
                visibility = declaration.visibility,
                annotations = declaration.annotations,
                location = declaration.location
            )
        }
    }
    
    /**
     * 获取函数类型
     */
    fun getFunctionType(): CompositeType.FunctionType {
        return CompositeType.FunctionType(
            parameterTypes = parameters.map { it.type },
            returnType = returnType
        )
    }
    
    /**
     * 检查是否匹配调用
     */
    fun matchesCall(arguments: List<Argument>): Boolean {
        return parameters.size >= arguments.size && 
               arguments.all { arg -> 
                   parameters.any { param -> 
                       param.name == arg.name || arg.name == null 
                   }
               }
    }
}

/**
 * 函数参数
 */
data class FunctionParameter(
    val name: String,
    val type: Type,
    val optional: Boolean,
    val defaultValue: Expression?,
    val annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
) {
    val hasDefaultValue: Boolean get() = defaultValue != null
    
    companion object {
        fun fromParameter(parameter: Parameter): FunctionParameter {
            return FunctionParameter(
                name = parameter.name,
                type = parameter.type,
                optional = parameter.optional,
                defaultValue = parameter.defaultValue,
                annotations = parameter.annotations
            )
        }
    }
}

/**
 * 函数注册结果
 */
sealed class FunctionRegistrationResult {
    data class Success(val signature: FunctionSignature) : FunctionRegistrationResult()
    data class Error(val errors: List<String>) : FunctionRegistrationResult()
}

/**
 * 函数调用结果
 */
sealed class FunctionCallResult {
    data class Success(
        val function: FunctionSignature,
        val argumentMapping: Map<String, Expression>
    ) : FunctionCallResult()
    data class Error(val errors: List<String>) : FunctionCallResult()
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.annotations.*
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 函数修饰符处理器
 * 负责处理函数的各种修饰符，如可见性、异步、内联等
 */
class FunctionModifierProcessor {
    
    /**
     * 验证函数修饰符组合
     */
    fun validateModifierCombination(
        modifiers: FunctionModifiers,
        signature: FunctionSignature
    ): ModifierValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证异步修饰符
        if (modifiers.async) {
            validateAsyncModifier(signature, errors, warnings)
        }
        
        // 验证内联修饰符
        if (modifiers.inline) {
            validateInlineModifier(signature, errors, warnings)
        }
        
        // 验证纯函数修饰符
        if (modifiers.pure) {
            validatePureModifier(signature, errors, warnings)
        }
        
        // 验证外部函数修饰符
        if (modifiers.external) {
            validateExternalModifier(signature, errors, warnings)
        }
        
        // 验证修饰符冲突
        validateModifierConflicts(modifiers, errors)
        
        return ModifierValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证异步修饰符
     */
    private fun validateAsyncModifier(
        signature: FunctionSignature,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // 异步函数的返回类型应该是 Promise 或类似的异步类型
        // 简化实现：检查返回类型名称
        val returnTypeName = signature.returnType.name
        if (!returnTypeName.contains("Promise") && 
            !returnTypeName.contains("Future") && 
            !returnTypeName.contains("Task")) {
            warnings.add("异步函数 '${signature.name}' 的返回类型可能应该是异步类型")
        }
        
        // 异步函数不能是纯函数
        if (signature.modifiers.pure) {
            errors.add("异步函数不能同时是纯函数")
        }
    }
    
    /**
     * 验证内联修饰符
     */
    private fun validateInlineModifier(
        signature: FunctionSignature,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // 内联函数应该是小函数
        if (signature.returnType.size > 1024) { // 简化检查
            warnings.add("内联函数 '${signature.name}' 可能过大，不适合内联")
        }
        
        // 递归函数不应该内联
        if (isRecursiveFunction(signature)) {
            warnings.add("递归函数 '${signature.name}' 不建议内联")
        }
        
        // 内联函数不能是外部函数
        if (signature.modifiers.external) {
            errors.add("内联函数不能是外部函数")
        }
    }
    
    /**
     * 验证纯函数修饰符
     */
    private fun validatePureModifier(
        signature: FunctionSignature,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // 纯函数不能有副作用
        // 简化实现：检查参数类型
        for (param in signature.parameters) {
            if (param.type.name.contains("IO") || param.type.name.contains("File")) {
                warnings.add("纯函数 '${signature.name}' 的参数可能涉及 I/O 操作")
            }
        }
        
        // 纯函数的返回类型不应该是 void/Unit
        if (signature.returnType == PrimitiveType.UnitType) {
            warnings.add("纯函数 '${signature.name}' 返回 Unit，可能没有有用的计算结果")
        }
    }
    
    /**
     * 验证外部函数修饰符
     */
    private fun validateExternalModifier(
        signature: FunctionSignature,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // 外部函数应该有函数体声明但没有实现
        // 这里简化处理，实际应该检查 AST 结构
        
        // 外部函数不能有默认参数
        if (signature.parameters.any { it.hasDefaultValue }) {
            errors.add("外部函数 '${signature.name}' 不能有默认参数")
        }
    }
    
    /**
     * 验证修饰符冲突
     */
    private fun validateModifierConflicts(
        modifiers: FunctionModifiers,
        errors: MutableList<String>
    ) {
        // 异步和纯函数冲突
        if (modifiers.async && modifiers.pure) {
            errors.add("函数不能同时是异步和纯函数")
        }
        
        // 内联和外部函数冲突
        if (modifiers.inline && modifiers.external) {
            errors.add("函数不能同时是内联和外部函数")
        }
    }
    
    /**
     * 检查是否为递归函数
     */
    private fun isRecursiveFunction(signature: FunctionSignature): Boolean {
        // 简化实现：假设不是递归函数
        // 实际实现需要分析函数体
        return false
    }
    
    /**
     * 应用函数修饰符
     */
    fun applyModifiers(
        signature: FunctionSignature,
        context: ModifierApplicationContext
    ): ModifierApplicationResult {
        val transformations = mutableListOf<FunctionTransformation>()
        
        // 应用内联修饰符
        if (signature.modifiers.inline) {
            transformations.add(FunctionTransformation.InlineTransformation(signature))
        }
        
        // 应用异步修饰符
        if (signature.modifiers.async) {
            transformations.add(FunctionTransformation.AsyncTransformation(signature))
        }
        
        // 应用纯函数优化
        if (signature.modifiers.pure) {
            transformations.add(FunctionTransformation.PureOptimization(signature))
        }
        
        return ModifierApplicationResult(
            transformations = transformations,
            optimizations = extractOptimizations(signature)
        )
    }
    
    /**
     * 提取优化信息
     */
    private fun extractOptimizations(signature: FunctionSignature): List<FunctionOptimization> {
        val optimizations = mutableListOf<FunctionOptimization>()
        
        if (signature.modifiers.pure) {
            optimizations.add(FunctionOptimization.MEMOIZATION)
            optimizations.add(FunctionOptimization.CONSTANT_FOLDING)
        }
        
        if (signature.modifiers.inline) {
            optimizations.add(FunctionOptimization.INLINE_EXPANSION)
        }
        
        return optimizations
    }
}

/**
 * 注解处理器
 * 负责处理函数和参数上的注解
 */
class FunctionAnnotationProcessor {
    
    /**
     * 处理函数注解
     */
    fun processFunctionAnnotations(
        signature: FunctionSignature
    ): AnnotationProcessingResult {
        val metadata = mutableMapOf<String, Any>()
        val validationRules = mutableListOf<ValidationRule>()
        val transformations = mutableListOf<AnnotationTransformation>()
        
        for (annotation in signature.annotations) {
            when (annotation.name) {
                "Deprecated" -> processDeprecatedAnnotation(annotation, metadata, validationRules)
                "Override" -> processOverrideAnnotation(annotation, metadata, validationRules)
                "Throws" -> processThrowsAnnotation(annotation, metadata, validationRules)
                "Since" -> processSinceAnnotation(annotation, metadata)
                "Author" -> processAuthorAnnotation(annotation, metadata)
                "Performance" -> processPerformanceAnnotation(annotation, metadata, transformations)
                "Security" -> processSecurityAnnotation(annotation, metadata, validationRules)
                "Test" -> processTestAnnotation(annotation, metadata, transformations)
                else -> processCustomAnnotation(annotation, metadata)
            }
        }
        
        return AnnotationProcessingResult(
            metadata = metadata,
            validationRules = validationRules,
            transformations = transformations
        )
    }
    
    /**
     * 处理参数注解
     */
    fun processParameterAnnotations(
        parameter: FunctionParameter
    ): ParameterAnnotationResult {
        val constraints = mutableListOf<ParameterConstraint>()
        val metadata = mutableMapOf<String, Any>()
        
        for (annotation in parameter.annotations) {
            when (annotation.name) {
                "NotNull" -> constraints.add(ParameterConstraint.NOT_NULL)
                "Range" -> processRangeAnnotation(annotation, constraints)
                "Pattern" -> processPatternAnnotation(annotation, constraints)
                "Size" -> processSizeAnnotation(annotation, constraints)
                "Valid" -> constraints.add(ParameterConstraint.VALID)
                "Inject" -> processInjectAnnotation(annotation, metadata)
                else -> processCustomParameterAnnotation(annotation, metadata)
            }
        }
        
        return ParameterAnnotationResult(
            constraints = constraints,
            metadata = metadata
        )
    }
    
    /**
     * 验证注解约束
     */
    fun validateAnnotationConstraints(
        call: CallExpression,
        signature: FunctionSignature
    ): ConstraintValidationResult {
        val violations = mutableListOf<ConstraintViolation>()
        
        // 验证参数约束
        for ((index, argument) in call.arguments.withIndex()) {
            if (index < signature.parameters.size) {
                val parameter = signature.parameters[index]
                val paramResult = processParameterAnnotations(parameter)
                
                for (constraint in paramResult.constraints) {
                    val violation = validateParameterConstraint(argument, constraint, parameter)
                    if (violation != null) {
                        violations.add(violation)
                    }
                }
            }
        }
        
        return ConstraintValidationResult(
            isValid = violations.isEmpty(),
            violations = violations
        )
    }
    
    // 具体注解处理方法
    
    private fun processDeprecatedAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>,
        validationRules: MutableList<ValidationRule>
    ) {
        val message = annotation.getStringParameter("message") ?: "此函数已弃用"
        val since = annotation.getStringParameter("since")
        
        metadata["deprecated"] = true
        metadata["deprecationMessage"] = message
        if (since != null) {
            metadata["deprecatedSince"] = since
        }
        
        validationRules.add(ValidationRule.DEPRECATION_WARNING)
    }
    
    private fun processOverrideAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>,
        validationRules: MutableList<ValidationRule>
    ) {
        metadata["override"] = true
        validationRules.add(ValidationRule.OVERRIDE_CHECK)
    }
    
    private fun processThrowsAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>,
        validationRules: MutableList<ValidationRule>
    ) {
        val exceptions = annotation.getArrayParameter("value")?.mapNotNull { it.asString() } ?: emptyList()
        metadata["declaredExceptions"] = exceptions
        validationRules.add(ValidationRule.EXCEPTION_DECLARATION_CHECK)
    }
    
    private fun processSinceAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>
    ) {
        val version = annotation.getStringParameter("value")
        if (version != null) {
            metadata["since"] = version
        }
    }
    
    private fun processAuthorAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>
    ) {
        val author = annotation.getStringParameter("value")
        if (author != null) {
            metadata["author"] = author
        }
    }
    
    private fun processPerformanceAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>,
        transformations: MutableList<AnnotationTransformation>
    ) {
        val complexity = annotation.getStringParameter("complexity")
        val cacheable = annotation.getBooleanParameter("cacheable") ?: false
        
        if (complexity != null) {
            metadata["timeComplexity"] = complexity
        }
        
        if (cacheable) {
            transformations.add(AnnotationTransformation.CACHING)
        }
    }
    
    private fun processSecurityAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>,
        validationRules: MutableList<ValidationRule>
    ) {
        val level = annotation.getStringParameter("level") ?: "MEDIUM"
        val permissions = annotation.getArrayParameter("permissions")?.mapNotNull { it.asString() } ?: emptyList()
        
        metadata["securityLevel"] = level
        metadata["requiredPermissions"] = permissions
        validationRules.add(ValidationRule.SECURITY_CHECK)
    }
    
    private fun processTestAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>,
        transformations: MutableList<AnnotationTransformation>
    ) {
        metadata["isTest"] = true
        transformations.add(AnnotationTransformation.TEST_INSTRUMENTATION)
    }
    
    private fun processCustomAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>
    ) {
        metadata["custom_${annotation.name}"] = annotation.parameters
    }
    
    private fun processRangeAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        constraints: MutableList<ParameterConstraint>
    ) {
        val min = annotation.getIntParameter("min")
        val max = annotation.getIntParameter("max")
        
        if (min != null || max != null) {
            constraints.add(ParameterConstraint.Range(min, max))
        }
    }
    
    private fun processPatternAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        constraints: MutableList<ParameterConstraint>
    ) {
        val pattern = annotation.getStringParameter("value")
        if (pattern != null) {
            constraints.add(ParameterConstraint.Pattern(pattern))
        }
    }
    
    private fun processSizeAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        constraints: MutableList<ParameterConstraint>
    ) {
        val min = annotation.getIntParameter("min")?.toInt()
        val max = annotation.getIntParameter("max")?.toInt()
        
        constraints.add(ParameterConstraint.Size(min, max))
    }
    
    private fun processInjectAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>
    ) {
        val qualifier = annotation.getStringParameter("qualifier")
        metadata["inject"] = true
        if (qualifier != null) {
            metadata["injectQualifier"] = qualifier
        }
    }
    
    private fun processCustomParameterAnnotation(
        annotation: cn.llonvne.jsonlang.core.annotations.Annotation,
        metadata: MutableMap<String, Any>
    ) {
        metadata["custom_${annotation.name}"] = annotation.parameters
    }
    
    private fun validateParameterConstraint(
        argument: Argument,
        constraint: ParameterConstraint,
        parameter: FunctionParameter
    ): ConstraintViolation? {
        // 简化实现：基本约束验证
        return when (constraint) {
            ParameterConstraint.NOT_NULL -> {
                if (argument.value is LiteralExpression.NullLiteral) {
                    ConstraintViolation(
                        parameter = parameter.name,
                        constraint = "NotNull",
                        message = "参数 '${parameter.name}' 不能为 null"
                    )
                } else null
            }
            is ParameterConstraint.Range -> {
                // 检查数值范围
                val value = when (val argValue = argument.value) {
                    is LiteralExpression.IntLiteral -> argValue.value.toDouble()
                    is LiteralExpression.FloatLiteral -> argValue.value
                    else -> return null
                }
                
                val min = constraint.min?.toDouble()
                val max = constraint.max?.toDouble()
                
                when {
                    min != null && value < min -> ConstraintViolation(
                        parameter.name, "Range", "值 $value 小于最小值 $min"
                    )
                    max != null && value > max -> ConstraintViolation(
                        parameter.name, "Range", "值 $value 大于最大值 $max"
                    )
                    else -> null
                }
            }
            else -> null // 其他约束的简化实现
        }
    }
}

// 数据类和枚举

/**
 * 修饰符验证结果
 */
data class ModifierValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * 修饰符应用结果
 */
data class ModifierApplicationResult(
    val transformations: List<FunctionTransformation>,
    val optimizations: List<FunctionOptimization>
)

/**
 * 修饰符应用上下文
 */
data class ModifierApplicationContext(
    val targetPlatform: String,
    val optimizationLevel: Int,
    val debugMode: Boolean
)

/**
 * 函数转换
 */
sealed class FunctionTransformation {
    data class InlineTransformation(val signature: FunctionSignature) : FunctionTransformation()
    data class AsyncTransformation(val signature: FunctionSignature) : FunctionTransformation()
    data class PureOptimization(val signature: FunctionSignature) : FunctionTransformation()
}

/**
 * 函数优化
 */
enum class FunctionOptimization {
    MEMOIZATION,
    CONSTANT_FOLDING,
    INLINE_EXPANSION,
    TAIL_CALL_OPTIMIZATION
}

/**
 * 注解处理结果
 */
data class AnnotationProcessingResult(
    val metadata: Map<String, Any>,
    val validationRules: List<ValidationRule>,
    val transformations: List<AnnotationTransformation>
)

/**
 * 参数注解结果
 */
data class ParameterAnnotationResult(
    val constraints: List<ParameterConstraint>,
    val metadata: Map<String, Any>
)

/**
 * 约束验证结果
 */
data class ConstraintValidationResult(
    val isValid: Boolean,
    val violations: List<ConstraintViolation>
)

/**
 * 约束违反
 */
data class ConstraintViolation(
    val parameter: String,
    val constraint: String,
    val message: String
)

/**
 * 验证规则
 */
enum class ValidationRule {
    DEPRECATION_WARNING,
    OVERRIDE_CHECK,
    EXCEPTION_DECLARATION_CHECK,
    SECURITY_CHECK
}

/**
 * 注解转换
 */
enum class AnnotationTransformation {
    CACHING,
    TEST_INSTRUMENTATION,
    SECURITY_WRAPPER,
    LOGGING
}

/**
 * 参数约束
 */
sealed class ParameterConstraint {
    object NOT_NULL : ParameterConstraint()
    object VALID : ParameterConstraint()
    data class Range(val min: Number?, val max: Number?) : ParameterConstraint()
    data class Pattern(val regex: String) : ParameterConstraint()
    data class Size(val min: Int?, val max: Int?) : ParameterConstraint()
}
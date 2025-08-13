package cn.llonvne.jsonlang.core.ast.validation

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.errors.ErrorReporter

/**
 * AST 验证引擎
 * 负责管理和执行 AST 验证
 */
class ValidationEngine(
    private val errorReporter: ErrorReporter
) {
    private val validators = mutableListOf<ASTValidator>()
    
    /**
     * 注册验证器
     */
    fun registerValidator(validator: ASTValidator) {
        validators.add(validator)
        validators.sortBy { it.priority }
    }
    
    /**
     * 注册多个验证器
     */
    fun registerValidators(vararg validators: ASTValidator) {
        validators.forEach { registerValidator(it) }
    }
    
    /**
     * 移除验证器
     */
    fun removeValidator(name: String) {
        validators.removeAll { it.name == name }
    }
    
    /**
     * 获取所有已注册的验证器
     */
    fun getValidators(): List<ASTValidator> = validators.toList()
    
    /**
     * 验证程序
     */
    fun validate(program: Program, options: ValidationOptions = ValidationOptions()): ValidationResult {
        val context = ValidationContext(program, errorReporter, options)
        
        var overallResult = ValidationResult.success()
        
        for (validator in validators.filter { it.enabled }) {
            val result = validator.validate(program, context)
            overallResult = overallResult.merge(result)
            
            // 报告错误和警告
            result.errors.forEach { error ->
                errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = error.code,
                        message = error.message,
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                        location = error.location
                    )
                )
            }
            
            result.warnings.forEach { warning ->
                errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = warning.code,
                        message = warning.message,
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                        location = warning.location
                    )
                )
            }
            
            result.infos.forEach { info ->
                errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = info.code,
                        message = info.message,
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.INFO,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                        location = info.location
                    )
                )
            }
        }
        
        return overallResult
    }
    
    /**
     * 验证单个节点
     */
    fun validateNode(
        node: ASTNode, 
        validatorName: String, 
        context: ValidationContext
    ): ValidationResult {
        val validator = validators.find { it.name == validatorName }
            ?: throw IllegalArgumentException("未找到验证器: $validatorName")
        
        return validator.validate(node, context)
    }
    
    /**
     * 递归验证节点及其子节点
     */
    fun validateRecursively(node: ASTNode, context: ValidationContext): ValidationResult {
        val visitor = ValidationVisitor(this, context)
        return node.accept(visitor)
    }
}

/**
 * 验证访问者
 * 递归地对 AST 节点应用验证
 */
private class ValidationVisitor(
    private val engine: ValidationEngine,
    private val context: ValidationContext
) : RecursiveASTVisitor<ValidationResult>() {
    
    override fun defaultVisit(node: ASTNode): ValidationResult {
        var result = ValidationResult.success()
        
        // 对当前节点应用所有验证器
        for (validator in engine.getValidators().filter { it.enabled }) {
            val validationResult = validator.validate(node, context)
            result = result.merge(validationResult)
        }
        
        return result
    }
    
    override fun visitProgram(node: Program): ValidationResult {
        context.enterScope()
        val result = super.visitProgram(node)
        context.exitScope()
        return result
    }
    
    override fun visitFunctionDeclaration(node: FunctionDeclaration): ValidationResult {
        context.enterScope()
        context.symbolTable[node.name] = node
        val result = super.visitFunctionDeclaration(node)
        context.exitScope()
        return result
    }
    
    override fun visitBlockStatement(node: BlockStatement): ValidationResult {
        context.enterScope()
        val result = super.visitBlockStatement(node)
        context.exitScope()
        return result
    }
}

/**
 * 验证引擎构建器
 * 用于方便地构建和配置验证引擎
 */
class ValidationEngineBuilder {
    private val validators = mutableListOf<ASTValidator>()
    private var errorReporter: ErrorReporter? = null
    
    fun withErrorReporter(reporter: ErrorReporter): ValidationEngineBuilder {
        this.errorReporter = reporter
        return this
    }
    
    fun addValidator(validator: ASTValidator): ValidationEngineBuilder {
        validators.add(validator)
        return this
    }
    
    fun addStructuralValidation(): ValidationEngineBuilder {
        validators.add(StructuralValidator())
        return this
    }
    
    fun addSemanticValidation(): ValidationEngineBuilder {
        validators.add(SemanticValidator())
        return this
    }
    
    fun addAllValidations(): ValidationEngineBuilder {
        return addStructuralValidation()
            .addSemanticValidation()
    }
    
    fun build(): ValidationEngine {
        val reporter = errorReporter ?: throw IllegalStateException("ErrorReporter 必须设置")
        val engine = ValidationEngine(reporter)
        
        validators.sortedBy { it.priority }.forEach { engine.registerValidator(it) }
        
        return engine
    }
}

/**
 * 验证报告
 * 包含验证结果的详细报告
 */
data class ValidationReport(
    val result: ValidationResult,
    val statistics: ValidationStatistics,
    val executedValidators: List<String>
) {
    /** 是否验证通过 */
    val isValid: Boolean get() = result.isValid
    
    /** 错误总数 */
    val errorCount: Int get() = result.errors.size
    
    /** 警告总数 */
    val warningCount: Int get() = result.warnings.size
    
    /** 信息总数 */
    val infoCount: Int get() = result.infos.size
    
    /**
     * 生成验证报告摘要
     */
    fun generateSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("=== AST 验证报告 ===")
        sb.appendLine("验证状态: ${if (isValid) "通过" else "失败"}")
        sb.appendLine("错误数量: $errorCount")
        sb.appendLine("警告数量: $warningCount")
        sb.appendLine("信息数量: $infoCount")
        sb.appendLine("执行的验证器: ${executedValidators.joinToString(", ")}")
        sb.appendLine()
        
        if (result.errors.isNotEmpty()) {
            sb.appendLine("错误详情:")
            result.errors.forEach { error ->
                sb.appendLine("  [${error.code}] ${error.message} (${error.location})")
            }
            sb.appendLine()
        }
        
        if (result.warnings.isNotEmpty()) {
            sb.appendLine("警告详情:")
            result.warnings.forEach { warning ->
                sb.appendLine("  [${warning.code}] ${warning.message} (${warning.location})")
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
}

/**
 * 验证工具类
 * 提供便捷的验证方法
 */
object ValidationUtils {
    
    /**
     * 快速验证程序
     */
    fun quickValidate(program: Program, errorReporter: ErrorReporter): ValidationResult {
        val engine = ValidationEngineBuilder()
            .withErrorReporter(errorReporter)
            .addAllValidations()
            .build()
        
        return engine.validate(program)
    }
    
    /**
     * 创建默认验证引擎
     */
    fun createDefaultEngine(errorReporter: ErrorReporter): ValidationEngine {
        return ValidationEngineBuilder()
            .withErrorReporter(errorReporter)
            .addAllValidations()
            .build()
    }
    
    /**
     * 验证并生成报告
     */
    fun validateWithReport(program: Program, errorReporter: ErrorReporter): ValidationReport {
        val engine = createDefaultEngine(errorReporter)
        val context = ValidationContext(program, errorReporter)
        val result = engine.validate(program)
        
        return ValidationReport(
            result = result,
            statistics = context.statistics,
            executedValidators = engine.getValidators().map { it.name }
        )
    }
}
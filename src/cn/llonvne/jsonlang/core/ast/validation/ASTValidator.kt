package cn.llonvne.jsonlang.core.ast.validation

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.errors.ErrorReporter

/**
 * AST 验证器接口
 * 定义 AST 验证的基本契约
 */
interface ASTValidator {
    /** 验证器名称 */
    val name: String
    
    /** 验证器描述 */
    val description: String
    
    /** 验证优先级，数值越小优先级越高 */
    val priority: Int get() = 0
    
    /** 是否启用此验证器 */
    val enabled: Boolean get() = true
    
    /**
     * 验证 AST 节点
     * @param node 要验证的节点
     * @param context 验证上下文
     * @return 验证结果
     */
    fun validate(node: ASTNode, context: ValidationContext): ValidationResult
}

/**
 * 验证上下文
 */
data class ValidationContext(
    /** 当前程序根节点 */
    val program: Program,
    
    /** 错误报告器 */
    val errorReporter: ErrorReporter,
    
    /** 验证选项 */
    val options: ValidationOptions = ValidationOptions(),
    
    /** 符号表 */
    val symbolTable: MutableMap<String, ASTNode> = mutableMapOf(),
    
    /** 当前作用域深度 */
    var scopeDepth: Int = 0,
    
    /** 验证统计信息 */
    val statistics: ValidationStatistics = ValidationStatistics()
) {
    /** 进入新作用域 */
    fun enterScope() {
        scopeDepth++
    }
    
    /** 退出当前作用域 */
    fun exitScope() {
        scopeDepth--
    }
    
    /** 记录验证 */
    fun recordValidation(validatorName: String, result: ValidationResult) {
        statistics.recordValidation(validatorName, result)
    }
}

/**
 * 验证选项
 */
data class ValidationOptions(
    /** 是否启用严格模式 */
    val strictMode: Boolean = false,
    
    /** 是否检查未使用的变量 */
    val checkUnusedVariables: Boolean = true,
    
    /** 是否检查类型一致性 */
    val checkTypeConsistency: Boolean = true,
    
    /** 是否检查控制流 */
    val checkControlFlow: Boolean = true,
    
    /** 是否检查函数签名 */
    val checkFunctionSignatures: Boolean = true,
    
    /** 是否检查导入导出 */
    val checkImportsExports: Boolean = true
)

/**
 * 验证结果
 */
data class ValidationResult(
    /** 是否验证通过 */
    val isValid: Boolean,
    
    /** 错误列表 */
    val errors: List<ValidationError> = emptyList(),
    
    /** 警告列表 */
    val warnings: List<ValidationWarning> = emptyList(),
    
    /** 信息列表 */
    val infos: List<ValidationInfo> = emptyList()
) {
    /** 是否有错误 */
    val hasErrors: Boolean get() = errors.isNotEmpty()
    
    /** 是否有警告 */
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    
    /** 合并验证结果 */
    fun merge(other: ValidationResult): ValidationResult {
        return ValidationResult(
            isValid = isValid && other.isValid,
            errors = errors + other.errors,
            warnings = warnings + other.warnings,
            infos = infos + other.infos
        )
    }
    
    companion object {
        /** 成功的验证结果 */
        fun success(): ValidationResult = ValidationResult(true)
        
        /** 失败的验证结果 */
        fun failure(error: ValidationError): ValidationResult = 
            ValidationResult(false, listOf(error))
        
        /** 带警告的验证结果 */
        fun warning(warning: ValidationWarning): ValidationResult = 
            ValidationResult(true, warnings = listOf(warning))
    }
}

/**
 * 验证错误
 */
data class ValidationError(
    val code: String,
    val message: String,
    val location: cn.llonvne.jsonlang.core.source.SourceLocation,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * 验证警告
 */
data class ValidationWarning(
    val code: String,
    val message: String,
    val location: cn.llonvne.jsonlang.core.source.SourceLocation
)

/**
 * 验证信息
 */
data class ValidationInfo(
    val code: String,
    val message: String,
    val location: cn.llonvne.jsonlang.core.source.SourceLocation
)

/**
 * 错误严重程度
 */
enum class ErrorSeverity {
    ERROR, WARNING, INFO
}

/**
 * 验证统计信息
 */
class ValidationStatistics {
    private val validationCounts = mutableMapOf<String, Int>()
    private val errorCounts = mutableMapOf<String, Int>()
    private val warningCounts = mutableMapOf<String, Int>()
    
    /** 记录验证 */
    fun recordValidation(validatorName: String, result: ValidationResult) {
        validationCounts[validatorName] = validationCounts.getOrDefault(validatorName, 0) + 1
        
        if (result.hasErrors) {
            errorCounts[validatorName] = errorCounts.getOrDefault(validatorName, 0) + result.errors.size
        }
        
        if (result.hasWarnings) {
            warningCounts[validatorName] = warningCounts.getOrDefault(validatorName, 0) + result.warnings.size
        }
    }
    
    /** 获取验证统计 */
    fun getValidationStats(): Map<String, Int> = validationCounts.toMap()
    
    /** 获取错误统计 */
    fun getErrorStats(): Map<String, Int> = errorCounts.toMap()
    
    /** 获取警告统计 */
    fun getWarningStats(): Map<String, Int> = warningCounts.toMap()
    
    /** 获取总验证次数 */
    fun getTotalValidations(): Int = validationCounts.values.sum()
    
    /** 获取总错误数 */
    fun getTotalErrors(): Int = errorCounts.values.sum()
    
    /** 获取总警告数 */
    fun getTotalWarnings(): Int = warningCounts.values.sum()
}

/**
 * 抽象 AST 验证器基类
 */
abstract class BaseASTValidator : ASTValidator {
    
    /**
     * 递归验证子节点
     */
    protected fun validateChildren(node: ASTNode, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        when (node) {
            is Program -> {
                context.enterScope()
                node.imports.forEach { result = result.merge(validate(it, context)) }
                node.exports.forEach { result = result.merge(validate(it, context)) }
                node.types.forEach { result = result.merge(validate(it, context)) }
                node.constants.forEach { result = result.merge(validate(it, context)) }
                node.functions.forEach { result = result.merge(validate(it, context)) }
                node.macros.forEach { result = result.merge(validate(it, context)) }
                node.dsls.forEach { result = result.merge(validate(it, context)) }
                context.exitScope()
            }
            
            is ImportDeclaration -> { /* handled by specific validators */ }
            is ExportDeclaration -> { /* handled by specific validators */ }
            is TypeDeclaration -> { /* handled by specific validators */ }
            is ConstantDeclaration -> { 
                node.value?.let { result = result.merge(validate(it, context)) }
            }
            is FunctionDeclaration -> {
                context.enterScope()
                context.symbolTable[node.name] = node
                node.body.forEach { result = result.merge(validate(it, context)) }
                context.exitScope()
            }
            is MacroDeclaration -> { /* handled by specific validators */ }
            is DSLDeclaration -> { /* handled by specific validators */ }
            is IdentifierExpression -> { /* handled by specific validators */ }
            
            is FunctionDeclaration -> {
                context.enterScope()
                context.symbolTable[node.name] = node
                node.body.forEach { result = result.merge(validate(it, context)) }
                context.exitScope()
            }
            
            is BlockStatement -> {
                context.enterScope()
                node.statements.forEach { result = result.merge(validate(it, context)) }
                context.exitScope()
            }
            
            is BinaryExpression -> {
                result = result.merge(validate(node.left, context))
                result = result.merge(validate(node.right, context))
            }
            
            is UnaryExpression -> {
                result = result.merge(validate(node.operand, context))
            }
            
            is CallExpression -> {
                result = result.merge(validate(node.function, context))
                node.arguments.forEach { result = result.merge(validate(it.value, context)) }
            }
            
            is ConditionalStatement -> {
                result = result.merge(validate(node.condition, context))
                result = result.merge(validate(node.thenBranch, context))
                node.elseBranch?.let { result = result.merge(validate(it, context)) }
            }
            
            is LoopStatement.ForLoop -> {
                result = result.merge(validate(node.iterable, context))
                result = result.merge(validate(node.body, context))
            }
            
            is LoopStatement.WhileLoop -> {
                result = result.merge(validate(node.condition, context))
                result = result.merge(validate(node.body, context))
            }
            
            is TryCatchStatement -> {
                result = result.merge(validate(node.tryBlock, context))
                node.catchClauses.forEach { result = result.merge(validate(it.handler, context)) }
                node.finallyBlock?.let { result = result.merge(validate(it, context)) }
            }
            
            is ArrayExpression -> {
                node.elements.forEach { result = result.merge(validate(it, context)) }
            }
            
            is ObjectExpression -> {
                node.properties.forEach { prop ->
                    result = result.merge(validate(prop.key, context))
                    result = result.merge(validate(prop.value, context))
                }
            }
            
            is LambdaExpression -> {
                result = result.merge(validate(node.body, context))
            }
            
            is ConditionalExpression -> {
                result = result.merge(validate(node.condition, context))
                result = result.merge(validate(node.thenExpression, context))
                result = result.merge(validate(node.elseExpression, context))
            }
            
            is CastExpression -> {
                result = result.merge(validate(node.expression, context))
            }
            
            is TypeCheckExpression -> {
                result = result.merge(validate(node.expression, context))
            }
            
            is MemberExpression -> {
                result = result.merge(validate(node.target, context))
            }
            
            is VariableDeclaration -> {
                context.symbolTable[node.name] = node
                node.value?.let { result = result.merge(validate(it, context)) }
            }
            
            is ExpressionStatement -> {
                result = result.merge(validate(node.expression, context))
            }
            
            is JumpStatement.ReturnStatement -> {
                node.value?.let { result = result.merge(validate(it, context)) }
            }
            
            is MatchStatement -> {
                result = result.merge(validate(node.expression, context))
                node.cases.forEach { case ->
                    case.guard?.let { result = result.merge(validate(it, context)) }
                    result = result.merge(validate(case.body, context))
                }
                node.defaultCase?.let { result = result.merge(validate(it, context)) }
            }
            
            // 添加所有其他节点类型的处理
            else -> {
                // 对于其他节点类型，使用默认处理
            }
        }
        
        return result
    }
    
    /**
     * 创建验证错误
     */
    protected fun error(
        code: String, 
        message: String, 
        location: cn.llonvne.jsonlang.core.source.SourceLocation
    ): ValidationResult {
        return ValidationResult.failure(ValidationError(code, message, location))
    }
    
    /**
     * 创建验证警告
     */
    protected fun warning(
        code: String, 
        message: String, 
        location: cn.llonvne.jsonlang.core.source.SourceLocation
    ): ValidationResult {
        return ValidationResult.warning(ValidationWarning(code, message, location))
    }
}
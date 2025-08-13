package cn.llonvne.jsonlang.core.errors

import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * JSONLang 错误报告系统
 * 提供统一的错误处理和报告机制，支持中文错误消息
 */

/**
 * 错误严重程度枚举
 */
enum class ErrorSeverity(val displayName: String) {
    /** 信息 */
    INFO("信息"),
    /** 警告 */
    WARNING("警告"),
    /** 错误 */
    ERROR("错误"),
    /** 致命错误 */
    FATAL("致命错误")
}

/**
 * 错误类别枚举
 */
enum class ErrorCategory(val code: String, val displayName: String) {
    /** 语法错误 */
    SYNTAX("SYNTAX", "语法错误"),
    /** 类型错误 */
    TYPE("TYPE", "类型错误"),
    /** 语义错误 */
    SEMANTIC("SEMANTIC", "语义错误"),
    /** 导入错误 */
    IMPORT("IMPORT", "导入错误"),
    /** 宏错误 */
    MACRO("MACRO", "宏错误"),
    /** DSL错误 */
    DSL("DSL", "DSL错误"),
    /** 代码生成错误 */
    CODEGEN("CODEGEN", "代码生成错误"),
    /** 运行时错误 */
    RUNTIME("RUNTIME", "运行时错误"),
    /** 内部错误 */
    INTERNAL("INTERNAL", "内部错误")
}

/**
 * 编译错误
 */
data class CompilerError(
    /** 错误代码 */
    val code: String,
    /** 错误消息 */
    val message: String,
    /** 错误严重程度 */
    val severity: ErrorSeverity,
    /** 错误类别 */
    val category: ErrorCategory,
    /** 源码位置 */
    val location: SourceLocation,
    /** 错误原因（可选） */
    val cause: Throwable? = null,
    /** 相关位置（可选） */
    val relatedLocations: List<SourceLocation> = emptyList(),
    /** 修复建议（可选） */
    val suggestions: List<String> = emptyList(),
    /** 帮助信息（可选） */
    val help: String? = null
) {
    /**
     * 格式化错误消息
     */
    fun format(): String {
        val builder = StringBuilder()
        
        // 错误头部
        builder.append("${severity.displayName}: ${category.displayName} [$code]\n")
        
        // 位置信息
        builder.append("  位置: $location\n")
        
        // 错误消息
        builder.append("  消息: $message\n")
        
        // 相关位置
        if (relatedLocations.isNotEmpty()) {
            builder.append("  相关位置:\n")
            relatedLocations.forEach { loc ->
                builder.append("    - $loc\n")
            }
        }
        
        // 修复建议
        if (suggestions.isNotEmpty()) {
            builder.append("  建议:\n")
            suggestions.forEach { suggestion ->
                builder.append("    - $suggestion\n")
            }
        }
        
        // 帮助信息
        if (help != null) {
            builder.append("  帮助: $help\n")
        }
        
        return builder.toString()
    }
    
    /**
     * 检查是否为致命错误
     */
    fun isFatal(): Boolean = severity == ErrorSeverity.FATAL || severity == ErrorSeverity.ERROR
    
    /**
     * 检查是否为警告
     */
    fun isWarning(): Boolean = severity == ErrorSeverity.WARNING
}

/**
 * 错误报告器接口
 */
interface ErrorReporter {
    /** 报告错误 */
    fun report(error: CompilerError)
    
    /** 报告多个错误 */
    fun reportAll(errors: List<CompilerError>) {
        errors.forEach { report(it) }
    }
    
    /** 获取所有错误 */
    fun getErrors(): List<CompilerError>
    
    /** 获取错误数量 */
    fun getErrorCount(): Int
    
    /** 获取警告数量 */
    fun getWarningCount(): Int
    
    /** 检查是否有错误 */
    fun hasErrors(): Boolean
    
    /** 检查是否有警告 */
    fun hasWarnings(): Boolean
    
    /** 清除所有错误 */
    fun clear()
}

/**
 * 默认错误报告器实现
 */
class DefaultErrorReporter : ErrorReporter {
    private val errors = mutableListOf<CompilerError>()
    
    override fun report(error: CompilerError) {
        errors.add(error)
        
        // 立即输出错误信息
        when (error.severity) {
            ErrorSeverity.INFO -> println(error.format())
            ErrorSeverity.WARNING -> System.err.println(error.format())
            ErrorSeverity.ERROR, ErrorSeverity.FATAL -> System.err.println(error.format())
        }
    }
    
    override fun getErrors(): List<CompilerError> = errors.toList()
    
    override fun getErrorCount(): Int = errors.count { it.isFatal() }
    
    override fun getWarningCount(): Int = errors.count { it.isWarning() }
    
    override fun hasErrors(): Boolean = errors.any { it.isFatal() }
    
    override fun hasWarnings(): Boolean = errors.any { it.isWarning() }
    
    override fun clear() {
        errors.clear()
    }
}

/**
 * 错误构建器
 * 提供流式API来构建错误对象
 */
class ErrorBuilder(
    private val code: String,
    private val message: String,
    private val location: SourceLocation
) {
    private var severity: ErrorSeverity = ErrorSeverity.ERROR
    private var category: ErrorCategory = ErrorCategory.SEMANTIC
    private var cause: Throwable? = null
    private var relatedLocations: MutableList<SourceLocation> = mutableListOf()
    private var suggestions: MutableList<String> = mutableListOf()
    private var help: String? = null
    
    fun severity(severity: ErrorSeverity): ErrorBuilder {
        this.severity = severity
        return this
    }
    
    fun category(category: ErrorCategory): ErrorBuilder {
        this.category = category
        return this
    }
    
    fun cause(cause: Throwable): ErrorBuilder {
        this.cause = cause
        return this
    }
    
    fun relatedLocation(location: SourceLocation): ErrorBuilder {
        this.relatedLocations.add(location)
        return this
    }
    
    fun suggestion(suggestion: String): ErrorBuilder {
        this.suggestions.add(suggestion)
        return this
    }
    
    fun help(help: String): ErrorBuilder {
        this.help = help
        return this
    }
    
    fun build(): CompilerError {
        return CompilerError(
            code = code,
            message = message,
            severity = severity,
            category = category,
            location = location,
            cause = cause,
            relatedLocations = relatedLocations.toList(),
            suggestions = suggestions.toList(),
            help = help
        )
    }
}

/**
 * 预定义错误代码和消息
 */
object ErrorCodes {
    // 语法错误
    const val SYNTAX_UNEXPECTED_TOKEN = "SYNTAX_001"
    const val SYNTAX_MISSING_TOKEN = "SYNTAX_002"
    const val SYNTAX_INVALID_JSON = "SYNTAX_003"
    const val SYNTAX_INVALID_STRUCTURE = "SYNTAX_004"
    
    // 类型错误
    const val TYPE_MISMATCH = "TYPE_001"
    const val TYPE_NOT_FOUND = "TYPE_002"
    const val TYPE_INCOMPATIBLE = "TYPE_003"
    const val TYPE_INFERENCE_FAILED = "TYPE_004"
    
    // 语义错误
    const val SEMANTIC_UNDEFINED_SYMBOL = "SEMANTIC_001"
    const val SEMANTIC_DUPLICATE_SYMBOL = "SEMANTIC_002"
    const val SEMANTIC_INVALID_OPERATION = "SEMANTIC_003"
    const val SEMANTIC_UNREACHABLE_CODE = "SEMANTIC_004"
    
    // 导入错误
    const val IMPORT_NOT_FOUND = "IMPORT_001"
    const val IMPORT_CIRCULAR_DEPENDENCY = "IMPORT_002"
    const val IMPORT_VERSION_CONFLICT = "IMPORT_003"
    const val IMPORT_INVALID_PATH = "IMPORT_004"
    
    // 宏错误
    const val MACRO_EXPANSION_FAILED = "MACRO_001"
    const val MACRO_INVALID_SYNTAX = "MACRO_002"
    const val MACRO_RECURSIVE_EXPANSION = "MACRO_003"
    
    // DSL错误
    const val DSL_INVALID_SYNTAX = "DSL_001"
    const val DSL_TRANSFORMATION_FAILED = "DSL_002"
    const val DSL_UNDEFINED_RULE = "DSL_003"
    
    // 代码生成错误
    const val CODEGEN_UNSUPPORTED_FEATURE = "CODEGEN_001"
    const val CODEGEN_BACKEND_ERROR = "CODEGEN_002"
    const val CODEGEN_OPTIMIZATION_FAILED = "CODEGEN_003"
    
    // 运行时错误
    const val RUNTIME_NULL_POINTER = "RUNTIME_001"
    const val RUNTIME_INDEX_OUT_OF_BOUNDS = "RUNTIME_002"
    const val RUNTIME_DIVISION_BY_ZERO = "RUNTIME_003"
    
    // 内部错误
    const val INTERNAL_COMPILER_BUG = "INTERNAL_001"
    const val INTERNAL_ASSERTION_FAILED = "INTERNAL_002"
}

/**
 * 错误消息模板
 */
object ErrorMessages {
    // 语法错误消息
    fun unexpectedToken(expected: String, actual: String): String =
        "期望 '$expected'，但遇到 '$actual'"
    
    fun missingToken(token: String): String =
        "缺少必需的标记 '$token'"
    
    fun invalidJson(reason: String): String =
        "无效的 JSON 格式: $reason"
    
    fun invalidStructure(expected: String): String =
        "无效的 JSONLang 结构，期望 $expected"
    
    // 类型错误消息
    fun typeMismatch(expected: String, actual: String): String =
        "类型不匹配：期望 '$expected'，实际 '$actual'"
    
    fun typeNotFound(typeName: String): String =
        "未找到类型 '$typeName'"
    
    fun typeIncompatible(type1: String, type2: String, operation: String): String =
        "类型 '$type1' 和 '$type2' 不兼容，无法执行操作 '$operation'"
    
    fun typeInferenceFailed(expression: String): String =
        "无法推导表达式 '$expression' 的类型"
    
    // 语义错误消息
    fun undefinedSymbol(symbol: String): String =
        "未定义的符号 '$symbol'"
    
    fun duplicateSymbol(symbol: String): String =
        "重复定义的符号 '$symbol'"
    
    fun invalidOperation(operation: String, context: String): String =
        "在 $context 中无效的操作 '$operation'"
    
    fun unreachableCode(): String =
        "不可达的代码"
    
    // 导入错误消息
    fun importNotFound(module: String): String =
        "无法找到模块 '$module'"
    
    fun circularDependency(modules: List<String>): String =
        "检测到循环依赖: ${modules.joinToString(" -> ")}"
    
    fun versionConflict(module: String, required: String, found: String): String =
        "模块 '$module' 版本冲突：需要 '$required'，找到 '$found'"
    
    fun invalidPath(path: String): String =
        "无效的导入路径 '$path'"
}

/**
 * 错误恢复策略
 */
interface ErrorRecoveryStrategy {
    /** 尝试从错误中恢复 */
    fun recover(error: CompilerError, context: Any): Boolean
}

/**
 * 默认错误恢复策略
 */
class DefaultErrorRecoveryStrategy : ErrorRecoveryStrategy {
    override fun recover(error: CompilerError, context: Any): Boolean {
        // 简单的恢复策略：对于非致命错误，尝试继续处理
        return !error.isFatal()
    }
}
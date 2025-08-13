package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.Type

/**
 * 后端代码生成器抽象接口
 * 定义了将 JSONLang AST 转换为目标语言代码的通用接口
 */
interface Backend {
    /**
     * 获取后端名称
     */
    fun getName(): String
    
    /**
     * 获取目标语言名称
     */
    fun getTargetLanguage(): String
    
    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String
    
    /**
     * 生成完整程序代码
     */
    fun generateProgram(program: Program): BackendResult
    
    /**
     * 生成表达式代码
     */
    fun generateExpression(expression: Expression): String
    
    /**
     * 生成语句代码
     */
    fun generateStatement(statement: Statement): String
    
    /**
     * 生成声明代码
     */
    fun generateDeclaration(declaration: Declaration): String
    
    /**
     * 生成类型代码
     */
    fun generateType(type: Type): String
    
    /**
     * 获取类型映射
     */
    fun getTypeMapping(): TypeMapping
    
    /**
     * 获取后端特定的优化选项
     */
    fun getOptimizations(): List<BackendOptimization>
    
    /**
     * 验证 AST 是否与后端兼容
     */
    fun validateCompatibility(program: Program): List<CompatibilityIssue>
}

/**
 * 后端生成结果
 */
data class BackendResult(
    val code: String,
    val imports: List<String> = emptyList(),
    val exports: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = errors.isEmpty()
    
    fun withWarning(warning: String): BackendResult {
        return copy(warnings = warnings + warning)
    }
    
    fun withError(error: String): BackendResult {
        return copy(errors = errors + error)
    }
    
    fun withMetadata(key: String, value: Any): BackendResult {
        return copy(metadata = metadata + (key to value))
    }
}

/**
 * 类型映射接口
 * 定义 JSONLang 类型到目标语言类型的映射
 */
interface TypeMapping {
    /**
     * 映射基础类型
     */
    fun mapPrimitiveType(type: Type): String
    
    /**
     * 映射复合类型
     */
    fun mapCompositeType(type: Type): String
    
    /**
     * 映射用户定义类型
     */
    fun mapUserDefinedType(type: Type): String
    
    /**
     * 映射泛型类型
     */
    fun mapGenericType(type: Type): String
    
    /**
     * 映射可空类型
     */
    fun mapNullableType(type: Type): String
    
    /**
     * 获取默认值表示
     */
    fun getDefaultValue(type: Type): String
    
    /**
     * 检查类型是否需要导入
     */
    fun requiresImport(type: Type): String?
}

/**
 * 后端优化
 */
sealed class BackendOptimization {
    abstract val name: String
    abstract val description: String
    abstract val enabled: Boolean
    
    /**
     * 内联优化
     */
    data class InlineOptimization(
        override val enabled: Boolean = true
    ) : BackendOptimization() {
        override val name: String = "inline"
        override val description: String = "内联小函数以提高性能"
    }
    
    /**
     * 尾调用优化
     */
    data class TailCallOptimization(
        override val enabled: Boolean = true
    ) : BackendOptimization() {
        override val name: String = "tail-call"
        override val description: String = "优化尾递归调用"
    }
    
    /**
     * 死代码消除
     */
    data class DeadCodeElimination(
        override val enabled: Boolean = true
    ) : BackendOptimization() {
        override val name: String = "dead-code"
        override val description: String = "移除未使用的代码"
    }
    
    /**
     * 常量折叠
     */
    data class ConstantFolding(
        override val enabled: Boolean = true
    ) : BackendOptimization() {
        override val name: String = "constant-folding"
        override val description: String = "在编译时计算常量表达式"
    }
}

/**
 * 兼容性问题
 */
data class CompatibilityIssue(
    val severity: Severity,
    val message: String,
    val location: cn.llonvne.jsonlang.core.source.SourceLocation? = null,
    val suggestion: String? = null
) {
    enum class Severity {
        WARNING, ERROR
    }
}

/**
 * 抽象后端基类
 * 提供通用的代码生成功能
 */
abstract class AbstractBackend : Backend {
    protected var indentLevel = 0
    protected val indentSize = 4
    
    /**
     * 生成缩进
     */
    protected fun indent(): String = " ".repeat(indentLevel * indentSize)
    
    /**
     * 增加缩进级别
     */
    protected fun increaseIndent() {
        indentLevel++
    }
    
    /**
     * 减少缩进级别
     */
    protected fun decreaseIndent() {
        if (indentLevel > 0) {
            indentLevel--
        }
    }
    
    /**
     * 在指定缩进级别下执行代码块
     */
    protected inline fun withIndent(block: () -> String): String {
        increaseIndent()
        val result = block()
        decreaseIndent()
        return result
    }
    
    /**
     * 生成代码块
     */
    protected fun generateBlock(statements: List<Statement>): String {
        return statements.joinToString("\n") { "${indent()}${generateStatement(it)}" }
    }
    
    /**
     * 生成参数列表
     */
    protected fun generateParameterList(parameters: List<Parameter>): String {
        return parameters.joinToString(", ") { param ->
            generateParameter(param)
        }
    }
    
    /**
     * 生成单个参数
     */
    protected abstract fun generateParameter(parameter: Parameter): String
    
    /**
     * 生成函数修饰符
     */
    protected abstract fun generateModifiers(modifiers: FunctionModifiers): String
    
    /**
     * 生成导入语句
     */
    protected abstract fun generateImport(import: ImportDeclaration): String
    
    /**
     * 生成导出语句
     */
    protected abstract fun generateExport(export: ExportDeclaration): String
    
    /**
     * 默认兼容性验证实现
     */
    override fun validateCompatibility(program: Program): List<CompatibilityIssue> {
        val issues = mutableListOf<CompatibilityIssue>()
        
        // 检查不支持的特性
        program.functions.forEach { function ->
            if (function.modifiers.async && !supportsAsync()) {
                issues.add(
                    CompatibilityIssue(
                        CompatibilityIssue.Severity.ERROR,
                        "后端 ${getName()} 不支持异步函数",
                        function.location,
                        "移除 async 修饰符或使用支持异步的后端"
                    )
                )
            }
        }
        
        return issues
    }
    
    /**
     * 检查是否支持异步
     */
    protected open fun supportsAsync(): Boolean = false
    
    /**
     * 检查是否支持泛型
     */
    protected open fun supportsGenerics(): Boolean = true
    
    /**
     * 检查是否支持宏
     */
    protected open fun supportsMacros(): Boolean = false
}

/**
 * 后端注册表
 */
object BackendRegistry {
    private val backends = mutableMapOf<String, () -> Backend>()
    
    /**
     * 注册后端
     */
    fun register(name: String, factory: () -> Backend) {
        backends[name] = factory
    }
    
    /**
     * 创建后端实例
     */
    fun create(name: String): Backend? {
        return backends[name]?.invoke()
    }
    
    /**
     * 获取所有可用的后端名称
     */
    fun getAvailableBackends(): List<String> {
        return backends.keys.toList()
    }
    
    /**
     * 检查后端是否存在
     */
    fun exists(name: String): Boolean {
        return backends.containsKey(name)
    }
}
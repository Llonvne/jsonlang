package cn.llonvne.jsonlang.core.macros

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import kotlinx.serialization.Serializable

/**
 * 宏系统核心类
 * 负责宏的定义、注册、展开和管理
 */
class MacroSystem(
    private val errorReporter: ErrorReporter
) {
    private val macros = mutableMapOf<String, MacroDefinition>()
    private val expansionStack = mutableListOf<String>()
    private val hygieneContext = MacroHygieneContext()
    
    /**
     * 注册宏定义
     */
    fun registerMacro(macro: MacroDeclaration) {
        val definition = MacroDefinition(
            name = macro.name,
            parameters = macro.parameters,
            expansion = macro.expansion,
            phase = macro.phase,
            location = macro.location
        )
        macros[macro.name] = definition
    }
    
    /**
     * 展开宏调用
     */
    fun expandMacro(call: MacroCall, context: MacroExpansionContext): ASTNode? {
        val macro = macros[call.name] ?: run {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "MACRO_NOT_FOUND",
                    message = "未找到宏定义: ${call.name}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.MACRO,
                    location = call.location
                )
            )
            return null
        }
        
        // 检查循环展开
        if (call.name in expansionStack) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "MACRO_RECURSIVE_EXPANSION",
                    message = "宏递归展开: ${call.name}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.MACRO,
                    location = call.location
                )
            )
            return null
        }
        
        // 验证参数数量
        if (call.arguments.size != macro.parameters.size) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "MACRO_ARGUMENT_MISMATCH",
                    message = "宏参数数量不匹配: 期望 ${macro.parameters.size}，实际 ${call.arguments.size}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.MACRO,
                    location = call.location
                )
            )
            return null
        }
        
        try {
            expansionStack.add(call.name)
            
            // 创建参数绑定
            val bindings = macro.parameters.zip(call.arguments).toMap()
            
            // 执行宏展开
            val expanded = when (macro.expansion) {
                is MacroExpansion.TemplateExpansion -> {
                    expandTemplate(macro.expansion.template, bindings, context)
                }
                is MacroExpansion.FunctionExpansion -> {
                    expandFunction(macro.expansion.function, bindings, context)
                }
            }
            
            // 应用宏卫生性
            return applyHygiene(expanded, context)
            
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Recursive macro expansion") == true) {
                return null
            }
            throw e
        } finally {
            expansionStack.removeLastOrNull()
        }
    }
    
    /**
     * 模板展开
     */
    private fun expandTemplate(
        template: ASTNode,
        bindings: Map<String, ASTNode>,
        context: MacroExpansionContext
    ): ASTNode {
        val expander = MacroTemplateExpander(bindings, context)
        val expanded = expander.expand(template)
        
        // 检查展开结果中是否包含递归宏调用
        if (expanded is CallExpression && expanded.function is IdentifierExpression) {
            val functionName = (expanded.function as IdentifierExpression).name
            if (functionName in expansionStack) {
                errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = "MACRO_RECURSIVE_EXPANSION",
                        message = "宏递归展开: $functionName",
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.MACRO,
                        location = expanded.location
                    )
                )
                throw IllegalStateException("Recursive macro expansion detected")
            }
        }
        
        return expanded
    }
    
    /**
     * 函数展开
     */
    private fun expandFunction(
        functionName: String,
        bindings: Map<String, ASTNode>,
        context: MacroExpansionContext
    ): ASTNode {
        // 调用注册的宏展开函数
        val expander = context.getExpansionFunction(functionName)
            ?: throw IllegalArgumentException("未找到宏展开函数: $functionName")
        
        return expander.invoke(bindings, context)
    }
    
    /**
     * 应用宏卫生性
     */
    private fun applyHygiene(node: ASTNode, context: MacroExpansionContext): ASTNode {
        return hygieneContext.applyScopeRules(node, context)
    }
    
    /**
     * 检查宏是否存在
     */
    fun hasMacro(name: String): Boolean = macros.containsKey(name)
    
    /**
     * 获取所有已注册的宏
     */
    fun getAllMacros(): Map<String, MacroDefinition> = macros.toMap()
}

/**
 * 宏定义数据类
 */
@Serializable
data class MacroDefinition(
    val name: String,
    val parameters: List<String>,
    val expansion: MacroExpansion,
    val phase: MacroPhase,
    val location: SourceLocation
)



/**
 * 宏展开上下文
 */
data class MacroExpansionContext(
    val currentScope: String,
    val expansionDepth: Int = 0,
    val expansionFunctions: Map<String, MacroExpansionFunction> = emptyMap(),
    val variables: Map<String, ASTNode> = emptyMap()
) {
    fun getExpansionFunction(name: String): MacroExpansionFunction? = expansionFunctions[name]
    
    fun withIncrementedDepth(): MacroExpansionContext = copy(expansionDepth = expansionDepth + 1)
    
    fun withScope(scope: String): MacroExpansionContext = copy(currentScope = scope)
    
    fun withVariable(name: String, value: ASTNode): MacroExpansionContext = 
        copy(variables = variables + (name to value))
}

/**
 * 宏展开函数类型
 */
typealias MacroExpansionFunction = (Map<String, ASTNode>, MacroExpansionContext) -> ASTNode
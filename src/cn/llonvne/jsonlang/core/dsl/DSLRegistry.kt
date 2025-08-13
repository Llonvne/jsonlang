package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.DSLDeclaration
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * DSL 注册表
 * 管理所有已注册的 DSL 定义和相关操作
 */
class DSLRegistry(
    private val errorReporter: ErrorReporter
) {
    private val registeredDSLs = mutableMapOf<String, DSLDeclaration>()
    private val dslEngines = mutableMapOf<String, DSLEngine>()
    private val dslValidators = mutableMapOf<String, DSLValidator>()
    
    /**
     * 注册 DSL
     */
    fun registerDSL(declaration: DSLDeclaration): Boolean {
        // 验证 DSL 定义
        val validator = DSLValidator()
        val errors = validator.validate(declaration)
        
        if (errors.isNotEmpty()) {
            errors.forEach { error ->
                errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = "DSL_VALIDATION_ERROR",
                        message = "DSL 验证错误: ${error.message}",
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.DSL,
                        location = declaration.location
                    )
                )
            }
            return false
        }
        
        // 检查是否已存在同名 DSL
        if (registeredDSLs.containsKey(declaration.name)) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "DSL_ALREADY_EXISTS",
                    message = "DSL 已存在: ${declaration.name}",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.DSL,
                    location = declaration.location
                )
            )
        }
        
        // 注册 DSL
        registeredDSLs[declaration.name] = declaration
        
        // 创建 DSL 引擎
        val engine = DSLEngine(errorReporter)
        engine.registerDSL(declaration)
        dslEngines[declaration.name] = engine
        
        // 创建 DSL 验证器
        dslValidators[declaration.name] = DSLValidator()
        
        return true
    }
    
    /**
     * 注销 DSL
     */
    fun unregisterDSL(name: String): Boolean {
        val removed = registeredDSLs.remove(name) != null
        dslEngines.remove(name)
        dslValidators.remove(name)
        return removed
    }
    
    /**
     * 获取 DSL 声明
     */
    fun getDSL(name: String): DSLDeclaration? = registeredDSLs[name]
    
    /**
     * 获取 DSL 引擎
     */
    fun getDSLEngine(name: String): DSLEngine? = dslEngines[name]
    
    /**
     * 检查 DSL 是否已注册
     */
    fun isDSLRegistered(name: String): Boolean = registeredDSLs.containsKey(name)
    
    /**
     * 获取所有已注册的 DSL 名称
     */
    fun getRegisteredDSLNames(): Set<String> = registeredDSLs.keys.toSet()
    
    /**
     * 获取所有已注册的 DSL
     */
    fun getAllDSLs(): Map<String, DSLDeclaration> = registeredDSLs.toMap()
    
    /**
     * 解析 DSL 输入
     */
    fun parseDSL(dslName: String, input: String, location: SourceLocation): DSLNode? {
        val engine = dslEngines[dslName] ?: run {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "DSL_ENGINE_NOT_FOUND",
                    message = "未找到 DSL 引擎: $dslName",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.ERROR,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.DSL,
                    location = location
                )
            )
            return null
        }
        
        return engine.parseDSLSyntax(dslName, input, location)
    }
    
    /**
     * 转换 DSL 节点
     */
    fun transformDSL(dslNode: DSLNode): cn.llonvne.jsonlang.core.ast.ASTNode? {
        val engine = dslEngines[dslNode.dslName] ?: return null
        return engine.transformDSLToAST(dslNode)
    }
    
    /**
     * 验证 DSL 输入
     */
    fun validateDSLInput(dslName: String, input: String): List<DSLValidationError> {
        val validator = dslValidators[dslName] ?: return emptyList()
        return validator.validateInput(input)
    }
    
    /**
     * 批量注册 DSL
     */
    fun registerDSLs(declarations: List<DSLDeclaration>): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
        for (declaration in declarations) {
            results[declaration.name] = registerDSL(declaration)
        }
        
        return results
    }
    

    
    /**
     * 导出 DSL 定义
     */
    fun exportDSLs(): List<DSLDeclaration> = registeredDSLs.values.toList()
    
    /**
     * 导入 DSL 定义
     */
    fun importDSLs(declarations: List<DSLDeclaration>): ImportResult {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()
        
        for (declaration in declarations) {
            if (registerDSL(declaration)) {
                successful.add(declaration.name)
            } else {
                failed.add(declaration.name to "注册失败")
            }
        }
        
        return ImportResult(successful, failed)
    }
    
    /**
     * 清空所有 DSL
     */
    fun clear() {
        registeredDSLs.clear()
        dslEngines.clear()
        dslValidators.clear()
    }
    
    /**
     * 获取 DSL 统计信息
     */
    fun getStatistics(): DSLStatistics {
        val totalDSLs = registeredDSLs.size
        val totalPatterns = registeredDSLs.values.sumOf { it.syntax.patterns.size }
        val totalSemanticRules = registeredDSLs.values.sumOf { it.semantics.rules.size }
        
        val dslsByTarget = registeredDSLs.values.groupBy { it.transformation.target }
            .mapValues { it.value.size }
        
        return DSLStatistics(
            totalDSLs = totalDSLs,
            totalPatterns = totalPatterns,
            totalSemanticRules = totalSemanticRules,
            dslsByTarget = dslsByTarget
        )
    }
}

/**
 * DSL 验证器
 */
class DSLValidator {
    /**
     * 验证 DSL 声明
     */
    fun validate(declaration: DSLDeclaration): List<DSLValidationError> {
        val errors = mutableListOf<DSLValidationError>()
        
        // 验证名称
        if (declaration.name.isBlank()) {
            errors.add(DSLValidationError("DSL 名称不能为空"))
        }
        
        if (!isValidDSLName(declaration.name)) {
            errors.add(DSLValidationError("DSL 名称格式无效: ${declaration.name}"))
        }
        
        // 验证语法
        val syntaxValidator = DSLSyntaxValidator()
        errors.addAll(syntaxValidator.validate(declaration.syntax).map { 
            DSLValidationError("语法错误: ${it.message}") 
        })
        
        // 验证语义
        errors.addAll(validateSemantics(declaration.semantics))
        
        // 验证转换
        val transformationValidator = DSLTransformationValidator()
        errors.addAll(transformationValidator.validate(declaration.transformation).map { 
            DSLValidationError("转换错误: ${it.message}") 
        })
        
        // 验证一致性
        errors.addAll(validateConsistency(declaration))
        
        return errors
    }
    
    /**
     * 验证 DSL 输入
     */
    fun validateInput(input: String): List<DSLValidationError> {
        val errors = mutableListOf<DSLValidationError>()
        
        if (input.isBlank()) {
            errors.add(DSLValidationError("DSL 输入不能为空"))
        }
        
        // 这里可以添加更多的输入验证逻辑
        
        return errors
    }
    
    /**
     * 验证 DSL 名称格式
     */
    private fun isValidDSLName(name: String): Boolean {
        return name.matches(Regex("^[A-Za-z][A-Za-z0-9_]*$"))
    }
    
    /**
     * 验证语义定义
     */
    private fun validateSemantics(semantics: cn.llonvne.jsonlang.core.ast.DSLSemantics): List<DSLValidationError> {
        val errors = mutableListOf<DSLValidationError>()
        
        for ((ruleName, rule) in semantics.rules) {
            if (rule.type.isBlank()) {
                errors.add(DSLValidationError("语义规则 '$ruleName' 的类型不能为空"))
            }
            
            if (rule.mapping.isEmpty()) {
                errors.add(DSLValidationError("语义规则 '$ruleName' 的映射不能为空"))
            }
        }
        
        return errors
    }
    
    /**
     * 验证一致性
     */
    private fun validateConsistency(declaration: DSLDeclaration): List<DSLValidationError> {
        val errors = mutableListOf<DSLValidationError>()
        
        // 检查语法模式和语义规则的一致性
        val syntaxPatterns = declaration.syntax.patterns.keys
        val semanticRules = declaration.semantics.rules.keys
        
        for (pattern in syntaxPatterns) {
            if (pattern !in semanticRules) {
                errors.add(DSLValidationError("语法模式 '$pattern' 没有对应的语义规则"))
            }
        }
        
        for (rule in semanticRules) {
            if (rule !in syntaxPatterns) {
                errors.add(DSLValidationError("语义规则 '$rule' 没有对应的语法模式"))
            }
        }
        
        return errors
    }
}

/**
 * DSL 验证错误
 */
data class DSLValidationError(val message: String)

/**
 * 导入结果
 */
data class ImportResult(
    val successful: List<String>,
    val failed: List<Pair<String, String>>
)

/**
 * DSL 统计信息
 */
data class DSLStatistics(
    val totalDSLs: Int,
    val totalPatterns: Int,
    val totalSemanticRules: Int,
    val dslsByTarget: Map<String, Int>
)
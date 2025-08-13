package cn.llonvne.jsonlang.core.macros

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import java.util.*

/**
 * 宏卫生性上下文
 * 负责处理宏展开中的作用域和名称冲突问题
 */
class MacroHygieneContext {
    private val scopeStack = mutableListOf<HygieneScope>()
    private val renamedIdentifiers = mutableMapOf<String, String>()
    private var uniqueCounter = 0
    
    /**
     * 应用作用域规则到展开的 AST 节点
     */
    fun applyScopeRules(node: ASTNode, context: MacroExpansionContext): ASTNode {
        val hygieneScope = HygieneScope(context.currentScope, generateUniqueId())
        scopeStack.add(hygieneScope)
        
        try {
            return HygieneScopeApplier(this, hygieneScope).apply(node)
        } finally {
            scopeStack.removeLastOrNull()
        }
    }
    
    /**
     * 生成唯一标识符
     */
    fun generateUniqueIdentifier(baseName: String): String {
        val uniqueId = generateUniqueId()
        val uniqueName = "${baseName}_macro_${uniqueId}"
        renamedIdentifiers[baseName] = uniqueName
        return uniqueName
    }
    
    /**
     * 获取重命名的标识符
     */
    fun getRenamedIdentifier(originalName: String): String? {
        return renamedIdentifiers[originalName]
    }
    
    /**
     * 检查标识符是否需要重命名
     */
    fun shouldRename(identifier: String, scope: HygieneScope): Boolean {
        // 如果标识符在当前宏作用域中定义，则需要重命名以避免冲突
        return scope.definedIdentifiers.contains(identifier) ||
               isReservedIdentifier(identifier)
    }
    
    /**
     * 检查是否是保留标识符
     */
    private fun isReservedIdentifier(identifier: String): Boolean {
        val reservedKeywords = setOf(
            "if", "else", "for", "while", "function", "return", "break", "continue",
            "try", "catch", "finally", "throw", "import", "export", "type", "const",
            "let", "var", "true", "false", "null", "undefined"
        )
        return identifier in reservedKeywords
    }
    
    /**
     * 生成唯一 ID
     */
    fun generateUniqueId(): String {
        return (++uniqueCounter).toString() + "_" + UUID.randomUUID().toString().take(8)
    }
}

/**
 * 卫生性作用域
 */
data class HygieneScope(
    val scopeName: String,
    val uniqueId: String,
    val definedIdentifiers: MutableSet<String> = mutableSetOf(),
    val importedIdentifiers: MutableSet<String> = mutableSetOf()
) {
    
    /**
     * 添加定义的标识符
     */
    fun addDefinedIdentifier(identifier: String) {
        definedIdentifiers.add(identifier)
    }
    
    /**
     * 添加导入的标识符
     */
    fun addImportedIdentifier(identifier: String) {
        importedIdentifiers.add(identifier)
    }
    
    /**
     * 检查标识符是否在当前作用域中
     */
    fun containsIdentifier(identifier: String): Boolean {
        return identifier in definedIdentifiers || identifier in importedIdentifiers
    }
}

/**
 * 卫生性作用域应用器
 * 遍历 AST 并应用卫生性规则
 */
class HygieneScopeApplier(
    private val hygieneContext: MacroHygieneContext,
    private val currentScope: HygieneScope
) : BaseASTVisitor<ASTNode>() {
    
    /**
     * 应用卫生性规则
     */
    fun apply(node: ASTNode): ASTNode {
        return node.accept(this)
    }
    
    override fun defaultVisit(node: ASTNode): ASTNode = node
    
    /**
     * 处理标识符表达式
     */
    override fun visitIdentifierExpression(node: IdentifierExpression): ASTNode {
        val renamedName = if (hygieneContext.shouldRename(node.name, currentScope)) {
            hygieneContext.generateUniqueIdentifier(node.name)
        } else {
            hygieneContext.getRenamedIdentifier(node.name) ?: node.name
        }
        
        return if (renamedName != node.name) {
            node.copy(name = renamedName)
        } else {
            node
        }
    }
    
    /**
     * 处理函数声明
     */
    override fun visitFunctionDeclaration(node: FunctionDeclaration): ASTNode {
        // 记录函数名为已定义标识符
        currentScope.addDefinedIdentifier(node.name)
        
        // 处理参数名称
        val renamedParameters = node.parameters.map { param ->
            val renamedName = if (hygieneContext.shouldRename(param.name, currentScope)) {
                hygieneContext.generateUniqueIdentifier(param.name)
            } else {
                param.name
            }
            currentScope.addDefinedIdentifier(renamedName)
            param.copy(name = renamedName)
        }
        
        // 处理函数体
        val renamedBody = node.body.map { it.accept(this) as Statement }
        
        // 重命名函数名（如果需要）
        val renamedFunctionName = if (hygieneContext.shouldRename(node.name, currentScope)) {
            hygieneContext.generateUniqueIdentifier(node.name)
        } else {
            node.name
        }
        
        return node.copy(
            name = renamedFunctionName,
            parameters = renamedParameters,
            body = renamedBody
        )
    }
    
    /**
     * 处理变量声明
     */
    override fun visitVariableDeclaration(node: VariableDeclaration): ASTNode {
        // 记录变量名为已定义标识符
        currentScope.addDefinedIdentifier(node.name)
        
        val renamedName = if (hygieneContext.shouldRename(node.name, currentScope)) {
            hygieneContext.generateUniqueIdentifier(node.name)
        } else {
            node.name
        }
        
        val renamedInitializer = node.value?.accept(this) as? Expression
        
        return node.copy(
            name = renamedName,
            value = renamedInitializer
        )
    }
    
    /**
     * 处理二元表达式
     */
    override fun visitBinaryExpression(node: BinaryExpression): ASTNode {
        val renamedLeft = node.left.accept(this) as Expression
        val renamedRight = node.right.accept(this) as Expression
        
        return node.copy(
            left = renamedLeft,
            right = renamedRight
        )
    }
    
    /**
     * 处理函数调用表达式
     */
    override fun visitCallExpression(node: CallExpression): ASTNode {
        val renamedFunction = node.function.accept(this) as Expression
        val renamedArguments = node.arguments.map { arg ->
            arg.copy(value = arg.value.accept(this) as Expression)
        }
        
        return node.copy(
            function = renamedFunction,
            arguments = renamedArguments
        )
    }
    
    /**
     * 处理块语句
     */
    override fun visitBlockStatement(node: BlockStatement): ASTNode {
        // 为块语句创建新的作用域
        val blockScope = HygieneScope(
            scopeName = "${currentScope.scopeName}_block",
            uniqueId = hygieneContext.generateUniqueId()
        )
        
        val blockApplier = HygieneScopeApplier(hygieneContext, blockScope)
        val renamedStatements = node.statements.map { it.accept(blockApplier) as Statement }
        
        return node.copy(statements = renamedStatements)
    }
    
    /**
     * 处理 Lambda 表达式
     */
    override fun visitLambdaExpression(node: LambdaExpression): ASTNode {
        // 为 Lambda 创建新的作用域
        val lambdaScope = HygieneScope(
            scopeName = "${currentScope.scopeName}_lambda",
            uniqueId = hygieneContext.generateUniqueId()
        )
        
        val lambdaApplier = HygieneScopeApplier(hygieneContext, lambdaScope)
        
        // 处理参数
        val renamedParameters = node.parameters.map { param ->
            val renamedName = if (hygieneContext.shouldRename(param.name, lambdaScope)) {
                hygieneContext.generateUniqueIdentifier(param.name)
            } else {
                param.name
            }
            lambdaScope.addDefinedIdentifier(renamedName)
            param.copy(name = renamedName)
        }
        
        // 处理函数体
        val renamedBody = node.body.accept(lambdaApplier) as Expression
        
        return node.copy(
            parameters = renamedParameters,
            body = renamedBody
        )
    }
}

/**
 * 宏卫生性验证器
 * 验证宏展开后的代码是否符合卫生性要求
 */
class MacroHygieneValidator {
    
    /**
     * 验证宏展开结果的卫生性
     */
    fun validate(expandedNode: ASTNode, originalContext: MacroExpansionContext): List<HygieneViolation> {
        val violations = mutableListOf<HygieneViolation>()
        val validator = HygieneViolationDetector(violations)
        expandedNode.accept(validator)
        return violations
    }
}

/**
 * 卫生性违规检测器
 */
class HygieneViolationDetector(
    private val violations: MutableList<HygieneViolation>
) : BaseASTVisitor<Unit>() {
    
    private val definedIdentifiers = mutableSetOf<String>()
    private val usedIdentifiers = mutableSetOf<String>()
    
    override fun defaultVisit(node: ASTNode) {}
    
    override fun visitIdentifierExpression(node: IdentifierExpression) {
        usedIdentifiers.add(node.name)
        
        // 检查是否使用了未定义的标识符
        if (node.name !in definedIdentifiers && !isBuiltinIdentifier(node.name)) {
            violations.add(
                HygieneViolation.UndefinedIdentifier(
                    identifier = node.name,
                    location = node.location
                )
            )
        }
    }
    
    override fun visitFunctionDeclaration(node: FunctionDeclaration) {
        // 检查函数名冲突
        if (node.name in definedIdentifiers) {
            violations.add(
                HygieneViolation.IdentifierConflict(
                    identifier = node.name,
                    location = node.location
                )
            )
        }
        
        definedIdentifiers.add(node.name)
        
        // 递归检查函数体
        node.body.forEach { it.accept(this) }
    }
    
    private fun isBuiltinIdentifier(identifier: String): Boolean {
        val builtins = setOf("console", "Math", "Object", "Array", "String", "Number", "Boolean")
        return identifier in builtins
    }
}

/**
 * 卫生性违规类型
 */
sealed class HygieneViolation {
    abstract val location: SourceLocation
    
    data class UndefinedIdentifier(
        val identifier: String,
        override val location: SourceLocation
    ) : HygieneViolation()
    
    data class IdentifierConflict(
        val identifier: String,
        override val location: SourceLocation
    ) : HygieneViolation()
    
    data class ScopeViolation(
        val identifier: String,
        val expectedScope: String,
        val actualScope: String,
        override val location: SourceLocation
    ) : HygieneViolation()
}
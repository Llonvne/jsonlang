package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * AST 变换接口
 * 定义了 AST 节点变换的基本契约
 */
interface ASTTransformation {
    /** 变换名称 */
    val name: String
    
    /** 变换描述 */
    val description: String
    
    /** 变换优先级，数值越小优先级越高 */
    val priority: Int get() = 0
    
    /** 是否启用此变换 */
    val enabled: Boolean get() = true
    
    /**
     * 应用变换到 AST 节点
     * @param node 要变换的节点
     * @param context 变换上下文
     * @return 变换后的节点，如果没有变化则返回原节点
     */
    fun transform(node: ASTNode, context: TransformationContext): ASTNode
    
    /**
     * 检查是否应该应用此变换
     * @param node 要检查的节点
     * @param context 变换上下文
     * @return true 如果应该应用变换
     */
    fun shouldApply(node: ASTNode, context: TransformationContext): Boolean = true
}

/**
 * 变换上下文
 * 提供变换过程中需要的上下文信息
 */
data class TransformationContext(
    /** 当前程序根节点 */
    val program: Program,
    
    /** 错误报告器 */
    val errorReporter: ErrorReporter,
    
    /** 变换选项 */
    val options: TransformationOptions = TransformationOptions(),
    
    /** 符号表 */
    val symbolTable: MutableMap<String, ASTNode> = mutableMapOf(),
    
    /** 当前作用域深度 */
    var scopeDepth: Int = 0,
    
    /** 变换统计信息 */
    val statistics: TransformationStatistics = TransformationStatistics()
) {
    /** 进入新作用域 */
    fun enterScope() {
        scopeDepth++
    }
    
    /** 退出当前作用域 */
    fun exitScope() {
        scopeDepth--
    }
    
    /** 记录变换应用 */
    fun recordTransformation(transformationName: String) {
        statistics.recordTransformation(transformationName)
    }
}

/**
 * 变换选项
 */
data class TransformationOptions(
    /** 是否启用优化 */
    val optimizationEnabled: Boolean = true,
    
    /** 优化级别 (0-3) */
    val optimizationLevel: Int = 1,
    
    /** 是否保留调试信息 */
    val preserveDebugInfo: Boolean = true,
    
    /** 是否启用内联优化 */
    val inlineOptimization: Boolean = true,
    
    /** 是否启用死代码消除 */
    val deadCodeElimination: Boolean = true,
    
    /** 是否启用常量折叠 */
    val constantFolding: Boolean = true,
    
    /** 最大变换迭代次数 */
    val maxIterations: Int = 10
)

/**
 * 变换统计信息
 */
class TransformationStatistics {
    private val transformationCounts = mutableMapOf<String, Int>()
    private val nodeTypeCounts = mutableMapOf<String, Int>()
    
    /** 记录变换应用 */
    fun recordTransformation(transformationName: String) {
        transformationCounts[transformationName] = transformationCounts.getOrDefault(transformationName, 0) + 1
    }
    
    /** 记录节点类型处理 */
    fun recordNodeType(nodeType: String) {
        nodeTypeCounts[nodeType] = nodeTypeCounts.getOrDefault(nodeType, 0) + 1
    }
    
    /** 获取变换统计 */
    fun getTransformationStats(): Map<String, Int> = transformationCounts.toMap()
    
    /** 获取节点类型统计 */
    fun getNodeTypeStats(): Map<String, Int> = nodeTypeCounts.toMap()
    
    /** 获取总变换次数 */
    fun getTotalTransformations(): Int = transformationCounts.values.sum()
}

/**
 * 抽象 AST 变换基类
 * 提供通用的变换逻辑
 */
abstract class BaseASTTransformation : ASTTransformation {
    
    /**
     * 递归变换子节点
     */
    protected fun transformChildren(node: ASTNode, context: TransformationContext): ASTNode {
        return when (node) {
            is Program -> node.copy(
                imports = node.imports.map { transform(it, context) as ImportDeclaration },
                exports = node.exports.map { transform(it, context) as ExportDeclaration },
                types = node.types.map { transform(it, context) as TypeDeclaration },
                constants = node.constants.map { transform(it, context) as ConstantDeclaration },
                functions = node.functions.map { transform(it, context) as FunctionDeclaration },
                macros = node.macros.map { transform(it, context) as MacroDeclaration },
                dsls = node.dsls.map { transform(it, context) as DSLDeclaration }
            )
            
            is FunctionDeclaration -> node.copy(
                body = node.body.map { transform(it, context) as Statement }
            )
            
            is BlockStatement -> node.copy(
                statements = node.statements.map { transform(it, context) as Statement }
            )
            
            is BinaryExpression -> node.copy(
                left = transform(node.left, context) as Expression,
                right = transform(node.right, context) as Expression
            )
            
            is UnaryExpression -> node.copy(
                operand = transform(node.operand, context) as Expression
            )
            
            is CallExpression -> node.copy(
                function = transform(node.function, context) as Expression,
                arguments = node.arguments.map { 
                    it.copy(value = transform(it.value, context) as Expression) 
                }
            )
            
            is ConditionalStatement -> node.copy(
                condition = transform(node.condition, context) as Expression,
                thenBranch = transform(node.thenBranch, context) as Statement,
                elseBranch = node.elseBranch?.let { transform(it, context) as Statement }
            )
            
            is LoopStatement.ForLoop -> node.copy(
                iterable = transform(node.iterable, context) as Expression,
                body = transform(node.body, context) as Statement
            )
            
            is LoopStatement.WhileLoop -> node.copy(
                condition = transform(node.condition, context) as Expression,
                body = transform(node.body, context) as Statement
            )
            
            is TryCatchStatement -> node.copy(
                tryBlock = transform(node.tryBlock, context) as BlockStatement,
                catchClauses = node.catchClauses.map { clause ->
                    clause.copy(handler = transform(clause.handler, context) as BlockStatement)
                },
                finallyBlock = node.finallyBlock?.let { transform(it, context) as BlockStatement }
            )
            
            is ArrayExpression -> node.copy(
                elements = node.elements.map { transform(it, context) as Expression }
            )
            
            is ObjectExpression -> node.copy(
                properties = node.properties.map { prop ->
                    prop.copy(
                        key = transform(prop.key, context) as Expression,
                        value = transform(prop.value, context) as Expression
                    )
                }
            )
            
            is LambdaExpression -> node.copy(
                body = transform(node.body, context) as Expression
            )
            
            is ConditionalExpression -> node.copy(
                condition = transform(node.condition, context) as Expression,
                thenExpression = transform(node.thenExpression, context) as Expression,
                elseExpression = transform(node.elseExpression, context) as Expression
            )
            
            is CastExpression -> node.copy(
                expression = transform(node.expression, context) as Expression
            )
            
            is TypeCheckExpression -> node.copy(
                expression = transform(node.expression, context) as Expression
            )
            
            is MemberExpression -> node.copy(
                target = transform(node.target, context) as Expression
            )
            
            is VariableDeclaration -> node.copy(
                value = node.value?.let { transform(it, context) as Expression }
            )
            
            is ExpressionStatement -> node.copy(
                expression = transform(node.expression, context) as Expression
            )
            
            is JumpStatement.ReturnStatement -> node.copy(
                value = node.value?.let { transform(it, context) as Expression }
            )
            
            is MatchStatement -> node.copy(
                expression = transform(node.expression, context) as Expression,
                cases = node.cases.map { case ->
                    case.copy(
                        guard = case.guard?.let { transform(it, context) as Expression },
                        body = transform(case.body, context) as Statement
                    )
                },
                defaultCase = node.defaultCase?.let { transform(it, context) as Statement }
            )
            
            // 叶子节点直接返回
            else -> node
        }
    }
}

/**
 * 复合变换
 * 将多个变换组合成一个变换
 */
class CompositeTransformation(
    override val name: String,
    override val description: String,
    private val transformations: List<ASTTransformation>
) : ASTTransformation {
    
    override fun transform(node: ASTNode, context: TransformationContext): ASTNode {
        var currentNode = node
        
        for (transformation in transformations.filter { it.enabled }) {
            if (transformation.shouldApply(currentNode, context)) {
                currentNode = transformation.transform(currentNode, context)
                context.recordTransformation(transformation.name)
            }
        }
        
        return currentNode
    }
}
package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.types.Type
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * 语句节点基类
 */
@Serializable
sealed class Statement : ASTNode()

/**
 * 变量声明语句
 */
@Serializable
data class VariableDeclaration(
    val name: String,
    val type: Type?,
    val value: Expression?,
    val mutable: Boolean = false,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Statement() {
    override val nodeType: String = "VariableDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitVariableDeclaration(this)
}

/**
 * 表达式语句
 */
@Serializable
data class ExpressionStatement(
    val expression: Expression,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Statement() {
    override val nodeType: String = "ExpressionStatement"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitExpressionStatement(this)
}

/**
 * 条件语句
 */
@Serializable
data class ConditionalStatement(
    val condition: Expression,
    val thenBranch: Statement,
    val elseBranch: Statement? = null,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Statement() {
    override val nodeType: String = "ConditionalStatement"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitConditionalStatement(this)
}

/**
 * 循环语句
 */
@Serializable
sealed class LoopStatement : Statement() {
    /**
     * For 循环
     */
    @Serializable
    data class ForLoop(
        val iterator: String,
        val iterable: Expression,
        val body: Statement,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList()
    ) : LoopStatement() {
        override val nodeType: String = "ForLoop"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitForLoop(this)
    }

    /**
     * While 循环
     */
    @Serializable
    data class WhileLoop(
        val condition: Expression,
        val body: Statement,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList()
    ) : LoopStatement() {
        override val nodeType: String = "WhileLoop"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitWhileLoop(this)
    }
}

/**
 * 跳转语句
 */
@Serializable
sealed class JumpStatement : Statement() {
    /**
     * Return 语句
     */
    @Serializable
    data class ReturnStatement(
        val value: Expression? = null,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList()
    ) : JumpStatement() {
        override val nodeType: String = "ReturnStatement"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitReturnStatement(this)
    }

    /**
     * Break 语句
     */
    @Serializable
    data class BreakStatement(
        val label: String? = null,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList()
    ) : JumpStatement() {
        override val nodeType: String = "BreakStatement"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBreakStatement(this)
    }

    /**
     * Continue 语句
     */
    @Serializable
    data class ContinueStatement(
        val label: String? = null,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList()
    ) : JumpStatement() {
        override val nodeType: String = "ContinueStatement"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitContinueStatement(this)
    }
}

/**
 * 异常处理语句
 */
@Serializable
data class TryCatchStatement(
    val tryBlock: BlockStatement,
    val catchClauses: List<CatchClause>,
    val finallyBlock: BlockStatement? = null,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Statement() {
    override val nodeType: String = "TryCatchStatement"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitTryCatchStatement(this)
}

/**
 * Catch 子句
 */
@Serializable
data class CatchClause(
    val exceptionType: Type,
    val variableName: String? = null,
    val handler: BlockStatement,
    val location: SourceLocation
)

/**
 * 块语句
 */
@Serializable
data class BlockStatement(
    val statements: List<Statement>,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Statement() {
    override val nodeType: String = "BlockStatement"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBlockStatement(this)
}

/**
 * 模式匹配语句
 */
@Serializable
data class MatchStatement(
    val expression: Expression,
    val cases: List<MatchCase>,
    val defaultCase: Statement? = null,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Statement() {
    override val nodeType: String = "MatchStatement"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitMatchStatement(this)
}

/**
 * 匹配案例
 */
@Serializable
data class MatchCase(
    val pattern: Pattern,
    val guard: Expression? = null,
    val body: Statement,
    val location: SourceLocation
)

/**
 * 模式
 */
@Serializable
sealed class Pattern {
    @Serializable
    data class LiteralPattern(val value: @Contextual Any) : Pattern()
    @Serializable
    data class VariablePattern(val name: String) : Pattern()
    @Serializable
    data class ConstructorPattern(val constructor: String, val args: List<Pattern>) : Pattern()
    @Serializable
    data class WildcardPattern(val name: String? = null) : Pattern()
}
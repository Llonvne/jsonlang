package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.types.Type
import kotlinx.serialization.Serializable

/**
 * 表达式节点基类
 */
@Serializable
sealed class Expression : ASTNode() {
    /** 表达式的推导类型 */
    abstract val inferredType: Type?
}

/**
 * 字面量表达式
 */
@Serializable
sealed class LiteralExpression : Expression() {
    /**
     * 字符串字面量
     */
    @Serializable
    data class StringLiteral(
        val value: String,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList(),
        override val inferredType: Type? = null
    ) : LiteralExpression() {
        override val nodeType: String = "StringLiteral"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitStringLiteral(this)
    }

    /**
     * 整数字面量
     */
    @Serializable
    data class IntLiteral(
        val value: Long,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList(),
        override val inferredType: Type? = null
    ) : LiteralExpression() {
        override val nodeType: String = "IntLiteral"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitIntLiteral(this)
    }

    /**
     * 浮点数字面量
     */
    @Serializable
    data class FloatLiteral(
        val value: Double,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList(),
        override val inferredType: Type? = null
    ) : LiteralExpression() {
        override val nodeType: String = "FloatLiteral"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitFloatLiteral(this)
    }

    /**
     * 布尔字面量
     */
    @Serializable
    data class BooleanLiteral(
        val value: Boolean,
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList(),
        override val inferredType: Type? = null
    ) : LiteralExpression() {
        override val nodeType: String = "BooleanLiteral"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBooleanLiteral(this)
    }

    /**
     * 空值字面量
     */
    @Serializable
    data class NullLiteral(
        override val location: SourceLocation,
        override val annotations: List<Annotation> = emptyList(),
        override val inferredType: Type? = null
    ) : LiteralExpression() {
        override val nodeType: String = "NullLiteral"
        override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitNullLiteral(this)
    }
}

/**
 * 标识符表达式
 */
@Serializable
data class IdentifierExpression(
    val name: String,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "IdentifierExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitIdentifierExpression(this)
}

/**
 * 二元操作表达式
 */
@Serializable
data class BinaryExpression(
    val left: Expression,
    val operator: BinaryOperator,
    val right: Expression,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "BinaryExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitBinaryExpression(this)
}

/**
 * 二元操作符枚举
 */
@Serializable
enum class BinaryOperator(val symbol: String) {
    // 算术操作符
    PLUS("+"), MINUS("-"), MULTIPLY("*"), DIVIDE("/"), MODULO("%"),
    
    // 比较操作符
    EQUAL("=="), NOT_EQUAL("!="), LESS_THAN("<"), LESS_EQUAL("<="),
    GREATER_THAN(">"), GREATER_EQUAL(">="),
    
    // 逻辑操作符
    LOGICAL_AND("&&"), LOGICAL_OR("||"),
    
    // 位操作符
    BITWISE_AND("&"), BITWISE_OR("|"), BITWISE_XOR("^"),
    LEFT_SHIFT("<<"), RIGHT_SHIFT(">>"),
    
    // 赋值操作符
    ASSIGN("="), PLUS_ASSIGN("+="), MINUS_ASSIGN("-="),
    MULTIPLY_ASSIGN("*="), DIVIDE_ASSIGN("/=")
}

/**
 * 一元操作表达式
 */
@Serializable
data class UnaryExpression(
    val operator: UnaryOperator,
    val operand: Expression,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "UnaryExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitUnaryExpression(this)
}

/**
 * 一元操作符枚举
 */
@Serializable
enum class UnaryOperator(val symbol: String) {
    PLUS("+"), MINUS("-"), LOGICAL_NOT("!"), BITWISE_NOT("~"),
    PRE_INCREMENT("++"), POST_INCREMENT("++"),
    PRE_DECREMENT("--"), POST_DECREMENT("--")
}

/**
 * 函数调用表达式
 */
@Serializable
data class CallExpression(
    val function: Expression,
    val arguments: List<Argument>,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "CallExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitCallExpression(this)
}

/**
 * 函数参数
 */
@Serializable
data class Argument(
    val name: String? = null,  // 命名参数
    val value: Expression
)

/**
 * 成员访问表达式
 */
@Serializable
data class MemberExpression(
    val target: Expression,
    val property: String,
    val computed: Boolean = false,  // true for obj[prop], false for obj.prop
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "MemberExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitMemberExpression(this)
}

/**
 * 数组表达式
 */
@Serializable
data class ArrayExpression(
    val elements: List<Expression>,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "ArrayExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitArrayExpression(this)
}

/**
 * 对象表达式
 */
@Serializable
data class ObjectExpression(
    val properties: List<ObjectProperty>,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "ObjectExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitObjectExpression(this)
}

/**
 * 对象属性
 */
@Serializable
data class ObjectProperty(
    val key: Expression,
    val value: Expression,
    val computed: Boolean = false
)

/**
 * Lambda 表达式
 */
@Serializable
data class LambdaExpression(
    val parameters: List<Parameter>,
    val body: Expression,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "LambdaExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitLambdaExpression(this)
}

/**
 * 条件表达式 (三元操作符)
 */
@Serializable
data class ConditionalExpression(
    val condition: Expression,
    val thenExpression: Expression,
    val elseExpression: Expression,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "ConditionalExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitConditionalExpression(this)
}

/**
 * 类型转换表达式
 */
@Serializable
data class CastExpression(
    val expression: Expression,
    val targetType: Type,
    val safe: Boolean = false,  // true for safe cast (as?), false for unsafe cast (as)
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "CastExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitCastExpression(this)
}

/**
 * 类型检查表达式
 */
@Serializable
data class TypeCheckExpression(
    val expression: Expression,
    val type: Type,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "TypeCheckExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitTypeCheckExpression(this)
}

/**
 * 宏调用表达式
 */
@Serializable
data class MacroCall(
    val name: String,
    val arguments: List<ASTNode>,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "MacroCall"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitMacroCall(this)
}

/**
 * 宏参数占位符表达式
 * 用于在宏模板中标记参数位置
 */
@Serializable
data class MacroParameterPlaceholder(
    val parameterName: String,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "MacroParameterPlaceholder"
    override fun <T> accept(visitor: ASTVisitor<T>): T {
        // 如果访问者不支持宏参数占位符，则抛出异常
        throw UnsupportedOperationException("MacroParameterPlaceholder should be expanded before visiting")
    }
}/**
 * 代码
字面量表达式
 * 用于表示特定后端的代码片段
 */
@Serializable
data class CodeLiteralExpression(
    val code: String,
    val language: String,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList(),
    override val inferredType: Type? = null
) : Expression() {
    override val nodeType: String = "CodeLiteralExpression"
    override fun <T> accept(visitor: ASTVisitor<T>): T {
        // 默认处理为字符串字面量
        return visitor.visitStringLiteral(LiteralExpression.StringLiteral(code, location, annotations, inferredType))
    }
}
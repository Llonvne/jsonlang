package cn.llonvne.jsonlang.core.ast.optimizations

import cn.llonvne.jsonlang.core.ast.*

/**
 * 常量折叠优化
 * 在编译时计算常量表达式的值
 */
class ConstantFoldingTransformation : BaseASTTransformation() {
    override val name: String = "ConstantFolding"
    override val description: String = "常量折叠优化 - 在编译时计算常量表达式"
    override val priority: Int = 10
    
    override fun shouldApply(node: ASTNode, context: TransformationContext): Boolean {
        return context.options.constantFolding && node is BinaryExpression
    }
    
    override fun transform(node: ASTNode, context: TransformationContext): ASTNode {
        if (node !is BinaryExpression) return node
        
        // 先变换子节点
        val transformedNode = transformChildren(node, context) as BinaryExpression
        
        // 尝试折叠常量
        return foldConstantExpression(transformedNode) ?: transformedNode
    }
    
    /**
     * 折叠常量表达式
     */
    private fun foldConstantExpression(expr: BinaryExpression): Expression? {
        val left = expr.left
        val right = expr.right
        
        return when {
            // 整数运算
            left is LiteralExpression.IntLiteral && right is LiteralExpression.IntLiteral -> {
                foldIntegerOperation(left, expr.operator, right, expr.location, expr.annotations)
            }
            
            // 浮点数运算
            left is LiteralExpression.FloatLiteral && right is LiteralExpression.FloatLiteral -> {
                foldFloatOperation(left, expr.operator, right, expr.location, expr.annotations)
            }
            
            // 混合数值运算
            isNumericLiteral(left) && isNumericLiteral(right) -> {
                foldMixedNumericOperation(left, expr.operator, right, expr.location, expr.annotations)
            }
            
            // 字符串运算
            left is LiteralExpression.StringLiteral && right is LiteralExpression.StringLiteral -> {
                foldStringOperation(left, expr.operator, right, expr.location, expr.annotations)
            }
            
            // 布尔运算
            left is LiteralExpression.BooleanLiteral && right is LiteralExpression.BooleanLiteral -> {
                foldBooleanOperation(left, expr.operator, right, expr.location, expr.annotations)
            }
            
            // 比较运算
            isComparableConstant(left) && isComparableConstant(right) -> {
                foldComparisonOperation(left, expr.operator, right, expr.location, expr.annotations)
            }
            
            else -> null
        }
    }
    
    /**
     * 折叠整数运算
     */
    private fun foldIntegerOperation(
        left: LiteralExpression.IntLiteral,
        operator: BinaryOperator,
        right: LiteralExpression.IntLiteral,
        location: cn.llonvne.jsonlang.core.source.SourceLocation,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
    ): Expression? {
        return when (operator) {
            BinaryOperator.PLUS -> LiteralExpression.IntLiteral(left.value + right.value, location, annotations)
            BinaryOperator.MINUS -> LiteralExpression.IntLiteral(left.value - right.value, location, annotations)
            BinaryOperator.MULTIPLY -> LiteralExpression.IntLiteral(left.value * right.value, location, annotations)
            BinaryOperator.DIVIDE -> {
                if (right.value != 0L) {
                    LiteralExpression.IntLiteral(left.value / right.value, location, annotations)
                } else null // 避免除零错误
            }
            BinaryOperator.MODULO -> {
                if (right.value != 0L) {
                    LiteralExpression.IntLiteral(left.value % right.value, location, annotations)
                } else null
            }
            BinaryOperator.BITWISE_AND -> LiteralExpression.IntLiteral(left.value and right.value, location, annotations)
            BinaryOperator.BITWISE_OR -> LiteralExpression.IntLiteral(left.value or right.value, location, annotations)
            BinaryOperator.BITWISE_XOR -> LiteralExpression.IntLiteral(left.value xor right.value, location, annotations)
            BinaryOperator.LEFT_SHIFT -> LiteralExpression.IntLiteral(left.value shl right.value.toInt(), location, annotations)
            BinaryOperator.RIGHT_SHIFT -> LiteralExpression.IntLiteral(left.value shr right.value.toInt(), location, annotations)
            else -> null
        }
    }
    
    /**
     * 折叠浮点数运算
     */
    private fun foldFloatOperation(
        left: LiteralExpression.FloatLiteral,
        operator: BinaryOperator,
        right: LiteralExpression.FloatLiteral,
        location: cn.llonvne.jsonlang.core.source.SourceLocation,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
    ): Expression? {
        return when (operator) {
            BinaryOperator.PLUS -> LiteralExpression.FloatLiteral(left.value + right.value, location, annotations)
            BinaryOperator.MINUS -> LiteralExpression.FloatLiteral(left.value - right.value, location, annotations)
            BinaryOperator.MULTIPLY -> LiteralExpression.FloatLiteral(left.value * right.value, location, annotations)
            BinaryOperator.DIVIDE -> {
                if (right.value != 0.0) {
                    LiteralExpression.FloatLiteral(left.value / right.value, location, annotations)
                } else null
            }
            BinaryOperator.MODULO -> {
                if (right.value != 0.0) {
                    LiteralExpression.FloatLiteral(left.value % right.value, location, annotations)
                } else null
            }
            else -> null
        }
    }
    
    /**
     * 折叠混合数值运算
     */
    private fun foldMixedNumericOperation(
        left: Expression,
        operator: BinaryOperator,
        right: Expression,
        location: cn.llonvne.jsonlang.core.source.SourceLocation,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
    ): Expression? {
        val leftValue = getNumericValue(left) ?: return null
        val rightValue = getNumericValue(right) ?: return null
        
        return when (operator) {
            BinaryOperator.PLUS -> LiteralExpression.FloatLiteral(leftValue + rightValue, location, annotations)
            BinaryOperator.MINUS -> LiteralExpression.FloatLiteral(leftValue - rightValue, location, annotations)
            BinaryOperator.MULTIPLY -> LiteralExpression.FloatLiteral(leftValue * rightValue, location, annotations)
            BinaryOperator.DIVIDE -> {
                if (rightValue != 0.0) {
                    LiteralExpression.FloatLiteral(leftValue / rightValue, location, annotations)
                } else null
            }
            BinaryOperator.MODULO -> {
                if (rightValue != 0.0) {
                    LiteralExpression.FloatLiteral(leftValue % rightValue, location, annotations)
                } else null
            }
            else -> null
        }
    }
    
    /**
     * 折叠字符串运算
     */
    private fun foldStringOperation(
        left: LiteralExpression.StringLiteral,
        operator: BinaryOperator,
        right: LiteralExpression.StringLiteral,
        location: cn.llonvne.jsonlang.core.source.SourceLocation,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
    ): Expression? {
        return when (operator) {
            BinaryOperator.PLUS -> LiteralExpression.StringLiteral(left.value + right.value, location, annotations)
            else -> null
        }
    }
    
    /**
     * 折叠布尔运算
     */
    private fun foldBooleanOperation(
        left: LiteralExpression.BooleanLiteral,
        operator: BinaryOperator,
        right: LiteralExpression.BooleanLiteral,
        location: cn.llonvne.jsonlang.core.source.SourceLocation,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
    ): Expression? {
        return when (operator) {
            BinaryOperator.LOGICAL_AND -> LiteralExpression.BooleanLiteral(left.value && right.value, location, annotations)
            BinaryOperator.LOGICAL_OR -> LiteralExpression.BooleanLiteral(left.value || right.value, location, annotations)
            else -> null
        }
    }
    
    /**
     * 折叠比较运算
     */
    private fun foldComparisonOperation(
        left: Expression,
        operator: BinaryOperator,
        right: Expression,
        location: cn.llonvne.jsonlang.core.source.SourceLocation,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation>
    ): Expression? {
        return when (operator) {
            BinaryOperator.EQUAL -> LiteralExpression.BooleanLiteral(compareConstants(left, right) == 0, location, annotations)
            BinaryOperator.NOT_EQUAL -> LiteralExpression.BooleanLiteral(compareConstants(left, right) != 0, location, annotations)
            BinaryOperator.LESS_THAN -> LiteralExpression.BooleanLiteral(compareConstants(left, right) < 0, location, annotations)
            BinaryOperator.LESS_EQUAL -> LiteralExpression.BooleanLiteral(compareConstants(left, right) <= 0, location, annotations)
            BinaryOperator.GREATER_THAN -> LiteralExpression.BooleanLiteral(compareConstants(left, right) > 0, location, annotations)
            BinaryOperator.GREATER_EQUAL -> LiteralExpression.BooleanLiteral(compareConstants(left, right) >= 0, location, annotations)
            else -> null
        }
    }
    
    /**
     * 检查是否为数值字面量
     */
    private fun isNumericLiteral(expr: Expression): Boolean {
        return expr is LiteralExpression.IntLiteral || expr is LiteralExpression.FloatLiteral
    }
    
    /**
     * 获取数值
     */
    private fun getNumericValue(expr: Expression): Double? {
        return when (expr) {
            is LiteralExpression.IntLiteral -> expr.value.toDouble()
            is LiteralExpression.FloatLiteral -> expr.value
            else -> null
        }
    }
    
    /**
     * 检查是否为可比较的常量
     */
    private fun isComparableConstant(expr: Expression): Boolean {
        return expr is LiteralExpression.IntLiteral || 
               expr is LiteralExpression.FloatLiteral || 
               expr is LiteralExpression.StringLiteral ||
               expr is LiteralExpression.BooleanLiteral
    }
    
    /**
     * 比较常量
     */
    private fun compareConstants(left: Expression, right: Expression): Int {
        return when {
            left is LiteralExpression.IntLiteral && right is LiteralExpression.IntLiteral -> 
                left.value.compareTo(right.value)
            left is LiteralExpression.FloatLiteral && right is LiteralExpression.FloatLiteral -> 
                left.value.compareTo(right.value)
            left is LiteralExpression.StringLiteral && right is LiteralExpression.StringLiteral -> 
                left.value.compareTo(right.value)
            left is LiteralExpression.BooleanLiteral && right is LiteralExpression.BooleanLiteral -> 
                left.value.compareTo(right.value)
            isNumericLiteral(left) && isNumericLiteral(right) -> {
                val leftValue = getNumericValue(left)!!
                val rightValue = getNumericValue(right)!!
                leftValue.compareTo(rightValue)
            }
            else -> 0
        }
    }
}
package cn.llonvne.jsonlang.core.ast.validation

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.errors.*
import cn.llonvne.jsonlang.core.types.*

/**
 * 控制流验证器
 * 专门负责验证控制流语句的正确性
 */
class ControlFlowValidator(
    private val errorReporter: ErrorReporter
) : BaseASTVisitor<Unit>() {
    
    private var currentContext = ValidationContext()
    
    /**
     * 验证上下文
     */
    data class ValidationContext(
        val inLoop: Boolean = false,
        val inFunction: Boolean = false,
        val functionReturnType: Type? = null,
        val loopLabels: Set<String> = emptySet(),
        val inTryBlock: Boolean = false
    )
    
    override fun defaultVisit(node: ASTNode) {
        // 默认不做任何操作
    }
    
    /**
     * 验证函数声明
     */
    override fun visitFunctionDeclaration(node: FunctionDeclaration) {
        val previousContext = currentContext
        currentContext = currentContext.copy(
            inFunction = true,
            functionReturnType = node.returnType
        )
        
        // 验证函数体
        node.body.forEach { it.accept(this) }
        
        currentContext = previousContext
    }
    
    /**
     * 验证条件语句
     */
    override fun visitConditionalStatement(node: ConditionalStatement) {
        // 验证条件表达式
        validateConditionExpression(node.condition)
        
        // 验证分支
        node.thenBranch.accept(this)
        node.elseBranch?.accept(this)
    }
    
    /**
     * 验证 For 循环
     */
    override fun visitForLoop(node: LoopStatement.ForLoop) {
        // 验证可迭代表达式
        validateIterableExpression(node.iterable)
        
        val previousContext = currentContext
        currentContext = currentContext.copy(inLoop = true)
        
        // 验证循环体
        node.body.accept(this)
        
        currentContext = previousContext
    }
    
    /**
     * 验证 While 循环
     */
    override fun visitWhileLoop(node: LoopStatement.WhileLoop) {
        // 验证条件表达式
        validateConditionExpression(node.condition)
        
        val previousContext = currentContext
        currentContext = currentContext.copy(inLoop = true)
        
        // 验证循环体
        node.body.accept(this)
        
        currentContext = previousContext
    }
    
    /**
     * 验证 Return 语句
     */
    override fun visitReturnStatement(node: JumpStatement.ReturnStatement) {
        if (!currentContext.inFunction) {
            errorReporter.report(
                ErrorBuilder(
                    "RETURN_OUTSIDE_FUNCTION",
                    "Return 语句只能在函数内使用",
                    node.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
            return
        }
        
        // 验证返回值类型
        val returnType = currentContext.functionReturnType
        if (returnType != null) {
            when {
                node.value == null && !isUnitType(returnType) -> {
                    errorReporter.report(
                        ErrorBuilder(
                            "MISSING_RETURN_VALUE",
                            "函数需要返回值，期望类型: $returnType",
                            node.location
                        ).category(ErrorCategory.TYPE).build()
                    )
                }
                node.value != null && isUnitType(returnType) -> {
                    errorReporter.report(
                        ErrorBuilder(
                            "UNEXPECTED_RETURN_VALUE",
                            "Unit 函数不应该返回值",
                            node.location
                        ).category(ErrorCategory.TYPE).build()
                    )
                }
                node.value != null -> {
                    // 这里应该进行类型检查
                    validateReturnValueType(node.value, returnType, node.location)
                }
            }
        }
    }
    
    /**
     * 验证 Break 语句
     */
    override fun visitBreakStatement(node: JumpStatement.BreakStatement) {
        if (!currentContext.inLoop) {
            errorReporter.report(
                ErrorBuilder(
                    "BREAK_OUTSIDE_LOOP",
                    "Break 语句只能在循环内使用",
                    node.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
            return
        }
        
        // 验证标签
        if (node.label != null && !currentContext.loopLabels.contains(node.label)) {
            errorReporter.report(
                ErrorBuilder(
                    "UNDEFINED_LOOP_LABEL",
                    "未定义的循环标签: ${node.label}",
                    node.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
    }
    
    /**
     * 验证 Continue 语句
     */
    override fun visitContinueStatement(node: JumpStatement.ContinueStatement) {
        if (!currentContext.inLoop) {
            errorReporter.report(
                ErrorBuilder(
                    "CONTINUE_OUTSIDE_LOOP",
                    "Continue 语句只能在循环内使用",
                    node.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
            return
        }
        
        // 验证标签
        if (node.label != null && !currentContext.loopLabels.contains(node.label)) {
            errorReporter.report(
                ErrorBuilder(
                    "UNDEFINED_LOOP_LABEL",
                    "未定义的循环标签: ${node.label}",
                    node.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
    }
    
    /**
     * 验证 Try-Catch 语句
     */
    override fun visitTryCatchStatement(node: TryCatchStatement) {
        val previousContext = currentContext
        
        // 验证 try 块
        currentContext = currentContext.copy(inTryBlock = true)
        node.tryBlock.accept(this)
        
        // 验证 catch 子句
        currentContext = previousContext
        validateCatchClauses(node.catchClauses)
        
        // 验证 finally 块
        node.finallyBlock?.accept(this)
    }
    
    /**
     * 验证模式匹配语句
     */
    override fun visitMatchStatement(node: MatchStatement) {
        // 验证匹配表达式
        val expressionType = inferExpressionType(node.expression)
        
        // 验证每个匹配案例
        node.cases.forEach { case ->
            validateMatchCase(case, expressionType)
            case.body.accept(this)
        }
        
        // 验证默认案例
        node.defaultCase?.accept(this)
        
        // 检查匹配完整性
        validateMatchExhaustiveness(node, expressionType)
    }
    
    /**
     * 验证块语句
     */
    override fun visitBlockStatement(node: BlockStatement) {
        node.statements.forEach { it.accept(this) }
    }
    
    // 辅助验证方法
    
    private fun validateConditionExpression(expression: Expression) {
        val type = inferExpressionType(expression)
        if (!isBooleanType(type)) {
            errorReporter.report(
                ErrorBuilder(
                    "INVALID_CONDITION_TYPE",
                    "条件表达式必须是布尔类型，实际类型: $type",
                    expression.location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    private fun validateIterableExpression(expression: Expression) {
        val type = inferExpressionType(expression)
        if (!isIterableType(type)) {
            errorReporter.report(
                ErrorBuilder(
                    "NOT_ITERABLE",
                    "表达式不是可迭代类型，实际类型: $type",
                    expression.location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    private fun validateReturnValueType(expression: Expression, expectedType: Type, location: cn.llonvne.jsonlang.core.source.SourceLocation) {
        val actualType = inferExpressionType(expression)
        if (!isAssignableType(actualType, expectedType)) {
            errorReporter.report(
                ErrorBuilder(
                    "RETURN_TYPE_MISMATCH",
                    "返回值类型不匹配，期望: $expectedType，实际: $actualType",
                    location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    private fun validateCatchClauses(catchClauses: List<CatchClause>) {
        val caughtTypes = mutableSetOf<Type>()
        
        catchClauses.forEach { clause ->
            // 检查异常类型是否有效
            if (!isExceptionType(clause.exceptionType)) {
                errorReporter.report(
                    ErrorBuilder(
                        "INVALID_EXCEPTION_TYPE",
                        "Catch 子句必须捕获异常类型: ${clause.exceptionType}",
                        clause.location
                    ).category(ErrorCategory.TYPE).build()
                )
            }
            
            // 检查是否有重复的异常类型
            if (caughtTypes.contains(clause.exceptionType)) {
                errorReporter.report(
                    ErrorBuilder(
                        "DUPLICATE_CATCH_CLAUSE",
                        "重复的 catch 子句: ${clause.exceptionType}",
                        clause.location
                    ).category(ErrorCategory.SEMANTIC).build()
                )
            }
            
            caughtTypes.add(clause.exceptionType)
            
            // 验证处理器
            clause.handler.accept(this)
        }
    }
    
    private fun validateMatchCase(case: MatchCase, expressionType: Type) {
        // 验证模式
        validatePattern(case.pattern, expressionType, case.location)
        
        // 验证守卫条件
        case.guard?.let { guard ->
            val guardType = inferExpressionType(guard)
            if (!isBooleanType(guardType)) {
                errorReporter.report(
                    ErrorBuilder(
                        "INVALID_GUARD_TYPE",
                        "守卫条件必须是布尔类型，实际类型: $guardType",
                        case.location
                    ).category(ErrorCategory.TYPE).build()
                )
            }
        }
    }
    
    private fun validatePattern(pattern: Pattern, expressionType: Type, location: cn.llonvne.jsonlang.core.source.SourceLocation) {
        when (pattern) {
            is Pattern.LiteralPattern -> {
                val patternType = inferLiteralType(pattern.value)
                if (!isAssignableType(patternType, expressionType)) {
                    errorReporter.report(
                        ErrorBuilder(
                            "PATTERN_TYPE_MISMATCH",
                            "字面量模式类型不匹配，期望: $expressionType，实际: $patternType",
                            location
                        ).category(ErrorCategory.TYPE).build()
                    )
                }
            }
            is Pattern.ConstructorPattern -> {
                validateConstructorPattern(pattern, expressionType, location)
            }
            is Pattern.VariablePattern -> {
                // 变量模式总是有效的
            }
            is Pattern.WildcardPattern -> {
                // 通配符模式总是有效的
            }
        }
    }
    
    private fun validateConstructorPattern(
        pattern: Pattern.ConstructorPattern,
        expressionType: Type,
        location: cn.llonvne.jsonlang.core.source.SourceLocation
    ) {
        // 验证构造器是否存在
        if (!isValidConstructor(pattern.constructor, expressionType)) {
            errorReporter.report(
                ErrorBuilder(
                    "INVALID_CONSTRUCTOR",
                    "无效的构造器: ${pattern.constructor} for type $expressionType",
                    location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
        
        // 验证构造器参数
        pattern.args.forEach { arg ->
            // 这里需要获取构造器的参数类型进行验证
            // validatePattern(arg, constructorArgType, location)
        }
    }
    
    private fun validateMatchExhaustiveness(match: MatchStatement, expressionType: Type) {
        // 检查模式匹配是否穷尽
        if (match.defaultCase == null && !isExhaustiveMatch(match.cases, expressionType)) {
            errorReporter.report(
                ErrorBuilder(
                    "NON_EXHAUSTIVE_MATCH",
                    "模式匹配不完整，缺少某些情况的处理",
                    match.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
    }
    
    // 类型检查辅助方法（这些应该与类型系统集成）
    
    private fun inferExpressionType(expression: Expression): Type {
        // 简化的类型推导实现
        return when (expression) {
            is LiteralExpression.BooleanLiteral -> PrimitiveType.BooleanType
            is LiteralExpression.StringLiteral -> PrimitiveType.StringType
            is LiteralExpression.IntLiteral -> PrimitiveType.IntType
            is LiteralExpression.FloatLiteral -> PrimitiveType.FloatType
            is ArrayExpression -> CompositeType.ArrayType(PrimitiveType.AnyType)
            else -> PrimitiveType.AnyType // 临时实现
        }
    }
    
    private fun inferLiteralType(value: Any): Type {
        return when (value) {
            is String -> PrimitiveType.StringType
            is Int -> PrimitiveType.IntType
            is Float -> PrimitiveType.FloatType
            is Boolean -> PrimitiveType.BooleanType
            else -> PrimitiveType.AnyType
        }
    }
    
    private fun isBooleanType(type: Type): Boolean {
        return type == PrimitiveType.BooleanType || type == PrimitiveType.NullableBooleanType || type == PrimitiveType.AnyType
    }
    
    private fun isUnitType(type: Type): Boolean {
        return type == PrimitiveType.UnitType
    }
    
    private fun isIterableType(type: Type): Boolean {
        return when (type) {
            is CompositeType.ArrayType -> true
            is PrimitiveType.StringType, is PrimitiveType.NullableStringType -> true
            is PrimitiveType.AnyType -> true // 临时允许 AnyType 作为可迭代类型
            else -> false
        }
    }
    
    private fun isExceptionType(type: Type): Boolean {
        // 检查类型是否继承自 Exception
        return true // 临时实现
    }
    
    private fun isAssignableType(from: Type, to: Type): Boolean {
        // 检查类型兼容性
        return true // 临时实现
    }
    
    private fun isValidConstructor(constructor: String, type: Type): Boolean {
        // 检查构造器是否对给定类型有效
        return true // 临时实现
    }
    
    private fun isExhaustiveMatch(cases: List<MatchCase>, type: Type): Boolean {
        // 检查模式匹配是否穷尽
        return false // 临时实现，总是要求默认情况
    }
}
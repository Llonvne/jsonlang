package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.errors.*

/**
 * 控制流处理器
 * 负责处理和验证控制流语句的语义正确性
 */
class ControlFlowProcessor(
    private val errorReporter: ErrorReporter
) {
    
    /**
     * 控制流上下文
     * 跟踪当前的控制流状态
     */
    data class ControlFlowContext(
        val inLoop: Boolean = false,
        val inFunction: Boolean = false,
        val expectedReturnType: Type? = null,
        val loopLabels: Set<String> = emptySet()
    )
    
    /**
     * 处理条件语句
     */
    fun processConditionalStatement(
        statement: ConditionalStatement,
        context: ControlFlowContext
    ): ConditionalStatement {
        // 验证条件表达式必须是布尔类型
        validateBooleanCondition(statement.condition, statement.location)
        
        // 处理分支语句
        val processedThenBranch = processStatement(statement.thenBranch, context)
        val processedElseBranch = statement.elseBranch?.let { 
            processStatement(it, context) 
        }
        
        return statement.copy(
            thenBranch = processedThenBranch,
            elseBranch = processedElseBranch
        )
    }
    
    /**
     * 处理 For 循环语句
     */
    fun processForLoop(
        loop: LoopStatement.ForLoop,
        context: ControlFlowContext
    ): LoopStatement.ForLoop {
        // 验证可迭代表达式
        validateIterableExpression(loop.iterable, loop.location)
        
        // 在循环上下文中处理循环体
        val loopContext = context.copy(inLoop = true)
        val processedBody = processStatement(loop.body, loopContext)
        
        return loop.copy(body = processedBody)
    }
    
    /**
     * 处理 While 循环语句
     */
    fun processWhileLoop(
        loop: LoopStatement.WhileLoop,
        context: ControlFlowContext
    ): LoopStatement.WhileLoop {
        // 验证条件表达式必须是布尔类型
        validateBooleanCondition(loop.condition, loop.location)
        
        // 在循环上下文中处理循环体
        val loopContext = context.copy(inLoop = true)
        val processedBody = processStatement(loop.body, loopContext)
        
        return loop.copy(body = processedBody)
    }
    
    /**
     * 处理 Return 语句
     */
    fun processReturnStatement(
        statement: JumpStatement.ReturnStatement,
        context: ControlFlowContext
    ): JumpStatement.ReturnStatement {
        if (!context.inFunction) {
            errorReporter.report(
                ErrorBuilder(
                    "RETURN_OUTSIDE_FUNCTION",
                    "Return 语句只能在函数内使用",
                    statement.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
        
        // 验证返回值类型
        if (statement.value != null && context.expectedReturnType != null) {
            validateReturnType(statement.value, context.expectedReturnType, statement.location)
        }
        
        return statement
    }
    
    /**
     * 处理 Break 语句
     */
    fun processBreakStatement(
        statement: JumpStatement.BreakStatement,
        context: ControlFlowContext
    ): JumpStatement.BreakStatement {
        if (!context.inLoop) {
            errorReporter.report(
                ErrorBuilder(
                    "BREAK_OUTSIDE_LOOP",
                    "Break 语句只能在循环内使用",
                    statement.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
        
        // 验证标签是否存在
        if (statement.label != null && !context.loopLabels.contains(statement.label)) {
            errorReporter.report(
                ErrorBuilder(
                    "UNDEFINED_LABEL",
                    "未定义的标签: ${statement.label}",
                    statement.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
        
        return statement
    }
    
    /**
     * 处理 Continue 语句
     */
    fun processContinueStatement(
        statement: JumpStatement.ContinueStatement,
        context: ControlFlowContext
    ): JumpStatement.ContinueStatement {
        if (!context.inLoop) {
            errorReporter.report(
                ErrorBuilder(
                    "CONTINUE_OUTSIDE_LOOP",
                    "Continue 语句只能在循环内使用",
                    statement.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
        
        // 验证标签是否存在
        if (statement.label != null && !context.loopLabels.contains(statement.label)) {
            errorReporter.report(
                ErrorBuilder(
                    "UNDEFINED_LABEL",
                    "未定义的标签: ${statement.label}",
                    statement.location
                ).category(ErrorCategory.SEMANTIC).build()
            )
        }
        
        return statement
    }
    
    /**
     * 处理模式匹配语句
     */
    fun processMatchStatement(
        statement: MatchStatement,
        context: ControlFlowContext
    ): MatchStatement {
        // 验证匹配表达式
        val matchExpressionType = inferExpressionType(statement.expression)
        
        // 处理每个匹配案例
        val processedCases = statement.cases.map { case ->
            validatePattern(case.pattern, matchExpressionType, case.location)
            case.guard?.let { validateBooleanCondition(it, case.location) }
            
            case.copy(
                body = processStatement(case.body, context)
            )
        }
        
        // 处理默认案例
        val processedDefaultCase = statement.defaultCase?.let {
            processStatement(it, context)
        }
        
        // 检查模式匹配的完整性
        validateMatchCompleteness(statement.expression, processedCases, statement.location)
        
        return statement.copy(
            cases = processedCases,
            defaultCase = processedDefaultCase
        )
    }
    
    /**
     * 处理块语句
     */
    fun processBlockStatement(
        statement: BlockStatement,
        context: ControlFlowContext
    ): BlockStatement {
        val processedStatements = statement.statements.map { stmt ->
            processStatement(stmt, context)
        }
        
        return statement.copy(statements = processedStatements)
    }
    
    /**
     * 通用语句处理方法
     */
    fun processStatement(statement: Statement, context: ControlFlowContext): Statement {
        return when (statement) {
            is ConditionalStatement -> processConditionalStatement(statement, context)
            is LoopStatement.ForLoop -> processForLoop(statement, context)
            is LoopStatement.WhileLoop -> processWhileLoop(statement, context)
            is JumpStatement.ReturnStatement -> processReturnStatement(statement, context)
            is JumpStatement.BreakStatement -> processBreakStatement(statement, context)
            is JumpStatement.ContinueStatement -> processContinueStatement(statement, context)
            is MatchStatement -> processMatchStatement(statement, context)
            is BlockStatement -> processBlockStatement(statement, context)
            is TryCatchStatement -> processTryCatchStatement(statement, context)
            else -> statement // 其他语句类型不需要特殊处理
        }
    }
    
    /**
     * 处理异常处理语句
     */
    fun processTryCatchStatement(
        statement: TryCatchStatement,
        context: ControlFlowContext
    ): TryCatchStatement {
        // 处理 try 块
        val processedTryBlock = processBlockStatement(statement.tryBlock, context)
        
        // 处理 catch 子句
        val processedCatchClauses = statement.catchClauses.map { clause ->
            validateExceptionType(clause.exceptionType, clause.location)
            clause.copy(
                handler = processBlockStatement(clause.handler, context)
            )
        }
        
        // 处理 finally 块
        val processedFinallyBlock = statement.finallyBlock?.let {
            processBlockStatement(it, context)
        }
        
        return statement.copy(
            tryBlock = processedTryBlock,
            catchClauses = processedCatchClauses,
            finallyBlock = processedFinallyBlock
        )
    }
    
    // 辅助验证方法
    
    private fun validateBooleanCondition(expression: Expression, location: SourceLocation) {
        val type = inferExpressionType(expression)
        if (!isBooleanType(type)) {
            errorReporter.report(
                ErrorBuilder(
                    "INVALID_CONDITION_TYPE",
                    "条件表达式必须是布尔类型，实际类型: ${type}",
                    location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    private fun validateIterableExpression(expression: Expression, location: SourceLocation) {
        val type = inferExpressionType(expression)
        if (!isIterableType(type)) {
            errorReporter.report(
                ErrorBuilder(
                    "INVALID_ITERABLE_TYPE",
                    "表达式必须是可迭代类型，实际类型: ${type}",
                    location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    private fun validateReturnType(expression: Expression, expectedType: Type, location: SourceLocation) {
        val actualType = inferExpressionType(expression)
        if (!isCompatibleType(actualType, expectedType)) {
            errorReporter.report(
                ErrorBuilder(
                    "INVALID_RETURN_TYPE",
                    "返回值类型不匹配，期望: ${expectedType}，实际: ${actualType}",
                    location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    private fun validatePattern(pattern: Pattern, expressionType: Type, location: SourceLocation) {
        when (pattern) {
            is Pattern.LiteralPattern -> {
                val patternType = inferLiteralType(pattern.value)
                if (!isCompatibleType(patternType, expressionType)) {
                    errorReporter.report(
                        ErrorBuilder(
                            "PATTERN_TYPE_MISMATCH",
                            "模式类型不匹配，期望: ${expressionType}，实际: ${patternType}",
                            location
                        ).category(ErrorCategory.TYPE).build()
                    )
                }
            }
            is Pattern.ConstructorPattern -> {
                validateConstructorPattern(pattern, expressionType, location)
            }
            is Pattern.VariablePattern -> {
                // 变量模式总是匹配
            }
            is Pattern.WildcardPattern -> {
                // 通配符模式总是匹配
            }
        }
    }
    
    private fun validateConstructorPattern(
        pattern: Pattern.ConstructorPattern,
        expressionType: Type,
        location: SourceLocation
    ) {
        // 验证构造器是否存在且类型匹配
        // 这里需要类型系统的支持
    }
    
    private fun validateMatchCompleteness(
        expression: Expression,
        cases: List<MatchCase>,
        location: SourceLocation
    ) {
        // 检查模式匹配是否覆盖了所有可能的情况
        // 这是一个复杂的分析，需要根据表达式类型进行
    }
    
    private fun validateExceptionType(type: Type, location: SourceLocation) {
        // 验证异常类型是否继承自 Exception
        if (!isExceptionType(type)) {
            errorReporter.report(
                ErrorBuilder(
                    "INVALID_EXCEPTION_TYPE",
                    "Catch 子句必须捕获异常类型，实际类型: ${type}",
                    location
                ).category(ErrorCategory.TYPE).build()
            )
        }
    }
    
    // 类型推导和检查的辅助方法（这些应该与类型系统集成）
    
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
    
    private fun isIterableType(type: Type): Boolean {
        return when (type) {
            is CompositeType.ArrayType -> true
            is PrimitiveType.StringType, is PrimitiveType.NullableStringType -> true
            is PrimitiveType.AnyType -> true // 临时允许 AnyType 作为可迭代类型
            else -> false
        }
    }
    
    private fun isExceptionType(type: Type): Boolean {
        // 检查类型是否是异常类型
        return true // 暂时返回 true
    }
    
    private fun isCompatibleType(actual: Type, expected: Type): Boolean {
        // 检查类型兼容性
        return true // 暂时返回 true，应该调用类型检查器
    }
}
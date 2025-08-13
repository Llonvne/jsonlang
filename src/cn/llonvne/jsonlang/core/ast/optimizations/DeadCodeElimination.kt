package cn.llonvne.jsonlang.core.ast.optimizations

import cn.llonvne.jsonlang.core.ast.*

/**
 * 死代码消除优化
 * 移除永远不会执行的代码
 */
class DeadCodeEliminationTransformation : BaseASTTransformation() {
    override val name: String = "DeadCodeElimination"
    override val description: String = "死代码消除优化 - 移除永远不会执行的代码"
    override val priority: Int = 20
    
    override fun shouldApply(node: ASTNode, context: TransformationContext): Boolean {
        return context.options.deadCodeElimination && 
               (node is BlockStatement || node is ConditionalStatement || node is FunctionDeclaration)
    }
    
    override fun transform(node: ASTNode, context: TransformationContext): ASTNode {
        return when (node) {
            is BlockStatement -> eliminateDeadCodeInBlock(node, context)
            is ConditionalStatement -> eliminateDeadCodeInConditional(node, context)
            is FunctionDeclaration -> eliminateDeadCodeInFunction(node, context)
            else -> transformChildren(node, context)
        }
    }
    
    /**
     * 在块语句中消除死代码
     */
    private fun eliminateDeadCodeInBlock(block: BlockStatement, context: TransformationContext): BlockStatement {
        val liveStatements = mutableListOf<Statement>()
        var reachable = true
        
        for (statement in block.statements) {
            if (!reachable) {
                // 不可达代码，跳过
                context.errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = "UNREACHABLE_CODE",
                        message = "检测到不可达代码",
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                        location = statement.location
                    )
                )
                continue
            }
            
            val transformedStatement = transform(statement, context) as Statement
            liveStatements.add(transformedStatement)
            
            // 检查语句是否会终止控制流
            if (isTerminatingStatement(transformedStatement)) {
                reachable = false
            }
        }
        
        return block.copy(statements = liveStatements)
    }
    
    /**
     * 在条件语句中消除死代码
     */
    private fun eliminateDeadCodeInConditional(
        conditional: ConditionalStatement, 
        context: TransformationContext
    ): Statement {
        val transformedCondition = transform(conditional.condition, context) as Expression
        
        // 如果条件是常量，可以直接选择分支
        when (transformedCondition) {
            is LiteralExpression.BooleanLiteral -> {
                return if (transformedCondition.value) {
                    // 条件为真，只保留 then 分支
                    transform(conditional.thenBranch, context) as Statement
                } else {
                    // 条件为假，只保留 else 分支（如果存在）
                    conditional.elseBranch?.let { transform(it, context) as Statement }
                        ?: BlockStatement(emptyList(), conditional.location)
                }
            }
            else -> {
                // 条件不是常量，保留整个条件语句但变换子节点
                return conditional.copy(
                    condition = transformedCondition,
                    thenBranch = transform(conditional.thenBranch, context) as Statement,
                    elseBranch = conditional.elseBranch?.let { transform(it, context) as Statement }
                )
            }
        }
    }
    
    /**
     * 在函数中消除死代码
     */
    private fun eliminateDeadCodeInFunction(
        function: FunctionDeclaration, 
        context: TransformationContext
    ): FunctionDeclaration {
        val transformedBody = function.body.map { transform(it, context) as Statement }
        
        // 检查是否有不可达的语句
        val liveStatements = mutableListOf<Statement>()
        var reachable = true
        
        for (statement in transformedBody) {
            if (!reachable) {
                context.errorReporter.report(
                    cn.llonvne.jsonlang.core.errors.CompilerError(
                        code = "UNREACHABLE_CODE",
                        message = "函数中检测到不可达代码",
                        severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                        category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                        location = statement.location
                    )
                )
                continue
            }
            
            liveStatements.add(statement)
            
            if (isTerminatingStatement(statement)) {
                reachable = false
            }
        }
        
        return function.copy(body = liveStatements)
    }
    
    /**
     * 检查语句是否会终止控制流
     */
    private fun isTerminatingStatement(statement: Statement): Boolean {
        return when (statement) {
            is JumpStatement.ReturnStatement -> true
            is JumpStatement.BreakStatement -> true
            is JumpStatement.ContinueStatement -> true
            is BlockStatement -> statement.statements.any { isTerminatingStatement(it) }
            is ConditionalStatement -> {
                // 只有当两个分支都终止时，条件语句才终止
                val thenTerminates = isTerminatingStatement(statement.thenBranch)
                val elseTerminates = statement.elseBranch?.let { isTerminatingStatement(it) } ?: false
                thenTerminates && elseTerminates
            }
            is TryCatchStatement -> {
                // try-catch 语句的终止性比较复杂，这里简化处理
                isTerminatingStatement(statement.tryBlock)
            }
            else -> false
        }
    }
}

/**
 * 无用变量消除优化
 * 移除未使用的变量声明
 */
class UnusedVariableEliminationTransformation : BaseASTTransformation() {
    override val name: String = "UnusedVariableElimination"
    override val description: String = "无用变量消除优化 - 移除未使用的变量声明"
    override val priority: Int = 25
    
    private val usedVariables = mutableSetOf<String>()
    private val declaredVariables = mutableMapOf<String, VariableDeclaration>()
    
    override fun shouldApply(node: ASTNode, context: TransformationContext): Boolean {
        return context.options.deadCodeElimination && 
               (node is FunctionDeclaration || node is BlockStatement)
    }
    
    override fun transform(node: ASTNode, context: TransformationContext): ASTNode {
        return when (node) {
            is FunctionDeclaration -> eliminateUnusedVariablesInFunction(node, context)
            is BlockStatement -> eliminateUnusedVariablesInBlock(node, context)
            else -> transformChildren(node, context)
        }
    }
    
    /**
     * 在函数中消除未使用的变量
     */
    private fun eliminateUnusedVariablesInFunction(
        function: FunctionDeclaration,
        context: TransformationContext
    ): FunctionDeclaration {
        // 重置状态
        usedVariables.clear()
        declaredVariables.clear()
        
        // 第一遍：收集所有变量声明和使用
        collectVariableInfo(function.body)
        
        // 第二遍：移除未使用的变量声明
        val optimizedBody = function.body.mapNotNull { statement ->
            when (statement) {
                is VariableDeclaration -> {
                    if (usedVariables.contains(statement.name)) {
                        transform(statement, context) as Statement
                    } else {
                        context.errorReporter.report(
                            cn.llonvne.jsonlang.core.errors.CompilerError(
                                code = "UNUSED_VARIABLE",
                                message = "变量 '${statement.name}' 已声明但未使用",
                                severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                                category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                                location = statement.location
                            )
                        )
                        null // 移除未使用的变量声明
                    }
                }
                else -> transform(statement, context) as Statement
            }
        }
        
        return function.copy(body = optimizedBody)
    }
    
    /**
     * 在块语句中消除未使用的变量
     */
    private fun eliminateUnusedVariablesInBlock(
        block: BlockStatement,
        context: TransformationContext
    ): BlockStatement {
        // 重置状态
        usedVariables.clear()
        declaredVariables.clear()
        
        // 收集变量信息
        collectVariableInfo(block.statements)
        
        // 移除未使用的变量声明
        val optimizedStatements = block.statements.mapNotNull { statement ->
            when (statement) {
                is VariableDeclaration -> {
                    if (usedVariables.contains(statement.name)) {
                        transform(statement, context) as Statement
                    } else {
                        context.errorReporter.report(
                            cn.llonvne.jsonlang.core.errors.CompilerError(
                                code = "UNUSED_VARIABLE",
                                message = "变量 '${statement.name}' 已声明但未使用",
                                severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                                category = cn.llonvne.jsonlang.core.errors.ErrorCategory.SEMANTIC,
                                location = statement.location
                            )
                        )
                        null
                    }
                }
                else -> transform(statement, context) as Statement
            }
        }
        
        return block.copy(statements = optimizedStatements)
    }
    
    /**
     * 收集变量声明和使用信息
     */
    private fun collectVariableInfo(statements: List<Statement>) {
        val visitor = VariableUsageVisitor()
        statements.forEach { it.accept(visitor) }
    }
    
    /**
     * 变量使用访问者
     */
    private inner class VariableUsageVisitor : RecursiveASTVisitor<Unit>() {
        override fun defaultVisit(node: ASTNode) {
            // 递归访问子节点
        }
        
        override fun visitVariableDeclaration(node: VariableDeclaration) {
            declaredVariables[node.name] = node
            // 如果变量有初始值，需要访问初始值表达式
            node.value?.accept(this)
        }
        
        override fun visitIdentifierExpression(node: IdentifierExpression) {
            usedVariables.add(node.name)
        }
    }
}
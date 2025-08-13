package cn.llonvne.jsonlang.core.ast.optimizations

import cn.llonvne.jsonlang.core.ast.*

/**
 * 内联优化
 * 将简单函数调用替换为函数体
 */
class InlineOptimizationTransformation : BaseASTTransformation() {
    override val name: String = "InlineOptimization"
    override val description: String = "内联优化 - 将简单函数调用替换为函数体"
    override val priority: Int = 30
    
    private val inlinableFunctions = mutableMapOf<String, FunctionDeclaration>()
    
    override fun shouldApply(node: ASTNode, context: TransformationContext): Boolean {
        return context.options.inlineOptimization && 
               (node is Program || node is CallExpression)
    }
    
    override fun transform(node: ASTNode, context: TransformationContext): ASTNode {
        return when (node) {
            is Program -> {
                // 首先收集可内联的函数
                collectInlinableFunctions(node)
                transformChildren(node, context)
            }
            is CallExpression -> inlineFunction(node, context)
            else -> transformChildren(node, context)
        }
    }
    
    /**
     * 收集可内联的函数
     */
    private fun collectInlinableFunctions(program: Program) {
        inlinableFunctions.clear()
        
        for (function in program.functions) {
            if (isInlinable(function)) {
                inlinableFunctions[function.name] = function
            }
        }
    }
    
    /**
     * 检查函数是否可以内联
     */
    private fun isInlinable(function: FunctionDeclaration): Boolean {
        // 检查内联修饰符
        if (function.modifiers.inline) return true
        
        // 检查函数复杂度
        val complexity = calculateComplexity(function.body)
        if (complexity > 10) return false // 复杂度阈值
        
        // 检查是否有递归调用
        if (hasRecursiveCall(function)) return false
        
        // 检查是否有副作用
        if (hasSideEffects(function.body)) return false
        
        return true
    }
    
    /**
     * 计算函数复杂度
     */
    private fun calculateComplexity(statements: List<Statement>): Int {
        var complexity = 0
        
        for (statement in statements) {
            complexity += when (statement) {
                is VariableDeclaration -> 1
                is ExpressionStatement -> 1
                is ConditionalStatement -> 2 + calculateComplexity(listOf(statement.thenBranch)) +
                    (statement.elseBranch?.let { calculateComplexity(listOf(it)) } ?: 0)
                is LoopStatement -> 3 + when (statement) {
                    is LoopStatement.ForLoop -> calculateComplexity(listOf(statement.body))
                    is LoopStatement.WhileLoop -> calculateComplexity(listOf(statement.body))
                }
                is BlockStatement -> calculateComplexity(statement.statements)
                is TryCatchStatement -> 3 + calculateComplexity(statement.tryBlock.statements)
                is JumpStatement -> 1
                is MatchStatement -> 2 + statement.cases.size
                else -> 1
            }
        }
        
        return complexity
    }
    
    /**
     * 检查是否有递归调用
     */
    private fun hasRecursiveCall(function: FunctionDeclaration): Boolean {
        val visitor = RecursiveCallVisitor(function.name)
        function.body.forEach { it.accept(visitor) }
        return visitor.hasRecursiveCall
    }
    
    /**
     * 检查是否有副作用
     */
    private fun hasSideEffects(statements: List<Statement>): Boolean {
        // 简化实现：假设所有函数调用都有副作用
        val visitor = SideEffectVisitor()
        statements.forEach { it.accept(visitor) }
        return visitor.hasSideEffects
    }
    
    /**
     * 内联函数调用
     */
    private fun inlineFunction(call: CallExpression, context: TransformationContext): Expression {
        // 先变换子节点
        val transformedCall = transformChildren(call, context) as CallExpression
        
        // 检查是否为简单的函数调用
        if (transformedCall.function !is IdentifierExpression) {
            return transformedCall
        }
        
        val functionName = (transformedCall.function as IdentifierExpression).name
        val function = inlinableFunctions[functionName] ?: return transformedCall
        
        // 执行内联
        return performInline(function, transformedCall, context)
    }
    
    /**
     * 执行内联操作
     */
    private fun performInline(
        function: FunctionDeclaration,
        call: CallExpression,
        context: TransformationContext
    ): Expression {
        // 创建参数替换映射
        val parameterMap = mutableMapOf<String, Expression>()
        
        for (i in function.parameters.indices) {
            val parameter = function.parameters[i]
            val argument = if (i < call.arguments.size) {
                call.arguments[i].value
            } else {
                parameter.defaultValue ?: continue
            }
            parameterMap[parameter.name] = argument
        }
        
        // 内联函数体
        val inlinedBody = inlineFunctionBody(function.body, parameterMap, context)
        
        // 如果函数体只有一个返回语句，直接返回表达式
        if (inlinedBody.size == 1) {
            val statement = inlinedBody[0]
            if (statement is JumpStatement.ReturnStatement && statement.value != null) {
                return statement.value
            }
        }
        
        // 简化处理：如果函数体复杂，不进行内联
        return call
    }
    
    /**
     * 内联函数体
     */
    private fun inlineFunctionBody(
        body: List<Statement>,
        parameterMap: Map<String, Expression>,
        context: TransformationContext
    ): List<Statement> {
        val inliner = ParameterInliner(parameterMap)
        return body.map { it.accept(inliner) as Statement }
    }
    
    /**
     * 参数内联访问者
     */
    private class ParameterInliner(
        private val parameterMap: Map<String, Expression>
    ) : RecursiveASTVisitor<ASTNode>() {
        
        override fun defaultVisit(node: ASTNode): ASTNode = node
        
        override fun visitIdentifierExpression(node: IdentifierExpression): ASTNode {
            return parameterMap[node.name] ?: node
        }
        
        override fun visitVariableDeclaration(node: VariableDeclaration): ASTNode {
            return node.copy(
                value = node.value?.let { it.accept(this) as Expression }
            )
        }
        
        override fun visitBinaryExpression(node: BinaryExpression): ASTNode {
            return node.copy(
                left = node.left.accept(this) as Expression,
                right = node.right.accept(this) as Expression
            )
        }
        
        override fun visitCallExpression(node: CallExpression): ASTNode {
            return node.copy(
                function = node.function.accept(this) as Expression,
                arguments = node.arguments.map { 
                    it.copy(value = it.value.accept(this) as Expression)
                }
            )
        }
    }
    
    /**
     * 递归调用检测访问者
     */
    private class RecursiveCallVisitor(
        private val functionName: String
    ) : RecursiveASTVisitor<Unit>() {
        var hasRecursiveCall = false
        
        override fun defaultVisit(node: ASTNode) {}
        
        override fun visitCallExpression(node: CallExpression) {
            if (node.function is IdentifierExpression) {
                val calledFunction = (node.function as IdentifierExpression).name
                if (calledFunction == functionName) {
                    hasRecursiveCall = true
                }
            }
            super.visitCallExpression(node)
        }
    }
    
    /**
     * 副作用检测访问者
     */
    private class SideEffectVisitor : RecursiveASTVisitor<Unit>() {
        var hasSideEffects = false
        
        override fun defaultVisit(node: ASTNode) {}
        
        override fun visitCallExpression(node: CallExpression) {
            // 假设所有函数调用都有副作用（简化实现）
            hasSideEffects = true
            super.visitCallExpression(node)
        }
        
        override fun visitBinaryExpression(node: BinaryExpression) {
            // 检查赋值操作
            if (node.operator in listOf(
                BinaryOperator.ASSIGN,
                BinaryOperator.PLUS_ASSIGN,
                BinaryOperator.MINUS_ASSIGN,
                BinaryOperator.MULTIPLY_ASSIGN,
                BinaryOperator.DIVIDE_ASSIGN
            )) {
                hasSideEffects = true
            }
            super.visitBinaryExpression(node)
        }
    }
}

// 注意：由于 Expression 是密封类，我们不能直接扩展它
// 这里简化处理，直接返回函数调用而不进行复杂的内联
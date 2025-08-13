package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.types.Type

/**
 * AST 操作 API
 * 提供 AST 节点的创建、修改和查询功能
 */
class ASTOperations {
    
    /**
     * AST 节点构建器
     */
    class ASTBuilder(private val location: SourceLocation = SourceLocation.unknown()) {
        
        // 字面量构建器
        fun stringLiteral(value: String, annotations: List<Annotation> = emptyList()): LiteralExpression.StringLiteral {
            return LiteralExpression.StringLiteral(value, location, annotations)
        }
        
        fun intLiteral(value: Long, annotations: List<Annotation> = emptyList()): LiteralExpression.IntLiteral {
            return LiteralExpression.IntLiteral(value, location, annotations)
        }
        
        fun floatLiteral(value: Double, annotations: List<Annotation> = emptyList()): LiteralExpression.FloatLiteral {
            return LiteralExpression.FloatLiteral(value, location, annotations)
        }
        
        fun booleanLiteral(value: Boolean, annotations: List<Annotation> = emptyList()): LiteralExpression.BooleanLiteral {
            return LiteralExpression.BooleanLiteral(value, location, annotations)
        }
        
        fun nullLiteral(annotations: List<Annotation> = emptyList()): LiteralExpression.NullLiteral {
            return LiteralExpression.NullLiteral(location, annotations)
        }
        
        // 标识符构建器
        fun identifier(name: String, annotations: List<Annotation> = emptyList()): IdentifierExpression {
            return IdentifierExpression(name, location, annotations)
        }
        
        // 二元表达式构建器
        fun binaryExpression(
            left: Expression,
            operator: BinaryOperator,
            right: Expression,
            annotations: List<Annotation> = emptyList()
        ): BinaryExpression {
            return BinaryExpression(left, operator, right, location, annotations)
        }
        
        // 一元表达式构建器
        fun unaryExpression(
            operator: UnaryOperator,
            operand: Expression,
            annotations: List<Annotation> = emptyList()
        ): UnaryExpression {
            return UnaryExpression(operator, operand, location, annotations)
        }
        
        // 函数调用构建器
        fun callExpression(
            function: Expression,
            arguments: List<Argument>,
            annotations: List<Annotation> = emptyList()
        ): CallExpression {
            return CallExpression(function, arguments, location, annotations)
        }
        
        // 成员访问构建器
        fun memberExpression(
            target: Expression,
            property: String,
            computed: Boolean = false,
            annotations: List<Annotation> = emptyList()
        ): MemberExpression {
            return MemberExpression(target, property, computed, location, annotations)
        }
        
        // 数组构建器
        fun arrayExpression(
            elements: List<Expression>,
            annotations: List<Annotation> = emptyList()
        ): ArrayExpression {
            return ArrayExpression(elements, location, annotations)
        }
        
        // 对象构建器
        fun objectExpression(
            properties: List<ObjectProperty>,
            annotations: List<Annotation> = emptyList()
        ): ObjectExpression {
            return ObjectExpression(properties, location, annotations)
        }
        
        // Lambda 构建器
        fun lambdaExpression(
            parameters: List<Parameter>,
            body: Expression,
            annotations: List<Annotation> = emptyList()
        ): LambdaExpression {
            return LambdaExpression(parameters, body, location, annotations)
        }
        
        // 条件表达式构建器
        fun conditionalExpression(
            condition: Expression,
            thenExpression: Expression,
            elseExpression: Expression,
            annotations: List<Annotation> = emptyList()
        ): ConditionalExpression {
            return ConditionalExpression(condition, thenExpression, elseExpression, location, annotations)
        }
        
        // 类型转换构建器
        fun castExpression(
            expression: Expression,
            targetType: Type,
            safe: Boolean = false,
            annotations: List<Annotation> = emptyList()
        ): CastExpression {
            return CastExpression(expression, targetType, safe, location, annotations)
        }
        
        // 类型检查构建器
        fun typeCheckExpression(
            expression: Expression,
            type: Type,
            annotations: List<Annotation> = emptyList()
        ): TypeCheckExpression {
            return TypeCheckExpression(expression, type, location, annotations)
        }
        
        // 宏调用构建器
        fun macroCall(
            name: String,
            arguments: List<ASTNode>,
            annotations: List<Annotation> = emptyList()
        ): MacroCall {
            return MacroCall(name, arguments, location, annotations)
        }
        
        // 宏参数占位符构建器
        fun macroParameterPlaceholder(
            parameterName: String,
            annotations: List<Annotation> = emptyList()
        ): MacroParameterPlaceholder {
            return MacroParameterPlaceholder(parameterName, location, annotations)
        }
        
        // 辅助方法：创建参数
        fun argument(name: String? = null, value: Expression): Argument {
            return Argument(name, value)
        }
        
        // 辅助方法：创建对象属性
        fun objectProperty(key: Expression, value: Expression, computed: Boolean = false): ObjectProperty {
            return ObjectProperty(key, value, computed)
        }
        
        // 辅助方法：创建参数定义
        fun parameter(
            name: String,
            type: Type,
            optional: Boolean = false,
            defaultValue: Expression? = null,
            annotations: List<Annotation> = emptyList()
        ): Parameter {
            return Parameter(name, type, optional, defaultValue, annotations)
        }
    }
    
    /**
     * AST 节点修改器
     */
    class ASTModifier {
        
        /**
         * 替换表达式中的子节点
         */
        fun replaceExpression(node: Expression, target: Expression, replacement: Expression): Expression {
            return when (node) {
                is BinaryExpression -> {
                    val newLeft = if (node.left == target) replacement else replaceExpression(node.left, target, replacement)
                    val newRight = if (node.right == target) replacement else replaceExpression(node.right, target, replacement)
                    node.copy(left = newLeft, right = newRight)
                }
                is UnaryExpression -> {
                    val newOperand = if (node.operand == target) replacement else replaceExpression(node.operand, target, replacement)
                    node.copy(operand = newOperand)
                }
                is CallExpression -> {
                    val newFunction = if (node.function == target) replacement else replaceExpression(node.function, target, replacement)
                    val newArguments = node.arguments.map { arg ->
                        val newValue = if (arg.value == target) replacement else replaceExpression(arg.value, target, replacement)
                        arg.copy(value = newValue)
                    }
                    node.copy(function = newFunction, arguments = newArguments)
                }
                is MemberExpression -> {
                    val newTarget = if (node.target == target) replacement else replaceExpression(node.target, target, replacement)
                    node.copy(target = newTarget)
                }
                is ArrayExpression -> {
                    val newElements = node.elements.map { element ->
                        if (element == target) replacement else replaceExpression(element, target, replacement)
                    }
                    node.copy(elements = newElements)
                }
                is ObjectExpression -> {
                    val newProperties = node.properties.map { prop ->
                        val newKey = if (prop.key == target) replacement else replaceExpression(prop.key, target, replacement)
                        val newValue = if (prop.value == target) replacement else replaceExpression(prop.value, target, replacement)
                        prop.copy(key = newKey, value = newValue)
                    }
                    node.copy(properties = newProperties)
                }
                is LambdaExpression -> {
                    val newBody = if (node.body == target) replacement else replaceExpression(node.body, target, replacement)
                    node.copy(body = newBody)
                }
                is ConditionalExpression -> {
                    val newCondition = if (node.condition == target) replacement else replaceExpression(node.condition, target, replacement)
                    val newThen = if (node.thenExpression == target) replacement else replaceExpression(node.thenExpression, target, replacement)
                    val newElse = if (node.elseExpression == target) replacement else replaceExpression(node.elseExpression, target, replacement)
                    node.copy(condition = newCondition, thenExpression = newThen, elseExpression = newElse)
                }
                is CastExpression -> {
                    val newExpression = if (node.expression == target) replacement else replaceExpression(node.expression, target, replacement)
                    node.copy(expression = newExpression)
                }
                is TypeCheckExpression -> {
                    val newExpression = if (node.expression == target) replacement else replaceExpression(node.expression, target, replacement)
                    node.copy(expression = newExpression)
                }
                else -> if (node == target) replacement else node
            }
        }
        
        /**
         * 添加注解到节点
         */
        fun addAnnotation(node: ASTNode, annotation: Annotation): ASTNode {
            return when (node) {
                is Expression -> addAnnotationToExpression(node, annotation)
                is Statement -> addAnnotationToStatement(node, annotation)
                is Declaration -> addAnnotationToDeclaration(node, annotation)
                else -> node
            }
        }
        
        private fun addAnnotationToExpression(expr: Expression, annotation: Annotation): Expression {
            val newAnnotations = expr.annotations + annotation
            return when (expr) {
                is LiteralExpression.StringLiteral -> expr.copy(annotations = newAnnotations)
                is LiteralExpression.IntLiteral -> expr.copy(annotations = newAnnotations)
                is LiteralExpression.FloatLiteral -> expr.copy(annotations = newAnnotations)
                is LiteralExpression.BooleanLiteral -> expr.copy(annotations = newAnnotations)
                is LiteralExpression.NullLiteral -> expr.copy(annotations = newAnnotations)
                is IdentifierExpression -> expr.copy(annotations = newAnnotations)
                is BinaryExpression -> expr.copy(annotations = newAnnotations)
                is UnaryExpression -> expr.copy(annotations = newAnnotations)
                is CallExpression -> expr.copy(annotations = newAnnotations)
                is MemberExpression -> expr.copy(annotations = newAnnotations)
                is ArrayExpression -> expr.copy(annotations = newAnnotations)
                is ObjectExpression -> expr.copy(annotations = newAnnotations)
                is LambdaExpression -> expr.copy(annotations = newAnnotations)
                is ConditionalExpression -> expr.copy(annotations = newAnnotations)
                is CastExpression -> expr.copy(annotations = newAnnotations)
                is TypeCheckExpression -> expr.copy(annotations = newAnnotations)
                is MacroCall -> expr.copy(annotations = newAnnotations)
                is MacroParameterPlaceholder -> expr.copy(annotations = newAnnotations)
                else -> expr
            }
        }
        
        private fun addAnnotationToStatement(stmt: Statement, annotation: Annotation): Statement {
            // 这里需要根据实际的 Statement 类型来实现
            // 由于 Statement 类还没有完全定义，这里先返回原节点
            return stmt
        }
        
        private fun addAnnotationToDeclaration(decl: Declaration, annotation: Annotation): Declaration {
            val newAnnotations = decl.annotations + annotation
            return when (decl) {
                is ImportDeclaration -> decl.copy(annotations = newAnnotations)
                is ExportDeclaration -> decl.copy(annotations = newAnnotations)
                is TypeDeclaration -> decl.copy(annotations = newAnnotations)
                is ConstantDeclaration -> decl.copy(annotations = newAnnotations)
                is FunctionDeclaration -> decl.copy(annotations = newAnnotations)
                is MacroDeclaration -> decl.copy(annotations = newAnnotations)
                is DSLDeclaration -> decl.copy(annotations = newAnnotations)
                else -> decl
            }
        }
        
        /**
         * 更新节点的类型信息
         */
        fun updateType(expr: Expression, newType: Type): Expression {
            return when (expr) {
                is LiteralExpression.StringLiteral -> expr.copy(inferredType = newType)
                is LiteralExpression.IntLiteral -> expr.copy(inferredType = newType)
                is LiteralExpression.FloatLiteral -> expr.copy(inferredType = newType)
                is LiteralExpression.BooleanLiteral -> expr.copy(inferredType = newType)
                is LiteralExpression.NullLiteral -> expr.copy(inferredType = newType)
                is IdentifierExpression -> expr.copy(inferredType = newType)
                is BinaryExpression -> expr.copy(inferredType = newType)
                is UnaryExpression -> expr.copy(inferredType = newType)
                is CallExpression -> expr.copy(inferredType = newType)
                is MemberExpression -> expr.copy(inferredType = newType)
                is ArrayExpression -> expr.copy(inferredType = newType)
                is ObjectExpression -> expr.copy(inferredType = newType)
                is LambdaExpression -> expr.copy(inferredType = newType)
                is ConditionalExpression -> expr.copy(inferredType = newType)
                is CastExpression -> expr.copy(inferredType = newType)
                is TypeCheckExpression -> expr.copy(inferredType = newType)
                is MacroCall -> expr.copy(inferredType = newType)
                is MacroParameterPlaceholder -> expr.copy(inferredType = newType)
                else -> expr
            }
        }
    }
    
    /**
     * AST 查询器
     */
    class ASTQuery {
        
        /**
         * 查找所有指定类型的节点
         */
        inline fun <reified T : ASTNode> findAll(root: ASTNode): List<T> {
            val results = mutableListOf<T>()
            val visitor = object : RecursiveASTVisitor<Unit>() {
                override fun defaultVisit(node: ASTNode) {
                    if (node is T) {
                        results.add(node)
                    }
                }
            }
            root.accept(visitor)
            return results
        }
        
        /**
         * 查找第一个指定类型的节点
         */
        inline fun <reified T : ASTNode> findFirst(root: ASTNode): T? {
            var result: T? = null
            val visitor = object : RecursiveASTVisitor<Unit>() {
                override fun defaultVisit(node: ASTNode) {
                    if (result == null && node is T) {
                        result = node
                    }
                }
            }
            root.accept(visitor)
            return result
        }
        
        /**
         * 查找所有标识符引用
         */
        fun findIdentifierReferences(root: ASTNode, name: String): List<IdentifierExpression> {
            return findAll<IdentifierExpression>(root).filter { it.name == name }
        }
        
        /**
         * 查找所有函数调用
         */
        fun findFunctionCalls(root: ASTNode, functionName: String? = null): List<CallExpression> {
            val calls = findAll<CallExpression>(root)
            return if (functionName != null) {
                calls.filter { call ->
                    when (val func = call.function) {
                        is IdentifierExpression -> func.name == functionName
                        is MemberExpression -> func.property == functionName
                        else -> false
                    }
                }
            } else {
                calls
            }
        }
        
        /**
         * 查找所有宏调用
         */
        fun findMacroCalls(root: ASTNode, macroName: String? = null): List<MacroCall> {
            val calls = findAll<MacroCall>(root)
            return if (macroName != null) {
                calls.filter { it.name == macroName }
            } else {
                calls
            }
        }
        
        /**
         * 检查节点是否包含指定的子节点
         */
        fun contains(root: ASTNode, target: ASTNode): Boolean {
            var found = false
            val visitor = object : RecursiveASTVisitor<Unit>() {
                override fun defaultVisit(node: ASTNode) {
                    if (node == target) {
                        found = true
                    }
                }
            }
            root.accept(visitor)
            return found
        }
        
        /**
         * 获取节点的深度
         */
        fun getDepth(root: ASTNode, target: ASTNode): Int? {
            var depth: Int? = null
            
            fun search(node: ASTNode, currentDepth: Int): Boolean {
                if (node == target) {
                    depth = currentDepth
                    return true
                }
                
                // 递归搜索子节点
                when (node) {
                    is BinaryExpression -> {
                        if (search(node.left, currentDepth + 1) || search(node.right, currentDepth + 1)) {
                            return true
                        }
                    }
                    is UnaryExpression -> {
                        if (search(node.operand, currentDepth + 1)) {
                            return true
                        }
                    }
                    is CallExpression -> {
                        if (search(node.function, currentDepth + 1)) {
                            return true
                        }
                        for (arg in node.arguments) {
                            if (search(arg.value, currentDepth + 1)) {
                                return true
                            }
                        }
                    }
                    // 可以继续添加其他节点类型的处理
                    else -> {
                        // 对于其他节点类型，暂时不进行递归搜索
                    }
                }
                
                return false
            }
            
            search(root, 0)
            return depth
        }
        
        /**
         * 获取节点的父节点
         */
        fun getParent(root: ASTNode, target: ASTNode): ASTNode? {
            var parent: ASTNode? = null
            
            fun search(node: ASTNode): Boolean {
                when (node) {
                    is BinaryExpression -> {
                        if (node.left == target || node.right == target) {
                            parent = node
                            return true
                        }
                        if (search(node.left) || search(node.right)) {
                            return true
                        }
                    }
                    is UnaryExpression -> {
                        if (node.operand == target) {
                            parent = node
                            return true
                        }
                        if (search(node.operand)) {
                            return true
                        }
                    }
                    is CallExpression -> {
                        if (node.function == target) {
                            parent = node
                            return true
                        }
                        if (search(node.function)) {
                            return true
                        }
                        for (arg in node.arguments) {
                            if (arg.value == target) {
                                parent = node
                                return true
                            }
                            if (search(arg.value)) {
                                return true
                            }
                        }
                    }
                    // 可以继续添加其他节点类型的处理
                    else -> {
                        // 对于其他节点类型，暂时不进行递归搜索
                    }
                }
                
                return false
            }
            
            search(root)
            return parent
        }
    }
    
    companion object {
        /**
         * 创建 AST 构建器
         */
        fun builder(location: SourceLocation = SourceLocation.unknown()): ASTBuilder {
            return ASTBuilder(location)
        }
        
        /**
         * 创建 AST 修改器
         */
        fun modifier(): ASTModifier {
            return ASTModifier()
        }
        
        /**
         * 创建 AST 查询器
         */
        fun query(): ASTQuery {
            return ASTQuery()
        }
    }
}
package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.errors.ErrorReporter

/**
 * AST 变换引擎
 * 负责管理和执行 AST 变换
 */
class TransformationEngine(
    private val errorReporter: ErrorReporter
) {
    private val transformations = mutableListOf<ASTTransformation>()
    
    /**
     * 注册变换
     */
    fun registerTransformation(transformation: ASTTransformation) {
        transformations.add(transformation)
        transformations.sortBy { it.priority }
    }
    
    /**
     * 注册多个变换
     */
    fun registerTransformations(vararg transformations: ASTTransformation) {
        transformations.forEach { registerTransformation(it) }
    }
    
    /**
     * 移除变换
     */
    fun removeTransformation(name: String) {
        transformations.removeAll { it.name == name }
    }
    
    /**
     * 获取所有已注册的变换
     */
    fun getTransformations(): List<ASTTransformation> = transformations.toList()
    
    /**
     * 应用所有变换到程序
     */
    fun transform(program: Program, options: TransformationOptions = TransformationOptions()): Program {
        val context = TransformationContext(program, errorReporter, options)
        
        var currentProgram = program
        var iteration = 0
        var hasChanges = true
        
        while (hasChanges && iteration < options.maxIterations) {
            hasChanges = false
            val previousProgram = currentProgram
            
            for (transformation in transformations.filter { it.enabled }) {
                if (transformation.shouldApply(currentProgram, context)) {
                    val transformedProgram = transformation.transform(currentProgram, context) as Program
                    
                    if (transformedProgram != currentProgram) {
                        currentProgram = transformedProgram
                        context.recordTransformation(transformation.name)
                        hasChanges = true
                    }
                }
            }
            
            iteration++
        }
        
        if (iteration >= options.maxIterations) {
            errorReporter.report(
                cn.llonvne.jsonlang.core.errors.CompilerError(
                    code = "TRANSFORM_MAX_ITERATIONS",
                    message = "变换引擎达到最大迭代次数 ${options.maxIterations}，可能存在无限循环",
                    severity = cn.llonvne.jsonlang.core.errors.ErrorSeverity.WARNING,
                    category = cn.llonvne.jsonlang.core.errors.ErrorCategory.INTERNAL,
                    location = program.location
                )
            )
        }
        
        return currentProgram
    }
    
    /**
     * 应用单个变换到节点
     */
    fun applyTransformation(
        node: ASTNode, 
        transformationName: String, 
        context: TransformationContext
    ): ASTNode {
        val transformation = transformations.find { it.name == transformationName }
            ?: throw IllegalArgumentException("未找到变换: $transformationName")
        
        return if (transformation.shouldApply(node, context)) {
            transformation.transform(node, context)
        } else {
            node
        }
    }
    
    /**
     * 递归应用变换到节点及其子节点
     */
    fun transformRecursively(node: ASTNode, context: TransformationContext): ASTNode {
        val visitor = TransformationVisitor(this, context)
        return node.accept(visitor)
    }
}

/**
 * 变换访问者
 * 递归地对 AST 节点应用变换
 */
private class TransformationVisitor(
    private val engine: TransformationEngine,
    private val context: TransformationContext
) : RecursiveASTVisitor<ASTNode>() {
    
    override fun defaultVisit(node: ASTNode): ASTNode {
        context.statistics.recordNodeType(node.nodeType)
        
        // 先变换子节点
        val nodeWithTransformedChildren = node
        
        // 然后对当前节点应用变换
        var transformedNode = nodeWithTransformedChildren
        for (transformation in engine.getTransformations().filter { it.enabled }) {
            if (transformation.shouldApply(transformedNode, context)) {
                transformedNode = transformation.transform(transformedNode, context)
                context.recordTransformation(transformation.name)
            }
        }
        
        return transformedNode
    }
    
    override fun visitProgram(node: Program): ASTNode {
        context.enterScope()
        val result = super.visitProgram(node)
        context.exitScope()
        return result
    }
    
    override fun visitFunctionDeclaration(node: FunctionDeclaration): ASTNode {
        context.enterScope()
        context.symbolTable[node.name] = node
        val result = super.visitFunctionDeclaration(node)
        context.exitScope()
        return result
    }
    
    override fun visitBlockStatement(node: BlockStatement): ASTNode {
        context.enterScope()
        val result = super.visitBlockStatement(node)
        context.exitScope()
        return result
    }
}

/**
 * 变换结果
 * 包含变换后的程序和统计信息
 */
data class TransformationResult(
    val program: Program,
    val statistics: TransformationStatistics,
    val appliedTransformations: List<String>
) {
    /** 是否有变换被应用 */
    val hasTransformations: Boolean get() = appliedTransformations.isNotEmpty()
    
    /** 总变换次数 */
    val totalTransformations: Int get() = statistics.getTotalTransformations()
}

/**
 * 变换构建器
 * 用于方便地构建和配置变换引擎
 */
class TransformationEngineBuilder {
    private val transformations = mutableListOf<ASTTransformation>()
    private var errorReporter: ErrorReporter? = null
    
    fun withErrorReporter(reporter: ErrorReporter): TransformationEngineBuilder {
        this.errorReporter = reporter
        return this
    }
    
    fun addTransformation(transformation: ASTTransformation): TransformationEngineBuilder {
        transformations.add(transformation)
        return this
    }
    
    fun addOptimizations(): TransformationEngineBuilder {
        // 将在后续实现中添加优化变换
        return this
    }
    
    fun addValidations(): TransformationEngineBuilder {
        // 将在后续实现中添加验证变换
        return this
    }
    
    fun build(): TransformationEngine {
        val reporter = errorReporter ?: throw IllegalStateException("ErrorReporter 必须设置")
        val engine = TransformationEngine(reporter)
        
        transformations.sortedBy { it.priority }.forEach { engine.registerTransformation(it) }
        
        return engine
    }
}
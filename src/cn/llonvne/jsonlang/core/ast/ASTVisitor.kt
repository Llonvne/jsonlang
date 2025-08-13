package cn.llonvne.jsonlang.core.ast

/**
 * AST 访问者接口
 * 实现访问者模式，用于遍历和处理 AST 节点
 */
interface ASTVisitor<T> {
    // 程序和元数据
    fun visitProgram(node: Program): T
    fun visitMetadata(node: Metadata): T
    
    // 声明节点
    fun visitImportDeclaration(node: ImportDeclaration): T
    fun visitExportDeclaration(node: ExportDeclaration): T
    fun visitTypeDeclaration(node: TypeDeclaration): T
    fun visitConstantDeclaration(node: ConstantDeclaration): T
    fun visitFunctionDeclaration(node: FunctionDeclaration): T
    fun visitMacroDeclaration(node: MacroDeclaration): T
    fun visitDSLDeclaration(node: DSLDeclaration): T
    
    // 语句节点
    fun visitVariableDeclaration(node: VariableDeclaration): T
    fun visitExpressionStatement(node: ExpressionStatement): T
    fun visitConditionalStatement(node: ConditionalStatement): T
    fun visitForLoop(node: LoopStatement.ForLoop): T
    fun visitWhileLoop(node: LoopStatement.WhileLoop): T
    fun visitReturnStatement(node: JumpStatement.ReturnStatement): T
    fun visitBreakStatement(node: JumpStatement.BreakStatement): T
    fun visitContinueStatement(node: JumpStatement.ContinueStatement): T
    fun visitTryCatchStatement(node: TryCatchStatement): T
    fun visitBlockStatement(node: BlockStatement): T
    fun visitMatchStatement(node: MatchStatement): T
    
    // 表达式节点
    fun visitStringLiteral(node: LiteralExpression.StringLiteral): T
    fun visitIntLiteral(node: LiteralExpression.IntLiteral): T
    fun visitFloatLiteral(node: LiteralExpression.FloatLiteral): T
    fun visitBooleanLiteral(node: LiteralExpression.BooleanLiteral): T
    fun visitNullLiteral(node: LiteralExpression.NullLiteral): T
    fun visitIdentifierExpression(node: IdentifierExpression): T
    fun visitBinaryExpression(node: BinaryExpression): T
    fun visitUnaryExpression(node: UnaryExpression): T
    fun visitCallExpression(node: CallExpression): T
    fun visitMemberExpression(node: MemberExpression): T
    fun visitArrayExpression(node: ArrayExpression): T
    fun visitObjectExpression(node: ObjectExpression): T
    fun visitLambdaExpression(node: LambdaExpression): T
    fun visitConditionalExpression(node: ConditionalExpression): T
    fun visitCastExpression(node: CastExpression): T
    fun visitTypeCheckExpression(node: TypeCheckExpression): T
    
    // 宏相关节点
    fun visitMacroCall(node: MacroCall): T
}

/**
 * 抽象 AST 访问者基类
 * 提供默认实现，子类只需重写感兴趣的方法
 */
abstract class BaseASTVisitor<T> : ASTVisitor<T> {
    protected abstract fun defaultVisit(node: ASTNode): T
    
    override fun visitProgram(node: Program): T = defaultVisit(node)
    override fun visitMetadata(node: Metadata): T = defaultVisit(node)
    
    override fun visitImportDeclaration(node: ImportDeclaration): T = defaultVisit(node)
    override fun visitExportDeclaration(node: ExportDeclaration): T = defaultVisit(node)
    override fun visitTypeDeclaration(node: TypeDeclaration): T = defaultVisit(node)
    override fun visitConstantDeclaration(node: ConstantDeclaration): T = defaultVisit(node)
    override fun visitFunctionDeclaration(node: FunctionDeclaration): T = defaultVisit(node)
    override fun visitMacroDeclaration(node: MacroDeclaration): T = defaultVisit(node)
    override fun visitDSLDeclaration(node: DSLDeclaration): T = defaultVisit(node)
    
    override fun visitVariableDeclaration(node: VariableDeclaration): T = defaultVisit(node)
    override fun visitExpressionStatement(node: ExpressionStatement): T = defaultVisit(node)
    override fun visitConditionalStatement(node: ConditionalStatement): T = defaultVisit(node)
    override fun visitForLoop(node: LoopStatement.ForLoop): T = defaultVisit(node)
    override fun visitWhileLoop(node: LoopStatement.WhileLoop): T = defaultVisit(node)
    override fun visitReturnStatement(node: JumpStatement.ReturnStatement): T = defaultVisit(node)
    override fun visitBreakStatement(node: JumpStatement.BreakStatement): T = defaultVisit(node)
    override fun visitContinueStatement(node: JumpStatement.ContinueStatement): T = defaultVisit(node)
    override fun visitTryCatchStatement(node: TryCatchStatement): T = defaultVisit(node)
    override fun visitBlockStatement(node: BlockStatement): T = defaultVisit(node)
    override fun visitMatchStatement(node: MatchStatement): T = defaultVisit(node)
    
    override fun visitStringLiteral(node: LiteralExpression.StringLiteral): T = defaultVisit(node)
    override fun visitIntLiteral(node: LiteralExpression.IntLiteral): T = defaultVisit(node)
    override fun visitFloatLiteral(node: LiteralExpression.FloatLiteral): T = defaultVisit(node)
    override fun visitBooleanLiteral(node: LiteralExpression.BooleanLiteral): T = defaultVisit(node)
    override fun visitNullLiteral(node: LiteralExpression.NullLiteral): T = defaultVisit(node)
    override fun visitIdentifierExpression(node: IdentifierExpression): T = defaultVisit(node)
    override fun visitBinaryExpression(node: BinaryExpression): T = defaultVisit(node)
    override fun visitUnaryExpression(node: UnaryExpression): T = defaultVisit(node)
    override fun visitCallExpression(node: CallExpression): T = defaultVisit(node)
    override fun visitMemberExpression(node: MemberExpression): T = defaultVisit(node)
    override fun visitArrayExpression(node: ArrayExpression): T = defaultVisit(node)
    override fun visitObjectExpression(node: ObjectExpression): T = defaultVisit(node)
    override fun visitLambdaExpression(node: LambdaExpression): T = defaultVisit(node)
    override fun visitConditionalExpression(node: ConditionalExpression): T = defaultVisit(node)
    override fun visitCastExpression(node: CastExpression): T = defaultVisit(node)
    override fun visitTypeCheckExpression(node: TypeCheckExpression): T = defaultVisit(node)
    
    override fun visitMacroCall(node: MacroCall): T = defaultVisit(node)
}

/**
 * 递归 AST 访问者
 * 自动遍历所有子节点
 */
abstract class RecursiveASTVisitor<T> : BaseASTVisitor<T>() {
    
    override fun visitProgram(node: Program): T {
        node.metadata.accept(this)
        node.imports.forEach { it.accept(this) }
        node.exports.forEach { it.accept(this) }
        node.types.forEach { it.accept(this) }
        node.constants.forEach { it.accept(this) }
        node.functions.forEach { it.accept(this) }
        node.macros.forEach { it.accept(this) }
        node.dsls.forEach { it.accept(this) }
        return defaultVisit(node)
    }
    
    override fun visitFunctionDeclaration(node: FunctionDeclaration): T {
        node.body.forEach { it.accept(this) }
        return defaultVisit(node)
    }
    
    override fun visitBlockStatement(node: BlockStatement): T {
        node.statements.forEach { it.accept(this) }
        return defaultVisit(node)
    }
    
    override fun visitBinaryExpression(node: BinaryExpression): T {
        node.left.accept(this)
        node.right.accept(this)
        return defaultVisit(node)
    }
    
    override fun visitUnaryExpression(node: UnaryExpression): T {
        node.operand.accept(this)
        return defaultVisit(node)
    }
    
    override fun visitCallExpression(node: CallExpression): T {
        node.function.accept(this)
        node.arguments.forEach { it.value.accept(this) }
        return defaultVisit(node)
    }
    
    // 其他需要递归访问的节点...
}
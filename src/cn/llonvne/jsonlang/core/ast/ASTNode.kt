package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import kotlinx.serialization.Serializable

/**
 * JSONLang 抽象语法树节点基类
 * 所有 AST 节点都必须继承此类
 */
@Serializable
sealed class ASTNode {
    /** 源码位置信息 */
    abstract val location: SourceLocation
    
    /** 节点注解列表 */
    abstract val annotations: List<Annotation>
    
    /** 节点类型标识 */
    abstract val nodeType: String
    
    /** 访问者模式支持 */
    abstract fun <T> accept(visitor: ASTVisitor<T>): T
}

/**
 * 程序根节点
 */
@Serializable
data class Program(
    val metadata: Metadata,
    val imports: List<ImportDeclaration>,
    val exports: List<ExportDeclaration>,
    val types: List<TypeDeclaration>,
    val constants: List<ConstantDeclaration>,
    val functions: List<FunctionDeclaration>,
    val macros: List<MacroDeclaration>,
    val dsls: List<DSLDeclaration>,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : ASTNode() {
    override val nodeType: String = "Program"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitProgram(this)
}

/**
 * 元数据节点
 */
@Serializable
data class Metadata(
    val name: String,
    val version: String,
    val description: String? = null,
    val author: String? = null,
    val license: String? = null,
    val targets: List<String> = emptyList(),
    val encoding: String = "utf-8",
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : ASTNode() {
    override val nodeType: String = "Metadata"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitMetadata(this)
}
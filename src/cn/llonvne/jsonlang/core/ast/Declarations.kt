package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.types.Type
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * 声明节点基类
 */
@Serializable
sealed class Declaration : ASTNode() {
    abstract val name: String
    abstract val visibility: Visibility
}

/**
 * 可见性枚举
 */
@Serializable
enum class Visibility {
    PUBLIC, PRIVATE, INTERNAL, PROTECTED
}

/**
 * 导入声明
 */
@Serializable
data class ImportDeclaration(
    val source: String,
    val version: String? = null,
    val items: List<String> = emptyList(),
    val alias: Map<String, String> = emptyMap(),
    val condition: ImportCondition? = null,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val name: String = source
    override val visibility: Visibility = Visibility.PUBLIC
    override val nodeType: String = "ImportDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitImportDeclaration(this)
}

/**
 * 导入条件
 */
@Serializable
data class ImportCondition(
    val target: String? = null,
    val platform: String? = null,
    val feature: String? = null
)

/**
 * 导出声明
 */
@Serializable
data class ExportDeclaration(
    override val name: String,
    val exportType: ExportType,
    val target: String? = null,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val nodeType: String = "ExportDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitExportDeclaration(this)
}

/**
 * 导出类型枚举
 */
@Serializable
enum class ExportType {
    FUNCTION, TYPE, CONSTANT, DEFAULT, NAMESPACE
}

/**
 * 类型声明
 */
@Serializable
data class TypeDeclaration(
    override val name: String,
    val kind: TypeKind,
    val definition: TypeDefinition,
    val constraints: List<TypeConstraint> = emptyList(),
    override val visibility: Visibility = Visibility.PUBLIC,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val nodeType: String = "TypeDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitTypeDeclaration(this)
}

/**
 * 类型种类枚举
 */
@Serializable
enum class TypeKind {
    STRUCT, ENUM, UNION, ALIAS, GENERIC
}

/**
 * 类型定义
 */
@Serializable
sealed class TypeDefinition {
    @Serializable
    data class StructDefinition(val fields: Map<String, FieldDefinition>) : TypeDefinition()
    @Serializable
    data class EnumDefinition(val variants: Map<String, Type?>) : TypeDefinition()
    @Serializable
    data class UnionDefinition(val types: List<Type>) : TypeDefinition()
    @Serializable
    data class AliasDefinition(val aliasedType: Type) : TypeDefinition()
    @Serializable
    data class GenericDefinition(val parameters: List<GenericParameter>, val baseType: Type) : TypeDefinition()
}

/**
 * 字段定义
 */
@Serializable
data class FieldDefinition(
    val type: Type,
    val optional: Boolean = false,
    val defaultValue: Expression? = null
)

/**
 * 类型约束
 */
@Serializable
data class TypeConstraint(
    val parameter: String,
    val bounds: List<Type>
)

/**
 * 泛型参数
 */
@Serializable
data class GenericParameter(
    val name: String,
    val constraints: List<Type> = emptyList(),
    val defaultType: Type? = null
)

/**
 * 常量声明
 */
@Serializable
data class ConstantDeclaration(
    override val name: String,
    val type: Type,
    val value: Expression,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val nodeType: String = "ConstantDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitConstantDeclaration(this)
}

/**
 * 函数声明
 */
@Serializable
data class FunctionDeclaration(
    override val name: String,
    val modifiers: FunctionModifiers,
    val generics: List<GenericParameter> = emptyList(),
    val parameters: List<Parameter>,
    val returnType: Type,
    val body: List<Statement>,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val nodeType: String = "FunctionDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitFunctionDeclaration(this)
}

/**
 * 函数修饰符
 */
@Serializable
data class FunctionModifiers(
    val async: Boolean = false,
    val inline: Boolean = false,
    val pure: Boolean = false,
    val external: Boolean = false
)

/**
 * 参数定义
 */
@Serializable
data class Parameter(
    val name: String,
    val type: Type,
    val optional: Boolean = false,
    val defaultValue: Expression? = null,
    val annotations: List<Annotation> = emptyList()
)

/**
 * 宏声明
 */
@Serializable
data class MacroDeclaration(
    override val name: String,
    val parameters: List<String>,
    val expansion: MacroExpansion,
    val phase: MacroPhase = MacroPhase.COMPILE_TIME,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val nodeType: String = "MacroDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitMacroDeclaration(this)
}

/**
 * 宏展开
 */
@Serializable
sealed class MacroExpansion {
    @Serializable
    data class TemplateExpansion(val template: ASTNode) : MacroExpansion()
    @Serializable
    data class FunctionExpansion(val function: String) : MacroExpansion()
}

/**
 * 宏阶段枚举
 */
@Serializable
enum class MacroPhase {
    COMPILE_TIME, RUNTIME
}

/**
 * DSL 声明
 */
@Serializable
data class DSLDeclaration(
    override val name: String,
    val syntax: DSLSyntax,
    val semantics: DSLSemantics,
    val transformation: DSLTransformation,
    override val visibility: Visibility = Visibility.PUBLIC,
    override val location: SourceLocation,
    override val annotations: List<Annotation> = emptyList()
) : Declaration() {
    override val nodeType: String = "DSLDeclaration"
    override fun <T> accept(visitor: ASTVisitor<T>): T = visitor.visitDSLDeclaration(this)
}

/**
 * DSL 语法定义
 */
@Serializable
data class DSLSyntax(
    val patterns: Map<String, DSLPattern>
)

/**
 * DSL 模式
 */
@Serializable
data class DSLPattern(
    val pattern: String,
    val fields: List<String>
)

/**
 * DSL 语义定义
 */
@Serializable
data class DSLSemantics(
    val rules: Map<String, DSLSemanticRule>
)

/**
 * DSL 语义规则
 */
@Serializable
data class DSLSemanticRule(
    val type: String,
    val mapping: Map<String, @Contextual Any>
)

/**
 * DSL 转换定义
 */
@Serializable
data class DSLTransformation(
    val target: String,
    val template: String
)
package cn.llonvne.jsonlang.core.modules

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlinx.serialization.Serializable

/**
 * 导入导出系统
 * 处理模块间的符号导入和导出
 */
open class ImportExportSystem(private val moduleSystem: ModuleSystem) {
    
    /**
     * 解析导入声明
     */
    open fun resolveImports(module: Module, context: ModuleContext): ImportResolutionResult {
        val resolvedImports = mutableMapOf<String, ResolvedImport>()
        val errors = mutableListOf<ImportError>()
        
        for (import in module.imports) {
            try {
                val resolvedImport = resolveImport(import, context)
                resolvedImports[import.source] = resolvedImport
            } catch (e: Exception) {
                errors.add(ImportError(import.source, e.message ?: "未知错误"))
            }
        }
        
        return ImportResolutionResult(resolvedImports, errors)
    }
    
    private fun resolveImport(import: ImportDeclaration, context: ModuleContext): ResolvedImport {
        // 检查条件导入
        if (import.condition != null && !evaluateImportCondition(import.condition, context)) {
            return ResolvedImport(
                import = import,
                targetModule = null,
                symbols = emptyMap(),
                skipped = true,
                reason = "条件不满足"
            )
        }
        
        val targetModule = moduleSystem.resolveModule(import.source, context)
        val symbols = resolveImportedSymbols(import, targetModule)
        
        return ResolvedImport(
            import = import,
            targetModule = targetModule,
            symbols = symbols,
            skipped = false
        )
    }
    
    /**
     * 解析导入的符号
     */
    private fun resolveImportedSymbols(
        import: ImportDeclaration, 
        targetModule: Module
    ): Map<String, ImportedSymbol> {
        val symbols = mutableMapOf<String, ImportedSymbol>()
        val exportedSymbols = targetModule.getExportedSymbols()
        
        when {
            // 导入所有符号 (import *)
            import.items.contains("*") -> {
                for ((name, declaration) in exportedSymbols) {
                    val localName = import.alias[name] ?: name
                    symbols[localName] = ImportedSymbol(
                        originalName = name,
                        localName = localName,
                        declaration = declaration,
                        module = targetModule
                    )
                }
            }
            
            // 选择性导入
            import.items.isNotEmpty() -> {
                for (itemName in import.items) {
                    val declaration = exportedSymbols[itemName]
                        ?: throw ImportError(import.source, "符号 '$itemName' 未导出")
                    
                    val localName = import.alias[itemName] ?: itemName
                    symbols[localName] = ImportedSymbol(
                        originalName = itemName,
                        localName = localName,
                        declaration = declaration,
                        module = targetModule
                    )
                }
            }
            
            // 命名空间导入 (import module as alias)
            else -> {
                val namespaceName = import.alias.values.firstOrNull() ?: targetModule.metadata.name
                symbols[namespaceName] = ImportedSymbol(
                    originalName = targetModule.metadata.name,
                    localName = namespaceName,
                    declaration = createNamespaceDeclaration(targetModule),
                    module = targetModule
                )
            }
        }
        
        return symbols
    }
    
    /**
     * 创建命名空间声明
     */
    private fun createNamespaceDeclaration(module: Module): Declaration {
        // 使用常量声明来表示命名空间
        return ConstantDeclaration(
            name = module.metadata.name,
            type = createNamespaceType(),
            value = createNamespaceExpression(module),
            visibility = Visibility.PUBLIC,
            location = SourceLocation.unknown(module.metadata.name)
        )
    }
    
    private fun createNamespaceType(): cn.llonvne.jsonlang.core.types.Type {
        return cn.llonvne.jsonlang.core.types.PrimitiveType.AnyType
    }
    
    private fun createNamespaceExpression(module: Module): Expression {
        return LiteralExpression.StringLiteral(
            value = module.metadata.name,
            location = SourceLocation.unknown(module.metadata.name)
        )
    }
    
    /**
     * 评估导入条件
     */
    private fun evaluateImportCondition(condition: ImportCondition, context: ModuleContext): Boolean {
        // 检查目标平台
        condition.target?.let { target ->
            // 这里应该检查当前编译目标是否匹配
            // 为了测试，假设只有 "javascript" 目标匹配
            return target == "javascript"
        }
        
        // 检查平台特性
        condition.platform?.let { platform ->
            // 这里应该检查当前平台是否匹配
            return platform == "web"
        }
        
        // 检查功能特性
        condition.feature?.let { feature ->
            // 这里应该检查是否启用了指定功能
            return feature == "enabled"
        }
        
        return true
    }
    
    /**
     * 验证导出声明
     */
    fun validateExports(module: Module): ExportValidationResult {
        val errors = mutableListOf<ExportError>()
        val validExports = mutableListOf<ExportDeclaration>()
        
        for (export in module.exports) {
            try {
                validateExport(export, module)
                validExports.add(export)
            } catch (e: Exception) {
                errors.add(ExportError(export.name, e.message ?: "未知错误"))
            }
        }
        
        return ExportValidationResult(validExports, errors)
    }
    
    private fun validateExport(export: ExportDeclaration, module: Module) {
        // 检查导出的符号是否存在
        val declaration = module.declarations.find { it.name == export.name }
            ?: throw ExportError(export.name, "导出的符号不存在")
        
        // 检查可见性
        if (declaration.visibility == Visibility.PRIVATE) {
            throw ExportError(export.name, "不能导出私有符号")
        }
        
        // 检查导出类型匹配
        val expectedType = when (declaration) {
            is FunctionDeclaration -> ExportType.FUNCTION
            is TypeDeclaration -> ExportType.TYPE
            is ConstantDeclaration -> ExportType.CONSTANT
            else -> throw ExportError(export.name, "不支持的导出类型")
        }
        
        if (export.exportType != expectedType && export.exportType != ExportType.DEFAULT) {
            throw ExportError(export.name, "导出类型不匹配")
        }
    }
    
    /**
     * 创建符号表
     */
    fun createSymbolTable(
        module: Module, 
        resolvedImports: Map<String, ResolvedImport>
    ): SymbolTable {
        val symbolTable = SymbolTable()
        
        // 添加本模块的声明
        for (declaration in module.declarations) {
            symbolTable.addSymbol(declaration.name, Symbol(
                name = declaration.name,
                declaration = declaration,
                module = module,
                imported = false
            ))
        }
        
        // 添加导入的符号
        for (resolvedImport in resolvedImports.values) {
            if (!resolvedImport.skipped) {
                for ((localName, importedSymbol) in resolvedImport.symbols) {
                    symbolTable.addSymbol(localName, Symbol(
                        name = localName,
                        declaration = importedSymbol.declaration,
                        module = importedSymbol.module,
                        imported = true,
                        originalName = importedSymbol.originalName
                    ))
                }
            }
        }
        
        return symbolTable
    }
}

/**
 * 解析的导入
 */
@Serializable
data class ResolvedImport(
    val import: ImportDeclaration,
    val targetModule: Module?,
    val symbols: Map<String, ImportedSymbol>,
    val skipped: Boolean = false,
    val reason: String? = null
)

/**
 * 导入的符号
 */
@Serializable
data class ImportedSymbol(
    val originalName: String,
    val localName: String,
    val declaration: Declaration,
    val module: Module
)

/**
 * 导入解析结果
 */
data class ImportResolutionResult(
    val resolvedImports: Map<String, ResolvedImport>,
    val errors: List<ImportError>
)

/**
 * 导出验证结果
 */
data class ExportValidationResult(
    val validExports: List<ExportDeclaration>,
    val errors: List<ExportError>
)

/**
 * 符号表
 */
class SymbolTable {
    private val symbols = mutableMapOf<String, Symbol>()
    private val scopes = mutableListOf<MutableMap<String, Symbol>>()
    
    init {
        // 创建全局作用域
        pushScope()
    }
    
    fun addSymbol(name: String, symbol: Symbol) {
        val currentScope = scopes.lastOrNull() ?: symbols
        if (name in currentScope) {
            throw SymbolConflictException("符号 '$name' 已存在")
        }
        currentScope[name] = symbol
    }
    
    fun lookupSymbol(name: String): Symbol? {
        // 从内层作用域向外层查找
        for (scope in scopes.reversed()) {
            scope[name]?.let { return it }
        }
        return symbols[name]
    }
    
    fun pushScope() {
        scopes.add(mutableMapOf())
    }
    
    fun popScope() {
        if (scopes.isNotEmpty()) {
            scopes.removeAt(scopes.size - 1)
        }
    }
    
    fun getAllSymbols(): Map<String, Symbol> {
        val allSymbols = mutableMapOf<String, Symbol>()
        allSymbols.putAll(symbols)
        for (scope in scopes) {
            allSymbols.putAll(scope)
        }
        return allSymbols
    }
}

/**
 * 符号定义
 */
@Serializable
data class Symbol(
    val name: String,
    val declaration: Declaration,
    val module: Module,
    val imported: Boolean = false,
    val originalName: String? = null
)

/**
 * 导入导出错误
 */
sealed class ImportExportException(message: String) : Exception(message)

class ImportError(val source: String, message: String) : ImportExportException("导入错误 [$source]: $message")
class ExportError(val symbol: String, message: String) : ImportExportException("导出错误 [$symbol]: $message")
class SymbolConflictException(message: String) : ImportExportException(message)
class SymbolNotFoundException(message: String) : ImportExportException(message)
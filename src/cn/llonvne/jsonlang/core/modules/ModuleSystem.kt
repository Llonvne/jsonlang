package cn.llonvne.jsonlang.core.modules

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 模块系统核心类
 * 负责模块的定义、解析和依赖管理
 */
open class ModuleSystem {
    private val moduleCache = mutableMapOf<String, Module>()
    private val dependencyGraph = mutableMapOf<String, MutableSet<String>>()
    private val resolvers = mutableListOf<ModuleResolver>()
    
    init {
        // 注册默认的模块解析器
        resolvers.add(FileModuleResolver())
        resolvers.add(PackageModuleResolver())
        resolvers.add(StandardLibraryResolver())
    }
    
    /**
     * 解析模块
     */
    open fun resolveModule(moduleId: String, context: ModuleContext): Module {
        // 检查缓存
        moduleCache[moduleId]?.let { return it }
        
        // 尝试各个解析器
        for (resolver in resolvers) {
            if (resolver.canResolve(moduleId, context)) {
                val module = resolver.resolve(moduleId, context)
                moduleCache[moduleId] = module
                return module
            }
        }
        
        throw ModuleNotFoundException("无法找到模块: $moduleId")
    }
    
    /**
     * 构建依赖图
     */
    fun buildDependencyGraph(rootModule: Module): DependencyGraph {
        val visited = mutableSetOf<String>()
        val graph = DependencyGraph()
        
        buildDependencyGraphRecursive(rootModule, visited, graph)
        
        // 检查循环依赖
        detectCircularDependencies(graph)
        
        return graph
    }
    
    private fun buildDependencyGraphRecursive(
        module: Module, 
        visited: MutableSet<String>, 
        graph: DependencyGraph
    ) {
        if (module.metadata.name in visited) return
        visited.add(module.metadata.name)
        
        graph.addModule(module)
        
        for (import in module.imports) {
            val dependency = resolveModule(import.source, ModuleContext(module.path))
            graph.addDependency(module.metadata.name, dependency.metadata.name)
            buildDependencyGraphRecursive(dependency, visited, graph)
        }
    }
    
    /**
     * 检测循环依赖
     */
    private fun detectCircularDependencies(graph: DependencyGraph) {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        for (module in graph.modules.keys) {
            if (module !in visited) {
                if (hasCycleDFS(module, graph, visited, recursionStack)) {
                    throw CircularDependencyException("检测到循环依赖")
                }
            }
        }
    }
    
    private fun hasCycleDFS(
        module: String,
        graph: DependencyGraph,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>
    ): Boolean {
        visited.add(module)
        recursionStack.add(module)
        
        graph.dependencies[module]?.forEach { dependency ->
            if (dependency !in visited) {
                if (hasCycleDFS(dependency, graph, visited, recursionStack)) {
                    return true
                }
            } else if (dependency in recursionStack) {
                return true
            }
        }
        
        recursionStack.remove(module)
        return false
    }
}

/**
 * 模块定义
 */
@Serializable
data class Module(
    val metadata: ModuleMetadata,
    val imports: List<ImportDeclaration>,
    val exports: List<ExportDeclaration>,
    val declarations: List<Declaration>,
    val path: String,
    val content: String? = null
) {
    /**
     * 获取导出的符号
     */
    fun getExportedSymbols(): Map<String, Declaration> {
        val symbols = mutableMapOf<String, Declaration>()
        
        for (export in exports) {
            val declaration = declarations.find { it.name == export.name }
            if (declaration != null) {
                symbols[export.name] = declaration
            }
        }
        
        return symbols
    }
    
    /**
     * 检查是否导出指定符号
     */
    fun isExported(symbolName: String): Boolean {
        return exports.any { it.name == symbolName }
    }
}

/**
 * 模块元数据
 */
@Serializable
data class ModuleMetadata(
    val name: String,
    val version: String,
    val description: String? = null,
    val author: String? = null,
    val license: String? = null,
    val targets: List<String> = emptyList(),
    val encoding: String = "utf-8",
    val dependencies: Map<String, String> = emptyMap()
)

/**
 * 模块上下文
 */
data class ModuleContext(
    val currentPath: String,
    val searchPaths: List<String> = emptyList(),
    val packageRegistry: String? = null
)

/**
 * 依赖图
 */
class DependencyGraph {
    val modules = mutableMapOf<String, Module>()
    val dependencies = mutableMapOf<String, MutableSet<String>>()
    
    fun addModule(module: Module) {
        modules[module.metadata.name] = module
        dependencies.putIfAbsent(module.metadata.name, mutableSetOf())
    }
    
    fun addDependency(from: String, to: String) {
        dependencies.getOrPut(from) { mutableSetOf() }.add(to)
    }
    
    /**
     * 获取拓扑排序的模块列表
     */
    fun getTopologicalOrder(): List<String> {
        val visited = mutableSetOf<String>()
        val stack = mutableListOf<String>()
        
        for (module in modules.keys) {
            if (module !in visited) {
                topologicalSortDFS(module, visited, stack)
            }
        }
        
        return stack
    }
    
    private fun topologicalSortDFS(
        module: String,
        visited: MutableSet<String>,
        stack: MutableList<String>
    ) {
        visited.add(module)
        
        dependencies[module]?.forEach { dependency ->
            if (dependency !in visited) {
                topologicalSortDFS(dependency, visited, stack)
            }
        }
        
        stack.add(module)
    }
}

/**
 * 模块解析器接口
 */
interface ModuleResolver {
    fun canResolve(moduleId: String, context: ModuleContext): Boolean
    fun resolve(moduleId: String, context: ModuleContext): Module
}

/**
 * 文件模块解析器
 * 处理相对路径和绝对路径的文件导入
 */
class FileModuleResolver : ModuleResolver {
    override fun canResolve(moduleId: String, context: ModuleContext): Boolean {
        return moduleId.startsWith("./") || 
               moduleId.startsWith("../") || 
               moduleId.startsWith("/") ||
               moduleId.endsWith(".lang.json")
    }
    
    override fun resolve(moduleId: String, context: ModuleContext): Module {
        val path = resolveFilePath(moduleId, context.currentPath)
        val content = readFileContent(path)
        return parseModule(content, path)
    }
    
    private fun resolveFilePath(moduleId: String, currentPath: String): String {
        return when {
            moduleId.startsWith("./") || moduleId.startsWith("../") -> {
                val currentDir = Paths.get(currentPath).parent ?: Paths.get(".")
                currentDir.resolve(moduleId).normalize().toString()
            }
            moduleId.startsWith("/") -> moduleId
            else -> {
                // 假设是相对于当前文件的路径
                val currentDir = Paths.get(currentPath).parent ?: Paths.get(".")
                currentDir.resolve(moduleId).normalize().toString()
            }
        }
    }
    
    private fun readFileContent(path: String): String {
        // 这里应该实际读取文件内容
        // 为了演示，返回空内容
        return "{}"
    }
    
    private fun parseModule(content: String, path: String): Module {
        // 这里应该解析 JSON 内容为 Module 对象
        // 为了演示，返回一个基本的模块
        return Module(
            metadata = ModuleMetadata(
                name = Paths.get(path).fileName.toString(),
                version = "1.0.0"
            ),
            imports = emptyList(),
            exports = emptyList(),
            declarations = emptyList(),
            path = path,
            content = content
        )
    }
}

/**
 * 包管理器模块解析器
 * 处理包管理器格式的导入 (package:name)
 */
class PackageModuleResolver : ModuleResolver {
    override fun canResolve(moduleId: String, context: ModuleContext): Boolean {
        return moduleId.startsWith("package:")
    }
    
    override fun resolve(moduleId: String, context: ModuleContext): Module {
        val packageName = moduleId.removePrefix("package:")
        // 这里应该从包注册表下载和解析包
        // 为了演示，返回一个基本的模块
        return Module(
            metadata = ModuleMetadata(
                name = packageName,
                version = "1.0.0"
            ),
            imports = emptyList(),
            exports = emptyList(),
            declarations = emptyList(),
            path = "packages/$packageName"
        )
    }
}

/**
 * 标准库解析器
 * 处理标准库模块的导入
 */
class StandardLibraryResolver : ModuleResolver {
    private val standardModules = setOf("stdlib", "core", "io", "math", "string")
    
    override fun canResolve(moduleId: String, context: ModuleContext): Boolean {
        return moduleId in standardModules
    }
    
    override fun resolve(moduleId: String, context: ModuleContext): Module {
        // 这里应该加载内置的标准库模块
        return Module(
            metadata = ModuleMetadata(
                name = moduleId,
                version = "1.0.0",
                description = "JSONLang 标准库模块"
            ),
            imports = emptyList(),
            exports = emptyList(),
            declarations = emptyList(),
            path = "stdlib/$moduleId"
        )
    }
}

/**
 * 模块系统异常
 */
sealed class ModuleException(message: String) : Exception(message)

class ModuleNotFoundException(message: String) : ModuleException(message)
class CircularDependencyException(message: String) : ModuleException(message)
class ModuleParseException(message: String) : ModuleException(message)
class ModuleVersionConflictException(message: String) : ModuleException(message)
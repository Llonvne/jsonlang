package cn.llonvne.jsonlang.core.modules

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach


/**
 * 模块系统集成测试
 * 测试模块解析、导入导出和依赖管理的完整流程
 */
class ModuleIntegrationTest {
    
    private lateinit var moduleSystem: ModuleSystem
    private lateinit var importExportSystem: ImportExportSystem
    private lateinit var moduleParser: ModuleParser
    
    @BeforeEach
    fun setUp() {
        moduleSystem = ModuleSystem()
        importExportSystem = ImportExportSystem(moduleSystem)
        moduleParser = ModuleParser()
    }
    
    @Test
    fun `should parse and resolve complete module with dependencies`() {
        // 创建一个数学工具模块
        val mathModuleJson = """
        {
            "metadata": {
                "name": "math-utils",
                "version": "1.0.0",
                "description": "数学工具模块"
            },
            "exports": {
                "add": {
                    "type": "function",
                    "visibility": "public"
                },
                "PI": {
                    "type": "constant",
                    "visibility": "public"
                }
            },
            "functions": {
                "add": {
                    "parameters": [
                        {"name": "a", "type": "Int"},
                        {"name": "b", "type": "Int"}
                    ],
                    "return": {"type": "Int"},
                    "body": []
                }
            },
            "constants": {
                "PI": {
                    "type": "Float",
                    "value": 3.14159
                }
            }
        }
        """.trimIndent()
        
        // 创建一个使用数学工具的主模块
        val mainModuleJson = """
        {
            "metadata": {
                "name": "calculator",
                "version": "1.0.0",
                "description": "计算器应用"
            },
            "imports": {
                "math": {
                    "source": "./math-utils.lang.json",
                    "items": ["add", "PI"],
                    "alias": {
                        "add": "加法"
                    }
                }
            },
            "functions": {
                "calculate": {
                    "parameters": [
                        {"name": "x", "type": "Int"},
                        {"name": "y", "type": "Int"}
                    ],
                    "return": {"type": "Int"},
                    "body": []
                }
            }
        }
        """.trimIndent()
        
        // 解析模块
        val mathModule = moduleParser.parseModule(mathModuleJson, "./math-utils.lang.json")
        val mainModule = moduleParser.parseModule(mainModuleJson, "./calculator.lang.json")
        
        // 验证数学模块解析正确
        assert(mathModule.metadata.name == "math-utils")
        assert(mathModule.exports.size == 2)
        assert(mathModule.declarations.size == 2)
        
        // 验证主模块解析正确
        assert(mainModule.metadata.name == "calculator")
        assert(mainModule.imports.size == 1)
        assert(mainModule.declarations.size == 1)
        
        // 创建模拟的模块系统来测试导入解析
        val testModuleSystem = object : ModuleSystem() {
            override fun resolveModule(moduleId: String, context: ModuleContext): Module {
                return when (moduleId) {
                    "./math-utils.lang.json" -> mathModule
                    else -> throw ModuleNotFoundException("Module not found: $moduleId")
                }
            }
        }
        
        val testImportExportSystem = ImportExportSystem(testModuleSystem)
        
        // 解析导入
        val importResult = testImportExportSystem.resolveImports(
            mainModule, 
            ModuleContext("./calculator.lang.json")
        )
        
        // 验证导入解析结果
        assert(importResult.errors.isEmpty())
        assert(importResult.resolvedImports.size == 1)
        
        val mathImport = importResult.resolvedImports["./math-utils.lang.json"]
        assert(mathImport != null)
        assert(!mathImport!!.skipped)
        assert(mathImport.symbols.size == 2)
        
        // 验证别名导入
        assert(mathImport.symbols.containsKey("加法"))
        assert(mathImport.symbols.containsKey("PI"))
        assert(!mathImport.symbols.containsKey("add"))
        
        // 验证符号表创建
        val symbolTable = testImportExportSystem.createSymbolTable(mainModule, importResult.resolvedImports)
        
        // 本地函数应该存在
        val calculateSymbol = symbolTable.lookupSymbol("calculate")
        assert(calculateSymbol != null)
        assert(!calculateSymbol!!.imported)
        
        // 导入的符号应该存在
        val addSymbol = symbolTable.lookupSymbol("加法")
        assert(addSymbol != null)
        assert(addSymbol!!.imported)
        assert(addSymbol.originalName == "add")
        
        val piSymbol = symbolTable.lookupSymbol("PI")
        assert(piSymbol != null)
        assert(piSymbol!!.imported)
    }
    
    @Test
    fun `should handle conditional imports correctly`() {
        val moduleJson = """
        {
            "metadata": {
                "name": "platform-specific",
                "version": "1.0.0"
            },
            "imports": {
                "web-api": {
                    "source": "web-api",
                    "items": ["fetch"],
                    "condition": {
                        "platform": "web"
                    }
                },
                "node-api": {
                    "source": "node-api", 
                    "items": ["fs"],
                    "condition": {
                        "platform": "node"
                    }
                },
                "js-target": {
                    "source": "js-utils",
                    "items": ["*"],
                    "condition": {
                        "target": "javascript"
                    }
                }
            }
        }
        """.trimIndent()
        
        val module = moduleParser.parseModule(moduleJson, "./platform-specific.lang.json")
        
        // 创建模拟的模块系统
        val testModuleSystem = object : ModuleSystem() {
            override fun resolveModule(moduleId: String, context: ModuleContext): Module {
                return Module(
                    metadata = ModuleMetadata(moduleId, "1.0.0"),
                    imports = emptyList(),
                    exports = listOf(
                        ExportDeclaration(
                            name = "testFunction",
                            exportType = ExportType.FUNCTION,
                            location = SourceLocation.unknown()
                        )
                    ),
                    declarations = listOf(
                        createTestFunction("testFunction")
                    ),
                    path = "./$moduleId.lang.json"
                )
            }
        }
        
        val testImportExportSystem = ImportExportSystem(testModuleSystem)
        val importResult = testImportExportSystem.resolveImports(
            module, 
            ModuleContext("./platform-specific.lang.json")
        )
        
        // 验证条件导入结果 (只有满足条件的导入会被处理)
        assert(importResult.resolvedImports.size == 2)
        
        // 只有满足条件的导入会被处理，不满足条件的会被跳过
        // js-target 应该被导入（条件满足）
        val jsImport = importResult.resolvedImports["js-utils"]
        assert(jsImport != null)
        assert(!jsImport!!.skipped)
        
        // 检查是否有跳过的导入
        val skippedImports = importResult.resolvedImports.values.filter { it.skipped }
        assert(skippedImports.isNotEmpty())
    }
    
    @Test
    fun `should validate export declarations correctly`() {
        val moduleJson = """
        {
            "metadata": {
                "name": "export-test",
                "version": "1.0.0"
            },
            "exports": {
                "validFunction": {
                    "type": "function",
                    "visibility": "public"
                },
                "invalidFunction": {
                    "type": "function",
                    "visibility": "public"
                },
                "validConstant": {
                    "type": "constant",
                    "visibility": "public"
                }
            },
            "functions": {
                "validFunction": {
                    "parameters": [],
                    "return": {"type": "Unit"},
                    "body": []
                }
            },
            "constants": {
                "validConstant": {
                    "type": "String",
                    "value": "test"
                }
            }
        }
        """.trimIndent()
        
        val module = moduleParser.parseModule(moduleJson, "./export-test.lang.json")
        val validationResult = importExportSystem.validateExports(module)
        
        // 应该有一个有效导出和一个错误
        assert(validationResult.validExports.size == 2)
        assert(validationResult.errors.size == 1)
        
        // 验证错误是关于不存在的函数
        val error = validationResult.errors[0]
        assert(error.symbol == "invalidFunction")
        assert(error.message?.contains("导出的符号不存在") == true)
    }
    
    @Test
    fun `should handle namespace imports correctly`() {
        val utilsModuleJson = """
        {
            "metadata": {
                "name": "utils",
                "version": "1.0.0"
            },
            "exports": {
                "helper1": {"type": "function"},
                "helper2": {"type": "function"},
                "CONSTANT": {"type": "constant"}
            },
            "functions": {
                "helper1": {
                    "parameters": [],
                    "return": {"type": "Unit"},
                    "body": []
                },
                "helper2": {
                    "parameters": [],
                    "return": {"type": "Unit"},
                    "body": []
                }
            },
            "constants": {
                "CONSTANT": {
                    "type": "String",
                    "value": "test"
                }
            }
        }
        """.trimIndent()
        
        val mainModuleJson = """
        {
            "metadata": {
                "name": "main",
                "version": "1.0.0"
            },
            "imports": {
                "utils": {
                    "source": "./utils.lang.json",
                    "alias": {
                        "utils": "工具"
                    }
                }
            }
        }
        """.trimIndent()
        
        val utilsModule = moduleParser.parseModule(utilsModuleJson, "./utils.lang.json")
        val mainModule = moduleParser.parseModule(mainModuleJson, "./main.lang.json")
        
        val testModuleSystem = object : ModuleSystem() {
            override fun resolveModule(moduleId: String, context: ModuleContext): Module {
                return when (moduleId) {
                    "./utils.lang.json" -> utilsModule
                    else -> throw ModuleNotFoundException("Module not found: $moduleId")
                }
            }
        }
        
        val testImportExportSystem = ImportExportSystem(testModuleSystem)
        val importResult = testImportExportSystem.resolveImports(
            mainModule,
            ModuleContext("./main.lang.json")
        )
        
        // 验证命名空间导入
        assert(importResult.errors.isEmpty())
        assert(importResult.resolvedImports.size == 1)
        
        val utilsImport = importResult.resolvedImports["./utils.lang.json"]
        assert(utilsImport != null)
        assert(!utilsImport!!.skipped)
        
        // 应该创建一个命名空间符号
        assert(utilsImport.symbols.size == 1)
        assert(utilsImport.symbols.containsKey("工具"))
        
        val symbolTable = testImportExportSystem.createSymbolTable(mainModule, importResult.resolvedImports)
        val namespaceSymbol = symbolTable.lookupSymbol("工具")
        assert(namespaceSymbol != null)
        assert(namespaceSymbol!!.imported)
    }
    
    private fun createTestFunction(name: String): FunctionDeclaration {
        return FunctionDeclaration(
            name = name,
            modifiers = FunctionModifiers(),
            parameters = emptyList(),
            returnType = cn.llonvne.jsonlang.core.types.PrimitiveType.UnitType,
            body = emptyList(),
            location = SourceLocation.unknown()
        )
    }
}
package cn.llonvne.jsonlang.core.modules

import cn.llonvne.jsonlang.core.ast.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModuleParserTest {
    
    private lateinit var parser: ModuleParser
    
    @BeforeEach
    fun setUp() {
        parser = ModuleParser()
    }
    
    @Test
    fun `should parse minimal module`() {
        val json = """
        {
            "metadata": {
                "name": "test-module",
                "version": "1.0.0"
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./test-module.lang.json")
        
        assertEquals("test-module", module.metadata.name)
        assertEquals("1.0.0", module.metadata.version)
        assertTrue(module.imports.isEmpty())
        assertTrue(module.exports.isEmpty())
        assertTrue(module.declarations.isEmpty())
    }
    
    @Test
    fun `should parse module with metadata`() {
        val json = """
        {
            "metadata": {
                "name": "advanced-module",
                "version": "2.1.0",
                "description": "一个高级的 JSONLang 模块",
                "author": "张三",
                "license": "MIT",
                "target": ["kotlin", "javascript"],
                "encoding": "utf-8",
                "dependencies": {
                    "stdlib": "1.0.0",
                    "utils": "^2.0.0"
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./advanced-module.lang.json")
        
        assertEquals("advanced-module", module.metadata.name)
        assertEquals("2.1.0", module.metadata.version)
        assertEquals("一个高级的 JSONLang 模块", module.metadata.description)
        assertEquals("张三", module.metadata.author)
        assertEquals("MIT", module.metadata.license)
        assertEquals(listOf("kotlin", "javascript"), module.metadata.targets)
        assertEquals("utf-8", module.metadata.encoding)
        assertEquals(mapOf("stdlib" to "1.0.0", "utils" to "^2.0.0"), module.metadata.dependencies)
    }
    
    @Test
    fun `should parse module with imports`() {
        val json = """
        {
            "metadata": {
                "name": "import-test",
                "version": "1.0.0"
            },
            "imports": {
                "stdlib": {
                    "source": "stdlib",
                    "version": "1.0.0",
                    "items": ["*"]
                },
                "math": {
                    "source": "./math.lang.json",
                    "items": ["add", "subtract"],
                    "alias": {
                        "add": "加法",
                        "subtract": "减法"
                    }
                },
                "http": {
                    "source": "package:jsonlang-http",
                    "version": "^2.0.0",
                    "items": ["Client", "Server"],
                    "condition": {
                        "target": "kotlin"
                    }
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./import-test.lang.json")
        
        assertEquals(3, module.imports.size)
        
        // 检查 stdlib 导入
        val stdlibImport = module.imports.find { it.source == "stdlib" }
        assertNotNull(stdlibImport)
        assertEquals("1.0.0", stdlibImport.version)
        assertEquals(listOf("*"), stdlibImport.items)
        
        // 检查 math 导入
        val mathImport = module.imports.find { it.source == "./math.lang.json" }
        assertNotNull(mathImport)
        assertEquals(listOf("add", "subtract"), mathImport.items)
        assertEquals(mapOf("add" to "加法", "subtract" to "减法"), mathImport.alias)
        
        // 检查 http 导入
        val httpImport = module.imports.find { it.source == "package:jsonlang-http" }
        assertNotNull(httpImport)
        assertEquals("^2.0.0", httpImport.version)
        assertEquals(listOf("Client", "Server"), httpImport.items)
        assertNotNull(httpImport.condition)
        assertEquals("kotlin", httpImport.condition!!.target)
    }
    
    @Test
    fun `should parse module with exports`() {
        val json = """
        {
            "metadata": {
                "name": "export-test",
                "version": "1.0.0"
            },
            "exports": {
                "add": {
                    "type": "function",
                    "visibility": "public"
                },
                "MyType": {
                    "type": "type",
                    "visibility": "public"
                },
                "PI": {
                    "type": "constant",
                    "visibility": "public"
                },
                "default": {
                    "type": "default",
                    "target": "Calculator"
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./export-test.lang.json")
        
        assertEquals(4, module.exports.size)
        
        // 检查函数导出
        val functionExport = module.exports.find { it.name == "add" }
        assertNotNull(functionExport)
        assertEquals(ExportType.FUNCTION, functionExport.exportType)
        assertEquals(Visibility.PUBLIC, functionExport.visibility)
        
        // 检查类型导出
        val typeExport = module.exports.find { it.name == "MyType" }
        assertNotNull(typeExport)
        assertEquals(ExportType.TYPE, typeExport.exportType)
        
        // 检查常量导出
        val constantExport = module.exports.find { it.name == "PI" }
        assertNotNull(constantExport)
        assertEquals(ExportType.CONSTANT, constantExport.exportType)
        
        // 检查默认导出
        val defaultExport = module.exports.find { it.name == "default" }
        assertNotNull(defaultExport)
        assertEquals(ExportType.DEFAULT, defaultExport.exportType)
        assertEquals("Calculator", defaultExport.target)
    }
    
    @Test
    fun `should parse module with function declarations`() {
        val json = """
        {
            "metadata": {
                "name": "function-test",
                "version": "1.0.0"
            },
            "functions": {
                "add": {
                    "modifiers": {
                        "async": false,
                        "inline": true,
                        "pure": true
                    },
                    "parameters": [
                        {
                            "name": "a",
                            "type": "Int",
                            "optional": false
                        },
                        {
                            "name": "b",
                            "type": "Int",
                            "optional": false
                        }
                    ],
                    "return": {
                        "type": "Int"
                    },
                    "body": [
                        {
                            "type": "return",
                            "value": {
                                "type": "binary_op",
                                "operator": "+",
                                "left": {"type": "identifier", "name": "a"},
                                "right": {"type": "identifier", "name": "b"}
                            }
                        }
                    ],
                    "visibility": "public"
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./function-test.lang.json")
        
        assertEquals(1, module.declarations.size)
        
        val functionDecl = module.declarations[0] as FunctionDeclaration
        assertEquals("add", functionDecl.name)
        assertEquals(Visibility.PUBLIC, functionDecl.visibility)
        assertNotNull(functionDecl.modifiers)
        assertEquals(false, functionDecl.modifiers.async)
        assertEquals(true, functionDecl.modifiers.inline)
        assertEquals(true, functionDecl.modifiers.pure)
        assertEquals(2, functionDecl.parameters.size)
        assertEquals("a", functionDecl.parameters[0].name)
        assertEquals("b", functionDecl.parameters[1].name)
    }
    
    @Test
    fun `should parse module with type declarations`() {
        val json = """
        {
            "metadata": {
                "name": "type-test",
                "version": "1.0.0"
            },
            "types": {
                "Person": {
                    "kind": "struct",
                    "definition": {
                        "fields": {
                            "name": {
                                "type": "String",
                                "optional": false
                            },
                            "age": {
                                "type": "Int",
                                "optional": true,
                                "default": 0
                            }
                        }
                    },
                    "visibility": "public"
                },
                "Color": {
                    "kind": "enum",
                    "definition": {
                        "variants": {
                            "Red": null,
                            "Green": null,
                            "Blue": null,
                            "RGB": {
                                "r": "Int",
                                "g": "Int", 
                                "b": "Int"
                            }
                        }
                    }
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./type-test.lang.json")
        
        assertEquals(2, module.declarations.size)
        
        val personType = module.declarations.find { it.name == "Person" } as TypeDeclaration
        assertEquals("Person", personType.name)
        assertEquals(TypeKind.STRUCT, personType.kind)
        assertEquals(Visibility.PUBLIC, personType.visibility)
        
        val colorType = module.declarations.find { it.name == "Color" } as TypeDeclaration
        assertEquals("Color", colorType.name)
        assertEquals(TypeKind.ENUM, colorType.kind)
    }
    
    @Test
    fun `should parse module with constants`() {
        val json = """
        {
            "metadata": {
                "name": "constant-test",
                "version": "1.0.0"
            },
            "constants": {
                "PI": {
                    "type": "Float",
                    "value": 3.14159,
                    "visibility": "public"
                },
                "MAX_SIZE": {
                    "type": "Int",
                    "value": 1000,
                    "visibility": "internal"
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./constant-test.lang.json")
        
        assertEquals(2, module.declarations.size)
        
        val piConstant = module.declarations.find { it.name == "PI" } as ConstantDeclaration
        assertEquals("PI", piConstant.name)
        assertEquals(Visibility.PUBLIC, piConstant.visibility)
        
        val maxSizeConstant = module.declarations.find { it.name == "MAX_SIZE" } as ConstantDeclaration
        assertEquals("MAX_SIZE", maxSizeConstant.name)
        assertEquals(Visibility.INTERNAL, maxSizeConstant.visibility)
    }
    
    @Test
    fun `should handle module without metadata`() {
        val json = """
        {
            "functions": {
                "test": {
                    "parameters": [],
                    "return": {"type": "Unit"},
                    "body": []
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./unnamed.lang.json")
        
        assertEquals("unnamed", module.metadata.name)
        assertEquals("1.0.0", module.metadata.version)
        assertEquals(1, module.declarations.size)
    }
    
    @Test
    fun `should throw exception for invalid JSON`() {
        val invalidJson = """
        {
            "metadata": {
                "name": "test"
                // 缺少逗号
                "version": "1.0.0"
            }
        }
        """.trimIndent()
        
        assertThrows<ModuleParseException> {
            parser.parseModule(invalidJson, "./test.lang.json")
        }
    }
    
    @Test
    fun `should throw exception for non-object JSON`() {
        val arrayJson = """["not", "an", "object"]"""
        
        assertThrows<ModuleParseException> {
            parser.parseModule(arrayJson, "./test.lang.json")
        }
    }
    
    @Test
    fun `should parse complex module with all features`() {
        val json = """
        {
            "metadata": {
                "name": "complex-module",
                "version": "1.0.0",
                "description": "复杂的测试模块",
                "author": "测试作者",
                "license": "MIT",
                "target": ["kotlin", "go", "javascript"],
                "encoding": "utf-8"
            },
            "imports": {
                "stdlib": {
                    "source": "stdlib",
                    "items": ["*"]
                }
            },
            "exports": {
                "Calculator": {
                    "type": "type",
                    "visibility": "public"
                }
            },
            "types": {
                "Calculator": {
                    "kind": "struct",
                    "definition": {},
                    "visibility": "public"
                }
            },
            "constants": {
                "VERSION": {
                    "type": "String",
                    "value": "1.0.0",
                    "visibility": "public"
                }
            },
            "functions": {
                "create": {
                    "parameters": [],
                    "return": {"type": "Calculator"},
                    "body": [],
                    "visibility": "public"
                }
            },
            "macros": {
                "debug": {
                    "parameters": ["message"],
                    "expansion": {},
                    "phase": "compile_time"
                }
            },
            "dsl": {
                "query": {
                    "syntax": {},
                    "semantics": {},
                    "transformation": {
                        "target": "kotlin",
                        "template": ""
                    }
                }
            }
        }
        """.trimIndent()
        
        val module = parser.parseModule(json, "./complex-module.lang.json")
        
        assertEquals("complex-module", module.metadata.name)
        assertEquals("复杂的测试模块", module.metadata.description)
        assertEquals("测试作者", module.metadata.author)
        assertEquals(listOf("kotlin", "go", "javascript"), module.metadata.targets)
        
        assertEquals(1, module.imports.size)
        assertEquals(1, module.exports.size)
        assertEquals(5, module.declarations.size) // type + constant + function + macro + dsl
        
        // 验证各种声明类型都被正确解析
        assertTrue(module.declarations.any { it is TypeDeclaration })
        assertTrue(module.declarations.any { it is ConstantDeclaration })
        assertTrue(module.declarations.any { it is FunctionDeclaration })
        assertTrue(module.declarations.any { it is MacroDeclaration })
        assertTrue(module.declarations.any { it is DSLDeclaration })
    }
}
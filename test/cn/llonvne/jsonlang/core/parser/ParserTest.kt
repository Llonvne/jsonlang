package cn.llonvne.jsonlang.core.parser

import cn.llonvne.jsonlang.core.lexer.JSONLangLexer
import cn.llonvne.jsonlang.core.errors.DefaultErrorReporter
import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ParserTest {

    private fun parseProgram(source: String): Program? {
        val errorReporter = DefaultErrorReporter()
        val lexer = JSONLangLexer(source, "test.lang.json", errorReporter)
        val parser = JSONLangParser(lexer, errorReporter)
        return parser.parseProgram()
    }

    @Test
    fun testParseSimpleProgram() {
        val source = """
        {
            "metadata": {
                "name": "test",
                "version": "1.0.0"
            },
            "functions": {
                "add": {
                    "parameters": [
                        {
                            "name": "a",
                            "type": "Int"
                        },
                        {
                            "name": "b", 
                            "type": "Int"
                        }
                    ],
                    "return": "Int",
                    "body": [
                        {
                            "type": "return",
                            "value": {
                                "type": "binary_op",
                                "operator": "+",
                                "left": "a",
                                "right": "b"
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        assertEquals("test", program!!.metadata.name)
        assertEquals("1.0.0", program.metadata.version)
        assertEquals(1, program.functions.size)

        val addFunction = program.functions[0]
        assertEquals("add", addFunction.name)
        assertEquals(2, addFunction.parameters.size)
        assertEquals("a", addFunction.parameters[0].name)
        assertEquals("b", addFunction.parameters[1].name)
    }

    @Test
    fun testParseMetadata() {
        val source = """
        {
            "metadata": {
                "name": "example",
                "version": "2.0.0",
                "description": "示例程序",
                "author": "测试作者",
                "license": "MIT",
                "targets": ["kotlin", "javascript"],
                "encoding": "utf-8"
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        val metadata = program!!.metadata
        assertEquals("example", metadata.name)
        assertEquals("2.0.0", metadata.version)
        assertEquals("示例程序", metadata.description)
        assertEquals("测试作者", metadata.author)
        assertEquals("MIT", metadata.license)
        assertEquals(listOf("kotlin", "javascript"), metadata.targets)
        assertEquals("utf-8", metadata.encoding)
    }

    @Test
    fun testParseImports() {
        val source = """
        {
            "metadata": {
                "name": "test",
                "version": "1.0.0"
            },
            "imports": {
                "math": {
                    "source": "./math.lang.json",
                    "version": "1.0.0",
                    "items": ["add", "subtract"],
                    "alias": {
                        "add": "加法"
                    }
                },
                "stdlib": {
                    "source": "stdlib",
                    "items": ["*"]
                }
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        assertEquals(2, program!!.imports.size)

        val mathImport = program.imports.find { it.source == "./math.lang.json" }
        assertNotNull(mathImport)
        assertEquals("1.0.0", mathImport!!.version)
        assertEquals(listOf("add", "subtract"), mathImport.items)
        assertEquals(mapOf("add" to "加法"), mathImport.alias)

        val stdlibImport = program.imports.find { it.source == "stdlib" }
        assertNotNull(stdlibImport)
        assertEquals(listOf("*"), stdlibImport!!.items)
    }

    @Test
    fun testParseTypes() {
        val source = """
        {
            "metadata": {
                "name": "test",
                "version": "1.0.0"
            },
            "types": {
                "Person": {
                    "kind": "struct",
                    "definition": {
                        "name": {
                            "type": "String"
                        },
                        "age": {
                            "type": "Int",
                            "optional": false
                        }
                    }
                },
                "Color": {
                    "kind": "enum",
                    "definition": {
                        "Red": null,
                        "Green": null,
                        "Blue": null
                    }
                }
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        assertEquals(2, program!!.types.size)

        val personType = program.types.find { it.name == "Person" }
        assertNotNull(personType)
        assertEquals(TypeKind.STRUCT, personType!!.kind)
        assertTrue(personType.definition is TypeDefinition.StructDefinition)

        val colorType = program.types.find { it.name == "Color" }
        assertNotNull(colorType)
        assertEquals(TypeKind.ENUM, colorType!!.kind)
        assertTrue(colorType.definition is TypeDefinition.EnumDefinition)
    }

    @Test
    fun testParseConstants() {
        val source = """
        {
            "metadata": {
                "name": "test",
                "version": "1.0.0"
            },
            "constants": {
                "PI": {
                    "type": "Double",
                    "value": 3.14159,
                    "visibility": "public"
                },
                "MAX_SIZE": {
                    "type": "Int",
                    "value": 1000
                }
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        assertEquals(2, program!!.constants.size)

        val piConstant = program.constants.find { it.name == "PI" }
        assertNotNull(piConstant)
        assertEquals(Visibility.PUBLIC, piConstant!!.visibility)

        val maxSizeConstant = program.constants.find { it.name == "MAX_SIZE" }
        assertNotNull(maxSizeConstant)
    }

    @Test
    fun testParseFunctionWithComplexBody() {
        val source = """
        {
            "metadata": {
                "name": "test",
                "version": "1.0.0"
            },
            "functions": {
                "factorial": {
                    "parameters": [
                        {
                            "name": "n",
                            "type": "Int"
                        }
                    ],
                    "return": "Int",
                    "body": [
                        {
                            "type": "conditional",
                            "condition": {
                                "type": "binary_op",
                                "operator": "<=",
                                "left": "n",
                                "right": 1
                            },
                            "then": {
                                "type": "return",
                                "value": 1
                            },
                            "else": {
                                "type": "return",
                                "value": {
                                    "type": "binary_op",
                                    "operator": "*",
                                    "left": "n",
                                    "right": {
                                        "type": "call",
                                        "function": "factorial",
                                        "arguments": {
                                            "n": {
                                                "type": "binary_op",
                                                "operator": "-",
                                                "left": "n",
                                                "right": 1
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        assertEquals(1, program!!.functions.size)
        val factorial = program.functions[0]
        assertEquals("factorial", factorial.name)
        assertEquals(1, factorial.parameters.size)
        assertEquals("n", factorial.parameters[0].name)
        assertEquals(1, factorial.body.size)
    }

    @Test
    fun testParseChineseKeywords() {
        val source = """
        {
            "元数据": {
                "名称": "中文测试",
                "版本": "1.0.0"
            },
            "函数": {
                "问候": {
                    "参数": [
                        {
                            "名称": "姓名",
                            "类型": "String"
                        }
                    ],
                    "返回": "String",
                    "函数体": [
                        {
                            "类型": "返回",
                            "值": "你好"
                        }
                    ]
                }
            }
        }
        """.trimIndent()

        val program = parseProgram(source)
        assertNotNull(program)

        assertEquals("中文测试", program!!.metadata.name)
        assertEquals(1, program.functions.size)
        assertEquals("问候", program.functions[0].name)
    }

    @Test
    fun testErrorRecovery() {
        val source = """
        {
            "metadata": {
                "name": "test",
                "version": "1.0.0"
            },
            "invalid_field": {
                "some": "data"
            },
            "functions": {
                "test": {
                    "return": "Unit",
                    "body": []
                }
            }
        }
        """.trimIndent()

        val errorReporter = DefaultErrorReporter()
        val lexer = JSONLangLexer(source, "test.lang.json", errorReporter)
        val parser = JSONLangParser(lexer, errorReporter)
        val program = parser.parseProgram()

        // 程序应该能够解析，尽管有未知字段
        assertNotNull(program)
        assertEquals("test", program!!.metadata.name)
        assertEquals(1, program.functions.size)

        // 应该有错误报告
        assertTrue(errorReporter.hasErrors() || errorReporter.hasWarnings())
    }
}
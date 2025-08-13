package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation

/**
 * Kotlin 后端演示
 * 展示如何使用 Kotlin 后端生成代码
 */
object KotlinBackendDemo {
    
    fun main() {
        // 初始化后端注册表
        BackendRegistration.initialize()
        
        // 创建 Kotlin 后端
        val backend = BackendRegistry.create("kotlin") ?: error("Kotlin backend not found")
        
        // 创建示例程序
        val program = createSampleProgram()
        
        // 生成 Kotlin 代码
        val result = backend.generateProgram(program)
        
        if (result.isSuccess) {
            println("Generated Kotlin code:")
            println("=" * 50)
            println(result.code)
            println("=" * 50)
            
            if (result.warnings.isNotEmpty()) {
                println("\nWarnings:")
                result.warnings.forEach { println("  - $it") }
            }
        } else {
            println("Code generation failed:")
            result.errors.forEach { println("  - $it") }
        }
    }
    
    private fun createSampleProgram(): Program {
        val sourceLocation = SourceLocation("demo.lang.json", 1, 1, 1, 1, 0, 100)
        
        return Program(
            metadata = Metadata(
                name = "com.example.demo",
                version = "1.0.0",
                description = "Demo program for Kotlin backend",
                author = "JSONLang",
                license = "MIT",
                targets = listOf("kotlin"),
                encoding = "utf-8",
                location = sourceLocation,
                annotations = emptyList()
            ),
            imports = listOf(
                ImportDeclaration(
                    source = "kotlin.collections",
                    items = listOf("List", "Map"),
                    alias = emptyMap(),
                    version = null,
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            exports = emptyList(),
            types = listOf(
                TypeDeclaration(
                    name = "Person",
                    kind = TypeKind.STRUCT,
                    definition = TypeDefinition.StructDefinition(
                        mapOf(
                            "name" to FieldDefinition(PrimitiveType.StringType, false),
                            "age" to FieldDefinition(PrimitiveType.IntType, false),
                            "email" to FieldDefinition(PrimitiveType.NullableStringType, true)
                        )
                    ),
                    constraints = emptyList(),
                    visibility = Visibility.PUBLIC,
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            constants = listOf(
                ConstantDeclaration(
                    name = "DEFAULT_AGE",
                    type = PrimitiveType.IntType,
                    value = LiteralExpression.IntLiteral(18, sourceLocation, emptyList()),
                    visibility = Visibility.PUBLIC,
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            functions = listOf(
                FunctionDeclaration(
                    name = "greet",
                    modifiers = FunctionModifiers(
                        async = false,
                        inline = false,
                        pure = true,
                        external = false
                    ),
                    generics = emptyList(),
                    parameters = listOf(
                        Parameter(
                            name = "name",
                            type = PrimitiveType.StringType,
                            optional = false,
                            defaultValue = null,
                            annotations = emptyList()
                        )
                    ),
                    returnType = PrimitiveType.StringType,
                    body = listOf(
                        ExpressionStatement(
                            expression = BinaryExpression(
                                left = LiteralExpression.StringLiteral("Hello, ", sourceLocation, emptyList()),
                                operator = BinaryOperator.PLUS,
                                right = IdentifierExpression("name", sourceLocation, emptyList()),
                                location = sourceLocation,
                                annotations = emptyList()
                            ),
                            location = sourceLocation,
                            annotations = emptyList()
                        )
                    ),
                    visibility = Visibility.PUBLIC,
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            macros = emptyList(),
            dsls = emptyList(),
            location = sourceLocation,
            annotations = emptyList()
        )
    }
    
    private operator fun String.times(n: Int): String = repeat(n)
}

fun main() {
    KotlinBackendDemo.main()
}
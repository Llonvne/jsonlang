package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * 演示脚本：展示 JSONLang 到 Go 和 Kotlin 的代码生成结果
 */
fun main() {
    println("=== JSONLang Backend Code Generation Demo ===")
    println()
    
    val goBackend = GoBackend()
    val kotlinBackend = KotlinBackend()
    val location = SourceLocation("demo.lang.json", 1, 1, 1, 1, 0, 100)
    
    // 创建一个示例程序
    val program = createDemoProgram(location)
    
    println("=== Input JSONLang Program Structure ===")
    println("Program: ${program.metadata.name}")
    println("Types: ${program.types.map { it.name }}")
    println("Constants: ${program.constants.map { it.name }}")
    println("Functions: ${program.functions.map { it.name }}")
    println()
    
    // 生成 Go 代码
    println("=== Go Backend Output ===")
    val goResult = goBackend.generateProgram(program)
    if (goResult.isSuccess) {
        println(goResult.code)
        println()
        println("Go Imports: ${goResult.imports}")
        println("Go Metadata: ${goResult.metadata}")
    } else {
        println("Go generation failed:")
        goResult.errors.forEach { println("ERROR: $it") }
    }
    
    println()
    println("=" * 60)
    println()
    
    // 生成 Kotlin 代码
    println("=== Kotlin Backend Output ===")
    val kotlinResult = kotlinBackend.generateProgram(program)
    if (kotlinResult.isSuccess) {
        println(kotlinResult.code)
        println()
        println("Kotlin Imports: ${kotlinResult.imports}")
        println("Kotlin Metadata: ${kotlinResult.metadata}")
    } else {
        println("Kotlin generation failed:")
        kotlinResult.errors.forEach { println("ERROR: $it") }
    }
    
    println()
    println("=== Comparison Summary ===")
    println("Go Success: ${goResult.isSuccess}")
    println("Kotlin Success: ${kotlinResult.isSuccess}")
    println("Go Code Lines: ${goResult.code.lines().size}")
    println("Kotlin Code Lines: ${kotlinResult.code.lines().size}")
    
    // 展示特定特性的对比
    println()
    println("=== Feature Comparison ===")
    
    println("Async Support:")
    println("  Go: ${goBackend.supportsAsync()} (uses goroutines and channels)")
    println("  Kotlin: ${kotlinBackend.supportsAsync()} (uses suspend functions)")
    
    println("Generics Support:")
    println("  Go: ${goBackend.supportsGenerics()} (Go 1.18+ generics)")
    println("  Kotlin: ${kotlinBackend.supportsGenerics()} (native generics)")
    
    println()
    println("=== Type Mapping Examples ===")
    val goTypeMapping = goBackend.getTypeMapping()
    val kotlinTypeMapping = kotlinBackend.getTypeMapping()
    
    val testTypes = listOf(
        PrimitiveType.StringType,
        PrimitiveType.IntType,
        PrimitiveType.BooleanType,
        CompositeType.ArrayType(PrimitiveType.IntType),
        CompositeType.ObjectType(PrimitiveType.StringType, PrimitiveType.IntType),
        CompositeType.FunctionType(listOf(PrimitiveType.IntType), PrimitiveType.StringType)
    )
    
    testTypes.forEach { type ->
        val goType = goTypeMapping.mapType(type)
        val kotlinType = kotlinTypeMapping.mapType(type)
        println("${type.name}: Go='$goType', Kotlin='$kotlinType'")
    }
}

private fun createDemoProgram(location: SourceLocation): Program {
    return Program(
        metadata = Metadata("demo", "1.0.0", location = location),
        imports = listOf(
            ImportDeclaration("math", location = location)
        ),
        exports = emptyList(),
        types = listOf(
            // 结构体类型
            TypeDeclaration(
                "Person",
                TypeKind.STRUCT,
                TypeDefinition.StructDefinition(
                    mapOf(
                        "name" to FieldDefinition(PrimitiveType.StringType),
                        "age" to FieldDefinition(PrimitiveType.IntType),
                        "email" to FieldDefinition(PrimitiveType.NullableStringType, optional = true)
                    )
                ),
                location = location
            ),
            // 枚举类型
            TypeDeclaration(
                "Status",
                TypeKind.ENUM,
                TypeDefinition.EnumDefinition(
                    mapOf(
                        "Active" to null,
                        "Inactive" to null,
                        "Error" to PrimitiveType.StringType
                    )
                ),
                location = location
            )
        ),
        constants = listOf(
            ConstantDeclaration(
                "MAX_USERS",
                PrimitiveType.IntType,
                LiteralExpression.IntLiteral(1000, location),
                location = location
            )
        ),
        functions = listOf(
            // 简单函数
            FunctionDeclaration(
                "greet",
                FunctionModifiers(),
                emptyList(),
                listOf(Parameter("name", PrimitiveType.StringType)),
                PrimitiveType.StringType,
                listOf(
                    JumpStatement.ReturnStatement(
                        BinaryExpression(
                            LiteralExpression.StringLiteral("Hello, ", location),
                            BinaryOperator.PLUS,
                            IdentifierExpression("name", location),
                            location
                        ),
                        location
                    )
                ),
                Visibility.PUBLIC,
                location
            ),
            // 异步函数
            FunctionDeclaration(
                "fetchUserData",
                FunctionModifiers(async = true),
                emptyList(),
                listOf(Parameter("userId", PrimitiveType.IntType)),
                UserDefinedType("Person"),
                listOf(
                    VariableDeclaration(
                        "user",
                        UserDefinedType("Person"),
                        ObjectExpression(
                            listOf(
                                ObjectProperty(
                                    LiteralExpression.StringLiteral("name", location),
                                    LiteralExpression.StringLiteral("John Doe", location)
                                ),
                                ObjectProperty(
                                    LiteralExpression.StringLiteral("age", location),
                                    LiteralExpression.IntLiteral(30, location)
                                )
                            ),
                            location
                        ),
                        false,
                        location
                    ),
                    JumpStatement.ReturnStatement(
                        IdentifierExpression("user", location),
                        location
                    )
                ),
                Visibility.PUBLIC,
                location
            ),
            // 泛型函数
            FunctionDeclaration(
                "identity",
                FunctionModifiers(inline = true),
                listOf(GenericParameter("T")),
                listOf(Parameter("value", GenericType("T"))),
                GenericType("T"),
                listOf(
                    JumpStatement.ReturnStatement(
                        IdentifierExpression("value", location),
                        location
                    )
                ),
                Visibility.PUBLIC,
                location
            ),
            // 主函数
            FunctionDeclaration(
                "main",
                FunctionModifiers(),
                emptyList(),
                emptyList(),
                PrimitiveType.UnitType,
                listOf(
                    ExpressionStatement(
                        CallExpression(
                            IdentifierExpression("println", location),
                            listOf(
                                Argument(
                                    value = CallExpression(
                                        IdentifierExpression("greet", location),
                                        listOf(
                                            Argument(value = LiteralExpression.StringLiteral("World", location))
                                        ),
                                        location
                                    )
                                )
                            ),
                            location
                        ),
                        location
                    )
                ),
                location = location
            )
        ),
        macros = emptyList(),
        dsls = emptyList(),
        location = location
    )
}

private operator fun String.times(n: Int): String = this.repeat(n)
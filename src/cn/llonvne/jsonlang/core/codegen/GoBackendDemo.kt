package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * Go 后端演示
 * 展示如何使用 Go 后端生成代码
 */
object GoBackendDemo {
    
    private val dummyLocation = SourceLocation("demo.json", 1, 1, 1, 1, 0, 0)
    
    fun main() {
        println("=== JSONLang Go Backend Demo ===\n")
        
        // 初始化后端注册表
        BackendRegistration.initialize()
        
        // 创建 Go 后端
        val backend = BackendRegistry.create("go") ?: error("Go backend not found")
        
        // 演示基础类型映射
        demonstrateTypeMapping(backend)
        
        // 演示表达式生成
        demonstrateExpressions(backend)
        
        // 演示语句生成
        demonstrateStatements(backend)
        
        // 演示类型声明
        demonstrateTypeDeclarations(backend)
        
        // 演示函数声明
        demonstrateFunctionDeclarations(backend)
        
        // 演示完整程序生成
        demonstrateCompleteProgram(backend)
        
        // 演示 Go 特有功能
        demonstrateGoSpecificFeatures(backend)
    }
    
    private fun demonstrateTypeMapping(backend: Backend) {
        println("1. Type Mapping Demo:")
        println("====================")
        
        val typeMapping = backend.getTypeMapping()
        val types = listOf(
            PrimitiveType.StringType to "string",
            PrimitiveType.IntType to "int", 
            PrimitiveType.LongType to "int64",
            PrimitiveType.DoubleType to "float64",
            PrimitiveType.BooleanType to "bool",
            PrimitiveType.AnyType to "interface{}",
            CompositeType.ArrayType(PrimitiveType.IntType) to "[]int",
            CompositeType.ObjectType(PrimitiveType.StringType, PrimitiveType.IntType) to "map[string]int",
            CompositeType.OptionalType(PrimitiveType.StringType) to "*string"
        )
        
        types.forEach { (type, expected) ->
            val mapped = when (type) {
                is PrimitiveType -> typeMapping.mapPrimitiveType(type)
                is CompositeType -> typeMapping.mapCompositeType(type)
                else -> backend.generateType(type)
            }
            println("${type.getDisplayName()} -> $mapped")
            assert(mapped == expected) { "Expected $expected, got $mapped" }
        }
        println()
    }
    
    private fun demonstrateExpressions(backend: Backend) {
        println("2. Expression Generation Demo:")
        println("==============================")
        
        // 字面量表达式
        val stringLit = LiteralExpression.StringLiteral("Hello, Go!", dummyLocation)
        println("String literal: ${backend.generateExpression(stringLit)}")
        
        val intLit = LiteralExpression.IntLiteral(42, dummyLocation)
        println("Int literal: ${backend.generateExpression(intLit)}")
        
        val boolLit = LiteralExpression.BooleanLiteral(true, dummyLocation)
        println("Bool literal: ${backend.generateExpression(boolLit)}")
        
        val nullLit = LiteralExpression.NullLiteral(dummyLocation)
        println("Null literal: ${backend.generateExpression(nullLit)}")
        
        // 二元表达式
        val binaryExpr = BinaryExpression(
            LiteralExpression.IntLiteral(10, dummyLocation),
            BinaryOperator.PLUS,
            LiteralExpression.IntLiteral(5, dummyLocation),
            dummyLocation
        )
        println("Binary expression: ${backend.generateExpression(binaryExpr)}")
        
        // 函数调用
        val callExpr = CallExpression(
            IdentifierExpression("fmt.Println", dummyLocation),
            listOf(Argument(value = stringLit)),
            dummyLocation
        )
        println("Function call: ${backend.generateExpression(callExpr)}")
        
        // 数组表达式
        val arrayExpr = ArrayExpression(
            listOf(intLit, LiteralExpression.IntLiteral(24, dummyLocation)),
            dummyLocation
        )
        println("Array expression: ${backend.generateExpression(arrayExpr)}")
        
        // Lambda 表达式
        val lambdaExpr = LambdaExpression(
            listOf(Parameter("x", PrimitiveType.IntType)),
            BinaryExpression(
                IdentifierExpression("x", dummyLocation),
                BinaryOperator.MULTIPLY,
                LiteralExpression.IntLiteral(2, dummyLocation),
                dummyLocation
            ),
            dummyLocation
        )
        println("Lambda expression: ${backend.generateExpression(lambdaExpr)}")
        println()
    }
    
    private fun demonstrateStatements(backend: Backend) {
        println("3. Statement Generation Demo:")
        println("=============================")
        
        // 变量声明
        val varDecl = VariableDeclaration(
            "message",
            PrimitiveType.StringType,
            LiteralExpression.StringLiteral("Hello", dummyLocation),
            false,
            dummyLocation
        )
        println("Variable declaration: ${backend.generateStatement(varDecl)}")
        
        // 条件语句
        val ifStmt = ConditionalStatement(
            LiteralExpression.BooleanLiteral(true, dummyLocation),
            ExpressionStatement(
                LiteralExpression.StringLiteral("then branch", dummyLocation),
                dummyLocation
            ),
            ExpressionStatement(
                LiteralExpression.StringLiteral("else branch", dummyLocation),
                dummyLocation
            ),
            dummyLocation
        )
        println("Conditional statement:")
        println(backend.generateStatement(ifStmt))
        
        // For 循环
        val forLoop = LoopStatement.ForLoop(
            "item",
            IdentifierExpression("items", dummyLocation),
            ExpressionStatement(
                CallExpression(
                    IdentifierExpression("fmt.Println", dummyLocation),
                    listOf(Argument(value = IdentifierExpression("item", dummyLocation))),
                    dummyLocation
                ),
                dummyLocation
            ),
            dummyLocation
        )
        println("For loop:")
        println(backend.generateStatement(forLoop))
        
        // Return 语句
        val returnStmt = JumpStatement.ReturnStatement(
            LiteralExpression.IntLiteral(42, dummyLocation),
            dummyLocation
        )
        println("Return statement: ${backend.generateStatement(returnStmt)}")
        println()
    }
    
    private fun demonstrateTypeDeclarations(backend: Backend) {
        println("4. Type Declaration Demo:")
        println("=========================")
        
        // 结构体声明
        val structFields = mapOf(
            "name" to FieldDefinition(PrimitiveType.StringType),
            "age" to FieldDefinition(PrimitiveType.IntType),
            "email" to FieldDefinition(PrimitiveType.StringType, optional = true)
        )
        val structDecl = TypeDeclaration(
            "Person",
            TypeKind.STRUCT,
            TypeDefinition.StructDefinition(structFields),
            location = dummyLocation
        )
        println("Struct declaration:")
        println(backend.generateDeclaration(structDecl))
        
        // 枚举声明
        val enumVariants = mapOf(
            "RED" to null,
            "GREEN" to null,
            "BLUE" to null
        )
        val enumDecl = TypeDeclaration(
            "Color",
            TypeKind.ENUM,
            TypeDefinition.EnumDefinition(enumVariants),
            location = dummyLocation
        )
        println("Enum declaration:")
        println(backend.generateDeclaration(enumDecl))
        
        // 类型别名
        val aliasDecl = TypeDeclaration(
            "UserID",
            TypeKind.ALIAS,
            TypeDefinition.AliasDefinition(PrimitiveType.IntType),
            location = dummyLocation
        )
        println("Type alias: ${backend.generateDeclaration(aliasDecl)}")
        println()
    }
    
    private fun demonstrateFunctionDeclarations(backend: Backend) {
        println("5. Function Declaration Demo:")
        println("=============================")
        
        // 简单函数
        val simpleFunc = FunctionDeclaration(
            "add",
            FunctionModifiers(),
            emptyList(),
            listOf(
                Parameter("a", PrimitiveType.IntType),
                Parameter("b", PrimitiveType.IntType)
            ),
            PrimitiveType.IntType,
            listOf(
                JumpStatement.ReturnStatement(
                    BinaryExpression(
                        IdentifierExpression("a", dummyLocation),
                        BinaryOperator.PLUS,
                        IdentifierExpression("b", dummyLocation),
                        dummyLocation
                    ),
                    dummyLocation
                )
            ),
            Visibility.PUBLIC,
            dummyLocation
        )
        println("Simple function:")
        println(backend.generateDeclaration(simpleFunc))
        
        // 异步函数
        val asyncFunc = FunctionDeclaration(
            "processAsync",
            FunctionModifiers(async = true),
            emptyList(),
            listOf(Parameter("data", PrimitiveType.StringType)),
            PrimitiveType.StringType,
            listOf(
                JumpStatement.ReturnStatement(
                    BinaryExpression(
                        LiteralExpression.StringLiteral("Processed: ", dummyLocation),
                        BinaryOperator.PLUS,
                        IdentifierExpression("data", dummyLocation),
                        dummyLocation
                    ),
                    dummyLocation
                )
            ),
            location = dummyLocation
        )
        println("Async function:")
        println(backend.generateDeclaration(asyncFunc))
        
        // 泛型函数
        val genericFunc = FunctionDeclaration(
            "identity",
            FunctionModifiers(),
            listOf(GenericParameter("T")),
            listOf(Parameter("value", GenericType("T"))),
            GenericType("T"),
            listOf(
                JumpStatement.ReturnStatement(
                    IdentifierExpression("value", dummyLocation),
                    dummyLocation
                )
            ),
            location = dummyLocation
        )
        println("Generic function:")
        println(backend.generateDeclaration(genericFunc))
        println()
    }
    
    private fun demonstrateCompleteProgram(backend: Backend) {
        println("6. Complete Program Demo:")
        println("=========================")
        
        val program = createSampleProgram()
        val result = backend.generateProgram(program)
        
        if (result.isSuccess) {
            println("Generated Go code:")
            println("------------------")
            println(result.code)
        } else {
            println("Errors:")
            result.errors.forEach { println("  - $it") }
        }
        
        if (result.warnings.isNotEmpty()) {
            println("\nWarnings:")
            result.warnings.forEach { println("  - $it") }
        }
        println()
    }
    
    private fun demonstrateGoSpecificFeatures(backend: Backend) {
        println("7. Go-Specific Features Demo:")
        println("=============================")
        
        // 演示 Go 的错误处理模式
        println("Error handling pattern:")
        val tryBlock = BlockStatement(
            listOf(
                ExpressionStatement(
                    LiteralExpression.StringLiteral("risky operation", dummyLocation),
                    dummyLocation
                )
            ),
            dummyLocation
        )
        val tryCatch = TryCatchStatement(
            tryBlock,
            listOf(
                CatchClause(
                    PrimitiveType.AnyType,
                    "err",
                    BlockStatement(
                        listOf(
                            ExpressionStatement(
                                LiteralExpression.StringLiteral("handle error", dummyLocation),
                                dummyLocation
                            )
                        ),
                        dummyLocation
                    ),
                    dummyLocation
                )
            ),
            location = dummyLocation
        )
        println(backend.generateStatement(tryCatch))
        
        // 演示 Go 的并发模式
        println("Concurrency pattern (goroutines):")
        val concurrentFunc = FunctionDeclaration(
            "worker",
            FunctionModifiers(async = true),
            emptyList(),
            listOf(Parameter("id", PrimitiveType.IntType)),
            PrimitiveType.StringType,
            listOf(
                JumpStatement.ReturnStatement(
                    BinaryExpression(
                        LiteralExpression.StringLiteral("Worker ", dummyLocation),
                        BinaryOperator.PLUS,
                        IdentifierExpression("id", dummyLocation),
                        dummyLocation
                    ),
                    dummyLocation
                )
            ),
            location = dummyLocation
        )
        println(backend.generateDeclaration(concurrentFunc))
        
        // 演示 Go 的包管理
        println("Package and import handling:")
        val importDecl = ImportDeclaration(
            "github.com/example/mypackage",
            version = "v1.2.3",
            location = dummyLocation
        )
        println("Import: ${backend.generateDeclaration(importDecl)}")
        println()
    }
    
    private fun createSampleProgram(): Program {
        val metadata = Metadata(
            "sample-go-program",
            "1.0.0",
            "A sample JSONLang program for Go backend demo",
            "JSONLang Team",
            "MIT",
            listOf("go"),
            location = dummyLocation
        )
        
        val imports = listOf(
            ImportDeclaration("fmt", location = dummyLocation),
            ImportDeclaration("time", location = dummyLocation)
        )
        
        val types = listOf(
            TypeDeclaration(
                "User",
                TypeKind.STRUCT,
                TypeDefinition.StructDefinition(
                    mapOf(
                        "id" to FieldDefinition(PrimitiveType.IntType),
                        "name" to FieldDefinition(PrimitiveType.StringType),
                        "email" to FieldDefinition(PrimitiveType.StringType, optional = true)
                    )
                ),
                location = dummyLocation
            )
        )
        
        val constants = listOf(
            ConstantDeclaration(
                "MAX_USERS",
                PrimitiveType.IntType,
                LiteralExpression.IntLiteral(1000, dummyLocation),
                location = dummyLocation
            )
        )
        
        val functions = listOf(
            FunctionDeclaration(
                "main",
                FunctionModifiers(),
                emptyList(),
                emptyList(),
                PrimitiveType.UnitType,
                listOf(
                    ExpressionStatement(
                        CallExpression(
                            IdentifierExpression("fmt.Println", dummyLocation),
                            listOf(Argument(value = LiteralExpression.StringLiteral("Hello from JSONLang!", dummyLocation))),
                            dummyLocation
                        ),
                        dummyLocation
                    )
                ),
                location = dummyLocation
            ),
            FunctionDeclaration(
                "createUser",
                FunctionModifiers(),
                emptyList(),
                listOf(
                    Parameter("id", PrimitiveType.IntType),
                    Parameter("name", PrimitiveType.StringType)
                ),
                UserDefinedType.StructType("User", emptyMap(), location = dummyLocation),
                listOf(
                    JumpStatement.ReturnStatement(
                        ObjectExpression(
                            listOf(
                                ObjectProperty(
                                    LiteralExpression.StringLiteral("id", dummyLocation),
                                    IdentifierExpression("id", dummyLocation)
                                ),
                                ObjectProperty(
                                    LiteralExpression.StringLiteral("name", dummyLocation),
                                    IdentifierExpression("name", dummyLocation)
                                )
                            ),
                            dummyLocation
                        ),
                        dummyLocation
                    )
                ),
                location = dummyLocation
            )
        )
        
        return Program(
            metadata,
            imports,
            emptyList(),
            types,
            constants,
            functions,
            emptyList(),
            emptyList(),
            dummyLocation
        )
    }
}

fun main() {
    GoBackendDemo.main()
}
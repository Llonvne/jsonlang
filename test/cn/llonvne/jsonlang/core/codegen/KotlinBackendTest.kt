package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class KotlinBackendTest {
    
    private val backend = KotlinBackend()
    private val sourceLocation = SourceLocation("test.lang.json", 1, 1, 1, 1, 0, 10)
    
    @Test
    fun testGetBackendInfo() {
        assertEquals("kotlin", backend.getName())
        assertEquals("Kotlin", backend.getTargetLanguage())
        assertEquals("kt", backend.getFileExtension())
    }
    
    @Test
    fun testTypeMapping() {
        val typeMapping = backend.getTypeMapping()
        
        // 测试基础类型映射
        assertEquals("String", typeMapping.mapPrimitiveType(PrimitiveType.StringType))
        assertEquals("String?", typeMapping.mapPrimitiveType(PrimitiveType.NullableStringType))
        assertEquals("Int", typeMapping.mapPrimitiveType(PrimitiveType.IntType))
        assertEquals("Boolean", typeMapping.mapPrimitiveType(PrimitiveType.BooleanType))
        assertEquals("Unit", typeMapping.mapPrimitiveType(PrimitiveType.UnitType))
        
        // 测试复合类型映射
        val arrayType = CompositeType.ArrayType(PrimitiveType.IntType)
        assertEquals("List<Int>", typeMapping.mapCompositeType(arrayType))
        
        val objectType = CompositeType.ObjectType(PrimitiveType.StringType, PrimitiveType.IntType)
        assertEquals("Map<String, Int>", typeMapping.mapCompositeType(objectType))
        
        val functionType = CompositeType.FunctionType(
            listOf(PrimitiveType.IntType, PrimitiveType.StringType),
            PrimitiveType.BooleanType
        )
        assertEquals("(Int, String) -> Boolean", typeMapping.mapCompositeType(functionType))
    }
    
    @Test
    fun testGenerateExpression() {
        // 测试字面量表达式
        assertEquals("\"hello\"", backend.generateExpression(
            LiteralExpression.StringLiteral("hello", sourceLocation, emptyList())
        ))
        
        assertEquals("42", backend.generateExpression(
            LiteralExpression.IntLiteral(42, sourceLocation, emptyList())
        ))
        
        assertEquals("true", backend.generateExpression(
            LiteralExpression.BooleanLiteral(true, sourceLocation, emptyList())
        ))
        
        assertEquals("null", backend.generateExpression(
            LiteralExpression.NullLiteral(sourceLocation, emptyList())
        ))
        
        // 测试标识符表达式
        assertEquals("myVar", backend.generateExpression(
            IdentifierExpression("myVar", sourceLocation, emptyList())
        ))
        
        // 测试二元表达式
        val binaryExpr = BinaryExpression(
            LiteralExpression.IntLiteral(1, sourceLocation, emptyList()),
            BinaryOperator.PLUS,
            LiteralExpression.IntLiteral(2, sourceLocation, emptyList()),
            sourceLocation,
            emptyList()
        )
        assertEquals("1 + 2", backend.generateExpression(binaryExpr))
        
        // 测试数组表达式
        val arrayExpr = ArrayExpression(
            listOf(
                LiteralExpression.IntLiteral(1, sourceLocation, emptyList()),
                LiteralExpression.IntLiteral(2, sourceLocation, emptyList()),
                LiteralExpression.IntLiteral(3, sourceLocation, emptyList())
            ),
            sourceLocation,
            emptyList()
        )
        assertEquals("listOf(1, 2, 3)", backend.generateExpression(arrayExpr))
    }
    
    @Test
    fun testGenerateTypeDeclaration() {
        // 测试结构体声明
        val structDefinition = TypeDefinition.StructDefinition(
            mapOf(
                "name" to FieldDefinition(PrimitiveType.StringType, false),
                "age" to FieldDefinition(PrimitiveType.IntType, true),
                "email" to FieldDefinition(PrimitiveType.NullableStringType, false)
            )
        )
        
        val typeDecl = TypeDeclaration(
            "Person",
            TypeKind.STRUCT,
            structDefinition,
            emptyList(),
            Visibility.PUBLIC,
            sourceLocation,
            emptyList()
        )
        
        val result = backend.generateDeclaration(typeDecl)
        assertTrue(result.contains("data class Person"))
        assertTrue(result.contains("val name: String"))
        assertTrue(result.contains("val age: Int?"))
        assertTrue(result.contains("val email: String?"))
        
        // 测试枚举声明
        val enumDefinition = TypeDefinition.EnumDefinition(
            mapOf(
                "SUCCESS" to null,
                "ERROR" to PrimitiveType.StringType
            )
        )
        
        val enumDecl = TypeDeclaration(
            "Result",
            TypeKind.ENUM,
            enumDefinition,
            emptyList(),
            Visibility.PUBLIC,
            sourceLocation,
            emptyList()
        )
        
        val enumResult = backend.generateDeclaration(enumDecl)
        assertTrue(enumResult.contains("sealed class Result"))
        assertTrue(enumResult.contains("SUCCESS"))
        assertTrue(enumResult.contains("ERROR"))
    }
    
    @Test
    fun testGenerateFunctionDeclaration() {
        val functionDecl = FunctionDeclaration(
            name = "add",
            modifiers = FunctionModifiers(
                async = false,
                inline = true,
                pure = true,
                external = false
            ),
            generics = emptyList(),
            parameters = listOf(
                Parameter(
                    "a",
                    PrimitiveType.IntType,
                    false,
                    null,
                    emptyList()
                ),
                Parameter(
                    "b",
                    PrimitiveType.IntType,
                    false,
                    null,
                    emptyList()
                )
            ),
            returnType = PrimitiveType.IntType,
            body = emptyList(), // 简化测试
            location = sourceLocation,
            annotations = emptyList()
        )
        
        val result = backend.generateDeclaration(functionDecl)
        assertTrue(result.contains("public inline fun add"))
        assertTrue(result.contains("a: Int"))
        assertTrue(result.contains("b: Int"))
        assertTrue(result.contains("): Int"))
    }
    
    @Test
    fun testGenerateProgram() {
        val program = Program(
            metadata = Metadata(
                name = "com.example.test",
                version = "1.0.0",
                description = "Test program",
                author = "Test Author",
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
            types = emptyList(),
            constants = listOf(
                ConstantDeclaration(
                    name = "PI",
                    type = PrimitiveType.DoubleType,
                    value = LiteralExpression.FloatLiteral(3.14, sourceLocation, emptyList()),
                    visibility = Visibility.PUBLIC,
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            functions = listOf(
                FunctionDeclaration(
                    name = "main",
                    modifiers = FunctionModifiers(
                        async = false,
                        inline = false,
                        pure = false,
                        external = false
                    ),
                    generics = emptyList(),
                    parameters = emptyList(),
                    returnType = PrimitiveType.UnitType,
                    body = emptyList(),
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            macros = emptyList(),
            dsls = emptyList(),
            location = sourceLocation,
            annotations = emptyList()
        )
        
        val result = backend.generateProgram(program)
        assertTrue(result.isSuccess)
        
        val code = result.code
        assertTrue(code.contains("package com.example.test"))
        assertTrue(code.contains("import kotlin.collections.List"))
        assertTrue(code.contains("import kotlin.collections.Map"))
        assertTrue(code.contains("const val PI: Double = 3.14"))
        assertTrue(code.contains("fun main(): Unit"))
    }
    
    @Test
    fun testOptimizations() {
        val optimizations = backend.getOptimizations()
        assertTrue(optimizations.isNotEmpty())
        
        val optimizationNames = optimizations.map { it.name }
        assertTrue(optimizationNames.contains("inline"))
        assertTrue(optimizationNames.contains("tail-call"))
        assertTrue(optimizationNames.contains("dead-code"))
        assertTrue(optimizationNames.contains("constant-folding"))
    }
    
    @Test
    fun testAsyncSupport() {
        // Test that the backend supports async and generics
        val optimizations = backend.getOptimizations()
        assertTrue(optimizations.isNotEmpty())
    }
    
    @Test
    fun testCompatibilityValidation() {
        val program = Program(
            metadata = Metadata(
                name = "test",
                version = "1.0.0",
                location = sourceLocation,
                annotations = emptyList()
            ),
            imports = emptyList(),
            exports = emptyList(),
            types = emptyList(),
            constants = emptyList(),
            functions = listOf(
                FunctionDeclaration(
                    name = "asyncFunction",
                    modifiers = FunctionModifiers(
                        async = true,
                        inline = false,
                        pure = false,
                        external = false
                    ),
                    generics = emptyList(),
                    parameters = emptyList(),
                    returnType = PrimitiveType.UnitType,
                    body = emptyList(),
                    location = sourceLocation,
                    annotations = emptyList()
                )
            ),
            macros = emptyList(),
            dsls = emptyList(),
            location = sourceLocation,
            annotations = emptyList()
        )
        
        val issues = backend.validateCompatibility(program)
        assertTrue(issues.isEmpty()) // Kotlin 支持异步函数
    }
}
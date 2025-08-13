package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.PrimitiveType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * AST 序列化测试
 */
class ASTSerializationTest {
    
    @Test
    fun `测试基本 AST 节点序列化`() {
        // 创建一个简单的程序节点
        val location = SourceLocation.unknown()
        val metadata = Metadata(
            name = "test",
            version = "1.0.0",
            description = "测试程序",
            location = location
        )
        
        val program = Program(
            metadata = metadata,
            imports = emptyList(),
            exports = emptyList(),
            types = emptyList(),
            constants = emptyList(),
            functions = emptyList(),
            macros = emptyList(),
            dsls = emptyList(),
            location = location
        )
        
        // 序列化
        val json = ASTSerialization.serializeToJson(program)
        assertNotNull(json)
        assertTrue(json.contains("test"))
        assertTrue(json.contains("1.0.0"))
        
        // 反序列化
        val deserializedProgram = ASTSerialization.deserializeFromJson(json) as Program
        assertEquals(program.metadata.name, deserializedProgram.metadata.name)
        assertEquals(program.metadata.version, deserializedProgram.metadata.version)
        assertEquals(program.metadata.description, deserializedProgram.metadata.description)
    }
    
    @Test
    fun `测试表达式序列化`() {
        val location = SourceLocation.unknown()
        
        // 创建字符串字面量
        val stringLiteral = LiteralExpression.StringLiteral(
            value = "Hello, World!",
            location = location,
            inferredType = PrimitiveType.StringType
        )
        
        // 序列化
        val json = ASTSerialization.serializeToJson(stringLiteral)
        assertNotNull(json)
        assertTrue(json.contains("Hello, World!"))
        
        // 反序列化
        val deserializedLiteral = ASTSerialization.deserializeFromJson(json) as LiteralExpression.StringLiteral
        assertEquals(stringLiteral.value, deserializedLiteral.value)
    }
    
    @Test
    fun `测试二元表达式序列化`() {
        val location = SourceLocation.unknown()
        
        // 创建二元表达式: 1 + 2
        val left = LiteralExpression.IntLiteral(1, location, emptyList(), PrimitiveType.IntType)
        val right = LiteralExpression.IntLiteral(2, location, emptyList(), PrimitiveType.IntType)
        val binaryExpr = BinaryExpression(
            left = left,
            operator = BinaryOperator.PLUS,
            right = right,
            location = location,
            inferredType = PrimitiveType.IntType
        )
        
        // 序列化
        val json = ASTSerialization.serializeToJson(binaryExpr)
        assertNotNull(json)
        
        // 反序列化
        val deserializedExpr = ASTSerialization.deserializeFromJson(json) as BinaryExpression
        assertEquals(binaryExpr.operator, deserializedExpr.operator)
        assertEquals((binaryExpr.left as LiteralExpression.IntLiteral).value, 
                    (deserializedExpr.left as LiteralExpression.IntLiteral).value)
        assertEquals((binaryExpr.right as LiteralExpression.IntLiteral).value, 
                    (deserializedExpr.right as LiteralExpression.IntLiteral).value)
    }
    
    @Test
    fun `测试 JSONLang 文件格式序列化`() {
        val location = SourceLocation.unknown()
        val metadata = Metadata(
            name = "math",
            version = "1.0.0",
            description = "数学工具库",
            author = "JSONLang Team",
            license = "MIT",
            targets = listOf("kotlin", "javascript"),
            location = location
        )
        
        // 创建一个简单的函数
        val addFunction = FunctionDeclaration(
            name = "add",
            modifiers = FunctionModifiers(),
            parameters = listOf(
                Parameter("a", PrimitiveType.IntType),
                Parameter("b", PrimitiveType.IntType)
            ),
            returnType = PrimitiveType.IntType,
            body = listOf(
                JumpStatement.ReturnStatement(
                    value = BinaryExpression(
                        left = IdentifierExpression("a", location, emptyList(), PrimitiveType.IntType),
                        operator = BinaryOperator.PLUS,
                        right = IdentifierExpression("b", location, emptyList(), PrimitiveType.IntType),
                        location = location,
                        inferredType = PrimitiveType.IntType
                    ),
                    location = location
                )
            ),
            location = location
        )
        
        val program = Program(
            metadata = metadata,
            imports = emptyList(),
            exports = emptyList(),
            types = emptyList(),
            constants = emptyList(),
            functions = listOf(addFunction),
            macros = emptyList(),
            dsls = emptyList(),
            location = location
        )
        
        // 序列化为 JSONLang 格式
        val jsonLang = ASTSerialization.serializeProgramToJsonLang(program)
        assertNotNull(jsonLang)
        assertTrue(jsonLang.contains("math"))
        assertTrue(jsonLang.contains("add"))
        assertTrue(jsonLang.contains("kotlin"))
        
        // 反序列化
        val deserializedProgram = ASTSerialization.deserializeProgramFromJsonLang(jsonLang)
        assertEquals(program.metadata.name, deserializedProgram.metadata.name)
        assertEquals(program.metadata.version, deserializedProgram.metadata.version)
        assertEquals(program.functions.size, deserializedProgram.functions.size)
        assertEquals(program.functions[0].name, deserializedProgram.functions[0].name)
    }
    
    @Test
    fun `测试导入声明序列化`() {
        val location = SourceLocation.unknown()
        val importDecl = ImportDeclaration(
            source = "./math.lang.json",
            version = "1.0.0",
            items = listOf("add", "subtract"),
            alias = mapOf("add" to "加法", "subtract" to "减法"),
            condition = ImportCondition(target = "kotlin"),
            location = location
        )
        
        // 序列化
        val json = ASTSerialization.serializeToJson(importDecl)
        assertNotNull(json)
        assertTrue(json.contains("math.lang.json"))
        assertTrue(json.contains("加法"))
        
        // 反序列化
        val deserializedImport = ASTSerialization.deserializeFromJson(json) as ImportDeclaration
        assertEquals(importDecl.source, deserializedImport.source)
        assertEquals(importDecl.version, deserializedImport.version)
        assertEquals(importDecl.items, deserializedImport.items)
        assertEquals(importDecl.alias, deserializedImport.alias)
        assertEquals(importDecl.condition?.target, deserializedImport.condition?.target)
    }
}
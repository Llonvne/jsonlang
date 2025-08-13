package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.Type
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ASTOperationsTest {
    
    private val location = SourceLocation.unknown()
    
    @Test
    fun testASTBuilder() {
        val builder = ASTOperations.builder(location)
        
        // 测试字面量构建
        val stringLit = builder.stringLiteral("hello")
        assertEquals("hello", stringLit.value)
        assertEquals(location, stringLit.location)
        
        val intLit = builder.intLiteral(42)
        assertEquals(42L, intLit.value)
        
        val floatLit = builder.floatLiteral(3.14)
        assertEquals(3.14, floatLit.value)
        
        val boolLit = builder.booleanLiteral(true)
        assertTrue(boolLit.value)
        
        val nullLit = builder.nullLiteral()
        assertEquals(location, nullLit.location)
        
        // 测试标识符构建
        val identifier = builder.identifier("myVar")
        assertEquals("myVar", identifier.name)
        
        // 测试二元表达式构建
        val binaryExpr = builder.binaryExpression(
            builder.intLiteral(1),
            BinaryOperator.PLUS,
            builder.intLiteral(2)
        )
        assertEquals(BinaryOperator.PLUS, binaryExpr.operator)
        assertTrue(binaryExpr.left is LiteralExpression.IntLiteral)
        assertTrue(binaryExpr.right is LiteralExpression.IntLiteral)
        
        // 测试一元表达式构建
        val unaryExpr = builder.unaryExpression(
            UnaryOperator.MINUS,
            builder.intLiteral(5)
        )
        assertEquals(UnaryOperator.MINUS, unaryExpr.operator)
        assertTrue(unaryExpr.operand is LiteralExpression.IntLiteral)
        
        // 测试函数调用构建
        val callExpr = builder.callExpression(
            builder.identifier("func"),
            listOf(
                builder.argument(value = builder.stringLiteral("arg1")),
                builder.argument("param", builder.intLiteral(42))
            )
        )
        assertTrue(callExpr.function is IdentifierExpression)
        assertEquals(2, callExpr.arguments.size)
        assertNull(callExpr.arguments[0].name)
        assertEquals("param", callExpr.arguments[1].name)
        
        // 测试成员访问构建
        val memberExpr = builder.memberExpression(
            builder.identifier("obj"),
            "property"
        )
        assertTrue(memberExpr.target is IdentifierExpression)
        assertEquals("property", memberExpr.property)
        assertFalse(memberExpr.computed)
        
        // 测试数组构建
        val arrayExpr = builder.arrayExpression(
            listOf(
                builder.intLiteral(1),
                builder.intLiteral(2),
                builder.intLiteral(3)
            )
        )
        assertEquals(3, arrayExpr.elements.size)
        
        // 测试对象构建
        val objectExpr = builder.objectExpression(
            listOf(
                builder.objectProperty(
                    builder.stringLiteral("key1"),
                    builder.stringLiteral("value1")
                ),
                builder.objectProperty(
                    builder.stringLiteral("key2"),
                    builder.intLiteral(42)
                )
            )
        )
        assertEquals(2, objectExpr.properties.size)
        
        // 测试条件表达式构建
        val conditionalExpr = builder.conditionalExpression(
            builder.booleanLiteral(true),
            builder.stringLiteral("then"),
            builder.stringLiteral("else")
        )
        assertTrue(conditionalExpr.condition is LiteralExpression.BooleanLiteral)
        assertTrue(conditionalExpr.thenExpression is LiteralExpression.StringLiteral)
        assertTrue(conditionalExpr.elseExpression is LiteralExpression.StringLiteral)
        
        // 测试宏调用构建
        val macroCall = builder.macroCall(
            "testMacro",
            listOf(builder.stringLiteral("arg"))
        )
        assertEquals("testMacro", macroCall.name)
        assertEquals(1, macroCall.arguments.size)
        
        // 测试宏参数占位符构建
        val placeholder = builder.macroParameterPlaceholder("param")
        assertEquals("param", placeholder.parameterName)
    }
    
    @Test
    fun testASTModifier() {
        val builder = ASTOperations.builder(location)
        val modifier = ASTOperations.modifier()
        
        // 测试表达式替换
        val original = builder.binaryExpression(
            builder.identifier("x"),
            BinaryOperator.PLUS,
            builder.identifier("y")
        )
        
        val target = builder.identifier("x")
        val replacement = builder.intLiteral(42)
        
        val modified = modifier.replaceExpression(original, target, replacement) as BinaryExpression
        assertTrue(modified.left is LiteralExpression.IntLiteral)
        assertEquals(42L, (modified.left as LiteralExpression.IntLiteral).value)
        assertTrue(modified.right is IdentifierExpression)
        
        // 测试嵌套替换
        val nested = builder.binaryExpression(
            builder.binaryExpression(
                builder.identifier("a"),
                BinaryOperator.MULTIPLY,
                builder.identifier("b")
            ),
            BinaryOperator.PLUS,
            builder.identifier("c")
        )
        
        val nestedTarget = builder.identifier("b")
        val nestedReplacement = builder.intLiteral(10)
        
        val nestedModified = modifier.replaceExpression(nested, nestedTarget, nestedReplacement)
        assertTrue(nestedModified is BinaryExpression)
        val nestedBinary = nestedModified as BinaryExpression
        assertTrue(nestedBinary.left is BinaryExpression)
        val leftBinary = nestedBinary.left as BinaryExpression
        assertTrue(leftBinary.right is LiteralExpression.IntLiteral)
        assertEquals(10L, (leftBinary.right as LiteralExpression.IntLiteral).value)
    }
    
    @Test
    fun testASTQuery() {
        val builder = ASTOperations.builder(location)
        val query = ASTOperations.query()
        
        // 创建一个复杂的 AST
        val ast = builder.binaryExpression(
            builder.callExpression(
                builder.identifier("func"),
                listOf(
                    builder.argument(value = builder.identifier("x")),
                    builder.argument(value = builder.intLiteral(42))
                )
            ),
            BinaryOperator.PLUS,
            builder.binaryExpression(
                builder.identifier("y"),
                BinaryOperator.MULTIPLY,
                builder.identifier("z")
            )
        )
        
        // 测试查找所有标识符
        val identifiers = query.findAll<IdentifierExpression>(ast)
        assertEquals(4, identifiers.size) // func, x, y, z
        val identifierNames = identifiers.map { it.name }.toSet()
        assertTrue(identifierNames.contains("func"))
        assertTrue(identifierNames.contains("x"))
        assertTrue(identifierNames.contains("y"))
        assertTrue(identifierNames.contains("z"))
        
        // 测试查找第一个标识符
        val firstIdentifier = query.findFirst<IdentifierExpression>(ast)
        assertNotNull(firstIdentifier)
        assertEquals("func", firstIdentifier?.name)
        
        // 测试查找特定名称的标识符引用
        val xReferences = query.findIdentifierReferences(ast, "x")
        assertEquals(1, xReferences.size)
        assertEquals("x", xReferences[0].name)
        
        // 测试查找函数调用
        val functionCalls = query.findFunctionCalls(ast)
        assertEquals(1, functionCalls.size)
        assertTrue(functionCalls[0].function is IdentifierExpression)
        assertEquals("func", (functionCalls[0].function as IdentifierExpression).name)
        
        // 测试查找特定函数的调用
        val funcCalls = query.findFunctionCalls(ast, "func")
        assertEquals(1, funcCalls.size)
        
        val nonExistentCalls = query.findFunctionCalls(ast, "nonexistent")
        assertEquals(0, nonExistentCalls.size)
        
        // 测试包含检查
        val intLiteral = builder.intLiteral(42)
        assertTrue(query.contains(ast, intLiteral))
        
        val notContained = builder.stringLiteral("not in ast")
        assertFalse(query.contains(ast, notContained))
        
        // 测试查找所有二元表达式
        val binaryExpressions = query.findAll<BinaryExpression>(ast)
        assertEquals(2, binaryExpressions.size) // 顶层的 PLUS 和右侧的 MULTIPLY
        
        // 测试查找所有字面量
        val literals = query.findAll<LiteralExpression>(ast)
        assertEquals(1, literals.size) // 只有一个 IntLiteral(42)
        assertTrue(literals[0] is LiteralExpression.IntLiteral)
        assertEquals(42L, (literals[0] as LiteralExpression.IntLiteral).value)
    }
    
    @Test
    fun testComplexASTOperations() {
        val builder = ASTOperations.builder(location)
        val modifier = ASTOperations.modifier()
        val query = ASTOperations.query()
        
        // 创建一个复杂的表达式: (a + b) * (c - d)
        val expr = builder.binaryExpression(
            builder.binaryExpression(
                builder.identifier("a"),
                BinaryOperator.PLUS,
                builder.identifier("b")
            ),
            BinaryOperator.MULTIPLY,
            builder.binaryExpression(
                builder.identifier("c"),
                BinaryOperator.MINUS,
                builder.identifier("d")
            )
        )
        
        // 查找所有标识符
        val identifiers = query.findAll<IdentifierExpression>(expr)
        assertEquals(4, identifiers.size)
        
        // 替换所有 'a' 为常量 1
        var modified: Expression = expr
        val aIdentifiers = query.findIdentifierReferences(expr, "a")
        for (aId in aIdentifiers) {
            modified = modifier.replaceExpression(modified, aId, builder.intLiteral(1))
        }
        
        // 验证替换结果
        val modifiedIdentifiers = query.findIdentifierReferences(modified, "a")
        assertEquals(0, modifiedIdentifiers.size) // 应该没有 'a' 了
        
        val literals = query.findAll<LiteralExpression.IntLiteral>(modified)
        assertEquals(1, literals.size)
        assertEquals(1L, literals[0].value)
        
        // 验证其他标识符仍然存在
        assertEquals(1, query.findIdentifierReferences(modified, "b").size)
        assertEquals(1, query.findIdentifierReferences(modified, "c").size)
        assertEquals(1, query.findIdentifierReferences(modified, "d").size)
    }
}
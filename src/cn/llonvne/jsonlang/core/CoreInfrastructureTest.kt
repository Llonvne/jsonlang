package cn.llonvne.jsonlang.core

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.PrimitiveType
import cn.llonvne.jsonlang.core.errors.*
import cn.llonvne.jsonlang.core.annotations.Annotation

/**
 * 核心基础设施测试
 * 验证 AST 节点、类型系统、错误报告等核心组件的基本功能
 */
object CoreInfrastructureTest {
    
    fun runTests() {
        println("开始运行核心基础设施测试...")
        
        testSourceLocation()
        testASTNodes()
        testTypeSystem()
        testErrorReporting()
        
        println("所有测试通过！")
    }
    
    private fun testSourceLocation() {
        println("测试源码位置跟踪...")
        
        val location = SourceLocation(
            file = "test.lang.json",
            startLine = 1,
            startColumn = 1,
            endLine = 1,
            endColumn = 10,
            startOffset = 0,
            endOffset = 9
        )
        
        assert(location.isValid()) { "位置信息应该有效" }
        assert(location.toString() == "test.lang.json:1:1-10") { "位置格式化错误" }
        
        println("✓ 源码位置跟踪测试通过")
    }
    
    private fun testASTNodes() {
        println("测试 AST 节点...")
        
        val location = SourceLocation.unknown()
        
        // 测试字符串字面量
        val stringLiteral = LiteralExpression.StringLiteral(
            value = "Hello, JSONLang!",
            location = location
        )
        
        assert(stringLiteral.nodeType == "StringLiteral") { "节点类型错误" }
        assert(stringLiteral.value == "Hello, JSONLang!") { "字符串值错误" }
        
        // 测试二元表达式
        val intLiteral1 = LiteralExpression.IntLiteral(1, location)
        val intLiteral2 = LiteralExpression.IntLiteral(2, location)
        val binaryExpr = BinaryExpression(
            left = intLiteral1,
            operator = BinaryOperator.PLUS,
            right = intLiteral2,
            location = location
        )
        
        assert(binaryExpr.nodeType == "BinaryExpression") { "二元表达式节点类型错误" }
        assert(binaryExpr.operator == BinaryOperator.PLUS) { "操作符错误" }
        
        println("✓ AST 节点测试通过")
    }
    
    private fun testTypeSystem() {
        println("测试类型系统...")
        
        // 测试基础类型
        val stringType = PrimitiveType.StringType
        val intType = PrimitiveType.IntType
        val nullableStringType = PrimitiveType.NullableStringType
        
        assert(stringType.name == "String") { "字符串类型名称错误" }
        assert(intType.name == "Int") { "整数类型名称错误" }
        assert(!stringType.nullable) { "字符串类型不应该可空" }
        assert(nullableStringType.nullable) { "可空字符串类型应该可空" }
        
        // 测试类型兼容性
        assert(stringType.isAssignableFrom(stringType)) { "相同类型应该兼容" }
        assert(!stringType.isAssignableFrom(intType)) { "不同类型不应该兼容" }
        
        println("✓ 类型系统测试通过")
    }
    
    private fun testErrorReporting() {
        println("测试错误报告系统...")
        
        val reporter = DefaultErrorReporter()
        val location = SourceLocation.unknown()
        
        val error = ErrorBuilder(
            code = ErrorCodes.TYPE_MISMATCH,
            message = ErrorMessages.typeMismatch("String", "Int"),
            location = location
        )
            .severity(ErrorSeverity.ERROR)
            .category(ErrorCategory.TYPE)
            .suggestion("检查变量类型是否正确")
            .build()
        
        reporter.report(error)
        
        assert(reporter.hasErrors()) { "应该有错误" }
        assert(reporter.getErrorCount() == 1) { "错误数量应该为1" }
        assert(reporter.getErrors().first().code == ErrorCodes.TYPE_MISMATCH) { "错误代码不匹配" }
        
        println("✓ 错误报告系统测试通过")
    }
}

fun main() {
    CoreInfrastructureTest.runTests()
}
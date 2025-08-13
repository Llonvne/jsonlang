package cn.llonvne.jsonlang.core.eval

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.CompilerError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class DynamicEvaluatorTest {
    
    private lateinit var evaluator: DynamicEvaluator
    private lateinit var builder: ASTOperations.ASTBuilder
    private val errors = mutableListOf<CompilerError>()
    
    @BeforeEach
    fun setUp() {
        errors.clear()
        evaluator = DynamicEvaluator(object : ErrorReporter {
            override fun report(error: CompilerError) {
                errors.add(error)
            }
            
            override fun getErrors(): List<CompilerError> = errors
            override fun getErrorCount(): Int = errors.count { it.isFatal() }
            override fun getWarningCount(): Int = errors.count { it.isWarning() }
            override fun hasErrors(): Boolean = errors.any { it.isFatal() }
            override fun hasWarnings(): Boolean = errors.any { it.isWarning() }
            override fun clear() { errors.clear() }
        })
        builder = ASTOperations.builder(SourceLocation.unknown())
    }
    
    @Test
    fun testLiteralEvaluation() {
        // 测试字符串字面量
        val stringResult = evaluator.evaluate(builder.stringLiteral("hello"))
        assertTrue(stringResult is EvaluationResult.Success)
        assertEquals("hello", (stringResult as EvaluationResult.Success).value)
        
        // 测试整数字面量
        val intResult = evaluator.evaluate(builder.intLiteral(42))
        assertTrue(intResult is EvaluationResult.Success)
        assertEquals(42L, (intResult as EvaluationResult.Success).value)
        
        // 测试浮点数字面量
        val floatResult = evaluator.evaluate(builder.floatLiteral(3.14))
        assertTrue(floatResult is EvaluationResult.Success)
        assertEquals(3.14, (floatResult as EvaluationResult.Success).value)
        
        // 测试布尔字面量
        val boolResult = evaluator.evaluate(builder.booleanLiteral(true))
        assertTrue(boolResult is EvaluationResult.Success)
        assertEquals(true, (boolResult as EvaluationResult.Success).value)
        
        // 测试空值字面量
        val nullResult = evaluator.evaluate(builder.nullLiteral())
        assertTrue(nullResult is EvaluationResult.Success)
        assertNull((nullResult as EvaluationResult.Success).value)
    }
    
    @Test
    fun testBinaryExpressionEvaluation() {
        // 测试算术运算
        val addResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.intLiteral(10),
                BinaryOperator.PLUS,
                builder.intLiteral(5)
            )
        )
        assertTrue(addResult is EvaluationResult.Success)
        assertEquals(15L, (addResult as EvaluationResult.Success).value)
        
        val subtractResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.intLiteral(10),
                BinaryOperator.MINUS,
                builder.intLiteral(3)
            )
        )
        assertTrue(subtractResult is EvaluationResult.Success)
        assertEquals(7L, (subtractResult as EvaluationResult.Success).value)
        
        val multiplyResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.intLiteral(6),
                BinaryOperator.MULTIPLY,
                builder.intLiteral(7)
            )
        )
        assertTrue(multiplyResult is EvaluationResult.Success)
        assertEquals(42L, (multiplyResult as EvaluationResult.Success).value)
        
        val divideResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.floatLiteral(15.0),
                BinaryOperator.DIVIDE,
                builder.floatLiteral(3.0)
            )
        )
        assertTrue(divideResult is EvaluationResult.Success)
        assertEquals(5.0, (divideResult as EvaluationResult.Success).value)
        
        // 测试字符串连接
        val stringConcatResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.stringLiteral("Hello, "),
                BinaryOperator.PLUS,
                builder.stringLiteral("World!")
            )
        )
        assertTrue(stringConcatResult is EvaluationResult.Success)
        assertEquals("Hello, World!", (stringConcatResult as EvaluationResult.Success).value)
        
        // 测试比较运算
        val equalResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.intLiteral(5),
                BinaryOperator.EQUAL,
                builder.intLiteral(5)
            )
        )
        assertTrue(equalResult is EvaluationResult.Success)
        assertEquals(true, (equalResult as EvaluationResult.Success).value)
        
        val lessResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.intLiteral(3),
                BinaryOperator.LESS_THAN,
                builder.intLiteral(7)
            )
        )
        assertTrue(lessResult is EvaluationResult.Success)
        assertEquals(true, (lessResult as EvaluationResult.Success).value)
        
        // 测试逻辑运算
        val andResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.booleanLiteral(true),
                BinaryOperator.LOGICAL_AND,
                builder.booleanLiteral(false)
            )
        )
        assertTrue(andResult is EvaluationResult.Success)
        assertEquals(false, (andResult as EvaluationResult.Success).value)
        
        val orResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.booleanLiteral(true),
                BinaryOperator.LOGICAL_OR,
                builder.booleanLiteral(false)
            )
        )
        assertTrue(orResult is EvaluationResult.Success)
        assertEquals(true, (orResult as EvaluationResult.Success).value)
    }
    
    @Test
    fun testUnaryExpressionEvaluation() {
        // 测试一元加法
        val plusResult = evaluator.evaluate(
            builder.unaryExpression(
                UnaryOperator.PLUS,
                builder.intLiteral(42)
            )
        )
        assertTrue(plusResult is EvaluationResult.Success)
        assertEquals(42L, (plusResult as EvaluationResult.Success).value)
        
        // 测试一元减法
        val minusResult = evaluator.evaluate(
            builder.unaryExpression(
                UnaryOperator.MINUS,
                builder.intLiteral(42)
            )
        )
        assertTrue(minusResult is EvaluationResult.Success)
        assertEquals(-42L, (minusResult as EvaluationResult.Success).value)
        
        // 测试逻辑非
        val notResult = evaluator.evaluate(
            builder.unaryExpression(
                UnaryOperator.LOGICAL_NOT,
                builder.booleanLiteral(true)
            )
        )
        assertTrue(notResult is EvaluationResult.Success)
        assertEquals(false, (notResult as EvaluationResult.Success).value)
    }
    
    @Test
    fun testArrayExpressionEvaluation() {
        val arrayResult = evaluator.evaluate(
            builder.arrayExpression(
                listOf(
                    builder.intLiteral(1),
                    builder.intLiteral(2),
                    builder.intLiteral(3)
                )
            )
        )
        
        assertTrue(arrayResult is EvaluationResult.Success)
        val array = (arrayResult as EvaluationResult.Success).value as List<*>
        assertEquals(3, array.size)
        assertEquals(1L, array[0])
        assertEquals(2L, array[1])
        assertEquals(3L, array[2])
    }
    
    @Test
    fun testObjectExpressionEvaluation() {
        val objectResult = evaluator.evaluate(
            builder.objectExpression(
                listOf(
                    builder.objectProperty(
                        builder.stringLiteral("name"),
                        builder.stringLiteral("Alice")
                    ),
                    builder.objectProperty(
                        builder.stringLiteral("age"),
                        builder.intLiteral(25)
                    )
                )
            )
        )
        
        assertTrue(objectResult is EvaluationResult.Success)
        val obj = (objectResult as EvaluationResult.Success).value as Map<*, *>
        assertEquals(2, obj.size)
        assertEquals("Alice", obj["name"])
        assertEquals(25L, obj["age"])
    }
    
    @Test
    fun testMemberExpressionEvaluation() {
        val scope = EvaluationScope()
        
        // 设置一个对象变量
        scope.setValue("obj", mapOf("property" to "value", "number" to 42))
        scope.setValue("arr", listOf("a", "b", "c"))
        
        // 测试对象属性访问
        val memberResult = evaluator.evaluate(
            builder.memberExpression(
                builder.identifier("obj"),
                "property"
            ),
            scope
        )
        assertTrue(memberResult is EvaluationResult.Success)
        assertEquals("value", (memberResult as EvaluationResult.Success).value)
        
        // 测试数组索引访问
        val arrayAccessResult = evaluator.evaluate(
            builder.memberExpression(
                builder.identifier("arr"),
                "1",
                computed = true
            ),
            scope
        )
        assertTrue(arrayAccessResult is EvaluationResult.Success)
        assertEquals("b", (arrayAccessResult as EvaluationResult.Success).value)
        
        // 测试数组长度属性
        val lengthResult = evaluator.evaluate(
            builder.memberExpression(
                builder.identifier("arr"),
                "length"
            ),
            scope
        )
        assertTrue(lengthResult is EvaluationResult.Success)
        assertEquals(3, (lengthResult as EvaluationResult.Success).value)
    }
    
    @Test
    fun testConditionalExpressionEvaluation() {
        // 测试条件为真的情况
        val trueResult = evaluator.evaluate(
            builder.conditionalExpression(
                builder.booleanLiteral(true),
                builder.stringLiteral("then"),
                builder.stringLiteral("else")
            )
        )
        assertTrue(trueResult is EvaluationResult.Success)
        assertEquals("then", (trueResult as EvaluationResult.Success).value)
        
        // 测试条件为假的情况
        val falseResult = evaluator.evaluate(
            builder.conditionalExpression(
                builder.booleanLiteral(false),
                builder.stringLiteral("then"),
                builder.stringLiteral("else")
            )
        )
        assertTrue(falseResult is EvaluationResult.Success)
        assertEquals("else", (falseResult as EvaluationResult.Success).value)
        
        // 测试复杂条件
        val complexResult = evaluator.evaluate(
            builder.conditionalExpression(
                builder.binaryExpression(
                    builder.intLiteral(5),
                    BinaryOperator.GREATER_THAN,
                    builder.intLiteral(3)
                ),
                builder.stringLiteral("greater"),
                builder.stringLiteral("not greater")
            )
        )
        assertTrue(complexResult is EvaluationResult.Success)
        assertEquals("greater", (complexResult as EvaluationResult.Success).value)
    }
    
    @Test
    fun testVariableAccess() {
        val scope = EvaluationScope()
        scope.setValue("x", 42)
        scope.setValue("name", "Alice")
        
        // 测试已定义变量
        val xResult = evaluator.evaluate(builder.identifier("x"), scope)
        assertTrue(xResult is EvaluationResult.Success)
        assertEquals(42, (xResult as EvaluationResult.Success).value)
        
        val nameResult = evaluator.evaluate(builder.identifier("name"), scope)
        assertTrue(nameResult is EvaluationResult.Success)
        assertEquals("Alice", (nameResult as EvaluationResult.Success).value)
        
        // 测试未定义变量
        val undefinedResult = evaluator.evaluate(builder.identifier("undefined"), scope)
        assertTrue(undefinedResult is EvaluationResult.Error)
        assertTrue((undefinedResult as EvaluationResult.Error).message.contains("Undefined variable"))
    }
    
    @Test
    fun testComplexExpression() {
        val scope = EvaluationScope()
        scope.setValue("a", 10)
        scope.setValue("b", 5)
        scope.setValue("c", 2)
        
        // 测试复杂表达式: (a + b) * c
        val complexResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.binaryExpression(
                    builder.identifier("a"),
                    BinaryOperator.PLUS,
                    builder.identifier("b")
                ),
                BinaryOperator.MULTIPLY,
                builder.identifier("c")
            ),
            scope
        )
        
        assertTrue(complexResult is EvaluationResult.Success)
        assertEquals(30L, (complexResult as EvaluationResult.Success).value)
    }
    
    @Test
    fun testErrorHandling() {
        // 测试除零错误
        val divideByZeroResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.intLiteral(10),
                BinaryOperator.DIVIDE,
                builder.intLiteral(0)
            )
        )
        assertTrue(divideByZeroResult is EvaluationResult.Error)
        assertTrue((divideByZeroResult as EvaluationResult.Error).message.contains("Division by zero"))
        
        // 测试类型错误
        val typeErrorResult = evaluator.evaluate(
            builder.binaryExpression(
                builder.stringLiteral("hello"),
                BinaryOperator.MINUS,
                builder.intLiteral(5)
            )
        )
        assertTrue(typeErrorResult is EvaluationResult.Error)
        assertTrue((typeErrorResult as EvaluationResult.Error).message.contains("Cannot subtract"))
    }
    
    @Test
    fun testEvalUtility() {
        // 测试 Eval.eval 函数
        val result1 = Eval.eval(builder.intLiteral(42))
        assertEquals(42L, result1)
        
        val result2 = Eval.eval(
            builder.binaryExpression(
                builder.intLiteral(10),
                BinaryOperator.PLUS,
                builder.intLiteral(5)
            )
        )
        assertEquals(15L, result2)
        
        // 测试错误情况
        assertThrows(EvaluationException::class.java) {
            Eval.eval(builder.identifier("undefined"))
        }
        
        // 测试带作用域的执行
        val scope = Eval.createScope()
        Eval.registerNativeFunction("add", { args ->
            val a = args.getOrNull(0) as? Number ?: 0
            val b = args.getOrNull(1) as? Number ?: 0
            a.toLong() + b.toLong()
        }, scope)
        
        scope.setValue("x", 10)
        val result3 = Eval.eval(builder.identifier("x"), scope)
        assertEquals(10, result3)
    }
    
    @Test
    fun testScopeHierarchy() {
        val parentScope = EvaluationScope()
        parentScope.setValue("x", 10)
        parentScope.setValue("y", 20)
        
        val childScope = parentScope.createChild()
        childScope.setValue("y", 30) // 覆盖父作用域的值
        childScope.setValue("z", 40)
        
        // 测试子作用域可以访问父作用域的变量
        assertEquals(10, childScope.getValue("x"))
        
        // 测试子作用域覆盖父作用域的变量
        assertEquals(30, childScope.getValue("y"))
        assertEquals(20, parentScope.getValue("y")) // 父作用域不受影响
        
        // 测试子作用域的新变量
        assertEquals(40, childScope.getValue("z"))
        assertNull(parentScope.getValue("z")) // 父作用域访问不到子作用域的变量
        
        // 测试 hasValue 方法
        assertTrue(childScope.hasValue("x"))
        assertTrue(childScope.hasValue("y"))
        assertTrue(childScope.hasValue("z"))
        assertFalse(childScope.hasValue("nonexistent"))
        
        assertTrue(parentScope.hasValue("x"))
        assertTrue(parentScope.hasValue("y"))
        assertFalse(parentScope.hasValue("z"))
    }
}
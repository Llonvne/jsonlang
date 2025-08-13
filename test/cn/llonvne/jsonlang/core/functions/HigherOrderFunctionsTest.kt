package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlin.test.*

class HigherOrderFunctionsTest {
    
    private lateinit var higherOrderSupport: HigherOrderFunctionSupport
    private lateinit var closureSupport: ClosureSupport
    private lateinit var lambdaAnalyzer: LambdaExpressionAnalyzer
    private val testLocation = SourceLocation("test.jsonlang", 1, 1, 1, 10, 0, 9)
    
    @BeforeTest
    fun setup() {
        higherOrderSupport = HigherOrderFunctionSupport()
        closureSupport = ClosureSupport()
        lambdaAnalyzer = LambdaExpressionAnalyzer()
    }
    
    @Test
    fun testValidateLambdaExpression() {
        // 创建 lambda: (x: Int) -> x + 1
        val lambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = BinaryExpression(
                left = IdentifierExpression("x", testLocation, inferredType = PrimitiveType.IntType),
                operator = BinaryOperator.PLUS,
                right = LiteralExpression.IntLiteral(1, testLocation, inferredType = PrimitiveType.IntType),
                location = testLocation,
                inferredType = PrimitiveType.IntType
            ),
            location = testLocation,
            inferredType = CompositeType.FunctionType(
                listOf(PrimitiveType.IntType), 
                PrimitiveType.IntType
            )
        )
        
        val expectedType = CompositeType.FunctionType(
            listOf(PrimitiveType.IntType),
            PrimitiveType.IntType
        )
        
        val result = lambdaAnalyzer.validateLambda(lambda, expectedType)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun testValidateLambdaWithWrongParameterCount() {
        val lambda = LambdaExpression(
            parameters = listOf(
                Parameter("x", PrimitiveType.IntType),
                Parameter("y", PrimitiveType.IntType)
            ),
            body = LiteralExpression.IntLiteral(0, testLocation),
            location = testLocation
        )
        
        val expectedType = CompositeType.FunctionType(
            listOf(PrimitiveType.IntType), // 期望1个参数，但lambda有2个
            PrimitiveType.IntType
        )
        
        val result = lambdaAnalyzer.validateLambda(lambda, expectedType)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("参数数量不匹配") })
    }
    
    @Test
    fun testValidateLambdaWithDuplicateParameterNames() {
        val lambda = LambdaExpression(
            parameters = listOf(
                Parameter("x", PrimitiveType.IntType),
                Parameter("x", PrimitiveType.StringType) // 重复参数名
            ),
            body = LiteralExpression.IntLiteral(0, testLocation),
            location = testLocation
        )
        
        val result = lambdaAnalyzer.validateLambda(lambda, null)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("参数名重复") })
    }
    
    @Test
    fun testInferLambdaType() {
        val lambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = LiteralExpression.StringLiteral("result", testLocation),
            location = testLocation
        )
        
        val context = object : TypeInferenceContext {
            override fun getVariableType(name: String): Type? = null
            override fun getFunctionType(name: String): CompositeType.FunctionType? = null
        }
        
        val inferredType = lambdaAnalyzer.inferLambdaType(lambda, context)
        
        assertNotNull(inferredType)
        assertEquals(1, inferredType.parameterTypes.size)
        assertEquals(PrimitiveType.IntType, inferredType.parameterTypes[0])
        assertEquals(PrimitiveType.StringType, inferredType.returnType)
    }
    
    @Test
    fun testValidateHigherOrderFunctionCall() {
        // 创建高阶函数签名: map(list: Array<Int>, fn: (Int) -> String) -> Array<String>
        val mapFunction = FunctionSignature(
            name = "map",
            modifiers = FunctionModifiers(),
            generics = emptyList(),
            parameters = listOf(
                FunctionParameter(
                    name = "list",
                    type = CompositeType.ArrayType(PrimitiveType.IntType),
                    optional = false,
                    defaultValue = null,
                    annotations = emptyList()
                ),
                FunctionParameter(
                    name = "fn",
                    type = CompositeType.FunctionType(
                        listOf(PrimitiveType.IntType),
                        PrimitiveType.StringType
                    ),
                    optional = false,
                    defaultValue = null,
                    annotations = emptyList()
                )
            ),
            returnType = CompositeType.ArrayType(PrimitiveType.StringType),
            visibility = Visibility.PUBLIC,
            annotations = emptyList(),
            location = testLocation
        )
        
        // 创建调用: map([1, 2, 3], (x) -> x.toString())
        val lambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = LiteralExpression.StringLiteral("converted", testLocation, inferredType = PrimitiveType.StringType),
            location = testLocation,
            inferredType = PrimitiveType.StringType  // Lambda's inferredType should be its return type
        )
        
        val call = CallExpression(
            function = IdentifierExpression("map", testLocation),
            arguments = listOf(
                Argument(
                    name = "list",
                    value = ArrayExpression(
                        elements = listOf(
                            LiteralExpression.IntLiteral(1, testLocation),
                            LiteralExpression.IntLiteral(2, testLocation),
                            LiteralExpression.IntLiteral(3, testLocation)
                        ),
                        location = testLocation,
                        inferredType = CompositeType.ArrayType(PrimitiveType.IntType)
                    )
                ),
                Argument(
                    name = "fn",
                    value = lambda
                )
            ),
            location = testLocation
        )
        
        val result = higherOrderSupport.validateHigherOrderCall(call, mapFunction)
        
        if (!result.isValid) {
            println("Validation errors: ${result.errors}")
        }
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun testClosureAnalysis() {
        // 创建闭包: (x) -> x + capturedVar
        val capturedVar = "capturedVar"
        val lambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = BinaryExpression(
                left = IdentifierExpression("x", testLocation),
                operator = BinaryOperator.PLUS,
                right = IdentifierExpression(capturedVar, testLocation),
                location = testLocation
            ),
            location = testLocation
        )
        
        val context = object : ClosureContext {
            override fun isVariableInScope(name: String): Boolean = name == capturedVar
            override fun isVariableMutable(name: String): Boolean = false
            override fun getVariableType(name: String): Type = PrimitiveType.IntType
        }
        
        val result = closureSupport.analyzeClosure(lambda, context)
        
        assertTrue(result.isValid)
        assertTrue(result.capturedVariables.contains(capturedVar))
        assertTrue(result.requiresHeapAllocation)
    }
    
    @Test
    fun testClosureWithUndefinedVariable() {
        val lambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = IdentifierExpression("undefinedVar", testLocation),
            location = testLocation
        )
        
        val context = object : ClosureContext {
            override fun isVariableInScope(name: String): Boolean = false
            override fun isVariableMutable(name: String): Boolean = false
            override fun getVariableType(name: String): Type = PrimitiveType.IntType
        }
        
        val result = closureSupport.analyzeClosure(lambda, context)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("未定义的变量") })
    }
    
    @Test
    fun testClosureWithMutableCapture() {
        val lambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = IdentifierExpression("mutableVar", testLocation),
            location = testLocation
        )
        
        val context = object : ClosureContext {
            override fun isVariableInScope(name: String): Boolean = name == "mutableVar"
            override fun isVariableMutable(name: String): Boolean = true
            override fun getVariableType(name: String): Type = PrimitiveType.IntType
        }
        
        val result = closureSupport.analyzeClosure(lambda, context)
        
        assertTrue(result.isValid) // 仍然有效，但有警告
        assertTrue(result.errors.any { it.contains("警告") && it.contains("可变变量") })
    }
    
    @Test
    fun testCreateClosureEnvironment() {
        val capturedVariables = setOf("var1", "var2")
        val context = object : ClosureContext {
            override fun isVariableInScope(name: String): Boolean = capturedVariables.contains(name)
            override fun isVariableMutable(name: String): Boolean = name == "var2"
            override fun getVariableType(name: String): Type = when (name) {
                "var1" -> PrimitiveType.IntType
                "var2" -> PrimitiveType.StringType
                else -> PrimitiveType.AnyType
            }
        }
        
        val environment = closureSupport.createClosureEnvironment(capturedVariables, context)
        
        assertEquals(2, environment.getAllBindings().size)
        
        val var1Binding = environment.getBinding("var1")
        assertNotNull(var1Binding)
        assertEquals(PrimitiveType.IntType, var1Binding.type)
        assertFalse(var1Binding.mutable)
        assertEquals(CaptureMode.BY_VALUE, var1Binding.captureMode)
        
        val var2Binding = environment.getBinding("var2")
        assertNotNull(var2Binding)
        assertEquals(PrimitiveType.StringType, var2Binding.type)
        assertTrue(var2Binding.mutable)
        assertEquals(CaptureMode.BY_REFERENCE, var2Binding.captureMode)
    }
    
    @Test
    fun testValidateClosureAccess() {
        val bindings = mapOf(
            "immutableVar" to ClosureBinding("immutableVar", PrimitiveType.IntType, false, CaptureMode.BY_VALUE),
            "mutableVar" to ClosureBinding("mutableVar", PrimitiveType.StringType, true, CaptureMode.BY_REFERENCE)
        )
        val environment = ClosureEnvironment(bindings)
        
        // 读取不可变变量 - 应该成功
        val readImmutable = closureSupport.validateClosureAccess("immutableVar", AccessType.READ, environment)
        assertTrue(readImmutable.isValid)
        
        // 写入不可变变量 - 应该失败
        val writeImmutable = closureSupport.validateClosureAccess("immutableVar", AccessType.WRITE, environment)
        assertFalse(writeImmutable.isValid)
        assertTrue(writeImmutable.errors.any { it.contains("无法修改不可变") })
        
        // 写入可变变量 - 应该成功
        val writeMutable = closureSupport.validateClosureAccess("mutableVar", AccessType.WRITE, environment)
        assertTrue(writeMutable.isValid)
        
        // 访问未捕获的变量 - 应该失败
        val accessUncaptured = closureSupport.validateClosureAccess("uncapturedVar", AccessType.READ, environment)
        assertFalse(accessUncaptured.isValid)
        assertTrue(accessUncaptured.errors.any { it.contains("未在闭包中捕获") })
    }
    
    @Test
    fun testVariableAnalyzer() {
        // 分析表达式: x + y + localVar (其中 localVar 是局部变量)
        val expression = BinaryExpression(
            left = BinaryExpression(
                left = IdentifierExpression("x", testLocation),
                operator = BinaryOperator.PLUS,
                right = IdentifierExpression("y", testLocation),
                location = testLocation
            ),
            operator = BinaryOperator.PLUS,
            right = IdentifierExpression("localVar", testLocation),
            location = testLocation
        )
        
        val analyzer = VariableAnalyzer(setOf("localVar"))
        val referencedVariables = analyzer.analyze(expression)
        
        assertEquals(2, referencedVariables.size)
        assertTrue(referencedVariables.contains("x"))
        assertTrue(referencedVariables.contains("y"))
        assertFalse(referencedVariables.contains("localVar")) // 局部变量不应该被包含
    }
    
    @Test
    fun testNestedLambdaAnalysis() {
        // 创建嵌套 lambda: (x) -> (y) -> x + y + outerVar
        val innerLambda = LambdaExpression(
            parameters = listOf(Parameter("y", PrimitiveType.IntType)),
            body = BinaryExpression(
                left = BinaryExpression(
                    left = IdentifierExpression("x", testLocation),
                    operator = BinaryOperator.PLUS,
                    right = IdentifierExpression("y", testLocation),
                    location = testLocation
                ),
                operator = BinaryOperator.PLUS,
                right = IdentifierExpression("outerVar", testLocation),
                location = testLocation
            ),
            location = testLocation
        )
        
        val outerLambda = LambdaExpression(
            parameters = listOf(Parameter("x", PrimitiveType.IntType)),
            body = innerLambda,
            location = testLocation
        )
        
        val analyzer = VariableAnalyzer(emptySet())
        val referencedVariables = analyzer.analyze(outerLambda)
        
        assertEquals(1, referencedVariables.size)
        assertTrue(referencedVariables.contains("outerVar"))
        // x 和 y 都是参数，不应该被包含在外部引用中
    }
}
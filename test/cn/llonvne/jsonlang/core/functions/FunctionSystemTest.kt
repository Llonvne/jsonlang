package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import kotlin.test.Test
import kotlin.test.BeforeTest


class FunctionSystemTest {
    
    private lateinit var functionSystem: FunctionSystem
    private val testLocation = SourceLocation("test.jsonlang", 1, 1, 1, 10, 0, 9)
    
    @BeforeTest
    fun setup() {
        functionSystem = FunctionSystem()
    }
    
    @Test
    fun testRegisterSimpleFunction() {
        val function = createSimpleFunction("add", 
            listOf("a" to PrimitiveType.IntType, "b" to PrimitiveType.IntType),
            PrimitiveType.IntType
        )
        
        val result = functionSystem.registerFunction(function)
        
        assert(result is FunctionRegistrationResult.Success)
        assert(functionSystem.hasFunction("add"))
        assert(functionSystem.getFunctionSignatures("add").size == 1)
    }
    
    @Test
    fun testRegisterFunctionWithDefaultParameters() {
        val defaultValue = LiteralExpression.IntLiteral(0, testLocation)
        val parameters = listOf(
            Parameter("x", PrimitiveType.IntType, false, null),
            Parameter("y", PrimitiveType.IntType, true, defaultValue)
        )
        
        val function = FunctionDeclaration(
            name = "increment",
            modifiers = FunctionModifiers(),
            parameters = parameters,
            returnType = PrimitiveType.IntType,
            body = emptyList(),
            location = testLocation
        )
        
        val result = functionSystem.registerFunction(function)
        
        assert(result is FunctionRegistrationResult.Success)
        val signature = (result as FunctionRegistrationResult.Success).signature
        assert(signature.parameters[1].hasDefaultValue)
    }
    
    @Test
    fun testRegisterFunctionWithDuplicateParameterNames() {
        val parameters = listOf(
            Parameter("x", PrimitiveType.IntType),
            Parameter("x", PrimitiveType.StringType)  // 重复参数名
        )
        
        val function = FunctionDeclaration(
            name = "invalid",
            modifiers = FunctionModifiers(),
            parameters = parameters,
            returnType = PrimitiveType.UnitType,
            body = emptyList(),
            location = testLocation
        )
        
        val result = functionSystem.registerFunction(function)
        
        assert(result is FunctionRegistrationResult.Error)
        val errors = (result as FunctionRegistrationResult.Error).errors
        assert(errors.any { it.contains("参数名重复") })
    }
    
    @Test
    fun testFunctionOverloading() {
        // 注册第一个重载
        val function1 = createSimpleFunction("process",
            listOf("data" to PrimitiveType.StringType),
            PrimitiveType.StringType
        )
        
        // 注册第二个重载
        val function2 = createSimpleFunction("process",
            listOf("data" to PrimitiveType.IntType),
            PrimitiveType.IntType
        )
        
        val result1 = functionSystem.registerFunction(function1)
        val result2 = functionSystem.registerFunction(function2)
        
        assert(result1 is FunctionRegistrationResult.Success)
        assert(result2 is FunctionRegistrationResult.Success)
        assert(functionSystem.getFunctionSignatures("process").size == 2)
    }
    
    @Test
    fun testFunctionOverloadConflict() {
        val function1 = createSimpleFunction("duplicate",
            listOf("x" to PrimitiveType.IntType),
            PrimitiveType.IntType
        )
        
        val function2 = createSimpleFunction("duplicate",
            listOf("y" to PrimitiveType.IntType),  // 相同类型，不同参数名
            PrimitiveType.StringType  // 不同返回类型
        )
        
        val result1 = functionSystem.registerFunction(function1)
        val result2 = functionSystem.registerFunction(function2)
        
        assert(result1 is FunctionRegistrationResult.Success)
        assert(result2 is FunctionRegistrationResult.Error)
    }
    
    @Test
    fun testResolveSimpleFunctionCall() {
        // 注册函数
        val function = createSimpleFunction("greet",
            listOf("name" to PrimitiveType.StringType),
            PrimitiveType.StringType
        )
        functionSystem.registerFunction(function)
        
        // 创建函数调用
        val call = CallExpression(
            function = IdentifierExpression("greet", testLocation),
            arguments = listOf(
                Argument(value = LiteralExpression.StringLiteral("World", testLocation))
            ),
            location = testLocation
        )
        
        val result = functionSystem.resolveCall(call)
        
        assert(result is FunctionCallResult.Success)
        val success = result as FunctionCallResult.Success
        assert(success.function.name == "greet")
        assert(success.argumentMapping.size == 1)
    }
    
    @Test
    fun testResolveOverloadedFunctionCall() {
        // 注册重载函数
        val stringFunction = createSimpleFunction("format",
            listOf("value" to PrimitiveType.StringType),
            PrimitiveType.StringType
        )
        val intFunction = createSimpleFunction("format",
            listOf("value" to PrimitiveType.IntType),
            PrimitiveType.StringType
        )
        
        functionSystem.registerFunction(stringFunction)
        functionSystem.registerFunction(intFunction)
        
        // 调用整数版本
        val intCall = CallExpression(
            function = IdentifierExpression("format", testLocation),
            arguments = listOf(
                Argument(value = LiteralExpression.IntLiteral(42, testLocation, inferredType = PrimitiveType.IntType))
            ),
            location = testLocation
        )
        
        val result = functionSystem.resolveCall(intCall)
        
        assert(result is FunctionCallResult.Success)
        val success = result as FunctionCallResult.Success
        assert(success.function.parameters[0].type == PrimitiveType.IntType)
    }
    
    @Test
    fun testResolveCallWithNamedArguments() {
        val function = createSimpleFunction("divide",
            listOf("dividend" to PrimitiveType.IntType, "divisor" to PrimitiveType.IntType),
            PrimitiveType.IntType
        )
        functionSystem.registerFunction(function)
        
        // 使用命名参数调用
        val call = CallExpression(
            function = IdentifierExpression("divide", testLocation),
            arguments = listOf(
                Argument("divisor", LiteralExpression.IntLiteral(2, testLocation)),
                Argument("dividend", LiteralExpression.IntLiteral(10, testLocation))
            ),
            location = testLocation
        )
        
        val result = functionSystem.resolveCall(call)
        
        assert(result is FunctionCallResult.Success)
        val success = result as FunctionCallResult.Success
        assert(success.argumentMapping.size == 2)
        assert(success.argumentMapping.containsKey("dividend"))
        assert(success.argumentMapping.containsKey("divisor"))
    }
    
    @Test
    fun testResolveCallWithDefaultParameters() {
        val defaultValue = LiteralExpression.IntLiteral(1, testLocation)
        val parameters = listOf(
            Parameter("base", PrimitiveType.IntType),
            Parameter("exponent", PrimitiveType.IntType, true, defaultValue)
        )
        
        val function = FunctionDeclaration(
            name = "power",
            modifiers = FunctionModifiers(),
            parameters = parameters,
            returnType = PrimitiveType.IntType,
            body = emptyList(),
            location = testLocation
        )
        
        functionSystem.registerFunction(function)
        
        // 只提供第一个参数
        val call = CallExpression(
            function = IdentifierExpression("power", testLocation),
            arguments = listOf(
                Argument(value = LiteralExpression.IntLiteral(5, testLocation))
            ),
            location = testLocation
        )
        
        val result = functionSystem.resolveCall(call)
        
        assert(result is FunctionCallResult.Success)
        val success = result as FunctionCallResult.Success
        assert(success.argumentMapping.size == 2)  // 包含默认参数
    }
    
    @Test
    fun testResolveCallWithInvalidFunction() {
        val call = CallExpression(
            function = IdentifierExpression("nonexistent", testLocation),
            arguments = emptyList(),
            location = testLocation
        )
        
        val result = functionSystem.resolveCall(call)
        
        assert(result is FunctionCallResult.Error)
        val errors = (result as FunctionCallResult.Error).errors
        assert(errors.any { it.contains("未找到函数") })
    }
    
    @Test
    fun testResolveCallWithWrongArgumentCount() {
        val function = createSimpleFunction("test",
            listOf("a" to PrimitiveType.IntType, "b" to PrimitiveType.IntType),
            PrimitiveType.IntType
        )
        functionSystem.registerFunction(function)
        
        // 提供错误数量的参数
        val call = CallExpression(
            function = IdentifierExpression("test", testLocation),
            arguments = listOf(
                Argument(value = LiteralExpression.IntLiteral(1, testLocation))
            ),
            location = testLocation
        )
        
        val result = functionSystem.resolveCall(call)
        
        assert(result is FunctionCallResult.Error)
    }
    
    private fun createSimpleFunction(
        name: String,
        params: List<Pair<String, Type>>,
        returnType: Type
    ): FunctionDeclaration {
        val parameters = params.map { (paramName, paramType) ->
            Parameter(paramName, paramType)
        }
        
        return FunctionDeclaration(
            name = name,
            modifiers = FunctionModifiers(),
            parameters = parameters,
            returnType = returnType,
            body = emptyList(),
            location = testLocation
        )
    }
}
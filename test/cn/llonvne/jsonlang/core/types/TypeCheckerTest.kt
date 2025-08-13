package cn.llonvne.jsonlang.core.types

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.errors.DefaultErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlin.test.*

class TypeCheckerTest {
    
    private lateinit var typeChecker: TypeChecker
    private lateinit var errorReporter: DefaultErrorReporter
    
    @BeforeTest
    fun setup() {
        errorReporter = DefaultErrorReporter()
        typeChecker = BasicTypeChecker(errorReporter)
    }
    
    private fun testLocation(line: Int = 1, column: Int = 1): SourceLocation {
        return SourceLocation("test", line, column, line, column, 0, 0)
    }
    
    @Test
    fun testBasicTypeInference() {
        // 测试字面量类型推导
        val stringLiteral = LiteralExpression.StringLiteral(
            value = "hello",
            location = testLocation(),
            annotations = emptyList()
        )
        
        val result = typeChecker.inferType(stringLiteral)
        assertEquals(PrimitiveType.StringType, result)
        
        val intLiteral = LiteralExpression.IntLiteral(
            value = 42,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val intResult = typeChecker.inferType(intLiteral)
        assertEquals(PrimitiveType.LongType, intResult)
        
        val boolLiteral = LiteralExpression.BooleanLiteral(
            value = true,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val boolResult = typeChecker.inferType(boolLiteral)
        assertEquals(PrimitiveType.BooleanType, boolResult)
    }
    
    @Test
    fun testBinaryExpressionTypeInference() {
        val left = LiteralExpression.IntLiteral(
            value = 10,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val right = LiteralExpression.IntLiteral(
            value = 20,
            location = testLocation(1, 5),
            annotations = emptyList()
        )
        
        val addExpression = BinaryExpression(
            left = left,
            operator = BinaryOperator.PLUS,
            right = right,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val result = typeChecker.inferType(addExpression)
        assertEquals(PrimitiveType.LongType, result)
        
        // 测试比较表达式
        val compareExpression = BinaryExpression(
            left = left,
            operator = BinaryOperator.LESS_THAN,
            right = right,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val compareResult = typeChecker.inferType(compareExpression)
        assertEquals(PrimitiveType.BooleanType, compareResult)
    }
    
    @Test
    fun testArrayTypeInference() {
        val element1 = LiteralExpression.IntLiteral(
            value = 1,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val element2 = LiteralExpression.IntLiteral(
            value = 2,
            location = testLocation(1, 4),
            annotations = emptyList()
        )
        
        val arrayExpression = ArrayExpression(
            elements = listOf(element1, element2),
            location = testLocation(),
            annotations = emptyList()
        )
        
        val result = typeChecker.inferType(arrayExpression)
        assertTrue(result is CompositeType.ArrayType)
        assertEquals(PrimitiveType.LongType, (result as CompositeType.ArrayType).elementType)
    }
    
    @Test
    fun testTypeCompatibilityCheck() {
        val location = testLocation()
        
        // 兼容的类型
        val compatibleResult = typeChecker.checkTypeCompatibility(
            expected = PrimitiveType.AnyType,
            actual = PrimitiveType.StringType,
            location = location
        )
        assertTrue(compatibleResult is TypeCheckResult.Success)
        
        // 不兼容的类型
        val incompatibleResult = typeChecker.checkTypeCompatibility(
            expected = PrimitiveType.IntType,
            actual = PrimitiveType.StringType,
            location = location
        )
        assertTrue(incompatibleResult is TypeCheckResult.Error)
    }
    
    @Test
    fun testTypeEquivalence() {
        // 相同类型
        assertTrue(typeChecker.areTypesEquivalent(PrimitiveType.StringType, PrimitiveType.StringType))
        
        // 不同类型
        assertFalse(typeChecker.areTypesEquivalent(PrimitiveType.StringType, PrimitiveType.IntType))
        
        // 可空和非空类型
        assertFalse(typeChecker.areTypesEquivalent(PrimitiveType.StringType, PrimitiveType.NullableStringType))
    }
    
    @Test
    fun testSubtypeRelationship() {
        // Nothing 是所有类型的子类型
        assertTrue(typeChecker.isSubtype(PrimitiveType.NothingType, PrimitiveType.StringType))
        assertTrue(typeChecker.isSubtype(PrimitiveType.NothingType, PrimitiveType.IntType))
        
        // 所有类型都是 Any 的子类型
        assertTrue(typeChecker.isSubtype(PrimitiveType.StringType, PrimitiveType.AnyType))
        assertTrue(typeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.AnyType))
        
        // Null 只能赋值给可空类型
        assertTrue(typeChecker.isSubtype(PrimitiveType.NullType, PrimitiveType.NullableStringType))
        assertFalse(typeChecker.isSubtype(PrimitiveType.NullType, PrimitiveType.StringType))
        
        // 数值类型的子类型关系
        assertTrue(typeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.LongType))
        assertTrue(typeChecker.isSubtype(PrimitiveType.LongType, PrimitiveType.FloatType))
        assertTrue(typeChecker.isSubtype(PrimitiveType.FloatType, PrimitiveType.DoubleType))
    }
    
    @Test
    fun testCommonSupertype() {
        // 相同类型的公共超类型是自己
        val sameSupertype = typeChecker.getCommonSupertype(PrimitiveType.StringType, PrimitiveType.StringType)
        assertEquals(PrimitiveType.StringType, sameSupertype)
        
        // 数值类型的公共超类型
        val numericSupertype = typeChecker.getCommonSupertype(PrimitiveType.LongType, PrimitiveType.DoubleType)
        assertEquals(PrimitiveType.DoubleType, numericSupertype)
        
        // 不相关类型的公共超类型是 Any
        val anySupertype = typeChecker.getCommonSupertype(PrimitiveType.StringType, PrimitiveType.BooleanType)
        assertEquals(PrimitiveType.AnyType, anySupertype)
    }
    
    @Test
    fun testUserDefinedTypeRegistration() {
        val structType = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf(
                "name" to FieldType(PrimitiveType.StringType),
                "age" to FieldType(PrimitiveType.IntType)
            ),
            location = testLocation()
        )
        
        typeChecker.registerType(structType)
        
        val resolvedType = typeChecker.resolveType("Person")
        assertEquals(structType, resolvedType)
        
        val allTypes = typeChecker.getAllTypes()
        assertTrue(allTypes.containsKey("Person"))
        assertEquals(structType, allTypes["Person"])
    }
    
    @Test
    fun testComplexTypeInference() {
        // 测试混合类型数组的类型推导
        val intElement = LiteralExpression.IntLiteral(
            value = 1,
            location = testLocation(),
            annotations = emptyList()
        )
        
        val floatElement = LiteralExpression.FloatLiteral(
            value = 2.5,
            location = testLocation(1, 4),
            annotations = emptyList()
        )
        
        val mixedArray = ArrayExpression(
            elements = listOf(intElement, floatElement),
            location = testLocation(),
            annotations = emptyList()
        )
        
        val result = typeChecker.inferType(mixedArray)
        assertTrue(result is CompositeType.ArrayType)
        assertEquals(PrimitiveType.DoubleType, (result as CompositeType.ArrayType).elementType)
    }
    
    // New tests for complex types and generics
    
    @Test
    fun testObjectTypeInference() {
        val objectExpr = ObjectExpression(
            properties = listOf(
                ObjectProperty(
                    key = LiteralExpression.StringLiteral("name", testLocation()),
                    value = LiteralExpression.StringLiteral("John", testLocation())
                ),
                ObjectProperty(
                    key = LiteralExpression.StringLiteral("age", testLocation()),
                    value = LiteralExpression.IntLiteral(30, testLocation())
                )
            ),
            location = testLocation()
        )
        
        val result = typeChecker.inferType(objectExpr)
        assertTrue(result is CompositeType.ObjectType)
        val objType = result as CompositeType.ObjectType
        assertEquals(PrimitiveType.StringType, objType.keyType)
        assertEquals(PrimitiveType.AnyType, objType.valueType)
    }
    
    @Test
    fun testLambdaTypeInference() {
        val lambdaExpr = LambdaExpression(
            parameters = listOf(
                Parameter("x", PrimitiveType.IntType),
                Parameter("y", PrimitiveType.IntType)
            ),
            body = BinaryExpression(
                left = IdentifierExpression("x", testLocation()),
                operator = BinaryOperator.PLUS,
                right = IdentifierExpression("y", testLocation()),
                location = testLocation()
            ),
            location = testLocation()
        )
        
        val result = typeChecker.inferType(lambdaExpr)
        assertTrue(result is CompositeType.FunctionType)
        val funcType = result as CompositeType.FunctionType
        assertEquals(2, funcType.parameterTypes.size)
        assertEquals(PrimitiveType.IntType, funcType.parameterTypes[0])
        assertEquals(PrimitiveType.IntType, funcType.parameterTypes[1])
        assertEquals(PrimitiveType.IntType, funcType.returnType)
    }
    
    @Test
    fun testGenericTypeInference() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        val genericParameters = listOf(
            GenericParameter("T", listOf(PrimitiveType.AnyType))
        )
        val argumentTypes = listOf(PrimitiveType.StringType)
        val parameterTypes = listOf(GenericType("T"))
        
        val substitutions = basicTypeChecker.inferGenericTypes(genericParameters, argumentTypes, parameterTypes)
        
        assertEquals(1, substitutions.size)
        assertEquals(PrimitiveType.StringType, substitutions["T"])
    }
    
    @Test
    fun testGenericConstraintValidation() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        val genericParameters = listOf(
            GenericParameter("T", listOf(PrimitiveType.StringType))
        )
        val typeArguments = listOf(PrimitiveType.IntType)
        
        val errors = basicTypeChecker.checkGenericConstraints(genericParameters, typeArguments)
        
        assertEquals(1, errors.size)
        assertEquals("GENERIC_CONSTRAINT_VIOLATION", errors[0].errorCode)
    }
    
    @Test
    fun testTypeSubstitution() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        val genericType = CompositeType.ArrayType(GenericType("T"))
        val substitutions = mapOf("T" to PrimitiveType.StringType)
        
        val result = basicTypeChecker.applyTypeSubstitutions(genericType, substitutions)
        
        assertTrue(result is CompositeType.ArrayType)
        assertEquals(PrimitiveType.StringType, (result as CompositeType.ArrayType).elementType)
    }
    
    @Test
    fun testOptionalTypeInference() {
        val optionalType = CompositeType.OptionalType(PrimitiveType.StringType)
        
        assertTrue(optionalType.isAssignableFrom(PrimitiveType.StringType))
        assertTrue(optionalType.isAssignableFrom(PrimitiveType.NullType))
        assertFalse(optionalType.isAssignableFrom(PrimitiveType.IntType))
    }
    
    @Test
    fun testFunctionTypeCompatibility() {
        val func1 = CompositeType.FunctionType(
            listOf(PrimitiveType.StringType),
            PrimitiveType.StringType
        )
        val func2 = CompositeType.FunctionType(
            listOf(PrimitiveType.StringType),
            PrimitiveType.StringType
        )
        
        // Basic test: identical function types should be compatible
        assertTrue(typeChecker.isSubtype(func1, func2))
    }
    
    @Test
    fun testCastExpressionTypeInference() {
        val castExpr = CastExpression(
            expression = LiteralExpression.StringLiteral("hello", testLocation()),
            targetType = PrimitiveType.AnyType,
            safe = false,
            location = testLocation()
        )
        
        val result = typeChecker.inferType(castExpr)
        assertEquals(PrimitiveType.AnyType, result)
    }
    
    @Test
    fun testSafeCastExpressionTypeInference() {
        val safeCastExpr = CastExpression(
            expression = LiteralExpression.StringLiteral("hello", testLocation()),
            targetType = PrimitiveType.IntType,
            safe = true,
            location = testLocation()
        )
        
        val result = typeChecker.inferType(safeCastExpr)
        assertTrue(result is CompositeType.OptionalType)
        assertEquals(PrimitiveType.IntType, (result as CompositeType.OptionalType).innerType)
    }
    
    @Test
    fun testTypeCheckExpressionInference() {
        val typeCheckExpr = TypeCheckExpression(
            expression = LiteralExpression.StringLiteral("hello", testLocation()),
            type = PrimitiveType.StringType,
            location = testLocation()
        )
        
        val result = typeChecker.inferType(typeCheckExpr)
        assertEquals(PrimitiveType.BooleanType, result)
    }
    
    @Test
    fun testComplexGenericTypeSubstitution() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        
        // Test nested generic types
        val nestedGenericType = CompositeType.ObjectType(
            GenericType("K"),
            CompositeType.ArrayType(GenericType("V"))
        )
        
        val substitutions = mapOf(
            "K" to PrimitiveType.StringType,
            "V" to PrimitiveType.IntType
        )
        
        val result = basicTypeChecker.applyTypeSubstitutions(nestedGenericType, substitutions)
        
        assertTrue(result is CompositeType.ObjectType)
        val objType = result as CompositeType.ObjectType
        assertEquals(PrimitiveType.StringType, objType.keyType)
        assertTrue(objType.valueType is CompositeType.ArrayType)
        assertEquals(PrimitiveType.IntType, (objType.valueType as CompositeType.ArrayType).elementType)
    }
    
    @Test
    fun testGenericTypeInstantiation() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        val genericType = GenericType("T")
        val typeArguments = listOf(PrimitiveType.StringType)
        
        val result = basicTypeChecker.instantiateGenericType(genericType, typeArguments)
        assertEquals(PrimitiveType.StringType, result)
    }
    
    @Test
    fun testUserDefinedTypeValidation() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        
        // 测试有效的结构体类型
        val validStructType = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf(
                "name" to FieldType(PrimitiveType.StringType),
                "age" to FieldType(PrimitiveType.IntType)
            ),
            location = testLocation()
        )
        
        val validationErrors = basicTypeChecker.validateUserDefinedType(validStructType)
        assertTrue(validationErrors.isEmpty())
        
        // 测试包含无效字段类型的结构体 - 使用一个真正无效的类型
        val invalidType = UserDefinedType.StructType(
            name = "NonExistentType",
            fields = mapOf("field" to FieldType(PrimitiveType.StringType)),
            location = testLocation()
        )
        // 不注册这个类型，然后引用它
        val invalidStructType = UserDefinedType.StructType(
            name = "InvalidPerson",
            fields = mapOf(
                "name" to FieldType(invalidType)
            ),
            location = testLocation()
        )
        
        val invalidErrors = basicTypeChecker.validateUserDefinedType(invalidStructType)
        assertTrue(invalidErrors.isNotEmpty())
        assertEquals("INVALID_FIELD_TYPE", invalidErrors[0].errorCode)
    }
    
    @Test
    fun testTypeConstructor() {
        val basicTypeChecker = typeChecker as BasicTypeChecker
        
        // 测试 Array 类型构造器
        val arrayConstructor = basicTypeChecker.createTypeConstructor(
            "Array",
            listOf(GenericParameter("T"))
        )
        
        val arrayType = arrayConstructor.apply(listOf(PrimitiveType.StringType))
        assertTrue(arrayType is CompositeType.ArrayType)
        assertEquals(PrimitiveType.StringType, (arrayType as CompositeType.ArrayType).elementType)
        
        // 测试 Object 类型构造器
        val objectConstructor = basicTypeChecker.createTypeConstructor(
            "Object",
            listOf(GenericParameter("K"), GenericParameter("V"))
        )
        
        val objectType = objectConstructor.apply(listOf(PrimitiveType.StringType, PrimitiveType.IntType))
        assertTrue(objectType is CompositeType.ObjectType)
        val objType = objectType as CompositeType.ObjectType
        assertEquals(PrimitiveType.StringType, objType.keyType)
        assertEquals(PrimitiveType.IntType, objType.valueType)
    }
    
    @Test
    fun testEnumTypeDefinition() {
        val enumType = UserDefinedType.EnumType(
            name = "Color",
            variants = mapOf(
                "Red" to null,
                "Green" to null,
                "Blue" to null,
                "Custom" to PrimitiveType.StringType
            ),
            location = testLocation()
        )
        
        typeChecker.registerType(enumType)
        
        val resolvedType = typeChecker.resolveType("Color")
        assertEquals(enumType, resolvedType)
        
        // 测试枚举类型的赋值兼容性
        assertTrue(enumType.isAssignableFrom(enumType))
        assertFalse(enumType.isAssignableFrom(PrimitiveType.StringType))
    }
    
    @Test
    fun testUnionTypeDefinition() {
        val unionType = UserDefinedType.UnionType(
            name = "StringOrNumber",
            types = listOf(PrimitiveType.StringType, PrimitiveType.IntType),
            location = testLocation()
        )
        
        typeChecker.registerType(unionType)
        
        val resolvedType = typeChecker.resolveType("StringOrNumber")
        assertEquals(unionType, resolvedType)
        
        // 测试联合类型的赋值兼容性
        assertTrue(unionType.isAssignableFrom(PrimitiveType.StringType))
        assertTrue(unionType.isAssignableFrom(PrimitiveType.IntType))
        assertFalse(unionType.isAssignableFrom(PrimitiveType.BooleanType))
    }
    
    @Test
    fun testTypeAliasDefinition() {
        val aliasType = UserDefinedType.AliasType(
            name = "UserId",
            aliasedType = PrimitiveType.LongType,
            location = testLocation()
        )
        
        typeChecker.registerType(aliasType)
        
        val resolvedType = typeChecker.resolveType("UserId")
        assertEquals(aliasType, resolvedType)
        
        // 测试类型别名的赋值兼容性
        assertTrue(aliasType.isAssignableFrom(PrimitiveType.LongType))
        assertFalse(aliasType.isAssignableFrom(PrimitiveType.StringType))
    }
}
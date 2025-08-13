package cn.llonvne.jsonlang.core.types

import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlin.test.*

class SubtypeCheckerTest {
    
    private fun testLocation(line: Int = 1, column: Int = 1): SourceLocation {
        return SourceLocation("test", line, column, line, column, 0, 0)
    }
    
    @Test
    fun testBasicSubtypeRelations() {
        // 反射性：每个类型都是自己的子类型
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.StringType, PrimitiveType.StringType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.IntType))
        
        // Any 是所有类型的超类型
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.StringType, PrimitiveType.AnyType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.AnyType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.BooleanType, PrimitiveType.AnyType))
        
        // Nothing 是所有类型的子类型
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.NothingType, PrimitiveType.StringType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.NothingType, PrimitiveType.IntType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.NothingType, PrimitiveType.AnyType))
    }
    
    @Test
    fun testNullabilitySubtypeRules() {
        // Null 只能赋值给可空类型
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.NullType, PrimitiveType.NullableStringType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.NullType, PrimitiveType.NullableIntType))
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.NullType, PrimitiveType.StringType))
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.NullType, PrimitiveType.IntType))
        
        // 非空类型不能赋值给可空类型的非空版本
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.NullableStringType, PrimitiveType.StringType))
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.NullableIntType, PrimitiveType.IntType))
    }
    
    @Test
    fun testNumericSubtypeHierarchy() {
        // 数值类型的隐式提升
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.LongType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.FloatType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.IntType, PrimitiveType.DoubleType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.LongType, PrimitiveType.FloatType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.LongType, PrimitiveType.DoubleType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.FloatType, PrimitiveType.DoubleType))
        
        // 反向不成立
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.LongType, PrimitiveType.IntType))
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.FloatType, PrimitiveType.IntType))
        assertFalse(SubtypeChecker.isSubtype(PrimitiveType.DoubleType, PrimitiveType.FloatType))
    }
    
    @Test
    fun testArraySubtypeCovariance() {
        val intArray = CompositeType.ArrayType(PrimitiveType.IntType)
        val longArray = CompositeType.ArrayType(PrimitiveType.LongType)
        val anyArray = CompositeType.ArrayType(PrimitiveType.AnyType)
        
        // 数组协变性
        assertTrue(SubtypeChecker.isSubtype(intArray, longArray))
        assertTrue(SubtypeChecker.isSubtype(intArray, anyArray))
        assertTrue(SubtypeChecker.isSubtype(longArray, anyArray))
        
        assertFalse(SubtypeChecker.isSubtype(longArray, intArray))
        assertFalse(SubtypeChecker.isSubtype(anyArray, intArray))
    }
    
    @Test
    fun testFunctionSubtypeContravarianceCovariance() {
        // 函数类型：参数逆变，返回值协变
        val func1 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.AnyType),
            returnType = PrimitiveType.StringType
        )
        
        val func2 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.StringType),
            returnType = PrimitiveType.AnyType
        )
        
        // func1 是 func2 的子类型
        // 因为：Any -> String <: String -> Any
        // 参数：String <: Any（逆变）
        // 返回值：String <: Any（协变）
        assertTrue(SubtypeChecker.isSubtype(func1, func2))
        assertFalse(SubtypeChecker.isSubtype(func2, func1))
    }
    
    @Test
    fun testOptionalTypeSubtypeRules() {
        val optionalString = CompositeType.OptionalType(PrimitiveType.StringType)
        val optionalAny = CompositeType.OptionalType(PrimitiveType.AnyType)
        
        // Optional<String> <: Optional<Any>
        assertTrue(SubtypeChecker.isSubtype(optionalString, optionalAny))
        
        // String <: Optional<String>
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.StringType, optionalString))
        
        // Optional<String> <: String（当目标类型不可空时）
        assertTrue(SubtypeChecker.isSubtype(optionalString, PrimitiveType.StringType))
    }
    
    @Test
    fun testUnionTypeSubtypeRules() {
        val location = testLocation()
        
        val stringOrInt = UserDefinedType.UnionType(
            name = "StringOrInt",
            types = listOf(PrimitiveType.StringType, PrimitiveType.IntType),
            location = location
        )
        
        val stringOrIntOrBool = UserDefinedType.UnionType(
            name = "StringOrIntOrBool",
            types = listOf(PrimitiveType.StringType, PrimitiveType.IntType, PrimitiveType.BooleanType),
            location = location
        )
        
        // String <: StringOrInt
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.StringType, stringOrInt))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.IntType, stringOrInt))
        
        // StringOrInt <: StringOrIntOrBool
        assertTrue(SubtypeChecker.isSubtype(stringOrInt, stringOrIntOrBool))
        
        // StringOrIntOrBool 不是 StringOrInt 的子类型
        assertFalse(SubtypeChecker.isSubtype(stringOrIntOrBool, stringOrInt))
    }
    
    @Test
    fun testStructTypeSubtypeRules() {
        val location = testLocation()
        
        val person1 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf("name" to FieldType(PrimitiveType.StringType)),
            location = location
        )
        
        val person2 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf("name" to FieldType(PrimitiveType.StringType)),
            location = location
        )
        
        val employee = UserDefinedType.StructType(
            name = "Employee",
            fields = mapOf("name" to FieldType(PrimitiveType.StringType)),
            location = location
        )
        
        // 相同名称的结构体是子类型
        assertTrue(SubtypeChecker.isSubtype(person1, person2))
        
        // 不同名称的结构体不是子类型（名义类型系统）
        assertFalse(SubtypeChecker.isSubtype(person1, employee))
    }
    
    @Test
    fun testTypeAliasTransparency() {
        val location = testLocation()
        
        val stringAlias = UserDefinedType.AliasType(
            name = "MyString",
            aliasedType = PrimitiveType.StringType,
            location = location
        )
        
        // 类型别名是透明的
        assertTrue(SubtypeChecker.isSubtype(stringAlias, PrimitiveType.StringType))
        assertTrue(SubtypeChecker.isSubtype(PrimitiveType.StringType, stringAlias))
        assertTrue(SubtypeChecker.isSubtype(stringAlias, PrimitiveType.AnyType))
    }
    
    @Test
    fun testGenericTypeSubtypeRules() {
        val genericT = GenericType("T")
        val genericU = GenericType("U")
        val constrainedT = GenericType("T", listOf(PrimitiveType.StringType))
        
        // 相同名称的泛型类型
        assertTrue(SubtypeChecker.isSubtype(genericT, genericT))
        assertFalse(SubtypeChecker.isSubtype(genericT, genericU))
        
        // 有约束的泛型类型
        assertTrue(SubtypeChecker.isSubtype(constrainedT, constrainedT))
    }
    
    @Test
    fun testSafeConversion() {
        // 子类型关系允许安全转换
        assertTrue(SubtypeChecker.isSafeConversion(PrimitiveType.IntType, PrimitiveType.LongType))
        assertTrue(SubtypeChecker.isSafeConversion(PrimitiveType.StringType, PrimitiveType.AnyType))
        
        // 显式的安全转换规则
        assertTrue(SubtypeChecker.isSafeConversion(PrimitiveType.IntType, PrimitiveType.StringType))
        assertTrue(SubtypeChecker.isSafeConversion(PrimitiveType.BooleanType, PrimitiveType.StringType))
        
        // 不安全的转换
        assertFalse(SubtypeChecker.isSafeConversion(PrimitiveType.StringType, PrimitiveType.IntType))
        assertFalse(SubtypeChecker.isSafeConversion(PrimitiveType.FloatType, PrimitiveType.IntType))
    }
    
    @Test
    fun testExplicitConversionRequirement() {
        // 子类型不需要显式转换
        assertFalse(SubtypeChecker.requiresExplicitConversion(PrimitiveType.IntType, PrimitiveType.LongType))
        
        // 安全转换不需要显式转换
        assertFalse(SubtypeChecker.requiresExplicitConversion(PrimitiveType.IntType, PrimitiveType.StringType))
        
        // 需要显式转换的情况
        assertTrue(SubtypeChecker.requiresExplicitConversion(PrimitiveType.DoubleType, PrimitiveType.FloatType))
        assertTrue(SubtypeChecker.requiresExplicitConversion(PrimitiveType.LongType, PrimitiveType.IntType))
        assertTrue(SubtypeChecker.requiresExplicitConversion(PrimitiveType.StringType, PrimitiveType.IntType))
    }
    
    @Test
    fun testGetAllSupertypes() {
        val supertypes = SubtypeChecker.getAllSupertypes(PrimitiveType.StringType)
        
        assertTrue(supertypes.contains(PrimitiveType.AnyType))
        assertFalse(supertypes.contains(PrimitiveType.IntType))
        
        val nullableSupertypes = SubtypeChecker.getAllSupertypes(PrimitiveType.NullableStringType)
        assertTrue(nullableSupertypes.contains(PrimitiveType.StringType))
        assertTrue(nullableSupertypes.contains(PrimitiveType.AnyType))
    }
    
    @Test
    fun testGetAllSubtypes() {
        val allTypes = listOf(
            PrimitiveType.StringType,
            PrimitiveType.IntType,
            PrimitiveType.NothingType,
            PrimitiveType.AnyType,
            PrimitiveType.NullType
        )
        
        val anySubtypes = SubtypeChecker.getAllSubtypes(PrimitiveType.AnyType, allTypes)
        assertEquals(allTypes.toSet(), anySubtypes)
        
        val stringSubtypes = SubtypeChecker.getAllSubtypes(PrimitiveType.StringType, allTypes)
        assertTrue(stringSubtypes.contains(PrimitiveType.StringType))
        assertTrue(stringSubtypes.contains(PrimitiveType.NothingType))
        assertFalse(stringSubtypes.contains(PrimitiveType.IntType))
    }
}
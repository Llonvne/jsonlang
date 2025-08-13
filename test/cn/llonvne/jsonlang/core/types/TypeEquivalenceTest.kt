package cn.llonvne.jsonlang.core.types

import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlin.test.*

class TypeEquivalenceTest {
    
    private fun testLocation(line: Int = 1, column: Int = 1): SourceLocation {
        return SourceLocation("test", line, column, line, column, 0, 0)
    }
    
    @Test
    fun testStructuralEquivalence() {
        // 基础类型的结构等价性
        assertTrue(TypeEquivalence.areStructurallyEquivalent(
            PrimitiveType.StringType, 
            PrimitiveType.StringType
        ))
        
        assertFalse(TypeEquivalence.areStructurallyEquivalent(
            PrimitiveType.StringType, 
            PrimitiveType.IntType
        ))
        
        // 可空性影响结构等价性
        assertFalse(TypeEquivalence.areStructurallyEquivalent(
            PrimitiveType.StringType, 
            PrimitiveType.NullableStringType
        ))
    }
    
    @Test
    fun testArrayStructuralEquivalence() {
        val intArray1 = CompositeType.ArrayType(PrimitiveType.IntType)
        val intArray2 = CompositeType.ArrayType(PrimitiveType.IntType)
        val stringArray = CompositeType.ArrayType(PrimitiveType.StringType)
        
        assertTrue(TypeEquivalence.areStructurallyEquivalent(intArray1, intArray2))
        assertFalse(TypeEquivalence.areStructurallyEquivalent(intArray1, stringArray))
    }
    
    @Test
    fun testFunctionStructuralEquivalence() {
        val func1 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.IntType, PrimitiveType.StringType),
            returnType = PrimitiveType.BooleanType
        )
        
        val func2 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.IntType, PrimitiveType.StringType),
            returnType = PrimitiveType.BooleanType
        )
        
        val func3 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.IntType),
            returnType = PrimitiveType.BooleanType
        )
        
        assertTrue(TypeEquivalence.areStructurallyEquivalent(func1, func2))
        assertFalse(TypeEquivalence.areStructurallyEquivalent(func1, func3))
    }
    
    @Test
    fun testStructTypeEquivalence() {
        val location = testLocation()
        
        val struct1 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf(
                "name" to FieldType(PrimitiveType.StringType),
                "age" to FieldType(PrimitiveType.IntType)
            ),
            location = location
        )
        
        val struct2 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf(
                "name" to FieldType(PrimitiveType.StringType),
                "age" to FieldType(PrimitiveType.IntType)
            ),
            location = location
        )
        
        val struct3 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf(
                "name" to FieldType(PrimitiveType.StringType),
                "age" to FieldType(PrimitiveType.IntType),
                "email" to FieldType(PrimitiveType.StringType)
            ),
            location = location
        )
        
        assertTrue(TypeEquivalence.areStructurallyEquivalent(struct1, struct2))
        assertFalse(TypeEquivalence.areStructurallyEquivalent(struct1, struct3))
    }
    
    @Test
    fun testUnionTypeEquivalence() {
        val location = testLocation()
        
        val union1 = UserDefinedType.UnionType(
            name = "StringOrInt",
            types = listOf(PrimitiveType.StringType, PrimitiveType.IntType),
            location = location
        )
        
        val union2 = UserDefinedType.UnionType(
            name = "StringOrInt",
            types = listOf(PrimitiveType.IntType, PrimitiveType.StringType), // 顺序不同
            location = location
        )
        
        val union3 = UserDefinedType.UnionType(
            name = "StringOrInt",
            types = listOf(PrimitiveType.StringType, PrimitiveType.BooleanType),
            location = location
        )
        
        assertTrue(TypeEquivalence.areStructurallyEquivalent(union1, union2))
        assertFalse(TypeEquivalence.areStructurallyEquivalent(union1, union3))
    }
    
    @Test
    fun testNominalEquivalence() {
        val location = testLocation()
        
        val struct1 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf("name" to FieldType(PrimitiveType.StringType)),
            location = location
        )
        
        val struct2 = UserDefinedType.StructType(
            name = "Person",
            fields = mapOf("name" to FieldType(PrimitiveType.StringType)),
            location = location
        )
        
        val struct3 = UserDefinedType.StructType(
            name = "Employee", // 不同名称
            fields = mapOf("name" to FieldType(PrimitiveType.StringType)),
            location = location
        )
        
        assertTrue(TypeEquivalence.areNominallyEquivalent(struct1, struct2))
        assertFalse(TypeEquivalence.areNominallyEquivalent(struct1, struct3))
    }
    
    @Test
    fun testTypeCompatibility() {
        // 基础兼容性
        assertTrue(TypeEquivalence.areCompatible(
            PrimitiveType.StringType, 
            PrimitiveType.AnyType
        ))
        
        assertTrue(TypeEquivalence.areCompatible(
            PrimitiveType.NothingType, 
            PrimitiveType.StringType
        ))
        
        assertTrue(TypeEquivalence.areCompatible(
            PrimitiveType.NullType, 
            PrimitiveType.NullableStringType
        ))
        
        // 数值类型兼容性
        assertTrue(TypeEquivalence.areCompatible(
            PrimitiveType.IntType, 
            PrimitiveType.LongType
        ))
        
        assertTrue(TypeEquivalence.areCompatible(
            PrimitiveType.IntType, 
            PrimitiveType.FloatType
        ))
        
        assertFalse(TypeEquivalence.areCompatible(
            PrimitiveType.FloatType, 
            PrimitiveType.IntType
        ))
    }
    
    @Test
    fun testOptionalTypeCompatibility() {
        val optionalString = CompositeType.OptionalType(PrimitiveType.StringType)
        
        // Optional<String> 兼容 String
        assertTrue(TypeEquivalence.areCompatible(optionalString, PrimitiveType.StringType))
        
        // String 兼容 Optional<String>
        assertTrue(TypeEquivalence.areCompatible(PrimitiveType.StringType, optionalString))
        
        // Optional<String> 兼容 Optional<Any>
        val optionalAny = CompositeType.OptionalType(PrimitiveType.AnyType)
        assertTrue(TypeEquivalence.areCompatible(optionalString, optionalAny))
    }
    
    @Test
    fun testFunctionTypeCompatibility() {
        val func1 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.AnyType),
            returnType = PrimitiveType.StringType
        )
        
        val func2 = CompositeType.FunctionType(
            parameterTypes = listOf(PrimitiveType.StringType),
            returnType = PrimitiveType.AnyType
        )
        
        // func1 兼容 func2（参数逆变，返回值协变）
        assertTrue(TypeEquivalence.areCompatible(func1, func2))
        assertFalse(TypeEquivalence.areCompatible(func2, func1))
    }
    
    @Test
    fun testCommonSupertypeComputation() {
        // 相同类型
        assertEquals(
            PrimitiveType.StringType,
            TypeEquivalence.computeCommonSupertype(PrimitiveType.StringType, PrimitiveType.StringType)
        )
        
        // 数值类型
        assertEquals(
            PrimitiveType.FloatType,
            TypeEquivalence.computeCommonSupertype(PrimitiveType.IntType, PrimitiveType.FloatType)
        )
        
        assertEquals(
            PrimitiveType.DoubleType,
            TypeEquivalence.computeCommonSupertype(PrimitiveType.FloatType, PrimitiveType.DoubleType)
        )
        
        // 不相关类型
        assertEquals(
            PrimitiveType.AnyType,
            TypeEquivalence.computeCommonSupertype(PrimitiveType.StringType, PrimitiveType.BooleanType)
        )
        
        // 可空类型
        val nullableString = PrimitiveType.NullableStringType
        val nullableInt = PrimitiveType.NullableIntType
        val commonNullable = TypeEquivalence.computeCommonSupertype(nullableString, nullableInt)
        assertEquals(PrimitiveType.NullableAnyType, commonNullable)
    }
    
    @Test
    fun testArrayCommonSupertype() {
        val intArray = CompositeType.ArrayType(PrimitiveType.IntType)
        val floatArray = CompositeType.ArrayType(PrimitiveType.FloatType)
        
        val commonArray = TypeEquivalence.computeCommonSupertype(intArray, floatArray)
        assertTrue(commonArray is CompositeType.ArrayType)
        assertEquals(PrimitiveType.FloatType, (commonArray as CompositeType.ArrayType).elementType)
    }
}
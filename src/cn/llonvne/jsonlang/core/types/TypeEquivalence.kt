package cn.llonvne.jsonlang.core.types

/**
 * 类型等价性检查工具
 * 提供各种类型等价性和兼容性检查功能
 */
object TypeEquivalence {
    
    /**
     * 检查结构等价性
     * 两个类型在结构上是否等价（忽略名称）
     */
    fun areStructurallyEquivalent(type1: Type, type2: Type): Boolean {
        return when {
            type1::class != type2::class -> false
            type1 is PrimitiveType && type2 is PrimitiveType -> 
                type1.name == type2.name && type1.nullable == type2.nullable
            type1 is CompositeType.ArrayType && type2 is CompositeType.ArrayType ->
                areStructurallyEquivalent(type1.elementType, type2.elementType) &&
                type1.nullable == type2.nullable
            type1 is CompositeType.ObjectType && type2 is CompositeType.ObjectType ->
                areStructurallyEquivalent(type1.keyType, type2.keyType) &&
                areStructurallyEquivalent(type1.valueType, type2.valueType) &&
                type1.nullable == type2.nullable
            type1 is CompositeType.FunctionType && type2 is CompositeType.FunctionType ->
                type1.parameterTypes.size == type2.parameterTypes.size &&
                type1.parameterTypes.zip(type2.parameterTypes).all { (p1, p2) ->
                    areStructurallyEquivalent(p1, p2)
                } &&
                areStructurallyEquivalent(type1.returnType, type2.returnType) &&
                type1.nullable == type2.nullable
            type1 is CompositeType.OptionalType && type2 is CompositeType.OptionalType ->
                areStructurallyEquivalent(type1.innerType, type2.innerType)
            type1 is UserDefinedType.StructType && type2 is UserDefinedType.StructType ->
                type1.name == type2.name &&
                areStructFieldsEquivalent(type1.fields, type2.fields) &&
                type1.nullable == type2.nullable
            type1 is UserDefinedType.EnumType && type2 is UserDefinedType.EnumType ->
                areEnumVariantsEquivalent(type1.variants, type2.variants) &&
                type1.nullable == type2.nullable
            type1 is UserDefinedType.UnionType && type2 is UserDefinedType.UnionType ->
                areUnionTypesEquivalent(type1.types, type2.types) &&
                type1.nullable == type2.nullable
            type1 is UserDefinedType.AliasType && type2 is UserDefinedType.AliasType ->
                areStructurallyEquivalent(type1.aliasedType, type2.aliasedType) &&
                type1.nullable == type2.nullable
            type1 is GenericType && type2 is GenericType ->
                type1.name == type2.name &&
                areConstraintsEquivalent(type1.constraints, type2.constraints) &&
                type1.nullable == type2.nullable
            else -> false
        }
    }
    
    private fun areStructFieldsEquivalent(fields1: Map<String, FieldType>, fields2: Map<String, FieldType>): Boolean {
        if (fields1.size != fields2.size) return false
        
        return fields1.all { (name, field1) ->
            val field2 = fields2[name] ?: return false
            areStructurallyEquivalent(field1.type, field2.type) &&
            field1.optional == field2.optional
        }
    }
    
    private fun areEnumVariantsEquivalent(variants1: Map<String, Type?>, variants2: Map<String, Type?>): Boolean {
        if (variants1.size != variants2.size) return false
        
        return variants1.all { (name, type1) ->
            val type2 = variants2[name] ?: return false
            when {
                type1 == null && type2 == null -> true
                type1 != null && type2 != null -> areStructurallyEquivalent(type1, type2)
                else -> false
            }
        }
    }
    
    private fun areUnionTypesEquivalent(types1: List<Type>, types2: List<Type>): Boolean {
        if (types1.size != types2.size) return false
        
        // 联合类型的顺序不重要，所以需要检查每个类型是否都有对应的等价类型
        return types1.all { type1 ->
            types2.any { type2 -> areStructurallyEquivalent(type1, type2) }
        } && types2.all { type2 ->
            types1.any { type1 -> areStructurallyEquivalent(type1, type2) }
        }
    }
    
    private fun areConstraintsEquivalent(constraints1: List<Type>, constraints2: List<Type>): Boolean {
        if (constraints1.size != constraints2.size) return false
        
        return constraints1.zip(constraints2).all { (c1, c2) ->
            areStructurallyEquivalent(c1, c2)
        }
    }
    
    /**
     * 检查名义等价性
     * 两个类型在名称上是否等价
     */
    fun areNominallyEquivalent(type1: Type, type2: Type): Boolean {
        return when {
            type1::class != type2::class -> false
            type1 is PrimitiveType && type2 is PrimitiveType -> 
                type1.name == type2.name && type1.nullable == type2.nullable
            type1 is UserDefinedType && type2 is UserDefinedType ->
                type1.name == type2.name && type1.nullable == type2.nullable
            type1 is GenericType && type2 is GenericType ->
                type1.name == type2.name && type1.nullable == type2.nullable
            else -> areStructurallyEquivalent(type1, type2)
        }
    }
    
    /**
     * 检查类型兼容性
     * 考虑隐式转换和子类型关系
     */
    fun areCompatible(sourceType: Type, targetType: Type): Boolean {
        return SubtypeChecker.isSubtype(sourceType, targetType) || 
               SubtypeChecker.isSafeConversion(sourceType, targetType)
    }
    
    /**
     * 计算两个类型的最小公共超类型
     */
    fun computeCommonSupertype(type1: Type, type2: Type): Type {
        return when {
            areStructurallyEquivalent(type1, type2) -> type1
            SubtypeChecker.isSubtype(type1, type2) -> type2
            SubtypeChecker.isSubtype(type2, type1) -> type1
            type1.nullable || type2.nullable -> {
                val nonNull1 = type1.makeNonNull()
                val nonNull2 = type2.makeNonNull()
                computeCommonSupertype(nonNull1, nonNull2).makeNullable()
            }
            isNumericType(type1) && isNumericType(type2) -> computeNumericCommonType(type1, type2)
            type1 is CompositeType.ArrayType && type2 is CompositeType.ArrayType -> {
                val commonElementType = computeCommonSupertype(type1.elementType, type2.elementType)
                CompositeType.ArrayType(commonElementType, type1.nullable || type2.nullable)
            }
            type1 is CompositeType.OptionalType && type2 is CompositeType.OptionalType -> {
                val commonInnerType = computeCommonSupertype(type1.innerType, type2.innerType)
                CompositeType.OptionalType(commonInnerType)
            }
            else -> PrimitiveType.AnyType
        }
    }
    
    private fun isNumericType(type: Type): Boolean {
        return type in listOf(
            PrimitiveType.IntType,
            PrimitiveType.LongType,
            PrimitiveType.FloatType,
            PrimitiveType.DoubleType
        )
    }
    
    private fun computeNumericCommonType(type1: Type, type2: Type): Type {
        val types = listOf(type1, type2)
        return when {
            PrimitiveType.DoubleType in types -> PrimitiveType.DoubleType
            PrimitiveType.FloatType in types -> PrimitiveType.FloatType
            PrimitiveType.LongType in types -> PrimitiveType.LongType
            else -> PrimitiveType.IntType
        }
    }
}
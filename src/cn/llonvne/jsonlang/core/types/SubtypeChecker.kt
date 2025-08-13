package cn.llonvne.jsonlang.core.types

/**
 * 子类型关系检查器
 * 实现 JSONLang 的子类型规则
 */
object SubtypeChecker {
    
    /**
     * 检查 subtype 是否是 supertype 的子类型
     */
    fun isSubtype(subtype: Type, supertype: Type): Boolean {
        return when {
            // 反射性：每个类型都是自己的子类型
            TypeEquivalence.areStructurallyEquivalent(subtype, supertype) -> true
            
            // Any 是所有类型的超类型
            supertype == PrimitiveType.AnyType -> true
            
            // Nothing 是所有类型的子类型
            subtype == PrimitiveType.NothingType -> true
            
            // Null 类型只能赋值给可空类型
            subtype is PrimitiveType.NullType -> supertype.nullable
            
            // 可选类型的子类型规则（在可空性检查之前）
            supertype is CompositeType.OptionalType -> 
                isSubtype(subtype, supertype.innerType) || 
                (subtype is CompositeType.OptionalType && isSubtype(subtype.innerType, supertype.innerType)) ||
                subtype is PrimitiveType.NullType
            
            subtype is CompositeType.OptionalType ->
                isSubtype(subtype.innerType, supertype)
            
            // 可空性规则：非空类型不能赋值给可空类型的非空版本
            subtype.nullable && !supertype.nullable -> false
            
            // 联合类型的子类型规则
            subtype is UserDefinedType.UnionType && supertype is UserDefinedType.UnionType -> 
                subtype.types.all { subType -> supertype.types.any { superType -> isSubtype(subType, superType) } }
            
            supertype is UserDefinedType.UnionType -> 
                supertype.types.any { isSubtype(subtype, it) }
            
            subtype is UserDefinedType.UnionType -> 
                subtype.types.all { isSubtype(it, supertype) }
            
            // 数组类型的协变性
            subtype is CompositeType.ArrayType && supertype is CompositeType.ArrayType ->
                isSubtype(subtype.elementType, supertype.elementType)
            
            // 对象类型的协变性
            subtype is CompositeType.ObjectType && supertype is CompositeType.ObjectType ->
                isSubtype(subtype.keyType, supertype.keyType) &&
                isSubtype(subtype.valueType, supertype.valueType)
            
            // 函数类型的协变和逆变
            subtype is CompositeType.FunctionType && supertype is CompositeType.FunctionType ->
                isFunctionSubtype(subtype, supertype)
            
            // 结构体类型的名义子类型
            subtype is UserDefinedType.StructType && supertype is UserDefinedType.StructType ->
                isStructSubtype(subtype, supertype)
            
            // 枚举类型的名义子类型
            subtype is UserDefinedType.EnumType && supertype is UserDefinedType.EnumType ->
                subtype.name == supertype.name
            
            // 类型别名的透明性
            subtype is UserDefinedType.AliasType ->
                isSubtype(subtype.aliasedType, supertype)
            
            supertype is UserDefinedType.AliasType ->
                isSubtype(subtype, supertype.aliasedType)
            
            // 泛型类型的子类型规则
            subtype is GenericType && supertype is GenericType ->
                isGenericSubtype(subtype, supertype)
            
            // 数值类型的隐式转换
            isNumericSubtype(subtype, supertype) -> true
            
            // 默认情况：使用类型的 isAssignableFrom 方法
            else -> supertype.isAssignableFrom(subtype)
        }
    }
    
    /**
     * 检查函数类型的子类型关系
     * 函数类型的子类型规则：
     * - 参数类型是逆变的（contravariant）
     * - 返回类型是协变的（covariant）
     */
    private fun isFunctionSubtype(subtype: CompositeType.FunctionType, supertype: CompositeType.FunctionType): Boolean {
        if (subtype.parameterTypes.size != supertype.parameterTypes.size) {
            return false
        }
        
        // 参数类型逆变：超类型的参数必须是子类型参数的子类型
        val parametersValid = subtype.parameterTypes.zip(supertype.parameterTypes).all { (subParam, superParam) ->
            isSubtype(superParam, subParam)
        }
        
        // 返回类型协变：子类型的返回类型必须是超类型返回类型的子类型
        val returnTypeValid = isSubtype(subtype.returnType, supertype.returnType)
        
        return parametersValid && returnTypeValid
    }
    
    /**
     * 检查结构体类型的子类型关系
     * 结构体使用名义子类型系统
     */
    private fun isStructSubtype(subtype: UserDefinedType.StructType, supertype: UserDefinedType.StructType): Boolean {
        // 简单的名义检查：只有相同名称的结构体才是子类型
        // 在更复杂的系统中，可以支持结构子类型（structural subtyping）
        return subtype.name == supertype.name
    }
    
    /**
     * 检查泛型类型的子类型关系
     */
    private fun isGenericSubtype(subtype: GenericType, supertype: GenericType): Boolean {
        return when {
            // 相同的泛型类型参数
            subtype.name == supertype.name -> true
            
            // 检查约束条件
            subtype.constraints.isNotEmpty() && supertype.constraints.isNotEmpty() -> {
                // 子类型的约束必须更严格（是超类型约束的子类型）
                supertype.constraints.all { superConstraint ->
                    subtype.constraints.any { subConstraint ->
                        isSubtype(subConstraint, superConstraint)
                    }
                }
            }
            
            // 无约束的泛型类型
            subtype.constraints.isEmpty() && supertype.constraints.isEmpty() -> false
            
            else -> false
        }
    }
    
    /**
     * 检查数值类型的子类型关系
     * 支持数值类型的隐式提升
     */
    private fun isNumericSubtype(subtype: Type, supertype: Type): Boolean {
        val numericHierarchy = listOf(
            PrimitiveType.IntType,
            PrimitiveType.LongType,
            PrimitiveType.FloatType,
            PrimitiveType.DoubleType
        )
        
        val subtypeIndex = numericHierarchy.indexOf(subtype)
        val supertypeIndex = numericHierarchy.indexOf(supertype)
        
        return subtypeIndex != -1 && supertypeIndex != -1 && subtypeIndex <= supertypeIndex
    }
    
    /**
     * 检查类型是否可以安全地转换为另一个类型
     */
    fun isSafeConversion(fromType: Type, toType: Type): Boolean {
        return when {
            isSubtype(fromType, toType) -> true
            
            // 显式的安全转换规则
            fromType == PrimitiveType.IntType && toType == PrimitiveType.StringType -> true
            fromType == PrimitiveType.FloatType && toType == PrimitiveType.StringType -> true
            fromType == PrimitiveType.BooleanType && toType == PrimitiveType.StringType -> true
            
            // 可选类型的安全转换
            fromType is CompositeType.OptionalType && toType is CompositeType.OptionalType ->
                isSafeConversion(fromType.innerType, toType.innerType)
            
            // Optional<T> 可以安全转换为 T
            fromType is CompositeType.OptionalType && !toType.nullable ->
                isSafeConversion(fromType.innerType, toType)
            
            // T 可以安全转换为 Optional<T>
            !fromType.nullable && toType is CompositeType.OptionalType ->
                isSafeConversion(fromType, toType.innerType)
            
            else -> false
        }
    }
    
    /**
     * 检查类型是否需要显式转换
     */
    fun requiresExplicitConversion(fromType: Type, toType: Type): Boolean {
        return when {
            isSubtype(fromType, toType) -> false
            isSafeConversion(fromType, toType) -> false
            
            // 需要显式转换的情况
            fromType == PrimitiveType.DoubleType && toType == PrimitiveType.FloatType -> true
            fromType == PrimitiveType.FloatType && toType == PrimitiveType.IntType -> true
            fromType == PrimitiveType.LongType && toType == PrimitiveType.IntType -> true
            fromType == PrimitiveType.StringType && isNumericType(toType) -> true
            
            else -> true
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
    
    /**
     * 获取类型层次结构中的所有超类型
     */
    fun getAllSupertypes(type: Type): Set<Type> {
        val supertypes = mutableSetOf<Type>()
        
        // 添加直接超类型
        when (type) {
            is PrimitiveType -> {
                if (type != PrimitiveType.AnyType) {
                    supertypes.add(PrimitiveType.AnyType)
                }
                if (type.nullable) {
                    supertypes.add(type.makeNonNull())
                }
            }
            
            is CompositeType.ArrayType -> {
                supertypes.add(PrimitiveType.AnyType)
                if (type.nullable) {
                    supertypes.add(type.makeNonNull())
                }
            }
            
            is CompositeType.OptionalType -> {
                supertypes.add(type.innerType)
                supertypes.add(PrimitiveType.AnyType)
            }
            
            is UserDefinedType.UnionType -> {
                supertypes.addAll(type.types)
                supertypes.add(PrimitiveType.AnyType)
            }
            
            is UserDefinedType.AliasType -> {
                supertypes.addAll(getAllSupertypes(type.aliasedType))
            }
            
            else -> {
                supertypes.add(PrimitiveType.AnyType)
            }
        }
        
        // 递归添加间接超类型
        val indirectSupertypes = supertypes.toList()
        for (supertype in indirectSupertypes) {
            if (supertype != type) {
                supertypes.addAll(getAllSupertypes(supertype))
            }
        }
        
        return supertypes
    }
    
    /**
     * 获取类型层次结构中的所有子类型
     */
    fun getAllSubtypes(type: Type, allTypes: Collection<Type>): Set<Type> {
        return allTypes.filter { isSubtype(it, type) }.toSet()
    }
}
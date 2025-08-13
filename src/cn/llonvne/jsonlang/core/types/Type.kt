package cn.llonvne.jsonlang.core.types

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.ast.GenericParameter
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * JSONLang 类型系统
 * 支持基础类型、复合类型、泛型类型和用户定义类型
 */
@Serializable
sealed class Type {
    /** 类型名称 */
    abstract val name: String

    /** 类型大小（字节数，-1表示动态大小） */
    abstract val size: Int

    /** 是否可为空 */
    abstract val nullable: Boolean

    /** 类型参数 */
    open val typeParameters: List<Type> = emptyList()

    /**
     * 检查类型兼容性
     */
    abstract fun isAssignableFrom(other: Type): Boolean

    /**
     * 获取类型的字符串表示
     */
    abstract fun getDisplayName(): String

    /**
     * 创建可空版本
     */
    abstract fun makeNullable(): Type

    /**
     * 创建非空版本
     */
    abstract fun makeNonNull(): Type
}

/**
 * 基础类型
 */
@Serializable
sealed class PrimitiveType(
    override val name: String,
    override val size: Int,
    override val nullable: Boolean = false,
) : Type() {

    override fun getDisplayName(): String = if (nullable) "$name?" else name

    override fun isAssignableFrom(other: Type): Boolean {
        return when (other) {
            this -> true
            is NullType if this.nullable -> true
            else -> false
        }
    }

    /** 字符串类型 */
    @Serializable
    object StringType : PrimitiveType("String", -1) {
        override fun makeNullable(): Type = NullableStringType
        override fun makeNonNull(): Type = this
    }

    /** 可空字符串类型 */
    @Serializable
    object NullableStringType : PrimitiveType("String", -1, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = StringType
    }

    /** 整数类型 */
    @Serializable
    object IntType : PrimitiveType("Int", 4) {
        override fun makeNullable(): Type = NullableIntType
        override fun makeNonNull(): Type = this
    }

    /** 可空整数类型 */
    @Serializable
    object NullableIntType : PrimitiveType("Int", 4, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = IntType
    }

    /** 长整数类型 */
    @Serializable
    object LongType : PrimitiveType("Long", 8) {
        override fun makeNullable(): Type = NullableLongType
        override fun makeNonNull(): Type = this
    }

    /** 可空长整数类型 */
    @Serializable
    object NullableLongType : PrimitiveType("Long", 8, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = LongType
    }

    /** 浮点数类型 */
    @Serializable
    object FloatType : PrimitiveType("Float", 4) {
        override fun makeNullable(): Type = NullableFloatType
        override fun makeNonNull(): Type = this
    }

    /** 可空浮点数类型 */
    @Serializable
    object NullableFloatType : PrimitiveType("Float", 4, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = FloatType
    }

    /** 双精度浮点数类型 */
    @Serializable
    object DoubleType : PrimitiveType("Double", 8) {
        override fun makeNullable(): Type = NullableDoubleType
        override fun makeNonNull(): Type = this
    }

    /** 可空双精度浮点数类型 */
    @Serializable
    object NullableDoubleType : PrimitiveType("Double", 8, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = DoubleType
    }

    /** 布尔类型 */
    @Serializable
    object BooleanType : PrimitiveType("Boolean", 1) {
        override fun makeNullable(): Type = NullableBooleanType
        override fun makeNonNull(): Type = this
    }

    /** 可空布尔类型 */
    @Serializable
    object NullableBooleanType : PrimitiveType("Boolean", 1, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = BooleanType
    }

    /** 单元类型（空类型） */
    @Serializable
    object UnitType : PrimitiveType("Unit", 0) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = this
    }

    /** 任意类型（顶级类型） */
    @Serializable
    object AnyType : PrimitiveType("Any", -1) {
        override fun makeNullable(): Type = NullableAnyType
        override fun makeNonNull(): Type = this
        override fun isAssignableFrom(other: Type): Boolean = true
    }

    /** 可空任意类型 */
    @Serializable
    object NullableAnyType : PrimitiveType("Any", -1, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = AnyType
        override fun isAssignableFrom(other: Type): Boolean = true
    }

    /** 空类型（底级类型） */
    @Serializable
    object NothingType : PrimitiveType("Nothing", 0) {
        override fun makeNullable(): Type = NullType
        override fun makeNonNull(): Type = this
        override fun isAssignableFrom(other: Type): Boolean = other == this
    }

    /** Null 类型 */
    @Serializable
    object NullType : PrimitiveType("Null", 0, true) {
        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = NothingType
        override fun isAssignableFrom(other: Type): Boolean = other == this
    }
}

/**
 * 复合类型
 */
@Serializable
sealed class CompositeType : Type() {
    /**
     * 数组类型
     */
    @Serializable
    data class ArrayType(
        val elementType: Type,
        override val nullable: Boolean = false,
    ) : CompositeType() {
        override val name: String = "Array"
        override val size: Int = -1

        override fun getDisplayName(): String {
            val base = "Array<${elementType.getDisplayName()}>"
            return if (nullable) "$base?" else base
        }

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is ArrayType -> elementType.isAssignableFrom(other.elementType)
                is PrimitiveType.NullType -> nullable
                else -> false
            }
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)

        override val typeParameters: List<Type> get() = listOf(elementType)
    }

    /**
     * 对象类型
     */
    @Serializable
    data class ObjectType(
        val keyType: Type,
        val valueType: Type,
        override val nullable: Boolean = false,
    ) : CompositeType() {
        override val name: String = "Object"
        override val size: Int = -1

        override fun getDisplayName(): String {
            val base = "Object<${keyType.getDisplayName()}, ${valueType.getDisplayName()}>"
            return if (nullable) "$base?" else base
        }

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is ObjectType -> keyType.isAssignableFrom(other.keyType) &&
                        valueType.isAssignableFrom(other.valueType)

                is PrimitiveType.NullType -> nullable
                else -> false
            }
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)

        override val typeParameters: List<Type> get() = listOf(keyType, valueType)
    }

    /**
     * 函数类型
     */
    @Serializable
    data class FunctionType(
        val parameterTypes: List<Type>,
        val returnType: Type,
        override val nullable: Boolean = false,
    ) : CompositeType() {
        override val name: String = "Function"
        override val size: Int = -1

        override fun getDisplayName(): String {
            val params = parameterTypes.joinToString(", ") { it.getDisplayName() }
            val base = "($params) -> ${returnType.getDisplayName()}"
            return if (nullable) "($base)?" else base
        }

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is FunctionType -> {
                    parameterTypes.size == other.parameterTypes.size &&
                            parameterTypes.zip(other.parameterTypes).all { (expected, actual) ->
                                actual.isAssignableFrom(expected) // 参数类型是逆变的
                            } &&
                            returnType.isAssignableFrom(other.returnType) // 返回类型是协变的
                }

                is PrimitiveType.NullType -> nullable
                else -> false
            }
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)

        override val typeParameters: List<Type> get() = parameterTypes + returnType
    }

    /**
     * 可选类型
     */
    @Serializable
    data class OptionalType(
        val innerType: Type,
    ) : CompositeType() {
        override val name: String = "Optional"
        override val size: Int = innerType.size
        override val nullable: Boolean = true

        override fun getDisplayName(): String = "Optional<${innerType.getDisplayName()}>"

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is OptionalType -> innerType.isAssignableFrom(other.innerType)
                is PrimitiveType.NullType -> true
                else -> innerType.isAssignableFrom(other)
            }
        }

        override fun makeNullable(): Type = this
        override fun makeNonNull(): Type = innerType

        override val typeParameters: List<Type> get() = listOf(innerType)
    }
}

/**
 * 用户定义类型
 */
@Serializable
public sealed class UserDefinedType : Type() {
    /**
     * 结构体类型
     */
    @Serializable
    data class StructType(
        override val name: String,
        val fields: Map<String, FieldType>,
        override val nullable: Boolean = false,
        val location: SourceLocation,
    ) : UserDefinedType() {
        override val size: Int = fields.values.sumOf { it.type.size }

        override fun getDisplayName(): String = if (nullable) "$name?" else name

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is StructType -> this.name == other.name
                is PrimitiveType.NullType -> nullable
                else -> false
            }
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)
    }

    /**
     * 枚举类型
     */
    @Serializable
    data class EnumType(
        override val name: String,
        val variants: Map<String, Type?>,
        override val nullable: Boolean = false,
        val location: SourceLocation,
    ) : UserDefinedType() {
        override val size: Int = 4 // 枚举通常用整数表示

        override fun getDisplayName(): String = if (nullable) "$name?" else name

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is EnumType -> this.name == other.name
                is PrimitiveType.NullType -> nullable
                else -> false
            }
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)
    }

    /**
     * 联合类型
     */
    @Serializable
    data class UnionType(
        override val name: String,
        val types: List<Type>,
        override val nullable: Boolean = false,
        val location: SourceLocation,
    ) : UserDefinedType() {
        override val size: Int = types.maxOfOrNull { it.size } ?: 0

        override fun getDisplayName(): String {
            val base = types.joinToString(" | ") { it.getDisplayName() }
            return if (nullable) "($base)?" else base
        }

        override fun isAssignableFrom(other: Type): Boolean {
            return when (other) {
                is PrimitiveType.NullType -> nullable
                else -> types.any { it.isAssignableFrom(other) }
            }
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)

        override val typeParameters: List<Type> get() = types
    }

    /**
     * 类型别名
     */
    @Serializable
    data class AliasType(
        override val name: String,
        val aliasedType: Type,
        override val nullable: Boolean = false,
        val location: SourceLocation,
    ) : UserDefinedType() {
        override val size: Int = aliasedType.size

        override fun getDisplayName(): String = if (nullable) "$name?" else name

        override fun isAssignableFrom(other: Type): Boolean {
            return aliasedType.isAssignableFrom(other) ||
                    (other is PrimitiveType.NullType && nullable)
        }

        override fun makeNullable(): Type = copy(nullable = true)
        override fun makeNonNull(): Type = copy(nullable = false)

        override val typeParameters: List<Type> get() = aliasedType.typeParameters
    }
}

/**
 * 泛型类型
 */
@Serializable
data class GenericType(
    override val name: String,
    val constraints: List<Type> = emptyList(),
    override val nullable: Boolean = false,
) : Type() {
    override val size: Int = -1

    override fun getDisplayName(): String = if (nullable) "$name?" else name

    override fun isAssignableFrom(other: Type): Boolean {
        return when (other) {
            is GenericType -> this.name == other.name
            is PrimitiveType.NullType -> nullable
            else -> constraints.isEmpty() || constraints.any { it.isAssignableFrom(other) }
        }
    }

    override fun makeNullable(): Type = copy(nullable = true)
    override fun makeNonNull(): Type = copy(nullable = false)
}

/**
 * 字段类型定义
 */
@Serializable
data class FieldType(
    val type: Type,
    val optional: Boolean = false,
    val defaultValue: @Contextual Any? = null,
)

/**
 * 类型构造器
 * 用于创建参数化类型实例
 */
@Serializable
data class TypeConstructor(
    val name: String,
    val typeParameters: List<GenericParameter> = emptyList(),
) {
    /**
     * 应用类型参数创建具体类型
     */
    fun apply(typeArguments: List<Type>): Type? {
        if (typeArguments.size != typeParameters.size) {
            return null
        }

        // 创建类型替换映射
        val substitutions = typeParameters.zip(typeArguments).associate { (param, arg) ->
            param.name to arg
        }

        // 这里应该根据构造器名称创建相应的类型
        // 简化实现：假设构造器名称对应已注册的泛型类型
        return when (name) {
            "Array" -> if (typeArguments.isNotEmpty()) CompositeType.ArrayType(typeArguments[0]) else null
            "Object" -> if (typeArguments.size >= 2) CompositeType.ObjectType(
                typeArguments[0],
                typeArguments[1]
            ) else null

            "Optional" -> if (typeArguments.isNotEmpty()) CompositeType.OptionalType(typeArguments[0]) else null
            "Function" -> {
                if (typeArguments.size >= 2) {
                    val paramTypes = typeArguments.dropLast(1)
                    val returnType = typeArguments.last()
                    CompositeType.FunctionType(paramTypes, returnType)
                } else null
            }

            else -> null
        }
    }

    /**
     * 获取构造器的显示名称
     */
    fun getDisplayName(): String {
        return if (typeParameters.isEmpty()) {
            name
        } else {
            val params = typeParameters.joinToString(", ") { param ->
                if (param.constraints.isNotEmpty()) {
                    "${param.name} : ${param.constraints.joinToString(" & ") { constraint -> constraint.getDisplayName() }}"
                } else {
                    param.name
                }
            }
            "$name<$params>"
        }
    }
}
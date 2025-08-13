package cn.llonvne.jsonlang.core.annotations

import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * 注解系统
 * 支持编译时和运行时注解，用于元编程和代码生成
 */
@Serializable
data class Annotation(
    /** 注解名称 */
    val name: String,
    /** 注解参数 */
    val parameters: Map<String, AnnotationValue> = emptyMap(),
    /** 注解目标 */
    val target: AnnotationTarget = AnnotationTarget.DEFAULT,
    /** 注解保留策略 */
    val retention: AnnotationRetention = AnnotationRetention.RUNTIME,
    /** 源码位置 */
    val location: SourceLocation
) {
    /**
     * 获取参数值
     */
    fun getParameter(name: String): AnnotationValue? = parameters[name]
    
    /**
     * 获取字符串参数
     */
    fun getStringParameter(name: String): String? = getParameter(name)?.asString()
    
    /**
     * 获取整数参数
     */
    fun getIntParameter(name: String): Long? = getParameter(name)?.asInt()
    
    /**
     * 获取布尔参数
     */
    fun getBooleanParameter(name: String): Boolean? = getParameter(name)?.asBoolean()
    
    /**
     * 获取数组参数
     */
    fun getArrayParameter(name: String): List<AnnotationValue>? = getParameter(name)?.asArray()
}

/**
 * 注解值
 */
@Serializable
sealed class AnnotationValue {
    /** 字符串值 */
    @Serializable
    data class StringValue(val value: String) : AnnotationValue()
    
    /** 整数值 */
    @Serializable
    data class IntValue(val value: Long) : AnnotationValue()
    
    /** 浮点数值 */
    @Serializable
    data class FloatValue(val value: Double) : AnnotationValue()
    
    /** 布尔值 */
    @Serializable
    data class BooleanValue(val value: Boolean) : AnnotationValue()
    
    /** 数组值 */
    @Serializable
    data class ArrayValue(val values: List<AnnotationValue>) : AnnotationValue()
    
    /** 对象值 */
    @Serializable
    data class ObjectValue(val properties: Map<String, AnnotationValue>) : AnnotationValue()
    
    /** 类型值 */
    @Serializable
    data class TypeValue(val typeName: String) : AnnotationValue()
    
    /** 枚举值 */
    @Serializable
    data class EnumValue(val enumType: String, val value: String) : AnnotationValue()
    
    /**
     * 转换为字符串
     */
    fun asString(): String? = when (this) {
        is StringValue -> value
        else -> null
    }
    
    /**
     * 转换为整数
     */
    fun asInt(): Long? = when (this) {
        is IntValue -> value
        else -> null
    }
    
    /**
     * 转换为浮点数
     */
    fun asFloat(): Double? = when (this) {
        is FloatValue -> value
        else -> null
    }
    
    /**
     * 转换为布尔值
     */
    fun asBoolean(): Boolean? = when (this) {
        is BooleanValue -> value
        else -> null
    }
    
    /**
     * 转换为数组
     */
    fun asArray(): List<AnnotationValue>? = when (this) {
        is ArrayValue -> values
        else -> null
    }
    
    /**
     * 转换为对象
     */
    fun asObject(): Map<String, AnnotationValue>? = when (this) {
        is ObjectValue -> properties
        else -> null
    }
}

/**
 * 注解目标枚举
 */
@Serializable
enum class AnnotationTarget {
    /** 默认目标 */
    DEFAULT,
    /** 类型 */
    TYPE,
    /** 函数 */
    FUNCTION,
    /** 参数 */
    PARAMETER,
    /** 属性 */
    PROPERTY,
    /** 变量 */
    VARIABLE,
    /** 表达式 */
    EXPRESSION,
    /** 语句 */
    STATEMENT,
    /** 文件 */
    FILE,
    /** 模块 */
    MODULE
}

/**
 * 注解保留策略枚举
 */
@Serializable
enum class AnnotationRetention {
    /** 仅在源码中保留 */
    SOURCE,
    /** 编译时保留 */
    COMPILE_TIME,
    /** 运行时保留 */
    RUNTIME
}

/**
 * 预定义注解
 */
object BuiltinAnnotations {
    /** 废弃注解 */
    const val DEPRECATED = "Deprecated"
    
    /** 实验性注解 */
    const val EXPERIMENTAL = "Experimental"
    
    /** 内联注解 */
    const val INLINE = "Inline"
    
    /** 纯函数注解 */
    const val PURE = "Pure"
    
    /** 不为空注解 */
    const val NOT_NULL = "NotNull"
    
    /** 可为空注解 */
    const val NULLABLE = "Nullable"
    
    /** 线程安全注解 */
    const val THREAD_SAFE = "ThreadSafe"
    
    /** 不可变注解 */
    const val IMMUTABLE = "Immutable"
    
    /** 序列化注解 */
    const val SERIALIZABLE = "Serializable"
    
    /** JSON 序列化名称注解 */
    const val JSON_NAME = "JsonName"
    
    /** 忽略序列化注解 */
    const val JSON_IGNORE = "JsonIgnore"
    
    /** 测试注解 */
    const val TEST = "Test"
    
    /** 基准测试注解 */
    const val BENCHMARK = "Benchmark"
    
    /** 文档注解 */
    const val DOC = "Doc"
    
    /** 示例注解 */
    const val EXAMPLE = "Example"
    
    /** 作者注解 */
    const val AUTHOR = "Author"
    
    /** 版本注解 */
    const val VERSION = "Version"
    
    /** 许可证注解 */
    const val LICENSE = "License"
}

/**
 * 注解处理器接口
 */
interface AnnotationProcessor {
    /** 支持的注解名称 */
    val supportedAnnotations: Set<String>
    
    /** 处理注解 */
    fun process(annotation: Annotation, context: AnnotationProcessingContext): AnnotationProcessingResult
}

/**
 * 注解处理上下文
 */
@Serializable
data class AnnotationProcessingContext(
    /** 当前处理的文件 */
    val currentFile: String,
    /** 全局上下文 */
    val globalContext: Map<String, @Contextual Any> = emptyMap(),
    /** 编译选项 */
    val compilerOptions: Map<String, @Contextual Any> = emptyMap()
)

/**
 * 注解处理结果
 */
@Serializable
sealed class AnnotationProcessingResult {
    /** 成功 */
    @Serializable
    object Success : AnnotationProcessingResult()
    
    /** 警告 */
    @Serializable
    data class Warning(val message: String, val location: SourceLocation) : AnnotationProcessingResult()
    
    /** 错误 */
    @Serializable
    data class Error(val message: String, val location: SourceLocation) : AnnotationProcessingResult()
    
    /** 生成代码 */
    @Serializable
    data class GeneratedCode(val code: String, val targetFile: String) : AnnotationProcessingResult()
}

/**
 * Any 类型的序列化器
 */
object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")
    
    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Int -> encoder.encodeInt(value)
            is Long -> encoder.encodeLong(value)
            is Float -> encoder.encodeFloat(value)
            is Double -> encoder.encodeDouble(value)
            is Boolean -> encoder.encodeBoolean(value)
            else -> encoder.encodeString(value.toString())
        }
    }
    
    override fun deserialize(decoder: Decoder): Any {
        return when (val element = (decoder as JsonDecoder).decodeJsonElement()) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" || element.content == "false" -> element.boolean
                    element.content.contains('.') -> element.double
                    else -> element.long
                }
            }
            else -> element.toString()
        }
    }
}
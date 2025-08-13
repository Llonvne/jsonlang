package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.annotations.AnnotationValue
import cn.llonvne.jsonlang.core.types.Type
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

/**
 * AST 序列化和反序列化支持
 * 支持将 AST 节点序列化为 JSON 格式，以及从 JSON 反序列化为 AST 节点
 */
object ASTSerialization {
    
    /**
     * JSON 序列化器配置
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "__type"
        serializersModule = SerializersModule {
            polymorphic(ASTNode::class) {
                subclass(Program::class)
                subclass(Metadata::class)
                subclass(ImportDeclaration::class)
                subclass(ExportDeclaration::class)
                subclass(TypeDeclaration::class)
                subclass(ConstantDeclaration::class)
                subclass(FunctionDeclaration::class)
                subclass(MacroDeclaration::class)
                subclass(DSLDeclaration::class)
                subclass(VariableDeclaration::class)
                subclass(ExpressionStatement::class)
                subclass(ConditionalStatement::class)
                subclass(LoopStatement.ForLoop::class)
                subclass(LoopStatement.WhileLoop::class)
                subclass(JumpStatement.ReturnStatement::class)
                subclass(JumpStatement.BreakStatement::class)
                subclass(JumpStatement.ContinueStatement::class)
                subclass(TryCatchStatement::class)
                subclass(BlockStatement::class)
                subclass(MatchStatement::class)
                subclass(LiteralExpression.StringLiteral::class)
                subclass(LiteralExpression.IntLiteral::class)
                subclass(LiteralExpression.FloatLiteral::class)
                subclass(LiteralExpression.BooleanLiteral::class)
                subclass(LiteralExpression.NullLiteral::class)
                subclass(IdentifierExpression::class)
                subclass(BinaryExpression::class)
                subclass(UnaryExpression::class)
                subclass(CallExpression::class)
                subclass(MemberExpression::class)
                subclass(ArrayExpression::class)
                subclass(ObjectExpression::class)
                subclass(LambdaExpression::class)
                subclass(ConditionalExpression::class)
                subclass(CastExpression::class)
                subclass(TypeCheckExpression::class)
            }
        }
    }
    
    /**
     * 将 AST 节点序列化为 JSON 字符串
     */
    fun serializeToJson(node: ASTNode): String {
        return json.encodeToString(node)
    }
    
    /**
     * 从 JSON 字符串反序列化为 AST 节点
     */
    fun deserializeFromJson(jsonString: String): ASTNode {
        return json.decodeFromString(jsonString)
    }
    
    /**
     * 将 AST 节点序列化为 JsonElement
     */
    fun serializeToJsonElement(node: ASTNode): JsonElement {
        return json.encodeToJsonElement(node)
    }
    
    /**
     * 从 JsonElement 反序列化为 AST 节点
     */
    fun deserializeFromJsonElement(element: JsonElement): ASTNode {
        return json.decodeFromJsonElement(element)
    }
    
    /**
     * 将程序节点序列化为完整的 JSONLang 文件格式
     */
    fun serializeProgramToJsonLang(program: Program): String {
        val jsonObject = buildJsonObject {
            // 元数据
            put("metadata", serializeMetadata(program.metadata))
            
            // 导入
            if (program.imports.isNotEmpty()) {
                put("imports", serializeImports(program.imports))
            }
            
            // 导出
            if (program.exports.isNotEmpty()) {
                put("exports", serializeExports(program.exports))
            }
            
            // 类型定义
            if (program.types.isNotEmpty()) {
                put("types", serializeTypes(program.types))
            }
            
            // 常量定义
            if (program.constants.isNotEmpty()) {
                put("constants", serializeConstants(program.constants))
            }
            
            // 函数定义
            if (program.functions.isNotEmpty()) {
                put("functions", serializeFunctions(program.functions))
            }
            
            // 宏定义
            if (program.macros.isNotEmpty()) {
                put("macros", serializeMacros(program.macros))
            }
            
            // DSL 定义
            if (program.dsls.isNotEmpty()) {
                put("dsls", serializeDSLs(program.dsls))
            }
        }
        
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), jsonObject)
    }
    
    /**
     * 从 JSONLang 文件格式反序列化为程序节点
     */
    fun deserializeProgramFromJsonLang(jsonString: String): Program {
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        
        val metadata = deserializeMetadata(jsonObject["metadata"]?.jsonObject 
            ?: throw IllegalArgumentException("缺少必需的 metadata 字段"))
        
        val imports = jsonObject["imports"]?.jsonObject?.let { deserializeImports(it) } ?: emptyList()
        val exports = jsonObject["exports"]?.jsonObject?.let { deserializeExports(it) } ?: emptyList()
        val types = jsonObject["types"]?.jsonObject?.let { deserializeTypes(it) } ?: emptyList()
        val constants = jsonObject["constants"]?.jsonObject?.let { deserializeConstants(it) } ?: emptyList()
        val functions = jsonObject["functions"]?.jsonObject?.let { deserializeFunctions(it) } ?: emptyList()
        val macros = jsonObject["macros"]?.jsonObject?.let { deserializeMacros(it) } ?: emptyList()
        val dsls = jsonObject["dsls"]?.jsonObject?.let { deserializeDSLs(it) } ?: emptyList()
        
        return Program(
            metadata = metadata,
            imports = imports,
            exports = exports,
            types = types,
            constants = constants,
            functions = functions,
            macros = macros,
            dsls = dsls,
            location = SourceLocation.unknown(),
            annotations = emptyList()
        )
    }
    
    // 私有辅助方法
    private fun serializeMetadata(metadata: Metadata): JsonObject {
        return buildJsonObject {
            put("name", metadata.name)
            put("version", metadata.version)
            metadata.description?.let { put("description", it) }
            metadata.author?.let { put("author", it) }
            metadata.license?.let { put("license", it) }
            if (metadata.targets.isNotEmpty()) {
                put("targets", JsonArray(metadata.targets.map { JsonPrimitive(it) }))
            }
            put("encoding", metadata.encoding)
        }
    }
    
    private fun deserializeMetadata(jsonObject: JsonObject): Metadata {
        return Metadata(
            name = jsonObject["name"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("缺少 name 字段"),
            version = jsonObject["version"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("缺少 version 字段"),
            description = jsonObject["description"]?.jsonPrimitive?.content,
            author = jsonObject["author"]?.jsonPrimitive?.content,
            license = jsonObject["license"]?.jsonPrimitive?.content,
            targets = jsonObject["targets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            encoding = jsonObject["encoding"]?.jsonPrimitive?.content ?: "utf-8",
            location = SourceLocation.unknown()
        )
    }
    
    private fun serializeImports(imports: List<ImportDeclaration>): JsonObject {
        return buildJsonObject {
            imports.forEach { import ->
                put(import.name, buildJsonObject {
                    put("source", import.source)
                    import.version?.let { put("version", it) }
                    if (import.items.isNotEmpty()) {
                        put("items", JsonArray(import.items.map { JsonPrimitive(it) }))
                    }
                    if (import.alias.isNotEmpty()) {
                        put("alias", buildJsonObject {
                            import.alias.forEach { (key, value) ->
                                put(key, value)
                            }
                        })
                    }
                    import.condition?.let { condition ->
                        put("condition", buildJsonObject {
                            condition.target?.let { put("target", it) }
                            condition.platform?.let { put("platform", it) }
                            condition.feature?.let { put("feature", it) }
                        })
                    }
                })
            }
        }
    }
    
    private fun deserializeImports(jsonObject: JsonObject): List<ImportDeclaration> {
        return jsonObject.map { (name, value) ->
            val importObj = value.jsonObject
            ImportDeclaration(
                source = importObj["source"]?.jsonPrimitive?.content ?: name,
                version = importObj["version"]?.jsonPrimitive?.content,
                items = importObj["items"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                alias = importObj["alias"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap(),
                condition = importObj["condition"]?.jsonObject?.let { conditionObj ->
                    ImportCondition(
                        target = conditionObj["target"]?.jsonPrimitive?.content,
                        platform = conditionObj["platform"]?.jsonPrimitive?.content,
                        feature = conditionObj["feature"]?.jsonPrimitive?.content
                    )
                },
                location = SourceLocation.unknown()
            )
        }
    }
    
    // 其他序列化/反序列化方法的占位符
    private fun serializeExports(exports: List<ExportDeclaration>): JsonObject = buildJsonObject {}
    private fun deserializeExports(jsonObject: JsonObject): List<ExportDeclaration> = emptyList()
    
    private fun serializeTypes(types: List<TypeDeclaration>): JsonObject = buildJsonObject {}
    private fun deserializeTypes(jsonObject: JsonObject): List<TypeDeclaration> = emptyList()
    
    private fun serializeConstants(constants: List<ConstantDeclaration>): JsonObject = buildJsonObject {}
    private fun deserializeConstants(jsonObject: JsonObject): List<ConstantDeclaration> = emptyList()
    
    private fun serializeFunctions(functions: List<FunctionDeclaration>): JsonObject {
        return buildJsonObject {
            functions.forEach { function ->
                put(function.name, buildJsonObject {
                    put("modifiers", buildJsonObject {
                        put("async", function.modifiers.async)
                        put("inline", function.modifiers.inline)
                        put("pure", function.modifiers.pure)
                        put("external", function.modifiers.external)
                    })
                    if (function.generics.isNotEmpty()) {
                        put("generics", buildJsonObject {
                            function.generics.forEach { generic ->
                                put(generic.name, buildJsonObject {
                                    if (generic.constraints.isNotEmpty()) {
                                        put("constraints", JsonArray(generic.constraints.map { JsonPrimitive(it.name) }))
                                    }
                                    generic.defaultType?.let { put("default", JsonPrimitive(it.name)) }
                                })
                            }
                        })
                    }
                    put("parameters", JsonArray(function.parameters.map { param ->
                        buildJsonObject {
                            put("name", param.name)
                            put("type", param.type.name)
                            put("optional", param.optional)
                            param.defaultValue?.let { put("default", JsonPrimitive("expression")) }
                        }
                    }))
                    put("return", buildJsonObject {
                        put("type", function.returnType.name)
                    })
                    put("body", JsonArray(function.body.map { JsonPrimitive("statement") }))
                })
            }
        }
    }
    
    private fun deserializeFunctions(jsonObject: JsonObject): List<FunctionDeclaration> {
        return jsonObject.map { (name, value) ->
            val functionObj = value.jsonObject
            FunctionDeclaration(
                name = name,
                modifiers = FunctionModifiers(), // 简化实现
                parameters = emptyList(), // 简化实现
                returnType = cn.llonvne.jsonlang.core.types.PrimitiveType.IntType, // 简化实现
                body = emptyList(), // 简化实现
                location = SourceLocation.unknown()
            )
        }
    }
    
    private fun serializeMacros(macros: List<MacroDeclaration>): JsonObject = buildJsonObject {}
    private fun deserializeMacros(jsonObject: JsonObject): List<MacroDeclaration> = emptyList()
    
    private fun serializeDSLs(dsls: List<DSLDeclaration>): JsonObject = buildJsonObject {}
    private fun deserializeDSLs(jsonObject: JsonObject): List<DSLDeclaration> = emptyList()
}


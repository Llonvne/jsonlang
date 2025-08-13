package cn.llonvne.jsonlang.core.modules

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.types.Type
import kotlinx.serialization.json.*
import kotlinx.serialization.decodeFromString

/**
 * 模块解析器
 * 负责将 JSON 格式的 JSONLang 文件解析为模块对象
 */
class ModuleParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * 解析模块文件
     */
    fun parseModule(content: String, filePath: String): Module {
        try {
            val jsonElement = json.parseToJsonElement(content)
            if (jsonElement !is JsonObject) {
                throw ModuleParseException("模块文件必须是 JSON 对象")
            }
            
            return parseModuleFromJson(jsonElement, filePath)
        } catch (e: Exception) {
            throw ModuleParseException("解析模块文件失败: ${e.message}")
        }
    }
    
    private fun parseModuleFromJson(jsonObject: JsonObject, filePath: String): Module {
        val metadata = parseMetadata(jsonObject["metadata"]?.jsonObject, filePath)
        val imports = parseImports(jsonObject["imports"]?.jsonObject)
        val exports = parseExports(jsonObject["exports"]?.jsonObject)
        val declarations = parseDeclarations(jsonObject)
        
        return Module(
            metadata = metadata,
            imports = imports,
            exports = exports,
            declarations = declarations,
            path = filePath,
            content = jsonObject.toString()
        )
    }
    
    /**
     * 解析元数据
     */
    private fun parseMetadata(metadataJson: JsonObject?, filePath: String): ModuleMetadata {
        if (metadataJson == null) {
            // 如果没有元数据，使用默认值
            val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
                .removeSuffix(".lang") // 移除 .lang 后缀
            return ModuleMetadata(
                name = fileName,
                version = "1.0.0"
            )
        }
        
        return ModuleMetadata(
            name = metadataJson["name"]?.jsonPrimitive?.content ?: "unnamed",
            version = metadataJson["version"]?.jsonPrimitive?.content ?: "1.0.0",
            description = metadataJson["description"]?.jsonPrimitive?.content,
            author = metadataJson["author"]?.jsonPrimitive?.content,
            license = metadataJson["license"]?.jsonPrimitive?.content,
            targets = metadataJson["target"]?.jsonArray?.map { 
                it.jsonPrimitive.content 
            } ?: emptyList(),
            encoding = metadataJson["encoding"]?.jsonPrimitive?.content ?: "utf-8",
            dependencies = metadataJson["dependencies"]?.jsonObject?.mapValues { 
                it.value.jsonPrimitive.content 
            } ?: emptyMap()
        )
    }
    
    /**
     * 解析导入声明
     */
    private fun parseImports(importsJson: JsonObject?): List<ImportDeclaration> {
        if (importsJson == null) return emptyList()
        
        return importsJson.map { (alias, importJson) ->
            val importObj = importJson.jsonObject
            
            ImportDeclaration(
                source = importObj["source"]?.jsonPrimitive?.content ?: alias,
                version = importObj["version"]?.jsonPrimitive?.content,
                items = importObj["items"]?.jsonArray?.map { 
                    it.jsonPrimitive.content 
                } ?: emptyList(),
                alias = importObj["alias"]?.jsonObject?.mapValues { 
                    it.value.jsonPrimitive.content 
                } ?: emptyMap(),
                condition = parseImportCondition(importObj["condition"]?.jsonObject),
                location = SourceLocation.unknown(alias)
            )
        }
    }
    
    /**
     * 解析导入条件
     */
    private fun parseImportCondition(conditionJson: JsonObject?): ImportCondition? {
        if (conditionJson == null) return null
        
        return ImportCondition(
            target = conditionJson["target"]?.jsonPrimitive?.content,
            platform = conditionJson["platform"]?.jsonPrimitive?.content,
            feature = conditionJson["feature"]?.jsonPrimitive?.content
        )
    }
    
    /**
     * 解析导出声明
     */
    private fun parseExports(exportsJson: JsonObject?): List<ExportDeclaration> {
        if (exportsJson == null) return emptyList()
        
        return exportsJson.map { (name, exportJson) ->
            val exportObj = exportJson.jsonObject
            
            ExportDeclaration(
                name = name,
                exportType = parseExportType(exportObj["type"]?.jsonPrimitive?.content),
                target = exportObj["target"]?.jsonPrimitive?.content,
                visibility = parseVisibility(exportObj["visibility"]?.jsonPrimitive?.content),
                location = SourceLocation.unknown(name)
            )
        }
    }
    
    /**
     * 解析导出类型
     */
    private fun parseExportType(typeStr: String?): ExportType {
        return when (typeStr?.lowercase()) {
            "function" -> ExportType.FUNCTION
            "type" -> ExportType.TYPE
            "constant" -> ExportType.CONSTANT
            "default" -> ExportType.DEFAULT
            "namespace" -> ExportType.NAMESPACE
            else -> ExportType.FUNCTION // 默认为函数
        }
    }
    
    /**
     * 解析可见性
     */
    private fun parseVisibility(visibilityStr: String?): Visibility {
        return when (visibilityStr?.lowercase()) {
            "public" -> Visibility.PUBLIC
            "private" -> Visibility.PRIVATE
            "internal" -> Visibility.INTERNAL
            "protected" -> Visibility.PROTECTED
            else -> Visibility.PUBLIC // 默认为公开
        }
    }
    
    /**
     * 解析声明
     */
    private fun parseDeclarations(jsonObject: JsonObject): List<Declaration> {
        val declarations = mutableListOf<Declaration>()
        
        // 解析类型声明
        jsonObject["types"]?.jsonObject?.let { typesJson ->
            declarations.addAll(parseTypeDeclarations(typesJson))
        }
        
        // 解析常量声明
        jsonObject["constants"]?.jsonObject?.let { constantsJson ->
            declarations.addAll(parseConstantDeclarations(constantsJson))
        }
        
        // 解析函数声明
        jsonObject["functions"]?.jsonObject?.let { functionsJson ->
            declarations.addAll(parseFunctionDeclarations(functionsJson))
        }
        
        // 解析宏声明
        jsonObject["macros"]?.jsonObject?.let { macrosJson ->
            declarations.addAll(parseMacroDeclarations(macrosJson))
        }
        
        // 解析 DSL 声明
        jsonObject["dsl"]?.jsonObject?.let { dslJson ->
            declarations.addAll(parseDSLDeclarations(dslJson))
        }
        
        return declarations
    }
    
    /**
     * 解析类型声明
     */
    private fun parseTypeDeclarations(typesJson: JsonObject): List<TypeDeclaration> {
        return typesJson.map { (name, typeJson) ->
            val typeObj = typeJson.jsonObject
            
            TypeDeclaration(
                name = name,
                kind = parseTypeKind(typeObj["kind"]?.jsonPrimitive?.content),
                definition = parseTypeDefinition(typeObj["definition"]),
                constraints = parseTypeConstraints(typeObj["constraints"]?.jsonArray),
                visibility = parseVisibility(typeObj["visibility"]?.jsonPrimitive?.content),
                location = SourceLocation.unknown(name)
            )
        }
    }
    
    private fun parseTypeKind(kindStr: String?): TypeKind {
        return when (kindStr?.lowercase()) {
            "struct" -> TypeKind.STRUCT
            "enum" -> TypeKind.ENUM
            "union" -> TypeKind.UNION
            "alias" -> TypeKind.ALIAS
            "generic" -> TypeKind.GENERIC
            else -> TypeKind.STRUCT
        }
    }
    
    private fun parseTypeDefinition(definitionJson: JsonElement?): TypeDefinition {
        // 这里应该根据具体的类型定义格式进行解析
        // 为了演示，返回一个简单的结构体定义
        return TypeDefinition.StructDefinition(emptyMap())
    }
    
    private fun parseTypeConstraints(constraintsJson: JsonArray?): List<TypeConstraint> {
        // 解析类型约束
        return emptyList()
    }
    
    /**
     * 解析常量声明
     */
    private fun parseConstantDeclarations(constantsJson: JsonObject): List<ConstantDeclaration> {
        return constantsJson.map { (name, constantJson) ->
            val constantObj = constantJson.jsonObject
            
            ConstantDeclaration(
                name = name,
                type = parseType(constantObj["type"]),
                value = parseExpression(constantObj["value"]),
                visibility = parseVisibility(constantObj["visibility"]?.jsonPrimitive?.content),
                location = SourceLocation.unknown(name)
            )
        }
    }
    
    /**
     * 解析函数声明
     */
    private fun parseFunctionDeclarations(functionsJson: JsonObject): List<FunctionDeclaration> {
        return functionsJson.map { (name, functionJson) ->
            val functionObj = functionJson.jsonObject
            
            FunctionDeclaration(
                name = name,
                modifiers = parseFunctionModifiers(functionObj["modifiers"]?.jsonObject),
                generics = parseGenericParameters(functionObj["generics"]?.jsonArray),
                parameters = parseParameters(functionObj["parameters"]?.jsonArray),
                returnType = parseType(functionObj["return"]),
                body = parseStatements(functionObj["body"]?.jsonArray),
                visibility = parseVisibility(functionObj["visibility"]?.jsonPrimitive?.content),
                location = SourceLocation.unknown(name)
            )
        }
    }
    
    private fun parseFunctionModifiers(modifiersJson: JsonObject?): FunctionModifiers {
        if (modifiersJson == null) return FunctionModifiers()
        
        return FunctionModifiers(
            async = modifiersJson["async"]?.jsonPrimitive?.boolean ?: false,
            inline = modifiersJson["inline"]?.jsonPrimitive?.boolean ?: false,
            pure = modifiersJson["pure"]?.jsonPrimitive?.boolean ?: false,
            external = modifiersJson["external"]?.jsonPrimitive?.boolean ?: false
        )
    }
    
    private fun parseGenericParameters(genericsJson: JsonArray?): List<GenericParameter> {
        // 解析泛型参数
        return emptyList()
    }
    
    private fun parseParameters(parametersJson: JsonArray?): List<Parameter> {
        if (parametersJson == null) return emptyList()
        
        return parametersJson.map { paramJson ->
            val paramObj = paramJson.jsonObject
            
            Parameter(
                name = paramObj["name"]?.jsonPrimitive?.content ?: "",
                type = parseType(paramObj["type"]),
                optional = paramObj["optional"]?.jsonPrimitive?.boolean ?: false,
                defaultValue = paramObj["default"]?.let { parseExpression(it) }
            )
        }
    }
    
    /**
     * 解析宏声明
     */
    private fun parseMacroDeclarations(macrosJson: JsonObject): List<MacroDeclaration> {
        return macrosJson.map { (name, macroJson) ->
            val macroObj = macroJson.jsonObject
            
            MacroDeclaration(
                name = name,
                parameters = macroObj["parameters"]?.jsonArray?.map { 
                    it.jsonPrimitive.content 
                } ?: emptyList(),
                expansion = parseMacroExpansion(macroObj["expansion"]),
                phase = parseMacroPhase(macroObj["phase"]?.jsonPrimitive?.content),
                visibility = parseVisibility(macroObj["visibility"]?.jsonPrimitive?.content),
                location = SourceLocation.unknown(name)
            )
        }
    }
    
    private fun parseMacroExpansion(expansionJson: JsonElement?): MacroExpansion {
        // 解析宏展开
        return MacroExpansion.TemplateExpansion(
            // 这里应该解析实际的 AST 节点
            createDummyASTNode()
        )
    }
    
    private fun parseMacroPhase(phaseStr: String?): MacroPhase {
        return when (phaseStr?.lowercase()) {
            "compile_time" -> MacroPhase.COMPILE_TIME
            "runtime" -> MacroPhase.RUNTIME
            else -> MacroPhase.COMPILE_TIME
        }
    }
    
    /**
     * 解析 DSL 声明
     */
    private fun parseDSLDeclarations(dslJson: JsonObject): List<DSLDeclaration> {
        return dslJson.map { (name, dslDefJson) ->
            val dslObj = dslDefJson.jsonObject
            
            DSLDeclaration(
                name = name,
                syntax = parseDSLSyntax(dslObj["syntax"]?.jsonObject),
                semantics = parseDSLSemantics(dslObj["semantics"]?.jsonObject),
                transformation = parseDSLTransformation(dslObj["transformation"]?.jsonObject),
                visibility = parseVisibility(dslObj["visibility"]?.jsonPrimitive?.content),
                location = SourceLocation.unknown(name)
            )
        }
    }
    
    private fun parseDSLSyntax(syntaxJson: JsonObject?): DSLSyntax {
        // 解析 DSL 语法
        return DSLSyntax(emptyMap())
    }
    
    private fun parseDSLSemantics(semanticsJson: JsonObject?): DSLSemantics {
        // 解析 DSL 语义
        return DSLSemantics(emptyMap())
    }
    
    private fun parseDSLTransformation(transformationJson: JsonObject?): DSLTransformation {
        // 解析 DSL 转换
        return DSLTransformation("", "")
    }
    
    /**
     * 解析类型
     */
    private fun parseType(typeJson: JsonElement?): cn.llonvne.jsonlang.core.types.Type {
        // 这里应该解析具体的类型定义
        // 为了演示，返回一个基本类型
        return cn.llonvne.jsonlang.core.types.PrimitiveType.AnyType
    }
    
    /**
     * 解析表达式
     */
    private fun parseExpression(exprJson: JsonElement?): Expression {
        // 这里应该解析具体的表达式
        // 为了演示，返回一个字符串字面量
        return LiteralExpression.StringLiteral(
            value = exprJson?.toString() ?: "",
            location = SourceLocation.unknown()
        )
    }
    
    /**
     * 解析语句列表
     */
    private fun parseStatements(statementsJson: JsonArray?): List<Statement> {
        // 这里应该解析具体的语句
        return emptyList()
    }
    
    /**
     * 创建虚拟 AST 节点（用于演示）
     */
    private fun createDummyASTNode(): ASTNode {
        return LiteralExpression.StringLiteral(
            value = "dummy",
            location = SourceLocation.unknown()
        )
    }
}
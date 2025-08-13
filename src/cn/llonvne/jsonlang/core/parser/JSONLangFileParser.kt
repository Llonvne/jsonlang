package cn.llonvne.jsonlang.core.parser

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlinx.serialization.json.*
import java.io.File

/**
 * JSONLang 文件解析器
 * 专门用于解析 .jsonlang 文件格式
 */
class JSONLangFileParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowStructuredMapKeys = true
    }
    
    /**
     * 解析 JSONLang 文件
     */
    fun parseFile(filePath: String): Program {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }
        
        val content = file.readText()
        return parseContent(content, filePath)
    }
    
    /**
     * 解析 JSONLang 内容
     */
    fun parseContent(content: String, fileName: String = "unknown.lang.json"): Program {
        try {
            val jsonElement = json.parseToJsonElement(content)
            if (jsonElement !is JsonObject) {
                throw IllegalArgumentException("JSONLang file must be a JSON object")
            }
            
            return parseProgram(jsonElement, fileName)
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse JSONLang file $fileName: ${e.message}", e)
        }
    }
    
    private fun parseProgram(jsonObject: JsonObject, fileName: String): Program {
        val location = SourceLocation(fileName, 1, 1, 1, 1, 0, 0)
        
        // 解析元数据
        val metadata = parseMetadata(jsonObject["metadata"]?.jsonObject, fileName, location)
        
        // 解析各个部分
        val imports = parseImports(jsonObject["imports"]?.jsonObject, location)
        val exports = parseExports(jsonObject["exports"]?.jsonObject, location)
        val types = parseTypes(jsonObject["types"]?.jsonObject, location)
        val constants = parseConstants(jsonObject["constants"]?.jsonObject, location)
        val functions = parseFunctions(jsonObject["functions"]?.jsonObject, location)
        val macros = parseMacros(jsonObject["macros"]?.jsonObject, location)
        val dsls = parseDSLs(jsonObject["dsl"]?.jsonObject, location)
        
        return Program(
            metadata = metadata,
            imports = imports,
            exports = exports,
            types = types,
            constants = constants,
            functions = functions,
            macros = macros,
            dsls = dsls,
            location = location
        )
    }
    
    private fun parseMetadata(metadataJson: JsonObject?, fileName: String, location: SourceLocation): Metadata {
        val name = metadataJson?.get("name")?.jsonPrimitive?.content 
            ?: fileName.substringAfterLast("/").substringBeforeLast(".").removeSuffix(".lang")
        val version = metadataJson?.get("version")?.jsonPrimitive?.content ?: "1.0.0"
        val description = metadataJson?.get("description")?.jsonPrimitive?.content
        val author = metadataJson?.get("author")?.jsonPrimitive?.content
        val license = metadataJson?.get("license")?.jsonPrimitive?.content
        val targets = metadataJson?.get("targets")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        val encoding = metadataJson?.get("encoding")?.jsonPrimitive?.content ?: "utf-8"
        
        return Metadata(
            name = name,
            version = version,
            description = description,
            author = author,
            license = license,
            targets = targets,
            encoding = encoding,
            location = location
        )
    }
    
    private fun parseImports(importsJson: JsonObject?, location: SourceLocation): List<ImportDeclaration> {
        if (importsJson == null) return emptyList()
        
        return importsJson.map { (alias, importJson) ->
            val importObj = importJson.jsonObject
            val source = importObj["source"]?.jsonPrimitive?.content 
                ?: importObj["file"]?.jsonPrimitive?.content 
                ?: alias
            val version = importObj["version"]?.jsonPrimitive?.content
            val items = importObj["items"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("*")
            
            ImportDeclaration(
                source = source,
                version = version,
                items = items,
                alias = emptyMap(),
                location = location
            )
        }
    }
    
    private fun parseExports(exportsJson: JsonObject?, location: SourceLocation): List<ExportDeclaration> {
        if (exportsJson == null) return emptyList()
        
        return exportsJson.map { (name, exportJson) ->
            val exportObj = exportJson.jsonObject
            val exportType = when (exportObj["type"]?.jsonPrimitive?.content) {
                "function" -> ExportType.FUNCTION
                "type" -> ExportType.TYPE
                "constant" -> ExportType.CONSTANT
                "default" -> ExportType.DEFAULT
                "namespace" -> ExportType.NAMESPACE
                else -> ExportType.FUNCTION
            }
            
            ExportDeclaration(
                name = name,
                exportType = exportType,
                target = exportObj["target"]?.jsonPrimitive?.content,
                visibility = parseVisibility(exportObj["visibility"]?.jsonPrimitive?.content),
                location = location
            )
        }
    }
    
    private fun parseTypes(typesJson: JsonObject?, location: SourceLocation): List<TypeDeclaration> {
        if (typesJson == null) return emptyList()
        
        return typesJson.map { (name, typeJson) ->
            val typeObj = typeJson.jsonObject
            val kind = parseTypeKind(typeObj["kind"]?.jsonPrimitive?.content)
            val definition = parseTypeDefinition(typeObj, kind, location)
            
            TypeDeclaration(
                name = name,
                kind = kind,
                definition = definition,
                location = location
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
    
    private fun parseTypeDefinition(typeObj: JsonObject, kind: TypeKind, location: SourceLocation): TypeDefinition {
        return when (kind) {
            TypeKind.STRUCT -> parseStructDefinition(typeObj["fields"]?.jsonObject, location)
            TypeKind.ENUM -> parseEnumDefinition(typeObj["variants"]?.jsonObject, location)
            TypeKind.UNION -> parseUnionDefinition(typeObj["types"]?.jsonArray, location)
            TypeKind.ALIAS -> parseAliasDefinition(typeObj["definition"], location)
            TypeKind.GENERIC -> parseGenericDefinition(typeObj, location)
        }
    }
    
    private fun parseStructDefinition(fieldsJson: JsonObject?, location: SourceLocation): TypeDefinition.StructDefinition {
        if (fieldsJson == null) return TypeDefinition.StructDefinition(emptyMap())
        
        val fields = fieldsJson.mapValues { (_, fieldJson) ->
            val fieldObj = fieldJson.jsonObject
            val type = parseTypeReference(fieldObj["type"], location)
            val optional = fieldObj["optional"]?.jsonPrimitive?.boolean ?: false
            val defaultValue = fieldObj["default"]?.let { parseExpression(it, location) }
            
            FieldDefinition(type, optional, defaultValue)
        }
        
        return TypeDefinition.StructDefinition(fields)
    }
    
    private fun parseEnumDefinition(variantsJson: JsonObject?, location: SourceLocation): TypeDefinition.EnumDefinition {
        if (variantsJson == null) return TypeDefinition.EnumDefinition(emptyMap())
        
        val variants = variantsJson.mapValues { (_, variantJson) ->
            when {
                variantJson is JsonNull -> null
                variantJson is JsonObject -> parseTypeReference(variantJson["type"], location)
                else -> null
            }
        }
        
        return TypeDefinition.EnumDefinition(variants)
    }
    
    private fun parseUnionDefinition(typesJson: JsonArray?, location: SourceLocation): TypeDefinition.UnionDefinition {
        if (typesJson == null) return TypeDefinition.UnionDefinition(emptyList())
        
        val types = typesJson.map { parseTypeReference(it, location) }
        return TypeDefinition.UnionDefinition(types)
    }
    
    private fun parseAliasDefinition(definitionJson: JsonElement?, location: SourceLocation): TypeDefinition.AliasDefinition {
        val aliasedType = parseTypeReference(definitionJson, location)
        return TypeDefinition.AliasDefinition(aliasedType)
    }
    
    private fun parseGenericDefinition(typeObj: JsonObject, location: SourceLocation): TypeDefinition.GenericDefinition {
        val parameters = parseGenericParameters(typeObj["parameters"]?.jsonArray, location)
        val baseType = parseTypeReference(typeObj["definition"] ?: typeObj["baseType"], location)
        
        return TypeDefinition.GenericDefinition(parameters, baseType)
    }
    
    private fun parseGenericParameters(parametersJson: JsonArray?, location: SourceLocation): List<GenericParameter> {
        if (parametersJson == null) return emptyList()
        
        return parametersJson.map { paramJson ->
            when (paramJson) {
                is JsonPrimitive -> GenericParameter(paramJson.content)
                is JsonObject -> {
                    val name = paramJson["name"]?.jsonPrimitive?.content ?: "T"
                    val constraints = paramJson["constraints"]?.jsonArray?.map { 
                        parseTypeReference(it, location) 
                    } ?: emptyList()
                    GenericParameter(name, constraints)
                }
                else -> GenericParameter("T")
            }
        }
    }
    
    private fun parseConstants(constantsJson: JsonObject?, location: SourceLocation): List<ConstantDeclaration> {
        if (constantsJson == null) return emptyList()
        
        return constantsJson.map { (name, constantJson) ->
            val constantObj = constantJson.jsonObject
            val type = parseTypeReference(constantObj["type"], location)
            val value = parseExpression(constantObj["value"] ?: JsonPrimitive(""), location)
            val visibility = parseVisibility(constantObj["visibility"]?.jsonPrimitive?.content)
            
            ConstantDeclaration(
                name = name,
                type = type,
                value = value,
                visibility = visibility,
                location = location
            )
        }
    }
    
    private fun parseFunctions(functionsJson: JsonObject?, location: SourceLocation): List<FunctionDeclaration> {
        if (functionsJson == null) return emptyList()
        
        return functionsJson.map { (name, functionJson) ->
            val functionObj = functionJson.jsonObject
            val modifiers = parseFunctionModifiers(functionObj["modifiers"]?.jsonObject)
            val generics = parseGenericParameters(functionObj["generics"]?.jsonArray, location)
            val parameters = parseParameters(functionObj["args"]?.jsonArray ?: functionObj["parameters"]?.jsonArray, location)
            val returnType = parseTypeReference(functionObj["return"] ?: functionObj["returnType"], location)
            val body = parseStatements(functionObj["body"]?.jsonArray, location)
            val visibility = parseVisibility(functionObj["visibility"]?.jsonPrimitive?.content)
            
            FunctionDeclaration(
                name = name,
                modifiers = modifiers,
                generics = generics,
                parameters = parameters,
                returnType = returnType,
                body = body,
                visibility = visibility,
                location = location
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
    
    private fun parseParameters(parametersJson: JsonArray?, location: SourceLocation): List<Parameter> {
        if (parametersJson == null) return emptyList()
        
        return parametersJson.map { paramJson ->
            val paramObj = paramJson.jsonObject
            val name = paramObj["name"]?.jsonPrimitive?.content ?: "param"
            val type = parseTypeReference(paramObj["type"], location)
            val optional = paramObj["optional"]?.jsonPrimitive?.boolean ?: false
            val defaultValue = paramObj["default"]?.let { parseExpression(it, location) }
            
            Parameter(name, type, optional, defaultValue)
        }
    }
    
    private fun parseMacros(macrosJson: JsonObject?, location: SourceLocation): List<MacroDeclaration> {
        if (macrosJson == null) return emptyList()
        
        return macrosJson.map { (name, macroJson) ->
            val macroObj = macroJson.jsonObject
            val parameters = macroObj["parameters"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val expansion = MacroExpansion.TemplateExpansion(
                LiteralExpression.StringLiteral("macro_expansion", location)
            )
            val phase = MacroPhase.COMPILE_TIME
            val visibility = parseVisibility(macroObj["visibility"]?.jsonPrimitive?.content)
            
            MacroDeclaration(
                name = name,
                parameters = parameters,
                expansion = expansion,
                phase = phase,
                visibility = visibility,
                location = location
            )
        }
    }
    
    private fun parseDSLs(dslJson: JsonObject?, location: SourceLocation): List<DSLDeclaration> {
        if (dslJson == null) return emptyList()
        
        return dslJson.map { (name, dslDefJson) ->
            val dslObj = dslDefJson.jsonObject
            val syntax = DSLSyntax(emptyMap())
            val semantics = DSLSemantics(emptyMap())
            val transformation = DSLTransformation("", "")
            val visibility = parseVisibility(dslObj["visibility"]?.jsonPrimitive?.content)
            
            DSLDeclaration(
                name = name,
                syntax = syntax,
                semantics = semantics,
                transformation = transformation,
                visibility = visibility,
                location = location
            )
        }
    }
    
    private fun parseStatements(statementsJson: JsonArray?, location: SourceLocation): List<Statement> {
        if (statementsJson == null) return emptyList()
        
        return statementsJson.map { statementJson ->
            parseStatement(statementJson, location)
        }
    }
    
    private fun parseStatement(statementJson: JsonElement, location: SourceLocation): Statement {
        return when (statementJson) {
            is JsonObject -> {
                val type = statementJson["type"]?.jsonPrimitive?.content
                when (type) {
                    "return" -> {
                        val value = statementJson["value"]?.let { parseExpression(it, location) }
                        JumpStatement.ReturnStatement(value, location)
                    }
                    "variable" -> {
                        val name = statementJson["name"]?.jsonPrimitive?.content ?: "var"
                        val varType = statementJson["type"]?.let { parseTypeReference(it, location) }
                        val value = statementJson["value"]?.let { parseExpression(it, location) }
                        val mutable = statementJson["mutable"]?.jsonPrimitive?.boolean ?: false
                        VariableDeclaration(name, varType, value, mutable, location)
                    }
                    "if" -> {
                        val condition = parseExpression(statementJson["condition"]!!, location)
                        val thenBranch = parseStatement(statementJson["then"]!!, location)
                        val elseBranch = statementJson["else"]?.let { parseStatement(it, location) }
                        ConditionalStatement(condition, thenBranch, elseBranch, location)
                    }
                    "for" -> {
                        val iterator = statementJson["iterator"]?.jsonPrimitive?.content ?: "item"
                        val iterable = parseExpression(statementJson["iterable"]!!, location)
                        val body = parseStatement(statementJson["body"]!!, location)
                        LoopStatement.ForLoop(iterator, iterable, body, location)
                    }
                    "while" -> {
                        val condition = parseExpression(statementJson["condition"]!!, location)
                        val body = parseStatement(statementJson["body"]!!, location)
                        LoopStatement.WhileLoop(condition, body, location)
                    }
                    "block" -> {
                        val statements = statementJson["statements"]?.jsonArray?.map { 
                            parseStatement(it, location) 
                        } ?: emptyList()
                        BlockStatement(statements, location)
                    }
                    "expression" -> {
                        val expression = parseExpression(statementJson["value"]!!, location)
                        ExpressionStatement(expression, location)
                    }
                    "match" -> {
                        val expression = parseExpression(statementJson["expression"]!!, location)
                        val cases = statementJson["cases"]?.jsonArray?.map { caseJson ->
                            val caseObj = caseJson.jsonObject
                            val pattern = parsePattern(caseObj["pattern"]!!, location)
                            val body = parseStatement(caseObj["body"]!!, location)
                            MatchCase(pattern, null, body, location)
                        } ?: emptyList()
                        val defaultCase = statementJson["default"]?.let { parseStatement(it, location) }
                        MatchStatement(expression, cases, defaultCase, location)
                    }
                    "try" -> {
                        val tryBlock = parseStatement(statementJson["tryBlock"]!!, location) as BlockStatement
                        val catchClauses = statementJson["catchClauses"]?.jsonArray?.map { catchJson ->
                            val catchObj = catchJson.jsonObject
                            val exceptionType = parseTypeReference(catchObj["exceptionType"], location)
                            val variable = catchObj["variable"]?.jsonPrimitive?.content
                            val handler = parseStatement(catchObj["body"]!!, location) as BlockStatement
                            CatchClause(exceptionType, variable, handler, location)
                        } ?: emptyList()
                        TryCatchStatement(tryBlock, catchClauses, location = location)
                    }
                    "assign" -> {
                        val target = parseExpression(statementJson["target"]!!, location)
                        val value = parseExpression(statementJson["value"]!!, location)
                        ExpressionStatement(
                            BinaryExpression(target, BinaryOperator.ASSIGN, value, location),
                            location
                        )
                    }
                    else -> {
                        // 默认作为表达式语句处理
                        val expression = parseExpression(statementJson, location)
                        ExpressionStatement(expression, location)
                    }
                }
            }
            else -> {
                val expression = parseExpression(statementJson, location)
                ExpressionStatement(expression, location)
            }
        }
    }
    
    private fun parsePattern(patternJson: JsonElement, location: SourceLocation): Pattern {
        return when (patternJson) {
            is JsonObject -> {
                val type = patternJson["type"]?.jsonPrimitive?.content
                when (type) {
                    "literal" -> Pattern.LiteralPattern(patternJson["value"]?.jsonPrimitive?.content ?: "")
                    "variable" -> Pattern.VariablePattern(patternJson["name"]?.jsonPrimitive?.content ?: "var")
                    "constructor" -> {
                        val constructor = patternJson["constructor"]?.jsonPrimitive?.content ?: ""
                        val args = patternJson["args"]?.jsonArray?.map { parsePattern(it, location) } ?: emptyList()
                        Pattern.ConstructorPattern(constructor, args)
                    }
                    "wildcard" -> Pattern.WildcardPattern()
                    else -> Pattern.WildcardPattern()
                }
            }
            is JsonPrimitive -> Pattern.LiteralPattern(patternJson.content)
            else -> Pattern.WildcardPattern()
        }
    }
    
    private fun parseExpression(exprJson: JsonElement, location: SourceLocation): Expression {
        return when (exprJson) {
            is JsonPrimitive -> {
                when {
                    exprJson.isString -> LiteralExpression.StringLiteral(exprJson.content, location)
                    exprJson.booleanOrNull != null -> LiteralExpression.BooleanLiteral(exprJson.boolean, location)
                    exprJson.longOrNull != null -> LiteralExpression.IntLiteral(exprJson.long, location)
                    exprJson.doubleOrNull != null -> LiteralExpression.FloatLiteral(exprJson.double, location)
                    else -> LiteralExpression.StringLiteral(exprJson.content, location)
                }
            }
            is JsonNull -> LiteralExpression.NullLiteral(location)
            is JsonArray -> {
                val elements = exprJson.map { parseExpression(it, location) }
                ArrayExpression(elements, location)
            }
            is JsonObject -> {
                val type = exprJson["type"]?.jsonPrimitive?.content
                when (type) {
                    "literal" -> {
                        val value = exprJson["value"]
                        parseExpression(value!!, location)
                    }
                    "identifier" -> {
                        val name = exprJson["name"]?.jsonPrimitive?.content ?: "unknown"
                        IdentifierExpression(name, location)
                    }
                    "binary" -> {
                        val left = parseExpression(exprJson["left"]!!, location)
                        val operator = parseBinaryOperator(exprJson["operator"]?.jsonPrimitive?.content)
                        val right = parseExpression(exprJson["right"]!!, location)
                        BinaryExpression(left, operator, right, location)
                    }
                    "unary" -> {
                        val operator = parseUnaryOperator(exprJson["operator"]?.jsonPrimitive?.content)
                        val operand = parseExpression(exprJson["operand"]!!, location)
                        UnaryExpression(operator, operand, location)
                    }
                    "call" -> {
                        val callValue = exprJson["value"]?.jsonObject
                        if (callValue != null) {
                            val functionName = callValue["function"]?.jsonPrimitive?.content ?: "unknown"
                            val function = IdentifierExpression(functionName, location)
                            val args = callValue["args"]?.jsonArray?.map { argJson ->
                                Argument(value = parseExpression(argJson, location))
                            } ?: emptyList()
                            CallExpression(function, args, location)
                        } else {
                            val function = parseExpression(exprJson["function"]!!, location)
                            val args = exprJson["arguments"]?.jsonArray?.map { argJson ->
                                Argument(value = parseExpression(argJson, location))
                            } ?: emptyList()
                            CallExpression(function, args, location)
                        }
                    }
                    "member" -> {
                        val target = parseExpression(exprJson["target"]!!, location)
                        val property = exprJson["property"]?.jsonPrimitive?.content ?: "property"
                        val computed = exprJson["computed"]?.jsonPrimitive?.boolean ?: false
                        MemberExpression(target, property, computed, location)
                    }
                    "array" -> {
                        val elements = exprJson["elements"]?.jsonArray?.map { 
                            parseExpression(it, location) 
                        } ?: emptyList()
                        ArrayExpression(elements, location)
                    }
                    "object" -> {
                        val properties = exprJson["properties"]?.jsonArray?.map { propJson ->
                            val propObj = propJson.jsonObject
                            val key = parseExpression(propObj["key"]!!, location)
                            val value = parseExpression(propObj["value"]!!, location)
                            val computed = propObj["computed"]?.jsonPrimitive?.boolean ?: false
                            ObjectProperty(key, value, computed)
                        } ?: emptyList()
                        ObjectExpression(properties, location)
                    }
                    "lambda" -> {
                        val parameters = exprJson["parameters"]?.jsonArray?.map { paramJson ->
                            val paramObj = paramJson.jsonObject
                            val name = paramObj["name"]?.jsonPrimitive?.content ?: "param"
                            val type = parseTypeReference(paramObj["type"], location)
                            Parameter(name, type)
                        } ?: emptyList()
                        val body = parseExpression(exprJson["body"]!!, location)
                        LambdaExpression(parameters, body, location)
                    }
                    "conditional" -> {
                        val condition = parseExpression(exprJson["condition"]!!, location)
                        val thenExpr = parseExpression(exprJson["then"]!!, location)
                        val elseExpr = parseExpression(exprJson["else"]!!, location)
                        ConditionalExpression(condition, thenExpr, elseExpr, location)
                    }
                    else -> {
                        // 如果没有 type 字段，尝试解析为对象表达式
                        val properties = exprJson.map { (key, value) ->
                            val keyExpr = LiteralExpression.StringLiteral(key, location)
                            val valueExpr = parseExpression(value, location)
                            ObjectProperty(keyExpr, valueExpr)
                        }
                        ObjectExpression(properties, location)
                    }
                }
            }
            else -> LiteralExpression.StringLiteral("unknown", location)
        }
    }
    
    private fun parseBinaryOperator(operatorStr: String?): BinaryOperator {
        return when (operatorStr) {
            "+" -> BinaryOperator.PLUS
            "-" -> BinaryOperator.MINUS
            "*" -> BinaryOperator.MULTIPLY
            "/" -> BinaryOperator.DIVIDE
            "%" -> BinaryOperator.MODULO
            "==" -> BinaryOperator.EQUAL
            "!=" -> BinaryOperator.NOT_EQUAL
            "<" -> BinaryOperator.LESS_THAN
            "<=" -> BinaryOperator.LESS_EQUAL
            ">" -> BinaryOperator.GREATER_THAN
            ">=" -> BinaryOperator.GREATER_EQUAL
            "&&" -> BinaryOperator.LOGICAL_AND
            "||" -> BinaryOperator.LOGICAL_OR
            "&" -> BinaryOperator.BITWISE_AND
            "|" -> BinaryOperator.BITWISE_OR
            "^" -> BinaryOperator.BITWISE_XOR
            "<<" -> BinaryOperator.LEFT_SHIFT
            ">>" -> BinaryOperator.RIGHT_SHIFT
            "=" -> BinaryOperator.ASSIGN
            "+=" -> BinaryOperator.PLUS_ASSIGN
            "-=" -> BinaryOperator.MINUS_ASSIGN
            "*=" -> BinaryOperator.MULTIPLY_ASSIGN
            "/=" -> BinaryOperator.DIVIDE_ASSIGN
            else -> BinaryOperator.PLUS
        }
    }
    
    private fun parseUnaryOperator(operatorStr: String?): UnaryOperator {
        return when (operatorStr) {
            "+" -> UnaryOperator.PLUS
            "-" -> UnaryOperator.MINUS
            "!" -> UnaryOperator.LOGICAL_NOT
            "~" -> UnaryOperator.BITWISE_NOT
            "++" -> UnaryOperator.PRE_INCREMENT
            "--" -> UnaryOperator.PRE_DECREMENT
            else -> UnaryOperator.PLUS
        }
    }
    
    private fun parseTypeReference(typeJson: JsonElement?, location: SourceLocation): Type {
        if (typeJson == null) return PrimitiveType.AnyType
        
        return when (typeJson) {
            is JsonPrimitive -> {
                // 简单类型名称
                parseSimpleType(typeJson.content)
            }
            is JsonObject -> {
                val namespace = typeJson["namespace"]?.jsonPrimitive?.content
                val typeName = typeJson["type"]?.jsonPrimitive?.content ?: "Any"
                val typeArgs = typeJson["typeArgs"]?.jsonArray?.map { parseTypeReference(it, location) }
                val elementType = typeJson["elementType"]?.let { parseTypeReference(it, location) }
                val keyType = typeJson["keyType"]?.let { parseTypeReference(it, location) }
                val valueType = typeJson["valueType"]?.let { parseTypeReference(it, location) }
                val parameterTypes = typeJson["parameterTypes"]?.jsonArray?.map { parseTypeReference(it, location) }
                val returnType = typeJson["returnType"]?.let { parseTypeReference(it, location) }
                val innerType = typeJson["innerType"]?.let { parseTypeReference(it, location) }
                
                when (namespace) {
                    "jsonlang" -> parseSimpleType(typeName)
                    "generic" -> GenericType(typeName)
                    "file" -> TODO()
                    else -> {
                        // 根据类型名称和参数构造复合类型
                        when (typeName) {
                            "Array" -> {
                                val elemType = elementType ?: typeArgs?.firstOrNull() ?: PrimitiveType.AnyType
                                CompositeType.ArrayType(elemType)
                            }
                            "Object" -> {
                                val kType = keyType ?: typeArgs?.getOrNull(0) ?: PrimitiveType.StringType
                                val vType = valueType ?: typeArgs?.getOrNull(1) ?: PrimitiveType.AnyType
                                CompositeType.ObjectType(kType, vType)
                            }
                            "Function" -> {
                                val params = parameterTypes ?: typeArgs?.dropLast(1) ?: emptyList()
                                val ret = returnType ?: typeArgs?.lastOrNull() ?: PrimitiveType.UnitType
                                CompositeType.FunctionType(params, ret)
                            }
                            "Optional" -> {
                                val inner = innerType ?: typeArgs?.firstOrNull() ?: PrimitiveType.AnyType
                                CompositeType.OptionalType(inner)
                            }
                            "Future" -> {
                                val inner = innerType ?: typeArgs?.firstOrNull() ?: PrimitiveType.AnyType
                                GenericType("Future") // 简化处理
                            }
                            else -> TODO()
                        }
                    }
                }
            }
            else -> PrimitiveType.AnyType
        }
    }
    
    private fun parseSimpleType(typeName: String): Type {
        return when (typeName.lowercase()) {
            "string" -> PrimitiveType.StringType
            "int" -> PrimitiveType.IntType
            "long" -> PrimitiveType.LongType
            "float" -> PrimitiveType.FloatType
            "double" -> PrimitiveType.DoubleType
            "boolean" -> PrimitiveType.BooleanType
            "unit" -> PrimitiveType.UnitType
            "any" -> PrimitiveType.AnyType
            "nothing" -> PrimitiveType.NothingType
            "null" -> PrimitiveType.NullType
            else -> createUserDefinedType(typeName)
        }
    }
    
    private fun createUserDefinedType(name: String): Type {
        // Create a simple struct type as a placeholder for user-defined types
        return UserDefinedType.StructType(
            name = name,
            fields = emptyMap(),
            location = SourceLocation.unknown(name)
        )
    }
    
    private fun parseVisibility(visibilityStr: String?): Visibility {
        return when (visibilityStr?.lowercase()) {
            "public" -> Visibility.PUBLIC
            "private" -> Visibility.PRIVATE
            "internal" -> Visibility.INTERNAL
            "protected" -> Visibility.PROTECTED
            else -> Visibility.PUBLIC
        }
    }
}


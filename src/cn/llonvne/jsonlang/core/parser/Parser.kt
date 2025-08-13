package cn.llonvne.jsonlang.core.parser

import cn.llonvne.jsonlang.core.lexer.Lexer
import cn.llonvne.jsonlang.core.lexer.Token
import cn.llonvne.jsonlang.core.lexer.TokenType
import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.*
import cn.llonvne.jsonlang.core.annotations.Annotation

/**
 * JSONLang 递归下降语法分析器
 * 支持错误恢复和同步机制，提供中文错误消息
 */
class JSONLangParser(
    private val lexer: Lexer,
    private val errorReporter: ErrorReporter = DefaultErrorReporter()
) {
    private var current: Token = lexer.nextToken()
    private var previous: Token = current
    
    // 错误恢复状态
    private var panicMode = false
    private var synchronizing = false
    
    // 同步点 Token 类型
    private val synchronizationTokens = setOf(
        TokenType.METADATA, TokenType.IMPORTS, TokenType.EXPORTS,
        TokenType.TYPES, TokenType.CONSTANTS, TokenType.FUNCTIONS,
        TokenType.MACROS, TokenType.DSL, TokenType.LEFT_BRACE,
        TokenType.RIGHT_BRACE, TokenType.EOF
    )
    
    /**
     * 解析 JSONLang 程序
     */
    fun parseProgram(): Program? {
        try {
            return program()
        } catch (e: ParseException) {
            errorReporter.report(e.error)
            return null
        } catch (e: Exception) {
            val error = CompilerError(
                code = ErrorCodes.INTERNAL_COMPILER_BUG,
                message = "解析器内部错误: ${e.message}",
                severity = ErrorSeverity.FATAL,
                category = ErrorCategory.INTERNAL,
                location = current.location,
                cause = e
            )
            errorReporter.report(error)
            return null
        }
    }
    
    /**
     * 解析程序根节点
     */
    private fun program(): Program {
        val startLocation = current.location
        
        // JSONLang 程序必须是一个 JSON 对象
        consume(TokenType.LEFT_BRACE, "JSONLang 程序必须以 '{' 开始")
        
        var metadata: Metadata? = null
        val imports = mutableListOf<ImportDeclaration>()
        val exports = mutableListOf<ExportDeclaration>()
        val types = mutableListOf<TypeDeclaration>()
        val constants = mutableListOf<ConstantDeclaration>()
        val functions = mutableListOf<FunctionDeclaration>()
        val macros = mutableListOf<MacroDeclaration>()
        val dsls = mutableListOf<DSLDeclaration>()
        
        // 解析顶级字段
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (synchronizing) {
                synchronize()
                continue
            }
            
            try {
                val key = parseStringLiteral()
                consume(TokenType.COLON, "期望 ':' 在字段名后")
                
                when (key) {
                    "metadata", "元数据" -> {
                        if (metadata != null) {
                            reportError("重复的 metadata 字段", previous.location)
                        } else {
                            metadata = parseMetadata()
                        }
                    }
                    "imports", "导入" -> {
                        imports.addAll(parseImports())
                    }
                    "exports", "导出" -> {
                        exports.addAll(parseExports())
                    }
                    "types", "类型" -> {
                        types.addAll(parseTypes())
                    }
                    "constants", "常量" -> {
                        constants.addAll(parseConstants())
                    }
                    "functions", "函数" -> {
                        functions.addAll(parseFunctions())
                    }
                    "macros", "宏" -> {
                        macros.addAll(parseMacros())
                    }
                    "dsl", "领域语言" -> {
                        dsls.addAll(parseDSLs())
                    }
                    else -> {
                        reportError("未知的顶级字段: '$key'", previous.location)
                        skipValue() // 跳过未知字段的值
                    }
                }
                
                // 可选的逗号
                if (match(TokenType.COMMA)) {
                    // 逗号已消费
                }
            } catch (e: ParseException) {
                errorReporter.report(e.error)
                synchronize()
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束程序")
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        // 如果没有 metadata，创建默认的
        val finalMetadata = metadata ?: Metadata(
            name = "unknown",
            version = "1.0.0",
            location = location
        )
        
        return Program(
            metadata = finalMetadata,
            imports = imports,
            exports = exports,
            types = types,
            constants = constants,
            functions = functions,
            macros = macros,
            dsls = dsls,
            location = location,
            annotations = emptyList()
        )
    }
    
    /**
     * 解析元数据
     */
    private fun parseMetadata(): Metadata {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 metadata")
        
        var name: String? = null
        var version: String? = null
        var description: String? = null
        var author: String? = null
        var license: String? = null
        val targets = mutableListOf<String>()
        var encoding = "utf-8"
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "name", "名称" -> name = parseStringLiteral()
                "version", "版本" -> version = parseStringLiteral()
                "description", "描述" -> description = parseStringLiteral()
                "author", "作者" -> author = parseStringLiteral()
                "license", "许可证" -> license = parseStringLiteral()
                "target", "targets", "目标" -> {
                    if (check(TokenType.LEFT_BRACKET)) {
                        targets.addAll(parseStringArray())
                    } else {
                        targets.add(parseStringLiteral())
                    }
                }
                "encoding", "编码" -> encoding = parseStringLiteral()
                else -> {
                    reportError("未知的 metadata 字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 metadata")
        
        if (name == null) {
            reportError("metadata 必须包含 'name' 字段", startLocation)
            name = "unknown"
        }
        if (version == null) {
            reportError("metadata 必须包含 'version' 字段", startLocation)
            version = "1.0.0"
        }
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
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
    
    /**
     * 解析导入声明列表
     */
    private fun parseImports(): List<ImportDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 imports")
        val imports = mutableListOf<ImportDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val alias = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在导入别名后")
            imports.add(parseImport(alias))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 imports")
        return imports
    }
    
    /**
     * 解析单个导入声明
     */
    private fun parseImport(alias: String): ImportDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始导入定义")
        
        var source: String? = null
        var version: String? = null
        val items = mutableListOf<String>()
        val aliasMap = mutableMapOf<String, String>()
        var condition: ImportCondition? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "source", "来源" -> source = parseStringLiteral()
                "version", "版本" -> version = parseStringLiteral()
                "items", "项目" -> {
                    if (check(TokenType.LEFT_BRACKET)) {
                        items.addAll(parseStringArray())
                    } else if (check(TokenType.STRING_LITERAL) && peek().lexeme == "*") {
                        advance() // 消费 "*"
                        items.add("*")
                    }
                }
                "alias", "别名" -> {
                    consume(TokenType.LEFT_BRACE, "期望 '{' 开始别名映射")
                    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                        val from = parseStringLiteral()
                        consume(TokenType.COLON, "期望 ':' 在别名映射中")
                        val to = parseStringLiteral()
                        aliasMap[from] = to
                        
                        if (match(TokenType.COMMA)) {
                            // 逗号已消费
                        }
                    }
                    consume(TokenType.RIGHT_BRACE, "期望 '}' 结束别名映射")
                }
                "condition", "条件" -> {
                    condition = parseImportCondition()
                }
                else -> {
                    reportError("未知的导入字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束导入定义")
        
        if (source == null) {
            reportError("导入必须包含 'source' 字段", startLocation)
            source = ""
        }
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return ImportDeclaration(
            source = source,
            version = version,
            items = items,
            alias = aliasMap,
            condition = condition,
            location = location
        )
    }
    
    /**
     * 解析导入条件
     */
    private fun parseImportCondition(): ImportCondition {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始导入条件")
        
        var target: String? = null
        var platform: String? = null
        var feature: String? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "target", "目标" -> target = parseStringLiteral()
                "platform", "平台" -> platform = parseStringLiteral()
                "feature", "特性" -> feature = parseStringLiteral()
                else -> {
                    reportError("未知的导入条件字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束导入条件")
        
        return ImportCondition(
            target = target,
            platform = platform,
            feature = feature
        )
    }
    
    /**
     * 解析导出声明列表
     */
    private fun parseExports(): List<ExportDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 exports")
        val exports = mutableListOf<ExportDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在导出名称后")
            exports.add(parseExport(name))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 exports")
        return exports
    }
    
    /**
     * 解析单个导出声明
     */
    private fun parseExport(name: String): ExportDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始导出定义")
        
        var exportType: ExportType? = null
        var target: String? = null
        var visibility = Visibility.PUBLIC
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "type", "类型" -> {
                    val typeStr = parseStringLiteral()
                    exportType = when (typeStr) {
                        "function", "函数" -> ExportType.FUNCTION
                        "type", "类型" -> ExportType.TYPE
                        "constant", "常量" -> ExportType.CONSTANT
                        "default", "默认" -> ExportType.DEFAULT
                        "namespace", "命名空间" -> ExportType.NAMESPACE
                        else -> {
                            reportError("未知的导出类型: '$typeStr'", previous.location)
                            ExportType.FUNCTION
                        }
                    }
                }
                "target", "目标" -> target = parseStringLiteral()
                "visibility", "可见性" -> {
                    val visStr = parseStringLiteral()
                    visibility = when (visStr) {
                        "public", "公开" -> Visibility.PUBLIC
                        "private", "私有" -> Visibility.PRIVATE
                        "internal", "内部" -> Visibility.INTERNAL
                        "protected", "保护" -> Visibility.PROTECTED
                        else -> {
                            reportError("未知的可见性: '$visStr'", previous.location)
                            Visibility.PUBLIC
                        }
                    }
                }
                else -> {
                    reportError("未知的导出字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束导出定义")
        
        if (exportType == null) {
            reportError("导出必须包含 'type' 字段", startLocation)
            exportType = ExportType.FUNCTION
        }
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return ExportDeclaration(
            name = name,
            exportType = exportType,
            target = target,
            visibility = visibility,
            location = location
        )
    }
    
    /**
     * 解析类型声明列表
     */
    private fun parseTypes(): List<TypeDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 types")
        val types = mutableListOf<TypeDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在类型名称后")
            types.add(parseTypeDeclaration(name))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 types")
        return types
    }
    
    /**
     * 解析单个类型声明
     */
    private fun parseTypeDeclaration(name: String): TypeDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始类型定义")
        
        var kind: TypeKind? = null
        var definition: TypeDefinition? = null
        val constraints = mutableListOf<TypeConstraint>()
        var visibility = Visibility.PUBLIC
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "kind", "种类" -> {
                    val kindStr = parseStringLiteral()
                    kind = when (kindStr) {
                        "struct", "结构体" -> TypeKind.STRUCT
                        "enum", "枚举" -> TypeKind.ENUM
                        "union", "联合" -> TypeKind.UNION
                        "alias", "别名" -> TypeKind.ALIAS
                        "generic", "泛型" -> TypeKind.GENERIC
                        else -> {
                            reportError("未知的类型种类: '$kindStr'", previous.location)
                            TypeKind.STRUCT
                        }
                    }
                }
                "definition", "定义" -> {
                    definition = parseTypeDefinition(kind ?: TypeKind.STRUCT)
                }
                "constraints", "约束" -> {
                    constraints.addAll(parseTypeConstraints())
                }
                "visibility", "可见性" -> {
                    val visStr = parseStringLiteral()
                    visibility = when (visStr) {
                        "public", "公开" -> Visibility.PUBLIC
                        "private", "私有" -> Visibility.PRIVATE
                        "internal", "内部" -> Visibility.INTERNAL
                        "protected", "保护" -> Visibility.PROTECTED
                        else -> {
                            reportError("未知的可见性: '$visStr'", previous.location)
                            Visibility.PUBLIC
                        }
                    }
                }
                else -> {
                    reportError("未知的类型字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束类型定义")
        
        if (kind == null) {
            reportError("类型必须包含 'kind' 字段", startLocation)
            kind = TypeKind.STRUCT
        }
        if (definition == null) {
            reportError("类型必须包含 'definition' 字段", startLocation)
            definition = TypeDefinition.StructDefinition(emptyMap())
        }
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return TypeDeclaration(
            name = name,
            kind = kind,
            definition = definition,
            constraints = constraints,
            visibility = visibility,
            location = location
        )
    }    

    /**
     * 解析类型定义
     */
    private fun parseTypeDefinition(kind: TypeKind): TypeDefinition {
        return when (kind) {
            TypeKind.STRUCT -> parseStructDefinition()
            TypeKind.ENUM -> parseEnumDefinition()
            TypeKind.UNION -> parseUnionDefinition()
            TypeKind.ALIAS -> parseAliasDefinition()
            TypeKind.GENERIC -> parseGenericDefinition()
        }
    }
    
    /**
     * 解析结构体定义
     */
    private fun parseStructDefinition(): TypeDefinition.StructDefinition {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始结构体定义")
        val fields = mutableMapOf<String, FieldDefinition>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val fieldName = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            fields[fieldName] = parseFieldDefinition()
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束结构体定义")
        return TypeDefinition.StructDefinition(fields)
    }
    
    /**
     * 解析字段定义
     */
    private fun parseFieldDefinition(): FieldDefinition {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始字段定义")
        
        var type: Type? = null
        var optional = false
        var defaultValue: Expression? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "type", "类型" -> type = parseType()
                "optional", "可选" -> optional = parseBooleanLiteral()
                "default", "默认值" -> defaultValue = parseExpression()
                else -> {
                    reportError("未知的字段定义字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束字段定义")
        
        if (type == null) {
            reportError("字段定义必须包含 'type' 字段", current.location)
            type = PrimitiveType.AnyType
        }
        
        return FieldDefinition(
            type = type,
            optional = optional,
            defaultValue = defaultValue
        )
    }
    
    /**
     * 解析枚举定义
     */
    private fun parseEnumDefinition(): TypeDefinition.EnumDefinition {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始枚举定义")
        val variants = mutableMapOf<String, Type?>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val variantName = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在变体名后")
            
            val variantType = if (check(TokenType.NULL_LITERAL)) {
                advance() // 消费 null
                null
            } else if (check(TokenType.STRING_LITERAL)) {
                parseType()
            } else {
                // 跳过非字符串、非null的值
                skipValue()
                null
            }
            
            variants[variantName] = variantType
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束枚举定义")
        return TypeDefinition.EnumDefinition(variants)
    }
    
    /**
     * 解析联合类型定义
     */
    private fun parseUnionDefinition(): TypeDefinition.UnionDefinition {
        consume(TokenType.LEFT_BRACKET, "期望 '[' 开始联合类型定义")
        val types = mutableListOf<Type>()
        
        while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
            types.add(parseType())
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束联合类型定义")
        return TypeDefinition.UnionDefinition(types)
    }
    
    /**
     * 解析别名定义
     */
    private fun parseAliasDefinition(): TypeDefinition.AliasDefinition {
        val aliasedType = parseType()
        return TypeDefinition.AliasDefinition(aliasedType)
    }
    
    /**
     * 解析泛型定义
     */
    private fun parseGenericDefinition(): TypeDefinition.GenericDefinition {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始泛型定义")
        
        var parameters: List<GenericParameter> = emptyList()
        var baseType: Type? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "parameters", "参数" -> parameters = parseGenericParameters()
                "baseType", "基础类型" -> baseType = parseType()
                else -> {
                    reportError("未知的泛型定义字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束泛型定义")
        
        if (baseType == null) {
            reportError("泛型定义必须包含 'baseType' 字段", current.location)
            baseType = PrimitiveType.AnyType
        }
        
        return TypeDefinition.GenericDefinition(parameters, baseType)
    }
    
    /**
     * 解析泛型参数列表
     */
    private fun parseGenericParameters(): List<GenericParameter> {
        consume(TokenType.LEFT_BRACKET, "期望 '[' 开始泛型参数列表")
        val parameters = mutableListOf<GenericParameter>()
        
        while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
            parameters.add(parseGenericParameter())
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束泛型参数列表")
        return parameters
    }
    
    /**
     * 解析单个泛型参数
     */
    private fun parseGenericParameter(): GenericParameter {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始泛型参数")
        
        var name: String? = null
        val constraints = mutableListOf<Type>()
        var defaultType: Type? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "name", "名称" -> name = parseStringLiteral()
                "constraints", "约束" -> {
                    consume(TokenType.LEFT_BRACKET, "期望 '[' 开始约束列表")
                    while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
                        constraints.add(parseType())
                        if (match(TokenType.COMMA)) {
                            // 逗号已消费
                        }
                    }
                    consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束约束列表")
                }
                "default", "默认" -> defaultType = parseType()
                else -> {
                    reportError("未知的泛型参数字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束泛型参数")
        
        if (name == null) {
            reportError("泛型参数必须包含 'name' 字段", current.location)
            name = "T"
        }
        
        return GenericParameter(
            name = name,
            constraints = constraints,
            defaultType = defaultType
        )
    }
    
    /**
     * 解析类型约束列表
     */
    private fun parseTypeConstraints(): List<TypeConstraint> {
        consume(TokenType.LEFT_BRACKET, "期望 '[' 开始类型约束列表")
        val constraints = mutableListOf<TypeConstraint>()
        
        while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
            constraints.add(parseTypeConstraint())
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束类型约束列表")
        return constraints
    }
    
    /**
     * 解析单个类型约束
     */
    private fun parseTypeConstraint(): TypeConstraint {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始类型约束")
        
        var parameter: String? = null
        val bounds = mutableListOf<Type>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "parameter", "参数" -> parameter = parseStringLiteral()
                "bounds", "边界" -> {
                    consume(TokenType.LEFT_BRACKET, "期望 '[' 开始边界列表")
                    while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
                        bounds.add(parseType())
                        if (match(TokenType.COMMA)) {
                            // 逗号已消费
                        }
                    }
                    consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束边界列表")
                }
                else -> {
                    reportError("未知的类型约束字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束类型约束")
        
        if (parameter == null) {
            reportError("类型约束必须包含 'parameter' 字段", current.location)
            parameter = "T"
        }
        
        return TypeConstraint(
            parameter = parameter,
            bounds = bounds
        )
    }
    
    /**
     * 解析常量声明列表
     */
    private fun parseConstants(): List<ConstantDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 constants")
        val constants = mutableListOf<ConstantDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在常量名称后")
            constants.add(parseConstantDeclaration(name))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 constants")
        return constants
    }
    
    /**
     * 解析单个常量声明
     */
    private fun parseConstantDeclaration(name: String): ConstantDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始常量定义")
        
        var type: Type? = null
        var value: Expression? = null
        var visibility = Visibility.PUBLIC
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "type", "类型" -> type = parseType()
                "value", "值" -> value = parseExpression()
                "visibility", "可见性" -> {
                    val visStr = parseStringLiteral()
                    visibility = when (visStr) {
                        "public", "公开" -> Visibility.PUBLIC
                        "private", "私有" -> Visibility.PRIVATE
                        "internal", "内部" -> Visibility.INTERNAL
                        "protected", "保护" -> Visibility.PROTECTED
                        else -> {
                            reportError("未知的可见性: '$visStr'", previous.location)
                            Visibility.PUBLIC
                        }
                    }
                }
                else -> {
                    reportError("未知的常量字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束常量定义")
        
        if (type == null) {
            reportError("常量必须包含 'type' 字段", startLocation)
            type = PrimitiveType.AnyType
        }
        if (value == null) {
            reportError("常量必须包含 'value' 字段", startLocation)
            value = LiteralExpression.NullLiteral(current.location)
        }
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return ConstantDeclaration(
            name = name,
            type = type,
            value = value,
            visibility = visibility,
            location = location
        )
    }
    
    /**
     * 解析函数声明列表
     */
    private fun parseFunctions(): List<FunctionDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 functions")
        val functions = mutableListOf<FunctionDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在函数名称后")
            functions.add(parseFunctionDeclaration(name))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 functions")
        return functions
    }
    
    /**
     * 解析单个函数声明
     */
    private fun parseFunctionDeclaration(name: String): FunctionDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始函数定义")
        
        var modifiers = FunctionModifiers()
        val generics = mutableListOf<GenericParameter>()
        val parameters = mutableListOf<Parameter>()
        var returnType: Type = PrimitiveType.UnitType
        val body = mutableListOf<Statement>()
        var visibility = Visibility.PUBLIC
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "modifiers", "修饰符" -> modifiers = parseFunctionModifiers()
                "generics", "泛型" -> generics.addAll(parseGenericParameters())
                "parameters", "参数" -> parameters.addAll(parseParameters())
                "return", "返回" -> returnType = parseReturnType()
                "body", "函数体" -> body.addAll(parseStatements())
                "visibility", "可见性" -> {
                    val visStr = parseStringLiteral()
                    visibility = when (visStr) {
                        "public", "公开" -> Visibility.PUBLIC
                        "private", "私有" -> Visibility.PRIVATE
                        "internal", "内部" -> Visibility.INTERNAL
                        "protected", "保护" -> Visibility.PROTECTED
                        else -> {
                            reportError("未知的可见性: '$visStr'", previous.location)
                            Visibility.PUBLIC
                        }
                    }
                }
                else -> {
                    reportError("未知的函数字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束函数定义")
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return FunctionDeclaration(
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
 
    /**
     * 解析函数修饰符
     */
    private fun parseFunctionModifiers(): FunctionModifiers {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始函数修饰符")
        
        var async = false
        var inline = false
        var pure = false
        var external = false
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "async", "异步" -> async = parseBooleanLiteral()
                "inline", "内联" -> inline = parseBooleanLiteral()
                "pure", "纯函数" -> pure = parseBooleanLiteral()
                "external", "外部" -> external = parseBooleanLiteral()
                else -> {
                    reportError("未知的函数修饰符: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束函数修饰符")
        
        return FunctionModifiers(
            async = async,
            inline = inline,
            pure = pure,
            external = external
        )
    }
    
    /**
     * 解析参数列表
     */
    private fun parseParameters(): List<Parameter> {
        consume(TokenType.LEFT_BRACKET, "期望 '[' 开始参数列表")
        val parameters = mutableListOf<Parameter>()
        
        while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
            parameters.add(parseParameter())
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束参数列表")
        return parameters
    }
    
    /**
     * 解析单个参数
     */
    private fun parseParameter(): Parameter {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始参数定义")
        
        var name: String? = null
        var type: Type? = null
        var optional = false
        var defaultValue: Expression? = null
        val annotations = mutableListOf<Annotation>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "name", "名称" -> name = parseStringLiteral()
                "type", "类型" -> type = parseType()
                "optional", "可选" -> optional = parseBooleanLiteral()
                "default", "默认值" -> defaultValue = parseExpression()
                "annotations", "注解" -> {
                    // 暂时跳过注解解析
                    skipValue()
                }
                else -> {
                    reportError("未知的参数字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束参数定义")
        
        if (name == null) {
            reportError("参数必须包含 'name' 字段", current.location)
            name = "param"
        }
        if (type == null) {
            reportError("参数必须包含 'type' 字段", current.location)
            type = PrimitiveType.AnyType
        }
        
        return Parameter(
            name = name,
            type = type,
            optional = optional,
            defaultValue = defaultValue,
            annotations = annotations
        )
    }
    
    /**
     * 解析返回类型
     */
    private fun parseReturnType(): Type {
        if (check(TokenType.LEFT_BRACE)) {
            // 复杂返回类型定义
            consume(TokenType.LEFT_BRACE, "期望 '{' 开始返回类型定义")
            
            var type: Type? = null
            
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                val key = parseStringLiteral()
                consume(TokenType.COLON, "期望 ':' 在字段名后")
                
                when (key) {
                    "type", "类型" -> type = parseType()
                    else -> {
                        reportError("未知的返回类型字段: '$key'", previous.location)
                        skipValue()
                    }
                }
                
                if (match(TokenType.COMMA)) {
                    // 逗号已消费
                }
            }
            
            consume(TokenType.RIGHT_BRACE, "期望 '}' 结束返回类型定义")
            
            return type ?: PrimitiveType.UnitType
        } else {
            // 简单返回类型
            return parseType()
        }
    }
    
    /**
     * 解析语句列表
     */
    private fun parseStatements(): List<Statement> {
        consume(TokenType.LEFT_BRACKET, "期望 '[' 开始语句列表")
        val statements = mutableListOf<Statement>()
        
        while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
            statements.add(parseStatement())
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束语句列表")
        return statements
    }
    
    /**
     * 解析单个语句
     */
    private fun parseStatement(): Statement {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始语句")
        
        var type: String? = null
        val properties = mutableMapOf<String, Any>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            if (key == "type" || key == "类型") {
                type = parseStringLiteral()
            } else {
                // 存储其他属性，稍后根据类型解析
                properties[key] = parseValue()
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束语句")
        
        if (type == null) {
            reportError("语句必须包含 'type' 字段", current.location)
            type = "expression"
        }
        
        return parseStatementByType(type, properties)
    }
    
    /**
     * 根据类型解析语句
     */
    private fun parseStatementByType(type: String, properties: Map<String, Any>): Statement {
        val location = current.location
        
        return when (type) {
            "variable_declaration", "变量声明" -> {
                val name = properties["name"] as? String ?: "unknown"
                val varType = properties["type"] as? Type
                val value = properties["value"] as? Expression
                val mutable = properties["mutable"] as? Boolean ?: false
                
                VariableDeclaration(
                    name = name,
                    type = varType,
                    value = value,
                    mutable = mutable,
                    location = location
                )
            }
            "expression", "表达式" -> {
                val expression = properties["expression"] as? Expression
                    ?: LiteralExpression.NullLiteral(location)
                
                ExpressionStatement(
                    expression = expression,
                    location = location
                )
            }
            "conditional", "条件" -> {
                val condition = properties["condition"] as? Expression
                    ?: LiteralExpression.BooleanLiteral(false, location)
                val thenBranch = properties["then"] as? Statement
                    ?: ExpressionStatement(LiteralExpression.NullLiteral(location), location)
                val elseBranch = properties["else"] as? Statement
                
                ConditionalStatement(
                    condition = condition,
                    thenBranch = thenBranch,
                    elseBranch = elseBranch,
                    location = location
                )
            }
            "return", "返回" -> {
                val value = properties["value"] as? Expression
                
                JumpStatement.ReturnStatement(
                    value = value,
                    location = location
                )
            }
            "break", "跳出" -> {
                val label = properties["label"] as? String
                
                JumpStatement.BreakStatement(
                    label = label,
                    location = location
                )
            }
            "continue", "继续" -> {
                val label = properties["label"] as? String
                
                JumpStatement.ContinueStatement(
                    label = label,
                    location = location
                )
            }
            else -> {
                reportError("未知的语句类型: '$type'", location)
                ExpressionStatement(
                    expression = LiteralExpression.NullLiteral(location),
                    location = location
                )
            }
        }
    }
    
    /**
     * 解析宏声明列表
     */
    private fun parseMacros(): List<MacroDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 macros")
        val macros = mutableListOf<MacroDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在宏名称后")
            macros.add(parseMacroDeclaration(name))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 macros")
        return macros
    }
    
    /**
     * 解析单个宏声明
     */
    private fun parseMacroDeclaration(name: String): MacroDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始宏定义")
        
        val parameters = mutableListOf<String>()
        var expansion: MacroExpansion? = null
        var phase = MacroPhase.COMPILE_TIME
        var visibility = Visibility.PUBLIC
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "parameters", "参数" -> {
                    parameters.addAll(parseStringArray())
                }
                "expansion", "展开" -> {
                    expansion = parseMacroExpansion()
                }
                "phase", "阶段" -> {
                    val phaseStr = parseStringLiteral()
                    phase = when (phaseStr) {
                        "compile_time", "编译时" -> MacroPhase.COMPILE_TIME
                        "runtime", "运行时" -> MacroPhase.RUNTIME
                        else -> {
                            reportError("未知的宏阶段: '$phaseStr'", previous.location)
                            MacroPhase.COMPILE_TIME
                        }
                    }
                }
                "visibility", "可见性" -> {
                    val visStr = parseStringLiteral()
                    visibility = when (visStr) {
                        "public", "公开" -> Visibility.PUBLIC
                        "private", "私有" -> Visibility.PRIVATE
                        "internal", "内部" -> Visibility.INTERNAL
                        "protected", "保护" -> Visibility.PROTECTED
                        else -> {
                            reportError("未知的可见性: '$visStr'", previous.location)
                            Visibility.PUBLIC
                        }
                    }
                }
                else -> {
                    reportError("未知的宏字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束宏定义")
        
        if (expansion == null) {
            reportError("宏必须包含 'expansion' 字段", startLocation)
            expansion = MacroExpansion.TemplateExpansion(
                LiteralExpression.NullLiteral(current.location)
            )
        }
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return MacroDeclaration(
            name = name,
            parameters = parameters,
            expansion = expansion,
            phase = phase,
            visibility = visibility,
            location = location
        )
    }
    
    /**
     * 解析宏展开
     */
    private fun parseMacroExpansion(): MacroExpansion {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始宏展开")
        
        var type: String? = null
        var template: ASTNode? = null
        var function: String? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "type", "类型" -> type = parseStringLiteral()
                "template", "模板" -> {
                    // 简化处理，解析为表达式
                    template = parseExpression()
                }
                "function", "函数" -> function = parseStringLiteral()
                else -> {
                    reportError("未知的宏展开字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束宏展开")
        
        return when (type) {
            "template", "模板" -> {
                MacroExpansion.TemplateExpansion(
                    template ?: LiteralExpression.NullLiteral(current.location)
                )
            }
            "function", "函数" -> {
                MacroExpansion.FunctionExpansion(
                    function ?: "unknown"
                )
            }
            else -> {
                reportError("未知的宏展开类型: '$type'", current.location)
                MacroExpansion.TemplateExpansion(
                    LiteralExpression.NullLiteral(current.location)
                )
            }
        }
    }
    
    /**
     * 解析 DSL 声明列表
     */
    private fun parseDSLs(): List<DSLDeclaration> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 dsl")
        val dsls = mutableListOf<DSLDeclaration>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在 DSL 名称后")
            dsls.add(parseDSLDeclaration(name))
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 dsl")
        return dsls
    }
    
    /**
     * 解析单个 DSL 声明
     */
    private fun parseDSLDeclaration(name: String): DSLDeclaration {
        val startLocation = current.location
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 DSL 定义")
        
        var syntax: DSLSyntax? = null
        var semantics: DSLSemantics? = null
        var transformation: DSLTransformation? = null
        var visibility = Visibility.PUBLIC
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "syntax", "语法" -> syntax = parseDSLSyntax()
                "semantics", "语义" -> semantics = parseDSLSemantics()
                "transformation", "转换" -> transformation = parseDSLTransformation()
                "visibility", "可见性" -> {
                    val visStr = parseStringLiteral()
                    visibility = when (visStr) {
                        "public", "公开" -> Visibility.PUBLIC
                        "private", "私有" -> Visibility.PRIVATE
                        "internal", "内部" -> Visibility.INTERNAL
                        "protected", "保护" -> Visibility.PROTECTED
                        else -> {
                            reportError("未知的可见性: '$visStr'", previous.location)
                            Visibility.PUBLIC
                        }
                    }
                }
                else -> {
                    reportError("未知的 DSL 字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 DSL 定义")
        
        val endLocation = previous.location
        val location = startLocation.merge(endLocation)
        
        return DSLDeclaration(
            name = name,
            syntax = syntax ?: DSLSyntax(emptyMap()),
            semantics = semantics ?: DSLSemantics(emptyMap()),
            transformation = transformation ?: DSLTransformation("", ""),
            visibility = visibility,
            location = location
        )
    }
    
    /**
     * 解析 DSL 语法
     */
    private fun parseDSLSyntax(): DSLSyntax {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 DSL 语法")
        val patterns = mutableMapOf<String, DSLPattern>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在模式名称后")
            patterns[name] = parseDSLPattern()
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 DSL 语法")
        return DSLSyntax(patterns)
    }
    
    /**
     * 解析 DSL 模式
     */
    private fun parseDSLPattern(): DSLPattern {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 DSL 模式")
        
        var pattern: String? = null
        val fields = mutableListOf<String>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "pattern", "模式" -> pattern = parseStringLiteral()
                "fields", "字段" -> fields.addAll(parseStringArray())
                else -> {
                    reportError("未知的 DSL 模式字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 DSL 模式")
        
        return DSLPattern(
            pattern = pattern ?: "",
            fields = fields
        )
    }
    
    /**
     * 解析 DSL 语义
     */
    private fun parseDSLSemantics(): DSLSemantics {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 DSL 语义")
        val rules = mutableMapOf<String, DSLSemanticRule>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在规则名称后")
            rules[name] = parseDSLSemanticRule()
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 DSL 语义")
        return DSLSemantics(rules)
    }
    
    /**
     * 解析 DSL 语义规则
     */
    private fun parseDSLSemanticRule(): DSLSemanticRule {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 DSL 语义规则")
        
        var type: String? = null
        val mapping = mutableMapOf<String, Any>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "type", "类型" -> type = parseStringLiteral()
                "mapping", "映射" -> {
                    consume(TokenType.LEFT_BRACE, "期望 '{' 开始映射")
                    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                        val mapKey = parseStringLiteral()
                        consume(TokenType.COLON, "期望 ':' 在映射键后")
                        mapping[mapKey] = parseValue()
                        
                        if (match(TokenType.COMMA)) {
                            // 逗号已消费
                        }
                    }
                    consume(TokenType.RIGHT_BRACE, "期望 '}' 结束映射")
                }
                else -> {
                    reportError("未知的 DSL 语义规则字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 DSL 语义规则")
        
        return DSLSemanticRule(
            type = type ?: "",
            mapping = mapping
        )
    }
    
    /**
     * 解析 DSL 转换
     */
    private fun parseDSLTransformation(): DSLTransformation {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始 DSL 转换")
        
        var target: String? = null
        var template: String? = null
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            when (key) {
                "target", "目标" -> target = parseStringLiteral()
                "template", "模板" -> template = parseStringLiteral()
                else -> {
                    reportError("未知的 DSL 转换字段: '$key'", previous.location)
                    skipValue()
                }
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束 DSL 转换")
        
        return DSLTransformation(
            target = target ?: "",
            template = template ?: ""
        )
    }    

    // ========== 表达式解析 ==========
    
    /**
     * 解析表达式
     */
    private fun parseExpression(): Expression {
        return parseAssignment()
    }
    
    /**
     * 解析赋值表达式
     */
    private fun parseAssignment(): Expression {
        val expr = parseLogicalOr()
        
        if (match(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
                  TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN)) {
            val operator = when (previous.type) {
                TokenType.ASSIGN -> BinaryOperator.ASSIGN
                TokenType.PLUS_ASSIGN -> BinaryOperator.PLUS_ASSIGN
                TokenType.MINUS_ASSIGN -> BinaryOperator.MINUS_ASSIGN
                TokenType.MULTIPLY_ASSIGN -> BinaryOperator.MULTIPLY_ASSIGN
                TokenType.DIVIDE_ASSIGN -> BinaryOperator.DIVIDE_ASSIGN
                else -> BinaryOperator.ASSIGN
            }
            
            val value = parseAssignment()
            val location = expr.location.merge(value.location)
            
            return BinaryExpression(expr, operator, value, location)
        }
        
        return expr
    }
    
    /**
     * 解析逻辑或表达式
     */
    private fun parseLogicalOr(): Expression {
        var expr = parseLogicalAnd()
        
        while (match(TokenType.OR)) {
            val operator = BinaryOperator.LOGICAL_OR
            val right = parseLogicalAnd()
            val location = expr.location.merge(right.location)
            expr = BinaryExpression(expr, operator, right, location)
        }
        
        return expr
    }
    
    /**
     * 解析逻辑与表达式
     */
    private fun parseLogicalAnd(): Expression {
        var expr = parseEquality()
        
        while (match(TokenType.AND)) {
            val operator = BinaryOperator.LOGICAL_AND
            val right = parseEquality()
            val location = expr.location.merge(right.location)
            expr = BinaryExpression(expr, operator, right, location)
        }
        
        return expr
    }
    
    /**
     * 解析相等性表达式
     */
    private fun parseEquality(): Expression {
        var expr = parseComparison()
        
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            val operator = when (previous.type) {
                TokenType.EQUAL -> BinaryOperator.EQUAL
                TokenType.NOT_EQUAL -> BinaryOperator.NOT_EQUAL
                else -> BinaryOperator.EQUAL
            }
            val right = parseComparison()
            val location = expr.location.merge(right.location)
            expr = BinaryExpression(expr, operator, right, location)
        }
        
        return expr
    }
    
    /**
     * 解析比较表达式
     */
    private fun parseComparison(): Expression {
        var expr = parseTerm()
        
        while (match(TokenType.GREATER_THAN, TokenType.GREATER_EQUAL,
                     TokenType.LESS_THAN, TokenType.LESS_EQUAL)) {
            val operator = when (previous.type) {
                TokenType.GREATER_THAN -> BinaryOperator.GREATER_THAN
                TokenType.GREATER_EQUAL -> BinaryOperator.GREATER_EQUAL
                TokenType.LESS_THAN -> BinaryOperator.LESS_THAN
                TokenType.LESS_EQUAL -> BinaryOperator.LESS_EQUAL
                else -> BinaryOperator.EQUAL
            }
            val right = parseTerm()
            val location = expr.location.merge(right.location)
            expr = BinaryExpression(expr, operator, right, location)
        }
        
        return expr
    }
    
    /**
     * 解析项表达式（加减）
     */
    private fun parseTerm(): Expression {
        var expr = parseFactor()
        
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = when (previous.type) {
                TokenType.MINUS -> BinaryOperator.MINUS
                TokenType.PLUS -> BinaryOperator.PLUS
                else -> BinaryOperator.PLUS
            }
            val right = parseFactor()
            val location = expr.location.merge(right.location)
            expr = BinaryExpression(expr, operator, right, location)
        }
        
        return expr
    }
    
    /**
     * 解析因子表达式（乘除）
     */
    private fun parseFactor(): Expression {
        var expr = parseUnary()
        
        while (match(TokenType.DIVIDE, TokenType.MULTIPLY, TokenType.MODULO)) {
            val operator = when (previous.type) {
                TokenType.DIVIDE -> BinaryOperator.DIVIDE
                TokenType.MULTIPLY -> BinaryOperator.MULTIPLY
                TokenType.MODULO -> BinaryOperator.MODULO
                else -> BinaryOperator.MULTIPLY
            }
            val right = parseUnary()
            val location = expr.location.merge(right.location)
            expr = BinaryExpression(expr, operator, right, location)
        }
        
        return expr
    }
    
    /**
     * 解析一元表达式
     */
    private fun parseUnary(): Expression {
        if (match(TokenType.EXCLAMATION, TokenType.MINUS, TokenType.PLUS)) {
            val operator = when (previous.type) {
                TokenType.EXCLAMATION -> UnaryOperator.LOGICAL_NOT
                TokenType.MINUS -> UnaryOperator.MINUS
                TokenType.PLUS -> UnaryOperator.PLUS
                else -> UnaryOperator.PLUS
            }
            val right = parseUnary()
            val location = previous.location.merge(right.location)
            return UnaryExpression(operator, right, location)
        }
        
        return parseCall()
    }
    
    /**
     * 解析函数调用表达式
     */
    private fun parseCall(): Expression {
        var expr = parsePrimary()
        
        while (true) {
            when {
                match(TokenType.LEFT_BRACKET) -> {
                    // 数组访问 obj[index]
                    val index = parseExpression()
                    consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束数组访问")
                    val location = expr.location.merge(previous.location)
                    expr = MemberExpression(expr, "", true, location)
                }
                match(TokenType.DOT) -> {
                    // 成员访问 obj.property
                    val name = consume(TokenType.IDENTIFIER, "期望属性名").lexeme
                    val location = expr.location.merge(previous.location)
                    expr = MemberExpression(expr, name, false, location)
                }
                check(TokenType.LEFT_BRACE) -> {
                    // 函数调用（JSON 对象作为参数）
                    val args = parseArguments()
                    val location = expr.location.merge(previous.location)
                    expr = CallExpression(expr, args, location)
                }
                else -> break
            }
        }
        
        return expr
    }
    
    /**
     * 解析函数参数
     */
    private fun parseArguments(): List<Argument> {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始参数")
        val arguments = mutableListOf<Argument>()
        
        if (!check(TokenType.RIGHT_BRACE)) {
            do {
                val name = if (check(TokenType.STRING_LITERAL)) {
                    val nameToken = advance()
                    consume(TokenType.COLON, "期望 ':' 在参数名后")
                    nameToken.lexeme.removeSurrounding("\"")
                } else {
                    null
                }
                
                val value = parseExpression()
                arguments.add(Argument(name, value))
            } while (match(TokenType.COMMA))
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束参数")
        return arguments
    }
    
    /**
     * 解析基本表达式
     */
    private fun parsePrimary(): Expression {
        val location = current.location
        
        when {
            match(TokenType.BOOLEAN_LITERAL) -> {
                val value = previous.literal as Boolean
                return LiteralExpression.BooleanLiteral(value, location)
            }
            
            match(TokenType.NULL_LITERAL) -> {
                return LiteralExpression.NullLiteral(location)
            }
            
            match(TokenType.INTEGER_LITERAL) -> {
                val value = previous.literal as Long
                return LiteralExpression.IntLiteral(value, location)
            }
            
            match(TokenType.FLOAT_LITERAL) -> {
                val value = previous.literal as Double
                return LiteralExpression.FloatLiteral(value, location)
            }
            
            match(TokenType.STRING_LITERAL) -> {
                val value = previous.literal as String
                return LiteralExpression.StringLiteral(value, location)
            }
            
            match(TokenType.IDENTIFIER) -> {
                val name = previous.lexeme
                return IdentifierExpression(name, location)
            }
            
            match(TokenType.LEFT_BRACKET) -> {
                // 数组字面量
                return parseArrayLiteral()
            }
            
            match(TokenType.LEFT_BRACE) -> {
                // 对象字面量
                return parseObjectLiteral()
            }
            
            else -> {
                reportError("期望表达式", current.location)
                return LiteralExpression.NullLiteral(location)
            }
        }
    }
    
    /**
     * 解析数组字面量
     */
    private fun parseArrayLiteral(): ArrayExpression {
        val startLocation = previous.location
        val elements = mutableListOf<Expression>()
        
        if (!check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(parseExpression())
            } while (match(TokenType.COMMA))
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束数组")
        val location = startLocation.merge(previous.location)
        
        return ArrayExpression(elements, location)
    }
    
    /**
     * 解析对象字面量
     */
    private fun parseObjectLiteral(): ObjectExpression {
        val startLocation = previous.location
        val properties = mutableListOf<ObjectProperty>()
        
        if (!check(TokenType.RIGHT_BRACE)) {
            do {
                val key = parseExpression()
                consume(TokenType.COLON, "期望 ':' 在对象键后")
                val value = parseExpression()
                properties.add(ObjectProperty(key, value))
            } while (match(TokenType.COMMA))
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束对象")
        val location = startLocation.merge(previous.location)
        
        return ObjectExpression(properties, location)
    }
    
    // ========== 类型解析 ==========
    
    /**
     * 解析类型
     */
    private fun parseType(): Type {
        if (check(TokenType.STRING_LITERAL)) {
            val typeName = parseStringLiteral()
            return parseTypeByName(typeName)
        } else if (check(TokenType.LEFT_BRACE)) {
            // 复杂类型定义
            return parseComplexType()
        } else {
            reportError("期望类型定义", current.location)
            return PrimitiveType.AnyType
        }
    }
    
    /**
     * 根据名称解析类型
     */
    private fun parseTypeByName(typeName: String): Type {
        return when (typeName) {
            "String", "字符串" -> PrimitiveType.StringType
            "Int", "整数" -> PrimitiveType.IntType
            "Long", "长整数" -> PrimitiveType.LongType
            "Float", "浮点数" -> PrimitiveType.FloatType
            "Double", "双精度" -> PrimitiveType.DoubleType
            "Boolean", "布尔" -> PrimitiveType.BooleanType
            "Unit", "单元" -> PrimitiveType.UnitType
            "Any", "任意" -> PrimitiveType.AnyType
            "Nothing", "空" -> PrimitiveType.NothingType
            "Null", "空值" -> PrimitiveType.NullType
            else -> {
                // 用户定义类型或泛型类型
                GenericType(typeName)
            }
        }
    }
    
    /**
     * 解析复杂类型
     */
    private fun parseComplexType(): Type {
        consume(TokenType.LEFT_BRACE, "期望 '{' 开始复杂类型")
        
        var kind: String? = null
        val properties = mutableMapOf<String, Any>()
        
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val key = parseStringLiteral()
            consume(TokenType.COLON, "期望 ':' 在字段名后")
            
            if (key == "kind" || key == "种类") {
                kind = parseStringLiteral()
            } else {
                properties[key] = parseValue()
            }
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACE, "期望 '}' 结束复杂类型")
        
        return when (kind) {
            "Array", "数组" -> {
                val elementType = properties["elementType"] as? Type ?: PrimitiveType.AnyType
                CompositeType.ArrayType(elementType)
            }
            "Object", "对象" -> {
                val keyType = properties["keyType"] as? Type ?: PrimitiveType.StringType
                val valueType = properties["valueType"] as? Type ?: PrimitiveType.AnyType
                CompositeType.ObjectType(keyType, valueType)
            }
            "Function", "函数" -> {
                val parameterTypes = properties["parameterTypes"] as? List<Type> ?: emptyList()
                val returnType = properties["returnType"] as? Type ?: PrimitiveType.UnitType
                CompositeType.FunctionType(parameterTypes, returnType)
            }
            "Optional", "可选" -> {
                val innerType = properties["innerType"] as? Type ?: PrimitiveType.AnyType
                CompositeType.OptionalType(innerType)
            }
            else -> {
                reportError("未知的复杂类型: '$kind'", current.location)
                PrimitiveType.AnyType
            }
        }
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 解析字符串字面量
     */
    private fun parseStringLiteral(): String {
        val token = consume(TokenType.STRING_LITERAL, "期望字符串字面量")
        return token.literal as String
    }
    
    /**
     * 解析布尔字面量
     */
    private fun parseBooleanLiteral(): Boolean {
        val token = consume(TokenType.BOOLEAN_LITERAL, "期望布尔字面量")
        return token.literal as Boolean
    }
    
    /**
     * 解析字符串数组
     */
    private fun parseStringArray(): List<String> {
        consume(TokenType.LEFT_BRACKET, "期望 '[' 开始字符串数组")
        val strings = mutableListOf<String>()
        
        while (!check(TokenType.RIGHT_BRACKET) && !isAtEnd()) {
            strings.add(parseStringLiteral())
            
            if (match(TokenType.COMMA)) {
                // 逗号已消费
            }
        }
        
        consume(TokenType.RIGHT_BRACKET, "期望 ']' 结束字符串数组")
        return strings
    }
    
    /**
     * 解析任意值（用于跳过未知字段）
     */
    private fun parseValue(): Any {
        return when {
            check(TokenType.STRING_LITERAL) -> parseStringLiteral()
            check(TokenType.INTEGER_LITERAL) -> {
                advance()
                previous.literal ?: 0L
            }
            check(TokenType.FLOAT_LITERAL) -> {
                advance()
                previous.literal ?: 0.0
            }
            check(TokenType.BOOLEAN_LITERAL) -> parseBooleanLiteral()
            check(TokenType.NULL_LITERAL) -> {
                advance()
                "null"
            }
            check(TokenType.LEFT_BRACE) -> {
                skipObject()
                "object"
            }
            check(TokenType.LEFT_BRACKET) -> {
                skipArray()
                "array"
            }
            else -> {
                reportError("期望值", current.location)
                advance()
                "unknown"
            }
        }
    }
    
    /**
     * 跳过值（用于错误恢复）
     */
    private fun skipValue() {
        when {
            check(TokenType.LEFT_BRACE) -> skipObject()
            check(TokenType.LEFT_BRACKET) -> skipArray()
            else -> advance()
        }
    }
    
    /**
     * 跳过对象
     */
    private fun skipObject() {
        consume(TokenType.LEFT_BRACE, "期望 '{'")
        var depth = 1
        
        while (depth > 0 && !isAtEnd()) {
            when (current.type) {
                TokenType.LEFT_BRACE -> {
                    depth++
                    advance()
                }
                TokenType.RIGHT_BRACE -> {
                    depth--
                    advance()
                }
                else -> advance()
            }
        }
    }
    
    /**
     * 跳过数组
     */
    private fun skipArray() {
        consume(TokenType.LEFT_BRACKET, "期望 '['")
        var depth = 1
        
        while (depth > 0 && !isAtEnd()) {
            when (current.type) {
                TokenType.LEFT_BRACKET -> {
                    depth++
                    advance()
                }
                TokenType.RIGHT_BRACKET -> {
                    depth--
                    advance()
                }
                else -> advance()
            }
        }
    }
    
    /**
     * 检查当前 Token 类型
     */
    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return current.type == type
    }
    
    /**
     * 匹配并消费 Token
     */
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }
    
    /**
     * 前进到下一个 Token
     */
    private fun advance(): Token {
        if (!isAtEnd()) {
            previous = current
            current = lexer.nextToken()
        }
        return previous
    }
    
    /**
     * 检查是否到达文件末尾
     */
    private fun isAtEnd(): Boolean {
        return current.type == TokenType.EOF
    }
    
    /**
     * 查看当前 Token
     */
    private fun peek(): Token {
        return current
    }
    
    /**
     * 消费指定类型的 Token
     */
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        
        reportError(message, current.location)
        throw ParseException(
            CompilerError(
                code = ErrorCodes.SYNTAX_UNEXPECTED_TOKEN,
                message = ErrorMessages.unexpectedToken(type.name, current.type.name),
                severity = ErrorSeverity.ERROR,
                category = ErrorCategory.SYNTAX,
                location = current.location
            )
        )
    }
    
    /**
     * 报告错误
     */
    private fun reportError(message: String, location: SourceLocation) {
        val error = CompilerError(
            code = ErrorCodes.SYNTAX_UNEXPECTED_TOKEN,
            message = message,
            severity = ErrorSeverity.ERROR,
            category = ErrorCategory.SYNTAX,
            location = location
        )
        errorReporter.report(error)
        panicMode = true
    }
    
    /**
     * 错误同步
     */
    private fun synchronize() {
        synchronizing = true
        panicMode = false
        
        advance()
        
        while (!isAtEnd()) {
            if (previous.type == TokenType.SEMICOLON) {
                synchronizing = false
                return
            }
            
            if (current.type in synchronizationTokens) {
                synchronizing = false
                return
            }
            
            advance()
        }
        
        synchronizing = false
    }
}

/**
 * 解析异常
 */
class ParseException(val error: CompilerError) : Exception(error.message)
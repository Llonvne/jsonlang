package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*

/**
 * Kotlin 后端代码生成器
 * 将 JSONLang AST 转换为 Kotlin 代码
 */
class KotlinBackend : AbstractBackend() {
    
    private val typeMapping = KotlinTypeMapping()
    private val imports = mutableSetOf<String>()
    
    override fun getName(): String = "kotlin"
    
    override fun getTargetLanguage(): String = "Kotlin"
    
    override fun getFileExtension(): String = "kt"
    
    override fun getTypeMapping(): TypeMapping = typeMapping
    
    override fun supportsAsync(): Boolean = true
    
    override fun supportsGenerics(): Boolean = true
    
    override fun generateProgram(program: Program): BackendResult {
        imports.clear()
        val parts = mutableListOf<String>()
        
        // 生成包声明
        if (program.metadata.name.isNotEmpty()) {
            parts.add("package ${program.metadata.name}")
            parts.add("")
        }
        
        // 生成导入
        program.imports.forEach { import ->
            val importCode = generateImport(import)
            if (importCode.isNotEmpty()) {
                imports.add(importCode)
            }
        }
        
        // 添加必要的导入
        addRequiredImports(program)
        
        if (imports.isNotEmpty()) {
            parts.addAll(imports.sorted())
            parts.add("")
        }
        
        // 生成类型声明
        program.types.forEach { type ->
            parts.add(generateDeclaration(type))
            parts.add("")
        }
        
        // 生成常量声明
        program.constants.forEach { constant ->
            parts.add(generateDeclaration(constant))
        }
        
        if (program.constants.isNotEmpty()) {
            parts.add("")
        }
        
        // 生成函数声明
        program.functions.forEach { function ->
            parts.add(generateDeclaration(function))
            parts.add("")
        }
        
        // 生成主函数（如果需要）
        if (hasMainFunction(program)) {
            parts.add(generateMainFunction(program))
        }
        
        val code = parts.joinToString("\n").trimEnd()
        
        return BackendResult(
            code = code,
            imports = imports.toList(),
            metadata = mapOf(
                "package" to program.metadata.name,
                "version" to program.metadata.version
            )
        )
    }
    
    override fun generateExpression(expression: Expression): String {
        return when (expression) {
            is LiteralExpression.StringLiteral -> "\"${escapeString(expression.value)}\""
            is LiteralExpression.IntLiteral -> expression.value.toString()
            is LiteralExpression.FloatLiteral -> "${expression.value}f"
            is LiteralExpression.BooleanLiteral -> expression.value.toString()
            is LiteralExpression.NullLiteral -> "null"
            is IdentifierExpression -> expression.name
            is BinaryExpression -> generateBinaryExpression(expression)
            is UnaryExpression -> generateUnaryExpression(expression)
            is CallExpression -> generateCallExpression(expression)
            is MemberExpression -> generateMemberExpression(expression)
            is ArrayExpression -> generateArrayExpression(expression)
            is ObjectExpression -> generateObjectExpression(expression)
            is LambdaExpression -> generateLambdaExpression(expression)
            is ConditionalExpression -> generateConditionalExpression(expression)
            is CastExpression -> generateCastExpression(expression)
            is TypeCheckExpression -> generateTypeCheckExpression(expression)
            is CodeLiteralExpression -> expression.code
            is MacroCall -> {
                // Kotlin 不直接支持宏，转换为函数调用
                val args = expression.arguments.joinToString(", ") { arg ->
                    when (arg) {
                        is Expression -> generateExpression(arg)
                        else -> arg.toString() // 简化处理非表达式参数
                    }
                }
                "${expression.name}($args)"
            }
            is MacroParameterPlaceholder -> "\${${expression.parameterName}}"
        }
    }
    
    override fun generateStatement(statement: Statement): String {
        // 由于 Statement 类型还没有完全定义，这里提供基础实现
        return when (statement) {
            // TODO: 实现具体的语句类型
            else -> "// TODO: Implement statement generation for ${statement::class.simpleName}"
        }
    }
    
    override fun generateDeclaration(declaration: Declaration): String {
        return when (declaration) {
            is ImportDeclaration -> generateImport(declaration)
            is ExportDeclaration -> generateExport(declaration)
            is TypeDeclaration -> generateTypeDeclaration(declaration)
            is ConstantDeclaration -> generateConstantDeclaration(declaration)
            is FunctionDeclaration -> generateFunctionDeclaration(declaration)
            is MacroDeclaration -> generateMacroDeclaration(declaration)
            is DSLDeclaration -> generateDSLDeclaration(declaration)
        }
    }
    
    override fun generateType(type: Type): String {
        return typeMapping.mapType(type)
    }
    
    override fun getOptimizations(): List<BackendOptimization> {
        return listOf(
            BackendOptimization.InlineOptimization(),
            BackendOptimization.TailCallOptimization(),
            BackendOptimization.DeadCodeElimination(),
            BackendOptimization.ConstantFolding()
        )
    }
    
    override fun generateParameter(parameter: Parameter): String {
        val name = parameter.name
        val type = generateType(parameter.type)
        val optional = if (parameter.optional) "?" else ""
        val default = if (parameter.defaultValue != null) {
            " = ${generateExpression(parameter.defaultValue)}"
        } else ""
        
        return "$name: $type$optional$default"
    }
    
    override fun generateModifiers(modifiers: FunctionModifiers): String {
        val parts = mutableListOf<String>()
        
        if (modifiers.async) {
            parts.add("suspend")
            imports.add("import kotlinx.coroutines.*")
        }
        
        if (modifiers.inline) {
            parts.add("inline")
        }
        
        if (modifiers.external) {
            parts.add("external")
        }
        
        return if (parts.isNotEmpty()) parts.joinToString(" ") + " " else ""
    }
    
    override fun generateImport(import: ImportDeclaration): String {
        return when {
            import.items.isEmpty() || import.items.contains("*") -> {
                "import ${import.source}.*"
            }
            import.items.size == 1 -> {
                val item = import.items.first()
                val alias = import.alias[item]
                if (alias != null) {
                    "import ${import.source}.$item as $alias"
                } else {
                    "import ${import.source}.$item"
                }
            }
            else -> {
                // Kotlin 不支持多项导入，需要分别导入
                import.items.joinToString("\n") { item ->
                    val alias = import.alias[item]
                    if (alias != null) {
                        "import ${import.source}.$item as $alias"
                    } else {
                        "import ${import.source}.$item"
                    }
                }
            }
        }
    }
    
    override fun generateExport(export: ExportDeclaration): String {
        // Kotlin 没有显式的导出语法，通过可见性修饰符控制
        return "// Export: ${export.name} (${export.exportType})"
    }
    
    private fun generateBinaryExpression(expr: BinaryExpression): String {
        val left = generateExpression(expr.left)
        val right = generateExpression(expr.right)
        val operator = when (expr.operator) {
            BinaryOperator.PLUS -> "+"
            BinaryOperator.MINUS -> "-"
            BinaryOperator.MULTIPLY -> "*"
            BinaryOperator.DIVIDE -> "/"
            BinaryOperator.MODULO -> "%"
            BinaryOperator.EQUAL -> "=="
            BinaryOperator.NOT_EQUAL -> "!="
            BinaryOperator.LESS_THAN -> "<"
            BinaryOperator.LESS_EQUAL -> "<="
            BinaryOperator.GREATER_THAN -> ">"
            BinaryOperator.GREATER_EQUAL -> ">="
            BinaryOperator.LOGICAL_AND -> "&&"
            BinaryOperator.LOGICAL_OR -> "||"
            BinaryOperator.BITWISE_AND -> "and"
            BinaryOperator.BITWISE_OR -> "or"
            BinaryOperator.BITWISE_XOR -> "xor"
            BinaryOperator.LEFT_SHIFT -> "shl"
            BinaryOperator.RIGHT_SHIFT -> "shr"
            BinaryOperator.ASSIGN -> "="
            BinaryOperator.PLUS_ASSIGN -> "+="
            BinaryOperator.MINUS_ASSIGN -> "-="
            BinaryOperator.MULTIPLY_ASSIGN -> "*="
            BinaryOperator.DIVIDE_ASSIGN -> "/="
        }
        
        return "$left $operator $right"
    }
    
    private fun generateUnaryExpression(expr: UnaryExpression): String {
        val operand = generateExpression(expr.operand)
        val operator = when (expr.operator) {
            UnaryOperator.PLUS -> "+"
            UnaryOperator.MINUS -> "-"
            UnaryOperator.LOGICAL_NOT -> "!"
            UnaryOperator.BITWISE_NOT -> ".inv()"
            UnaryOperator.PRE_INCREMENT -> "++"
            UnaryOperator.POST_INCREMENT -> "++"
            UnaryOperator.PRE_DECREMENT -> "--"
            UnaryOperator.POST_DECREMENT -> "--"
        }
        
        return when (expr.operator) {
            UnaryOperator.BITWISE_NOT -> "$operand$operator"
            UnaryOperator.POST_INCREMENT, UnaryOperator.POST_DECREMENT -> "$operand$operator"
            else -> "$operator$operand"
        }
    }
    
    private fun generateCallExpression(expr: CallExpression): String {
        val function = generateExpression(expr.function)
        val args = expr.arguments.joinToString(", ") { arg ->
            if (arg.name != null) {
                "${arg.name} = ${generateExpression(arg.value)}"
            } else {
                generateExpression(arg.value)
            }
        }
        return "$function($args)"
    }
    
    private fun generateMemberExpression(expr: MemberExpression): String {
        val target = generateExpression(expr.target)
        return if (expr.computed) {
            "$target[${expr.property}]"
        } else {
            "$target.${expr.property}"
        }
    }
    
    private fun generateArrayExpression(expr: ArrayExpression): String {
        val elements = expr.elements.joinToString(", ") { generateExpression(it) }
        return "listOf($elements)"
    }
    
    private fun generateObjectExpression(expr: ObjectExpression): String {
        val properties = expr.properties.joinToString(", ") { prop ->
            val key = if (prop.computed) {
                generateExpression(prop.key)
            } else {
                "\"${generateExpression(prop.key)}\""
            }
            val value = generateExpression(prop.value)
            "$key to $value"
        }
        return "mapOf($properties)"
    }
    
    private fun generateLambdaExpression(expr: LambdaExpression): String {
        val params = expr.parameters.joinToString(", ") { "${it.name}: ${generateType(it.type)}" }
        val body = generateExpression(expr.body)
        return "{ $params -> $body }"
    }
    
    private fun generateConditionalExpression(expr: ConditionalExpression): String {
        val condition = generateExpression(expr.condition)
        val thenExpr = generateExpression(expr.thenExpression)
        val elseExpr = generateExpression(expr.elseExpression)
        return "if ($condition) $thenExpr else $elseExpr"
    }
    
    private fun generateCastExpression(expr: CastExpression): String {
        val expression = generateExpression(expr.expression)
        val targetType = generateType(expr.targetType)
        val operator = if (expr.safe) "as?" else "as"
        return "$expression $operator $targetType"
    }
    
    private fun generateTypeCheckExpression(expr: TypeCheckExpression): String {
        val expression = generateExpression(expr.expression)
        val type = generateType(expr.type)
        return "$expression is $type"
    }
    

    
    private fun generateTypeDeclaration(decl: TypeDeclaration): String {
        return when (val definition = decl.definition) {
            is TypeDefinition.StructDefinition -> generateStructDeclaration(decl.name, definition)
            is TypeDefinition.EnumDefinition -> generateEnumDeclaration(decl.name, definition)
            is TypeDefinition.UnionDefinition -> generateUnionDeclaration(decl.name, definition)
            is TypeDefinition.AliasDefinition -> generateAliasDeclaration(decl.name, definition)
            is TypeDefinition.GenericDefinition -> generateGenericDeclaration(decl.name, definition)
        }
    }
    
    private fun generateStructDeclaration(name: String, definition: TypeDefinition.StructDefinition): String {
        val fields = definition.fields.entries.joinToString(",\n") { (fieldName, field) ->
            val type = generateType(field.type)
            val optional = if (field.optional) "?" else ""
            val default = if (field.defaultValue != null) {
                " = ${generateExpression(field.defaultValue as Expression)}"
            } else ""
            "${indent()}    val $fieldName: $type$optional$default"
        }
        
        return """data class $name(
$fields
)"""
    }
    
    private fun generateEnumDeclaration(name: String, definition: TypeDefinition.EnumDefinition): String {
        val variants = definition.variants.entries.joinToString(",\n") { (variantName, type) ->
            if (type != null) {
                "${indent()}    $variantName(val value: ${generateType(type)})"
            } else {
                "${indent()}    $variantName"
            }
        }
        
        return """sealed class $name {
$variants
}"""
    }
    
    private fun generateUnionDeclaration(name: String, definition: TypeDefinition.UnionDefinition): String {
        // Kotlin 使用密封类实现联合类型
        val types = definition.types.mapIndexed { index, type ->
            "${indent()}    data class Variant$index(val value: ${generateType(type)}) : $name()"
        }.joinToString("\n")
        
        return """sealed class $name {
$types
}"""
    }
    
    private fun generateAliasDeclaration(name: String, definition: TypeDefinition.AliasDefinition): String {
        return "typealias $name = ${generateType(definition.aliasedType)}"
    }
    
    private fun generateGenericDeclaration(name: String, definition: TypeDefinition.GenericDefinition): String {
        val params = definition.parameters.joinToString(", ") { param ->
            if (param.constraints.isNotEmpty()) {
                "${param.name} : ${param.constraints.joinToString(" & ") { generateType(it) }}"
            } else {
                param.name
            }
        }
        return "typealias $name<$params> = ${generateType(definition.baseType)}"
    }
    
    private fun generateConstantDeclaration(decl: ConstantDeclaration): String {
        val modifiers = when (decl.visibility) {
            Visibility.PUBLIC -> "public "
            Visibility.PRIVATE -> "private "
            Visibility.PROTECTED -> "protected "
            Visibility.INTERNAL -> "internal "
        }
        val type = generateType(decl.type)
        val value = generateExpression(decl.value)
        return "${modifiers}const val ${decl.name}: $type = $value"
    }
    
    private fun generateFunctionDeclaration(decl: FunctionDeclaration): String {
        val visibilityModifier = when (decl.visibility) {
            Visibility.PUBLIC -> "public "
            Visibility.PRIVATE -> "private "
            Visibility.PROTECTED -> "protected "
            Visibility.INTERNAL -> "internal "
        }
        val modifiers = visibilityModifier + generateModifiers(decl.modifiers)
        val generics = if (decl.generics.isNotEmpty()) {
            val params = decl.generics.joinToString(", ") { param ->
                if (param.constraints.isNotEmpty()) {
                    "${param.name} : ${param.constraints.joinToString(" & ") { generateType(it) }}"
                } else {
                    param.name
                }
            }
            "<$params>"
        } else ""
        
        val params = generateParameterList(decl.parameters)
        val returnType = generateType(decl.returnType)
        val body = generateBlock(decl.body)
        
        return """${modifiers}fun $generics${decl.name}($params): $returnType {
$body
}"""
    }
    
    private fun generateMacroDeclaration(decl: MacroDeclaration): String {
        // Kotlin 不支持宏，转换为内联函数
        val params = decl.parameters.joinToString(", ") { "$it: Any" }
        return "// Macro ${decl.name}($params) - converted to inline function"
    }
    
    private fun generateDSLDeclaration(decl: DSLDeclaration): String {
        return "// DSL ${decl.name} - requires manual implementation"
    }
    
    private fun addRequiredImports(program: Program) {
        // 添加序列化相关导入
        if (program.types.isNotEmpty()) {
            imports.add("import kotlinx.serialization.*")
        }
        
        // 添加协程相关导入
        if (program.functions.any { it.modifiers.async }) {
            imports.add("import kotlinx.coroutines.*")
        }
    }
    
    private fun hasMainFunction(program: Program): Boolean {
        return program.functions.any { it.name == "main" }
    }
    
    private fun generateMainFunction(program: Program): String {
        val mainFunction = program.functions.find { it.name == "main" }
        return if (mainFunction != null) {
            generateFunctionDeclaration(mainFunction)
        } else {
            """fun main() {
    // Generated main function
}"""
        }
    }
    
    private fun escapeString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

/**
 * Kotlin 类型映射实现
 */
class KotlinTypeMapping : TypeMapping {
    
    fun mapType(type: Type): String {
        return when (type) {
            is PrimitiveType -> mapPrimitiveType(type)
            is CompositeType -> mapCompositeType(type)
            is UserDefinedType -> mapUserDefinedType(type)
            is GenericType -> mapGenericType(type)
            else -> type.name
        }
    }
    
    override fun mapPrimitiveType(type: Type): String {
        return when (type) {
            is PrimitiveType.StringType -> "String"
            is PrimitiveType.NullableStringType -> "String?"
            is PrimitiveType.IntType -> "Int"
            is PrimitiveType.NullableIntType -> "Int?"
            is PrimitiveType.LongType -> "Long"
            is PrimitiveType.NullableLongType -> "Long?"
            is PrimitiveType.FloatType -> "Float"
            is PrimitiveType.NullableFloatType -> "Float?"
            is PrimitiveType.DoubleType -> "Double"
            is PrimitiveType.NullableDoubleType -> "Double?"
            is PrimitiveType.BooleanType -> "Boolean"
            is PrimitiveType.NullableBooleanType -> "Boolean?"
            is PrimitiveType.UnitType -> "Unit"
            is PrimitiveType.AnyType -> "Any"
            is PrimitiveType.NullableAnyType -> "Any?"
            is PrimitiveType.NothingType -> "Nothing"
            is PrimitiveType.NullType -> "Nothing?"
            else -> type.name
        }
    }
    
    override fun mapCompositeType(type: Type): String {
        return when (type) {
            is CompositeType.ArrayType -> {
                val elementType = mapType(type.elementType)
                val base = "List<$elementType>"
                if (type.nullable) "$base?" else base
            }
            is CompositeType.ObjectType -> {
                val keyType = mapType(type.keyType)
                val valueType = mapType(type.valueType)
                val base = "Map<$keyType, $valueType>"
                if (type.nullable) "$base?" else base
            }
            is CompositeType.FunctionType -> {
                val paramTypes = type.parameterTypes.joinToString(", ") { mapType(it) }
                val returnType = mapType(type.returnType)
                val base = "($paramTypes) -> $returnType"
                if (type.nullable) "($base)?" else base
            }
            is CompositeType.OptionalType -> {
                "${mapType(type.innerType)}?"
            }
            else -> type.name
        }
    }
    
    override fun mapUserDefinedType(type: Type): String {
        val base = type.name
        return if (type.nullable) "$base?" else base
    }
    
    override fun mapGenericType(type: Type): String {
        val base = type.name
        return if (type.nullable) "$base?" else base
    }
    
    override fun mapNullableType(type: Type): String {
        return "${mapType(type.makeNonNull())}?"
    }
    
    override fun getDefaultValue(type: Type): String {
        return when (type) {
            is PrimitiveType.StringType -> "\"\""
            is PrimitiveType.IntType -> "0"
            is PrimitiveType.LongType -> "0L"
            is PrimitiveType.FloatType -> "0.0f"
            is PrimitiveType.DoubleType -> "0.0"
            is PrimitiveType.BooleanType -> "false"
            is PrimitiveType.UnitType -> "Unit"
            is CompositeType.ArrayType -> "emptyList()"
            is CompositeType.ObjectType -> "emptyMap()"
            else -> if (type.nullable) "null" else "TODO(\"Default value for ${type.name}\")"
        }
    }
    
    override fun requiresImport(type: Type): String? {
        return when (type) {
            is CompositeType.ArrayType -> null // List 是内置的
            is CompositeType.ObjectType -> null // Map 是内置的
            else -> null
        }
    }
}
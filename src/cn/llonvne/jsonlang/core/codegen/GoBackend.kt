package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*

/**
 * Go 后端代码生成器
 * 将 JSONLang AST 转换为 Go 代码
 */
class GoBackend : AbstractBackend() {
    
    private val typeMapping = GoTypeMapping()
    private val imports = mutableSetOf<String>()
    private val packageName = mutableSetOf<String>()
    
    override fun getName(): String = "go"
    
    override fun getTargetLanguage(): String = "Go"
    
    override fun getFileExtension(): String = "go"
    
    override fun getTypeMapping(): TypeMapping = typeMapping
    
    public override fun supportsAsync(): Boolean = true // Go 支持 goroutines
    
    public override fun supportsGenerics(): Boolean = true // Go 1.18+ 支持泛型
    
    override fun generateProgram(program: Program): BackendResult {
        imports.clear()
        packageName.clear()
        val parts = mutableListOf<String>()
        
        // 生成包声明
        val pkgName = if (program.metadata.name.isNotEmpty()) {
            program.metadata.name.lowercase().replace("-", "")
        } else {
            "main"
        }
        parts.add("package $pkgName")
        parts.add("")
        
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
            if (imports.size == 1) {
                parts.add("import ${imports.first()}")
            } else {
                parts.add("import (")
                imports.sorted().forEach { import ->
                    parts.add("\t$import")
                }
                parts.add(")")
            }
            parts.add("")
        }
        
        // 生成类型声明
        program.types.forEach { type ->
            parts.add(generateDeclaration(type))
            parts.add("")
        }
        
        // 生成常量声明
        if (program.constants.isNotEmpty()) {
            parts.add("const (")
            program.constants.forEach { constant ->
                parts.add("\t${generateConstantDeclaration(constant)}")
            }
            parts.add(")")
            parts.add("")
        }
        
        // 生成函数声明
        program.functions.forEach { function ->
            parts.add(generateDeclaration(function))
            parts.add("")
        }
        
        val code = parts.joinToString("\n").trimEnd()
        
        return BackendResult(
            code = code,
            imports = imports.toList(),
            metadata = mapOf(
                "package" to pkgName,
                "version" to program.metadata.version,
                "module" to program.metadata.name
            )
        )
    }
    
    override fun generateExpression(expression: Expression): String {
        return when (expression) {
            is LiteralExpression.StringLiteral -> "\"${escapeString(expression.value)}\""
            is LiteralExpression.IntLiteral -> expression.value.toString()
            is LiteralExpression.FloatLiteral -> expression.value.toString()
            is LiteralExpression.BooleanLiteral -> expression.value.toString()
            is LiteralExpression.NullLiteral -> "nil"
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
                // Go 不支持宏，转换为函数调用
                val args = expression.arguments.joinToString(", ") { arg ->
                    when (arg) {
                        is Expression -> generateExpression(arg)
                        else -> arg.toString()
                    }
                }
                "${expression.name}($args)"
            }
            is MacroParameterPlaceholder -> "\${${expression.parameterName}}"
        }
    }
    
    override fun generateStatement(statement: Statement): String {
        return when (statement) {
            is VariableDeclaration -> generateVariableDeclaration(statement)
            is ExpressionStatement -> generateExpression(statement.expression)
            is ConditionalStatement -> generateConditionalStatement(statement)
            is LoopStatement.ForLoop -> generateForLoop(statement)
            is LoopStatement.WhileLoop -> generateWhileLoop(statement)
            is JumpStatement.ReturnStatement -> generateReturnStatement(statement)
            is JumpStatement.BreakStatement -> "break"
            is JumpStatement.ContinueStatement -> "continue"
            is TryCatchStatement -> generateTryCatchStatement(statement)
            is BlockStatement -> generateBlockStatement(statement)
            is MatchStatement -> generateMatchStatement(statement)
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
            BackendOptimization.DeadCodeElimination(),
            BackendOptimization.ConstantFolding()
        )
    }
    
    override fun generateParameter(parameter: Parameter): String {
        val name = parameter.name
        val type = generateType(parameter.type)
        return "$name $type"
    }
    
    override fun generateModifiers(modifiers: FunctionModifiers): String {
        // Go 没有函数修饰符，但我们可以通过注释或命名约定来表示
        val parts = mutableListOf<String>()
        
        if (modifiers.async) {
            // Go 使用 goroutines，我们在函数体中处理
        }
        
        if (modifiers.inline) {
            // Go 编译器自动内联，添加注释提示
            parts.add("// inline")
        }
        
        if (modifiers.external) {
            // Go 使用 cgo 或 syscall
            parts.add("// external")
        }
        
        return if (parts.isNotEmpty()) parts.joinToString(" ") + "\n" else ""
    }
    
    override fun generateImport(import: ImportDeclaration): String {
        return when {
            import.source.startsWith("./") || import.source.startsWith("../") -> {
                // 相对路径导入
                "\"${import.source}\""
            }
            import.source.contains("/") -> {
                // 包路径导入
                "\"${import.source}\""
            }
            else -> {
                // 标准库导入
                "\"${import.source}\""
            }
        }
    }
    
    override fun generateExport(export: ExportDeclaration): String {
        // Go 通过首字母大写来导出符号
        return "// Export: ${export.name} (${export.exportType}) - use capitalized name"
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
            BinaryOperator.BITWISE_AND -> "&"
            BinaryOperator.BITWISE_OR -> "|"
            BinaryOperator.BITWISE_XOR -> "^"
            BinaryOperator.LEFT_SHIFT -> "<<"
            BinaryOperator.RIGHT_SHIFT -> ">>"
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
            UnaryOperator.BITWISE_NOT -> "^"
            UnaryOperator.PRE_INCREMENT -> "++"
            UnaryOperator.POST_INCREMENT -> "++"
            UnaryOperator.PRE_DECREMENT -> "--"
            UnaryOperator.POST_DECREMENT -> "--"
        }
        
        return when (expr.operator) {
            UnaryOperator.POST_INCREMENT, UnaryOperator.POST_DECREMENT -> "$operand$operator"
            else -> "$operator$operand"
        }
    }
    
    private fun generateCallExpression(expr: CallExpression): String {
        val function = generateExpression(expr.function)
        val args = expr.arguments.joinToString(", ") { arg ->
            generateExpression(arg.value)
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
        return "[]any{$elements}"
    }
    
    private fun generateObjectExpression(expr: ObjectExpression): String {
        val properties = expr.properties.joinToString(", ") { prop ->
            val key = generateExpression(prop.key)
            val value = generateExpression(prop.value)
            "$key: $value"
        }
        return "map[string]any{$properties}"
    }
    
    private fun generateLambdaExpression(expr: LambdaExpression): String {
        val params = expr.parameters.joinToString(", ") { "${it.name} ${generateType(it.type)}" }
        val body = generateExpression(expr.body)
        val returnType = expr.inferredType?.let { 
            if (it is CompositeType.FunctionType) generateType(it.returnType) else "any"
        } ?: "any"
        
        return "func($params) $returnType { return $body }"
    }
    
    private fun generateConditionalExpression(expr: ConditionalExpression): String {
        val condition = generateExpression(expr.condition)
        val thenExpr = generateExpression(expr.thenExpression)
        val elseExpr = generateExpression(expr.elseExpression)
        
        // Go 没有三元操作符，使用立即执行函数
        return "func() any { if $condition { return $thenExpr } else { return $elseExpr } }()"
    }
    
    private fun generateCastExpression(expr: CastExpression): String {
        val expression = generateExpression(expr.expression)
        val targetType = generateType(expr.targetType)
        
        return if (expr.safe) {
            // 安全类型转换
            "$expression.($targetType)"
        } else {
            // 强制类型转换
            "$targetType($expression)"
        }
    }
    
    private fun generateTypeCheckExpression(expr: TypeCheckExpression): String {
        val expression = generateExpression(expr.expression)
        val type = generateType(expr.type)
        return "_, ok := $expression.($type); ok"
    }
    
    private fun generateVariableDeclaration(stmt: VariableDeclaration): String {
        val name = stmt.name
        val value = stmt.value?.let { generateExpression(it) }
        val type = stmt.type?.let { generateType(it) }
        
        return when {
            value != null && type != null -> {
                if (stmt.mutable) "var $name $type = $value" else "$name := $value"
            }
            value != null -> {
                if (stmt.mutable) "var $name = $value" else "$name := $value"
            }
            type != null -> "var $name $type"
            else -> "var $name any"
        }
    }
    
    private fun generateConditionalStatement(stmt: ConditionalStatement): String {
        val condition = generateExpression(stmt.condition)
        val thenBranch = withIndent { generateStatement(stmt.thenBranch) }
        val elseBranch = stmt.elseBranch?.let { withIndent { generateStatement(it) } }
        
        return if (elseBranch != null) {
            """if $condition {
$thenBranch
} else {
$elseBranch
}"""
        } else {
            """if $condition {
$thenBranch
}"""
        }
    }
    
    private fun generateForLoop(stmt: LoopStatement.ForLoop): String {
        val iterator = stmt.iterator
        val iterable = generateExpression(stmt.iterable)
        val body = withIndent { generateStatement(stmt.body) }
        
        return """for $iterator := range $iterable {
$body
}"""
    }
    
    private fun generateWhileLoop(stmt: LoopStatement.WhileLoop): String {
        val condition = generateExpression(stmt.condition)
        val body = withIndent { generateStatement(stmt.body) }
        
        return """for $condition {
$body
}"""
    }
    
    private fun generateReturnStatement(stmt: JumpStatement.ReturnStatement): String {
        return if (stmt.value != null) {
            "return ${generateExpression(stmt.value)}"
        } else {
            "return"
        }
    }
    
    private fun generateTryCatchStatement(stmt: TryCatchStatement): String {
        // Go 使用错误值而不是异常，这里提供一个基本的转换
        val tryBlock = generateBlockStatement(stmt.tryBlock)
        
        return """// Try-catch converted to Go error handling
$tryBlock
// TODO: Implement proper error handling"""
    }
    
    private fun generateBlockStatement(stmt: BlockStatement): String {
        return stmt.statements.joinToString("\n") { "${indent()}${generateStatement(it)}" }
    }
    
    private fun generateMatchStatement(stmt: MatchStatement): String {
        val expression = generateExpression(stmt.expression)
        val cases = stmt.cases.joinToString("\n") { case ->
            val pattern = generatePattern(case.pattern)
            val body = withIndent { generateStatement(case.body) }
            """case $pattern:
$body"""
        }
        
        val defaultCase = stmt.defaultCase?.let { 
            val body = withIndent { generateStatement(it) }
            """default:
$body"""
        } ?: ""
        
        return """switch $expression {
$cases
$defaultCase
}"""
    }
    
    private fun generatePattern(pattern: Pattern): String {
        return when (pattern) {
            is Pattern.LiteralPattern -> pattern.value.toString()
            is Pattern.VariablePattern -> pattern.name
            is Pattern.ConstructorPattern -> "${pattern.constructor}(${pattern.args.joinToString(", ") { generatePattern(it) }})"
            is Pattern.WildcardPattern -> "_"
        }
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
        val fields = definition.fields.entries.joinToString("\n") { (fieldName, field) ->
            val type = generateType(field.type)
            val jsonTag = "`json:\"${fieldName.lowercase()}\"`"
            "${indent()}    ${fieldName.replaceFirstChar { it.uppercase() }} $type $jsonTag"
        }
        
        return """type $name struct {
$fields
}"""
    }
    
    private fun generateEnumDeclaration(name: String, definition: TypeDefinition.EnumDefinition): String {
        val baseType = "int"
        val variants = definition.variants.entries.mapIndexed { index, (variantName, _) ->
            "${indent()}    $variantName $name = $index"
        }.joinToString("\n")
        
        return """type $name $baseType

const (
$variants
)"""
    }
    
    private fun generateUnionDeclaration(name: String, definition: TypeDefinition.UnionDefinition): String {
        // Go 使用接口实现联合类型
        val methods = definition.types.mapIndexed { index, _ ->
            "${indent()}    is${name}Variant$index() bool"
        }.joinToString("\n")
        
        return """type $name interface {
$methods
}"""
    }
    
    private fun generateAliasDeclaration(name: String, definition: TypeDefinition.AliasDefinition): String {
        return "type $name = ${generateType(definition.aliasedType)}"
    }
    
    private fun generateGenericDeclaration(name: String, definition: TypeDefinition.GenericDefinition): String {
        val params = definition.parameters.joinToString(", ") { param ->
            if (param.constraints.isNotEmpty()) {
                "${param.name} ${param.constraints.joinToString(" | ") { generateType(it) }}"
            } else {
                "${param.name} any"
            }
        }
        return "type $name[$params] = ${generateType(definition.baseType)}"
    }
    
    private fun generateConstantDeclaration(decl: ConstantDeclaration): String {
        val type = generateType(decl.type)
        val value = generateExpression(decl.value)
        return "${decl.name} $type = $value"
    }
    
    private fun generateFunctionDeclaration(decl: FunctionDeclaration): String {
        val modifiers = generateModifiers(decl.modifiers)
        val generics = if (decl.generics.isNotEmpty()) {
            val params = decl.generics.joinToString(", ") { param ->
                if (param.constraints.isNotEmpty()) {
                    "${param.name} ${param.constraints.joinToString(" | ") { generateType(it) }}"
                } else {
                    "${param.name} any"
                }
            }
            "[$params]"
        } else ""
        
        val params = generateParameterList(decl.parameters)
        val returnType = generateType(decl.returnType)
        val body = generateBlock(decl.body)
        
        val funcName = if (decl.visibility == Visibility.PUBLIC) {
            decl.name.replaceFirstChar { it.uppercase() }
        } else {
            decl.name
        }
        
        val asyncWrapper = if (decl.modifiers.async) {
            // 为异步函数添加 goroutine 支持
            val syncFuncName = "${funcName}Sync"
            val paramNames = decl.parameters.joinToString(", ") { it.name }
            val asyncFunc = """func $funcName$generics($params) <-chan $returnType {
    ch := make(chan $returnType, 1)
    go func() {
        defer close(ch)
        result := $syncFuncName($paramNames)
        ch <- result
    }()
    return ch
}

${modifiers}func $syncFuncName$generics($params) $returnType {
$body
}"""
            asyncFunc
        } else {
            """${modifiers}func $funcName$generics($params) $returnType {
$body
}"""
        }
        
        return asyncWrapper
    }
    
    private fun generateMacroDeclaration(decl: MacroDeclaration): String {
        // Go 不支持宏，转换为代码生成注释
        val params = decl.parameters.joinToString(", ")
        return "// Macro ${decl.name}($params) - requires code generation"
    }
    
    private fun generateDSLDeclaration(decl: DSLDeclaration): String {
        return "// DSL ${decl.name} - requires manual implementation"
    }
    
    private fun addRequiredImports(program: Program) {
        // 添加 JSON 相关导入
        if (program.types.isNotEmpty()) {
            imports.add("\"encoding/json\"")
        }
        
        // 添加并发相关导入
        if (program.functions.any { it.modifiers.async }) {
            imports.add("\"sync\"")
        }
        
        // 添加错误处理相关导入
        if (program.functions.any { it.body.any { stmt -> stmt is TryCatchStatement } }) {
            imports.add("\"errors\"")
        }
        
        // 添加反射相关导入（用于类型检查）
        if (program.functions.any { func -> 
            func.body.any { stmt -> containsTypeCheck(stmt) }
        }) {
            imports.add("\"reflect\"")
        }
    }
    
    private fun containsTypeCheck(stmt: Statement): Boolean {
        return when (stmt) {
            is ExpressionStatement -> containsTypeCheckExpression(stmt.expression)
            is ConditionalStatement -> containsTypeCheckExpression(stmt.condition)
            is BlockStatement -> stmt.statements.any { containsTypeCheck(it) }
            else -> false
        }
    }
    
    private fun containsTypeCheckExpression(expr: Expression): Boolean {
        return when (expr) {
            is TypeCheckExpression -> true
            is BinaryExpression -> containsTypeCheckExpression(expr.left) || containsTypeCheckExpression(expr.right)
            is UnaryExpression -> containsTypeCheckExpression(expr.operand)
            is CallExpression -> expr.arguments.any { containsTypeCheckExpression(it.value) }
            else -> false
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
 * Go 类型映射实现
 */
class GoTypeMapping : TypeMapping {
    
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
            is PrimitiveType.StringType -> "string"
            is PrimitiveType.NullableStringType -> "*string"
            is PrimitiveType.IntType -> "int"
            is PrimitiveType.NullableIntType -> "*int"
            is PrimitiveType.LongType -> "int64"
            is PrimitiveType.NullableLongType -> "*int64"
            is PrimitiveType.FloatType -> "float32"
            is PrimitiveType.NullableFloatType -> "*float32"
            is PrimitiveType.DoubleType -> "float64"
            is PrimitiveType.NullableDoubleType -> "*float64"
            is PrimitiveType.BooleanType -> "bool"
            is PrimitiveType.NullableBooleanType -> "*bool"
            is PrimitiveType.UnitType -> "struct{}"
            is PrimitiveType.AnyType -> "any"
            is PrimitiveType.NullableAnyType -> "any"
            is PrimitiveType.NothingType -> "struct{}"
            is PrimitiveType.NullType -> "any"
            else -> type.name
        }
    }
    
    override fun mapCompositeType(type: Type): String {
        return when (type) {
            is CompositeType.ArrayType -> {
                val elementType = mapType(type.elementType)
                val base = "[]$elementType"
                if (type.nullable) "*$base" else base
            }
            is CompositeType.ObjectType -> {
                val keyType = mapType(type.keyType)
                val valueType = mapType(type.valueType)
                val base = "map[$keyType]$valueType"
                if (type.nullable) "*$base" else base
            }
            is CompositeType.FunctionType -> {
                val paramTypes = type.parameterTypes.joinToString(", ") { mapType(it) }
                val returnType = mapType(type.returnType)
                val base = "func($paramTypes) $returnType"
                if (type.nullable) "*$base" else base
            }
            is CompositeType.OptionalType -> {
                "*${mapType(type.innerType)}"
            }
            else -> type.name
        }
    }
    
    override fun mapUserDefinedType(type: Type): String {
        val base = type.name
        return if (type.nullable) "*$base" else base
    }
    
    override fun mapGenericType(type: Type): String {
        val base = type.name
        return if (type.nullable) "*$base" else base
    }
    
    override fun mapNullableType(type: Type): String {
        return "*${mapType(type.makeNonNull())}"
    }
    
    override fun getDefaultValue(type: Type): String {
        return when (type) {
            is PrimitiveType.StringType -> "\"\""
            is PrimitiveType.IntType -> "0"
            is PrimitiveType.LongType -> "0"
            is PrimitiveType.FloatType -> "0.0"
            is PrimitiveType.DoubleType -> "0.0"
            is PrimitiveType.BooleanType -> "false"
            is PrimitiveType.UnitType -> "struct{}{}"
            is CompositeType.ArrayType -> "nil"
            is CompositeType.ObjectType -> "nil"
            else -> if (type.nullable) "nil" else "/* TODO: Default value for ${type.name} */"
        }
    }
    
    override fun requiresImport(type: Type): String? {
        return when (type) {
            is CompositeType.ArrayType -> null // 内置类型
            is CompositeType.ObjectType -> null // 内置类型
            else -> null
        }
    }
}
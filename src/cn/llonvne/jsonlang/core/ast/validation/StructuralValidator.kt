package cn.llonvne.jsonlang.core.ast.validation

import cn.llonvne.jsonlang.core.ast.*

/**
 * 结构完整性验证器
 * 验证 AST 的基本结构完整性
 */
class StructuralValidator : BaseASTValidator() {
    override val name: String = "StructuralValidator"
    override val description: String = "验证 AST 的基本结构完整性"
    override val priority: Int = 0
    
    override fun validate(node: ASTNode, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证节点基本属性
        result = result.merge(validateNodeProperties(node))
        
        // 验证特定节点类型的结构
        result = result.merge(validateNodeStructure(node, context))
        
        // 递归验证子节点
        result = result.merge(validateChildren(node, context))
        
        context.recordValidation(name, result)
        return result
    }
    
    /**
     * 验证节点基本属性
     */
    private fun validateNodeProperties(node: ASTNode): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证位置信息
        if (node.location.startLine < 1 || node.location.startColumn < 1) {
            result = result.merge(error(
                "INVALID_LOCATION",
                "节点位置信息无效: 行号和列号必须大于0",
                node.location
            ))
        }
        
        // 验证节点类型
        if (node.nodeType.isBlank()) {
            result = result.merge(error(
                "EMPTY_NODE_TYPE",
                "节点类型不能为空",
                node.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证特定节点类型的结构
     */
    private fun validateNodeStructure(node: ASTNode, context: ValidationContext): ValidationResult {
        return when (node) {
            is Program -> validateProgram(node)
            is FunctionDeclaration -> validateFunction(node)
            is VariableDeclaration -> validateVariable(node)
            is BinaryExpression -> validateBinaryExpression(node)
            is CallExpression -> validateCallExpression(node)
            is ConditionalStatement -> validateConditionalStatement(node)
            is LoopStatement -> validateLoopStatement(node)
            is TryCatchStatement -> validateTryCatchStatement(node)
            is MatchStatement -> validateMatchStatement(node)
            else -> ValidationResult.success()
        }
    }
    
    /**
     * 验证程序结构
     */
    private fun validateProgram(program: Program): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证元数据
        if (program.metadata.name.isBlank()) {
            result = result.merge(error(
                "EMPTY_PROGRAM_NAME",
                "程序名称不能为空",
                program.metadata.location
            ))
        }
        
        if (program.metadata.version.isBlank()) {
            result = result.merge(error(
                "EMPTY_PROGRAM_VERSION",
                "程序版本不能为空",
                program.metadata.location
            ))
        }
        
        // 验证重复的函数名
        val functionNames = mutableSetOf<String>()
        for (function in program.functions) {
            if (functionNames.contains(function.name)) {
                result = result.merge(error(
                    "DUPLICATE_FUNCTION",
                    "重复的函数名: ${function.name}",
                    function.location
                ))
            } else {
                functionNames.add(function.name)
            }
        }
        
        // 验证重复的类型名
        val typeNames = mutableSetOf<String>()
        for (type in program.types) {
            if (typeNames.contains(type.name)) {
                result = result.merge(error(
                    "DUPLICATE_TYPE",
                    "重复的类型名: ${type.name}",
                    type.location
                ))
            } else {
                typeNames.add(type.name)
            }
        }
        
        return result
    }
    
    /**
     * 验证函数结构
     */
    private fun validateFunction(function: FunctionDeclaration): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证函数名
        if (function.name.isBlank()) {
            result = result.merge(error(
                "EMPTY_FUNCTION_NAME",
                "函数名不能为空",
                function.location
            ))
        }
        
        // 验证参数名重复
        val parameterNames = mutableSetOf<String>()
        for (parameter in function.parameters) {
            if (parameterNames.contains(parameter.name)) {
                result = result.merge(error(
                    "DUPLICATE_PARAMETER",
                    "重复的参数名: ${parameter.name}",
                    function.location
                ))
            } else {
                parameterNames.add(parameter.name)
            }
        }
        
        // 验证函数体不为空（除非是外部函数）
        if (function.body.isEmpty() && !function.modifiers.external) {
            result = result.merge(error(
                "EMPTY_FUNCTION_BODY",
                "非外部函数的函数体不能为空",
                function.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证变量声明
     */
    private fun validateVariable(variable: VariableDeclaration): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证变量名
        if (variable.name.isBlank()) {
            result = result.merge(error(
                "EMPTY_VARIABLE_NAME",
                "变量名不能为空",
                variable.location
            ))
        }
        
        // 验证类型或初始值至少有一个
        if (variable.type == null && variable.value == null) {
            result = result.merge(error(
                "MISSING_TYPE_OR_VALUE",
                "变量声明必须指定类型或初始值",
                variable.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证二元表达式
     */
    private fun validateBinaryExpression(expr: BinaryExpression): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证操作符的合法性
        when (expr.operator) {
            BinaryOperator.DIVIDE, BinaryOperator.MODULO -> {
                // 检查除零（如果右操作数是常量）
                if (expr.right is LiteralExpression.IntLiteral && expr.right.value == 0L) {
                    result = result.merge(error(
                        "DIVISION_BY_ZERO",
                        "除零错误",
                        expr.location
                    ))
                }
                if (expr.right is LiteralExpression.FloatLiteral && expr.right.value == 0.0) {
                    result = result.merge(error(
                        "DIVISION_BY_ZERO",
                        "除零错误",
                        expr.location
                    ))
                }
            }
            else -> {}
        }
        
        return result
    }
    
    /**
     * 验证函数调用表达式
     */
    private fun validateCallExpression(call: CallExpression): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证参数名的唯一性（对于命名参数）
        val namedArguments = call.arguments.filter { it.name != null }
        val argumentNames = mutableSetOf<String>()
        
        for (arg in namedArguments) {
            val name = arg.name!!
            if (argumentNames.contains(name)) {
                result = result.merge(error(
                    "DUPLICATE_ARGUMENT_NAME",
                    "重复的参数名: $name",
                    call.location
                ))
            } else {
                argumentNames.add(name)
            }
        }
        
        return result
    }
    
    /**
     * 验证条件语句
     */
    private fun validateConditionalStatement(conditional: ConditionalStatement): ValidationResult {
        var result = ValidationResult.success()
        
        // 检查条件是否为常量（可能的逻辑错误）
        when (conditional.condition) {
            is LiteralExpression.BooleanLiteral -> {
                result = result.merge(warning(
                    "CONSTANT_CONDITION",
                    "条件表达式是常量，可能存在逻辑错误",
                    conditional.condition.location
                ))
            }
            else -> {}
        }
        
        return result
    }
    
    /**
     * 验证循环语句
     */
    private fun validateLoopStatement(loop: LoopStatement): ValidationResult {
        var result = ValidationResult.success()
        
        when (loop) {
            is LoopStatement.WhileLoop -> {
                // 检查无限循环
                if (loop.condition is LiteralExpression.BooleanLiteral && 
                    loop.condition.value) {
                    result = result.merge(warning(
                        "INFINITE_LOOP",
                        "检测到可能的无限循环",
                        loop.location
                    ))
                }
            }
            is LoopStatement.ForLoop -> {
                // 验证迭代器名称
                if (loop.iterator.isBlank()) {
                    result = result.merge(error(
                        "EMPTY_ITERATOR_NAME",
                        "迭代器名称不能为空",
                        loop.location
                    ))
                }
            }
        }
        
        return result
    }
    
    /**
     * 验证异常处理语句
     */
    private fun validateTryCatchStatement(tryCatch: TryCatchStatement): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证至少有一个 catch 子句
        if (tryCatch.catchClauses.isEmpty()) {
            result = result.merge(error(
                "MISSING_CATCH_CLAUSE",
                "try 语句必须至少有一个 catch 子句",
                tryCatch.location
            ))
        }
        
        // 验证 catch 子句的异常类型不重复
        val exceptionTypes = mutableSetOf<String>()
        for (catchClause in tryCatch.catchClauses) {
            val typeName = catchClause.exceptionType.toString()
            if (exceptionTypes.contains(typeName)) {
                result = result.merge(warning(
                    "DUPLICATE_CATCH_TYPE",
                    "重复的异常类型: $typeName",
                    catchClause.location
                ))
            } else {
                exceptionTypes.add(typeName)
            }
        }
        
        return result
    }
    
    /**
     * 验证模式匹配语句
     */
    private fun validateMatchStatement(match: MatchStatement): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证至少有一个 case 或默认 case
        if (match.cases.isEmpty() && match.defaultCase == null) {
            result = result.merge(error(
                "EMPTY_MATCH_STATEMENT",
                "match 语句必须至少有一个 case 或默认 case",
                match.location
            ))
        }
        
        return result
    }
}
package cn.llonvne.jsonlang.core.ast.validation

import cn.llonvne.jsonlang.core.ast.*

/**
 * 语义验证器
 * 验证 AST 的语义正确性
 */
class SemanticValidator : BaseASTValidator() {
    override val name: String = "SemanticValidator"
    override val description: String = "验证 AST 的语义正确性"
    override val priority: Int = 10
    
    private val symbolTable = mutableMapOf<String, ASTNode>()
    private val scopeStack = mutableListOf<MutableMap<String, ASTNode>>()
    
    override fun validate(node: ASTNode, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证语义规则
        result = result.merge(validateSemantics(node, context))
        
        // 递归验证子节点
        result = result.merge(validateChildren(node, context))
        
        context.recordValidation(name, result)
        return result
    }
    
    /**
     * 验证语义规则
     */
    private fun validateSemantics(node: ASTNode, context: ValidationContext): ValidationResult {
        return when (node) {
            is Program -> validateProgramSemantics(node, context)
            is FunctionDeclaration -> validateFunctionSemantics(node, context)
            is VariableDeclaration -> validateVariableSemantics(node, context)
            is IdentifierExpression -> validateIdentifierSemantics(node, context)
            is CallExpression -> validateCallSemantics(node, context)
            is BinaryExpression -> validateBinarySemantics(node, context)
            is JumpStatement.ReturnStatement -> validateReturnSemantics(node, context)
            is JumpStatement.BreakStatement -> validateBreakSemantics(node, context)
            is JumpStatement.ContinueStatement -> validateContinueSemantics(node, context)
            is ImportDeclaration -> validateImportSemantics(node, context)
            is ExportDeclaration -> validateExportSemantics(node, context)
            else -> ValidationResult.success()
        }
    }
    
    /**
     * 验证程序语义
     */
    private fun validateProgramSemantics(program: Program, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 构建全局符号表
        enterScope()
        
        // 添加所有顶级声明到符号表
        program.functions.forEach { symbolTable[it.name] = it }
        program.types.forEach { symbolTable[it.name] = it }
        program.constants.forEach { symbolTable[it.name] = it }
        
        // 验证导出的符号是否存在
        for (export in program.exports) {
            if (!symbolTable.containsKey(export.name)) {
                result = result.merge(error(
                    "UNDEFINED_EXPORT",
                    "导出的符号 '${export.name}' 未定义",
                    export.location
                ))
            }
        }
        
        exitScope()
        return result
    }
    
    /**
     * 验证函数语义
     */
    private fun validateFunctionSemantics(function: FunctionDeclaration, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        enterScope()
        
        // 添加参数到符号表
        for (parameter in function.parameters) {
            if (symbolTable.containsKey(parameter.name)) {
                result = result.merge(error(
                    "DUPLICATE_PARAMETER",
                    "重复的参数名: ${parameter.name}",
                    function.location
                ))
            } else {
                symbolTable[parameter.name] = VariableDeclaration(
                    parameter.name,
                    parameter.type,
                    parameter.defaultValue,
                    false,
                    function.location
                )
            }
        }
        
        // 验证函数体中的返回语句
        val returnStatements = findReturnStatements(function.body)
        if (returnStatements.isEmpty() && function.returnType.toString() != "Unit") {
            result = result.merge(warning(
                "MISSING_RETURN",
                "函数可能缺少返回语句",
                function.location
            ))
        }
        
        exitScope()
        return result
    }
    
    /**
     * 验证变量语义
     */
    private fun validateVariableSemantics(variable: VariableDeclaration, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 检查变量名是否已存在于当前作用域
        val currentScope = scopeStack.lastOrNull() ?: symbolTable
        if (currentScope.containsKey(variable.name)) {
            result = result.merge(error(
                "VARIABLE_REDEFINITION",
                "变量 '${variable.name}' 重复定义",
                variable.location
            ))
        } else {
            currentScope[variable.name] = variable
        }
        
        return result
    }
    
    /**
     * 验证标识符语义
     */
    private fun validateIdentifierSemantics(identifier: IdentifierExpression, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 检查标识符是否已定义
        if (!isSymbolDefined(identifier.name)) {
            result = result.merge(error(
                "UNDEFINED_SYMBOL",
                "未定义的符号: ${identifier.name}",
                identifier.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证函数调用语义
     */
    private fun validateCallSemantics(call: CallExpression, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 如果是简单的函数调用，验证函数是否存在
        if (call.function is IdentifierExpression) {
            val functionName = (call.function as IdentifierExpression).name
            val function = findSymbol(functionName)
            
            if (function == null) {
                result = result.merge(error(
                    "UNDEFINED_FUNCTION",
                    "未定义的函数: $functionName",
                    call.location
                ))
            } else if (function is FunctionDeclaration) {
                // 验证参数数量
                val requiredParams = function.parameters.count { it.defaultValue == null }
                val providedArgs = call.arguments.size
                
                if (providedArgs < requiredParams) {
                    result = result.merge(error(
                        "INSUFFICIENT_ARGUMENTS",
                        "参数不足: 需要 $requiredParams 个，提供了 $providedArgs 个",
                        call.location
                    ))
                }
                
                if (providedArgs > function.parameters.size) {
                    result = result.merge(error(
                        "TOO_MANY_ARGUMENTS",
                        "参数过多: 最多 ${function.parameters.size} 个，提供了 $providedArgs 个",
                        call.location
                    ))
                }
            }
        }
        
        return result
    }
    
    /**
     * 验证二元表达式语义
     */
    private fun validateBinarySemantics(binary: BinaryExpression, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证赋值操作的左值
        if (binary.operator == BinaryOperator.ASSIGN) {
            if (binary.left !is IdentifierExpression && binary.left !is MemberExpression) {
                result = result.merge(error(
                    "INVALID_ASSIGNMENT_TARGET",
                    "无效的赋值目标",
                    binary.left.location
                ))
            }
        }
        
        return result
    }
    
    /**
     * 验证返回语句语义
     */
    private fun validateReturnSemantics(returnStmt: JumpStatement.ReturnStatement, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 检查是否在函数内部
        if (!isInFunction()) {
            result = result.merge(error(
                "RETURN_OUTSIDE_FUNCTION",
                "return 语句只能在函数内部使用",
                returnStmt.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证 break 语句语义
     */
    private fun validateBreakSemantics(breakStmt: JumpStatement.BreakStatement, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 检查是否在循环内部
        if (!isInLoop()) {
            result = result.merge(error(
                "BREAK_OUTSIDE_LOOP",
                "break 语句只能在循环内部使用",
                breakStmt.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证 continue 语句语义
     */
    private fun validateContinueSemantics(continueStmt: JumpStatement.ContinueStatement, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 检查是否在循环内部
        if (!isInLoop()) {
            result = result.merge(error(
                "CONTINUE_OUTSIDE_LOOP",
                "continue 语句只能在循环内部使用",
                continueStmt.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证导入语义
     */
    private fun validateImportSemantics(import: ImportDeclaration, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证导入路径
        if (import.source.isBlank()) {
            result = result.merge(error(
                "EMPTY_IMPORT_SOURCE",
                "导入源不能为空",
                import.location
            ))
        }
        
        // 验证循环导入（简化实现）
        if (import.source == context.program.metadata.name) {
            result = result.merge(error(
                "CIRCULAR_IMPORT",
                "检测到循环导入",
                import.location
            ))
        }
        
        return result
    }
    
    /**
     * 验证导出语义
     */
    private fun validateExportSemantics(export: ExportDeclaration, context: ValidationContext): ValidationResult {
        var result = ValidationResult.success()
        
        // 验证导出名称
        if (export.name.isBlank()) {
            result = result.merge(error(
                "EMPTY_EXPORT_NAME",
                "导出名称不能为空",
                export.location
            ))
        }
        
        return result
    }
    
    // 辅助方法
    
    private fun enterScope() {
        scopeStack.add(mutableMapOf())
    }
    
    private fun exitScope() {
        if (scopeStack.isNotEmpty()) {
            scopeStack.removeAt(scopeStack.size - 1)
        }
    }
    
    private fun isSymbolDefined(name: String): Boolean {
        // 从内到外查找符号
        for (i in scopeStack.size - 1 downTo 0) {
            if (scopeStack[i].containsKey(name)) {
                return true
            }
        }
        return symbolTable.containsKey(name)
    }
    
    private fun findSymbol(name: String): ASTNode? {
        // 从内到外查找符号
        for (i in scopeStack.size - 1 downTo 0) {
            scopeStack[i][name]?.let { return it }
        }
        return symbolTable[name]
    }
    
    private fun findReturnStatements(statements: List<Statement>): List<JumpStatement.ReturnStatement> {
        val returns = mutableListOf<JumpStatement.ReturnStatement>()
        
        fun collectReturns(stmt: Statement) {
            when (stmt) {
                is JumpStatement.ReturnStatement -> returns.add(stmt)
                is BlockStatement -> stmt.statements.forEach { collectReturns(it) }
                is ConditionalStatement -> {
                    collectReturns(stmt.thenBranch)
                    stmt.elseBranch?.let { collectReturns(it) }
                }
                is LoopStatement.ForLoop -> collectReturns(stmt.body)
                is LoopStatement.WhileLoop -> collectReturns(stmt.body)
                is TryCatchStatement -> {
                    collectReturns(stmt.tryBlock)
                    stmt.catchClauses.forEach { collectReturns(it.handler) }
                    stmt.finallyBlock?.let { collectReturns(it) }
                }
                is MatchStatement -> {
                    stmt.cases.forEach { collectReturns(it.body) }
                    stmt.defaultCase?.let { collectReturns(it) }
                }
                is ExpressionStatement -> { /* no returns */ }
                is JumpStatement.BreakStatement -> { /* no returns */ }
                is JumpStatement.ContinueStatement -> { /* no returns */ }
                is VariableDeclaration -> { /* no returns */ }
            }
        }
        
        statements.forEach { collectReturns(it) }
        return returns
    }
    
    private fun isInFunction(): Boolean {
        // 简化实现：假设总是在函数内部
        return true
    }
    
    private fun isInLoop(): Boolean {
        // 简化实现：需要更复杂的上下文跟踪
        return true
    }
}
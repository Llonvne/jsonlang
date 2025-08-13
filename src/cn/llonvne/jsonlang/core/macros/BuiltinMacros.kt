package cn.llonvne.jsonlang.core.macros

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import cn.llonvne.jsonlang.core.types.Type

/**
 * 内置宏定义
 * 提供常用的宏功能
 */
object BuiltinMacros {
    
    private val unknownLocation = SourceLocation.unknown()
    
    /**
     * 获取所有内置宏定义
     */
    fun getAllBuiltinMacros(): List<MacroDeclaration> {
        return listOf(
            createAssertMacro(),
            createLogMacro(),
            createUnlessMacro(),
            createWhenMacro(),
            createRepeatMacro(),
            createDefPropertyMacro(),
            createLazyMacro()
        )
    }
    
    /**
     * 创建 assert 宏
     * 用法: assert(condition, "message")
     */
    private fun createAssertMacro(): MacroDeclaration {
        val template = ConditionalStatement(
            condition = UnaryExpression(
                operator = UnaryOperator.LOGICAL_NOT,
                operand = IdentifierExpression("condition", unknownLocation),
                location = unknownLocation
            ),
            thenBranch = ExpressionStatement(
                expression = CallExpression(
                    function = IdentifierExpression("throw", unknownLocation),
                    arguments = listOf(
                        Argument(
                            value = CallExpression(
                                function = IdentifierExpression("Error", unknownLocation),
                                arguments = listOf(
                                    Argument(value = IdentifierExpression("message", unknownLocation))
                                ),
                                location = unknownLocation
                            )
                        )
                    ),
                    location = unknownLocation
                ),
                location = unknownLocation
            ),
            location = unknownLocation
        )
        
        return MacroDeclaration(
            name = "assert",
            parameters = listOf("condition", "message"),
            expansion = MacroExpansion.TemplateExpansion(template),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
    
    /**
     * 创建 log 宏
     * 用法: log("message", value)
     */
    private fun createLogMacro(): MacroDeclaration {
        val template = ExpressionStatement(
            expression = CallExpression(
                function = MemberExpression(
                    target = IdentifierExpression("console", unknownLocation),
                    property = "log",
                    location = unknownLocation
                ),
                arguments = listOf(
                    Argument(value = IdentifierExpression("message", unknownLocation)),
                    Argument(value = IdentifierExpression("value", unknownLocation))
                ),
                location = unknownLocation
            ),
            location = unknownLocation
        )
        
        return MacroDeclaration(
            name = "log",
            parameters = listOf("message", "value"),
            expansion = MacroExpansion.TemplateExpansion(template),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
    
    /**
     * 创建 unless 宏
     * 用法: unless(condition) { body }
     */
    private fun createUnlessMacro(): MacroDeclaration {
        val template = ConditionalStatement(
            condition = UnaryExpression(
                operator = UnaryOperator.LOGICAL_NOT,
                operand = IdentifierExpression("condition", unknownLocation),
                location = unknownLocation
            ),
            thenBranch = ExpressionStatement(
                expression = IdentifierExpression("body", unknownLocation),
                location = unknownLocation
            ),
            location = unknownLocation
        )
        
        return MacroDeclaration(
            name = "unless",
            parameters = listOf("condition", "body"),
            expansion = MacroExpansion.TemplateExpansion(template),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
    
    /**
     * 创建 when 宏
     * 用法: when(condition) { body }
     */
    private fun createWhenMacro(): MacroDeclaration {
        val template = ConditionalStatement(
            condition = IdentifierExpression("condition", unknownLocation),
            thenBranch = ExpressionStatement(
                expression = IdentifierExpression("body", unknownLocation),
                location = unknownLocation
            ),
            location = unknownLocation
        )
        
        return MacroDeclaration(
            name = "when",
            parameters = listOf("condition", "body"),
            expansion = MacroExpansion.TemplateExpansion(template),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
    
    /**
     * 创建 repeat 宏
     * 用法: repeat(n) { body }
     */
    private fun createRepeatMacro(): MacroDeclaration {
        return MacroDeclaration(
            name = "repeat",
            parameters = listOf("times", "body"),
            expansion = MacroExpansion.FunctionExpansion("expandRepeatMacro"),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
    
    /**
     * 创建 defProperty 宏
     * 用法: defProperty(name, getter, setter)
     */
    private fun createDefPropertyMacro(): MacroDeclaration {
        return MacroDeclaration(
            name = "defProperty",
            parameters = listOf("name", "getter", "setter"),
            expansion = MacroExpansion.FunctionExpansion("expandDefPropertyMacro"),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
    
    /**
     * 创建 lazy 宏
     * 用法: lazy { expression }
     */
    private fun createLazyMacro(): MacroDeclaration {
        return MacroDeclaration(
            name = "lazy",
            parameters = listOf("expression"),
            expansion = MacroExpansion.FunctionExpansion("expandLazyMacro"),
            phase = MacroPhase.COMPILE_TIME,
            location = unknownLocation
        )
    }
}

/**
 * 内置宏展开函数
 */
object BuiltinMacroExpanders {
    
    private val unknownLocation = SourceLocation.unknown()
    
    /**
     * 获取所有内置宏展开函数
     */
    fun getAllExpansionFunctions(): Map<String, MacroExpansionFunction> {
        return mapOf(
            "expandRepeatMacro" to ::expandRepeatMacro,
            "expandDefPropertyMacro" to ::expandDefPropertyMacro,
            "expandLazyMacro" to ::expandLazyMacro
        )
    }
    
    /**
     * 展开 repeat 宏
     */
    private fun expandRepeatMacro(
        bindings: Map<String, ASTNode>,
        context: MacroExpansionContext
    ): ASTNode {
        val times = bindings["times"] as? LiteralExpression.IntLiteral
            ?: throw IllegalArgumentException("repeat 宏的第一个参数必须是整数字面量")
        
        val body = bindings["body"] as? Statement
            ?: throw IllegalArgumentException("repeat 宏的第二个参数必须是语句")
        
        val statements = mutableListOf<Statement>()
        
        // 创建循环变量
        val loopVar = "_i_${context.expansionDepth}"
        statements.add(
            VariableDeclaration(
                name = loopVar,
                type = null,
                value = LiteralExpression.IntLiteral(0, unknownLocation),
                location = unknownLocation
            )
        )
        
        // 创建 while 循环
        val whileLoop = LoopStatement.WhileLoop(
            condition = BinaryExpression(
                left = IdentifierExpression(loopVar, unknownLocation),
                operator = BinaryOperator.LESS_THAN,
                right = LiteralExpression.IntLiteral(times.value, unknownLocation),
                location = unknownLocation
            ),
            body = BlockStatement(
                statements = listOf(
                    body,
                    ExpressionStatement(
                        expression = BinaryExpression(
                            left = IdentifierExpression(loopVar, unknownLocation),
                            operator = BinaryOperator.ASSIGN,
                            right = BinaryExpression(
                                left = IdentifierExpression(loopVar, unknownLocation),
                                operator = BinaryOperator.PLUS,
                                right = LiteralExpression.IntLiteral(1, unknownLocation),
                                location = unknownLocation
                            ),
                            location = unknownLocation
                        ),
                        location = unknownLocation
                    )
                ),
                location = unknownLocation
            ),
            location = unknownLocation
        )
        
        statements.add(whileLoop)
        
        return BlockStatement(statements, unknownLocation)
    }
    
    /**
     * 展开 defProperty 宏
     */
    private fun expandDefPropertyMacro(
        bindings: Map<String, ASTNode>,
        context: MacroExpansionContext
    ): ASTNode {
        val name = bindings["name"] as? LiteralExpression.StringLiteral
            ?: throw IllegalArgumentException("defProperty 宏的第一个参数必须是字符串字面量")
        
        val getter = bindings["getter"] as? Expression
            ?: throw IllegalArgumentException("defProperty 宏的第二个参数必须是表达式")
        
        val setter = bindings["setter"] as? Expression
        
        // 创建属性定义对象
        val propertyDefinition = ObjectExpression(
            properties = listOfNotNull(
                ObjectProperty(
                    key = LiteralExpression.StringLiteral("get", unknownLocation),
                    value = getter
                ),
                setter?.let { 
                    ObjectProperty(
                        key = LiteralExpression.StringLiteral("set", unknownLocation),
                        value = it
                    )
                },
                ObjectProperty(
                    key = LiteralExpression.StringLiteral("enumerable", unknownLocation),
                    value = LiteralExpression.BooleanLiteral(true, unknownLocation)
                ),
                ObjectProperty(
                    key = LiteralExpression.StringLiteral("configurable", unknownLocation),
                    value = LiteralExpression.BooleanLiteral(true, unknownLocation)
                )
            ),
            location = unknownLocation
        )
        
        // 创建 Object.defineProperty 调用
        return ExpressionStatement(
            expression = CallExpression(
                function = MemberExpression(
                    target = IdentifierExpression("Object", unknownLocation),
                    property = "defineProperty",
                    location = unknownLocation
                ),
                arguments = listOf(
                    Argument(value = IdentifierExpression("this", unknownLocation)),
                    Argument(value = LiteralExpression.StringLiteral(name.value, unknownLocation)),
                    Argument(value = propertyDefinition)
                ),
                location = unknownLocation
            ),
            location = unknownLocation
        )
    }
    
    /**
     * 展开 lazy 宏
     */
    private fun expandLazyMacro(
        bindings: Map<String, ASTNode>,
        context: MacroExpansionContext
    ): ASTNode {
        val expression = bindings["expression"] as? Expression
            ?: throw IllegalArgumentException("lazy 宏的参数必须是表达式")
        
        val cacheVar = "_lazy_cache_${context.expansionDepth}"
        val computedVar = "_lazy_computed_${context.expansionDepth}"
        
        // 创建懒加载函数
        return LambdaExpression(
            parameters = emptyList(),
            body = ConditionalExpression(
                condition = UnaryExpression(
                    operator = UnaryOperator.LOGICAL_NOT,
                    operand = IdentifierExpression(computedVar, unknownLocation),
                    location = unknownLocation
                ),
                thenExpression = BlockStatement(
                    statements = listOf(
                        ExpressionStatement(
                            expression = BinaryExpression(
                                left = IdentifierExpression(cacheVar, unknownLocation),
                                operator = BinaryOperator.ASSIGN,
                                right = expression,
                                location = unknownLocation
                            ),
                            location = unknownLocation
                        ),
                        ExpressionStatement(
                            expression = BinaryExpression(
                                left = IdentifierExpression(computedVar, unknownLocation),
                                operator = BinaryOperator.ASSIGN,
                                right = LiteralExpression.BooleanLiteral(true, unknownLocation),
                                location = unknownLocation
                            ),
                            location = unknownLocation
                        )
                    ),
                    location = unknownLocation
                ) as Expression,
                elseExpression = IdentifierExpression(cacheVar, unknownLocation),
                location = unknownLocation
            ),
            location = unknownLocation
        )
    }
}
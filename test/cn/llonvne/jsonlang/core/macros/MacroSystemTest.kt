package cn.llonvne.jsonlang.core.macros

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.errors.CompilerError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach


class MacroSystemTest {
    
    private lateinit var errorReporter: ErrorReporter
    private lateinit var macroSystem: MacroSystem
    
    @BeforeEach
    fun setUp() {
        errorReporter = TestErrorReporter()
        macroSystem = MacroSystem(errorReporter)
    }
    
    @Test
    fun `test macro registration`() {
        val macro = createSimpleMacro("testMacro", listOf("param1"))
        macroSystem.registerMacro(macro)
        
        assert(macroSystem.hasMacro("testMacro"))
        assert(!macroSystem.hasMacro("nonExistentMacro"))
    }
    
    @Test
    fun `test simple macro expansion`() {
        val macro = createSimpleMacro("greet", listOf("name"))
        macroSystem.registerMacro(macro)
        
        val macroCall = MacroCall(
            name = "greet",
            arguments = listOf(
                LiteralExpression.StringLiteral("World", SourceLocation.unknown())
            ),
            location = SourceLocation.unknown()
        )
        
        val context = MacroExpansionContext("test")
        val result = macroSystem.expandMacro(macroCall, context)
        
        assert(result != null)
        assert(result is LiteralExpression.StringLiteral)
        assert((result as LiteralExpression.StringLiteral).value == "Hello, World!")
    }
    
    @Test
    fun `test macro parameter mismatch`() {
        val macro = createSimpleMacro("testMacro", listOf("param1", "param2"))
        macroSystem.registerMacro(macro)
        
        val macroCall = MacroCall(
            name = "testMacro",
            arguments = listOf(
                LiteralExpression.StringLiteral("arg1", SourceLocation.unknown())
            ),
            location = SourceLocation.unknown()
        )
        
        val context = MacroExpansionContext("test")
        val result = macroSystem.expandMacro(macroCall, context)
        
        assert(result == null)
        assert((errorReporter as TestErrorReporter).hasError("MACRO_ARGUMENT_MISMATCH"))
    }
    
    @Test
    fun `test recursive macro expansion detection`() {
        val recursiveMacro = MacroDeclaration(
            name = "recursive",
            parameters = listOf("x"),
            expansion = MacroExpansion.TemplateExpansion(
                CallExpression(
                    function = IdentifierExpression("recursive", SourceLocation.unknown()),
                    arguments = listOf(
                        Argument(value = IdentifierExpression("x", SourceLocation.unknown()))
                    ),
                    location = SourceLocation.unknown()
                )
            ),
            location = SourceLocation.unknown()
        )
        
        macroSystem.registerMacro(recursiveMacro)
        
        val macroCall = MacroCall(
            name = "recursive",
            arguments = listOf(
                LiteralExpression.IntLiteral(1, SourceLocation.unknown())
            ),
            location = SourceLocation.unknown()
        )
        
        val context = MacroExpansionContext("test")
        val result = macroSystem.expandMacro(macroCall, context)
        
        assert(result == null)
        assert((errorReporter as TestErrorReporter).hasError("MACRO_RECURSIVE_EXPANSION"))
    }
    
    @Test
    fun `test template expansion with parameter substitution`() {
        val template = BinaryExpression(
            left = IdentifierExpression("x", SourceLocation.unknown()),
            operator = BinaryOperator.PLUS,
            right = IdentifierExpression("y", SourceLocation.unknown()),
            location = SourceLocation.unknown()
        )
        
        val macro = MacroDeclaration(
            name = "add",
            parameters = listOf("x", "y"),
            expansion = MacroExpansion.TemplateExpansion(template),
            location = SourceLocation.unknown()
        )
        
        macroSystem.registerMacro(macro)
        
        val macroCall = MacroCall(
            name = "add",
            arguments = listOf(
                LiteralExpression.IntLiteral(5, SourceLocation.unknown()),
                LiteralExpression.IntLiteral(3, SourceLocation.unknown())
            ),
            location = SourceLocation.unknown()
        )
        
        val context = MacroExpansionContext("test")
        val result = macroSystem.expandMacro(macroCall, context)
        
        assert(result != null)
        assert(result is BinaryExpression)
        val binaryExpr = result as BinaryExpression
        assert(binaryExpr.operator == BinaryOperator.PLUS)
        assert(binaryExpr.left is LiteralExpression.IntLiteral)
        assert(binaryExpr.right is LiteralExpression.IntLiteral)
        assert((binaryExpr.left as LiteralExpression.IntLiteral).value == 5L)
        assert((binaryExpr.right as LiteralExpression.IntLiteral).value == 3L)
    }
    
    @Test
    fun `test builtin assert macro`() {
        val assertMacro = BuiltinMacros.getAllBuiltinMacros().find { it.name == "assert" }
        assert(assertMacro != null)
        
        macroSystem.registerMacro(assertMacro!!)
        
        val macroCall = MacroCall(
            name = "assert",
            arguments = listOf(
                LiteralExpression.BooleanLiteral(false, SourceLocation.unknown()),
                LiteralExpression.StringLiteral("Test failed", SourceLocation.unknown())
            ),
            location = SourceLocation.unknown()
        )
        
        val context = MacroExpansionContext("test")
        val result = macroSystem.expandMacro(macroCall, context)
        
        assert(result != null)
        assert(result is ConditionalStatement)
    }
    
    @Test
    fun `test macro expansion context`() {
        val context = MacroExpansionContext(
            currentScope = "test",
            expansionDepth = 0
        )
        
        assert(context.currentScope == "test")
        assert(context.expansionDepth == 0)
        
        val incrementedContext = context.withIncrementedDepth()
        assert(incrementedContext.expansionDepth == 1)
        
        val scopedContext = context.withScope("newScope")
        assert(scopedContext.currentScope == "newScope")
        
        val variableContext = context.withVariable("x", LiteralExpression.IntLiteral(42, SourceLocation.unknown()))
        assert(variableContext.variables.containsKey("x"))
    }
    
    private fun createSimpleMacro(name: String, parameters: List<String>): MacroDeclaration {
        val template = LiteralExpression.StringLiteral(
            "Hello, \${${parameters.firstOrNull() ?: "World"}}!",
            SourceLocation.unknown()
        )
        
        return MacroDeclaration(
            name = name,
            parameters = parameters,
            expansion = MacroExpansion.TemplateExpansion(template),
            location = SourceLocation.unknown()
        )
    }
}

class MacroTemplateExpanderTest {
    
    @Test
    fun `test string template expansion`() {
        val bindings = mapOf(
            "name" to LiteralExpression.StringLiteral("Alice", SourceLocation.unknown()),
            "age" to LiteralExpression.IntLiteral(30, SourceLocation.unknown())
        )
        
        val context = MacroExpansionContext("test")
        val expander = MacroTemplateExpander(bindings, context)
        
        val template = LiteralExpression.StringLiteral(
            "Hello, \${name}! You are \${age} years old.",
            SourceLocation.unknown()
        )
        
        val result = expander.expand(template)
        assert(result is LiteralExpression.StringLiteral)
        assert((result as LiteralExpression.StringLiteral).value == "Hello, Alice! You are 30 years old.")
    }
    
    @Test
    fun `test identifier replacement`() {
        val bindings = mapOf(
            "x" to LiteralExpression.IntLiteral(42, SourceLocation.unknown())
        )
        
        val context = MacroExpansionContext("test")
        val expander = MacroTemplateExpander(bindings, context)
        
        val template = IdentifierExpression("x", SourceLocation.unknown())
        val result = expander.expand(template)
        
        assert(result is LiteralExpression.IntLiteral)
        assert((result as LiteralExpression.IntLiteral).value == 42L)
    }
    
    @Test
    fun `test binary expression expansion`() {
        val bindings = mapOf(
            "a" to LiteralExpression.IntLiteral(10, SourceLocation.unknown()),
            "b" to LiteralExpression.IntLiteral(20, SourceLocation.unknown())
        )
        
        val context = MacroExpansionContext("test")
        val expander = MacroTemplateExpander(bindings, context)
        
        val template = BinaryExpression(
            left = IdentifierExpression("a", SourceLocation.unknown()),
            operator = BinaryOperator.PLUS,
            right = IdentifierExpression("b", SourceLocation.unknown()),
            location = SourceLocation.unknown()
        )
        
        val result = expander.expand(template)
        assert(result is BinaryExpression)
        val binaryExpr = result as BinaryExpression
        assert(binaryExpr.left is LiteralExpression.IntLiteral)
        assert(binaryExpr.right is LiteralExpression.IntLiteral)
        assert((binaryExpr.left as LiteralExpression.IntLiteral).value == 10L)
        assert((binaryExpr.right as LiteralExpression.IntLiteral).value == 20L)
    }
}

class MacroProcessorTest {
    
    private lateinit var errorReporter: ErrorReporter
    private lateinit var macroSystem: MacroSystem
    private lateinit var macroProcessor: MacroProcessor
    
    @BeforeEach
    fun setUp() {
        errorReporter = TestErrorReporter()
        macroSystem = MacroSystem(errorReporter)
        macroProcessor = MacroProcessor(macroSystem, errorReporter)
    }
    
    @Test
    fun `test program macro processing`() {
        val simpleMacro = MacroDeclaration(
            name = "double",
            parameters = listOf("x"),
            expansion = MacroExpansion.TemplateExpansion(
                BinaryExpression(
                    left = IdentifierExpression("x", SourceLocation.unknown()),
                    operator = BinaryOperator.MULTIPLY,
                    right = LiteralExpression.IntLiteral(2, SourceLocation.unknown()),
                    location = SourceLocation.unknown()
                )
            ),
            location = SourceLocation.unknown()
        )
        
        val function = FunctionDeclaration(
            name = "test",
            modifiers = FunctionModifiers(),
            parameters = emptyList(),
            returnType = cn.llonvne.jsonlang.core.types.PrimitiveType.IntType,
            body = listOf(
                JumpStatement.ReturnStatement(
                    value = CallExpression(
                        function = IdentifierExpression("double", SourceLocation.unknown()),
                        arguments = listOf(
                            Argument(value = LiteralExpression.IntLiteral(5, SourceLocation.unknown()))
                        ),
                        location = SourceLocation.unknown()
                    ),
                    location = SourceLocation.unknown()
                )
            ),
            location = SourceLocation.unknown()
        )
        
        val program = Program(
            metadata = Metadata("test", "1.0.0", location = SourceLocation.unknown()),
            imports = emptyList(),
            exports = emptyList(),
            types = emptyList(),
            constants = emptyList(),
            functions = listOf(function),
            macros = listOf(simpleMacro),
            dsls = emptyList(),
            location = SourceLocation.unknown()
        )
        
        val processedProgram = macroProcessor.processProgram(program)
        
        // 验证宏已被展开
        val processedFunction = processedProgram.functions.first()
        val returnStatement = processedFunction.body.first() as JumpStatement.ReturnStatement
        assert(returnStatement.value is BinaryExpression)
        
        val binaryExpr = returnStatement.value as BinaryExpression
        assert(binaryExpr.operator == BinaryOperator.MULTIPLY)
        assert(binaryExpr.left is LiteralExpression.IntLiteral)
        assert(binaryExpr.right is LiteralExpression.IntLiteral)
        assert((binaryExpr.left as LiteralExpression.IntLiteral).value == 5L)
        assert((binaryExpr.right as LiteralExpression.IntLiteral).value == 2L)
    }
}

/**
 * 测试用错误报告器
 */
class TestErrorReporter : ErrorReporter {
    private val errors = mutableListOf<CompilerError>()
    
    override fun report(error: CompilerError) {
        errors.add(error)
    }
    
    override fun getErrors(): List<CompilerError> = errors.toList()
    
    override fun getErrorCount(): Int = errors.count { it.isFatal() }
    
    override fun getWarningCount(): Int = errors.count { it.isWarning() }
    
    override fun hasErrors(): Boolean = errors.any { it.isFatal() }
    
    override fun hasWarnings(): Boolean = errors.any { it.isWarning() }
    
    override fun clear() {
        errors.clear()
    }
    
    fun hasError(code: String): Boolean = errors.any { it.code == code }
}
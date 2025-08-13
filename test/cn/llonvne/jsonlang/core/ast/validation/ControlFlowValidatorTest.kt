package cn.llonvne.jsonlang.core.ast.validation

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.errors.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class ControlFlowValidatorTest {
    
    private lateinit var errorReporter: TestErrorReporter
    private lateinit var validator: ControlFlowValidator
    private val testLocation = SourceLocation("test.json", 1, 1, 1, 1, 0, 0)
    
    @BeforeEach
    fun setUp() {
        errorReporter = TestErrorReporter()
        validator = ControlFlowValidator(errorReporter)
    }
    
    @Test
    fun `test valid conditional statement`() {
        val conditional = ConditionalStatement(
            condition = LiteralExpression.BooleanLiteral(true, testLocation),
            thenBranch = ExpressionStatement(
                LiteralExpression.StringLiteral("then", testLocation),
                testLocation
            ),
            elseBranch = ExpressionStatement(
                LiteralExpression.StringLiteral("else", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        validator.visitConditionalStatement(conditional)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test valid for loop`() {
        val forLoop = LoopStatement.ForLoop(
            iterator = "item",
            iterable = ArrayExpression(
                elements = listOf(
                    LiteralExpression.IntLiteral(1, testLocation),
                    LiteralExpression.IntLiteral(2, testLocation)
                ),
                location = testLocation
            ),
            body = ExpressionStatement(
                IdentifierExpression("item", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        validator.visitForLoop(forLoop)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test valid while loop`() {
        val whileLoop = LoopStatement.WhileLoop(
            condition = LiteralExpression.BooleanLiteral(true, testLocation),
            body = ExpressionStatement(
                LiteralExpression.StringLiteral("body", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        validator.visitWhileLoop(whileLoop)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test return statement in function context`() {
        val functionDecl = FunctionDeclaration(
            name = "testFunction",
            modifiers = FunctionModifiers(),
            generics = emptyList(),
            parameters = emptyList(),
            returnType = PrimitiveType.StringType,
            body = listOf(
                JumpStatement.ReturnStatement(
                    value = LiteralExpression.StringLiteral("result", testLocation),
                    location = testLocation
                )
            ),
            location = testLocation
        )
        
        validator.visitFunctionDeclaration(functionDecl)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test return statement outside function should report error`() {
        val returnStatement = JumpStatement.ReturnStatement(
            value = LiteralExpression.StringLiteral("result", testLocation),
            location = testLocation
        )
        
        validator.visitReturnStatement(returnStatement)
        
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals("RETURN_OUTSIDE_FUNCTION", errorReporter.reportedErrors[0].code)
    }
    
    @Test
    fun `test break statement in loop context`() {
        val forLoop = LoopStatement.ForLoop(
            iterator = "i",
            iterable = ArrayExpression(emptyList(), testLocation),
            body = BlockStatement(
                statements = listOf(
                    JumpStatement.BreakStatement(null, testLocation)
                ),
                location = testLocation
            ),
            location = testLocation
        )
        
        validator.visitForLoop(forLoop)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test break statement outside loop should report error`() {
        val breakStatement = JumpStatement.BreakStatement(null, testLocation)
        
        validator.visitBreakStatement(breakStatement)
        
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals("BREAK_OUTSIDE_LOOP", errorReporter.reportedErrors[0].code)
    }
    
    @Test
    fun `test continue statement in loop context`() {
        val whileLoop = LoopStatement.WhileLoop(
            condition = LiteralExpression.BooleanLiteral(true, testLocation),
            body = BlockStatement(
                statements = listOf(
                    JumpStatement.ContinueStatement(null, testLocation)
                ),
                location = testLocation
            ),
            location = testLocation
        )
        
        validator.visitWhileLoop(whileLoop)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test continue statement outside loop should report error`() {
        val continueStatement = JumpStatement.ContinueStatement(null, testLocation)
        
        validator.visitContinueStatement(continueStatement)
        
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals("CONTINUE_OUTSIDE_LOOP", errorReporter.reportedErrors[0].code)
    }
    
    @Test
    fun `test valid try-catch statement`() {
        val tryCatch = TryCatchStatement(
            tryBlock = BlockStatement(
                statements = listOf(
                    ExpressionStatement(
                        LiteralExpression.StringLiteral("try", testLocation),
                        testLocation
                    )
                ),
                location = testLocation
            ),
            catchClauses = listOf(
                CatchClause(
                    exceptionType = PrimitiveType.AnyType, // Using AnyType as placeholder for Exception
                    variableName = "e",
                    handler = BlockStatement(
                        statements = listOf(
                            ExpressionStatement(
                                LiteralExpression.StringLiteral("catch", testLocation),
                                testLocation
                            )
                        ),
                        location = testLocation
                    ),
                    location = testLocation
                )
            ),
            finallyBlock = BlockStatement(
                statements = listOf(
                    ExpressionStatement(
                        LiteralExpression.StringLiteral("finally", testLocation),
                        testLocation
                    )
                ),
                location = testLocation
            ),
            location = testLocation
        )
        
        validator.visitTryCatchStatement(tryCatch)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test valid match statement`() {
        val matchStatement = MatchStatement(
            expression = IdentifierExpression("value", testLocation),
            cases = listOf(
                MatchCase(
                    pattern = Pattern.LiteralPattern(1),
                    guard = null,
                    body = ExpressionStatement(
                        LiteralExpression.StringLiteral("one", testLocation),
                        testLocation
                    ),
                    location = testLocation
                ),
                MatchCase(
                    pattern = Pattern.LiteralPattern(2),
                    guard = null,
                    body = ExpressionStatement(
                        LiteralExpression.StringLiteral("two", testLocation),
                        testLocation
                    ),
                    location = testLocation
                )
            ),
            defaultCase = ExpressionStatement(
                LiteralExpression.StringLiteral("default", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        validator.visitMatchStatement(matchStatement)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test nested control flow structures`() {
        val nestedStructure = LoopStatement.ForLoop(
            iterator = "i",
            iterable = ArrayExpression(
                elements = listOf(
                    LiteralExpression.IntLiteral(1, testLocation),
                    LiteralExpression.IntLiteral(2, testLocation)
                ),
                location = testLocation
            ),
            body = BlockStatement(
                statements = listOf(
                    ConditionalStatement(
                        condition = LiteralExpression.BooleanLiteral(true, testLocation),
                        thenBranch = JumpStatement.BreakStatement(null, testLocation),
                        elseBranch = JumpStatement.ContinueStatement(null, testLocation),
                        location = testLocation
                    )
                ),
                location = testLocation
            ),
            location = testLocation
        )
        
        validator.visitForLoop(nestedStructure)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test function with multiple return statements`() {
        val functionDecl = FunctionDeclaration(
            name = "multiReturn",
            modifiers = FunctionModifiers(),
            generics = emptyList(),
            parameters = emptyList(),
            returnType = PrimitiveType.IntType,
            body = listOf(
                ConditionalStatement(
                    condition = LiteralExpression.BooleanLiteral(true, testLocation),
                    thenBranch = JumpStatement.ReturnStatement(
                        value = LiteralExpression.IntLiteral(1, testLocation),
                        location = testLocation
                    ),
                    elseBranch = JumpStatement.ReturnStatement(
                        value = LiteralExpression.IntLiteral(2, testLocation),
                        location = testLocation
                    ),
                    location = testLocation
                )
            ),
            location = testLocation
        )
        
        validator.visitFunctionDeclaration(functionDecl)
        
        assertTrue(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test unit function with return value should report error`() {
        val functionDecl = FunctionDeclaration(
            name = "unitFunction",
            modifiers = FunctionModifiers(),
            generics = emptyList(),
            parameters = emptyList(),
            returnType = PrimitiveType.UnitType,
            body = listOf(
                JumpStatement.ReturnStatement(
                    value = LiteralExpression.StringLiteral("unexpected", testLocation),
                    location = testLocation
                )
            ),
            location = testLocation
        )
        
        validator.visitFunctionDeclaration(functionDecl)
        
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals("UNEXPECTED_RETURN_VALUE", errorReporter.reportedErrors[0].code)
    }
    
    @Test
    fun `test non-unit function without return value should report error`() {
        val functionDecl = FunctionDeclaration(
            name = "nonUnitFunction",
            modifiers = FunctionModifiers(),
            generics = emptyList(),
            parameters = emptyList(),
            returnType = PrimitiveType.StringType,
            body = listOf(
                JumpStatement.ReturnStatement(
                    value = null,
                    location = testLocation
                )
            ),
            location = testLocation
        )
        
        validator.visitFunctionDeclaration(functionDecl)
        
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals("MISSING_RETURN_VALUE", errorReporter.reportedErrors[0].code)
    }
    
    // 测试错误报告器
    class TestErrorReporter : cn.llonvne.jsonlang.core.errors.ErrorReporter {
        private val _errors = mutableListOf<cn.llonvne.jsonlang.core.errors.CompilerError>()
        val reportedErrors: List<cn.llonvne.jsonlang.core.errors.CompilerError> get() = _errors
        
        override fun report(error: cn.llonvne.jsonlang.core.errors.CompilerError) {
            _errors.add(error)
        }
        
        override fun getErrors(): List<cn.llonvne.jsonlang.core.errors.CompilerError> = _errors.toList()
        
        override fun getErrorCount(): Int = _errors.count { it.isFatal() }
        
        override fun getWarningCount(): Int = _errors.count { it.isWarning() }
        
        override fun hasErrors(): Boolean = _errors.any { it.isFatal() }
        
        override fun hasWarnings(): Boolean = _errors.any { it.isWarning() }
        
        override fun clear() {
            _errors.clear()
        }
    }
}
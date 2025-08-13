package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.errors.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach


class ControlFlowProcessorTest {
    
    private lateinit var errorReporter: TestErrorReporter
    private lateinit var processor: ControlFlowProcessor
    private val testLocation = SourceLocation("test.json", 1, 1, 1, 1, 0, 0)
    
    @BeforeEach
    fun setUp() {
        errorReporter = TestErrorReporter()
        processor = ControlFlowProcessor(errorReporter)
    }
    
    @Test
    fun `test conditional statement processing`() {
        val condition = LiteralExpression.BooleanLiteral(true, testLocation)
        val thenBranch = ExpressionStatement(
            LiteralExpression.StringLiteral("then", testLocation),
            testLocation
        )
        val elseBranch = ExpressionStatement(
            LiteralExpression.StringLiteral("else", testLocation),
            testLocation
        )
        
        val conditional = ConditionalStatement(
            condition = condition,
            thenBranch = thenBranch,
            elseBranch = elseBranch,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext()
        val result = processor.processConditionalStatement(conditional, context)
        
        assert(result != null)
        assert(result.condition == conditional.condition)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test for loop processing`() {
        val iterable = ArrayExpression(
            elements = listOf(
                LiteralExpression.IntLiteral(1, testLocation),
                LiteralExpression.IntLiteral(2, testLocation),
                LiteralExpression.IntLiteral(3, testLocation)
            ),
            location = testLocation
        )
        
        val body = ExpressionStatement(
            IdentifierExpression("item", testLocation),
            testLocation
        )
        
        val forLoop = LoopStatement.ForLoop(
            iterator = "item",
            iterable = iterable,
            body = body,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext()
        val result = processor.processForLoop(forLoop, context)
        
        assert(result != null)
        assert(result.iterator == "item")
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test while loop processing`() {
        val condition = LiteralExpression.BooleanLiteral(true, testLocation)
        val body = ExpressionStatement(
            LiteralExpression.StringLiteral("loop body", testLocation),
            testLocation
        )
        
        val whileLoop = LoopStatement.WhileLoop(
            condition = condition,
            body = body,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext()
        val result = processor.processWhileLoop(whileLoop, context)
        
        assert(result != null)
        assert(result.condition == condition)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test return statement in function context`() {
        val returnValue = LiteralExpression.StringLiteral("return value", testLocation)
        val returnStatement = JumpStatement.ReturnStatement(
            value = returnValue,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext(
            inFunction = true,
            expectedReturnType = PrimitiveType.StringType
        )
        
        val result = processor.processReturnStatement(returnStatement, context)
        
        assert(result != null)
        assert(result.value == returnValue)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test return statement outside function should report error`() {
        val returnStatement = JumpStatement.ReturnStatement(
            value = LiteralExpression.StringLiteral("return value", testLocation),
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext(inFunction = false)
        
        processor.processReturnStatement(returnStatement, context)
        
        assert(errorReporter.reportedErrors.size == 1)
        assert(errorReporter.reportedErrors[0].code == "RETURN_OUTSIDE_FUNCTION")
    }
    
    @Test
    fun `test break statement in loop context`() {
        val breakStatement = JumpStatement.BreakStatement(
            label = null,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext(inLoop = true)
        
        val result = processor.processBreakStatement(breakStatement, context)
        
        assert(result != null)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test break statement outside loop should report error`() {
        val breakStatement = JumpStatement.BreakStatement(
            label = null,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext(inLoop = false)
        
        processor.processBreakStatement(breakStatement, context)
        
        assert(errorReporter.reportedErrors.size == 1)
        assert(errorReporter.reportedErrors[0].code == "BREAK_OUTSIDE_LOOP")
    }
    
    @Test
    fun `test continue statement in loop context`() {
        val continueStatement = JumpStatement.ContinueStatement(
            label = null,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext(inLoop = true)
        
        val result = processor.processContinueStatement(continueStatement, context)
        
        assert(result != null)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test continue statement outside loop should report error`() {
        val continueStatement = JumpStatement.ContinueStatement(
            label = null,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext(inLoop = false)
        
        processor.processContinueStatement(continueStatement, context)
        
        assert(errorReporter.reportedErrors.size == 1)
        assert(errorReporter.reportedErrors[0].code == "CONTINUE_OUTSIDE_LOOP")
    }
    
    @Test
    fun `test match statement processing`() {
        val expression = IdentifierExpression("value", testLocation)
        val pattern1 = Pattern.LiteralPattern(1)
        val pattern2 = Pattern.LiteralPattern(2)
        
        val case1 = MatchCase(
            pattern = pattern1,
            guard = null,
            body = ExpressionStatement(
                LiteralExpression.StringLiteral("case 1", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        val case2 = MatchCase(
            pattern = pattern2,
            guard = null,
            body = ExpressionStatement(
                LiteralExpression.StringLiteral("case 2", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        val matchStatement = MatchStatement(
            expression = expression,
            cases = listOf(case1, case2),
            defaultCase = ExpressionStatement(
                LiteralExpression.StringLiteral("default", testLocation),
                testLocation
            ),
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext()
        val result = processor.processMatchStatement(matchStatement, context)
        
        assert(result != null)
        assert(result.cases.size == 2)
        assert(result.defaultCase != null)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test try-catch statement processing`() {
        val tryBlock = BlockStatement(
            statements = listOf(
                ExpressionStatement(
                    LiteralExpression.StringLiteral("try block", testLocation),
                    testLocation
                )
            ),
            location = testLocation
        )
        
        val catchClause = CatchClause(
            exceptionType = PrimitiveType.AnyType, // Using AnyType as a placeholder for Exception
            variableName = "e",
            handler = BlockStatement(
                statements = listOf(
                    ExpressionStatement(
                        LiteralExpression.StringLiteral("catch block", testLocation),
                        testLocation
                    )
                ),
                location = testLocation
            ),
            location = testLocation
        )
        
        val finallyBlock = BlockStatement(
            statements = listOf(
                ExpressionStatement(
                    LiteralExpression.StringLiteral("finally block", testLocation),
                    testLocation
                )
            ),
            location = testLocation
        )
        
        val tryCatchStatement = TryCatchStatement(
            tryBlock = tryBlock,
            catchClauses = listOf(catchClause),
            finallyBlock = finallyBlock,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext()
        val result = processor.processTryCatchStatement(tryCatchStatement, context)
        
        assert(result != null)
        assert(result.catchClauses.size == 1)
        assert(result.finallyBlock != null)
        assert(errorReporter.reportedErrors.isEmpty())
    }
    
    @Test
    fun `test block statement processing`() {
        val statements = listOf(
            VariableDeclaration(
                name = "x",
                type = PrimitiveType.IntType,
                value = LiteralExpression.IntLiteral(42, testLocation),
                location = testLocation
            ),
            ExpressionStatement(
                IdentifierExpression("x", testLocation),
                testLocation
            )
        )
        
        val blockStatement = BlockStatement(
            statements = statements,
            location = testLocation
        )
        
        val context = ControlFlowProcessor.ControlFlowContext()
        val result = processor.processBlockStatement(blockStatement, context)
        
        assert(result != null)
        assert(result.statements.size == 2)
        assert(errorReporter.reportedErrors.isEmpty())
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
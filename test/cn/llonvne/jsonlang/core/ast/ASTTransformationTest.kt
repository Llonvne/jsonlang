package cn.llonvne.jsonlang.core.ast

import cn.llonvne.jsonlang.core.ast.optimizations.*
import cn.llonvne.jsonlang.core.ast.validation.*
import cn.llonvne.jsonlang.core.errors.ErrorReporter
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.types.Type
import cn.llonvne.jsonlang.core.types.PrimitiveType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class ASTTransformationTest {
    
    private lateinit var errorReporter: ErrorReporter
    private lateinit var sourceLocation: SourceLocation
    
    @BeforeEach
    fun setUp() {
        errorReporter = TestErrorReporter()
        sourceLocation = SourceLocation("test.lang.json", 1, 1, 1, 1, 0, 0)
    }
    
    @Test
    fun testConstantFolding() {
        // 创建常量折叠变换
        val constantFolding = ConstantFoldingTransformation()
        val context = TransformationContext(createTestProgram(), errorReporter)
        
        // 测试整数加法折叠
        val intAddition = BinaryExpression(
            left = LiteralExpression.IntLiteral(5, sourceLocation),
            operator = BinaryOperator.PLUS,
            right = LiteralExpression.IntLiteral(3, sourceLocation),
            location = sourceLocation
        )
        
        val result = constantFolding.transform(intAddition, context)
        assertTrue(result is LiteralExpression.IntLiteral)
        assertEquals(8L, (result as LiteralExpression.IntLiteral).value)
        
        // 测试浮点数乘法折叠
        val floatMultiplication = BinaryExpression(
            left = LiteralExpression.FloatLiteral(2.5, sourceLocation),
            operator = BinaryOperator.MULTIPLY,
            right = LiteralExpression.FloatLiteral(4.0, sourceLocation),
            location = sourceLocation
        )
        
        val floatResult = constantFolding.transform(floatMultiplication, context)
        assertTrue(floatResult is LiteralExpression.FloatLiteral)
        assertEquals(10.0, (floatResult as LiteralExpression.FloatLiteral).value, 0.001)
        
        // 测试字符串连接折叠
        val stringConcatenation = BinaryExpression(
            left = LiteralExpression.StringLiteral("Hello, ", sourceLocation),
            operator = BinaryOperator.PLUS,
            right = LiteralExpression.StringLiteral("World!", sourceLocation),
            location = sourceLocation
        )
        
        val stringResult = constantFolding.transform(stringConcatenation, context)
        assertTrue(stringResult is LiteralExpression.StringLiteral)
        assertEquals("Hello, World!", (stringResult as LiteralExpression.StringLiteral).value)
        
        // 测试除零保护
        val divisionByZero = BinaryExpression(
            left = LiteralExpression.IntLiteral(10, sourceLocation),
            operator = BinaryOperator.DIVIDE,
            right = LiteralExpression.IntLiteral(0, sourceLocation),
            location = sourceLocation
        )
        
        val divisionResult = constantFolding.transform(divisionByZero, context)
        // 应该保持原样，不进行折叠
        assertTrue(divisionResult is BinaryExpression)
    }
    
    @Test
    fun testDeadCodeElimination() {
        val deadCodeElimination = DeadCodeEliminationTransformation()
        val context = TransformationContext(createTestProgram(), errorReporter)
        
        // 测试条件语句中的死代码消除
        val conditionalWithConstant = ConditionalStatement(
            condition = LiteralExpression.BooleanLiteral(true, sourceLocation),
            thenBranch = ExpressionStatement(
                LiteralExpression.StringLiteral("executed", sourceLocation),
                sourceLocation
            ),
            elseBranch = ExpressionStatement(
                LiteralExpression.StringLiteral("never executed", sourceLocation),
                sourceLocation
            ),
            location = sourceLocation
        )
        
        val result = deadCodeElimination.transform(conditionalWithConstant, context)
        // 应该只保留 then 分支
        assertTrue(result is ExpressionStatement)
        val expr = (result as ExpressionStatement).expression
        assertTrue(expr is LiteralExpression.StringLiteral)
        assertEquals("executed", (expr as LiteralExpression.StringLiteral).value)
    }
    
    @Test
    fun testTransformationEngine() {
        val engine = TransformationEngineBuilder()
            .withErrorReporter(errorReporter)
            .addTransformation(ConstantFoldingTransformation())
            .addTransformation(DeadCodeEliminationTransformation())
            .build()
        
        val program = createTestProgram()
        val transformedProgram = engine.transform(program)
        
        assertNotNull(transformedProgram)
        assertEquals(program.metadata.name, transformedProgram.metadata.name)
    }
    
    @Test
    fun testStructuralValidator() {
        val validator = StructuralValidator()
        val context = ValidationContext(createTestProgram(), errorReporter)
        
        // 测试有效的程序结构
        val validProgram = createTestProgram()
        val result = validator.validate(validProgram, context)
        assertTrue(result.isValid)
        
        // 测试无效的程序结构（空函数名）
        val invalidFunction = FunctionDeclaration(
            name = "", // 空函数名
            modifiers = FunctionModifiers(),
            parameters = emptyList(),
            returnType = createTestType(),
            body = emptyList(),
            location = sourceLocation
        )
        
        val invalidResult = validator.validate(invalidFunction, context)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.any { it.code == "EMPTY_FUNCTION_NAME" })
    }
    
    @Test
    fun testSemanticValidator() {
        val validator = SemanticValidator()
        val context = ValidationContext(createTestProgram(), errorReporter)
        
        // 测试未定义的标识符
        val undefinedIdentifier = IdentifierExpression("undefinedVariable", sourceLocation)
        val result = validator.validate(undefinedIdentifier, context)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "UNDEFINED_SYMBOL" })
    }
    
    @Test
    fun testValidationEngine() {
        val engine = ValidationEngineBuilder()
            .withErrorReporter(errorReporter)
            .addStructuralValidation()
            .addSemanticValidation()
            .build()
        
        val program = createTestProgram()
        val result = engine.validate(program)
        
        assertNotNull(result)
        // 对于有效的测试程序，验证应该通过
        assertTrue(result.isValid)
    }
    
    @Test
    fun testValidationUtils() {
        val program = createTestProgram()
        val report = ValidationUtils.validateWithReport(program, errorReporter)
        
        assertNotNull(report)
        assertTrue(report.isValid)
        assertTrue(report.executedValidators.isNotEmpty())
        
        val summary = report.generateSummary()
        assertTrue(summary.contains("AST 验证报告"))
        assertTrue(summary.contains("验证状态: 通过"))
    }
    
    // 辅助方法
    
    private fun createTestProgram(): Program {
        return Program(
            metadata = Metadata(
                name = "TestProgram",
                version = "1.0.0",
                location = sourceLocation
            ),
            imports = emptyList(),
            exports = emptyList(),
            types = emptyList(),
            constants = emptyList(),
            functions = listOf(createTestFunction()),
            macros = emptyList(),
            dsls = emptyList(),
            location = sourceLocation
        )
    }
    
    private fun createTestFunction(): FunctionDeclaration {
        return FunctionDeclaration(
            name = "testFunction",
            modifiers = FunctionModifiers(),
            parameters = listOf(
                Parameter(
                    name = "param1",
                    type = createTestType(),
                    optional = false
                )
            ),
            returnType = createTestType(),
            body = listOf(
                JumpStatement.ReturnStatement(
                    value = IdentifierExpression("param1", sourceLocation),
                    location = sourceLocation
                )
            ),
            location = sourceLocation
        )
    }
    
    private fun createTestType(): Type {
        // 使用一个简单的基础类型
        return PrimitiveType.StringType
    }
}

/**
 * 测试用的错误报告器
 */
class TestErrorReporter : ErrorReporter {
    private val errors = mutableListOf<cn.llonvne.jsonlang.core.errors.CompilerError>()
    
    override fun report(error: cn.llonvne.jsonlang.core.errors.CompilerError) {
        errors.add(error)
    }
    
    override fun getErrors(): List<cn.llonvne.jsonlang.core.errors.CompilerError> = errors.toList()
    
    override fun getErrorCount(): Int = errors.count { it.isFatal() }
    
    override fun getWarningCount(): Int = errors.count { it.isWarning() }
    
    override fun hasErrors(): Boolean = errors.any { it.isFatal() }
    
    override fun hasWarnings(): Boolean = errors.any { it.isWarning() }
    
    override fun clear() {
        errors.clear()
    }
}
package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.annotations.Annotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class GoBackendTest {
    
    private lateinit var backend: GoBackend
    private val dummyLocation = SourceLocation("test.json", 1, 1, 1, 1, 0, 0)
    
    @BeforeEach
    fun setUp() {
        backend = GoBackend()
    }
    
    @Test
    fun `test backend properties`() {
        assertEquals("go", backend.getName())
        assertEquals("Go", backend.getTargetLanguage())
        assertEquals("go", backend.getFileExtension())
        assertTrue(backend.supportsAsync())
        assertTrue(backend.supportsGenerics())
    }
    
    @Test
    fun `test type mapping`() {
        val typeMapping = backend.getTypeMapping()
        
        // 基础类型测试
        assertEquals("string", typeMapping.mapPrimitiveType(PrimitiveType.StringType))
        assertEquals("*string", typeMapping.mapPrimitiveType(PrimitiveType.NullableStringType))
        assertEquals("int", typeMapping.mapPrimitiveType(PrimitiveType.IntType))
        assertEquals("*int", typeMapping.mapPrimitiveType(PrimitiveType.NullableIntType))
        assertEquals("int64", typeMapping.mapPrimitiveType(PrimitiveType.LongType))
        assertEquals("float64", typeMapping.mapPrimitiveType(PrimitiveType.DoubleType))
        assertEquals("bool", typeMapping.mapPrimitiveType(PrimitiveType.BooleanType))
        assertEquals("any", typeMapping.mapPrimitiveType(PrimitiveType.AnyType))
        
        // 复合类型测试
        val arrayType = CompositeType.ArrayType(PrimitiveType.IntType)
        assertEquals("[]int", typeMapping.mapCompositeType(arrayType))
        
        val objectType = CompositeType.ObjectType(PrimitiveType.StringType, PrimitiveType.IntType)
        assertEquals("map[string]int", typeMapping.mapCompositeType(objectType))
        
        val functionType = CompositeType.FunctionType(
            listOf(PrimitiveType.IntType, PrimitiveType.StringType),
            PrimitiveType.BooleanType
        )
        assertEquals("func(int, string) bool", typeMapping.mapCompositeType(functionType))
        
        val optionalType = CompositeType.OptionalType(PrimitiveType.StringType)
        assertEquals("*string", typeMapping.mapCompositeType(optionalType))
    }
    
    @Test
    fun `test default values`() {
        val typeMapping = backend.getTypeMapping()
        
        assertEquals("\"\"", typeMapping.getDefaultValue(PrimitiveType.StringType))
        assertEquals("0", typeMapping.getDefaultValue(PrimitiveType.IntType))
        assertEquals("0.0", typeMapping.getDefaultValue(PrimitiveType.DoubleType))
        assertEquals("false", typeMapping.getDefaultValue(PrimitiveType.BooleanType))
        assertEquals("nil", typeMapping.getDefaultValue(CompositeType.ArrayType(PrimitiveType.IntType)))
    }
    
    @Test
    fun `test literal expressions`() {
        val stringLiteral = LiteralExpression.StringLiteral("hello", dummyLocation)
        assertEquals("\"hello\"", backend.generateExpression(stringLiteral))
        
        val intLiteral = LiteralExpression.IntLiteral(42, dummyLocation)
        assertEquals("42", backend.generateExpression(intLiteral))
        
        val floatLiteral = LiteralExpression.FloatLiteral(3.14, dummyLocation)
        assertEquals("3.14", backend.generateExpression(floatLiteral))
        
        val boolLiteral = LiteralExpression.BooleanLiteral(true, dummyLocation)
        assertEquals("true", backend.generateExpression(boolLiteral))
        
        val nullLiteral = LiteralExpression.NullLiteral(dummyLocation)
        assertEquals("nil", backend.generateExpression(nullLiteral))
    }
    
    @Test
    fun `test binary expressions`() {
        val left = LiteralExpression.IntLiteral(10, dummyLocation)
        val right = LiteralExpression.IntLiteral(5, dummyLocation)
        
        val addExpr = BinaryExpression(left, BinaryOperator.PLUS, right, dummyLocation)
        assertEquals("10 + 5", backend.generateExpression(addExpr))
        
        val eqExpr = BinaryExpression(left, BinaryOperator.EQUAL, right, dummyLocation)
        assertEquals("10 == 5", backend.generateExpression(eqExpr))
        
        val andExpr = BinaryExpression(
            LiteralExpression.BooleanLiteral(true, dummyLocation),
            BinaryOperator.LOGICAL_AND,
            LiteralExpression.BooleanLiteral(false, dummyLocation),
            dummyLocation
        )
        assertEquals("true && false", backend.generateExpression(andExpr))
    }
    
    @Test
    fun `test unary expressions`() {
        val operand = LiteralExpression.IntLiteral(42, dummyLocation)
        
        val negExpr = UnaryExpression(UnaryOperator.MINUS, operand, dummyLocation)
        assertEquals("-42", backend.generateExpression(negExpr))
        
        val notExpr = UnaryExpression(
            UnaryOperator.LOGICAL_NOT,
            LiteralExpression.BooleanLiteral(true, dummyLocation),
            dummyLocation
        )
        assertEquals("!true", backend.generateExpression(notExpr))
    }
    
    @Test
    fun `test function call expressions`() {
        val function = IdentifierExpression("println", dummyLocation)
        val args = listOf(
            Argument(value = LiteralExpression.StringLiteral("Hello", dummyLocation))
        )
        val callExpr = CallExpression(function, args, dummyLocation)
        
        assertEquals("println(\"Hello\")", backend.generateExpression(callExpr))
    }
    
    @Test
    fun `test array expressions`() {
        val elements = listOf(
            LiteralExpression.IntLiteral(1, dummyLocation),
            LiteralExpression.IntLiteral(2, dummyLocation),
            LiteralExpression.IntLiteral(3, dummyLocation)
        )
        val arrayExpr = ArrayExpression(elements, dummyLocation)
        
        assertEquals("[]any{1, 2, 3}", backend.generateExpression(arrayExpr))
    }
    
    @Test
    fun `test object expressions`() {
        val properties = listOf(
            ObjectProperty(
                LiteralExpression.StringLiteral("name", dummyLocation),
                LiteralExpression.StringLiteral("John", dummyLocation)
            ),
            ObjectProperty(
                LiteralExpression.StringLiteral("age", dummyLocation),
                LiteralExpression.IntLiteral(30, dummyLocation)
            )
        )
        val objectExpr = ObjectExpression(properties, dummyLocation)
        
        assertEquals("map[string]any{\"name\": \"John\", \"age\": 30}", backend.generateExpression(objectExpr))
    }
    
    @Test
    fun `test lambda expressions`() {
        val params = listOf(
            Parameter("x", PrimitiveType.IntType),
            Parameter("y", PrimitiveType.IntType)
        )
        val body = BinaryExpression(
            IdentifierExpression("x", dummyLocation),
            BinaryOperator.PLUS,
            IdentifierExpression("y", dummyLocation),
            dummyLocation
        )
        val lambdaExpr = LambdaExpression(params, body, dummyLocation)
        
        assertEquals("func(x int, y int) any { return x + y }", backend.generateExpression(lambdaExpr))
    }
    
    @Test
    fun `test variable declarations`() {
        // 不可变变量声明
        val immutableVar = VariableDeclaration(
            "x",
            PrimitiveType.IntType,
            LiteralExpression.IntLiteral(42, dummyLocation),
            false,
            dummyLocation
        )
        assertEquals("x := 42", backend.generateStatement(immutableVar))
        
        // 可变变量声明
        val mutableVar = VariableDeclaration(
            "y",
            PrimitiveType.StringType,
            LiteralExpression.StringLiteral("hello", dummyLocation),
            true,
            dummyLocation
        )
        assertEquals("var y string = \"hello\"", backend.generateStatement(mutableVar))
        
        // 只有类型的声明
        val typeOnlyVar = VariableDeclaration(
            "z",
            PrimitiveType.BooleanType,
            null,
            false,
            dummyLocation
        )
        assertEquals("var z bool", backend.generateStatement(typeOnlyVar))
    }
    
    @Test
    fun `test conditional statements`() {
        val condition = LiteralExpression.BooleanLiteral(true, dummyLocation)
        val thenBranch = ExpressionStatement(
            LiteralExpression.StringLiteral("then", dummyLocation),
            dummyLocation
        )
        val elseBranch = ExpressionStatement(
            LiteralExpression.StringLiteral("else", dummyLocation),
            dummyLocation
        )
        
        val ifStmt = ConditionalStatement(condition, thenBranch, elseBranch, dummyLocation)
        val result = backend.generateStatement(ifStmt)
        
        assertTrue(result.contains("if true {"))
        assertTrue(result.contains("} else {"))
        assertTrue(result.contains("\"then\""))
        assertTrue(result.contains("\"else\""))
    }
    
    @Test
    fun `test loop statements`() {
        // For 循环
        val iterable = IdentifierExpression("items", dummyLocation)
        val body = ExpressionStatement(
            IdentifierExpression("item", dummyLocation),
            dummyLocation
        )
        val forLoop = LoopStatement.ForLoop("item", iterable, body, dummyLocation)
        
        val forResult = backend.generateStatement(forLoop)
        assertTrue(forResult.contains("for item := range items {"))
        
        // While 循环
        val condition = LiteralExpression.BooleanLiteral(true, dummyLocation)
        val whileLoop = LoopStatement.WhileLoop(condition, body, dummyLocation)
        
        val whileResult = backend.generateStatement(whileLoop)
        assertTrue(whileResult.contains("for true {"))
    }
    
    @Test
    fun `test struct type declaration`() {
        val fields = mapOf(
            "name" to FieldDefinition(PrimitiveType.StringType),
            "age" to FieldDefinition(PrimitiveType.IntType),
            "email" to FieldDefinition(PrimitiveType.StringType, optional = true)
        )
        val structDef = TypeDefinition.StructDefinition(fields)
        val typeDecl = TypeDeclaration(
            "Person",
            TypeKind.STRUCT,
            structDef,
            location = dummyLocation
        )
        
        val result = backend.generateDeclaration(typeDecl)
        assertTrue(result.contains("type Person struct {"))
        assertTrue(result.contains("Name string"))
        assertTrue(result.contains("Age int"))
        assertTrue(result.contains("Email string"))
    }
    
    @Test
    fun `test enum type declaration`() {
        val variants = mapOf(
            "RED" to null,
            "GREEN" to null,
            "BLUE" to null
        )
        val enumDef = TypeDefinition.EnumDefinition(variants)
        val typeDecl = TypeDeclaration(
            "Color",
            TypeKind.ENUM,
            enumDef,
            location = dummyLocation
        )
        
        val result = backend.generateDeclaration(typeDecl)
        assertTrue(result.contains("type Color int"))
        assertTrue(result.contains("const ("))
        assertTrue(result.contains("RED Color = 0"))
        assertTrue(result.contains("GREEN Color = 1"))
        assertTrue(result.contains("BLUE Color = 2"))
    }
    
    @Test
    fun `test function declaration`() {
        val params = listOf(
            Parameter("x", PrimitiveType.IntType),
            Parameter("y", PrimitiveType.IntType)
        )
        val body = listOf(
            JumpStatement.ReturnStatement(
                BinaryExpression(
                    IdentifierExpression("x", dummyLocation),
                    BinaryOperator.PLUS,
                    IdentifierExpression("y", dummyLocation),
                    dummyLocation
                ),
                dummyLocation
            )
        )
        val funcDecl = FunctionDeclaration(
            "add",
            FunctionModifiers(),
            emptyList(),
            params,
            PrimitiveType.IntType,
            body,
            Visibility.PUBLIC,
            dummyLocation
        )
        
        val result = backend.generateDeclaration(funcDecl)
        assertTrue(result.contains("func Add(x int, y int) int {"))
        assertTrue(result.contains("return x + y"))
    }
    
    @Test
    fun `test async function declaration`() {
        val params = listOf(Parameter("msg", PrimitiveType.StringType))
        val body = listOf(
            ExpressionStatement(
                CallExpression(
                    IdentifierExpression("println", dummyLocation),
                    listOf(Argument(value = IdentifierExpression("msg", dummyLocation))),
                    dummyLocation
                ),
                dummyLocation
            )
        )
        val asyncFunc = FunctionDeclaration(
            "printAsync",
            FunctionModifiers(async = true),
            emptyList(),
            params,
            PrimitiveType.UnitType,
            body,
            Visibility.PUBLIC,
            dummyLocation
        )
        
        val result = backend.generateDeclaration(asyncFunc)
        println("Async function result:\n$result")
        assertTrue(result.contains("func PrintAsync(msg string) <-chan struct{} {"))
        assertTrue(result.contains("ch := make(chan struct{}, 1)"))
        assertTrue(result.contains("go func() {"))
        assertTrue(result.contains("func PrintAsyncSync(msg string) struct{} {"))
    }
    
    @Test
    fun `test generic function declaration`() {
        val generics = listOf(
            GenericParameter("T", listOf(PrimitiveType.AnyType))
        )
        val params = listOf(
            Parameter("items", CompositeType.ArrayType(GenericType("T")))
        )
        val body = listOf(
            JumpStatement.ReturnStatement(
                LiteralExpression.IntLiteral(0, dummyLocation),
                dummyLocation
            )
        )
        val genericFunc = FunctionDeclaration(
            "length",
            FunctionModifiers(),
            generics,
            params,
            PrimitiveType.IntType,
            body,
            Visibility.PUBLIC,
            dummyLocation
        )
        
        val result = backend.generateDeclaration(genericFunc)
        println("Generic function result:\n$result")
        assertTrue(result.contains("func Length[T any](items []T) int {"))
    }
    
    @Test
    fun `test program generation`() {
        val metadata = Metadata(
            "test-module",
            "1.0.0",
            location = dummyLocation
        )
        
        val imports = listOf(
            ImportDeclaration("fmt", location = dummyLocation)
        )
        
        val constants = listOf(
            ConstantDeclaration(
                "PI",
                PrimitiveType.DoubleType,
                LiteralExpression.FloatLiteral(3.14159, dummyLocation),
                location = dummyLocation
            )
        )
        
        val functions = listOf(
            FunctionDeclaration(
                "main",
                FunctionModifiers(),
                emptyList(),
                emptyList(),
                PrimitiveType.UnitType,
                listOf(
                    ExpressionStatement(
                        CallExpression(
                            IdentifierExpression("println", dummyLocation),
                            listOf(Argument(value = LiteralExpression.StringLiteral("Hello, Go!", dummyLocation))),
                            dummyLocation
                        ),
                        dummyLocation
                    )
                ),
                location = dummyLocation
            )
        )
        
        val program = Program(
            metadata,
            imports,
            emptyList(),
            emptyList(),
            constants,
            functions,
            emptyList(),
            emptyList(),
            dummyLocation
        )
        
        val result = backend.generateProgram(program)
        
        if (!result.isSuccess) {
            println("Errors: ${result.errors}")
            println("Generated code: ${result.code}")
        }
        assertTrue(result.isSuccess)
        
        val code = result.code
        println("Generated code:\n$code")
        assertTrue(code.contains("package testmodule"))
        assertTrue(code.contains("import \"fmt\""))
        assertTrue(code.contains("const ("))
        assertTrue(code.contains("PI float64 = 3.14159"))
        assertTrue(code.contains("func main() struct{} {") || code.contains("func Main() struct{} {"))
        assertTrue(code.contains("println(\"Hello, Go!\")"))
    }
    
    @Test
    fun `test Go-specific error handling patterns`() {
        // 测试 Go 的错误处理模式
        val tryBlock = BlockStatement(
            listOf(
                ExpressionStatement(
                    LiteralExpression.StringLiteral("risky operation", dummyLocation),
                    dummyLocation
                )
            ),
            dummyLocation
        )
        
        val catchClause = CatchClause(
            PrimitiveType.AnyType,
            "err",
            BlockStatement(
                listOf(
                    ExpressionStatement(
                        LiteralExpression.StringLiteral("handle error", dummyLocation),
                        dummyLocation
                    )
                ),
                dummyLocation
            ),
            dummyLocation
        )
        
        val tryCatch = TryCatchStatement(
            tryBlock,
            listOf(catchClause),
            location = dummyLocation
        )
        
        val result = backend.generateStatement(tryCatch)
        assertTrue(result.contains("// Try-catch converted to Go error handling"))
        assertTrue(result.contains("// TODO: Implement proper error handling"))
    }
    
    @Test
    fun `test Go concurrency patterns`() {
        // 测试 Go 的并发模式
        val asyncFunc = FunctionDeclaration(
            "worker",
            FunctionModifiers(async = true),
            emptyList(),
            listOf(Parameter("id", PrimitiveType.IntType)),
            PrimitiveType.StringType,
            listOf(
                JumpStatement.ReturnStatement(
                    LiteralExpression.StringLiteral("done", dummyLocation),
                    dummyLocation
                )
            ),
            location = dummyLocation
        )
        
        val result = backend.generateDeclaration(asyncFunc)
        println("Concurrency function result:\n$result")
        assertTrue(result.contains("func worker(id int) <-chan string {") || result.contains("func Worker(id int) <-chan string {"))
        assertTrue(result.contains("ch := make(chan string, 1)"))
        assertTrue(result.contains("go func() {"))
        assertTrue(result.contains("defer close(ch)"))
    }
}
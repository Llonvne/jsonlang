package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.annotations.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import kotlin.test.*

class FunctionModifiersTest {
    
    private lateinit var modifierProcessor: FunctionModifierProcessor
    private lateinit var annotationProcessor: FunctionAnnotationProcessor
    private val testLocation = SourceLocation("test.jsonlang", 1, 1, 1, 10, 0, 9)
    
    @BeforeTest
    fun setup() {
        modifierProcessor = FunctionModifierProcessor()
        annotationProcessor = FunctionAnnotationProcessor()
    }
    
    @Test
    fun testValidAsyncFunction() {
        val signature = createFunctionSignature(
            name = "fetchData",
            modifiers = FunctionModifiers(async = true),
            returnType = createPromiseType(PrimitiveType.StringType)
        )
        
        val result = modifierProcessor.validateModifierCombination(signature.modifiers, signature)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun testAsyncPureConflict() {
        val signature = createFunctionSignature(
            name = "conflictFunction",
            modifiers = FunctionModifiers(async = true, pure = true),
            returnType = PrimitiveType.StringType
        )
        
        val result = modifierProcessor.validateModifierCombination(signature.modifiers, signature)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("异步和纯函数") })
    }
    
    @Test
    fun testInlineExternalConflict() {
        val signature = createFunctionSignature(
            name = "conflictFunction",
            modifiers = FunctionModifiers(inline = true, external = true),
            returnType = PrimitiveType.IntType
        )
        
        val result = modifierProcessor.validateModifierCombination(signature.modifiers, signature)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("内联和外部函数") })
    }
    
    @Test
    fun testValidInlineFunction() {
        val signature = createFunctionSignature(
            name = "add",
            modifiers = FunctionModifiers(inline = true),
            parameters = listOf(
                FunctionParameter("a", PrimitiveType.IntType, false, null, emptyList()),
                FunctionParameter("b", PrimitiveType.IntType, false, null, emptyList())
            ),
            returnType = PrimitiveType.IntType
        )
        
        val result = modifierProcessor.validateModifierCombination(signature.modifiers, signature)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun testPureFunctionValidation() {
        val signature = createFunctionSignature(
            name = "calculate",
            modifiers = FunctionModifiers(pure = true),
            returnType = PrimitiveType.IntType
        )
        
        val result = modifierProcessor.validateModifierCombination(signature.modifiers, signature)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun testExternalFunctionWithDefaultParameters() {
        val defaultValue = LiteralExpression.IntLiteral(0, testLocation)
        val signature = createFunctionSignature(
            name = "externalFunc",
            modifiers = FunctionModifiers(external = true),
            parameters = listOf(
                FunctionParameter("x", PrimitiveType.IntType, true, defaultValue, emptyList())
            ),
            returnType = PrimitiveType.IntType
        )
        
        val result = modifierProcessor.validateModifierCombination(signature.modifiers, signature)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("外部函数") && it.contains("默认参数") })
    }
    
    @Test
    fun testApplyModifiers() {
        val signature = createFunctionSignature(
            name = "optimizedFunc",
            modifiers = FunctionModifiers(inline = true, pure = true),
            returnType = PrimitiveType.IntType
        )
        
        val context = ModifierApplicationContext(
            targetPlatform = "jvm",
            optimizationLevel = 2,
            debugMode = false
        )
        
        val result = modifierProcessor.applyModifiers(signature, context)
        
        assertTrue(result.transformations.isNotEmpty())
        assertTrue(result.optimizations.contains(FunctionOptimization.INLINE_EXPANSION))
        assertTrue(result.optimizations.contains(FunctionOptimization.MEMOIZATION))
    }
    
    @Test
    fun testDeprecatedAnnotation() {
        val deprecatedAnnotation = Annotation(
            name = "Deprecated",
            parameters = mapOf(
                "message" to AnnotationValue.StringValue("使用 newFunction() 替代"),
                "since" to AnnotationValue.StringValue("1.2.0")
            ),
            location = testLocation
        )
        
        val signature = createFunctionSignature(
            name = "oldFunction",
            annotations = listOf(deprecatedAnnotation)
        )
        
        val result = annotationProcessor.processFunctionAnnotations(signature)
        
        assertEquals(true, result.metadata["deprecated"])
        assertEquals("使用 newFunction() 替代", result.metadata["deprecationMessage"])
        assertEquals("1.2.0", result.metadata["deprecatedSince"])
        assertTrue(result.validationRules.contains(ValidationRule.DEPRECATION_WARNING))
    }
    
    @Test
    fun testOverrideAnnotation() {
        val overrideAnnotation = Annotation(
            name = "Override",
            parameters = emptyMap(),
            location = testLocation
        )
        
        val signature = createFunctionSignature(
            name = "toString",
            annotations = listOf(overrideAnnotation)
        )
        
        val result = annotationProcessor.processFunctionAnnotations(signature)
        
        assertEquals(true, result.metadata["override"])
        assertTrue(result.validationRules.contains(ValidationRule.OVERRIDE_CHECK))
    }
    
    @Test
    fun testThrowsAnnotation() {
        val throwsAnnotation = Annotation(
            name = "Throws",
            parameters = mapOf(
                "value" to AnnotationValue.ArrayValue(listOf(
                    AnnotationValue.StringValue("IOException"),
                    AnnotationValue.StringValue("SecurityException")
                ))
            ),
            location = testLocation
        )
        
        val signature = createFunctionSignature(
            name = "readFile",
            annotations = listOf(throwsAnnotation)
        )
        
        val result = annotationProcessor.processFunctionAnnotations(signature)
        
        val exceptions = result.metadata["declaredExceptions"] as? List<String>
        assertNotNull(exceptions)
        assertTrue(exceptions.contains("IOException"))
        assertTrue(exceptions.contains("SecurityException"))
        assertTrue(result.validationRules.contains(ValidationRule.EXCEPTION_DECLARATION_CHECK))
    }
    
    @Test
    fun testPerformanceAnnotation() {
        val performanceAnnotation = Annotation(
            name = "Performance",
            parameters = mapOf(
                "complexity" to AnnotationValue.StringValue("O(n)"),
                "cacheable" to AnnotationValue.BooleanValue(true)
            ),
            location = testLocation
        )
        
        val signature = createFunctionSignature(
            name = "linearSearch",
            annotations = listOf(performanceAnnotation)
        )
        
        val result = annotationProcessor.processFunctionAnnotations(signature)
        
        assertEquals("O(n)", result.metadata["timeComplexity"])
        assertTrue(result.transformations.contains(AnnotationTransformation.CACHING))
    }
    
    @Test
    fun testSecurityAnnotation() {
        val securityAnnotation = Annotation(
            name = "Security",
            parameters = mapOf(
                "level" to AnnotationValue.StringValue("HIGH"),
                "permissions" to AnnotationValue.ArrayValue(listOf(
                    AnnotationValue.StringValue("READ_FILES"),
                    AnnotationValue.StringValue("NETWORK_ACCESS")
                ))
            ),
            location = testLocation
        )
        
        val signature = createFunctionSignature(
            name = "sensitiveOperation",
            annotations = listOf(securityAnnotation)
        )
        
        val result = annotationProcessor.processFunctionAnnotations(signature)
        
        assertEquals("HIGH", result.metadata["securityLevel"])
        val permissions = result.metadata["requiredPermissions"] as? List<String>
        assertNotNull(permissions)
        assertTrue(permissions.contains("READ_FILES"))
        assertTrue(permissions.contains("NETWORK_ACCESS"))
        assertTrue(result.validationRules.contains(ValidationRule.SECURITY_CHECK))
    }
    
    @Test
    fun testParameterNotNullAnnotation() {
        val notNullAnnotation = Annotation(
            name = "NotNull",
            parameters = emptyMap(),
            location = testLocation
        )
        
        val parameter = FunctionParameter(
            name = "input",
            type = PrimitiveType.StringType,
            optional = false,
            defaultValue = null,
            annotations = listOf(notNullAnnotation)
        )
        
        val result = annotationProcessor.processParameterAnnotations(parameter)
        
        assertTrue(result.constraints.contains(ParameterConstraint.NOT_NULL))
    }
    
    @Test
    fun testParameterRangeAnnotation() {
        val rangeAnnotation = Annotation(
            name = "Range",
            parameters = mapOf(
                "min" to AnnotationValue.IntValue(0),
                "max" to AnnotationValue.IntValue(100)
            ),
            location = testLocation
        )
        
        val parameter = FunctionParameter(
            name = "percentage",
            type = PrimitiveType.IntType,
            optional = false,
            defaultValue = null,
            annotations = listOf(rangeAnnotation)
        )
        
        val result = annotationProcessor.processParameterAnnotations(parameter)
        
        val rangeConstraint = result.constraints.find { it is ParameterConstraint.Range } as? ParameterConstraint.Range
        assertNotNull(rangeConstraint)
        assertEquals(0L, rangeConstraint.min)
        assertEquals(100L, rangeConstraint.max)
    }
    
    @Test
    fun testParameterPatternAnnotation() {
        val patternAnnotation = Annotation(
            name = "Pattern",
            parameters = mapOf(
                "value" to AnnotationValue.StringValue("[a-zA-Z0-9]+")
            ),
            location = testLocation
        )
        
        val parameter = FunctionParameter(
            name = "username",
            type = PrimitiveType.StringType,
            optional = false,
            defaultValue = null,
            annotations = listOf(patternAnnotation)
        )
        
        val result = annotationProcessor.processParameterAnnotations(parameter)
        
        val patternConstraint = result.constraints.find { it is ParameterConstraint.Pattern } as? ParameterConstraint.Pattern
        assertNotNull(patternConstraint)
        assertEquals("[a-zA-Z0-9]+", patternConstraint.regex)
    }
    
    @Test
    fun testValidateNotNullConstraint() {
        val parameter = FunctionParameter(
            name = "input",
            type = PrimitiveType.StringType,
            optional = false,
            defaultValue = null,
            annotations = listOf(
                Annotation("NotNull", emptyMap(), location = testLocation)
            )
        )
        
        val signature = createFunctionSignature(
            name = "process",
            parameters = listOf(parameter)
        )
        
        // 测试 null 参数
        val nullCall = CallExpression(
            function = IdentifierExpression("process", testLocation),
            arguments = listOf(
                Argument(value = LiteralExpression.NullLiteral(testLocation))
            ),
            location = testLocation
        )
        
        val result = annotationProcessor.validateAnnotationConstraints(nullCall, signature)
        
        assertFalse(result.isValid)
        assertTrue(result.violations.any { it.constraint == "NotNull" })
    }
    
    @Test
    fun testValidateRangeConstraint() {
        val parameter = FunctionParameter(
            name = "age",
            type = PrimitiveType.IntType,
            optional = false,
            defaultValue = null,
            annotations = listOf(
                Annotation("Range", mapOf(
                    "min" to AnnotationValue.IntValue(0), 
                    "max" to AnnotationValue.IntValue(120)
                ), location = testLocation)
            )
        )
        
        val signature = createFunctionSignature(
            name = "setAge",
            parameters = listOf(parameter)
        )
        
        // 测试超出范围的值
        val invalidCall = CallExpression(
            function = IdentifierExpression("setAge", testLocation),
            arguments = listOf(
                Argument(value = LiteralExpression.IntLiteral(150, testLocation))
            ),
            location = testLocation
        )
        
        val result = annotationProcessor.validateAnnotationConstraints(invalidCall, signature)
        
        assertFalse(result.isValid)
        assertTrue(result.violations.any { it.constraint == "Range" })
    }
    
    // 辅助方法
    
    private fun createFunctionSignature(
        name: String,
        modifiers: FunctionModifiers = FunctionModifiers(),
        parameters: List<FunctionParameter> = emptyList(),
        returnType: Type = PrimitiveType.UnitType,
        annotations: List<cn.llonvne.jsonlang.core.annotations.Annotation> = emptyList()
    ): FunctionSignature {
        return FunctionSignature(
            name = name,
            modifiers = modifiers,
            generics = emptyList(),
            parameters = parameters,
            returnType = returnType,
            visibility = Visibility.PUBLIC,
            annotations = annotations,
            location = testLocation
        )
    }
    
    private fun createPromiseType(innerType: Type): Type {
        // 简化实现：创建一个名为 Promise 的类型
        return UserDefinedType.StructType(
            name = "Promise",
            fields = mapOf("value" to FieldType(innerType)),
            location = testLocation
        )
    }
}
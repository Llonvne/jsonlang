package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CodeTemplateTest {
    
    @Test
    fun testBasicTemplateRendering() {
        val template = CodeTemplate(
            name = "greeting",
            template = "Hello, \${name}! You are \${age} years old.",
            parameters = listOf(
                TemplateParameter("name", String::class.java),
                TemplateParameter("age", Int::class.java)
            )
        )
        
        val context = TemplateContext(
            variables = mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )
        
        val result = template.render(context)
        assertEquals("Hello, Alice! You are 25 years old.", result)
    }
    
    @Test
    fun testConditionalBlocks() {
        val template = CodeTemplate(
            name = "conditional",
            template = "#{if showGreeting}Hello, \${name}!#{endif}#{if showAge} You are \${age}.#{endif}",
            parameters = listOf(
                TemplateParameter("name", String::class.java),
                TemplateParameter("age", Int::class.java),
                TemplateParameter("showGreeting", Boolean::class.java),
                TemplateParameter("showAge", Boolean::class.java)
            )
        )
        
        // 测试两个条件都为真
        val context1 = TemplateContext(
            variables = mapOf(
                "name" to "Bob",
                "age" to 30,
                "showGreeting" to true,
                "showAge" to true
            )
        )
        
        val result1 = template.render(context1)
        assertEquals("Hello, Bob! You are 30.", result1)
        
        // 测试只有第一个条件为真
        val context2 = TemplateContext(
            variables = mapOf(
                "name" to "Charlie",
                "age" to 35,
                "showGreeting" to true,
                "showAge" to false
            )
        )
        
        val result2 = template.render(context2)
        assertEquals("Hello, Charlie!", result2)
        
        // 测试两个条件都为假
        val context3 = TemplateContext(
            variables = mapOf(
                "name" to "David",
                "age" to 40,
                "showGreeting" to false,
                "showAge" to false
            )
        )
        
        val result3 = template.render(context3)
        assertEquals("", result3)
    }
    
    @Test
    fun testLoopBlocks() {
        val template = CodeTemplate(
            name = "list",
            template = "Items: #{for item in items}\${item}, #{endfor}",
            parameters = listOf(
                TemplateParameter("items", List::class.java)
            )
        )
        
        val context = TemplateContext(
            variables = mapOf(
                "items" to listOf<Any>("apple", "banana", "cherry")
            )
        )
        
        val result = template.render(context)
        assertEquals("Items: apple, banana, cherry, ", result)
    }
    
    @Test
    fun testNestedTemplates() {
        val headerTemplate = CodeTemplate(
            name = "header",
            template = "=== \${title} ==="
        )
        
        val mainTemplate = CodeTemplate(
            name = "main",
            template = "#{template header}\nContent: \${content}"
        )
        
        val context = TemplateContext(
            variables = mapOf(
                "title" to "My Document",
                "content" to "This is the main content."
            ),
            templates = mapOf(
                "header" to headerTemplate
            )
        )
        
        val result = mainTemplate.render(context)
        assertEquals("=== My Document ===\nContent: This is the main content.", result)
    }
    
    @Test
    fun testTemplateValidation() {
        val template = CodeTemplate(
            name = "validation_test",
            template = "Name: \${name}, Age: \${age}",
            parameters = listOf(
                TemplateParameter("name", String::class.java, required = true),
                TemplateParameter("age", Int::class.java, required = true),
                TemplateParameter("optional", String::class.java, required = false)
            )
        )
        
        // 测试有效上下文
        val validContext = TemplateContext(
            variables = mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )
        
        val validationErrors1 = template.validate(validContext)
        assertTrue(validationErrors1.isEmpty())
        
        // 测试缺少必需参数
        val invalidContext = TemplateContext(
            variables = mapOf(
                "name" to "Bob"
                // 缺少 age
            )
        )
        
        val validationErrors2 = template.validate(invalidContext)
        assertEquals(1, validationErrors2.size)
        assertEquals("Missing required parameter: age", validationErrors2[0].message)
        assertEquals("age", validationErrors2[0].parameter)
        
        // 测试参数类型错误
        val wrongTypeContext = TemplateContext(
            variables = mapOf(
                "name" to "Charlie",
                "age" to "not a number" // 应该是 Int
            )
        )
        
        val validationErrors3 = template.validate(wrongTypeContext)
        assertEquals(1, validationErrors3.size)
        assertTrue(validationErrors3[0].message.contains("wrong type"))
        assertEquals("age", validationErrors3[0].parameter)
    }
    
    @Test
    fun testASTTemplate() {
        val builder = ASTOperations.builder(SourceLocation.unknown())
        
        // 创建一个 AST 模板: ${func}(${arg1}, ${arg2})
        val templateAST = builder.callExpression(
            builder.macroParameterPlaceholder("func"),
            listOf(
                builder.argument(value = builder.macroParameterPlaceholder("arg1")),
                builder.argument(value = builder.macroParameterPlaceholder("arg2"))
            )
        )
        
        val astTemplate = ASTTemplate(
            name = "function_call",
            template = templateAST,
            parameters = listOf("func", "arg1", "arg2")
        )
        
        // 创建绑定
        val bindings = mapOf(
            "func" to builder.identifier("myFunction"),
            "arg1" to builder.stringLiteral("hello"),
            "arg2" to builder.intLiteral(42)
        )
        
        // 展开模板
        val expanded = astTemplate.expand(bindings)
        
        assertTrue(expanded is CallExpression)
        val callExpr = expanded as CallExpression
        
        assertTrue(callExpr.function is IdentifierExpression)
        assertEquals("myFunction", (callExpr.function as IdentifierExpression).name)
        
        assertEquals(2, callExpr.arguments.size)
        assertTrue(callExpr.arguments[0].value is LiteralExpression.StringLiteral)
        assertEquals("hello", (callExpr.arguments[0].value as LiteralExpression.StringLiteral).value)
        
        assertTrue(callExpr.arguments[1].value is LiteralExpression.IntLiteral)
        assertEquals(42L, (callExpr.arguments[1].value as LiteralExpression.IntLiteral).value)
    }
    
    @Test
    fun testTemplateManager() {
        val manager = TemplateManager()
        
        // 创建并注册模板
        val template1 = manager.createTemplate(
            "greeting",
            "Hello, \${name}!",
            listOf(TemplateParameter("name", String::class.java))
        )
        
        val template2 = CodeTemplate(
            name = "farewell",
            template = "Goodbye, \${name}!"
        )
        manager.registerTemplate(template2)
        
        // 测试模板检索
        val retrieved1 = manager.getTemplate("greeting")
        assertNotNull(retrieved1)
        assertEquals("greeting", retrieved1?.name)
        
        val retrieved2 = manager.getTemplate("farewell")
        assertNotNull(retrieved2)
        assertEquals("farewell", retrieved2?.name)
        
        val nonExistent = manager.getTemplate("nonexistent")
        assertNull(nonExistent)
        
        // 测试模板列表
        val templateNames = manager.listTemplates()
        assertEquals(2, templateNames.size)
        assertTrue(templateNames.contains("greeting"))
        assertTrue(templateNames.contains("farewell"))
        
        // 测试 AST 模板
        val builder = ASTOperations.builder(SourceLocation.unknown())
        val astTemplate = manager.createASTTemplate(
            "simple_call",
            builder.callExpression(
                builder.identifier("func"),
                listOf(builder.argument(value = builder.macroParameterPlaceholder("arg")))
            ),
            listOf("arg")
        )
        
        val retrievedAST = manager.getASTTemplate("simple_call")
        assertNotNull(retrievedAST)
        assertEquals("simple_call", retrievedAST?.name)
        
        val astTemplateNames = manager.listASTTemplates()
        assertEquals(1, astTemplateNames.size)
        assertTrue(astTemplateNames.contains("simple_call"))
    }
    
    @Test
    fun testBuiltinTemplateFunctions() {
        val context = BuiltinTemplateFunctions.createContext()
        
        // 测试 uppercase 函数
        val uppercaseFunc = context.getFunction("uppercase")
        assertNotNull(uppercaseFunc)
        assertEquals("HELLO", uppercaseFunc?.invoke(listOf("hello")))
        
        // 测试 lowercase 函数
        val lowercaseFunc = context.getFunction("lowercase")
        assertNotNull(lowercaseFunc)
        assertEquals("world", lowercaseFunc?.invoke(listOf("WORLD")))
        
        // 测试 capitalize 函数
        val capitalizeFunc = context.getFunction("capitalize")
        assertNotNull(capitalizeFunc)
        assertEquals("Hello", capitalizeFunc?.invoke(listOf("hello")))
        
        // 测试 join 函数
        val joinFunc = context.getFunction("join")
        assertNotNull(joinFunc)
        assertEquals("a, b, c", joinFunc?.invoke(listOf(listOf("a", "b", "c"), ", ")))
        assertEquals("a|b|c", joinFunc?.invoke(listOf(listOf("a", "b", "c"), "|")))
        
        // 测试 length 函数
        val lengthFunc = context.getFunction("length")
        assertNotNull(lengthFunc)
        assertEquals(5, lengthFunc?.invoke(listOf("hello")))
        assertEquals(3, lengthFunc?.invoke(listOf(listOf("a", "b", "c"))))
        
        // 测试 default 函数
        val defaultFunc = context.getFunction("default")
        assertNotNull(defaultFunc)
        assertEquals("default_value", defaultFunc?.invoke(listOf<Any?>(null, "default_value")))
        assertEquals("default_value", defaultFunc?.invoke(listOf<Any?>("", "default_value")))
        assertEquals("actual_value", defaultFunc?.invoke(listOf<Any?>("actual_value", "default_value")))
    }
    
    @Test
    fun testComplexTemplate() {
        val template = CodeTemplate(
            name = "function_definition",
            template = "#{if async}async #{endif}function \${name}() { /* body */ }",
            parameters = listOf(
                TemplateParameter("name", String::class.java),
                TemplateParameter("async", Boolean::class.java, required = false, defaultValue = false),
                TemplateParameter("parameters", List::class.java),
                TemplateParameter("body", List::class.java)
            )
        )
        
        val context = TemplateContext(
            variables = mapOf(
                "name" to "myFunction",
                "async" to true,
                "parameters" to listOf<Any>("param1", "param2"),
                "body" to listOf<Any>("console.log('hello')", "return 42")
            )
        )
        
        val result = template.render(context)
        assertTrue(result.contains("async function myFunction"))
        assertTrue(result.contains("/* body */"))
    }
}
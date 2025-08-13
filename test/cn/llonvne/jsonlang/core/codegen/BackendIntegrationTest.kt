package cn.llonvne.jsonlang.core.codegen

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*
import cn.llonvne.jsonlang.core.source.SourceLocation
import cn.llonvne.jsonlang.core.parser.JSONLangFileParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

/**
 * 综合测试：使用真实的 .jsonlang 文件测试 Go 和 Kotlin 后端的代码生成
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendIntegrationTest {

    private lateinit var goBackend: GoBackend
    private lateinit var kotlinBackend: KotlinBackend
    private lateinit var parser: JSONLangFileParser

    @BeforeEach
    fun setUp() {
        goBackend = GoBackend()
        kotlinBackend = KotlinBackend()
        parser = JSONLangFileParser()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "math.lang.json",
            "collections.lang.json",
            "async.lang.json",
            "control-flow.lang.json"
        ]
    )
    fun `test Go backend with real jsonlang files`(filename: String) {
        val file = File(filename)
        if (!file.exists()) {
            println("Skipping $filename - file not found")
            return
        }

        val program = parser.parseFile(filename)

        val result = goBackend.generateProgram(program)

        println("=== Go Backend Output for $filename ===")
        println(result.code)
        println("=== Imports: ${result.imports} ===")
        println("=== Metadata: ${result.metadata} ===")

        if (result.errors.isNotEmpty()) {
            println("=== Errors ===")
            result.errors.forEach { println("ERROR: $it") }
        }

        if (result.warnings.isNotEmpty()) {
            println("=== Warnings ===")
            result.warnings.forEach { println("WARNING: $it") }
        }

        // 基本验证
        assertTrue(result.isSuccess, "Go backend should generate code successfully for $filename")
        assertFalse(result.code.isEmpty(), "Generated Go code should not be empty")

        // Go 特定验证
        when (filename) {
            "main.lang.json" -> {
                assertTrue(result.code.contains("package main") || result.code.contains("package testmodule"))
                assertTrue(result.code.contains("func"))
            }

            "math.lang.json" -> {
                assertTrue(result.code.contains("type Point struct"))
                assertTrue(result.code.contains("func Add") || result.code.contains("func add"))
                assertTrue(result.code.contains("func Multiply") || result.code.contains("func multiply"))
                assertTrue(result.code.contains("const ("))
                assertTrue(result.code.contains("PI float64"))
            }

            "collections.lang.json" -> {
                assertTrue(result.code.contains("func Filter") || result.code.contains("func filter"))
                assertTrue(result.code.contains("func Map") || result.code.contains("func map"))
                assertTrue(result.code.contains("[]"))
                assertTrue(result.code.contains("for"))
            }

            "async.lang.json" -> {
                assertTrue(result.code.contains("chan") || result.code.contains("goroutine"))
                assertTrue(result.code.contains("func Delay") || result.code.contains("func delay"))
                assertTrue(result.code.contains("go func()") || result.code.contains("make(chan"))
            }

            "control-flow.lang.json" -> {
                assertTrue(result.code.contains("switch") || result.code.contains("if"))
                assertTrue(result.code.contains("for"))
                assertTrue(result.code.contains("func Factorial") || result.code.contains("func factorial"))
            }
        }

        // 验证 Go 语法正确性
        validateGoSyntax(result.code, filename)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "main.lang.json",
            "math.lang.json",
            "collections.lang.json",
            "async.lang.json",
            "control-flow.lang.json"
        ]
    )
    fun `test Kotlin backend with real jsonlang files`(filename: String) {
        val file = File(filename)
        if (!file.exists()) {
            println("Skipping $filename - file not found")
            return
        }

        val program = parser.parseFile(filename)

        val result = kotlinBackend.generateProgram(program)

        println("=== Kotlin Backend Output for $filename ===")
        println(result.code)
        println("=== Imports: ${result.imports} ===")
        println("=== Metadata: ${result.metadata} ===")

        if (result.errors.isNotEmpty()) {
            println("=== Errors ===")
            result.errors.forEach { println("ERROR: $it") }
        }

        if (result.warnings.isNotEmpty()) {
            println("=== Warnings ===")
            result.warnings.forEach { println("WARNING: $it") }
        }

        // 基本验证
        assertTrue(result.isSuccess, "Kotlin backend should generate code successfully for $filename")
        assertFalse(result.code.isEmpty(), "Generated Kotlin code should not be empty")

        // Kotlin 特定验证
        when (filename) {
            "main.lang.json" -> {
                assertTrue(result.code.contains("fun main"))
            }

            "math.lang.json" -> {
                assertTrue(result.code.contains("data class Point"))
                assertTrue(result.code.contains("fun add"))
                assertTrue(result.code.contains("fun multiply"))
                assertTrue(result.code.contains("const val PI"))
            }

            "collections.lang.json" -> {
                assertTrue(result.code.contains("inline fun") || result.code.contains("fun filter"))
                assertTrue(result.code.contains("fun map"))
                assertTrue(result.code.contains("List<"))
                assertTrue(result.code.contains("for"))
            }

            "async.lang.json" -> {
                assertTrue(result.code.contains("suspend") || result.code.contains("async"))
                assertTrue(result.code.contains("fun delay"))
                assertTrue(result.code.contains("kotlinx.coroutines") || result.imports.any { it.contains("coroutines") })
            }

            "control-flow.lang.json" -> {
                assertTrue(result.code.contains("when") || result.code.contains("if"))
                assertTrue(result.code.contains("sealed class") || result.code.contains("enum class"))
                assertTrue(result.code.contains("fun factorial"))
            }
        }

        // 验证 Kotlin 语法正确性
        validateKotlinSyntax(result.code, filename)
    }

    @Test
    fun `test backend comparison for same input`() {
        val file = File("math.lang.json")
        if (!file.exists()) {
            println("Skipping comparison test - math.lang.json not found")
            return
        }

        val program = parser.parseFile("math.lang.json")

        val goResult = goBackend.generateProgram(program)
        val kotlinResult = kotlinBackend.generateProgram(program)

        println("=== Backend Comparison for math.lang.json ===")
        println("Go Success: ${goResult.isSuccess}")
        println("Kotlin Success: ${kotlinResult.isSuccess}")

        assertTrue(goResult.isSuccess, "Go backend should succeed")
        assertTrue(kotlinResult.isSuccess, "Kotlin backend should succeed")

        // 比较结构相似性
        compareBackendOutputs(goResult, kotlinResult, "math.lang.json")
    }

    @Test
    fun `test async function generation comparison`() {
        val file = File("async.lang.json")
        if (!file.exists()) {
            println("Skipping async test - async.lang.json not found")
            return
        }

        val program = parser.parseFile("async.lang.json")

        val goResult = goBackend.generateProgram(program)
        val kotlinResult = kotlinBackend.generateProgram(program)

        println("=== Async Function Comparison ===")
        println("Go async patterns:")
        println(goResult.code.lines().filter { it.contains("chan") || it.contains("go func") }.joinToString("\n"))

        println("\nKotlin async patterns:")
        println(kotlinResult.code.lines().filter { it.contains("suspend") || it.contains("async") }.joinToString("\n"))

        // 验证异步模式
        assertTrue(
            goResult.code.contains("chan") || goResult.code.contains("goroutine"),
            "Go should use channels or goroutines for async"
        )
        assertTrue(
            kotlinResult.code.contains("suspend") || kotlinResult.imports.any { it.contains("coroutines") },
            "Kotlin should use suspend functions or coroutines"
        )
    }

    @Test
    fun `test generic function generation`() {
        val file = File("collections.lang.json")
        if (!file.exists()) {
            println("Skipping generic test - collections.lang.json not found")
            return
        }

        val program = parser.parseFile("collections.lang.json")

        val goResult = goBackend.generateProgram(program)
        val kotlinResult = kotlinBackend.generateProgram(program)

        println("=== Generic Function Comparison ===")
        println("Go generics:")
        println(goResult.code.lines().filter { it.contains("[") && it.contains("func") }.joinToString("\n"))

        println("\nKotlin generics:")
        println(kotlinResult.code.lines().filter { it.contains("<") && it.contains("fun") }.joinToString("\n"))

        // 验证泛型支持
        assertTrue(
            goResult.code.contains("[") || goResult.code.contains("any"),
            "Go should support generics or use any type"
        )
        assertTrue(
            kotlinResult.code.contains("<") && kotlinResult.code.contains(">"),
            "Kotlin should use angle brackets for generics"
        )
    }

    @Test
    fun `test error handling patterns`() {
        val file = File("async.lang.json")
        if (!file.exists()) {
            println("Skipping error handling test - async.lang.json not found")
            return
        }

        val program = parser.parseFile("async.lang.json")

        val goResult = goBackend.generateProgram(program)
        val kotlinResult = kotlinBackend.generateProgram(program)

        println("=== Error Handling Comparison ===")
        println("Go error handling:")
        println(goResult.code.lines().filter {
            it.contains("error") || it.contains("err") || it.contains("Try-catch")
        }.joinToString("\n"))

        println("\nKotlin error handling:")
        println(kotlinResult.code.lines().filter {
            it.contains("try") || it.contains("catch") || it.contains("Exception")
        }.joinToString("\n"))
    }

    private fun validateGoSyntax(code: String, filename: String) {
        // 基本的 Go 语法验证
        val lines = code.lines()

        // 检查包声明
        assertTrue(
            lines.any { it.startsWith("package ") },
            "Go code should have package declaration in $filename"
        )

        // 检查括号匹配
        var braceCount = 0
        var parenCount = 0
        for (line in lines) {
            braceCount += line.count { it == '{' } - line.count { it == '}' }
            parenCount += line.count { it == '(' } - line.count { it == ')' }
        }
        assertEquals(0, braceCount, "Braces should be balanced in Go code for $filename")
        assertEquals(0, parenCount, "Parentheses should be balanced in Go code for $filename")

        // 检查基本的 Go 关键字使用
        if (code.contains("func ")) {
            assertTrue(
                code.contains("{") && code.contains("}"),
                "Functions should have body braces in $filename"
            )
        }
    }

    private fun validateKotlinSyntax(code: String, filename: String) {
        // 基本的 Kotlin 语法验证
        val lines = code.lines()

        // 检查括号匹配
        var braceCount = 0
        var parenCount = 0
        for (line in lines) {
            braceCount += line.count { it == '{' } - line.count { it == '}' }
            parenCount += line.count { it == '(' } - line.count { it == ')' }
        }
        assertEquals(0, braceCount, "Braces should be balanced in Kotlin code for $filename")
        assertEquals(0, parenCount, "Parentheses should be balanced in Kotlin code for $filename")

        // 检查基本的 Kotlin 关键字使用
        if (code.contains("fun ")) {
            assertTrue(
                code.contains("{") && code.contains("}") || code.contains("="),
                "Functions should have body or expression in $filename"
            )
        }
    }

    private fun compareBackendOutputs(goResult: BackendResult, kotlinResult: BackendResult, filename: String) {
        println("=== Comparing outputs for $filename ===")

        // 比较成功状态
        assertEquals(
            goResult.isSuccess, kotlinResult.isSuccess,
            "Both backends should have same success status"
        )

        // 比较代码长度（应该在合理范围内）
        val goLines = goResult.code.lines().size
        val kotlinLines = kotlinResult.code.lines().size
        val ratio = if (kotlinLines > 0) goLines.toDouble() / kotlinLines else 0.0

        println("Go lines: $goLines, Kotlin lines: $kotlinLines, Ratio: $ratio")
        assertTrue(
            ratio > 0.5 && ratio < 3.0,
            "Code length ratio should be reasonable (0.5 < ratio < 3.0)"
        )

        // 比较导入数量
        println("Go imports: ${goResult.imports.size}, Kotlin imports: ${kotlinResult.imports.size}")

        // 比较元数据
        println("Go metadata: ${goResult.metadata}")
        println("Kotlin metadata: ${kotlinResult.metadata}")
    }
}
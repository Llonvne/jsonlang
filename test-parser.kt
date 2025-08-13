import cn.llonvne.jsonlang.core.parser.JSONLangFileParser

fun main() {
    val parser = JSONLangFileParser()
    
    println("Testing JSONLang file parser...")
    
    try {
        val program = parser.parseFile("math.lang.json")
        println("Successfully parsed math.lang.json!")
        println("Program name: ${program.metadata.name}")
        println("Types: ${program.types.map { it.name }}")
        println("Constants: ${program.constants.map { it.name }}")
        println("Functions: ${program.functions.map { it.name }}")
        println()
        
        // Test Go backend
        val goBackend = cn.llonvne.jsonlang.core.codegen.GoBackend()
        val goResult = goBackend.generateProgram(program)
        println("=== Go Backend Output ===")
        println(goResult.code)
        println()
        
        // Test Kotlin backend
        val kotlinBackend = cn.llonvne.jsonlang.core.codegen.KotlinBackend()
        val kotlinResult = kotlinBackend.generateProgram(program)
        println("=== Kotlin Backend Output ===")
        println(kotlinResult.code)
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}
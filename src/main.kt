import cn.llonvne.jsonlang.core.CoreInfrastructureTest

fun main() {
    println("=== JSONLang 核心基础设施测试 ===")
    CoreInfrastructureTest.runTests()
    println("=== 测试完成 ===")
    println()
    println("要运行词法分析器测试，请使用: ./amper test")
}
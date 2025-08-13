package cn.llonvne.jsonlang

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File

class GlobalContext(
    val files: Map<String, String>
)

class JsonLangParser(
    mainFiles: File,
    stdlibFile: String,
) {
    init {
        require(mainFiles.name.endsWith(".lang.json")) {
            "合法的 jsonlang 必须以 .lang.json 结尾"
        }
    }

    val parsed: JsonLangFile = Json.decodeFromString(mainFiles.readText())

    val context = ParserContext()

    init {
        parseInternal()
    }

    private fun parseInternal() {
        parseImports(parsed.imports)
    }

    private fun parseImports(imports: Map<String, JsonLangImport>) {
        for ((key, import) in imports) {
            parseImport(key, import)
        }
    }

    private fun parseImport(name: String, jsonLangImport: JsonLangImport) {
        if (jsonLangImport.file != null) {
            context.fileMappings[jsonLangImport.file] = JsonLangParser(File(jsonLangImport.file), "")
            return
        }
        // 对于
        if (jsonLangImport.version != null) {
            if (name == "jsonlang") {
            }
        }
    }
}

class ParserContext {
    val fileMappings: MutableMap<String, JsonLangParser> = mutableMapOf()
}


@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun main() {
    // 运行核心基础设施测试
    cn.llonvne.jsonlang.core.CoreInfrastructureTest.runTests()
}
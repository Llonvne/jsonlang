package cn.llonvne.jsonlang

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JsonLangFile(
    val imports: Map<String, JsonLangImport> = emptyMap(),
    val functions: Map<String, JsonLangFunction> = emptyMap(),
)

@Serializable
data class JsonLangImport(
    val version: String? = null,
    val file: String? = null,
)

@Serializable
data class JsonLangFunction(
    val modifiers: JsonLangFunctionModifiers = JsonLangFunctionModifiers(),
    val `return`: JsonLangFunctionReturn = JsonLangFunctionReturn(),
    val args: List<JsonElement> = emptyList(),
    val body: List<JsonLangFunctionBody> = emptyList(),
)

@Serializable
data class JsonLangFunctionModifiers(
    val visibility: String = "public",
)

@Serializable
data class JsonLangFunctionReturn(
    val namespace: String = "jsonlang",
    val type: String = "Unit",
)

@Serializable
data class JsonLangFunctionBody(
    val type: String,
    val value: JsonElement,
)




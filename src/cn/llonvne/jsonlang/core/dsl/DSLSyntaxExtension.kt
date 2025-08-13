package cn.llonvne.jsonlang.core.dsl

import cn.llonvne.jsonlang.core.ast.DSLSyntax
import cn.llonvne.jsonlang.core.source.SourceLocation
import java.util.regex.Pattern

/**
 * DSL 语法扩展类
 * 负责扩展和自定义 DSL 语法解析
 */
class DSLSyntaxExtension(
    val dslName: String,
    val syntax: DSLSyntax
) {
    private val compiledPatterns = mutableMapOf<String, CompiledDSLPattern>()
    
    init {
        // 编译所有模式
        syntax.patterns.forEach { (name, pattern) ->
            compiledPatterns[name] = CompiledDSLPattern(name, pattern)
        }
    }
    
    /**
     * 解析 DSL 输入
     */
    fun parse(input: String, location: SourceLocation): DSLNode? {
        for ((patternName, compiledPattern) in compiledPatterns) {
            val match = compiledPattern.match(input.trim())
            if (match != null) {
                return DSLNode(
                    dslName = dslName,
                    patternName = patternName,
                    fields = match,
                    location = location
                )
            }
        }
        return null
    }
    
    /**
     * 检查输入是否匹配任何模式
     */
    fun matches(input: String): Boolean {
        return compiledPatterns.values.any { it.matches(input.trim()) }
    }
    
    /**
     * 获取所有模式名称
     */
    fun getPatternNames(): Set<String> = compiledPatterns.keys
}

/**
 * 编译后的 DSL 模式
 */
class CompiledDSLPattern(
    val name: String,
    val pattern: cn.llonvne.jsonlang.core.ast.DSLPattern
) {
    private val regex: Pattern
    private val fieldNames: List<String>
    
    init {
        val (compiledRegex, extractedFields) = compilePattern(pattern.pattern, pattern.fields)
        regex = compiledRegex
        fieldNames = extractedFields
    }
    
    /**
     * 匹配输入字符串
     */
    fun match(input: String): Map<String, String>? {
        val matcher = regex.matcher(input)
        if (!matcher.matches()) {
            return null
        }
        
        val result = mutableMapOf<String, String>()
        for (i in fieldNames.indices) {
            val fieldName = fieldNames[i]
            val fieldValue = matcher.group(i + 1) ?: ""
            result[fieldName] = fieldValue.trim()
        }
        
        return result
    }
    
    /**
     * 检查是否匹配
     */
    fun matches(input: String): Boolean {
        return regex.matcher(input).matches()
    }
    
    /**
     * 编译 DSL 模式为正则表达式
     */
    private fun compilePattern(patternStr: String, fields: List<String>): Pair<Pattern, List<String>> {
        var regexStr = patternStr
        val extractedFields = mutableListOf<String>()
        
        // 替换字段占位符为捕获组
        for (field in fields) {
            val placeholder = "\${$field}"
            if (regexStr.contains(placeholder)) {
                // Use a greedy pattern that matches everything until the next static part
                regexStr = regexStr.replace(placeholder, "(.+?)")
                extractedFields.add(field)
            }
        }
        
        // 处理特殊的 DSL 语法
        regexStr = processDSLSyntax(regexStr)
        
        return Pair(Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE), extractedFields)
    }
    
    /**
     * 处理特殊的 DSL 语法
     */
    private fun processDSLSyntax(regexStr: String): String {
        var processed = regexStr
        
        // 处理可选部分 [optional]
        processed = processed.replace(Regex("\\[([^\\]]+)\\]")) { matchResult ->
            "(${matchResult.groupValues[1]})?"
        }
        
        // 处理重复部分 {repeated}
        processed = processed.replace(Regex("\\{([^}]+)\\}")) { matchResult ->
            "(${matchResult.groupValues[1]})+"
        }
        
        // 处理选择 (option1|option2)
        processed = processed.replace(Regex("\\(([^)]+)\\)")) { matchResult ->
            val options = matchResult.groupValues[1].split("|")
            "(${options.joinToString("|") { it.trim() }})"
        }
        
        // 转义特殊字符，但要避免转义已经在正则表达式中的特殊字符
        // 我们需要更智能的转义，只转义不在正则表达式语法中的字符
        // 暂时移除这些转义，因为它们会破坏我们生成的捕获组
        
        return processed
    }
}

/**
 * DSL 模式匹配器
 */
class DSLPatternMatcher(
    private val pattern: cn.llonvne.jsonlang.core.ast.DSLPattern
) {
    private val compiledPattern = CompiledDSLPattern("matcher", pattern)
    
    /**
     * 匹配输入
     */
    fun match(input: String): Map<String, String>? {
        return compiledPattern.match(input)
    }
    
    /**
     * 检查是否匹配
     */
    fun matches(input: String): Boolean {
        return compiledPattern.matches(input)
    }
}

/**
 * DSL 语法构建器
 * 提供流式 API 来构建 DSL 语法
 */
class DSLSyntaxBuilder(private val dslName: String) {
    private val patterns = mutableMapOf<String, cn.llonvne.jsonlang.core.ast.DSLPattern>()
    
    /**
     * 添加模式
     */
    fun pattern(name: String, patternStr: String, vararg fields: String): DSLSyntaxBuilder {
        patterns[name] = cn.llonvne.jsonlang.core.ast.DSLPattern(patternStr, fields.toList())
        return this
    }
    
    /**
     * 添加简单模式（自动提取字段）
     */
    fun simplePattern(name: String, patternStr: String): DSLSyntaxBuilder {
        val fields = extractFieldsFromPattern(patternStr)
        patterns[name] = cn.llonvne.jsonlang.core.ast.DSLPattern(patternStr, fields)
        return this
    }
    
    /**
     * 构建语法定义
     */
    fun build(): DSLSyntax {
        return DSLSyntax(patterns.toMap())
    }
    
    /**
     * 从模式字符串中提取字段
     */
    private fun extractFieldsFromPattern(patternStr: String): List<String> {
        val fieldRegex = Regex("\\$\\{([^}]+)\\}")
        return fieldRegex.findAll(patternStr).map { it.groupValues[1] }.toList()
    }
}

/**
 * DSL 语法验证器
 */
class DSLSyntaxValidator {
    /**
     * 验证 DSL 语法定义
     */
    fun validate(syntax: DSLSyntax): List<DSLSyntaxError> {
        val errors = mutableListOf<DSLSyntaxError>()
        
        for ((patternName, pattern) in syntax.patterns) {
            // 检查模式是否为空
            if (pattern.pattern.isBlank()) {
                errors.add(DSLSyntaxError("模式 '$patternName' 不能为空"))
                continue
            }
            
            // 检查字段是否在模式中使用
            for (field in pattern.fields) {
                if (!pattern.pattern.contains("\${$field}")) {
                    errors.add(DSLSyntaxError("字段 '$field' 在模式 '$patternName' 中未使用"))
                }
            }
            
            // 检查模式中的字段是否都已声明
            val fieldRegex = Regex("\\$\\{([^}]+)\\}")
            val usedFields = fieldRegex.findAll(pattern.pattern).map { it.groupValues[1] }.toSet()
            for (usedField in usedFields) {
                if (usedField !in pattern.fields) {
                    errors.add(DSLSyntaxError("模式 '$patternName' 中使用了未声明的字段 '$usedField'"))
                }
            }
            
            // 尝试编译正则表达式
            try {
                CompiledDSLPattern(patternName, pattern)
            } catch (e: Exception) {
                errors.add(DSLSyntaxError("模式 '$patternName' 编译失败: ${e.message}"))
            }
        }
        
        return errors
    }
}

/**
 * DSL 语法错误
 */
data class DSLSyntaxError(val message: String)
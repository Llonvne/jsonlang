package cn.llonvne.jsonlang.core.functions

import cn.llonvne.jsonlang.core.ast.*
import cn.llonvne.jsonlang.core.types.*

/**
 * 函数重载解析器
 * 负责解析函数重载和选择最佳匹配的函数
 */
class OverloadResolver {
    
    /**
     * 检查重载冲突
     */
    fun checkOverloadConflict(
        newSignature: FunctionSignature,
        existingSignatures: List<FunctionSignature>
    ): OverloadResult {
        val errors = mutableListOf<String>()
        
        for (existing in existingSignatures) {
            if (hasOverloadConflict(newSignature, existing)) {
                errors.add("函数重载冲突: ${newSignature.name} 与现有函数签名冲突")
            }
        }
        
        return OverloadResult(errors.isEmpty(), errors)
    }
    
    /**
     * 解析重载
     */
    fun resolveOverload(
        call: CallExpression,
        candidates: List<FunctionSignature>
    ): OverloadResolutionResult {
        if (candidates.isEmpty()) {
            return OverloadResolutionResult(false, listOf("没有候选函数"), null)
        }
        
        if (candidates.size == 1) {
            return OverloadResolutionResult(true, emptyList(), candidates.first())
        }
        
        // 过滤可行的候选函数
        val viableCandidates = candidates.filter { isViableCandidate(call, it) }
        
        if (viableCandidates.isEmpty()) {
            return OverloadResolutionResult(
                false, 
                listOf("没有匹配的函数重载"),
                null
            )
        }
        
        if (viableCandidates.size == 1) {
            return OverloadResolutionResult(true, emptyList(), viableCandidates.first())
        }
        
        // 选择最佳匹配
        val bestMatch = selectBestMatch(call, viableCandidates)
        
        return if (bestMatch != null) {
            OverloadResolutionResult(true, emptyList(), bestMatch)
        } else {
            OverloadResolutionResult(
                false,
                listOf("函数调用存在歧义，有多个匹配的重载"),
                null
            )
        }
    }
    
    /**
     * 检查是否存在重载冲突
     */
    private fun hasOverloadConflict(
        signature1: FunctionSignature,
        signature2: FunctionSignature
    ): Boolean {
        // 如果参数数量不同，不冲突
        if (signature1.parameters.size != signature2.parameters.size) {
            return false
        }
        
        // 检查参数类型是否完全相同
        return signature1.parameters.zip(signature2.parameters).all { (param1, param2) ->
            areTypesEquivalent(param1.type, param2.type)
        }
    }
    
    /**
     * 检查是否为可行候选函数
     */
    private fun isViableCandidate(call: CallExpression, signature: FunctionSignature): Boolean {
        val arguments = call.arguments
        val parameters = signature.parameters
        
        // 检查参数数量
        val requiredParamCount = parameters.count { !it.optional && !it.hasDefaultValue }
        val maxParamCount = parameters.size
        
        if (arguments.size < requiredParamCount || arguments.size > maxParamCount) {
            return false
        }
        
        // 检查命名参数
        val namedArgs = arguments.filter { it.name != null }
        val positionalArgs = arguments.filter { it.name == null }
        
        // 位置参数不能超过参数列表长度
        if (positionalArgs.size > parameters.size) {
            return false
        }
        
        // 检查命名参数是否存在
        for (namedArg in namedArgs) {
            if (parameters.none { it.name == namedArg.name }) {
                return false
            }
        }
        
        // 检查参数类型兼容性
        return checkArgumentTypeCompatibility(call, signature)
    }
    
    /**
     * 检查参数类型兼容性
     */
    private fun checkArgumentTypeCompatibility(
        call: CallExpression,
        signature: FunctionSignature
    ): Boolean {
        val arguments = call.arguments
        val parameters = signature.parameters
        
        // 简化实现：假设所有参数类型都兼容
        // 实际实现需要进行详细的类型检查
        for (i in arguments.indices) {
            if (i < parameters.size) {
                val argType = arguments[i].value.inferredType
                val paramType = parameters[i].type
                
                if (argType != null && !paramType.isAssignableFrom(argType)) {
                    return false
                }
            }
        }
        
        return true
    }
    
    /**
     * 选择最佳匹配
     */
    private fun selectBestMatch(
        call: CallExpression,
        candidates: List<FunctionSignature>
    ): FunctionSignature? {
        if (candidates.size == 1) {
            return candidates.first()
        }
        
        // 计算每个候选函数的匹配分数
        val scoredCandidates = candidates.map { candidate ->
            candidate to calculateMatchScore(call, candidate)
        }.sortedByDescending { it.second }
        
        val bestScore = scoredCandidates.first().second
        val bestCandidates = scoredCandidates.takeWhile { it.second == bestScore }
        
        // 如果有多个最佳匹配，返回null表示歧义
        return if (bestCandidates.size == 1) {
            bestCandidates.first().first
        } else {
            null
        }
    }
    
    /**
     * 计算匹配分数
     */
    private fun calculateMatchScore(call: CallExpression, signature: FunctionSignature): Int {
        var score = 0
        val arguments = call.arguments
        val parameters = signature.parameters
        
        // 精确类型匹配得分更高
        for (i in arguments.indices) {
            if (i < parameters.size) {
                val argType = arguments[i].value.inferredType
                val paramType = parameters[i].type
                
                when {
                    argType == paramType -> score += 100  // 精确匹配
                    argType != null && paramType.isAssignableFrom(argType) -> score += 50  // 兼容匹配
                    else -> score -= 10  // 不匹配
                }
            }
        }
        
        // 参数数量匹配得分
        if (arguments.size == parameters.size) {
            score += 20
        }
        
        // 没有默认参数的函数得分更高
        if (parameters.none { it.hasDefaultValue }) {
            score += 10
        }
        
        return score
    }
    
    /**
     * 检查类型是否等价
     */
    private fun areTypesEquivalent(type1: Type, type2: Type): Boolean {
        return type1 == type2 || 
               (type1.name == type2.name && type1.nullable == type2.nullable)
    }
}

/**
 * 重载检查结果
 */
data class OverloadResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * 重载解析结果
 */
data class OverloadResolutionResult(
    val isValid: Boolean,
    val errors: List<String>,
    val selectedFunction: FunctionSignature?
)
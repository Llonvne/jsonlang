package cn.llonvne.jsonlang.core.source

import kotlinx.serialization.Serializable

/**
 * 源码位置信息
 * 用于跟踪 AST 节点在源文件中的位置，支持错误报告和调试
 */
@Serializable
data class SourceLocation(
    /** 源文件路径 */
    val file: String,
    /** 起始行号 (从1开始) */
    val startLine: Int,
    /** 起始列号 (从1开始) */
    val startColumn: Int,
    /** 结束行号 (从1开始) */
    val endLine: Int,
    /** 结束列号 (从1开始) */
    val endColumn: Int,
    /** 起始字符偏移量 (从0开始) */
    val startOffset: Int,
    /** 结束字符偏移量 (从0开始) */
    val endOffset: Int
) {
    /**
     * 检查位置是否有效
     */
    fun isValid(): Boolean {
        return startLine > 0 && startColumn > 0 && endLine > 0 && endColumn > 0 &&
                startLine <= endLine && 
                (startLine < endLine || startColumn <= endColumn) &&
                startOffset >= 0 && endOffset >= startOffset
    }
    
    /**
     * 获取位置范围
     */
    fun getRange(): SourceRange = SourceRange(startLine, startColumn, endLine, endColumn)
    
    /**
     * 合并两个位置，创建包含两者的新位置
     */
    fun merge(other: SourceLocation): SourceLocation {
        require(this.file == other.file) { "无法合并不同文件的位置信息" }
        
        val newStartLine = minOf(this.startLine, other.startLine)
        val newStartColumn = if (newStartLine == this.startLine) this.startColumn else other.startColumn
        val newEndLine = maxOf(this.endLine, other.endLine)
        val newEndColumn = if (newEndLine == this.endLine) this.endColumn else other.endColumn
        val newStartOffset = minOf(this.startOffset, other.startOffset)
        val newEndOffset = maxOf(this.endOffset, other.endOffset)
        
        return SourceLocation(
            file = this.file,
            startLine = newStartLine,
            startColumn = newStartColumn,
            endLine = newEndLine,
            endColumn = newEndColumn,
            startOffset = newStartOffset,
            endOffset = newEndOffset
        )
    }
    
    /**
     * 检查是否包含另一个位置
     */
    fun contains(other: SourceLocation): Boolean {
        if (this.file != other.file) return false
        
        return this.startOffset <= other.startOffset && this.endOffset >= other.endOffset
    }
    
    /**
     * 检查是否与另一个位置重叠
     */
    fun overlaps(other: SourceLocation): Boolean {
        if (this.file != other.file) return false
        
        return !(this.endOffset < other.startOffset || other.endOffset < this.startOffset)
    }
    
    /**
     * 格式化为字符串
     */
    override fun toString(): String {
        return if (startLine == endLine) {
            if (startColumn == endColumn) {
                "$file:$startLine:$startColumn"
            } else {
                "$file:$startLine:$startColumn-$endColumn"
            }
        } else {
            "$file:$startLine:$startColumn-$endLine:$endColumn"
        }
    }
    
    companion object {
        /**
         * 创建未知位置
         */
        fun unknown(file: String = "<unknown>"): SourceLocation {
            return SourceLocation(file, 0, 0, 0, 0, 0, 0)
        }
        
        /**
         * 创建单点位置
         */
        fun point(file: String, line: Int, column: Int, offset: Int): SourceLocation {
            return SourceLocation(file, line, column, line, column, offset, offset)
        }
        
        /**
         * 从范围创建位置
         */
        fun fromRange(file: String, range: SourceRange, startOffset: Int, endOffset: Int): SourceLocation {
            return SourceLocation(
                file = file,
                startLine = range.startLine,
                startColumn = range.startColumn,
                endLine = range.endLine,
                endColumn = range.endColumn,
                startOffset = startOffset,
                endOffset = endOffset
            )
        }
    }
}

/**
 * 源码范围信息
 * 仅包含行列信息，不包含文件和偏移量
 */
@Serializable
data class SourceRange(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
) {
    /**
     * 检查范围是否有效
     */
    fun isValid(): Boolean {
        return startLine > 0 && startColumn > 0 && endLine > 0 && endColumn > 0 &&
                startLine <= endLine && 
                (startLine < endLine || startColumn <= endColumn)
    }
    
    /**
     * 格式化为字符串
     */
    override fun toString(): String {
        return if (startLine == endLine) {
            if (startColumn == endColumn) {
                "$startLine:$startColumn"
            } else {
                "$startLine:$startColumn-$endColumn"
            }
        } else {
            "$startLine:$startColumn-$endLine:$endColumn"
        }
    }
}

/**
 * 源码位置构建器
 * 用于在解析过程中构建位置信息
 */
class SourceLocationBuilder(private val file: String) {
    private var currentLine = 1
    private var currentColumn = 1
    private var currentOffset = 0
    
    /**
     * 前进到下一个字符
     */
    fun advance(char: Char) {
        if (char == '\n') {
            currentLine++
            currentColumn = 1
        } else {
            currentColumn++
        }
        currentOffset++
    }
    
    /**
     * 前进多个字符
     */
    fun advance(text: String) {
        for (char in text) {
            advance(char)
        }
    }
    
    /**
     * 获取当前位置
     */
    fun getCurrentLocation(): SourceLocation {
        return SourceLocation.point(file, currentLine, currentColumn, currentOffset)
    }
    
    /**
     * 创建从起始位置到当前位置的范围
     */
    fun createRange(startLocation: SourceLocation): SourceLocation {
        return SourceLocation(
            file = file,
            startLine = startLocation.startLine,
            startColumn = startLocation.startColumn,
            endLine = currentLine,
            endColumn = currentColumn,
            startOffset = startLocation.startOffset,
            endOffset = currentOffset
        )
    }
    
    /**
     * 重置到指定位置
     */
    fun reset(line: Int = 1, column: Int = 1, offset: Int = 0) {
        currentLine = line
        currentColumn = column
        currentOffset = offset
    }
}
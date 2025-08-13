package cn.llonvne.jsonlang.core.lexer

import cn.llonvne.jsonlang.core.source.SourceLocation

/**
 * JSONLang Token 类型枚举
 * 定义了 JSONLang 语言中所有可能的 Token 类型
 */
enum class TokenType {
    // JSON 基础 Token
    LEFT_BRACE,         // {
    RIGHT_BRACE,        // }
    LEFT_BRACKET,       // [
    RIGHT_BRACKET,      // ]
    COMMA,              // ,
    COLON,              // :
    
    // JSONLang 扩展操作符
    SEMICOLON,          // ;
    DOT,                // .
    ARROW,              // ->
    FAT_ARROW,          // =>
    DOUBLE_COLON,       // ::
    QUESTION,           // ?
    EXCLAMATION,        // !
    
    // 算术操作符
    PLUS,               // +
    MINUS,              // -
    MULTIPLY,           // *
    DIVIDE,             // /
    MODULO,             // %
    POWER,              // **
    
    // 比较操作符
    EQUAL,              // ==
    NOT_EQUAL,          // !=
    LESS_THAN,          // <
    LESS_EQUAL,         // <=
    GREATER_THAN,       // >
    GREATER_EQUAL,      // >=
    
    // 逻辑操作符
    AND,                // &&
    OR,                 // ||
    NOT,                // !
    
    // 赋值操作符
    ASSIGN,             // =
    PLUS_ASSIGN,        // +=
    MINUS_ASSIGN,       // -=
    MULTIPLY_ASSIGN,    // *=
    DIVIDE_ASSIGN,      // /=
    
    // 位操作符
    BIT_AND,            // &
    BIT_OR,             // |
    BIT_XOR,            // ^
    BIT_NOT,            // ~
    LEFT_SHIFT,         // <<
    RIGHT_SHIFT,        // >>
    
    // JSONLang 关键字
    METADATA,           // metadata
    IMPORTS,            // imports
    EXPORTS,            // exports
    TYPES,              // types
    CONSTANTS,          // constants
    FUNCTIONS,          // functions
    MACROS,             // macros
    DSL,                // dsl
    
    // 类型关键字
    STRING,             // String
    INT,                // Int
    LONG,               // Long
    FLOAT,              // Float
    DOUBLE,             // Double
    BOOLEAN,            // Boolean
    UNIT,               // Unit
    ANY,                // Any
    NOTHING,            // Nothing
    NULL,               // null
    
    // 控制流关键字
    IF,                 // if
    ELSE,               // else
    WHEN,               // when
    FOR,                // for
    WHILE,              // while
    DO,                 // do
    BREAK,              // break
    CONTINUE,           // continue
    RETURN,             // return
    
    // 异常处理关键字
    TRY,                // try
    CATCH,              // catch
    FINALLY,            // finally
    THROW,              // throw
    
    // 函数相关关键字
    FUN,                // fun
    LAMBDA,             // lambda
    ASYNC,              // async
    AWAIT,              // await
    SUSPEND,            // suspend
    INLINE,             // inline
    
    // 可见性修饰符
    PUBLIC,             // public
    PRIVATE,            // private
    PROTECTED,          // protected
    INTERNAL,           // internal
    
    // 其他修饰符
    ABSTRACT,           // abstract
    FINAL,              // final
    OPEN,               // open
    OVERRIDE,           // override
    VIRTUAL,            // virtual
    STATIC,             // static
    
    // 类型定义关键字
    STRUCT,             // struct
    ENUM,               // enum
    UNION,              // union
    ALIAS,              // alias
    INTERFACE,          // interface
    CLASS,              // class
    
    // 模块系统关键字
    IMPORT,             // import
    EXPORT,             // export
    FROM,               // from
    AS,                 // as
    
    // 宏和元编程关键字
    MACRO,              // macro
    QUOTE,              // quote
    UNQUOTE,            // unquote
    SPLICE,             // splice
    EVAL,               // eval
    
    // 中文关键字（可选支持）
    元数据,              // metadata
    导入,                // imports
    导出,                // exports
    类型,                // types
    常量,                // constants
    函数,                // functions
    宏,                  // macros
    领域语言,            // dsl
    如果,                // if
    否则,                // else
    当,                  // when
    循环,                // for
    当循环,              // while
    执行,                // do
    跳出,                // break
    继续,                // continue
    返回,                // return
    尝试,                // try
    捕获,                // catch
    最终,                // finally
    抛出,                // throw
    
    // 字面量
    STRING_LITERAL,     // "string"
    INTEGER_LITERAL,    // 123
    FLOAT_LITERAL,      // 123.45
    BOOLEAN_LITERAL,    // true, false
    NULL_LITERAL,       // null
    
    // 标识符和注释
    IDENTIFIER,         // 标识符
    COMMENT,            // 注释
    
    // 特殊 Token
    NEWLINE,            // 换行符
    WHITESPACE,         // 空白字符
    EOF,                // 文件结束
    ERROR,              // 错误 Token
    
    // 宏和 DSL 特殊 Token
    MACRO_START,        // 宏开始标记
    MACRO_END,          // 宏结束标记
    DSL_START,          // DSL 开始标记
    DSL_END,            // DSL 结束标记
    TEMPLATE_START,     // 模板开始 ${
    TEMPLATE_END,       // 模板结束 }
    
    // 扩展语法糖 Token
    OPTIONAL_CHAIN,     // 可选链 ?.
    NULL_COALESCING,    // 空合并 ??
    PIPE,               // 管道操作符 |>
    RANGE,              // 范围操作符 ..
    SPREAD,             // 展开操作符 ...
    
    // DSL 特定 Token
    SQL_SELECT,         // SQL SELECT
    SQL_FROM,           // SQL FROM
    SQL_WHERE,          // SQL WHERE
    HTML_TAG,           // HTML 标签
    CSS_PROPERTY,       // CSS 属性
    REGEX_PATTERN,      // 正则表达式模式
    
    // 字符串插值 Token
    INTERPOLATION_START, // 插值开始
    INTERPOLATION_END,   // 插值结束
    STRING_PART,         // 字符串片段
}

/**
 * Token 数据类
 * 表示词法分析过程中识别的一个 Token
 */
data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any? = null,
    val location: SourceLocation
) {
    /**
     * 检查 Token 是否为关键字
     */
    fun isKeyword(): Boolean = when (type) {
        TokenType.METADATA, TokenType.IMPORTS, TokenType.EXPORTS, TokenType.TYPES,
        TokenType.CONSTANTS, TokenType.FUNCTIONS, TokenType.MACROS, TokenType.DSL,
        TokenType.STRING, TokenType.INT, TokenType.LONG, TokenType.FLOAT,
        TokenType.DOUBLE, TokenType.BOOLEAN, TokenType.UNIT, TokenType.ANY,
        TokenType.NOTHING, TokenType.NULL, TokenType.IF, TokenType.ELSE,
        TokenType.WHEN, TokenType.FOR, TokenType.WHILE, TokenType.DO,
        TokenType.BREAK, TokenType.CONTINUE, TokenType.RETURN, TokenType.TRY,
        TokenType.CATCH, TokenType.FINALLY, TokenType.THROW, TokenType.FUN,
        TokenType.LAMBDA, TokenType.ASYNC, TokenType.AWAIT, TokenType.SUSPEND,
        TokenType.INLINE, TokenType.PUBLIC, TokenType.PRIVATE, TokenType.PROTECTED,
        TokenType.INTERNAL, TokenType.ABSTRACT, TokenType.FINAL, TokenType.OPEN,
        TokenType.OVERRIDE, TokenType.VIRTUAL, TokenType.STATIC, TokenType.STRUCT,
        TokenType.ENUM, TokenType.UNION, TokenType.ALIAS, TokenType.INTERFACE,
        TokenType.CLASS, TokenType.IMPORT, TokenType.EXPORT, TokenType.FROM,
        TokenType.AS, TokenType.MACRO, TokenType.QUOTE, TokenType.UNQUOTE,
        TokenType.SPLICE, TokenType.EVAL,
        // 中文关键字
        TokenType.元数据, TokenType.导入, TokenType.导出, TokenType.类型,
        TokenType.常量, TokenType.函数, TokenType.宏, TokenType.领域语言,
        TokenType.如果, TokenType.否则, TokenType.当, TokenType.循环,
        TokenType.当循环, TokenType.执行, TokenType.跳出, TokenType.继续,
        TokenType.返回, TokenType.尝试, TokenType.捕获, TokenType.最终,
        TokenType.抛出 -> true
        else -> false
    }
    
    /**
     * 检查 Token 是否为操作符
     */
    fun isOperator(): Boolean = when (type) {
        TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE,
        TokenType.MODULO, TokenType.POWER, TokenType.EQUAL, TokenType.NOT_EQUAL,
        TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN,
        TokenType.GREATER_EQUAL, TokenType.AND, TokenType.OR, TokenType.NOT,
        TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
        TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN, TokenType.BIT_AND,
        TokenType.BIT_OR, TokenType.BIT_XOR, TokenType.BIT_NOT,
        TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT -> true
        else -> false
    }
    
    /**
     * 检查 Token 是否为字面量
     */
    fun isLiteral(): Boolean = when (type) {
        TokenType.STRING_LITERAL, TokenType.INTEGER_LITERAL,
        TokenType.FLOAT_LITERAL, TokenType.BOOLEAN_LITERAL,
        TokenType.NULL_LITERAL -> true
        else -> false
    }
    
    /**
     * 检查 Token 是否为分隔符
     */
    fun isDelimiter(): Boolean = when (type) {
        TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE, TokenType.LEFT_BRACKET,
        TokenType.RIGHT_BRACKET, TokenType.COMMA, TokenType.COLON,
        TokenType.SEMICOLON, TokenType.DOT -> true
        else -> false
    }
    
    /**
     * 获取 Token 的优先级（用于表达式解析）
     */
    fun getPrecedence(): Int = when (type) {
        TokenType.OR -> 1
        TokenType.AND -> 2
        TokenType.BIT_OR -> 3
        TokenType.BIT_XOR -> 4
        TokenType.BIT_AND -> 5
        TokenType.EQUAL, TokenType.NOT_EQUAL -> 6
        TokenType.LESS_THAN, TokenType.LESS_EQUAL,
        TokenType.GREATER_THAN, TokenType.GREATER_EQUAL -> 7
        TokenType.LEFT_SHIFT, TokenType.RIGHT_SHIFT -> 8
        TokenType.PLUS, TokenType.MINUS -> 9
        TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO -> 10
        TokenType.POWER -> 11
        else -> 0
    }
    
    /**
     * 检查操作符是否为右结合
     */
    fun isRightAssociative(): Boolean = when (type) {
        TokenType.POWER, TokenType.ASSIGN, TokenType.PLUS_ASSIGN,
        TokenType.MINUS_ASSIGN, TokenType.MULTIPLY_ASSIGN,
        TokenType.DIVIDE_ASSIGN -> true
        else -> false
    }
    
    override fun toString(): String {
        return "Token(type=$type, lexeme='$lexeme', literal=$literal, location=$location)"
    }
}

/**
 * Token 工厂类
 * 提供创建各种 Token 的便捷方法
 */
object TokenFactory {
    /**
     * 创建关键字 Token
     */
    fun keyword(type: TokenType, lexeme: String, location: SourceLocation): Token {
        return Token(type, lexeme, null, location)
    }
    
    /**
     * 创建操作符 Token
     */
    fun operator(type: TokenType, lexeme: String, location: SourceLocation): Token {
        return Token(type, lexeme, null, location)
    }
    
    /**
     * 创建标识符 Token
     */
    fun identifier(lexeme: String, location: SourceLocation): Token {
        return Token(TokenType.IDENTIFIER, lexeme, null, location)
    }
    
    /**
     * 创建字符串字面量 Token
     */
    fun stringLiteral(lexeme: String, value: String, location: SourceLocation): Token {
        return Token(TokenType.STRING_LITERAL, lexeme, value, location)
    }
    
    /**
     * 创建整数字面量 Token
     */
    fun integerLiteral(lexeme: String, value: Long, location: SourceLocation): Token {
        return Token(TokenType.INTEGER_LITERAL, lexeme, value, location)
    }
    
    /**
     * 创建浮点数字面量 Token
     */
    fun floatLiteral(lexeme: String, value: Double, location: SourceLocation): Token {
        return Token(TokenType.FLOAT_LITERAL, lexeme, value, location)
    }
    
    /**
     * 创建布尔字面量 Token
     */
    fun booleanLiteral(lexeme: String, value: Boolean, location: SourceLocation): Token {
        return Token(TokenType.BOOLEAN_LITERAL, lexeme, value, location)
    }
    
    /**
     * 创建空值字面量 Token
     */
    fun nullLiteral(lexeme: String, location: SourceLocation): Token {
        return Token(TokenType.NULL_LITERAL, lexeme, null, location)
    }
    
    /**
     * 创建 EOF Token
     */
    fun eof(location: SourceLocation): Token {
        return Token(TokenType.EOF, "", null, location)
    }
    
    /**
     * 创建错误 Token
     */
    fun error(lexeme: String, location: SourceLocation): Token {
        return Token(TokenType.ERROR, lexeme, null, location)
    }
}
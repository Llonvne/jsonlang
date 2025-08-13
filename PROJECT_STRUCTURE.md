# JSONLang 项目结构

## 已完成的基础设施

### 1. 项目目录结构
```
src/
├── cn/llonvne/jsonlang/
│   ├── core/                           # 核心模块
│   │   ├── ast/                        # 抽象语法树定义
│   │   │   ├── ASTNode.kt             # AST 节点基类和程序根节点
│   │   │   ├── ASTVisitor.kt          # 访问者模式接口
│   │   │   ├── Declarations.kt        # 声明节点（导入、导出、类型、函数等）
│   │   │   ├── Statements.kt          # 语句节点（变量声明、控制流等）
│   │   │   └── Expressions.kt         # 表达式节点（字面量、操作符、调用等）
│   │   ├── source/                     # 源码位置跟踪
│   │   │   └── SourceLocation.kt      # 位置信息和构建器
│   │   ├── annotations/                # 注解系统
│   │   │   └── Annotation.kt          # 注解定义和处理器
│   │   ├── types/                      # 类型系统
│   │   │   └── Type.kt                # 完整的类型系统定义
│   │   └── errors/                     # 错误报告系统
│   │       └── ErrorReporting.kt      # 错误报告和恢复机制
│   ├── compiler/                       # 编译器模块（占位符）
│   │   └── CompilerModule.kt
│   ├── runtime/                        # 运行时模块（占位符）
│   │   └── RuntimeModule.kt
│   └── testing/                        # 测试模块（占位符）
│       └── TestingModule.kt
├── CoreInfrastructureTest.kt          # 核心基础设施测试
└── main.kt                            # 主入口点
docs/
└── README.md                          # 项目文档
```

### 2. 核心 AST 节点接口

#### 基础节点类型
- **ASTNode**: 所有 AST 节点的基类，包含位置信息、注解和访问者模式支持
- **Program**: 程序根节点，包含元数据、导入、导出、类型、常量、函数、宏和 DSL 声明
- **Metadata**: 模块元数据节点

#### 声明节点
- **Declaration**: 声明节点基类
- **ImportDeclaration**: 导入声明，支持版本控制、别名、选择性导入
- **ExportDeclaration**: 导出声明，支持多种导出类型
- **TypeDeclaration**: 类型声明，支持结构体、枚举、联合类型等
- **FunctionDeclaration**: 函数声明，支持修饰符、泛型、参数
- **MacroDeclaration**: 宏声明，支持编译时和运行时宏
- **DSLDeclaration**: DSL 声明，支持语法扩展

#### 语句节点
- **Statement**: 语句节点基类
- **VariableDeclaration**: 变量声明
- **ConditionalStatement**: 条件语句
- **LoopStatement**: 循环语句（For、While）
- **JumpStatement**: 跳转语句（Return、Break、Continue）
- **TryCatchStatement**: 异常处理
- **MatchStatement**: 模式匹配

#### 表达式节点
- **Expression**: 表达式节点基类，包含类型推导信息
- **LiteralExpression**: 字面量表达式（字符串、整数、浮点数、布尔、空值）
- **BinaryExpression**: 二元操作表达式
- **UnaryExpression**: 一元操作表达式
- **CallExpression**: 函数调用表达式
- **MemberExpression**: 成员访问表达式
- **ArrayExpression**: 数组表达式
- **ObjectExpression**: 对象表达式
- **LambdaExpression**: Lambda 表达式

### 3. 源码位置跟踪系统

#### SourceLocation
- 完整的位置信息：文件、行列号、字符偏移量
- 位置合并、包含检查、重叠检查
- 格式化输出支持

#### SourceLocationBuilder
- 解析过程中的位置构建器
- 字符前进和范围创建功能

### 4. 类型系统

#### 基础类型
- **PrimitiveType**: 基础类型（String、Int、Long、Float、Double、Boolean、Unit、Any、Nothing、Null）
- 支持可空和非空版本
- 类型兼容性检查

#### 复合类型
- **ArrayType**: 数组类型，支持泛型元素类型
- **ObjectType**: 对象类型，支持键值类型参数
- **FunctionType**: 函数类型，支持参数和返回类型
- **OptionalType**: 可选类型

#### 用户定义类型
- **StructType**: 结构体类型
- **EnumType**: 枚举类型
- **UnionType**: 联合类型
- **AliasType**: 类型别名

#### 泛型类型
- **GenericType**: 泛型类型参数，支持约束

### 5. 注解系统

#### Annotation
- 注解定义，支持参数、目标、保留策略
- 预定义注解常量
- 注解处理器接口

#### 注解值类型
- 支持字符串、整数、浮点数、布尔、数组、对象、类型、枚举值

### 6. 错误报告系统

#### CompilerError
- 完整的错误信息：代码、消息、严重程度、类别、位置
- 相关位置、修复建议、帮助信息
- 格式化输出

#### ErrorReporter
- 错误报告器接口和默认实现
- 错误统计和查询功能

#### 预定义错误
- 错误代码常量
- 中文错误消息模板
- 错误恢复策略

### 7. 访问者模式支持

#### ASTVisitor
- 完整的访问者接口，覆盖所有 AST 节点类型
- BaseASTVisitor: 抽象基类，提供默认实现
- RecursiveASTVisitor: 递归访问者，自动遍历子节点

## 测试验证

创建了 `CoreInfrastructureTest` 来验证：
- 源码位置跟踪功能
- AST 节点创建和访问
- 类型系统基本功能
- 错误报告系统

所有测试通过，证明核心基础设施工作正常。

## 下一步

基础设施已完成，可以开始实现：
1. 词法分析器（任务 2.1）
2. 语法分析器（任务 3.1）
3. 类型检查器（任务 4.1）
等后续功能模块。
# JSONLang 中文规范文档

## 概述

JSONLang 是一个基于 JSON 格式的现代编程语言，旨在结合 JSON 的简洁性和现代编程语言的表达力。

## 项目结构

```
src/
├── cn/llonvne/jsonlang/
│   ├── core/                    # 核心模块
│   │   ├── ast/                 # 抽象语法树定义
│   │   ├── source/              # 源码位置跟踪
│   │   ├── annotations/         # 注解系统
│   │   ├── types/               # 类型系统
│   │   └── errors/              # 错误报告系统
│   ├── compiler/                # 编译器模块
│   ├── runtime/                 # 运行时模块
│   └── testing/                 # 测试模块
docs/                            # 文档模块
```

## 核心特性

1. **代码即数据** - 利用 JSON 的结构化特性
2. **多后端支持** - 支持编译到 Kotlin、Go、JavaScript
3. **强大的类型系统** - 静态类型检查和类型推导
4. **元编程能力** - 宏系统和 DSL 支持
5. **中文友好** - 完整的中文开发体验

## 开发指南

详细的开发指南将在后续任务中完善。

## 许可证

MIT License
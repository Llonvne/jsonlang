#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JSONLang Python后端
完全独立的Python实现，可以独立运行JSON程序
"""

import json
import sys
import os
import time
import math
import random
from typing import Dict, Any, List, Optional

class PythonBackend:
    """Python后端实现"""
    
    def __init__(self):
        self.name = "python"
        self.version = "1.0.0"
        self.functions = {}
        self.stdlib_data = {}
        self._load_stdlib()
        self._register_functions()
    
    def _load_stdlib(self):
        """加载Python标准库定义"""
        try:
            with open('stdlib.python.json', 'r', encoding='utf-8') as f:
                self.stdlib_data = json.load(f)
        except FileNotFoundError:
            print("警告: 找不到 stdlib.python.json 文件，使用默认函数注册")
            self.stdlib_data = {}
        except Exception as e:
            print(f"警告: 加载 stdlib.python.json 失败: {e}")
            self.stdlib_data = {}
    
    def get_name(self) -> str:
        return self.name
    
    def get_version(self) -> str:
        return self.version
    
    def get_functions(self) -> Dict[str, Any]:
        return self.functions
    
    def execute_function(self, func_name: str, args: List[Any]) -> Any:
        """执行Python函数"""
        if func_name not in self.functions:
            raise NameError(f"Python后端不支持函数: {func_name}")
        
        func = self.functions[func_name]
        return func(*args)
    
    def _register_functions(self):
        """注册Python标准库函数"""
        # 如果加载了stdlib文件，根据stdlib定义注册函数
        if self.stdlib_data and 'functions' in self.stdlib_data:
            stdlib_functions = self.stdlib_data['functions']
            for func_name, func_info in stdlib_functions.items():
                if isinstance(func_info, dict) and 'implementation' in func_info:
                    impl_name = func_info['implementation']
                    # 根据实现名称映射到实际函数
                    actual_func = self._get_function_by_impl_name(impl_name)
                    if actual_func:
                        self.functions[func_name] = actual_func
                    else:
                        print(f"警告: 找不到实现函数 '{impl_name}' 用于 '{func_name}'")
        else:
            # 默认函数注册（向后兼容）
            self._register_default_functions()
    
    def _get_function_by_impl_name(self, impl_name: str):
        """根据实现名称获取实际函数"""
        # 映射实现名称到实际函数
        impl_mapping = {
            'print': self.println,
            'print_no_newline': self.print,
            'input': self.input,
            'read_file': self.read_file,
            'write_file': self.write_file,
            'add': self.add,
            'subtract': self.subtract,
            'multiply': self.multiply,
            'divide': self.divide,
            'power': self.power,
            'sqrt': self.sqrt,
            'abs': self.abs,
            'floor': self.floor,
            'ceil': self.ceil,
            'round': self.round,
            'concat': self.concat,
            'length': self.length,
            'substring': self.substring,
            'to_upper': self.to_upper,
            'to_lower': self.to_lower,
            'trim': self.trim,
            'split': self.split,
            'join': self.join,
            'array_create': self.array_create,
            'array_push': self.array_push,
            'array_pop': self.array_pop,
            'array_get': self.array_get,
            'array_set': self.array_set,
            'array_length': self.array_length,
            'array_sort': self.array_sort,
            'array_reverse': self.array_reverse,
            'sleep': self.sleep,
            'random': self.random,
            'random_int': self.random_int,
            'time_now': self.time_now,
            'exit': self.exit,
            'to_string': self.to_string,
            'to_number': self.to_number,
            'to_boolean': self.to_boolean,
            'is_empty': self.is_empty,
            'is_number': self.is_number,
            'is_string': self.is_string,
            'is_array': self.is_array,
            'is_boolean': self.is_boolean,
        }
        return impl_mapping.get(impl_name)
    
    def _register_default_functions(self):
        """注册默认函数（向后兼容）"""
        # 输入输出函数
        self.functions["println"] = self.println
        self.functions["print"] = self.print
        self.functions["input"] = self.input
        self.functions["read_file"] = self.read_file
        self.functions["write_file"] = self.write_file
        
        # 数学函数
        self.functions["add"] = self.add
        self.functions["subtract"] = self.subtract
        self.functions["multiply"] = self.multiply
        self.functions["divide"] = self.divide
        self.functions["power"] = self.power
        self.functions["sqrt"] = self.sqrt
        self.functions["abs"] = self.abs
        self.functions["floor"] = self.floor
        self.functions["ceil"] = self.ceil
        self.functions["round"] = self.round
        
        # 字符串函数
        self.functions["concat"] = self.concat
        self.functions["length"] = self.length
        self.functions["substring"] = self.substring
        self.functions["to_upper"] = self.to_upper
        self.functions["to_lower"] = self.to_lower
        self.functions["trim"] = self.trim
        self.functions["split"] = self.split
        self.functions["join"] = self.join
        
        # 数组函数
        self.functions["array_create"] = self.array_create
        self.functions["array_push"] = self.array_push
        self.functions["array_pop"] = self.array_pop
        self.functions["array_get"] = self.array_get
        self.functions["array_set"] = self.array_set
        self.functions["array_length"] = self.array_length
        self.functions["array_sort"] = self.array_sort
        self.functions["array_reverse"] = self.array_reverse
        
        # 系统函数
        self.functions["sleep"] = self.sleep
        self.functions["random"] = self.random
        self.functions["random_int"] = self.random_int
        self.functions["time_now"] = self.time_now
        self.functions["exit"] = self.exit
        
        # 类型转换函数
        self.functions["to_string"] = self.to_string
        self.functions["to_number"] = self.to_number
        self.functions["to_boolean"] = self.to_boolean
        
        # 逻辑函数
        self.functions["is_empty"] = self.is_empty
        self.functions["is_number"] = self.is_number
        self.functions["is_string"] = self.is_string
        self.functions["is_array"] = self.is_array
        self.functions["is_boolean"] = self.is_boolean
    
    # 函数实现
    def println(self, *args):
        print(*args)
        return None
    
    def print(self, *args):
        print(*args, end='')
        return None
    
    def input(self, prompt=""):
        return input(prompt)
    
    def read_file(self, filename):
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception as e:
            return f"错误: 无法读取文件 '{filename}': {e}"
    
    def write_file(self, filename, content):
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(str(content))
            return True
        except Exception:
            return False
    
    def add(self, a, b):
        return float(a) + float(b)
    
    def subtract(self, a, b):
        return float(a) - float(b)
    
    def multiply(self, a, b):
        return float(a) * float(b)
    
    def divide(self, a, b):
        if float(b) == 0:
            raise ValueError("错误: 除数不能为零")
        return float(a) / float(b)
    
    def power(self, base, exponent):
        return math.pow(float(base), float(exponent))
    
    def sqrt(self, x):
        if float(x) < 0:
            raise ValueError("错误: 不能计算负数的平方根")
        return math.sqrt(float(x))
    
    def abs(self, x):
        return abs(float(x))
    
    def floor(self, x):
        return math.floor(float(x))
    
    def ceil(self, x):
        return math.ceil(float(x))
    
    def round(self, x, decimals=0):
        return round(float(x), int(decimals))
    
    def concat(self, *args):
        return ''.join(str(arg) for arg in args)
    
    def length(self, s):
        return len(str(s))
    
    def substring(self, s, start, end=None):
        s = str(s)
        start = int(start)
        if end is None:
            return s[start:]
        end = int(end)
        return s[start:end]
    
    def to_upper(self, s):
        return str(s).upper()
    
    def to_lower(self, s):
        return str(s).lower()
    
    def trim(self, s):
        return str(s).strip()
    
    def split(self, s, delimiter=" "):
        return str(s).split(delimiter)
    
    def join(self, array, delimiter=""):
        return delimiter.join(str(item) for item in array)
    
    def array_create(self, *items):
        return list(items)
    
    def array_push(self, array, item):
        if not isinstance(array, list):
            raise TypeError("错误: 第一个参数必须是数组")
        array.append(item)
        return array
    
    def array_pop(self, array):
        if not isinstance(array, list):
            raise TypeError("错误: 参数必须是数组")
        if not array:
            raise ValueError("错误: 数组为空")
        return array.pop()
    
    def array_get(self, array, index):
        if not isinstance(array, list):
            raise TypeError("错误: 第一个参数必须是数组")
        index = int(index)
        if index < 0 or index >= len(array):
            raise IndexError("错误: 数组索引越界")
        return array[index]
    
    def array_set(self, array, index, value):
        if not isinstance(array, list):
            raise TypeError("错误: 第一个参数必须是数组")
        index = int(index)
        if index < 0 or index >= len(array):
            raise IndexError("错误: 数组索引越界")
        array[index] = value
        return array
    
    def array_length(self, array):
        if not isinstance(array, list):
            raise TypeError("错误: 参数必须是数组")
        return len(array)
    
    def array_sort(self, array, reverse=False):
        if not isinstance(array, list):
            raise TypeError("错误: 第一个参数必须是数组")
        return sorted(array, reverse=reverse)
    
    def array_reverse(self, array):
        if not isinstance(array, list):
            raise TypeError("错误: 参数必须是数组")
        return list(reversed(array))
    
    def sleep(self, seconds):
        time.sleep(float(seconds))
        return None
    
    def random(self):
        return random.random()
    
    def random_int(self, min_val, max_val):
        return random.randint(int(min_val), int(max_val))
    
    def time_now(self):
        return time.time()
    
    def exit(self, code=0):
        sys.exit(int(code))
    
    def to_string(self, value):
        return str(value)
    
    def to_number(self, value):
        try:
            return float(value)
        except (ValueError, TypeError):
            return 0.0
    
    def to_boolean(self, value):
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            return value.lower() in ('true', '1', 'yes', 'on')
        if isinstance(value, (int, float)):
            return value != 0
        return bool(value)
    
    def is_empty(self, value):
        if isinstance(value, str):
            return len(value.strip()) == 0
        if isinstance(value, list):
            return len(value) == 0
        return value is None
    
    def is_number(self, value):
        try:
            float(value)
            return True
        except (ValueError, TypeError):
            return False
    
    def is_string(self, value):
        return isinstance(value, str)
    
    def is_array(self, value):
        return isinstance(value, list)
    
    def is_boolean(self, value):
        return isinstance(value, bool)

class JSONProgram:
    """JSON程序结构"""
    
    def __init__(self, data: Dict[str, Any]):
        self.metadata = data.get('metadata', {})
        self.imports = data.get('imports', {})
        self.functions = data.get('functions', {})
        self.modifiers = data.get('modifiers', [])
        self.loaded_modules = {}  # 存储已加载的模块
    
    def get_function(self, name: str) -> Optional[Dict[str, Any]]:
        return self.functions.get(name)
    
    def has_function(self, name: str) -> bool:
        return name in self.functions
    
    def load_module(self, module_path: str) -> 'JSONProgram':
        """加载模块"""
        if module_path in self.loaded_modules:
            return self.loaded_modules[module_path]
        
        # 尝试不同的文件扩展名和路径
        possible_paths = [
            module_path + '.json',
            module_path + '',
            # 尝试从包路径中提取文件名
            module_path.split('.')[-1] + '.json',
            module_path.split('.')[-1] + ''
        ]
        
        module_file = None
        
        for test_path in possible_paths:
            if os.path.exists(test_path):
                module_file = test_path
                break
        
        if not module_file:
            raise FileNotFoundError(f"找不到模块文件: {module_path}")
        
        try:
            with open(module_file, 'r', encoding='utf-8') as f:
                module_data = json.load(f)
            
            module_program = JSONProgram(module_data)
            self.loaded_modules[module_path] = module_program
            return module_program
            
        except Exception as e:
            raise RuntimeError(f"加载模块失败 {module_path}: {e}")
    
    def get_function(self, name: str) -> Optional[Dict[str, Any]]:
        return self.functions.get(name)
    
    def has_function(self, name: str) -> bool:
        return name in self.functions

class JSONLangRuntime:
    """JSONLang运行时环境"""
    
    def __init__(self, backend: PythonBackend):
        self.backend = backend
        self.variables = {}
    
    def run_program(self, program: JSONProgram) -> Any:
        """运行JSON程序"""
        # 应用modifiers
        self.apply_modifiers(program)
        
        # 查找main函数
        main_func = program.get_function('main')
        if not main_func:
            raise ValueError("程序缺少main函数")
        
        # 执行main函数
        print("开始执行程序...")
        result = self.execute_function(program, 'main', [])
        print(f"程序执行完成，返回值: {result}")
        return result
    
    def apply_modifiers(self, program: JSONProgram):
        """应用modifiers到所有函数"""
        for func_name, func_data in program.functions.items():
            if isinstance(func_data, dict):
                # 获取函数的modifiers
                func_modifiers = func_data.get('modifiers', [])
                
                # 应用每个modifier
                for modifier_name in func_modifiers:
                    self.apply_modifier(program, func_name, func_data, modifier_name)
    
    def apply_modifier(self, program: JSONProgram, func_name: str, func_data: Dict[str, Any], modifier_name: str):
        """应用单个modifier到函数"""
        # 查找modifier定义
        modifier = None
        for mod in program.modifiers:
            if mod.get('name') == modifier_name:
                modifier = mod
                break
        
        if not modifier:
            print(f"警告: 找不到modifier '{modifier_name}'")
            return
        
        # 检查条件
        condition = modifier.get('condiction', '')
        if condition and not self.evaluate_condition(func_data, condition):
            return
        
        # 执行actions
        actions = modifier.get('actions', [])
        for action in actions:
            self.execute_modifier_action(func_data, action)
    
    def evaluate_condition(self, func_data: Dict[str, Any], condition: str) -> bool:
        """评估modifier条件"""
        # 简单的条件评估，支持基本的undefined检查
        if 'undefined' in condition:
            # 提取变量名
            var_name = condition.split('==')[0].strip()
            if var_name == 'function.args':
                return 'args' not in func_data
            elif var_name == 'function.return':
                return 'return' not in func_data
            elif var_name == 'function.modifiers':
                return 'modifiers' not in func_data
            elif var_name == 'function.visibility':
                return 'visibility' not in func_data
        
        return True  # 默认返回True
    
    def execute_modifier_action(self, func_data: Dict[str, Any], action: Dict[str, Any]):
        """执行modifier action"""
        action_type = action.get('type')
        target = action.get('target')
        value = action.get('value')
        
        if action_type == 'assignment':
            # 提取目标字段名
            if target.startswith('function.'):
                field_name = target.split('.')[1]
                func_data[field_name] = value
    
    def execute_function(self, program: JSONProgram, func_name: str, args: List[Any]) -> Any:
        """执行函数"""
        func_data = program.get_function(func_name)
        if not func_data:
            raise NameError(f"函数 '{func_name}' 未定义")
        
        actions = func_data.get('actions', [])
        result = None
        
        for action in actions:
            action_type = action.get('type')
            
            if action_type == 'function_call':
                result = self.execute_function_call(program, action)
            elif action_type == 'variable_declaration':
                result = self.execute_variable_declaration(action)
            elif action_type == 'assignment':
                result = self.execute_assignment(action)
            elif action_type == 'if_statement':
                result = self.execute_if_statement(action)
            elif action_type == 'loop':
                result = self.execute_loop(action)
            elif action_type == 'return':
                result = self.execute_return(action)
            elif action_type == 'literal':
                result = self.execute_literal(action)
        
        return result
    
    def execute_function_call(self, program: JSONProgram, action: Dict[str, Any]) -> Any:
        """执行函数调用"""
        function = action.get('function')
        args_data = action.get('args', [])
        
        # 评估参数
        args = []
        for arg_data in args_data:
            if isinstance(arg_data, dict):
                arg_type = arg_data.get('type')
                arg_value = arg_data.get('value')
                
                if arg_type in ['String', 'imports.String']:
                    args.append(str(arg_value))
                elif arg_type in ['Number', 'imports.Number']:
                    args.append(float(arg_value))
                elif arg_type in ['Boolean', 'imports.Boolean']:
                    args.append(bool(arg_value))
                else:
                    args.append(arg_data)
            else:
                args.append(arg_data)
        
        # 检查是否是用户定义函数
        if program.has_function(function):
            return self.execute_function(program, function, args)
        
        # 检查是否是导入函数
        if function in program.imports:
            import_path = program.imports[function]
            
            # 处理第三方库导入
            if '.' in import_path and not import_path.startswith('jsonlang.'):
                # 解析模块路径和函数名
                parts = import_path.split('.')
                if len(parts) >= 2:
                    module_path = '.'.join(parts[:-1])
                    func_name = parts[-1]
                    
                    # 加载模块
                    try:
                        module_program = program.load_module(module_path)
                        if module_program.has_function(func_name):
                            return self.execute_function(module_program, func_name, args)
                        else:
                            raise NameError(f"模块 '{module_path}' 中没有函数 '{func_name}'")
                    except Exception as e:
                        raise RuntimeError(f"导入模块失败: {e}")
            
            # 处理标准库函数
            backend_func = import_path.replace('jsonlang.', '')
            return self.backend.execute_function(backend_func, args)
        
        # 检查是否是带前缀的函数调用（如 imports.hello）
        if function.startswith('imports.'):
            func_name = function.replace('imports.', '')
            
            # 查找导入映射（键值对的形式）
            import_path = None
            for key, value in program.imports.items():
                if value == func_name:
                    import_path = key
                    break
            
            if import_path:
                # 处理第三方库导入
                if '.' in import_path and not import_path.startswith('jsonlang.'):
                    # 解析模块路径和函数名
                    parts = import_path.split('.')
                    if len(parts) >= 2:
                        module_path = '.'.join(parts[:-1])
                        actual_func_name = parts[-1]
                        
                        # 加载模块
                        try:
                            module_program = program.load_module(module_path)
                            if module_program.has_function(actual_func_name):
                                return self.execute_function(module_program, actual_func_name, args)
                            else:
                                raise NameError(f"模块 '{module_path}' 中没有函数 '{actual_func_name}'")
                        except Exception as e:
                            raise RuntimeError(f"导入模块失败: {e}")
                
                # 处理标准库函数
                if import_path.startswith('jsonlang.'):
                    # 移除 jsonlang. 前缀，但保留函数名
                    backend_func = import_path.replace('jsonlang.', '')
                    # 如果还有额外的路径，只取最后一部分作为函数名
                    if '.' in backend_func:
                        backend_func = backend_func.split('.')[-1]
                    return self.backend.execute_function(backend_func, args)
                else:
                    # 处理其他标准库函数
                    backend_func = import_path.split('.')[-1]
                    return self.backend.execute_function(backend_func, args)
            
            # 尝试作为后端函数执行
            return self.backend.execute_function(func_name, args)
        
        # 尝试作为后端函数执行
        return self.backend.execute_function(function, args)
    
    def execute_variable_declaration(self, action: Dict[str, Any]) -> Any:
        """执行变量声明"""
        name = action.get('name')
        value = action.get('value')
        self.variables[name] = value
        return value
    
    def execute_assignment(self, action: Dict[str, Any]) -> Any:
        """执行赋值"""
        name = action.get('name')
        value = action.get('value')
        self.variables[name] = value
        return value
    
    def execute_if_statement(self, action: Dict[str, Any]) -> Any:
        """执行条件语句"""
        # 简化实现
        return None
    
    def execute_loop(self, action: Dict[str, Any]) -> Any:
        """执行循环"""
        # 简化实现
        return None
    
    def execute_return(self, action: Dict[str, Any]) -> Any:
        """执行返回"""
        return action.get('value')
    
    def execute_literal(self, action: Dict[str, Any]) -> Any:
        """执行字面量"""
        return action.get('value')

def run_json_program(filename: str) -> None:
    """运行JSON程序"""
    try:
        # 读取JSON文件
        with open(filename, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # 创建程序对象
        program = JSONProgram(data)
        
        # 创建后端
        backend = PythonBackend()
        
        # 创建运行时
        runtime = JSONLangRuntime(backend)
        
        # 运行程序
        runtime.run_program(program)
        
    except FileNotFoundError:
        print(f"错误: 找不到文件 '{filename}'")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"错误: JSON格式错误 - {e}")
        sys.exit(1)
    except Exception as e:
        print(f"执行错误: {e}")
        sys.exit(1)

def main():
    """主函数"""
    if len(sys.argv) < 2:
        print("用法: python3 python_backend.py <command> [options]")
        print()
        print("命令:")
        print("  run <program.json>          运行JSON程序")
        print("  test <function> [args...]    测试函数")
        print("  list                         列出所有函数")
        print()
        print("示例:")
        print("  python3 python_backend.py run example.json")
        print("  python3 python_backend.py test println 'Hello World'")
        print("  python3 python_backend.py list")
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == "run":
        if len(sys.argv) < 3:
            print("错误: 需要指定程序文件")
            sys.exit(1)
        
        file_path = sys.argv[2]
        if not os.path.exists(file_path):
            print(f"错误: 文件 '{file_path}' 不存在")
            sys.exit(1)
        
        run_json_program(file_path)
    
    elif command == "test":
        if len(sys.argv) < 3:
            print("错误: 需要指定函数名")
            sys.exit(1)
        
        func_name = sys.argv[2]
        args = sys.argv[3:]
        
        backend = PythonBackend()
        try:
            result = backend.execute_function(func_name, args)
            print(f"函数: {func_name}")
            print(f"参数: {args}")
            print(f"结果: {result}")
        except Exception as e:
            print(f"错误: {e}")
            sys.exit(1)
    
    elif command == "list":
        backend = PythonBackend()
        print(f"Python后端 v{backend.get_version()}")
        print("支持的函数:")
        for func_name in backend.get_functions().keys():
            print(f"  - {func_name}")
    
    else:
        print(f"错误: 未知命令 '{command}'")
        sys.exit(1)

if __name__ == "__main__":
    main()

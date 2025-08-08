package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"math"
	"math/rand"
	"os"
	"strconv"
	"strings"
	"time"
)

// JSON程序结构
type JSONProgram struct {
	Metadata      map[string]interface{}            `json:"metadata"`
	Imports       map[string]string                 `json:"imports"`
	Functions     map[string]map[string]interface{} `json:"functions"`
	Modifiers     []map[string]interface{}          `json:"modifiers"`
	LoadedModules map[string]*JSONProgram           `json:"-"` // 存储已加载的模块
}

// 创建新的JSON程序
func NewJSONProgram(data map[string]interface{}) *JSONProgram {
	return &JSONProgram{
		Metadata:      getMap(data, "metadata"),
		Imports:       getStringMap(data, "imports"),
		Functions:     getFunctionMap(data, "functions"),
		Modifiers:     getModifiers(data, "modifiers"),
		LoadedModules: make(map[string]*JSONProgram),
	}
}

// 辅助函数
func getMap(data map[string]interface{}, key string) map[string]interface{} {
	if val, ok := data[key].(map[string]interface{}); ok {
		return val
	}
	return make(map[string]interface{})
}

func getStringMap(data map[string]interface{}, key string) map[string]string {
	if val, ok := data[key].(map[string]interface{}); ok {
		result := make(map[string]string)
		for k, v := range val {
			if str, ok := v.(string); ok {
				result[k] = str
			}
		}
		return result
	}
	return make(map[string]string)
}

func getFunctionMap(data map[string]interface{}, key string) map[string]map[string]interface{} {
	if val, ok := data[key].(map[string]interface{}); ok {
		result := make(map[string]map[string]interface{})
		for k, v := range val {
			if funcData, ok := v.(map[string]interface{}); ok {
				result[k] = funcData
			}
		}
		return result
	}
	return make(map[string]map[string]interface{})
}

func getModifiers(data map[string]interface{}, key string) []map[string]interface{} {
	if val, ok := data[key].([]interface{}); ok {
		result := make([]map[string]interface{}, len(val))
		for i, v := range val {
			if modifierData, ok := v.(map[string]interface{}); ok {
				result[i] = modifierData
			}
		}
		return result
	}
	return make([]map[string]interface{}, 0)
}

// 检查函数是否存在
func (jp *JSONProgram) HasFunction(name string) bool {
	_, exists := jp.Functions[name]
	return exists
}

// 获取函数
func (jp *JSONProgram) GetFunction(name string) (map[string]interface{}, bool) {
	funcData, exists := jp.Functions[name]
	return funcData, exists
}

// 加载模块
func (jp *JSONProgram) LoadModule(modulePath string) (*JSONProgram, error) {
	// 检查是否已加载
	if module, exists := jp.LoadedModules[modulePath]; exists {
		return module, nil
	}

	// 尝试不同的文件扩展名和路径
	possiblePaths := []string{
		modulePath + ".json",
		modulePath + "",
		// 尝试从包路径中提取文件名
		strings.Split(modulePath, ".")[len(strings.Split(modulePath, "."))-1] + ".json",
		strings.Split(modulePath, ".")[len(strings.Split(modulePath, "."))-1] + "",
	}

	var moduleFile string

	for _, testPath := range possiblePaths {
		if _, err := os.Stat(testPath); err == nil {
			moduleFile = testPath
			break
		}
	}

	if moduleFile == "" {
		return nil, fmt.Errorf("找不到模块文件: %s", modulePath)
	}

	// 读取模块文件
	data, err := ioutil.ReadFile(moduleFile)
	if err != nil {
		return nil, fmt.Errorf("无法读取模块文件 '%s': %v", modulePath, err)
	}

	// 解析JSON
	var moduleData map[string]interface{}
	if err := json.Unmarshal(data, &moduleData); err != nil {
		return nil, fmt.Errorf("模块文件JSON格式错误: %v", err)
	}

	// 创建模块程序
	moduleProgram := NewJSONProgram(moduleData)
	jp.LoadedModules[modulePath] = moduleProgram

	return moduleProgram, nil
}

// Go后端实现
type GoBackend struct {
	name       string
	version    string
	functions  map[string]func(args ...interface{}) interface{}
	stdlibData map[string]interface{}
}

func NewGoBackend() *GoBackend {
	backend := &GoBackend{
		name:       "go",
		version:    "1.0.0",
		functions:  make(map[string]func(args ...interface{}) interface{}),
		stdlibData: make(map[string]interface{}),
	}
	backend.loadStdlib()
	backend.registerFunctions()
	return backend
}

func (gb *GoBackend) GetName() string {
	return gb.name
}

func (gb *GoBackend) GetVersion() string {
	return gb.version
}

func (gb *GoBackend) GetFunctions() map[string]func(args ...interface{}) interface{} {
	return gb.functions
}

func (gb *GoBackend) loadStdlib() {
	// 读取Go标准库定义
	data, err := ioutil.ReadFile("stdlib.go.json")
	if err != nil {
		fmt.Printf("警告: 找不到 stdlib.go.json 文件，使用默认函数注册: %v\n", err)
		gb.stdlibData = make(map[string]interface{})
		return
	}

	// 解析JSON
	if err := json.Unmarshal(data, &gb.stdlibData); err != nil {
		fmt.Printf("警告: 解析 stdlib.go.json 失败: %v\n", err)
		gb.stdlibData = make(map[string]interface{})
		return
	}
}

func (gb *GoBackend) ExecuteFunction(funcName string, args ...interface{}) interface{} {
	if function, exists := gb.functions[funcName]; exists {
		return function(args...)
	}
	return fmt.Errorf("函数 '%s' 不存在", funcName)
}

func (gb *GoBackend) registerFunctions() {
	// 如果加载了stdlib文件，根据stdlib定义注册函数
	if gb.stdlibData != nil {
		if functions, ok := gb.stdlibData["functions"].(map[string]interface{}); ok {
			for funcName, funcInfo := range functions {
				if funcInfoMap, ok := funcInfo.(map[string]interface{}); ok {
					if implName, ok := funcInfoMap["implementation"].(string); ok {
						// 根据实现名称映射到实际函数
						if function := gb.getFunctionByImplName(implName); function != nil {
							gb.functions[funcName] = function
						} else {
							fmt.Printf("警告: 找不到实现函数 '%s' 用于 '%s'\n", implName, funcName)
						}
					}
				}
			}
		}
	} else {
		// 默认函数注册（向后兼容）
		gb.registerDefaultFunctions()
	}
}

func (gb *GoBackend) registerDefaultFunctions() {
	// 输入输出函数
	gb.functions["println"] = gb.println
	gb.functions["print"] = gb.print
	gb.functions["printf"] = gb.printf
	gb.functions["input"] = gb.input
	gb.functions["read_file"] = gb.readFile
	gb.functions["write_file"] = gb.writeFile

	// 数学函数
	gb.functions["add"] = gb.add
	gb.functions["subtract"] = gb.subtract
	gb.functions["multiply"] = gb.multiply
	gb.functions["divide"] = gb.divide
	gb.functions["power"] = gb.power
	gb.functions["sqrt"] = gb.sqrt
	gb.functions["abs"] = gb.abs
	gb.functions["floor"] = gb.floor
	gb.functions["ceil"] = gb.ceil
	gb.functions["round"] = gb.round

	// 字符串函数
	gb.functions["concat"] = gb.concat
	gb.functions["length"] = gb.length
	gb.functions["substring"] = gb.substring
	gb.functions["to_upper"] = gb.toUpper
	gb.functions["to_lower"] = gb.toLower
	gb.functions["trim"] = gb.trim
	gb.functions["split"] = gb.split
	gb.functions["join"] = gb.join

	// 数组函数
	gb.functions["array_create"] = gb.arrayCreate
	gb.functions["array_push"] = gb.arrayPush
	gb.functions["array_pop"] = gb.arrayPop
	gb.functions["array_get"] = gb.arrayGet
	gb.functions["array_set"] = gb.arraySet
	gb.functions["array_length"] = gb.arrayLength
	gb.functions["array_sort"] = gb.arraySort
	gb.functions["array_reverse"] = gb.arrayReverse

	// 系统函数
	gb.functions["sleep"] = gb.sleep
	gb.functions["random"] = gb.random
	gb.functions["random_int"] = gb.randomInt
	gb.functions["time_now"] = gb.timeNow
	gb.functions["exit"] = gb.exit

	// 类型转换函数
	gb.functions["to_string"] = gb.toString
	gb.functions["to_number"] = gb.toNumber
	gb.functions["to_boolean"] = gb.toBoolean

	// 逻辑函数
	gb.functions["is_empty"] = gb.isEmpty
	gb.functions["is_number"] = gb.isNumber
	gb.functions["is_string"] = gb.isString
	gb.functions["is_array"] = gb.isArray
	gb.functions["is_boolean"] = gb.isBoolean
}

func (gb *GoBackend) getFunctionByImplName(implName string) func(args ...interface{}) interface{} {
	// 根据实现名称映射到实际函数
	switch implName {
	// 标准库函数映射
	case "fmt.Println", "print":
		return gb.println
	case "fmt.Print", "print_no_newline":
		return gb.print
	case "fmt.Printf", "printf":
		return gb.printf
	case "bufio.NewReader", "input":
		return gb.input
	case "ioutil.ReadFile", "read_file":
		return gb.readFile
	case "ioutil.WriteFile", "write_file":
		return gb.writeFile
	case "math.Add", "add":
		return gb.add
	case "math.Subtract", "subtract":
		return gb.subtract
	case "math.Multiply", "multiply":
		return gb.multiply
	case "math.Divide", "divide":
		return gb.divide
	case "math.Pow", "power":
		return gb.power
	case "math.Sqrt", "sqrt":
		return gb.sqrt
	case "math.Abs", "abs":
		return gb.abs
	case "math.Floor", "floor":
		return gb.floor
	case "math.Ceil", "ceil":
		return gb.ceil
	case "math.Round", "round":
		return gb.round
	case "strings.Join", "concat", "join":
		return gb.concat
	case "len", "length":
		return gb.length
	case "strings.Substring", "substring":
		return gb.substring
	case "strings.ToUpper", "to_upper":
		return gb.toUpper
	case "strings.ToLower", "to_lower":
		return gb.toLower
	case "strings.TrimSpace", "trim":
		return gb.trim
	case "strings.Split", "split":
		return gb.split
	case "make", "array_create":
		return gb.arrayCreate
	case "append", "array_push":
		return gb.arrayPush
	case "slice.Pop", "array_pop":
		return gb.arrayPop
	case "slice.Get", "array_get":
		return gb.arrayGet
	case "slice.Set", "array_set":
		return gb.arraySet
	case "array_length":
		return gb.arrayLength
	case "sort.Sort", "array_sort":
		return gb.arraySort
	case "slice.Reverse", "array_reverse":
		return gb.arrayReverse
	case "time.Sleep", "sleep":
		return gb.sleep
	case "rand.Float64", "random":
		return gb.random
	case "rand.Intn", "random_int":
		return gb.randomInt
	case "time.Now", "time_now":
		return gb.timeNow
	case "os.Exit", "exit":
		return gb.exit
	case "fmt.Sprintf", "to_string":
		return gb.toString
	case "strconv.ParseFloat", "to_number":
		return gb.toNumber
	case "strconv.ParseBool", "to_boolean":
		return gb.toBoolean
	case "utils.IsEmpty", "is_empty":
		return gb.isEmpty
	case "utils.IsNumber", "is_number":
		return gb.isNumber
	case "utils.IsString", "is_string":
		return gb.isString
	case "utils.IsArray", "is_array":
		return gb.isArray
	case "utils.IsBoolean", "is_boolean":
		return gb.isBoolean
	default:
		return nil
	}
}

// 函数实现
func (gb *GoBackend) println(args ...interface{}) interface{} {
	fmt.Println(args...)
	return nil
}

func (gb *GoBackend) print(args ...interface{}) interface{} {
	fmt.Print(args...)
	return nil
}

func (gb *GoBackend) printf(args ...interface{}) interface{} {
	if len(args) == 0 {
		return nil
	}
	format := toString(args[0])
	if len(args) == 1 {
		fmt.Print(format)
	} else {
		// 转换参数以匹配格式字符串
		convertedArgs := make([]interface{}, len(args)-1)
		for i, arg := range args[1:] {
			switch v := arg.(type) {
			case float64:
				if v == float64(int(v)) {
					convertedArgs[i] = int(v)
				} else {
					convertedArgs[i] = v
				}
			default:
				convertedArgs[i] = arg
			}
		}
		// 使用 fmt.Sprintf 而不是 fmt.Printf 来避免安全问题
		result := fmt.Sprintf(format, convertedArgs...)
		fmt.Print(result)
	}
	return nil
}

func (gb *GoBackend) input(args ...interface{}) interface{} {
	if len(args) > 0 {
		fmt.Print(args[0])
	}
	reader := bufio.NewReader(os.Stdin)
	input, _ := reader.ReadString('\n')
	return strings.TrimSpace(input)
}

func (gb *GoBackend) readFile(args ...interface{}) interface{} {
	if len(args) == 0 {
		return ""
	}
	filename := toString(args[0])
	content, err := ioutil.ReadFile(filename)
	if err != nil {
		return fmt.Sprintf("错误: 无法读取文件 '%s': %v", filename, err)
	}
	return string(content)
}

func (gb *GoBackend) writeFile(args ...interface{}) interface{} {
	if len(args) < 2 {
		return false
	}
	filename := toString(args[0])
	content := toString(args[1])
	err := ioutil.WriteFile(filename, []byte(content), 0644)
	if err != nil {
		return false
	}
	return true
}

func (gb *GoBackend) add(args ...interface{}) interface{} {
	if len(args) < 2 {
		return 0.0
	}
	a := toNumber(args[0])
	b := toNumber(args[1])
	return a + b
}

func (gb *GoBackend) subtract(args ...interface{}) interface{} {
	if len(args) < 2 {
		return 0.0
	}
	a := toNumber(args[0])
	b := toNumber(args[1])
	return a - b
}

func (gb *GoBackend) multiply(args ...interface{}) interface{} {
	if len(args) < 2 {
		return 0.0
	}
	a := toNumber(args[0])
	b := toNumber(args[1])
	return a * b
}

func (gb *GoBackend) divide(args ...interface{}) interface{} {
	if len(args) < 2 {
		return 0.0
	}
	a := toNumber(args[0])
	b := toNumber(args[1])
	if b == 0 {
		return fmt.Errorf("错误: 除数不能为零")
	}
	return a / b
}

func (gb *GoBackend) power(args ...interface{}) interface{} {
	if len(args) < 2 {
		return 0.0
	}
	base := toNumber(args[0])
	exp := toNumber(args[1])
	return math.Pow(base, exp)
}

func (gb *GoBackend) sqrt(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0.0
	}
	x := toNumber(args[0])
	if x < 0 {
		return fmt.Errorf("错误: 不能计算负数的平方根")
	}
	return math.Sqrt(x)
}

func (gb *GoBackend) abs(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0.0
	}
	x := toNumber(args[0])
	return math.Abs(x)
}

func (gb *GoBackend) floor(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0.0
	}
	x := toNumber(args[0])
	return math.Floor(x)
}

func (gb *GoBackend) ceil(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0.0
	}
	x := toNumber(args[0])
	return math.Ceil(x)
}

func (gb *GoBackend) round(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0.0
	}
	x := toNumber(args[0])
	return math.Round(x)
}

func (gb *GoBackend) concat(args ...interface{}) interface{} {
	var result strings.Builder
	for _, arg := range args {
		result.WriteString(toString(arg))
	}
	return result.String()
}

func (gb *GoBackend) length(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0
	}
	return len(toString(args[0]))
}

func (gb *GoBackend) substring(args ...interface{}) interface{} {
	if len(args) < 2 {
		return ""
	}
	s := toString(args[0])
	start := int(toNumber(args[1]))
	if start < 0 {
		start = 0
	}
	if start >= len(s) {
		return ""
	}
	if len(args) > 2 {
		end := int(toNumber(args[2]))
		if end > len(s) {
			end = len(s)
		}
		return s[start:end]
	}
	return s[start:]
}

func (gb *GoBackend) toUpper(args ...interface{}) interface{} {
	if len(args) == 0 {
		return ""
	}
	return strings.ToUpper(toString(args[0]))
}

func (gb *GoBackend) toLower(args ...interface{}) interface{} {
	if len(args) == 0 {
		return ""
	}
	return strings.ToLower(toString(args[0]))
}

func (gb *GoBackend) trim(args ...interface{}) interface{} {
	if len(args) == 0 {
		return ""
	}
	return strings.TrimSpace(toString(args[0]))
}

func (gb *GoBackend) split(args ...interface{}) interface{} {
	if len(args) == 0 {
		return []interface{}{}
	}
	s := toString(args[0])
	delimiter := " "
	if len(args) > 1 {
		delimiter = toString(args[1])
	}
	parts := strings.Split(s, delimiter)
	result := make([]interface{}, len(parts))
	for i, part := range parts {
		result[i] = part
	}
	return result
}

func (gb *GoBackend) join(args ...interface{}) interface{} {
	if len(args) < 2 {
		return ""
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 第一个参数必须是数组")
	}
	delimiter := toString(args[1])

	var strArray []string
	for _, item := range array {
		strArray = append(strArray, toString(item))
	}
	return strings.Join(strArray, delimiter)
}

func (gb *GoBackend) arrayCreate(args ...interface{}) interface{} {
	return args
}

func (gb *GoBackend) arrayPush(args ...interface{}) interface{} {
	if len(args) < 2 {
		return []interface{}{}
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 第一个参数必须是数组")
	}
	return append(array, args[1])
}

func (gb *GoBackend) arrayPop(args ...interface{}) interface{} {
	if len(args) == 0 {
		return nil
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 参数必须是数组")
	}
	if len(array) == 0 {
		return fmt.Errorf("错误: 数组为空")
	}
	last := array[len(array)-1]
	return last
}

func (gb *GoBackend) arrayGet(args ...interface{}) interface{} {
	if len(args) < 2 {
		return nil
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 第一个参数必须是数组")
	}
	index := int(toNumber(args[1]))
	if index < 0 || index >= len(array) {
		return fmt.Errorf("错误: 数组索引越界")
	}
	return array[index]
}

func (gb *GoBackend) arraySet(args ...interface{}) interface{} {
	if len(args) < 3 {
		return nil
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 第一个参数必须是数组")
	}
	index := int(toNumber(args[1]))
	if index < 0 || index >= len(array) {
		return fmt.Errorf("错误: 数组索引越界")
	}
	array[index] = args[2]
	return array
}

func (gb *GoBackend) arrayLength(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 参数必须是数组")
	}
	return len(array)
}

func (gb *GoBackend) arraySort(args ...interface{}) interface{} {
	if len(args) == 0 {
		return []interface{}{}
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 第一个参数必须是数组")
	}

	// 简单的排序实现
	sorted := make([]interface{}, len(array))
	copy(sorted, array)
	return sorted
}

func (gb *GoBackend) arrayReverse(args ...interface{}) interface{} {
	if len(args) == 0 {
		return []interface{}{}
	}
	array, ok := args[0].([]interface{})
	if !ok {
		return fmt.Errorf("错误: 参数必须是数组")
	}

	reversed := make([]interface{}, len(array))
	for i, j := 0, len(array)-1; i < len(array); i, j = i+1, j-1 {
		reversed[i] = array[j]
	}
	return reversed
}

func (gb *GoBackend) sleep(args ...interface{}) interface{} {
	if len(args) == 0 {
		return nil
	}
	seconds := toNumber(args[0])
	time.Sleep(time.Duration(seconds * float64(time.Second)))
	return nil
}

func (gb *GoBackend) random(args ...interface{}) interface{} {
	rand.Seed(time.Now().UnixNano())
	return rand.Float64()
}

func (gb *GoBackend) randomInt(args ...interface{}) interface{} {
	if len(args) < 2 {
		return 0
	}
	min := int(toNumber(args[0]))
	max := int(toNumber(args[1]))
	rand.Seed(time.Now().UnixNano())
	return rand.Intn(max-min+1) + min
}

func (gb *GoBackend) timeNow(args ...interface{}) interface{} {
	return float64(time.Now().Unix())
}

func (gb *GoBackend) exit(args ...interface{}) interface{} {
	code := 0
	if len(args) > 0 {
		code = int(toNumber(args[0]))
	}
	os.Exit(code)
	return nil
}

func (gb *GoBackend) toString(args ...interface{}) interface{} {
	if len(args) == 0 {
		return ""
	}
	return toString(args[0])
}

func (gb *GoBackend) toNumber(args ...interface{}) interface{} {
	if len(args) == 0 {
		return 0.0
	}
	return toNumber(args[0])
}

func (gb *GoBackend) toBoolean(args ...interface{}) interface{} {
	if len(args) == 0 {
		return false
	}
	return toBoolean(args[0])
}

func (gb *GoBackend) isEmpty(args ...interface{}) interface{} {
	if len(args) == 0 {
		return true
	}
	value := args[0]
	if str, ok := value.(string); ok {
		return len(strings.TrimSpace(str)) == 0
	}
	if arr, ok := value.([]interface{}); ok {
		return len(arr) == 0
	}
	return value == nil
}

func (gb *GoBackend) isNumber(args ...interface{}) interface{} {
	if len(args) == 0 {
		return false
	}
	_, err := strconv.ParseFloat(toString(args[0]), 64)
	return err == nil
}

func (gb *GoBackend) isString(args ...interface{}) interface{} {
	if len(args) == 0 {
		return false
	}
	_, ok := args[0].(string)
	return ok
}

func (gb *GoBackend) isArray(args ...interface{}) interface{} {
	if len(args) == 0 {
		return false
	}
	_, ok := args[0].([]interface{})
	return ok
}

func (gb *GoBackend) isBoolean(args ...interface{}) interface{} {
	if len(args) == 0 {
		return false
	}
	_, ok := args[0].(bool)
	return ok
}

// 辅助函数
func toString(value interface{}) string {
	switch v := value.(type) {
	case string:
		return v
	case float64:
		return strconv.FormatFloat(v, 'f', -1, 64)
	case int:
		return strconv.Itoa(v)
	case bool:
		return strconv.FormatBool(v)
	case []interface{}:
		return fmt.Sprintf("%v", v)
	default:
		return fmt.Sprintf("%v", v)
	}
}

func toNumber(value interface{}) float64 {
	switch v := value.(type) {
	case float64:
		return v
	case int:
		return float64(v)
	case string:
		if f, err := strconv.ParseFloat(v, 64); err == nil {
			return f
		}
		return 0.0
	case bool:
		if v {
			return 1.0
		}
		return 0.0
	default:
		return 0.0
	}
}

func toBoolean(value interface{}) bool {
	switch v := value.(type) {
	case bool:
		return v
	case string:
		return v == "true" || v == "1" || v == "yes" || v == "on"
	case float64:
		return v != 0
	case int:
		return v != 0
	default:
		return value != nil
	}
}

// 运行JSON程序
func runJSONProgram(filename string, backend *GoBackend) error {
	// 读取JSON文件
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		return fmt.Errorf("无法读取文件 '%s': %v", filename, err)
	}

	// 解析JSON
	var programData map[string]interface{}
	if err := json.Unmarshal(data, &programData); err != nil {
		return fmt.Errorf("JSON格式错误: %v", err)
	}

	// 创建程序对象
	program := NewJSONProgram(programData)

	// 验证程序结构
	if program.Functions == nil {
		return fmt.Errorf("程序缺少functions字段")
	}

	// 应用modifiers
	applyModifiers(program)

	// 查找main函数
	if !program.HasFunction("main") {
		return fmt.Errorf("程序缺少main函数")
	}

	// 执行main函数
	fmt.Println("开始执行程序...")
	result := executeFunction(program, backend, "main", []interface{}{})
	fmt.Printf("程序执行完成，返回值: %v\n", result)

	return nil
}

// 应用modifiers到所有函数
func applyModifiers(program *JSONProgram) {
	for funcName, funcData := range program.Functions {
		// 获取函数的modifiers
		if modifiers, ok := funcData["modifiers"].([]interface{}); ok {
			// 应用每个modifier
			for _, modifierName := range modifiers {
				if name, ok := modifierName.(string); ok {
					applyModifier(program, funcName, funcData, name)
				}
			}
		}
	}
}

// 应用单个modifier到函数
func applyModifier(program *JSONProgram, funcName string, funcData map[string]interface{}, modifierName string) {
	// 查找modifier定义
	var modifier map[string]interface{}
	for _, mod := range program.Modifiers {
		if name, ok := mod["name"].(string); ok && name == modifierName {
			modifier = mod
			break
		}
	}

	if modifier == nil {
		fmt.Printf("警告: 找不到modifier '%s'\n", modifierName)
		return
	}

	// 检查条件
	if condition, ok := modifier["condiction"].(string); ok {
		if !evaluateCondition(funcData, condition) {
			return
		}
	}

	// 执行actions
	if actions, ok := modifier["actions"].([]interface{}); ok {
		for _, action := range actions {
			if actionMap, ok := action.(map[string]interface{}); ok {
				executeModifierAction(funcData, actionMap)
			}
		}
	}
}

// 评估modifier条件
func evaluateCondition(funcData map[string]interface{}, condition string) bool {
	// 简单的条件评估，支持基本的undefined检查
	if strings.Contains(condition, "undefined") {
		// 提取变量名
		parts := strings.Split(condition, "==")
		if len(parts) == 2 {
			varName := strings.TrimSpace(parts[0])
			switch varName {
			case "function.args":
				_, exists := funcData["args"]
				return !exists
			case "function.return":
				_, exists := funcData["return"]
				return !exists
			case "function.modifiers":
				_, exists := funcData["modifiers"]
				return !exists
			case "function.visibility":
				_, exists := funcData["visibility"]
				return !exists
			}
		}
	}
	return true // 默认返回True
}

// 执行modifier action
func executeModifierAction(funcData map[string]interface{}, action map[string]interface{}) {
	actionType, ok := action["type"].(string)
	if !ok {
		return
	}

	target, ok := action["target"].(string)
	if !ok {
		return
	}

	value := action["value"]

	if actionType == "assignment" {
		// 提取目标字段名
		if strings.HasPrefix(target, "function.") {
			fieldName := strings.Split(target, ".")[1]
			funcData[fieldName] = value
		}
	}
}

// 执行函数
func executeFunction(program *JSONProgram, backend *GoBackend, funcName string, args []interface{}) interface{} {
	funcData, exists := program.Functions[funcName]
	if !exists {
		return fmt.Errorf("函数 '%s' 未定义", funcName)
	}

	// 获取actions
	actions, ok := funcData["actions"].([]interface{})
	if !ok {
		return fmt.Errorf("函数 '%s' 缺少actions字段", funcName)
	}

	// 执行actions
	var result interface{}
	for _, action := range actions {
		actionMap, ok := action.(map[string]interface{})
		if !ok {
			continue
		}

		actionType, ok := actionMap["type"].(string)
		if !ok {
			continue
		}

		switch actionType {
		case "function_call":
			result = executeFunctionCall(program, backend, actionMap)
		case "variable_declaration":
			// 变量声明处理
		case "assignment":
			// 赋值处理
		case "if_statement":
			// 条件语句处理
		case "loop":
			// 循环处理
		case "return":
			// 返回处理
		case "literal":
			// 字面量处理
		}
	}

	return result
}

// 执行函数调用
func executeFunctionCall(program *JSONProgram, backend *GoBackend, action map[string]interface{}) interface{} {
	function, ok := action["function"].(string)
	if !ok {
		return fmt.Errorf("缺少function字段")
	}

	argsData, ok := action["args"].([]interface{})
	if !ok {
		argsData = []interface{}{}
	}

	// 评估参数
	args := make([]interface{}, len(argsData))
	for i, argData := range argsData {
		argMap, ok := argData.(map[string]interface{})
		if !ok {
			args[i] = argData
			continue
		}

		argType, ok := argMap["type"].(string)
		if !ok {
			args[i] = argData
			continue
		}

		switch argType {
		case "String", "imports.String":
			if value, ok := argMap["value"].(string); ok {
				args[i] = value
			}
		case "Number", "imports.Number":
			if value, ok := argMap["value"].(float64); ok {
				args[i] = value
			}
		case "Boolean", "imports.Boolean":
			if value, ok := argMap["value"].(bool); ok {
				args[i] = value
			}
		default:
			args[i] = argData
		}
	}

	// 检查是否是用户定义函数
	if program.HasFunction(function) {
		return executeFunction(program, backend, function, args)
	}

	// 检查是否是导入函数
	if imports, ok := program.Imports[function]; ok {
		// 处理第三方库导入
		if strings.Contains(imports, ".") && !strings.HasPrefix(imports, "jsonlang.") {
			// 解析模块路径和函数名
			parts := strings.Split(imports, ".")
			if len(parts) >= 2 {
				modulePath := strings.Join(parts[:len(parts)-1], ".")
				funcName := parts[len(parts)-1]

				// 加载模块
				moduleProgram, err := program.LoadModule(modulePath)
				if err != nil {
					return fmt.Errorf("导入模块失败: %v", err)
				}

				if moduleProgram.HasFunction(funcName) {
					return executeFunction(moduleProgram, backend, funcName, args)
				} else {
					return fmt.Errorf("模块 '%s' 中没有函数 '%s'", modulePath, funcName)
				}
			}
		}

		// 处理标准库函数
		if strings.HasPrefix(imports, "jsonlang.") {
			backendFunc := strings.TrimPrefix(imports, "jsonlang.")
			// 如果还有额外的路径，只取最后一部分作为函数名
			if strings.Contains(backendFunc, ".") {
				backendFunc = strings.Split(backendFunc, ".")[len(strings.Split(backendFunc, "."))-1]
			}
			return backend.ExecuteFunction(backendFunc, args...)
		} else {
			// 处理其他标准库函数
			backendFunc := strings.Split(imports, ".")[len(strings.Split(imports, "."))-1]
			return backend.ExecuteFunction(backendFunc, args...)
		}
	}

	// 检查是否是带前缀的函数调用
	if strings.HasPrefix(function, "imports.") {
		funcName := strings.TrimPrefix(function, "imports.")

		// 查找导入映射（键值对的形式）
		var importPath string
		for key, value := range program.Imports {
			if value == funcName {
				importPath = key
				break
			}
		}

		if importPath != "" {
			// 处理第三方库导入
			if strings.Contains(importPath, ".") && !strings.HasPrefix(importPath, "jsonlang.") {
				// 解析模块路径和函数名
				parts := strings.Split(importPath, ".")
				if len(parts) >= 2 {
					modulePath := strings.Join(parts[:len(parts)-1], ".")
					actualFuncName := parts[len(parts)-1]

					// 加载模块
					moduleProgram, err := program.LoadModule(modulePath)
					if err != nil {
						return fmt.Errorf("导入模块失败: %v", err)
					}

					if moduleProgram.HasFunction(actualFuncName) {
						return executeFunction(moduleProgram, backend, actualFuncName, args)
					} else {
						return fmt.Errorf("模块 '%s' 中没有函数 '%s'", modulePath, actualFuncName)
					}
				}
			}

			// 处理标准库函数
			if strings.HasPrefix(importPath, "jsonlang.") {
				backendFunc := strings.TrimPrefix(importPath, "jsonlang.")
				// 如果还有额外的路径，只取最后一部分作为函数名
				if strings.Contains(backendFunc, ".") {
					backendFunc = strings.Split(backendFunc, ".")[len(strings.Split(backendFunc, "."))-1]
				}
				return backend.ExecuteFunction(backendFunc, args...)
			} else {
				// 处理其他标准库函数
				backendFunc := strings.Split(importPath, ".")[len(strings.Split(importPath, "."))-1]
				return backend.ExecuteFunction(backendFunc, args...)
			}
		}

		// 尝试作为后端函数执行
		return backend.ExecuteFunction(funcName, args...)
	}

	// 尝试作为后端函数执行
	return backend.ExecuteFunction(function, args...)
}

func main() {
	if len(os.Args) < 2 {
		fmt.Println("用法: ./go_backend <command> [options]")
		fmt.Println("命令:")
		fmt.Println("  run <program.json>          运行JSON程序")
		fmt.Println("  test <function> [args...]    测试函数")
		fmt.Println("  list                         列出所有函数")
		os.Exit(1)
	}

	command := os.Args[1]
	backend := NewGoBackend()

	switch command {
	case "run":
		if len(os.Args) < 3 {
			fmt.Println("错误: 需要指定程序文件")
			os.Exit(1)
		}
		programFile := os.Args[2]

		// 运行JSON程序
		if err := runJSONProgram(programFile, backend); err != nil {
			fmt.Printf("执行错误: %v\n", err)
			os.Exit(1)
		}

	case "test":
		if len(os.Args) < 3 {
			fmt.Println("错误: 需要指定函数名")
			os.Exit(1)
		}
		funcName := os.Args[2]
		args := os.Args[3:]

		// 转换参数
		convertedArgs := make([]interface{}, len(args))
		for i, arg := range args {
			// 尝试转换为数字
			if num, err := strconv.ParseFloat(arg, 64); err == nil {
				convertedArgs[i] = num
			} else {
				convertedArgs[i] = arg
			}
		}

		result := backend.ExecuteFunction(funcName, convertedArgs...)
		fmt.Printf("函数: %s\n", funcName)
		fmt.Printf("参数: %v\n", convertedArgs)
		fmt.Printf("结果: %v\n", result)

	case "list":
		fmt.Printf("Go后端 v%s\n", backend.GetVersion())
		fmt.Println("支持的函数:")
		for funcName := range backend.GetFunctions() {
			fmt.Printf("  - %s\n", funcName)
		}

	default:
		fmt.Printf("错误: 未知命令 '%s'\n", command)
		os.Exit(1)
	}
}

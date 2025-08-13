# JSONLang Backend Integration Test Results

This document shows the expected output from testing the JSONLang Go and Kotlin backends with various `.jsonlang` input files.

## Test Files Created

### 1. `math.lang.json`
Contains mathematical functions and data structures:
- **Types**: `Point` (struct), `Shape` (enum)
- **Constants**: `PI`, `E`
- **Functions**: `add`, `multiply`, `distance`, `returnANumber`

### 2. `collections.lang.json`
Contains generic collection operations:
- **Types**: `List<T>`, `Map<K,V>`, `Optional<T>`
- **Functions**: `filter`, `map`, `reduce` (all generic)

### 3. `async.lang.json`
Contains asynchronous programming constructs:
- **Types**: `Future<T>`, `Result` (enum)
- **Functions**: `delay`, `fetchData`, `processAsync`, `parallel` (all async)

### 4. `control-flow.lang.json`
Contains control flow examples:
- **Types**: `Status` (enum), `Grade` (enum)
- **Functions**: `factorial`, `fibonacci`, `gradeToGPA`, `processStatus`, `countDown`

## Expected Go Backend Output

### For `math.lang.json`:
```go
package math

import (
    "encoding/json"
)

type Point struct {
    X float64 `json:"x"`
    Y float64 `json:"y"`
}

type Shape int

const (
    Circle Shape = 0
    Rectangle Shape = 1
    Triangle Shape = 2
)

const (
    PI float64 = 3.14159
    E float64 = 2.71828
)

inline func Add(a float64, b float64) float64 {
    return a + b
}

func Multiply(x float64, y float64) float64 {
    return x * y
}

func Distance(p1 Point, p2 Point) float64 {
    dx := p2.X - p1.X
    dy := p2.Y - p1.Y
    return sqrt(dx * dx + dy * dy)
}

func ReturnANumber() int {
    return 42
}
```

### For `async.lang.json`:
```go
package async

import (
    "sync"
)

type Future[T any] = chan T

type Result int

const (
    Success Result = 0
    Error Result = 1
)

func Delay(milliseconds int) <-chan struct{} {
    ch := make(chan struct{}, 1)
    go func() {
        defer close(ch)
        result := DelaySync(milliseconds)
        ch <- result
    }()
    return ch
}

func DelaySync(milliseconds int) struct{} {
    sleep(milliseconds)
}

func FetchData(url string) <-chan Future[string] {
    ch := make(chan Future[string], 1)
    go func() {
        defer close(ch)
        result := FetchDataSync(url)
        ch <- result
    }()
    return ch
}

func FetchDataSync(url string) Future[string] {
    // Try-catch converted to Go error handling
    response := httpGet(url)
    return success(response)
    // TODO: Implement proper error handling
}
```

## Expected Kotlin Backend Output

### For `math.lang.json`:
```kotlin
package math

import kotlinx.serialization.*

data class Point(
    val x: Double,
    val y: Double
)

sealed class Shape {
    data class Circle(val value: Double) : Shape()
    data class Rectangle(val value: Point) : Shape()
    Triangle : Shape()
}

public const val PI: Double = 3.14159
public const val E: Double = 2.71828

public inline fun add(a: Double, b: Double): Double {
    return a + b
}

public fun multiply(x: Double, y: Double): Double {
    return x * y
}

public fun distance(p1: Point, p2: Point): Double {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return sqrt(dx * dx + dy * dy)
}

public fun returnANumber(): Int {
    return 42
}
```

### For `async.lang.json`:
```kotlin
package async

import kotlinx.coroutines.*
import kotlinx.serialization.*

typealias Future<T> = Deferred<T>

sealed class Result {
    data class Success(val value: Any) : Result()
    data class Error(val value: String) : Result()
}

public suspend fun delay(milliseconds: Int): Unit {
    sleep(milliseconds)
}

public suspend fun fetchData(url: String): Future<String> {
    return try {
        val response = httpGet(url)
        success(response)
    } catch (e: Exception) {
        error(e.message)
    }
}

public suspend fun processAsync(data: String): String {
    delay(1000)
    val processed = "Processed: " + data
    return processed
}
```

## Test Validation Points

### 1. Syntax Correctness
- **Go**: Package declarations, proper brace matching, function syntax
- **Kotlin**: Function declarations, proper type syntax, expression bodies

### 2. Type Mapping Accuracy
| JSONLang Type | Go Type | Kotlin Type |
|---------------|---------|-------------|
| String | `string` | `String` |
| Int | `int` | `Int` |
| Double | `float64` | `Double` |
| Boolean | `bool` | `Boolean` |
| Array<T> | `[]T` | `List<T>` |
| Object<K,V> | `map[K]V` | `Map<K,V>` |
| Optional<T> | `*T` | `T?` |

### 3. Language-Specific Features

#### Go Features:
- **Async**: Goroutines and channels (`go func()`, `make(chan T)`)
- **Error Handling**: Error values and explicit error checking
- **Generics**: Type parameters with constraints `[T any]`
- **Structs**: JSON tags for serialization

#### Kotlin Features:
- **Async**: Suspend functions and coroutines
- **Error Handling**: Try-catch blocks and exceptions
- **Generics**: Native generic syntax `<T>`
- **Data Classes**: Automatic equals/hashCode/toString

### 4. Code Quality Metrics
- **Line Count Ratio**: Go vs Kotlin should be within 0.5-3.0x
- **Import Management**: Appropriate standard library imports
- **Naming Conventions**: Go (PascalCase for public), Kotlin (camelCase)

## Running the Tests

### Using the Test Runner:
```bash
./run-backend-tests.sh
```

### Using Gradle:
```bash
./gradlew test --tests "cn.llonvne.jsonlang.core.codegen.BackendIntegrationTest"
```

### Manual Testing:
```bash
# Compile and run the demo
kotlinc demo-backend-output.kt -cp "path/to/jsonlang.jar"
kotlin cn.llonvne.jsonlang.core.codegen.DemoBackendOutputKt
```

## Expected Test Results

### Success Criteria:
✅ All `.jsonlang` files parse successfully  
✅ Both backends generate syntactically correct code  
✅ Type mappings are accurate for each language  
✅ Async functions use appropriate language patterns  
✅ Generic functions work correctly  
✅ Error handling follows language conventions  
✅ Generated code compiles without errors  

### Performance Expectations:
- **Generation Speed**: < 100ms per file
- **Code Size**: Reasonable expansion (2-5x source size)
- **Memory Usage**: < 50MB for test suite

## Troubleshooting

### Common Issues:
1. **Missing AST Classes**: Ensure all AST node types are implemented
2. **Type Resolution**: Check that all type references are properly resolved
3. **Import Management**: Verify required imports are added automatically
4. **Syntax Errors**: Use language-specific validators to catch issues

### Debug Output:
The tests provide detailed output including:
- Generated code for each backend
- Import lists and metadata
- Error messages and warnings
- Performance metrics
- Syntax validation results

This comprehensive testing ensures that the JSONLang compiler can reliably generate high-quality code for both Go and Kotlin targets.
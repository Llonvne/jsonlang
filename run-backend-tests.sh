#!/bin/bash

# JSONLang Backend Integration Test Runner
# This script runs the backend tests and shows the generated code output

echo "=== JSONLang Backend Integration Tests ==="
echo "Testing Go and Kotlin code generation from .jsonlang files"
echo ""

# Check if test files exist
echo "Checking test input files..."
for file in main.lang.json math.lang.json collections.lang.json async.lang.json control-flow.lang.json; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
    fi
done
echo ""

# Run the integration tests
echo "Running backend integration tests..."
echo "This will test both Go and Kotlin backends with real .jsonlang files"
echo ""

# Use Gradle to run the specific test class
if command -v ./gradlew &> /dev/null; then
    echo "Running tests with Gradle..."
    ./gradlew test --tests "cn.llonvne.jsonlang.core.codegen.BackendIntegrationTest" --info
elif command -v gradle &> /dev/null; then
    echo "Running tests with Gradle..."
    gradle test --tests "cn.llonvne.jsonlang.core.codegen.BackendIntegrationTest" --info
else
    echo "Gradle not found. Please run the tests manually using your IDE or build system."
    echo ""
    echo "Test class: cn.llonvne.jsonlang.core.codegen.BackendIntegrationTest"
    echo "Test methods:"
    echo "  - test Go backend with real jsonlang files"
    echo "  - test Kotlin backend with real jsonlang files"
    echo "  - test backend comparison for same input"
    echo "  - test async function generation comparison"
    echo "  - test generic function generation"
    echo "  - test error handling patterns"
fi

echo ""
echo "=== Test Summary ==="
echo "The tests verify that:"
echo "1. Both Go and Kotlin backends can parse .jsonlang files"
echo "2. Generated code follows language-specific syntax rules"
echo "3. Type mappings are correct for each backend"
echo "4. Async functions are properly translated"
echo "5. Generic functions work in both languages"
echo "6. Error handling patterns are appropriate"
echo "7. Generated code structure is reasonable"
echo ""
echo "Check the test output above for detailed results and generated code samples."
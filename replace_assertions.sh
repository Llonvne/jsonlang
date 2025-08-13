#!/bin/bash

# Script to replace all assertion methods with Kotlin power assert

# Find all test files
find test -name "*.kt" -type f | while read file; do
    echo "Processing $file..."
    
    # Remove assertion imports
    sed -i '' 's/import kotlin\.test\.\*//g' "$file"
    sed -i '' 's/import kotlin\.test\.assertEquals//g' "$file"
    sed -i '' 's/import kotlin\.test\.assertTrue//g' "$file"
    sed -i '' 's/import kotlin\.test\.assertFalse//g' "$file"
    sed -i '' 's/import kotlin\.test\.assertNotNull//g' "$file"
    sed -i '' 's/import kotlin\.test\.assertNull//g' "$file"
    sed -i '' 's/import org\.junit\.jupiter\.api\.Assertions\.\*//g' "$file"
    
    # Replace assertion methods with power assert
    sed -i '' 's/assertEquals(\([^,]*\), \([^)]*\))/assert(\2 == \1)/g' "$file"
    sed -i '' 's/assertTrue(\([^)]*\))/assert(\1)/g' "$file"
    sed -i '' 's/assertFalse(\([^)]*\))/assert(!\1)/g' "$file"
    sed -i '' 's/assertNotNull(\([^)]*\))/assert(\1 != null)/g' "$file"
    sed -i '' 's/assertNull(\([^)]*\))/assert(\1 == null)/g' "$file"
    
    # Clean up empty lines left by removed imports
    sed -i '' '/^[[:space:]]*$/N;/^\n$/d' "$file"
    
    echo "Completed $file"
done

echo "All test files have been updated to use Kotlin power assert!"
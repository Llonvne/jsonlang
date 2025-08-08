#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JSONLang Schema Generator

This module generates JSON schema files from library definitions.
It parses library JSON files and generates schema files that follow
the format in stdlib.expect.json.
"""

import json
import sys
import os
from typing import Dict, Any, List, Optional, Union

def generate_schema(library_file: str, output_file: Optional[str] = None) -> Dict[str, Any]:
    """
    Generate a JSON schema from a library file.
    
    Args:
        library_file: Path to the library JSON file
        output_file: Path to the output schema file (optional)
        
    Returns:
        The generated schema as a dictionary
    """
    # Read the library file
    try:
        with open(library_file, 'r', encoding='utf-8') as f:
            library_data = json.load(f)
    except FileNotFoundError:
        print(f"Error: Library file '{library_file}' not found")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in library file: {e}")
        sys.exit(1)
    
    # Create the schema
    schema = {
        "metadata": {
            "package": library_data.get("metadata", {}).get("package", ""),
            "version": library_data.get("metadata", {}).get("version", "1.0.0"),
            "description": library_data.get("metadata", {}).get("description", "")
        },
        "interface": {},
        "types": {}
    }
    
    # Extract types from the library
    if "types" in library_data:
        schema["types"] = library_data["types"]
    
    # Process functions
    if "functions" in library_data:
        for func_name, func_data in library_data["functions"].items():
            # Skip internal functions (starting with underscore)
            if func_name.startswith("_"):
                continue
            
            # Get return type and create a meaningful description
            return_type = process_type(func_data.get("return", "jsonlang.Unit"))
            return_desc = get_return_description(return_type)
            
            # Create function schema with a more meaningful description
            func_schema = {
                "description": func_data.get("description", f"{func_name.capitalize()} operation"),
                "args": [],
                "return": {
                    "type": return_type,
                    "description": return_desc
                }
            }
            
            # Process arguments
            args = func_data.get("args", [])
            if isinstance(args, list):
                # Handle array of strings (simple argument names)
                for i, arg in enumerate(args):
                    if isinstance(arg, str):
                        # Simple argument name - try to infer type from function name and context
                        arg_type = infer_argument_type(func_name, arg, i)
                        arg_schema = {
                            "type": arg_type,
                            "name": arg,
                            "required": True,
                            "description": get_argument_description(func_name, arg, arg_type)
                        }
                        func_schema["args"].append(arg_schema)
                    elif isinstance(arg, dict):
                        # Argument with detailed information
                        arg_type = process_type(arg.get("type", "jsonlang.Any"))
                        arg_name = arg.get("name", f"arg{i}")
                        arg_schema = {
                            "type": arg_type,
                            "name": arg_name,
                            "required": arg.get("required", True),
                            "description": arg.get("description", get_argument_description(func_name, arg_name, arg_type))
                        }
                        
                        # Add optional fields if present
                        if "default" in arg:
                            arg_schema["default"] = arg["default"]
                        if "variadic" in arg:
                            arg_schema["variadic"] = arg["variadic"]
                            
                        func_schema["args"].append(arg_schema)
            
            # Add the function to the schema
            schema["interface"][func_name] = func_schema
    
    # Write the schema to the output file if specified
    if output_file:
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(schema, f, indent=2)
            print(f"Schema written to {output_file}")
        except Exception as e:
            print(f"Error writing schema to file: {e}")
            sys.exit(1)
    
    return schema

def process_type(type_str: str) -> str:
    """
    Process a type string to ensure it has the correct format.
    
    Args:
        type_str: The type string to process
        
    Returns:
        The processed type string
    """
    # Handle imports prefix
    if type_str.startswith("imports."):
        type_str = type_str[8:]  # Remove "imports." prefix
    
    # Add jsonlang prefix if not present and not a custom type
    if not type_str.startswith("jsonlang.") and type_str not in ["Unit", "String", "Number", "Boolean", "Array", "Any"]:
        # Check if it's a basic type that should have jsonlang prefix
        if type_str in ["Unit", "String", "Number", "Boolean", "Array", "Any"]:
            type_str = f"jsonlang.{type_str}"
    elif type_str in ["Unit", "String", "Number", "Boolean", "Array", "Any"]:
        type_str = f"jsonlang.{type_str}"
        
    return type_str

def get_return_description(return_type: str) -> str:
    """
    Generate a meaningful description for a return type.
    
    Args:
        return_type: The return type
        
    Returns:
        A description of the return value
    """
    if return_type == "jsonlang.Unit":
        return "No return value"
    elif return_type == "jsonlang.String":
        return "String result"
    elif return_type == "jsonlang.Number":
        return "Numeric result"
    elif return_type == "jsonlang.Boolean":
        return "Boolean result (true or false)"
    elif return_type == "jsonlang.Array":
        return "Array result"
    elif return_type == "jsonlang.Any":
        return "Result of any type"
    else:
        return f"Result of type {return_type}"

def infer_argument_type(func_name: str, arg_name: str, arg_index: int) -> str:
    """
    Infer the type of an argument based on function name and context.
    
    Args:
        func_name: The name of the function
        arg_name: The name of the argument
        arg_index: The index of the argument in the argument list
        
    Returns:
        The inferred type of the argument
    """
    # Common math functions typically use Number type
    math_functions = ["add", "subtract", "multiply", "divide", "power", "sqrt", "abs", "floor", "ceil", "round"]
    if func_name in math_functions:
        return "jsonlang.Number"
    
    # String functions typically use String type
    string_functions = ["concat", "length", "substring", "to_upper", "to_lower", "trim", "split", "join"]
    if func_name in string_functions:
        if arg_name in ["s", "str", "string", "text"]:
            return "jsonlang.String"
        elif arg_name in ["delimiter", "separator"]:
            return "jsonlang.String"
    
    # Array functions typically use Array type for the first argument
    array_functions = ["array_push", "array_pop", "array_get", "array_set", "array_length", "array_sort", "array_reverse"]
    if func_name in array_functions and arg_name in ["array", "arr"] or arg_index == 0:
        return "jsonlang.Array"
    
    # Common argument names
    if arg_name in ["str", "string", "text", "message", "name", "path", "filename"]:
        return "jsonlang.String"
    elif arg_name in ["num", "number", "value", "count", "index", "size", "length"]:
        return "jsonlang.Number"
    elif arg_name in ["bool", "flag", "enabled", "visible"]:
        return "jsonlang.Boolean"
    elif arg_name in ["arr", "array", "list", "items"]:
        return "jsonlang.Array"
    
    # Default to Any if we can't infer the type
    return "jsonlang.Any"

def get_argument_description(func_name: str, arg_name: str, arg_type: str) -> str:
    """
    Generate a meaningful description for an argument.
    
    Args:
        func_name: The name of the function
        arg_name: The name of the argument
        arg_type: The type of the argument
        
    Returns:
        A description of the argument
    """
    # Math function arguments
    if func_name == "add" and arg_name in ["a", "b"]:
        return f"{'First' if arg_name == 'a' else 'Second'} number to add"
    elif func_name == "subtract" and arg_name in ["a", "b"]:
        return "Minuend" if arg_name == "a" else "Subtrahend"
    elif func_name == "multiply" and arg_name in ["a", "b"]:
        return f"{'First' if arg_name == 'a' else 'Second'} factor"
    elif func_name == "divide" and arg_name in ["a", "b"]:
        return "Dividend" if arg_name == "a" else "Divisor"
    elif func_name == "power" and arg_name in ["base", "exponent"]:
        return "Base value" if arg_name == "base" else "Exponent value"
    
    # String function arguments
    elif func_name == "substring" and arg_name in ["s", "start", "end"]:
        if arg_name == "s":
            return "Input string"
        elif arg_name == "start":
            return "Start index"
        else:
            return "End index (optional)"
    
    # Common argument types
    if arg_type == "jsonlang.String":
        return f"String {arg_name}"
    elif arg_type == "jsonlang.Number":
        return f"Numeric {arg_name}"
    elif arg_type == "jsonlang.Boolean":
        return f"Boolean {arg_name}"
    elif arg_type == "jsonlang.Array":
        return f"Array of {arg_name}"
    elif arg_type == "jsonlang.Any":
        return f"{arg_name.capitalize()} of any type"
    
    # Default description
    return f"Argument {arg_name}"

def main():
    """Main function"""
    if len(sys.argv) < 2:
        print("Usage: python json_schema_generator.py <library_file> [output_file]")
        sys.exit(1)
    
    library_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    generate_schema(library_file, output_file)

if __name__ == "__main__":
    main()
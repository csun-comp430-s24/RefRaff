# RefRaff

The programming language RefRaff supports structs and reference counted memory management.

## Grammar

```
type ::= `int` | `bool` |                             // Integers and booleans are types
         `void` |
         structname |                                 // Structures are a type

param :: = type var

comma_param ::= [param (`,` param)*]

// Structs
structdef ::= `struct` structname           
  `{` (param `;`)* `}`

// Functions
fdef ::= `func` funcname `(` comma_param `)` `:` type    
         `{` stmt* `}`

struct_actual_param ::= var `:` exp

struct_actual_params ::=
  [struct_actual_param (`,` struct_actual_param)*]

comma_exp ::= [exp (`,` exp)*]

primary_exp ::=
  i | `true` | `false` | var |                        // Integers, booleans, and variables
  `null` |                                            // Null; assignable to struct types
  `(` exp `)` |                                       // Parenthesized expression
  `new` structname `{` struct_actual_params `}` |     // Allocate a new struct
  funcname `(` comma_exp `)`                          // Function calls

// Accessing the field of a struct or calls
dot_exp ::= primary_exp (`.` var)*

not_exp ::= [`!`]dot_exp
	
mult_exp ::= not_exp ((`*` | `/`) not_exp)*

add_exp ::= mult_exp ((`+` | `-`) mult_exp)*
		
lte_gte_exp ::= add_exp [(`<=` | `>=` | `<` | `>`) add_exp]

equals_exp ::= lte_gte_exp ((`==` | `!=`) lte_gte_exp)* 

and_exp ::= equals_exp (`&&` equals_exp)*

or_exp ::= and_exp (`||` and_exp)*
	
exp ::= or_exp

stmt ::= type var `=` exp `;` |                       // Variable declaration
         var `=` exp `;` |                            // Assignment
         `if` `(` exp `)` stmt [`else` stmt] |        // if
         `while` `(` exp `)` stmt |                   // while
         `break` `;` |                                // break
         `println` `(` exp `)` `;` |                  // Printing something
         `{` stmt* `}` |                              // Block
         `return` [exp] `;` |                         // Return
         exp `;`                                      // Expression statements

program ::= structdef* fdef* stmt*                    // stmt* is the entry point
```

### Valid symbols

```
`,`, `{`, `}`, ':', `;`, `(`, `)`, `.`, `!`, `*`, `/`, `+`,
`-`, `<=`, `>=`, `<`, `>`, `==`, `!=`, `&&`, `||`, `=`
```

### Keywords

```
`int`, `bool`, `void`, `struct`, `func`, `true`, `false`, `null`,
`new`, `if`, 'else', `while`, `break`, `println`, `return`
```

### Identifier Restrictions

1. Must start with an alphabetic character [a-zA-Z]
2. Followed by zero or more alphanumeric characters (a-zA-Z0-9)*

### Integer Literal Restrictions

1. Must be a zero or start with a numeric character [1-9]
2. Followed by any numeric character [0-9]*

### AST Definition

Node Interface
- AbstactSyntaxTreeNode Abstract Class
  - Program Class<br>

  *Expressions:*
  - Expression Abstract Class
    - BinaryOpExp Class
    - DotExp Class
    - UnaryOpExp Class<br>

    *Primary Expressions:*
      - PrimaryExpression Class
        - BoolLiteralExp Class
        - IntLiteralExp Class
        - NullExp Class
        - ParenExp Class
        - VariableExp Class<br>

  *Functions:*
  - FuntionDef Class
  - FunctionName Class (extends Type)<br>

  *Operators:*
  - OperatorEnum Enum<br>

  *Statements:*
  - Statement Abstract Class
    - AssignStmt Class
    - BreakStmt Class
    - ExpressionStmt Class
    - IfElseStmt Class
    - PrintlnStmt Class
    - ReturnStmt Class
    - StmtBlock Class
    - VardecStmt Class
    - WhileStmt Class<br>

  *Structs:*
  - Param Class
  - StructDef Class<br>

  *Types:*
  - Type Abstract Class
    - BoolType Class
    - IntType Class
    - StructName Class
    - VoidType Class

Variable Class


## Example Program
```
struct Node {
  int value;
  Node rest;
}

func length(Node list): int {
  int retval = 0;
  while (list != null) {
     retval = retval + 1;
     list = list.rest;
  }
  return retval;
}

Node list =
  new Node {
    value: 0,
    rest: new Node {
      value: 1,
      rest: new Node {
        value: 2,
        rest: null
      }
    }
  };

println(length(list));
```

## System Dependencies

In order to run RefRaff, you will need to install gcc and DrMemory. These are used to compile the code into C and generate memory leak reports, respectively.

## Reference Counting for Memory Management

This section describes the reference counting mechanism implemented in the compiler to manage the lifecycle of structs. Reference counting ensures proper memory management by tracking how many references exist to each struct instance, allowing for automatic memory allocation and deallocation.

### Overview
- **Immutability**: Structs are immutable, preventing cycles and simplifying reference counting.

- **Struct Lifecycle Management**: Functions are automatically generated for each struct to handle allocation, reference retention, and reference release.

### Features
- **structScopeManager**: Manages struct variables within scopes. When exiting a scope, the manager automatically releases all struct variables declared in that scope.

- **Reference Count Operations**:
  - **Allocation**: Allocates structs, initializing the reference count to 1. Structs can be allocated without being immediately assigned to a variable.

  - **Retention**: Increments the reference counts of the struct and recursively for all struct fields that are also structs.

  - **Release**: Decrements the reference counts recursively for the struct and its fields. Frees the struct if its reference count drops below one.

### Struct Allocation and Management Process
1. **Field Allocation**: When a struct has other structs as fields, these fields are allocated first and temporarily held.

2. **Struct Assembly**: Structs are assembled by assigning the allocated fields to the correct struct fields in a parent struct.

3. **Handling Temporary Variables**: Temporary variables used during struct assembly are managed by the same allocation, retention, and release functions, which can lead to wordy programs.

### Example
Consider a struct `A` with a field of its own type:
```
struct A {
  A a;
}

A a1 = new A {
  a: new A {
    a: null
  }
};
```
- **Allocation Order**: The innermost `A` struct is allocated first by assigning `null` to a temporary variable. This process is repeated until the outermost `A` struct is assembled and assigned to a temporary variable.<br>
This can then be assigned to a named variable if the struct allocation was part of an assignement or variable declaration statement.

- **Variable Management**: 
  - In a struct assignment statement, any earlier value the struct variable was holding is "released" before the new value is assigned.

  - In a struct assignment or variable declaration statement where an existing struct is being assigned, the existing struct is then "retained" so that the reference count can reflect that a addition variable points to it. When a new struct is created, it is not retained, as its reference count is initially set to 1.

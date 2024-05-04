# RefRaff

The programming language RefRaff supports structs and reference counted memory management.

## Table of Contents

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li><a href="#why-refraff">Why RefRaff</a></li>
    <li><a href="#refraff-examples">RefRaff Examples</a></li>
    <li><a href="#known-limitations">Known Limitations</a></li>
    <li><a href="#what-we-would-do-differently">What Would We Do Differently</a></li>
    <li><a href="#compiling-refraff">Compiling RefRaff</a></li>
    <li><a href="#running-the-refraff-compiler">Running the RefRaff Compiler</a></li>
    <li>
      <a href="#grammar">Formal Syntax Definition</a>
      <ul>
        <li><a href="#valid-symbols">Valid Symbols</a></li>
        <li><a href="#reserved-words">Reserved Words</a></li>
        <li><a href="#identifier-restrictions">Identifier Restrictions</a></li>
        <li><a href="#integer-literal-restrictions">Integer Literal Restrictions</a></li>
      </ul>
    </li>
  </ol>
</details>

## Why RefRaff

## RefRaff Examples

## Known Limitations

## What We Would Do Differently

## Compiling RefRaff

## Running the RefRaff Compiler

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

### Valid Symbols

```
`,`, `{`, `}`, ':', `;`, `(`, `)`, `.`, `!`, `*`, `/`, `+`,
`-`, `<=`, `>=`, `<`, `>`, `==`, `!=`, `&&`, `||`, `=`
```

### Reserved Words

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
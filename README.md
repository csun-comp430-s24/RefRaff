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
         `break` `;` | break
         `println` `(` exp `)` |                      // Printing something
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

interface Type
 - IntType
 - BoolType
 - VoidType
 - StructType
 - FuncType

interface Stmt


interface Exp
  

interface Op
 - DotOp
 - NotOp
 - MultiplyOp
 - DivisionOp
 - PlusOp
 - MinusOp
 - LessThanOp
 - LessThanOrEqualsOp
 - GreaterThanOp
 - GreaterThanOrEqualsOp
 - DoubleEqualsOp
 - NotEqualsOp
 - LogicalOrOp
 - LogicalAndOp

class Program

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

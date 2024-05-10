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
    <li><a href="#grammar">Formal Syntax Definition</a></li>
    <li><a href="#implementation-details">Implementation Details</a></li>
  </ol>
</details>

## Why RefRaff

RefRaff is a simple, memory-safe programming language that supports structs, functions, and typical language operators.
Our language compiles to C. RefRaff might be useful when writing a program that needs to be fast and memory safe. Given
the restrictions on our language, programs in RefRaff must not include circular data structures, strings, or comments.
Admittedly, the number of cases where RefRaff would be useful is quite limited.

We chose RefRaff as our project because our development team had an interest in creating a language that used reference
counted memory. We thought the language design was achievable over the course of the semester, even with a development
team of size two.

## RefRaff Examples

### Example 1: Linked List

The first example shows an implementation of a linked list with a struct and functions to find the length, print the length,
and check for list equality.

```
struct Node {
  int value;
  Node rest;
}

func length(Node list): int {
  if (list == null) {
    return 0;
  }

  return 1 + length(list.rest);
}

func printLength(Node list): void {
    println(length(list));
}

func equals(Node list1, Node list2): bool {
    bool foundInconsistentValue = false;
    while (list1 != null && list2 != null)
        if (list1.value != list2.value) {
            foundInconsistentValue = true;
            break;
        }

    return !foundInconsistentValue && list1 == null && list2 == null;
}

func sumNodes(Node list): int {
    int sum = 0;
    while (list != null) {
        sum = sum + list.value;
        list = list.rest;
    }

    return sum;
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

Node list2 = null;

printLength(list);
println(sumNodes(list));

println(equals(list, list2));
println(list.value);

Node list3 = new Node {
    value: length(list),
    rest: null
};
```

Output (spaces added for clarity):
```
3
3

false
0
```

### Example 2: Operators and function overloading

An example showing the operators used in our project and function overloading.

```
func returnSelf(bool b): bool {
    return b;
}

func returnSelf(int i): int {
    return i;
}

println(returnSelf(false));
println(returnSelf(3));

println(!false);

println((3 + 2) * 7 - 4 / 2);
println(1 > 2 && 2 <= 1);

println(1 >= 2 || 2 < 1);
println(3 == 3 && 4 != 3);
```

Output (extra spaces added for clarity):
```
false
3

true

33
false

false
true
```

## Known Limitations

### Intentional Limitations - Language Design

1. Structs are immutable. This prevents cycles that lead to leaked memory in implementations of reference counting that
   do not support weak references.
2. Structs must be defined in order. For example, if struct B uses struct A, struct A must be defined in the file before
   struct B.

* This was chosen to prevent structs from having the possibility of cycles and to mimic our target language of C.

3. Struct allocations must initialize all fields declared in the struct definition.
4. Functions must be defined in order. This works the same as is required by structs.
5. `println` does not allow struct arguments, only ints and bools.
6. Typical language features, such as comments and strings, are not supported.
7. Integer and identifier restrictions can be found in the grammar section below.

### Unknown Limitations

#### Exhaustive testing

Our compiler reports 91% code coverage using JaCoCo and 94% code coverage using the IntelliJ coverage tool. We have
created a comprehensive test suite composed of 395 tests. These include unit tests, integration tests, and end-to-end
tests (which check for expected code output and no leaked memory). While we believe that we have tested every edge case,
there is a possibility that there are undiscovered issues in our compiler.

#### Testing and compilation on different operating systems

Our compiler has only been tested on Windows (by the developers) and Linux (using GitHub Actions).
When setting up the GitHub Action for a Linux environment, we had to modify the commands used by
Dr. Memory and change how file paths were represented when running command line arguments. We have not
attempted to run our test suite or compile RefRaff on MacOS. It is currently unknown whether tests for the code
generator will run properly on MacOS.

Our [GitHub Actions page](https://github.com/csun-comp430-s24/RefRaff/actions) and main commit history can be viewed to
verify that our tests are currently working on a Linux environment.

## What We Would Do Differently

### Design Decisions

#### Meta language

Since our development team was a group of two, we chose Java as a meta language for the compiler out of familiarity
and to complete our compiler before the end of the course. This design decision had the biggest impact on how
RefRaff was written. We wrote ~4,500 lines of Java in just our compiler and an additional ~3,200 lines of code for
testing (excluding blank lines).

In class, an example of parsing with Scala was shown that used enums, pattern matching, and parser combinators that was 
extremely simple to implement. While Java does support enums and pattern matching, these features are extremely limited 
in comparison to languages like Scala, Rust, and Swift. Using a language with better enums and advanced matching features 
would generate less code bloat. Choosing a "better" meta language, from the start, would undeniably have made
our code easier to write, maintain, and read. The downside to choosing a "better" meta language was the possibility
of not completing our project, with the added complexity of learning a new programming language alongside our compiler
development.

#### Sourcing for more detailed error messages

When implementing error messages that show more detail, better design decisions could have been made. context is needed
about the original position of tokens and AST
nodes. We added this feature after work on the tokenizer and parser were complete. For the tokenizer, a wrapper class of
`Sourced` was used (e.g. `Sourced<Token>`) since single instances of tokens are created for reserved words and symbols.
For the parser, taking the same approach would have required modifying the definition of each AST node in RefRaff. We
instead added methods for `getSource` and `setSource` to the abstract class for an AST node. While this approach worked,
it caused issues later in the typechecker.

The most detailed error messages, found in the typechecker, used the source information from the tokenizer and parser
to point to pieces of the code that contained errors. When creating a unit test for the typechecker with an expected
failure, the test would fail because we had not injected the source information to the AST nodes. This created a lot of
headache and required additional preparation to get our tests running correctly in the code generator. Looking back,
more thought should have been given to avoiding this issue and coming up with a better solution.

#### Annotating the AST with type information

Similarly to adding sourcing to the AST, annotating the AST to add type information required many hours of debugging to
get our tests running. While we are not sure if a reasonable alternative is available, we did not spend much time
considering other approaches to this problem.

## Compiling RefRaff

In order to compile RefRaff, your machine must have Dr. Memory (a memory leak detection tool for C), GCC, Java JDK 17+,
and Maven installed. For convenience, some links are provided below to install these dependencies:

- [Installing Dr. Memory](https://drmemory.org/page_install.html)
- Installation tutorials for GCC:
    - [Windows](https://dev.to/gamegods3/how-to-install-gcc-in-windows-10-the-easier-way-422j)
    - [MacOS](https://osxdaily.com/2023/05/02/how-install-gcc-mac/)
    - [Linux](https://www.geeksforgeeks.org/how-to-install-gcc-compiler-on-linux/)
- [Installing JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- [Installing Maven](https://maven.apache.org/install.html)

Once all prerequisites have been installed, RefRaff can be compiled from the terminal using `mvn package`.
The compiled jar file is saved to the `target/refraff-1.0.0.jar` file.

**Note:** If tests fail because of MacOS incompatibility, `mvn package -D=skipTests` can be used to compile RefRaff
without running our test suite.

## Running the RefRaff Compiler

RefRaff can be run using the following command:

`java -jar <PATH_TO_REFRAFF_JAR> <INPUT_FILE> <OUTPUT_FILE>`

where a valid input file ends with either a `.txt` or `.refraff` extension, and a valid output file ends with the `.c`
extension.

## Grammar

### Formal Syntax Definition

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

### Identifier Restrictions

1. Must start with an alphabetic character [a-zA-Z]
2. Followed by zero or more alphanumeric characters (a-zA-Z0-9)*

### Integer Literal Restrictions

1. Must be a zero or start with a numeric character [1-9]
2. Followed by any numeric character [0-9]*

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

## Implementation Details

### Reference Counting for Memory Management

This section describes the reference counting mechanism implemented in the compiler to manage the lifecycle of structs.
Reference counting ensures proper memory management by tracking how many references exist to each struct instance,
allowing for automatic memory allocation and deallocation.

#### Overview

- **Immutability**: Structs are immutable, preventing cycles and simplifying reference counting.

- **Struct Lifecycle Management**: Functions are automatically generated for each struct to handle allocation, reference
  retention, and reference release.

#### Features

- **structScopeManager**: Manages struct variables within scopes. When exiting a scope, the manager automatically
  releases all struct variables declared in that scope.

- **Reference Count Operations**:
    - **Allocation**: Allocates structs, initializing the reference count to 1. Structs can be allocated without being
      immediately assigned to a variable.

    - **Retention**: Increments the reference counts of the struct and recursively for all struct fields that are also
      structs.

    - **Release**: Decrements the reference counts recursively for the struct and its fields. Frees the struct if its
      reference count drops below one.

#### Struct Allocation and Management Process

1. **Field Allocation**: When a struct has other structs as fields, these fields are allocated first and temporarily
   held.

2. **Struct Assembly**: Structs are assembled by assigning the allocated fields to the correct struct fields in a parent
   struct.

3. **Handling Temporary Variables**: Temporary variables used during struct assembly are managed by the same allocation,
   retention, and release functions, which can lead to wordy programs.

#### Example

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

- **Allocation Order**: The innermost `A` struct is allocated first by assigning `null` to a temporary variable. This
  process is repeated until the outermost `A` struct is assembled and assigned to a temporary variable.<br>
  This is then be assigned to a named variable if the struct allocation was part of an assignement or variable
  declaration statement.

| Generated Code                                         | Explanation                                                                                              |
|--------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| // Allocating struct A                                 |
| struct A* _temp_A_struct_alloc_var = NULL;             | Create temporary variable for allocation expression                                                      
| struct A* _temp_A_a = NULL;                            | Create temporary variable for struct field (this is the deepest 'a' field)                               
| refraff_A_release(_temp_A_a);                          | This line is superfluous in this case (but it matters in some other assignments)                         
| _temp_A_a = refraff_A_alloc(_temp_A_a);                | Create middle struct                                                                                     
| _temp_A_struct_alloc_var = refraff_A_alloc(_temp_A_a); | Create outer struct                                                                                      
| struct A* a1 = _temp_A_struct_alloc_var;               | Assign struct to named variable                                                                          
| refraff_A_retain(a1);                                  | This struct has A1 and the temp variable pointing to it. Both will be released when moving out of scope. 
| // Exiting scope                                       |
| refraff_A_release(a1);                                 |
| refraff_A_release(_temp_A_struct_alloc_var);           |
| refraff_A_release(_temp_A_a);                          |

- **Variable Management**:
    - In a struct assignment statement, any earlier value the struct variable was holding is "released" before the new
      value is assigned.

    - In a struct assignment or variable declaration statement where an existing struct is being assigned, the existing
      struct is then "retained" so that the reference count can reflect that a addition variable points to it. When a
      new struct is created, it is not retained, as its reference count is initially set to 1.



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
package refraff.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import refraff.parser.*;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.parser.function.*;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.statement.*;

public class Codegen {

    private static final String DEFAULT_OUTPUT_FILE_NAME = "output.c";

    private final Program program;
    private final Path generatedCodePath;
    private BufferedWriter writer;
    private int currentIndentCount;
    private StructScopeManager structScopeManager;

    // This is to get the types of struct fields when instantiating from StructAllocExp that don't have that info
    Map<String, StructDef> structNameToDef;

    private final Map<String, List<FunctionDef>> overloadedFunctionNameToFunctionDefs;
    private final Map<FunctionDef, String> functionDefToFunctionName;

    private Codegen(Program program, File directory, String outputFileName) {
        this.program = program;

        String trueOutputName = outputFileName == null ? DEFAULT_OUTPUT_FILE_NAME : outputFileName;
        this.generatedCodePath = Paths.get(directory.getPath(), trueOutputName);

        this.structScopeManager = new StructScopeManager();
        this.structNameToDef = program.getStructDefs().stream()
                .collect(Collectors.toMap(
                        structDef -> structDef.getStructName().getName(),
                        Function.identity()));

        this.overloadedFunctionNameToFunctionDefs = new HashMap<>();
        this.functionDefToFunctionName = new HashMap<>();
    }

    public static void generateProgram(Program program, File directory) throws CodegenException {
        generateProgram(program, directory, null);
    }

    public static void generateProgram(Program program, File directory, String outputFileName) throws CodegenException {
        // We could directly supply the System.getProperty("user.dir"); in the main function
        // But we should be able to specify where the output is going to go
        new Codegen(program, directory, outputFileName).generateProgram();
    }

    private void generateProgram() throws CodegenException {
        try {
            writer = new BufferedWriter(new FileWriter(generatedCodePath.toString()));
            
            // Add boilerplate
            addImports();

            // Generate struct definitions
            generateStructDefs(program.getStructDefs());

            // Generate function definitions
            generateFunctionDefs(program.getFunctionDefs());

            // Generate the main function entry point
            addString("int main()");
            addNewline();
            addString("{");
            addNewline();

            // Add the program's statements to the entry point
            enterScope();
            generateStatements(program.getStatements());
            exitScope();

            // Close the main method
            addString("}");

            writer.close();
        } catch (IOException e) {
            throw new CodegenException("Error in writing to file: " + e.getMessage());
        } 
    }

    private void addImports() throws CodegenException {
        addString("#include <stdio.h>");
        addNewline();

        addString("#include <stdlib.h>");
        addNewline();
        addNewline();
        addNewline();
    }

    // Adds the specified string to the open file
    private void addString(String str) throws CodegenException {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new CodegenException("Error in writing string");
        }
    }

    private void indentLine(int numIndents) throws CodegenException {
        try {
            for (int i = 0; i < numIndents; i++) {
                // We can change this to spaces if that's your preference
                writer.write("\t");
            }
        } catch (IOException e) {
            throw new CodegenException("Error in indenting line");
        }
    }

    // Adds the specified string to the open file
    private void addIndentedString(String str) throws CodegenException {
        try {
            indentLine(currentIndentCount);
            writer.write(str);
        } catch (IOException e) {
            throw new CodegenException("Error in writing indented string");
        }
    }

    // Adds a space to the open file
    private void addSpace() throws CodegenException {
        try {
            writer.write(" ");
        } catch (IOException e) {
            throw new CodegenException("Error in writing space");
        }
    }

    private void addSemicolonNewline() throws CodegenException {
        try {
            writer.write(";\n");
        } catch (IOException e) {
            throw new CodegenException("Error in writing semi-colon and new line");
        }
    }

    // Adds a newline to the open file
    private void addNewline() throws CodegenException {
        try {
            writer.write("\n");
        } catch (IOException e) {
            throw new CodegenException("Error in writing new line");
        }
    }

    private void addComment(String comment) throws CodegenException {
        addNewline();
        addIndentedString("// " + comment);
        addNewline();
    }

    private void generateStructVariableReleaseFunction(String structVariable, StructType structType) throws CodegenException {
        // refraff_<STRUCT_TYPE>_release(<STRUCT_VARIABLE_NAME>)
        addIndentedString(getReleaseStructFunctionName(structType));
        addString("(");
        addString(structVariable);
        addString(")");
        addSemicolonNewline();
    }

    private void enterScope() {
        currentIndentCount += 1;
        structScopeManager.enterScope();
    }

    // Frees structs held by variables that are about to go out of scope
    private void exitScope() throws CodegenException {
        addComment("Exiting scope");

        // Get the list of struct variables that were declared in the current scope
        Map<String, StructType> currentScopeStructVariables = structScopeManager.exitScope();

        // Release each variable
        for (Map.Entry<String, StructType> entry : currentScopeStructVariables.entrySet()) {
            generateStructVariableReleaseFunction(entry.getKey(), entry.getValue());
        }
        currentIndentCount -= 1;
    }

    private void addStructVariableToScope(String structVariable, StructType structType) throws CodegenException {
        structScopeManager.addStructVariableToScope(structVariable, structType);
    }

    private void generateStructDefs(List<StructDef> structDefs) throws CodegenException {
        // Struct definitions were allowed to be used in any order in the typechecker, but order matters in C
        // So we're going to re-order them according to dependencies, first

        Queue<StructDef> queueOfUnprocessedStructs = new ArrayDeque<>(structDefs);
        Set<String> alreadyCodeGeneratedStructNames = new HashSet<>();

        while (!queueOfUnprocessedStructs.isEmpty()) {
            StructDef structDef = queueOfUnprocessedStructs.poll();
            String currentStructName = structDef.getStructName().getName();

            boolean hasNonGeneratedStructDependencies = false;

            for (Param param : structDef.getParams()) {
                if (param.getType() instanceof StructType paramStructType) {
                    String paramStructName = paramStructType.getStructName().get().getName();

                    // If we have a recursive struct, we don't care about the order for this
                    if (paramStructName.equals(currentStructName)) {
                        continue;
                    }

                    // Otherwise, we need to make sure that the struct that this struct depends on, is already generated
                    if (alreadyCodeGeneratedStructNames.contains(paramStructName)) {
                        continue;
                    }

                    // We don't have a dependent struct already generated here! So we need to generate that one first
                    hasNonGeneratedStructDependencies = true;
                    break;
                }
            }

            // Add this struct to the back of the queue, if we need to code generate another struct first
            if (hasNonGeneratedStructDependencies) {
                queueOfUnprocessedStructs.offer(structDef);
                continue;
            }

            // Otherwise, just code gen it and add it to our list of already code generated structs
            generateStructDef(structDef);
            generateStructAllocationFunction(structDef);
            generateStructRetainFunction(structDef);
            generateStructReleaseFunction(structDef);

            alreadyCodeGeneratedStructNames.add(currentStructName);
        }
    }

    private String getStructRefcountField(StructType structType) throws CodegenException {
        return getStructRefcountField(structType.getStructName().get().getName());
    }

    private String getStructRefcountField(String structName) throws CodegenException {
        return structName + "_refcount";
    }

    private void generateStructDef(StructDef structDef) throws CodegenException {
        /*
         * typedef struct <STRUCT_NAME>
         * {
         *     int <STRUCT_NAME>_refcount;
         *     <PARAM_TYPE_1> <PARAM_NAME>;
         *      .
         *      .
         *      .
         * } <STRUCT_NAME>;
         *
         * This format allows for using STRUCT_NAME as a type, rather than needing to use struct <STRUCT_NAME>*
         */

        String structName = structDef.getStructName().getName();

        addString("typedef struct ");
        addString(structName);
        addNewline();
        addString("{");
        addNewline();

        currentIndentCount += 1;

        // Add field for reference counting
        indentLine(currentIndentCount);
        generateType(new IntType());
        addSpace();
        addString(getStructRefcountField(structName));
        addSemicolonNewline();

        for (Param param : structDef.getParams()) {
            indentLine(currentIndentCount);

            generateType(param.getType());
            addSpace();
            generateVariable(param.getVariable());
            addSemicolonNewline();
        }

        currentIndentCount -= 1;

        addString("} ");
        addString(structName);
        addSemicolonNewline();

        addNewline();
    }

    private String getStructAllocationFunctionName(StructType structType) {
        String structName = structType.getStructName().get().getName();

        // Return a function name: refraff_<STRUCT_NAME>_alloc
        return "refraff_" + structName + "_alloc";
    }

    private void generateCommaSeparatedParams(List<Param> params) throws CodegenException {
        for (int i = 0; i < params.size(); i++) {
            Param param = params.get(i);

            generateType(param.getType());
            addSpace();
            generateVariable(param.getVariable());

            if (i != params.size() - 1) {
                addString(", ");
            }
        }
    }

    private void generateStructAllocationFunction(StructDef structDef) throws CodegenException {
        /*
         * Generates a function to allocate new structs on the heap:
         *
         * <STRUCT_NAME>* refraff_<STRUCT_NAME>_alloc(<PARAMS>)
         * {
         *      <STRUCT_NAME>* newStruct = malloc(sizeof(struct <STRUCT_NAME>));
         * 
         *      if (newStruct == NULL) {
         *          fprintf(stderr, "Failed to allocate memory!\n");
         *          exit(EXIT_FAILURE);
         *      }
         * 
         *      newStruct-><STRUCT_NAME>_refcount = 1;
         *      newStruct->[FIELD_1] = [PARAM_1];
         *      newStruct->[FIELD_2] = [PARAM_2];
         *      ...
         *      return newStruct;
         * }
         */
        StructType structType = new StructType(structDef.getStructName());

        generateType(structType);
        addSpace();
        addString(getStructAllocationFunctionName(structType));

        // Comma separate the parameters into the new function
        addString("(");
        generateCommaSeparatedParams(structDef.getParams());
        addString(")");
        addNewline();

        addString("{");
        addNewline();

        currentIndentCount += 1;

        final String mallocFormat = "%1$s* newStruct = malloc(sizeof(struct %1$s));";

        indentLine(currentIndentCount);
        addString(String.format(mallocFormat, structDef.getStructName().getName()));
        addNewline();
        addNewline();

        // Check that struct was allocated - we can probably remove this. I was debugging
        addIndentedString("if (newStruct == NULL)\n");
        addIndentedString("{\n");
        currentIndentCount += 1;
        addIndentedString("fprintf(stderr, \"Failed to allocate memory!\\n\");\n");
        addIndentedString("exit(EXIT_FAILURE);\n");
        currentIndentCount -= 1;
        addIndentedString("}\n");

        // Initialize refcount
        final String refcountFieldInitializationFormat = "newStruct->%1$s = 1;";
        indentLine(currentIndentCount);
        addString(String.format(refcountFieldInitializationFormat, getStructRefcountField(structType)));
        addNewline();

        final String fieldInitializationFormat = "newStruct->%1$s = %1$s;";

        for (Param param : structDef.getParams()) {
            indentLine(currentIndentCount);
            addString(String.format(fieldInitializationFormat, param.getVariable().getName()));
            addNewline();
        }

        addNewline();
        indentLine(currentIndentCount);
        addString("return newStruct;");
        addNewline();

        currentIndentCount -= 1;

        addString("}");

        addNewline();
        addNewline();
    }

    private String getRetainStructFunctionName(StructType structType) {
        return "refraff_" + structType.getStructName().get().getName() + "_retain";
    }

    private void generateStructFieldFunctions(List<Param> params, Function<StructType, String> releaseOrRetainFunction) 
            throws CodegenException {
        for (Param param : params) {
            if (param.getType() instanceof StructType fieldStructType) {
                // refraff_<STRUCT_FIELD_1>_release(&my_struct-><STRUCT_FIELD_1>);
                addIndentedString(releaseOrRetainFunction.apply(fieldStructType) + "(");
                String structFieldAddressFormat = "my_struct->%1$s";
                addString(String.format(structFieldAddressFormat, param.getVariable().getName()));
                addString(")");
                addSemicolonNewline();
            }
        }
    }

    private void generateStructRetainFunction(StructDef structDef) throws CodegenException {
        /*
         * Generates a function to add to the reference count when a struct object is assigned
         * 
         * void refraff_<STRUCT_NAME>_retain(<STRUCT_NAME>* my_struct)
         * {
         *      if (my_struct == NULL) return;
         * 
         *      refraff_<STRUCT_FIELD_1>_retain(my_struct-><STRUCT_FIELD_1>);
         *      refraff_<STRUCT_FIELD_1>_retain(my_struct-><STRUCT_FIELD_2>);
         *      ...
         *      my_struct-><STRUCT_NAME>_refcount++;
         * }
         */

        StructType structType = new StructType(structDef.getStructName());

        generateType(new VoidType());
        addSpace();
        addString(getRetainStructFunctionName(structType) + "(");
        generateType(structType);
        addString(" my_struct)");
        addNewline();
        addString("{");
        addNewline();

        currentIndentCount += 1;

        // If struct is null, we can return
        addIndentedString("if (my_struct == NULL) return;\n\n");

        // Retain any child structs, first
        generateStructFieldFunctions(structDef.getParams(), this::getRetainStructFunctionName);

        addIndentedString("my_struct->");
        addString(getStructRefcountField(structType));
        addString("++");
        addSemicolonNewline();

        currentIndentCount -= 1;

        addString("}");
        addNewline();
        addNewline();
    }

    private String getReleaseStructFunctionName(StructType structType) {
        return "refraff_" + structType.getStructName().get().getName() + "_release";
    }

    private void generateStructReleaseFunction(StructDef structDef) throws CodegenException {
        /*
         * Generates a function to decrement the reference count when a struct variable is 
         * reassigned or when a struct variable goes out of scope
         * 
         * void refraff_<STRUCT_NAME>_release(<STRUCT_NAME>* my_struct)
         * {
         *      if (my_struct == NULL) return;
         *
         *      refraff_<STRUCT_FIELD_1>_release(my_struct-><STRUCT_FIELD_1>);
         *      refraff_<STRUCT_FIELD_1>_release(my_struct-><STRUCT_FIELD_2>);
         *      ...
         * 
         *      my_struct-><STRUCT_NAME>_refcount--;
         *      if (my_struct-><STRUCT_NAME>_refcount < 1)
         *      {
         *          free(my_struct);
         *      }
         * }
         */

        StructType structType = new StructType(structDef.getStructName());

        generateType(new VoidType());
        addSpace();
        addString(getReleaseStructFunctionName(structType) + "(");
        generateType(structType);
        addString(" my_struct)");
        addNewline();
        addString("{");
        addNewline();

        currentIndentCount += 1;

        // If struct is null, we can return
        addIndentedString("if (my_struct == NULL) return;\n\n");

        // Free any field structs, first
        generateStructFieldFunctions(structDef.getParams(), this::getReleaseStructFunctionName);

        addIndentedString("my_struct->");
        addString(getStructRefcountField(structType));
        addString("--");
        addSemicolonNewline();

        // if (my_struct-><STRUCT_NAME>_refcount < 1)
        String structFreeCheckFormat = "if (my_struct->%1$s < 1)";
        addIndentedString(String.format(structFreeCheckFormat, getStructRefcountField(structType)));
        addNewline();
        addIndentedString("{");
        addNewline();

        currentIndentCount += 1;

        addIndentedString("free(my_struct)");
        addSemicolonNewline();

        currentIndentCount -= 1;

        addIndentedString("}");
        addNewline();

        currentIndentCount -= 1;

        addString("}");
        addNewline();
        addNewline();
    }

    private void generateFunctionDefs(List<FunctionDef> functionDefs) throws CodegenException {
        Map<String, Boolean> hasOverload = new HashMap<>();

        // Iterate over the function definitions, determine if each function name has an overload
        for (FunctionDef functionDef : functionDefs) {
            String functionName = functionDef.getFunctionName().getName();

            // If we already have an entry in our map, then we've put another function def with this name in
            if (hasOverload.containsKey(functionName)) {
                hasOverload.put(functionName, true);
                continue;
            }

            // If we don't have an entry in our map, then put a false variable in
            hasOverload.put(functionName, false);
        }

        Map<String, Integer> overloadedFunctionCounter = new HashMap<>();

        // Assign each function def a
        for (FunctionDef functionDef : functionDefs) {
            String functionName = functionDef.getFunctionName().getName();
            String correctedFunctionName = functionName;

            if (hasOverload.get(functionName)) {
                // Increment the number of overloads
                int currentOverloadCount = overloadedFunctionCounter.getOrDefault(functionName, 0) + 1;
                overloadedFunctionCounter.put(functionName, currentOverloadCount);

                correctedFunctionName += "_overload_" + currentOverloadCount;

                // Add these to a list
                List<FunctionDef> overloadedFunctionDefs = overloadedFunctionNameToFunctionDefs.getOrDefault(functionName,
                        new ArrayList<>());
                overloadedFunctionDefs.add(functionDef);
                overloadedFunctionNameToFunctionDefs.put(functionName, overloadedFunctionDefs);
            }

            functionDefToFunctionName.put(functionDef, correctedFunctionName);
            generateFunctionDef(functionDef);
        }
    }

    private void generateFunctionDef(FunctionDef functionDef) throws CodegenException {
        /*
         * <RETURN_TYPE> <FUNCTION_NAME>(<FUNCTION_PARAMS>)
         * {
         *      <FUNCTION_BODY>
         * }
         *
         */

        generateType(functionDef.getReturnType());
        addSpace();
        addString(functionDefToFunctionName.get(functionDef));

        addString("(");
        generateCommaSeparatedParams(functionDef.getParams());
        addString(")");

        generateStmtBlock(functionDef.getFunctionBody());

        addNewline();
        addNewline();
    }

    // Map of statements to their codegenerating functions functions
    private static final Map<Class<? extends Statement>, 
            CodegenVoidFunction<Codegen, Statement>> STMT_TO_GEN_FUNC = Map.of(
        AssignStmt.class, Codegen::generateAssignStmt,
        BreakStmt.class, Codegen::generateBreakStmt,
        ExpressionStmt.class, Codegen::generateExpStmt,
        IfElseStmt.class, Codegen::generateIfElseStmt,
        PrintlnStmt.class, Codegen::generatePrintlnStmt,
        ReturnStmt.class, Codegen::generateReturnStmt,
        StmtBlock.class, Codegen::generateStmtBlock,
        VardecStmt.class, Codegen::generateVardecStmt,
        WhileStmt.class, Codegen::generateWhileStmt
    );

    private void generateStatements(List<Statement> statements) throws CodegenException {
        // Find out which statement this is, then direct to that statement's generator
        for (Statement stmt : statements) {
            // Get the statements class
            Class<? extends Statement> stmtClass = stmt.getClass();

            if (!STMT_TO_GEN_FUNC.containsKey(stmtClass)) {
                throw new UnsupportedOperationException("Map did not contain mapping function for: " + stmtClass);
            }

            // These functions will throw exceptions if there are type errors
            STMT_TO_GEN_FUNC.get(stmtClass).apply(this, stmt);
        }
    }

    private void generateRetainFunctionCall(VardecStmt vardecStmt) throws CodegenException {
        generateRetainFunctionCall(new AssignStmt(vardecStmt.getVariable(), vardecStmt.getExpression()));
    }

    private void generateRetainFunctionCall(AssignStmt assignStmt) throws CodegenException {
        StructType structType = structScopeManager.getStructTypeFromVariable(assignStmt.getVariable().getName());
        // refraff_<STRUCT_NAME>_retain(<VARIABLE_NAME>);
        addIndentedString(getRetainStructFunctionName(structType));
        addString("(");
        generateVariable(assignStmt.getVariable());
        addString(")");
        addSemicolonNewline();
    }

    // Generate temporary struct variables for nested structs
    private String getTempStructVariableName(Param definedParam, StructDef parentStructDef) {
        return getTempStructVariableName(definedParam.getVariable(), parentStructDef);
    }

    private String getTempStructVariableName(Variable paramVariable, StructDef parentStructDef) {
        return "_temp_" + parentStructDef.getStructName().getName() + "_" + paramVariable.getName();
    }

    // Generate arguments for allocating new structs with the generated struct alloc functions
    private void generateCommaSeparatedArgs(final AssignStmt assignStmt) throws CodegenException {
        // Get structdef so we know the types of the params
        StructType structType = structScopeManager.getStructTypeFromVariable(assignStmt.getVariable().getName());
        StructDef structDef = structNameToDef.get(structType.getStructName().get().getName());
        List<Param> definedParams = structDef.getParams();

        // Get actual params
        StructAllocExp structAllocExp = (StructAllocExp) assignStmt.getExpression();
        List<StructActualParam> structActualParams = structAllocExp.getParams().getStructActualParams();

        for (int i = 0; i < structActualParams.size(); i++) {
            // If it's a primitive, just use the expression, otherwise, use the temporary struct variable
            if (definedParams.get(i).getType() instanceof IntType
                    || definedParams.get(i).getType() instanceof BoolType) {
                generateExpression(structActualParams.get(i).getExpression());
            } else {
                // Otherwise, get the temporary variable holding the address of the struct
                addString(getTempStructVariableName(definedParams.get(i), structDef));
            }

            if (i != structActualParams.size() - 1) {
                addString(", ");
            }
        }
    }

    private void generateDeclareTemporaryStructVariables(List<Param> definedParams, StructDef structDef)
            throws CodegenException {
        // First, allocate temporary variables for each struct field
        for (Param definedParam : definedParams) {
            // If this param is a struct
            if (definedParam.getType() instanceof StructType structType) {
                // Create the temporary variable if it has not been created yet
                String tempVariableName = getTempStructVariableName(definedParam, structDef);
                if (!structScopeManager.isInScope(tempVariableName)) {
                    generateVardecStmt(new VardecStmt(structType, new Variable(tempVariableName), new NullExp()));
                } else {
                    // Otherwise, release the previous temporary variable
                    generateStructVariableReleaseFunction(tempVariableName, structType);
                }
            }
        }
    }

    // Generate function calls for allocating new structs (recursive for nested structs)
    private void generateStructAllocFunctionCalls(final AssignStmt assignStmt) throws CodegenException {
        // Get structdef so we know they types of the params
        StructType structType = structScopeManager.getStructTypeFromVariable(assignStmt.getVariable().getName());
        StructDef structDef = structNameToDef.get(structType.getStructName().get().getName());
        // Get params (for types)
        List<Param> definedParams = structDef.getParams();

        // First, make sure temporary variables for each struct field are declared
        generateDeclareTemporaryStructVariables(definedParams, structDef);

        // Get actual params
        StructAllocExp structAllocExp = (StructAllocExp) assignStmt.getExpression();
        List<StructActualParam> structActualParams = structAllocExp.getParams().getStructActualParams();

        // Go through params (we'll need the defined params and the actual params for this)
        for (int i = 0; i < structActualParams.size(); i++) {
            // But if it's a struct, we need to allocate that, then assign it to a temporary struct variable
            if (structActualParams.get(i).getExpression() instanceof StructAllocExp fieldAlloc) {
                AssignStmt fieldAssignStmt = new AssignStmt(
                        new Variable(getTempStructVariableName(definedParams.get(i), structDef)),
                        fieldAlloc);
                generateStructAllocFunctionCalls(fieldAssignStmt);
            }
        }

        // Then allocate and assign the struct
        indentLine(currentIndentCount);
        generateVariable(assignStmt.getVariable());
        addString(" = ");
        addString(getStructAllocationFunctionName(structAllocExp.getStructType()));
        addString("(");
        generateCommaSeparatedArgs(assignStmt);
        addString(")");
        addSemicolonNewline();
    }

    // Parentheses mess up the allocation, so get rid of them
    // e.g. A a = ((new A { a: null }));
    private AssignStmt getAssignStmtWithoutParens(AssignStmt assignStmt) {
        if (assignStmt.getExpression() instanceof ParenExp parenExp) {
            return getAssignStmtWithoutParens(
                new AssignStmt(assignStmt.getVariable(), parenExp.getExp())
            );
        } else {
            return assignStmt;
        }
    }

    private void generateAssignStmt(final Statement stmt) throws CodegenException {
        AssignStmt assignStmt = (AssignStmt) stmt;

        // Check if this is a struct variable
        if (structScopeManager.isStructVariable(assignStmt.getVariable().getName())) {
            StructType structType = structScopeManager.getStructTypeFromVariable(assignStmt.getVariable().getName());
            addComment("Generating struct assignment stmt for " + assignStmt.getVariable().getName());

            // Release whatever the variable was pointing to
            generateStructVariableReleaseFunction(assignStmt.getVariable().getName(), structType);
            // If the expression is in parens, get expression
            assignStmt = getAssignStmtWithoutParens(assignStmt);
            if (assignStmt.getExpression() instanceof StructAllocExp structAllocExp) {
                // Allocate the struct, it will be assigned to a temporary variable
                generateStructAllocExp(structAllocExp);
                // Then assign it to this statement
                AssignStmt assignWithTemp = new AssignStmt(assignStmt.getVariable(),
                        new VariableExp(getTempStructAllocVariable(structType)));
                generateAssignStmt(assignWithTemp);
            } else {
                indentLine(currentIndentCount);
                generateVariable(assignStmt.getVariable());
                addString(" = ");
                generateExpression(assignStmt.getExpression());
                addSemicolonNewline();
                // Retain whatever the variable is now pointing to (if not null)
                if (!(assignStmt.getExpression() instanceof NullExp)) {
                    generateRetainFunctionCall(assignStmt);
                }
            }
            
        } else {
            // Otherwise, print regular assignment statement
            indentLine(currentIndentCount);
            generateVariable(assignStmt.getVariable());
            addString(" = ");
            generateExpression(assignStmt.getExpression());
            addSemicolonNewline();
        }
    }

    private void generateBreakStmt(final Statement stmt) throws CodegenException {
        addIndentedString("break;\n");
    }

    private void generateExpStmt(final Statement stmt) throws CodegenException {
        ExpressionStmt expStmt = (ExpressionStmt)stmt;

        indentLine(currentIndentCount);
        generateExpression(expStmt.getExpression());
        addSemicolonNewline();
    }

    private void generateIfElseStmt(final Statement stmt) throws CodegenException {
        IfElseStmt ifElseStmt = (IfElseStmt)stmt;

        // Add if and condition and opening brace
        addIndentedString("if (");
        generateExpression(ifElseStmt.getCondition());
        addString(")\n");

        // The parenthesis are generated in the statement block function. A single line if statement
        // may end up being more lines in the generated code, so I just put them in a statement block by default
        if (ifElseStmt.getIfBody() instanceof StmtBlock) {
            generateStatements(List.of(ifElseStmt.getIfBody()));
        } else {
            generateStatements(List.of(new StmtBlock(List.of(ifElseStmt.getIfBody()))));
        }

        // Add an else if there is one
        Optional<Statement> optionalElseBody = ifElseStmt.getElseBody();
        if (!optionalElseBody.isEmpty()) {
            addIndentedString("else\n");
            if (optionalElseBody.get() instanceof StmtBlock stmtBlock) {
                generateStatements(List.of(stmtBlock));
            } else {
                generateStatements(List.of(new StmtBlock(List.of(optionalElseBody.get()))));
            }
        }
    }

    private void generatePrintlnStmt(final Statement stmt) throws CodegenException {
        // Without flushing, some of the outputs are not captured
        addIndentedString("fflush(stdout);\n");

        PrintlnStmt printlnStmt = (PrintlnStmt)stmt;

        String formatString = null;
        Type expressionType = printlnStmt.getExpression().getExpressionType();

        // We want to print booleans as "true" or "false" and integers as integers
        if (expressionType instanceof BoolType) {
            formatString = "%s";
        } else if (expressionType instanceof IntType) {
            formatString = "%d";
        }

        // Throw a runtime exception if we ever encounter something we don't expect
        assert(formatString != null);

        addIndentedString("printf(\"");
        addString(formatString);
        addString("\\n\", ");

        generateExpression(printlnStmt.getExpression());

        // If we have our boolean already printed, use ternary operator to print "true" or "false" instead
        if (expressionType instanceof BoolType) {
            addString(" ? \"true\" : \"false\"");
        }

        addString(");\n");
        addIndentedString("fflush(stdout);\n");
    }

    private void generateReturnStmt(final Statement stmt) throws CodegenException {
        ReturnStmt returnStmt = (ReturnStmt) stmt;

        addIndentedString("return");

        if (returnStmt.getReturnValue().isPresent()) {
            addSpace();
            generateExpression(returnStmt.getReturnValue().get());
        }

        addSemicolonNewline();
    }

    private void generateStmtBlock(final Statement stmt) throws CodegenException {
        StmtBlock stmtBlock = (StmtBlock)stmt;

        indentLine(currentIndentCount);
        addString("{");
        addNewline();

        enterScope();
        generateStatements(stmtBlock.getBlockBody());
        exitScope();

        indentLine(currentIndentCount);
        addString("}");
        addNewline();
    }

    // Parentheses only up struct allocations, so get rid of them
    private VardecStmt getVardecStmtWithoutParens(VardecStmt vardecStmt) {
        if (vardecStmt.getExpression() instanceof ParenExp parenExp) {
            return getVardecStmtWithoutParens(
                new VardecStmt(vardecStmt.getType(), vardecStmt.getVariable(), parenExp.getExp())
            );
        } else {
            return vardecStmt;
        }
    }

    private void generateVardecStmt(final Statement stmt) throws CodegenException {
        VardecStmt vardecStmt = (VardecStmt)stmt;

        // If we are declaring a struct variable,
        if (vardecStmt.getType() instanceof StructType structType) {
            // then add that variable to the current scope
            addStructVariableToScope(vardecStmt.getVariable().getName(), structType);

            // If the expression is in parens, get expression
            vardecStmt = getVardecStmtWithoutParens(vardecStmt);
            if (vardecStmt.getExpression() instanceof StructAllocExp structAllocExp) {
                // Allocate the struct, it will be assigned to a temporary variable
                generateStructAllocExp(structAllocExp);
                // Then assign it to this statement
                VardecStmt vardecWithTemp = new VardecStmt(structType, vardecStmt.getVariable(), 
                    new VariableExp(getTempStructAllocVariable(structType)));
                generateVardecStmt(vardecWithTemp);
            } else {
                // Assign the expression
                indentLine(currentIndentCount);
                generateType(vardecStmt.getType());
                addSpace();
                generateVariable(vardecStmt.getVariable());
                addString(" = ");
                generateExpression(vardecStmt.getExpression());
                addSemicolonNewline();
                // Then retain the struct if not null
                if (!(vardecStmt.getExpression() instanceof NullExp)) {
                    generateRetainFunctionCall(vardecStmt);
                }
            }
        } else {
            // Otherwise, it's any primitive type
            indentLine(currentIndentCount);
            generateType(vardecStmt.getType());
            addSpace();
            generateVariable(vardecStmt.getVariable());
            addString(" = ");
            generateExpression(vardecStmt.getExpression());
            addSemicolonNewline();
        }
    }

    private void generateWhileStmt(final Statement stmt) throws CodegenException {
        WhileStmt whileStmt = (WhileStmt)stmt;

        addIndentedString("while (");
        generateExpression(whileStmt.getCondition());
        addString(")\n");

        // I'm having it generate braces by default because if there's a single
        // struct vardec statement, we'll be adding more retain/release function calls
        // in the same scope (But this is mostly just for consistent curly braces)

        // generate statement(s)
        if (whileStmt.getBody() instanceof StmtBlock) {
            generateStatements(List.of(whileStmt.getBody()));
        } else {
            generateStatements(List.of(new StmtBlock(List.of(whileStmt.getBody()))));
        }
    }

    private static final Map<Class<? extends Type>, Function<Type, String>> TYPE_TO_STR = Map.of(
        BoolType.class, (Type type) -> "int", // All bools literals are converted to ints as well
        IntType.class, (Type type) -> "int",
        // convert the struct name to the pointer representation (e.g. type for 'struct foo' => `struct foo*` in C)
        StructType.class, (Type type) -> "struct " + ((StructType) type).getStructName().get().structName + "*",
        VoidType.class, (Type type) -> "void"
    );

    private void generateType(final Type type) throws CodegenException {
        // Get the types's class
        Class<? extends Type> typeClass = type.getClass();
        addString(TYPE_TO_STR.get(typeClass).apply(type));
    }

    // Map of Expression classes to their generator functions
    private static final Map<Class<? extends Expression>, 
            CodegenVoidFunction<Codegen, Expression>> EXP_TO_GEN_FUNC = Map.of(
        BoolLiteralExp.class, Codegen::generateBoolLiteralExp,
        IntLiteralExp.class, Codegen::generateIntLiteralExp,
        NullExp.class, Codegen::generateNullExp,
        FuncCallExp.class, Codegen::generateFuncCallExp,
        ParenExp.class, Codegen::generateParenExp,
        StructAllocExp.class, Codegen::generateStructAllocExp,
        VariableExp.class, Codegen::generateVarExp,
        BinaryOpExp.class, Codegen::generateBinOpExp,
        DotExp.class, Codegen::generateDotExp,
        UnaryOpExp.class, Codegen::generateUnaryOpExp);

    private void generateExpression(final Expression exp) throws CodegenException {
        // Get the expression's class
        Class<? extends Expression> expClass = exp.getClass();

        if (!EXP_TO_GEN_FUNC.containsKey(expClass)) {
            throw new UnsupportedOperationException("Map did not contain mapping function for: " + expClass);
        }

        // Generate the expression
        EXP_TO_GEN_FUNC.get(expClass).apply(this, exp);
    }

    private void generateBoolLiteralExp(final Expression exp) throws CodegenException {
        BoolLiteralExp boolLiteralExp = (BoolLiteralExp) exp;
        if (boolLiteralExp.getValue()) {
            addString("1");
        } else {
            addString("0");
        }
    }

    private void generateIntLiteralExp(final Expression exp) throws CodegenException {
        IntLiteralExp intLiteralExp = (IntLiteralExp) exp;
        addString(intLiteralExp.toString());
    }

    private void generateNullExp(final Expression exp) throws CodegenException {
        addString("NULL");
    }

    private <T> void generateCommaSeparatedExpressions(List<T> list, Function<T,
            Expression> getExpression) throws CodegenException {
        // <EXP_1>, <EXP_2>, ...

        for (int i = 0; i < list.size(); i++) {
            T t = list.get(i);
            generateExpression(getExpression.apply(t));

            if (i != list.size() - 1) {
                addString(", ");
            }
        }
    }

    private <T> void generateNamedFunctionCall(String functionName, List<T> params,
                                               Function<T, Expression> getParamExpression) throws CodegenException {
        // The format will look like:
        // <FUNCTION_NAME>(<EXP_1>, <EXP_2>, ...)

        addString(functionName);

        addString("(");
        generateCommaSeparatedExpressions(params, getParamExpression);
        addString(")");
    }

    private void generateFuncCallExp(final Expression exp) throws CodegenException {
        FuncCallExp funcCallExp = (FuncCallExp) exp;
        String functionName = getOverloadedFunctionNameFromExpressions(funcCallExp.getFuncName().getName(),
                funcCallExp.getCommaExp().getExpressions());

        // Generate a function call with comma separated expression parameters
        generateNamedFunctionCall(
                functionName,
                funcCallExp.getCommaExp().getExpressions(),
                Function.identity());
    }

    private String getOverloadedFunctionNameFromExpressions(String originalFunctionName,
                                                            List<Expression> expressions) throws CodegenException {
        // If this isn't an overloaded function, then return the original name
        if (!overloadedFunctionNameToFunctionDefs.containsKey(originalFunctionName)) {
            return originalFunctionName;
        }

        List<FunctionDef> functionDefs = overloadedFunctionNameToFunctionDefs.get(originalFunctionName);
        for (FunctionDef functionDef : functionDefs) {
            List<Param> params = functionDef.getParams();

            if (params.size() != expressions.size()) {
                continue;
            }

            boolean hasEquality = true;

            for (int i = 0; i < params.size(); i++) {
                Param param = params.get(i);
                Expression expression = expressions.get(i);

                // Check for type equality of all parameters of the function def vs the call expressions
                if (!param.getType().hasTypeEquality(expression.getExpressionType())) {
                    hasEquality = false;
                    break;
                }
            }

            // If all the types match exactly, return the corrected name of this function def
            if (hasEquality) {
                return functionDefToFunctionName.get(functionDef);
            }
        }

        throw new CodegenException("Illegal state: could not find overloaded function signature based on params.");
    }

    private Expression getExpressionWithoutParen(final Expression exp) {
        if (exp instanceof ParenExp parenExp) {
            return getExpressionWithoutParen(parenExp.getExp());
        } else {
            return exp;
        }
    }

    private void generateParenExp(final Expression exp) throws CodegenException {
        ParenExp parenExp = (ParenExp)exp;

        // Struct alloc expression statements need to be generated without parens
        if (getExpressionWithoutParen(exp) instanceof StructAllocExp structAllocExp) {
            generateStructAllocExp(structAllocExp);
        } else {
            addString("(");
            generateExpression(parenExp.getExp());
            addString(")");
        }
    }

    private String getTempStructAllocVariableName(StructType structType) {
        String structName = structType.getStructName().get().getName();
        return "_temp_" + structName + "_struct_alloc_var";
    }

    private Variable getTempStructAllocVariable(StructType structType) {
        return new Variable(getTempStructAllocVariableName(structType));
    }

    private void generateStructAllocExp(final Expression exp) throws CodegenException {
        StructAllocExp structAllocExp = (StructAllocExp) exp;
        StructType structType = structAllocExp.getStructType();
        addComment("Allocating struct " + structAllocExp.getStructType().getStructName().get().getName());

        String tempAllocVariableName = getTempStructAllocVariableName(structType);
        Variable tempAllocVariable = new Variable(tempAllocVariableName);
        if (!structScopeManager.isInScope(tempAllocVariableName)) {
            // Instantiate temporary struct alloc variable if it has not been already
            generateVardecStmt(new VardecStmt(structType, tempAllocVariable, new NullExp()));
        } else {
            // Otherwise, release the temporary variable so we can use it to allocate this new one
            generateAssignStmt(new AssignStmt(tempAllocVariable, new NullExp()));
        }

        // Then allocate the expression, storing it in the temporary variable
        // The temporary variable can then be assigned to another variable in a vardec or assignment stmt
        AssignStmt allocAssignStmt = new AssignStmt(tempAllocVariable, structAllocExp);
        generateStructAllocFunctionCalls(allocAssignStmt);
    }

    private void generateVarExp(final Expression exp) throws CodegenException {
        VariableExp variableExp = (VariableExp)exp;

        generateVariable(variableExp.getVar());
    }

    private void generateBinOpExp(final Expression exp) throws CodegenException {
        BinaryOpExp binOpExp = (BinaryOpExp)exp;

        generateExpression(binOpExp.getLeftExp());
        addString(" " + binOpExp.getOp().getSymbol() + " ");
        generateExpression(binOpExp.getRightExp());
    }

    private void generateDotExp(final Expression exp) throws CodegenException {
        DotExp dotExp = (DotExp) exp;

        // Structs will always be pointers, so we need the `->` operator instead of the `.` operator
        generateExpression(dotExp.getLeftExp());
        addString("->");
        generateVariable(dotExp.getRightVar());
    }

    private void generateUnaryOpExp(final Expression exp) throws CodegenException {
        UnaryOpExp UnaryOpExp = (UnaryOpExp)exp;

        addString(UnaryOpExp.getOp().getSymbol());
        generateExpression(UnaryOpExp.getExp());
    }

    private void generateVariable(final Variable variable) throws CodegenException {
        addString(variable.getName());
    }
}

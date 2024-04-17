package refraff.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import refraff.parser.*;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.parser.function.*;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.statement.*;

public class Codegen {
    private final Program program;
    private final Path generatedCodePath;
    private BufferedWriter writer;
    private int currentIndentCount;

    private Codegen(Program program, File directory) {
        this.program = program;
        this.generatedCodePath = Paths.get(directory.getPath(), "output.c");
//
//        // Get the working directory
//        String currentWorkingDir = System.getProperty("user.dir");
//        // Make a path for the generated file
//        generatedCodePath = Paths.get(
//            currentWorkingDir, "src", "main", "java", "refraff", "codegen", "generatedCode", "output.c");
    }

    public static void generateProgram(Program program, File directory) throws CodegenException {
        // We could directly supply the System.getProperty("user.dir"); in the main function
        // But we should be able to specify where the output is going to go
        new Codegen(program, directory).generateProgram();
    }

    private void generateProgram() throws CodegenException {
        try {
            writer = new BufferedWriter(new FileWriter(generatedCodePath.toString()));
            
            // Add boilerplate
            addImports();

            // Generate struct definitions
            generateStructDefs(program.getStructDefs());

            // Generate function definitions
            // generateFunctionDefs(program.getFunctionDefs());

            // Generate the main function entry point
            addString("int main()");
            addNewLine();
            addString("{");
            addNewLine();

            // Add the program's statements to the entry point
            currentIndentCount += 1;
            generateStatements(program.getStatements());
            currentIndentCount -= 1;

            // Close the main method
            addNewLine();
            addString("}");

            writer.close();
        } catch (IOException e) {
            throw new CodegenException("Error in writing to file: " + e.getMessage());
        } 
    }

    private void addImports() throws CodegenException {
        addString("#include <stdio.h>");
        addNewLine();

        addString("#include <stdlib.h>");
        addNewLine();

        addNewLine();
    }

    // Adds the specified string to the open file
    private void addString(String str) throws CodegenException {
        try {
            writer.write(str);
        } catch (IOException e) {
            throw new CodegenException("Error in writing space");
        }
    }

    // Adds the specified string to the open file
    private void addIndentedString(String str) throws CodegenException {
        try {
            indentLine(currentIndentCount);
            writer.write(str);
        } catch (IOException e) {
            throw new CodegenException("Error in writing space");
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

    private void addSemicolonNewLine() throws CodegenException {
        try {
            writer.write(";\n");
        } catch (IOException e) {
            throw new CodegenException("Error in writing new line");
        }
    }

    // Adds a newline to the open file
    private void addNewLine() throws CodegenException {
        try {
            writer.write("\n");
        } catch (IOException e) {
            throw new CodegenException("Error in writing new line");
        }
    }

    // Indents the specified number of times
    // I don't know if this will be useful yet
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

            alreadyCodeGeneratedStructNames.add(currentStructName);
        }
    }

    private void generateStructDef(StructDef structDef) throws CodegenException {
        /*
         * typedef struct <STRUCT_NAME>
         * {
         *     <PARAM_TYPE_1> <PARAM_NAME>;
         *      .
         *      .
         *      .
         * } <STRUCT_NAME>
         *
         * This format allows for using STRUCT_NAME as a type, rather than needing to use struct <STRUCT_NAME>*
         */

        String structName = structDef.getStructName().getName();

        addString("typedef struct ");
        addString(structName);
        addNewLine();
        addString("{");
        addNewLine();

        currentIndentCount += 1;

        for (Param param : structDef.getParams()) {
            indentLine(currentIndentCount);

            generateType(param.getType());
            addSpace();
            generateVariable(param.getVariable());
            addSemicolonNewLine();
        }

        currentIndentCount -= 1;

        addString("} ");
        addString(structName);
        addSemicolonNewLine();

        addNewLine();
    }

    private String getStructAllocationFunctionName(StructType structType) {
        String structName = structType.getStructName().get().getName();

        // Return a function named: refraff_<STRUCT_NAME>_alloc
        return "refraff_" + structName + "_alloc";
    }

    private void generateStructAllocationFunction(StructDef structDef) throws CodegenException {
        /*
         * Generates a function to allocate new structs on the heap:
         *
         * <STRUCT_NAME>* refraff_<STRUCT_NAME>_alloc(<PARAMS>)
         * {
         *      <STRUCT_NAME>* newStruct = malloc(sizeof(struct <STRUCT_NAME>));
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
        addString("(");

        // Comma separate the parameters into the new function
        for (int i = 0; i < structDef.getParams().size(); i++) {
            Param param = structDef.getParams().get(i);

            generateType(param.getType());
            addSpace();
            generateVariable(param.getVariable());

            if (i != structDef.getParams().size() - 1) {
                addString(", ");
            }
        }

        addString(")");
        addNewLine();

        addString("{");
        addNewLine();

        currentIndentCount += 1;

        final String mallocFormat = "%1$s* newStruct = malloc(sizeof(struct %1$s));";

        indentLine(currentIndentCount);
        addString(String.format(mallocFormat, structDef.getStructName().getName()));
        addNewLine();
        addNewLine();

        final String fieldInitializationFormat = "newStruct->%1$s = %1$s;";

        for (Param param : structDef.getParams()) {
            indentLine(currentIndentCount);
            addString(String.format(fieldInitializationFormat, param.getVariable().getName()));
            addNewLine();
        }

        addNewLine();
        indentLine(currentIndentCount);
        addString("return newStruct;");
        addNewLine();

        currentIndentCount -= 1;

        addString("}");

        addNewLine();
        addNewLine();
    }

    private void generateFunctionDefs(List<FunctionDef> functionDefs) throws CodegenException {
        throw new CodegenException("Function Defs not implemented yet");
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

    private void generateAssignStmt(final Statement stmt) throws CodegenException {
        AssignStmt assignStmt = (AssignStmt) stmt;

        indentLine(currentIndentCount);
        generateVariable(assignStmt.getVariable());
        addString(" = ");
        generateExpression(assignStmt.getExpression());
        addSemicolonNewLine();
    }

    private void generateBreakStmt(final Statement stmt) throws CodegenException {
        BreakStmt breakStmt = (BreakStmt)stmt;

        addIndentedString("break;\n");
    }

    private void generateExpStmt(final Statement stmt) throws CodegenException {
        ExpressionStmt expStmt = (ExpressionStmt)stmt;

        indentLine(currentIndentCount);
        generateExpression(expStmt.getExpression());
        addSemicolonNewLine();
    }

    private void generateIfElseStmt(final Statement stmt) throws CodegenException {
        IfElseStmt ifElseStmt = (IfElseStmt)stmt;

        // Add if and condition and opening brace
        addIndentedString("if (");
        generateExpression(ifElseStmt.getCondition());
        addString(")\n");
        addIndentedString("{\n");
        // Add to the indentation
        currentIndentCount += 1;
        // generate statement(s)
        generateStatements(List.of(ifElseStmt.getIfBody()));
        // Subtract from the indentation
        currentIndentCount -= 1;
        // Add closing brace
        addIndentedString("}\n");

        // Add an else if there is one
        Optional<Statement> optionalElseBody = ifElseStmt.getElseBody();
        if (!optionalElseBody.isEmpty()) {
            addIndentedString("else\n");
            addIndentedString("{\n");
            currentIndentCount += 1;
            generateStatements(List.of(optionalElseBody.get()));
            currentIndentCount -= 1;
            addIndentedString("}\n");
        }
    }

    private void generatePrintlnStmt(final Statement stmt) throws CodegenException {
        PrintlnStmt printlnStmt = (PrintlnStmt)stmt;

        addIndentedString("printf(\"%d\", ");
        generateExpression(printlnStmt.getExpression());
        addString(");\n");
    }

    private void generateReturnStmt(final Statement stmt) throws CodegenException {
        ReturnStmt returnStmt = (ReturnStmt) stmt;

        addString("return");

        if (returnStmt.getReturnValue().isPresent()) {
            addSpace();
            generateExpression(returnStmt.getReturnValue().get());
        }

        addSemicolonNewLine();
    }

    private void generateStmtBlock(final Statement stmt) throws CodegenException {
        StmtBlock stmtBlock = (StmtBlock)stmt;

        indentLine(currentIndentCount);
        addString("{");
        addNewLine();

        currentIndentCount += 1;
        generateStatements(stmtBlock.getBlockBody());
        currentIndentCount -= 1;

        indentLine(currentIndentCount);
        addString("}");
    }

    private void generateVardecStmt(final Statement stmt) throws CodegenException {
        VardecStmt vardecStmt = (VardecStmt)stmt;

        indentLine(currentIndentCount);
        generateType(vardecStmt.getType());
        addSpace();
        generateVariable(vardecStmt.getVariable());
        addString(" = ");
        generateExpression(vardecStmt.getExpression());
        addSemicolonNewLine();
    }

    private void generateWhileStmt(final Statement stmt) throws CodegenException {
        WhileStmt whileStmt = (WhileStmt)stmt;

        addIndentedString("while (");
        generateExpression(whileStmt.getCondition());
        addString(")\n");
        addIndentedString("{\n");
        currentIndentCount += 1;
        // generate statement(s)
        generateStatements(List.of(whileStmt.getBody()));
        // Subtract from the indentation
        currentIndentCount -= 1;
        // Add closing brace
        addIndentedString("}\n");
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

    private void generateFuncCallExp(final Expression exp) throws CodegenException {
        throw new CodegenException("Not implemented yet");
    }

    private void generateParenExp(final Expression exp) throws CodegenException {
        ParenExp parenExp = (ParenExp)exp;

        addString("(");
        generateExpression(parenExp.getExp());
        addString(")");
    }

    private void generateStructAllocExp(final Expression exp) throws CodegenException {
        StructAllocExp structAllocExp = (StructAllocExp) exp;

        // This method is going to make a call to the generated function for struct allocation in C
        // This will make our life easier trying to inline struct allocations

        String allocationFunctionName = getStructAllocationFunctionName(structAllocExp.getStructType());

        // The format will look like:
        // <ALLOCATION_FUNCTION_NAME>(<FIELD_1>, <FIELD_2>, ...)

        addString(allocationFunctionName);
        addString("(");

        List<StructActualParam> structParams = structAllocExp.getParams().params;
        for (int i = 0; i < structParams.size(); i++) {
            StructActualParam param = structParams.get(i);
            generateExpression(param.exp);

            if (i != structParams.size() - 1) {
                addString(", ");
            }
        }

        addString(")");
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

    // I don't know if I need this one? Is this one function too deep, lol
    private void generateVariable(final Variable variable) throws CodegenException {
        addString(variable.getName());
    }
}

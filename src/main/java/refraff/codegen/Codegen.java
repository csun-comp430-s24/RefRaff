package refraff.codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import refraff.parser.*;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.typechecker.Standardized;
import refraff.parser.function.*;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.statement.*;

public class Codegen {
    private final Program program;
    private final Path generatedCodePath;
    private BufferedWriter writer;
    private int currentIndentCount;

    private Codegen(Program program) {
        this.program = program;
        // Get the working directory
        String currentWorkingDir = System.getProperty("user.dir");
        // Make a path for the generated file
        generatedCodePath = Paths.get(
            currentWorkingDir, "src", "main", "java", "refraff", "codegen", "generatedCode", "output.c");
    }

    public static void generateProgram(Program program) throws CodegenException {
        new Codegen(program).generateProgram();
    } 

    private void generateProgram() throws CodegenException {
        try {
            writer = new BufferedWriter(new FileWriter(generatedCodePath.toString()));
            
            // Add boilerplate
            addBeginningBoilerPlate();
            // Generate struct definitions
            // generateStructDefs(program.getStructDefs());
            // Generate function definitions
            // generateFunctionDefs(program.getFunctionDefs());
            // Generate statements
            generateStatements(program.getStatements());
        
            addEndingBoilerPlate();
            writer.close();
        } catch (IOException e) {
            throw new CodegenException("Error in writing to file: " + e.getMessage());
        } 
    }

    private void addBeginningBoilerPlate() throws CodegenException {
        try {   
            writer.write("#include <stdio.h>\n\n"
                         + "int main()\n"
                         + "{\n");
            currentIndentCount += 1;
        } catch (IOException e) {
            throw new CodegenException("Error in writing space");
        }
    }

    private void addEndingBoilerPlate() throws CodegenException {
        try {
            writer.write("}\n");
            currentIndentCount -= 1; // For symmetry
        } catch (IOException e) {
            throw new CodegenException("Error in writing space");
        }
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
        throw new CodegenException("Struct Defs not implemented yet");
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
        throw new CodegenException("Not implemented yet");
    }

    private void generateReturnStmt(final Statement stmt) throws CodegenException {
        throw new CodegenException("Not implemented yet");
    }

    private void generateStmtBlock(final Statement stmt) throws CodegenException {
        StmtBlock stmtBlock = (StmtBlock)stmt;

        generateStatements(stmtBlock.getBlockBody());
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
        StructType.class, (Type type) -> type.toString(),
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
        addString("null");
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
        throw new CodegenException("Not implemented yet");
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
        throw new CodegenException("Not implemented yet");
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

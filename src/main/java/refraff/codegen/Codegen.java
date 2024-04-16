package refraff.codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    private BufferedWriter writer;

    private Codegen(Program program) {
        this.program = program;
    }

    public static void generateProgram(Program program) throws CodegenException {
        new Codegen(program).generateProgram();
    }
    

    private void generateProgram() throws CodegenException {
        try {
            writer = new BufferedWriter(new FileWriter("output.c"));
            
            // Generate struct definitions
            generateStructDefs(program.getStructDefs());
            // Generate function definitions
            generateFunctionDefs(program.getFunctionDefs());
            // Generate statements
            generateStatements(program.getStatements());
        
            writer.close();
        } catch (IOException e) {
            throw new CodegenException("Error in writing to file: " + e.getMessage());
        }
        
    }

    private void generateStructDefs(List<StructDef> structDefs) {
        System.out.println("Not implemented yet");
    }

    private void generateFunctionDefs(List<FunctionDef> functionDefs) {
        System.out.println("Not implemented yet");
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
        System.out.println("Not implemented yet");
        // Find out which statement this is, then direct to that statement's generator
        for (Statement stmt : statements) {
            // Get the statements class
            Class<? extends Statement> stmtClass = stmt.getClass();

            if (!STMT_TO_GEN_FUNC.containsKey(stmtClass)) {
                // Isn't a statement?
                throw new UnsupportedOperationException("Map did not contain mapping function for: " + stmtClass);
            }

            // These functions will throw exceptions if there are type errors
            STMT_TO_GEN_FUNC.get(stmtClass).apply(this, stmt);
        }
    }

    private void generateAssignStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateBreakStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateExpStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateIfElseStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generatePrintlnStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateReturnStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateStmtBlock(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateVardecStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }

    private void generateWhileStmt(final Statement stmt) {
        System.out.println("Not implemented yet");
    }
}

package refraff.codegen;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import refraff.parser.*;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.parser.function.*;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.statement.*;

public class CodegenTest {
    
    // Do we need these helper functions here?
    private IntType getIntType() {
        return Node.setNodeSource(new IntType(), "int");
    }

    private BoolType getBoolType() {
        return Node.setNodeSource(new BoolType(), "bool");
    }

    private VoidType getVoidType() {
        return Node.setNodeSource(new VoidType(), "void");
    }

    private NullExp getNullExp() {
        return Node.setNodeSource(new NullExp(), "null");
    }

    private Variable getVariable(String name) {
        return Node.setNodeSource(new Variable(name), name);
    }

    private StructType getStructType(String name) {
        StructName structName = getStructName(name);
        return Node.setNodeSource(new StructType(structName), name);
    }

    private StructName getStructName(String name) {
        return Node.setNodeSource(new StructName(name), name);
    }

    private FunctionName getFunctionName(String name) {
        return Node.setNodeSource(new FunctionName(name), name);
    }
    
    // Test valid inputs
    private void testGeneratedFileDoesNotThrow(String cSourceFile, String executionFile, String expectedOutput) {
        assertDoesNotThrow(() -> CCodeRunner.runAndCaptureOutput(cSourceFile, executionFile, expectedOutput));
    }

    private void testProgramGeneratesAndDoesNotThrow(Program program, String expectedOutput) {
        assertDoesNotThrow(() -> Codegen.generateProgram(program));
        testGeneratedFileDoesNotThrow("output.c", "output", expectedOutput);
    }

    @Test
    public void testCodeRunnerRunsExampleCFile() {
        // Run the code runner with the example input
        String expectedOutput = "42";
        // assertDoesNotThrow(() -> CCodeRunner.runAndCaptureOutput("example.c", "example", expectedOutput));
        testGeneratedFileDoesNotThrow("example.c", "example", expectedOutput);
    }

    @Test
    public void testCodegenWithVardec() {
        /*
         * int foo = 6;
         */
        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(6));

        Program program = new Program(List.of(), List.of(), List.of(vardecStmt));
        String expectedOutput = "";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithAssignment() {
        /*
         * int foo = 6;
         * foo = 0;
         */
        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(6));

        Statement assignStmt = new AssignStmt(
                getVariable("foo"),
                new IntLiteralExp(0));

        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, assignStmt));
        String expectedOutput = "";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithIfStmt() {
        /*
         * if (true) {
         *   int foo = 6;
         * }
         */

        Statement ifBody = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(6)
        );
        Expression condition = new BoolLiteralExp(true);
        Statement ifStmt = new IfElseStmt(condition, ifBody);
        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        String expectedOutput = "";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithIfElseStmt() {
        /*
         * if (true) {
         *   int foo = 6;
         * } else {
         *   int foo = 0;
         * }
         */

        Statement ifBody = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(6));
        Statement elseBody = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(0));
        Expression condition = new BoolLiteralExp(true);
        Statement ifElseStmt = new IfElseStmt(condition, ifBody, elseBody);
        Program program = new Program(List.of(), List.of(), List.of(ifElseStmt));
        String expectedOutput = "";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithWhileStmt() {
        /*
         * int foo = 3;
         * while (foo > 0) {
         *   foo = foo - 1;
         * }
         */

        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(3));

        Expression guard = new BinaryOpExp(
            new VariableExp(getVariable("foo")),
            OperatorEnum.GREATER_THAN,
            new IntLiteralExp(0));

        Expression assignExp = new BinaryOpExp(
                new VariableExp(getVariable("foo")),
                OperatorEnum.MINUS,
                new IntLiteralExp(1)); 

        Statement body = new AssignStmt(getVariable("foo"), assignExp);

        Statement whileStmt = new WhileStmt(guard, new StmtBlock(List.of(body)));
        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, whileStmt));
        String expectedOutput = "";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithBreakStmt() {
        /*
         * int foo = 4;
         * while (foo > 0) {
         *   foo = foo - 1;
         *   if (foo == 1) {
         *     break;
         *   }
         * }
         */

        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(4));

        Expression guard = new BinaryOpExp(
                new VariableExp(getVariable("foo")),
                OperatorEnum.GREATER_THAN,
                new IntLiteralExp(0));

        Expression assignExp = new BinaryOpExp(
                new VariableExp(getVariable("foo")),
                OperatorEnum.MINUS,
                new IntLiteralExp(1));

        Statement countDown = new AssignStmt(getVariable("foo"), assignExp);

        Expression condition = new BinaryOpExp(
            new VariableExp(getVariable("foo")),
            OperatorEnum.DOUBLE_EQUALS,
            new IntLiteralExp(1));

        Statement breakStmt = new BreakStmt();

        Statement ifStmt = new IfElseStmt(condition, breakStmt);

        Statement whileStmt = new WhileStmt(guard, new StmtBlock(List.of(countDown, ifStmt)));
        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, whileStmt));
        String expectedOutput = "";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }


    // Test invalid inputs
    private void testGeneratedFileThrowsCodegenException(String cSourceFile, String executionFile, String expectedOutput) {
        assertThrows(CodegenException.class,
                () -> CCodeRunner.runAndCaptureOutput(cSourceFile, executionFile, expectedOutput));
    }

    @Test
    public void testCodeRunnerDoesNotMatchExpectedOutputThrows() {
        // example.c will output 42
        String expectedOutput = "24";
        testGeneratedFileThrowsCodegenException("example.c", "example", expectedOutput);
    }

    @Test
    public void testCodeRunnerMissingSemiColonThrows() {
        // example_no_compile.c is missing a semi colon and won't compile
        String expectedOutput = "15";
        testGeneratedFileThrowsCodegenException("example_no_compile.c", "example_no_compile", expectedOutput);
    }

    @Test
    public void testCodeRunnerRunningCodeWithMemoryLeakThrows() {
        // example_leak.c doesn't free malloc-ed stuff
        String expectedOutput = "";
        testGeneratedFileThrowsCodegenException("example_leak.c", "example_leak", expectedOutput);
    }
}

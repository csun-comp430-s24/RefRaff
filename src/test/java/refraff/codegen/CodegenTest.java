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

    @Ignore // Not implemented yet
    @Test
    public void testCodegenWithAssignment() {
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

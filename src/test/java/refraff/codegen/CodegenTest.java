package refraff.codegen;

import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.Ignore;
// import refraff.codegen.CCodeRunner;

public class CodegenTest {
    
    // Test valid inputs
    private void testDoesNotThrow(String cSourceFile, String executionFile, String expectedOutput) {
        assertDoesNotThrow(() -> CCodeRunner.runAndCaptureOutput(cSourceFile, executionFile, expectedOutput));
    }
    
    @Test
    public void testCodeRunnerRunsExampleCFile() {
        // Run the code runner with the example input
        String expectedOutput = "42";
        // assertDoesNotThrow(() -> CCodeRunner.runAndCaptureOutput("example.c", "example", expectedOutput));
        testDoesNotThrow("example.c", "example", expectedOutput);
    }


    // Test invalid inputs
    private void testThrowsCodegenException(String cSourceFile, String executionFile, String expectedOutput) {
        assertThrows(CodegenException.class,
                () -> CCodeRunner.runAndCaptureOutput(cSourceFile, executionFile, expectedOutput));
    }

    @Test
    public void testCodeRunnerDoesNotMatchExpectedOutputThrows() {
        // example.c will output 42
        String expectedOutput = "24";
        testThrowsCodegenException("example.c", "example", expectedOutput);
    }

    @Test
    public void testCodeRunnerMissingSemiColonThrows() {
        // example_no_compile.c is missing a semi colon and won't compile
        String expectedOutput = "15";
        testThrowsCodegenException("example_no_compile.c", "example_no_compile", expectedOutput);
    }

    @Test
    public void testCodeRunnerRunningCodeWithMemoryLeakThrows() {
        // example_leak.c doesn't free malloc-ed stuff
        String expectedOutput = "";
        testThrowsCodegenException("example_leak.c", "example_leak", expectedOutput);
    }
}

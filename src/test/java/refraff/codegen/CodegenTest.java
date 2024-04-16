package refraff.codegen;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CodegenTest {
    
    // Test valid inputs
    

    // Test invalid inputs (What would these even be? We should have caught them by now, right?)

    // Test Code Runner
    @Test
    public void testCodeRunnerRunsExampleCFile() {
        // Run the code runner with the example input
        assertDoesNotThrow(() -> TestCCodeRunner.runAndCaptureOutput());
    }
}

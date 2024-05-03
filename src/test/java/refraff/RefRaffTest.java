package refraff;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import refraff.util.ResourceUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RefRaffTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private File tempDir;

    @TestFactory
    public List<DynamicTest> testCompilesSuccessfully() {
        String[] validInputPrograms = { "program.txt", "program2.txt", "program_function_overloading.txt" };

        return Arrays.stream(validInputPrograms)
                .map(this::testCompilesSuccessfully)
                .collect(Collectors.toList());
    }

    @TestFactory
    public List<DynamicTest> testDoesNotCompile() {
        return List.of(
                testDoesNotCompile("program.docx", "output.c"),
                testDoesNotCompile("program.txt", "output.pdf"),
                testDoesNotCompile("program_that_doesnt_exist.refraff", "output.c")
        );
    }

    private DynamicTest testCompilesSuccessfully(String inputFileName) {
        return testCompiles(inputFileName, true, (file) -> assertTrue(file.exists()));
    }

    private DynamicTest testDoesNotCompile(String inputFileName, String outputFileName) {
        return testCompiles(inputFileName, outputFileName, false, (file) -> assertFalse(file.exists()));
    }

    private DynamicTest testCompiles(String inputFileName, boolean tryToCopy, Consumer<File> outputFileConsumer) {
        return testCompiles(inputFileName, "output.c", tryToCopy, outputFileConsumer);
    }

    private DynamicTest testCompiles(String inputFileName, String outputFileName, boolean tryToCopy,
                                     Consumer<File> outputFileConsumer) {
        return DynamicTest.dynamicTest(inputFileName + " compiles successfully", () -> {
            // Copy the file to the temp dir, then run the program
            if (tryToCopy) {
                ResourceUtil.copyResourceFile(inputFileName, new File(tempDir, inputFileName));
            }
            RefRaff.doGeneration(tempDir, inputFileName, outputFileName);

            // We should have an output file then use the appropriate test consumer
            File outputFile = new File(tempDir, outputFileName);
            outputFileConsumer.accept(outputFile);
        });
    }
}

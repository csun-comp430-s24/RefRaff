package refraff.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceUtil {

    private static final String TEST_DIRECTORY = "src/test/resources";

    private static final String PROGRAM_FILE = "program.txt";
    private static final String PROGRAM2_FILE = "program2.txt";

    public static String readInputFile(String filePath) {
        File file = new File(TEST_DIRECTORY, filePath);
        assertTrue(file.exists(), "Input file " + filePath + " does not exist in " + TEST_DIRECTORY);

        StringBuffer stringBuffer = new StringBuffer();

        assertDoesNotThrow(() -> {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    stringBuffer.append(scanner.nextLine());
                    stringBuffer.append('\n');
                }

                stringBuffer.deleteCharAt(stringBuffer.length() - 1);
            }
        });

        return stringBuffer.toString();
    }

    public static void copyResourceFile(String filePath, File copyDestination) {
        File sourceFile = new File(TEST_DIRECTORY, filePath);
        assertTrue(sourceFile.exists(), "Input file " + filePath + " does not exist in " + TEST_DIRECTORY);

        try {
            Files.copy(sourceFile.toPath(), copyDestination.toPath());
        } catch (IOException ex) {
            fail(ex);
        }
    }

    public static String readProgramInputFile() {
        return readInputFile(PROGRAM_FILE);
    }

    public static String readProgram2InputFile() {
        return readInputFile(PROGRAM2_FILE);
    }
}

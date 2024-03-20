package refraff.util;

import java.io.File;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ResourceUtil {

    private static final String TEST_DIRECTORY = "src/test/resources";

    private static final String PROGRAM_FILE = "program.txt";

    public static String readInputFile(String filePath) {
        File file = new File(TEST_DIRECTORY, filePath);
        assertTrue("Input file " + filePath + " does not exist in " + TEST_DIRECTORY, file.exists());

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

    public static String readProgramInputFile() {
        return readInputFile(PROGRAM_FILE);
    }
}

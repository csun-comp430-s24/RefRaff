package refraff.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.InterruptedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

// This runs the generated code and throws exceptions if the code won't compile, run,
// output the expected output, or has memory leaks
public class CCodeRunner {

    public static void runAndCaptureOutput(File directory, File sourceFile, String... expectedLines) throws CodegenException {
        // Create file paths
        Path sourcePath = sourceFile.toPath();
        Path executable = Paths.get(directory.getPath(), "codegen_output");

        compileCCode(sourcePath, executable);

        // Run code and compare to the lines we expect to be printed
        runExecutable(executable, String.join("\n", expectedLines));
    }

    public static void runWithDrMemoryAndCaptureOutput(File directory, File sourceFile, String... expectedLines) throws CodegenException {
        // Create file paths
        Path executable = Paths.get(directory.getPath(), "codegen_output");
        Path drMemLogDir = Paths.get(directory.getPath(), "drMemoryLog");

        // Run and capture the output of the file, like normally
        runAndCaptureOutput(directory, sourceFile, expectedLines);
        // Run the compiled program with the Dr. Memory command line tool
        runWithDrMemory(directory, executable, drMemLogDir);

        // Check the log created by Dr. Memory to see if there were any memory leaks
        checkLeakReport(drMemLogDir);
    }

    private static String escapePath(Path path) {
        String pathString = path.toString();

        // If there's spaces, escape them; else return just the path
        return pathString.contains(" ") ? "\"" + pathString + "\"" : pathString;
    }

    public static void compileCCode(Path sourceFile, Path executable) throws CodegenException {
        try {
            // Compile generated C code
            Process compile = Runtime.getRuntime().exec("gcc -o " + escapePath(executable) + " " + escapePath(sourceFile));
            compile.waitFor();

            // Capture errors from the compilation process
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine + "\n");
            }

            // Throw error if we couldn't compile
            if (!errorOutput.toString().isEmpty()) {
                throw new CodegenException("Compilation errors:\n" + errorOutput);
            }

        } catch (IOException | InterruptedException e) {
            throw new CodegenException("Could not compile file: " + sourceFile);
        }
    }

    public static void runExecutable(Path executable, String expectedOutput) throws CodegenException {
        try {
            Process run = Runtime.getRuntime().exec(executable.toString());

            // Create threads to handle both input and error streams - when I use the other version with just the
            // output capture, some of the programs won't terminate, and I can't end them. Idk : (
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(run.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(run.getErrorStream()));
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();

            Thread outputThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = outputReader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread errorThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errors.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

            // Use a timeout for waitFor - This is interesting. I can only force a program to close if I use the
            // threads and not the other way of capturing output. So, some program won't close on their own when testing.
            // But if I compile and run the same generated code outside of the tests, it will output the correct information 
            // and terminate. If I run the executable created by the test, it will output the correct output and terminate.
            // So I just have this close the program after 30 seconds. It's annoying. I don't even know if you would have
            // The same issues on your end. You can check by uncommenting the exception and seeing which programs don't close -_- 
            if (!run.waitFor(20, TimeUnit.SECONDS)) {
                run.destroyForcibly();
                outputThread.interrupt();
                errorThread.interrupt();
                // throw new CodegenException("Executable timed out: " + executable);
            }

            // Ensure all output is processed
            outputThread.join();
            errorThread.join();

            // Check for errors in the output
            if (errors.length() > 0) {
                System.out.println("Error Output: " + errors);
            }

            // Compare outputs
            if (!output.toString().trim().equals(expectedOutput.trim())) {
                throw new CodegenException(
                        "Was expecting output `" + expectedOutput + "` but got `" + output + "`");
            }

        } catch (IOException | InterruptedException e) {
            throw new CodegenException("Could not run executable, compiled C file (" + executable + "): " + e.getMessage());
        }
    }

    // public static void runExecutable(Path executable, String expectedOutput) throws CodegenException {
    //     try {
    //         System.out.println("Running program: " + executable.toString());
    //         // Run the compiled C program
    //         Process run = Runtime.getRuntime().exec(executable.toString());
    //         BufferedReader outputReader = new BufferedReader(new InputStreamReader(run.getInputStream()));

    //         String line;
    //         StringBuilder output = new StringBuilder();
    //         while ((line = outputReader.readLine()) != null) {
    //             output.append(line + "\n");
    //         }

    //         // I don't know if we want to do anything about the exit code
    //         int exitCode = run.waitFor();

    //         // Compare outputs, throw exception if they don't match
    //         if (!output.toString().trim().equals(expectedOutput.trim())) {
    //             throw new CodegenException("Was expecting output `" + expectedOutput + "` but got `" + output.toString() + "`");
    //         }

    //     } catch (IOException | InterruptedException e) {
    //         throw new CodegenException("Could not run executable: " + executable);
    //     }
    // }

    public static void runWithDrMemory(File directory, Path executable, Path drMemLogDir)
            throws CodegenException {
        try {
            // Make sure log file exists - I think we can get rid of this?
            File logDir = new File(drMemLogDir.toString());
            logDir.mkdirs();

            String drMemoryCommand = System.getProperty("os.name").startsWith("Windows") ? "drmemory.exe" : "drmemory";

            // Run the program with Dr. Memory
            ProcessBuilder runBuilder = new ProcessBuilder(
                    drMemoryCommand, "-logdir", drMemLogDir.toString(), "-batch", "--", executable.toString());
            runBuilder.directory(directory);

            // Redirect Dr. Memory output to files (Dr. Memory dynamically names directories, and it's
            // easier to get at the files if we name them)
            File outputFile = new File(drMemLogDir.toString(), "drmemory_output.txt");
            File memoryReportFile = new File(drMemLogDir.toString(), "drmemory_report.txt");

            runBuilder.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
            runBuilder.redirectError(ProcessBuilder.Redirect.to(memoryReportFile));

            // Run command, wait for process to finish
            System.out.println("Running generated code with Dr. Memory. This may take a minute.");
            Process run = runBuilder.start();
            run.waitFor();

        } catch (IOException | InterruptedException e) {
            throw new CodegenException("Could not run executable with Dr Memory: " + e.getMessage());
        }

    }

    private static void checkLeakReport(Path drMemLogDir) throws CodegenException {
        File memoryReportFile = new File(drMemLogDir.toString(), "drmemory_report.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(memoryReportFile))) {
            // Go through the report and find the lines with the leak information
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("byte(s) of leak(s)") || line.contains("byte(s) of possible leak(s)")) {
                    int numLeaks = getNumLeaks(line);
                    // If the number of leaks is not 0, throw exception
                    if (numLeaks > 0) {
                        throw new CodegenMemoryLeakException("Generated code contains memory leaks:\n" + line);
                    }
                }
            }
        } catch (IOException e) {
            throw new CodegenException("Could not read memory report");
        }
    }

    // Returns the unique number of leaks from the Dr. Memory report
    private static int getNumLeaks(String leakLine) throws CodegenException {
        // Split string by spaces
        String[] parts = leakLine.split("\\s+");

        // Find the index of "~~Dr.M~~" and get the next element
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("~~Dr.M~~")) {
                // The next string contains the unique number of leaks
                if (i + 1 < parts.length) {
                    return Integer.parseInt(parts[i + 1]);
                }
            }
        }
        // If we got this far, something's wrong
        throw new CodegenException("Could not parse Dr. Memory report, line: " + leakLine);
    }
}

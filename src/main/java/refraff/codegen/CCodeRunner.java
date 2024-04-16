package refraff.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.InterruptedException;
import java.nio.file.Path;
import java.nio.file.Paths;

// This runs the generated code and throws exceptions if the code won't compile, run,
// output the expected output, or has memory leaks
public class CCodeRunner {

    public static void runAndCaptureOutput(String cSourceFile, String cExecutableFile, String expectedOutput) throws CodegenException {
        // Get the current working directory
        String currentWorkingDir = System.getProperty("user.dir");

        // Create file paths
        Path sourceFile = Paths.get(
            currentWorkingDir, "src", "main", "java", "refraff", "codegen", "generatedCode", cSourceFile);
        Path executable = Paths.get(
            currentWorkingDir, "src", "main", "java", "refraff", "codegen", "generatedCode", cExecutableFile);
        Path drMemLogDir = Paths.get(
            currentWorkingDir, "src", "main", "java", "refraff", "codegen", "drMemoryLog");

        compileCCode(sourceFile, executable);

        // Run code and compare to expected
        runExecutable(executable, expectedOutput);

        // Run the compiled program with the Dr. Memory command line tool
        runWithDrMemory(currentWorkingDir, executable, drMemLogDir);

        // Check the log created by Dr. Memory to see if there were any memory leaks
        checkLeakReport(drMemLogDir);
    }

    public static void compileCCode(Path sourceFile, Path executable) throws CodegenException {
        try {
            // Compile generated C code (The extra quotations are there to handle directory
            // names with spaces in them)
            Process compile = Runtime.getRuntime()
                    .exec("gcc -o " + "\"" + executable.toString() + "\" \"" + sourceFile + "\"");
            compile.waitFor();

            // Capture errors from the compilation process
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine + "\n");
            }
            // Throw error if we couln't compile
            if (!errorOutput.toString().isEmpty()) {
                throw new CodegenException("Compilation errors:\n" + errorOutput.toString());
            }

        } catch (IOException | InterruptedException e) {
            throw new CodegenException("Could not compile file: " + sourceFile);
        }
    }

    public static void runExecutable(Path executable, String expectedOutput) throws CodegenException {
        try {
            // Run the compiled C program
            Process run = Runtime.getRuntime().exec(executable.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));

            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

            // I don't know if we want to do anything about the exit code
            int exitCode = run.waitFor();

            // Compare outputs, throw exception if they don't match
            if (!output.toString().trim().equals(expectedOutput.trim())) {
                throw new CodegenException("Was expecting output `" + expectedOutput + "` but got `" + output.toString() + "`");
            }

        } catch (IOException | InterruptedException e) {
            throw new CodegenException("Could not run executable: " + executable);
        }

    }

    public static void runWithDrMemory(String currentWorkingDir, Path executable, Path drMemLogDir)
            throws CodegenException {
        try {
            // Make sure log file exists
            File logDir = new File(drMemLogDir.toString());
            logDir.mkdirs();

            // Clear logs from any previous runs
            clearDirectory(logDir);

            // Run the program with Dr. Memory
            // ProcessBuilder runBuilder = new ProcessBuilder(
            // "cmd", "/c", "drmemory.exe", "-logdir", drMemLogDir.toString(), "-batch",
            // "--", executable.toString(), "&", "exit");
            ProcessBuilder runBuilder = new ProcessBuilder(
                    "drmemory.exe", "-logdir", drMemLogDir.toString(), "-batch", "--", executable.toString());
            runBuilder.directory(new File(currentWorkingDir.toString()));

            // Redirect Dr. Memory output to files (Dr. Memory creates files with)
            File outputFile = new File(drMemLogDir.toString(), "drmemory_output.txt");
            File memoryReportFile = new File(drMemLogDir.toString(), "drmemory_report.txt");

            runBuilder.redirectOutput(ProcessBuilder.Redirect.to(outputFile));
            runBuilder.redirectError(ProcessBuilder.Redirect.to(memoryReportFile));

            // Run command, wait for process to finish
            System.out.println("Running generated code with Dr. Memory. This may take a minute.");
            Process run = runBuilder.start();
            run.waitFor();

        } catch (IOException | InterruptedException e) {
            throw new CodegenException("Could not run executable: " + executable);
        }

    }

    private static void checkLeakReport(Path drMemLogDir) throws CodegenException {
        File memoryReportFile = new File(drMemLogDir.toString(), "drmemory_report.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(memoryReportFile))) {
            // Go through the report and find the lines with the leak information
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("byte(s) of leak(s)")) {
                    System.out.println("Leak info: " + line.trim());
                    // Check if the number is not 1
                }
                if (line.contains("byte(s) of possible leak(s)")) {
                    System.out.println("Possible leak info: " + line.trim());
                    // Check if the number is not 1
                }
            }
        } catch (IOException e) {
            throw new CodegenException("Could not read memory report");
        }
    }

    // Clear the log directory before running the newly generated file
    private static void clearDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            // Necessary because some JVMs return null for empty dirs
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
    }

    private static void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        // Attempt to delete the file or directory
        if (!dir.delete()) {
            throw new IOException("Failed to delete " + dir);
        }
    }
}

package refraff.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.InterruptedException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestCCodeRunner {
    // public static void runAndCaptureOutput(String pathToFile, String executableMaybe, String expectedOutput) {
    
    public static void runAndCaptureOutput() throws CodegenException {
        // try {
            // Get and print the current working directory
            String currentWorkingDir = System.getProperty("user.dir");
            System.out.println("Current working directory: " + currentWorkingDir);

            // Adding the extra double quotes is for directory names that might have spaces in them
            // String sourceFile = "\"" + currentWorkingDir + "\\src\\test\\java\\refraff\\codegen\\testOutput\\example.c" + "\"";
            // String executable = "\"" + currentWorkingDir + "\\src\\test\\java\\refraff\\codegen\\testOutput\\example" + "\"";
            // String drMemLogDir = "\"" + currentWorkingDir + "\\src\\test\\java\\refraff\\codegen\\testDrMemoryLog" + "\"";
            Path sourceFile = Paths.get(currentWorkingDir, "src", "test", "java", "refraff", "codegen", "testOutput", "example.c");
            Path executable = Paths.get(currentWorkingDir, "src", "test", "java", "refraff", "codegen", "testOutput", "example");
            Path drMemLogDir = Paths.get(currentWorkingDir, "src", "test", "java", "refraff", "codegen", "testOutput", "testDrMemoryLog");

            compileCCode(sourceFile, executable);

            // Run code and compare to expected
            runExecutable(executable);

            // Run the compiled program with the Dr. Memory command line tool
            runWithDrMemory(currentWorkingDir, executable, drMemLogDir);

            // Check the log created by Dr. Memory to see if there were any memory leaks
            checkLeakReport(drMemLogDir);

            // File logDir = new File(drMemLogDir);
            // logDir.mkdirs();

            // ProcessBuilder runBuilder = new ProcessBuilder(
            //     "drmemory.exe", "-logdir", drMemLogDir, "-batch", "--", executable
            // );

            // runBuilder.directory(new File(currentWorkingDir));
            // Process run = runBuilder.start();
            // run.waitFor(); // Wait for process to finish

            // // Assuming results.txt is the output file from Dr. Memory
            // File resultFile = new File(logDir, "results.txt");
            // if (resultFile.exists()) {
            //     BufferedReader br = new BufferedReader(new FileReader(resultFile));
            //     String line;
            //     while ((line = br.readLine()) != null) {
            //         System.out.println(line);
            //     }
            //     br.close();
            // } else {
            //     System.out.println("No results file found.");
            // }

        // } catch (IOException | InterruptedException e) {
        //     e.printStackTrace();
        //     throw new CodegenException("Could not run generated C file");
        // }
    }

    public static void compileCCode(Path sourceFile, Path executable) throws CodegenException {
        try {
            // Compile generated C code (The extra quotations are there to handle directory names with spaces in them)
            Process compile = Runtime.getRuntime().exec("gcc -o " + "\"" + executable.toString() + "\" \"" + sourceFile + "\"");
            compile.waitFor();

            // Capture errors from the compilation process
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(compile.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine + "\n");
            }
            if (!errorOutput.toString().isEmpty()) {
                throw new CodegenException("Compilation errors:\n" + errorOutput.toString());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new CodegenException("Could not compile file: " + sourceFile);
        }
    }

    public static void runExecutable(Path executable) throws CodegenException {
        try {
            // Run the compiled C program
            Process run = Runtime.getRuntime().exec(executable.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(run.getInputStream()));

            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

            int exitCode = run.waitFor();
            System.out.println("Output of C program:\n" + output.toString());
            System.out.println("Exit code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
            //         "cmd", "/c", "drmemory.exe", "-logdir", drMemLogDir.toString(), "-batch", "--", executable.toString(), "&", "exit");
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
            e.printStackTrace();
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

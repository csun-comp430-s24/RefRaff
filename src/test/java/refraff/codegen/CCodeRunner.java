package refraff.codegen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.InterruptedException;

public class CCodeRunner {
    // public static void runAndCaptureOutput(String pathToFile, String executableMaybe, String expectedOutput) {
    
    public static void runAndCaptureOutput() throws CodegenException {
        try {
            // Get and print the current working directory
            String currentWorkingDir = System.getProperty("user.dir");
            System.out.println("Current working directory: " + currentWorkingDir);

            String sourceFile = "./testOutput/example.c";
            String executable = "./testOutput/example";

            // Compile generated C code
            Process compile = Runtime.getRuntime().exec("gcc -o " + executable + " " + sourceFile);
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

            // Run the compiled C program
            Process run = Runtime.getRuntime().exec(executable);
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
            throw new CodegenException("Could not run generated C file");
        }
    }
}

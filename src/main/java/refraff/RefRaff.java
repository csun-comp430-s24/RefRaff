package refraff;

import refraff.codegen.Codegen;
import refraff.codegen.CodegenException;
import refraff.parser.Parser;
import refraff.parser.ParserException;
import refraff.parser.Program;
import refraff.tokenizer.Token;
import refraff.tokenizer.Tokenizer;
import refraff.tokenizer.TokenizerException;
import refraff.typechecker.Typechecker;
import refraff.typechecker.TypecheckerException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class RefRaff {

    public static void main(String[] args) {
        // Parse input/output args
        if (args.length != 2) {
            printUsage("too many/too few args");
            return;
        }

        File currentWorkingDirectory = new File(System.getProperty("user.dir"));
        String inputFileName = args[0];
        String outputFileName = args[1];

        doGeneration(currentWorkingDirectory, inputFileName, outputFileName);
    }

    public static void doGeneration(File currentWorkingDirectory, String inputFileName, String outputFileName) {

        if (!inputFileName.endsWith(".txt") && !inputFileName.endsWith(".refraff")) {
            printUsage("on input file extension");
            return;
        }

        if (!outputFileName.endsWith(".c")) {
            printUsage("on output file extension");
            return;
        }

        File inputFile = new File(currentWorkingDirectory, inputFileName);
        if (!inputFile.exists()) {
            printUsage("on non-existent input file");
            return;
        }

        String input = readInput(inputFile);
        if (input == null) {
            return;
        }

        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
            Codegen.generateProgram(program, currentWorkingDirectory, outputFileName);

            System.out.printf("Wrote output to %s!%n", outputFileName);
        } catch (TokenizerException | ParserException | TypecheckerException | CodegenException ex) {
            System.out.println("RefRaff error:");
            System.out.println(ex.getMessage());
        }
    }

    private static String readInput(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine());
                stringBuilder.append('\n');
            }
        } catch (IOException ex) {
            System.out.println("Error reading input file, aborting...");
            ex.printStackTrace();

            return null;
        }

        return stringBuilder.toString();
    }

    private static void printUsage(String specificError) {
        System.out.printf("Invalid usage %s, expected 2 conforming to:%n", specificError);
        System.out.println("\t[ <input_name>.txt | <input_name>.refraff ] <output_name>.c");
    }

}

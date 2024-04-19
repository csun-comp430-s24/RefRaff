package refraff.codegen;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import refraff.Sourced;
import refraff.parser.*;
import refraff.parser.struct.*;
import refraff.parser.type.*;
import refraff.parser.function.*;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.statement.*;
import refraff.tokenizer.Token;
import refraff.tokenizer.Tokenizer;
import refraff.tokenizer.TokenizerException;
import refraff.typechecker.Typechecker;
import refraff.typechecker.TypecheckerException;
import refraff.util.ResourceUtil;

public class CodegenTest {
    
    // Do we need these helper functions here?
    private IntType getIntType() {
        return Node.setNodeSource(new IntType(), "int");
    }

    private BoolType getBoolType() {
        return Node.setNodeSource(new BoolType(), "bool");
    }

    private VoidType getVoidType() {
        return Node.setNodeSource(new VoidType(), "void");
    }

    private NullExp getNullExp() {
        return Node.setNodeSource(new NullExp(), "null");
    }

    private Variable getVariable(String name) {
        return Node.setNodeSource(new Variable(name), name);
    }

    private StructType getStructType(String name) {
        StructName structName = getStructName(name);
        return Node.setNodeSource(new StructType(structName), name);
    }

    private StructName getStructName(String name) {
        return Node.setNodeSource(new StructName(name), name);
    }

    private FunctionName getFunctionName(String name) {
        return Node.setNodeSource(new FunctionName(name), name);
    }

    private File copyCodeGenResourceFile(File toCopyDirectory, String resourceFileName) {
        File copyDestination = new File(toCopyDirectory, resourceFileName);
        ResourceUtil.copyResourceFile("codegen/" + resourceFileName, copyDestination);

        return copyDestination;
    }
    
    // Test valid inputs
    private void testGeneratedFileDoesNotThrow(String resourceFile, String... expectedLines) {
        File sourceFile = new File(tempDirectory, resourceFile);
        assertDoesNotThrow(() -> CCodeRunner.runAndCaptureOutput(tempDirectory, sourceFile, expectedLines));
    }

    private void testGeneratedFileDoesNotThrowOrLeakMemory(String resourceFile, String... expectedLines) {
        File sourceFile = new File(tempDirectory, resourceFile);
        assertDoesNotThrow(() -> CCodeRunner.runWithDrMemoryAndCaptureOutput(tempDirectory, sourceFile, expectedLines));
    }

    private void testProgramGeneratesAndDoesNotThrow(Program program, String... expectedLines) {
        assertDoesNotThrow(() -> Codegen.generateProgram(program, tempDirectory));
        testGeneratedFileDoesNotThrow("output.c", expectedLines);
    }

    private void testProgramGeneratesAndDoesNotThrowOrLeak(Program program, String... expectedLines) {
        assertDoesNotThrow(() -> Codegen.generateProgram(program, tempDirectory));
        testGeneratedFileDoesNotThrowOrLeakMemory("output.c", expectedLines);
    }

    // A temporary directory that is created for each individual test
    // CleanupMode.ON_SUCCESS will leave the directory open, so you can inspect the temporary directory for debugging.
    // The CleanupMode can be changed for debugging purposes: https://junit.org/junit5/docs/5.9.1/api/org.junit.jupiter.api/org/junit/jupiter/api/io/CleanupMode.html
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    File tempDirectory;

    @Test
    public void testCodeRunnerRunsExampleCFile() {
        // Run the code runner with the example input
        String expectedOutput = "42";

        // assertDoesNotThrow(() -> CCodeRunner.runAndCaptureOutput("example.c", "example", expectedOutput));
        copyCodeGenResourceFile(tempDirectory, "example.c");
        testGeneratedFileDoesNotThrow("example.c", expectedOutput);
    }

    @Test
    public void testCodegenWithRecursiveStructDef() {
        /*
         * struct A {
         *   A a;
         * }
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));

        Program program = new Program(List.of(structDef), List.of(), List.of());
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithDependentInOrderStructDef() {
        /*
         * struct A {}
         * struct B {
         *     A a;
         * }
         */

        StructDef structDefA = new StructDef(getStructName("A"), List.of());
        StructDef structDefB = new StructDef(getStructName("B"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));

        Program program = new Program(List.of(structDefA, structDefB), List.of(), List.of());
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithDependentOutOfOrderStructDef() {
        /*
         * struct B {
         *     A a;
         * }
         *
         * struct A {}
         */

        StructDef structDefB = new StructDef(getStructName("B"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));
        StructDef structDefA = new StructDef(getStructName("A"), List.of());

        Program program = new Program(List.of(structDefB, structDefA), List.of(), List.of());
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithFunctionDefCallAndPrint1() {
        /*
         *  func alwaysTrue(): bool {
         *       return true;
         *  }
         *
         *  println(alwaysTrue());
         */

        Expression boolTrue = new BoolLiteralExp(true);
        Statement returnStmtTrue = new ReturnStmt(boolTrue);
        StmtBlock funcBody = new StmtBlock(List.of(returnStmtTrue));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("alwaysTrue"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody
        );

        // Unfortunately, we'll need to manually set the expression type when printing - since we're not doing so through the typechecker
        Expression alwaysTrueCall = new FuncCallExp(getFunctionName("alwaysTrue"), new CommaExp(List.of()));
        alwaysTrueCall.setExpressionType(getBoolType());

        Statement println = new PrintlnStmt(alwaysTrueCall);

        Program program = new Program(List.of(), List.of(funcDef), List.of(println));
        testProgramGeneratesAndDoesNotThrow(program, "true");
    }

    @Test
    public void testCodegenWithFunctionDefCallAndPrint2() {
        /*
         *  func sum(int a, int b): int {
         *       return a + b;
         *  }
         *
         *  println(sum(3, 2));
         */

        Expression add = new BinaryOpExp(new VariableExp(getVariable("a")), OperatorEnum.PLUS, new VariableExp(getVariable("b")));
        add.setExpressionType(getIntType());

        Statement returnStmt = new ReturnStmt(add);

        StmtBlock funcBody = new StmtBlock(List.of(returnStmt));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("sum"),
                List.of(new Param(getIntType(), getVariable("a")), new Param(getIntType(), getVariable("b"))),
                getIntType(),
                funcBody
        );

        // Unfortunately, we'll need to manually set the expression type when printing - since we're not doing so through the typechecker
        Expression sumCall = new FuncCallExp(getFunctionName("sum"),
                new CommaExp(List.of(new IntLiteralExp(3), new IntLiteralExp(2))));
        sumCall.setExpressionType(getIntType());

        Statement println = new PrintlnStmt(sumCall);

        Program program = new Program(List.of(), List.of(funcDef), List.of(println));
        testProgramGeneratesAndDoesNotThrow(program, "5");
    }

    @Test
    public void testCodegenWithStructAllocExp() {
        /*
         * struct B {
         *     A a;
         * }
         *
         * struct A {}
         *
         * B b = new B { a: null };
         */

        StructDef structDefB = new StructDef(getStructName("B"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));
        StructDef structDefA = new StructDef(getStructName("A"), List.of());

        Expression allocExp = new StructAllocExp(getStructType("B"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement allocStatement = new VardecStmt(getStructType("B"), getVariable("b"), allocExp);

        Program program = new Program(List.of(structDefB, structDefA), List.of(), List.of(allocStatement));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenWithExpressionStmt() {
        /*
         * 3;
         */
        Statement expressionStatement = new ExpressionStmt(new IntLiteralExp(3));

        Program program = new Program(List.of(), List.of(), List.of(expressionStatement));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithStmtBlock() {
        /*
         * {
         *   3;
         *   7;
         * }
         */
        Statement expressionStatement1 = new ExpressionStmt(new IntLiteralExp(3));
        Statement expressionStatement2 = new ExpressionStmt(new IntLiteralExp(7));

        StmtBlock stmtBlock = new StmtBlock(List.of(expressionStatement1, expressionStatement2));

        Program program = new Program(List.of(), List.of(), List.of(stmtBlock));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithVardec() {
        /*
         * int foo = 6;
         */
        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(6));

        Program program = new Program(List.of(), List.of(), List.of(vardecStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithAssignment() {
        /*
         * int foo = 6;
         * foo = 0;
         */
        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(6));

        Statement assignStmt = new AssignStmt(
                getVariable("foo"),
                new IntLiteralExp(0));

        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, assignStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithIfStmt() {
        /*
         * if (true) {
         *   int foo = 6;
         * }
         */

        Statement ifBody = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(6)
        );
        Expression condition = new BoolLiteralExp(true);
        Statement ifStmt = new IfElseStmt(condition, ifBody);
        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithIfElseStmt() {
        /*
         * if (true) {
         *   int foo = 6;
         * } else {
         *   int foo = 0;
         * }
         */

        Statement ifBody = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(6));
        Statement elseBody = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(0));
        Expression condition = new BoolLiteralExp(true);
        Statement ifElseStmt = new IfElseStmt(condition, ifBody, elseBody);
        Program program = new Program(List.of(), List.of(), List.of(ifElseStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithWhileStmt() {
        /*
         * int foo = 3;
         * while (foo > 0) {
         *   foo = foo - 1;
         * }
         */

        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(3));

        Expression guard = new BinaryOpExp(
            new VariableExp(getVariable("foo")),
            OperatorEnum.GREATER_THAN,
            new IntLiteralExp(0));

        Expression assignExp = new BinaryOpExp(
                new VariableExp(getVariable("foo")),
                OperatorEnum.MINUS,
                new IntLiteralExp(1)); 

        Statement body = new AssignStmt(getVariable("foo"), assignExp);

        Statement whileStmt = new WhileStmt(guard, new StmtBlock(List.of(body)));
        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, whileStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithBreakStmt() {
        /*
         * int foo = 4;
         * while (foo > 0) {
         *   foo = foo - 1;
         *   if (foo == 1) {
         *     break;
         *   }
         * }
         */

        Statement vardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(4));

        Expression guard = new BinaryOpExp(
                new VariableExp(getVariable("foo")),
                OperatorEnum.GREATER_THAN,
                new IntLiteralExp(0));

        Expression assignExp = new BinaryOpExp(
                new VariableExp(getVariable("foo")),
                OperatorEnum.MINUS,
                new IntLiteralExp(1));

        Statement countDown = new AssignStmt(getVariable("foo"), assignExp);

        Expression condition = new BinaryOpExp(
            new VariableExp(getVariable("foo")),
            OperatorEnum.DOUBLE_EQUALS,
            new IntLiteralExp(1));

        Statement breakStmt = new BreakStmt();

        Statement ifStmt = new IfElseStmt(condition, breakStmt);

        Statement whileStmt = new WhileStmt(guard, new StmtBlock(List.of(countDown, ifStmt)));
        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, whileStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithPrintln() {
        /*
         * println(6);
         */
        Statement printLnStmt = new PrintlnStmt(new IntLiteralExp(6));

        Program program = new Program(List.of(), List.of(), List.of(printLnStmt));
        String expectedOutput = "6";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithMathmaticalExp() {
        /*
         * println(4 + 3 * 7 / 4 - 1 + 9);
         */

        Expression divideExp = new BinaryOpExp(
            new IntLiteralExp(7),
            OperatorEnum.DIVISION,
            new IntLiteralExp(4)
        );

        Expression multExp = new BinaryOpExp(
            new IntLiteralExp(3),
            OperatorEnum.MULTIPLY,
            divideExp
        );

        Expression addExp = new BinaryOpExp(
            new IntLiteralExp(1),
            OperatorEnum.PLUS,
            new IntLiteralExp(9)
        );

        Expression minusExp = new BinaryOpExp(
            multExp,
            OperatorEnum.MINUS,
            addExp
        );
        
        Expression mathExp = new BinaryOpExp(
            new IntLiteralExp(4),
            OperatorEnum.PLUS,
            minusExp
        );

        // Unfortunately, we'll need to manually set the expression type when printing - since we're not doing so through the typechecker
        mathExp.setExpressionType(getIntType());

        Statement printLnStmt = new PrintlnStmt(mathExp);

        Program program = new Program(List.of(), List.of(), List.of(printLnStmt));
        String expectedOutput = "17";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenDotExp() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * A a = null;
         * a->a;
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));

        Statement vardecA = new VardecStmt(getStructType("A"), getVariable("a"), getNullExp());

        Expression dotExp = new DotExp(new VariableExp(getVariable("a")), getVariable("a"));
        Statement dotExpStatement = new ExpressionStmt(dotExp);

        Program program = new Program(List.of(structDef), List.of(), List.of(vardecA, dotExpStatement));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    @Test
    public void testCodegenWithOrExpCondition() {
        /*
         * This should print because `6 || 0` should evaluate to 1
         *
         * if (6 || 0) {
         *   println(3);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(3));
        Expression condition = new BinaryOpExp(
            new IntLiteralExp(6),
            OperatorEnum.OR,
            new IntLiteralExp(0)
        );
        Statement ifStmt = new IfElseStmt(condition, ifBody);
        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        String expectedOutput = "3";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithAndExpCondition() {
        /*
         * This should not print because `6 && 0` should evaluate to 0
         *
         * if (6 && 0) {
         *   println(3);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(3));
        Expression condition = new BinaryOpExp(
                new IntLiteralExp(6),
                OperatorEnum.AND,
                new IntLiteralExp(0));
        Statement ifStmt = new IfElseStmt(condition, ifBody);
        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        testProgramGeneratesAndDoesNotThrow(program);
    }

    // Test invalid inputs
    private void testGeneratedFileThrowsCodegenException(String cSourceFile, String... expectedLines) {
        File sourceFile = new File(tempDirectory, cSourceFile);
        assertThrows(CodegenException.class,
                () -> CCodeRunner.runAndCaptureOutput(tempDirectory, sourceFile, expectedLines));
    }

    private void testGeneratedFileThrowsCodegenExceptionForMemoryLeak(String cSourceFile, String... expectedLines) {
        File sourceFile = new File(tempDirectory, cSourceFile);
        assertThrows(CodegenMemoryLeakException.class,
                () -> CCodeRunner.runWithDrMemoryAndCaptureOutput(tempDirectory, sourceFile, expectedLines));
    }

    @Test
    public void testCodeRunnerDoesNotMatchExpectedOutputThrows() {
        // example.c will output 42
        String expectedOutput = "24";

        copyCodeGenResourceFile(tempDirectory, "example.c");
        testGeneratedFileThrowsCodegenException("example.c", expectedOutput);
    }

    @Test
    public void testCodeRunnerMissingSemiColonThrows() {
        // example_no_compile.c is missing a semi colon and won't compile
        String expectedOutput = "15";

        copyCodeGenResourceFile(tempDirectory, "example_no_compile.c");
        testGeneratedFileThrowsCodegenException("example_no_compile.c", expectedOutput);
    }

    @Test
    public void testCodeRunnerRunningCodeWithMemoryLeakThrows() {
        // example_leak.c doesn't free malloc-ed stuff
        copyCodeGenResourceFile(tempDirectory, "example_leak.c");
        testGeneratedFileThrowsCodegenExceptionForMemoryLeak("example_leak.c");
    }

    // Integration test

    @Test
    public void testCodegenRefraffProgramWithoutException() {
        String input = ResourceUtil.readProgramInputFile();
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
            testProgramGeneratesAndDoesNotThrow(program, "3");
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

    @Test
    public void testCodegenRefraffProgram2WithoutException() {
        String input = ResourceUtil.readProgram2InputFile();
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
            testProgramGeneratesAndDoesNotThrow(program, "3", "false");
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

    // Needs integration testing with leaks (for when we start working on reference counted memory management)

}

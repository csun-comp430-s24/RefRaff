package refraff.codegen;

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

import static org.junit.jupiter.api.Assertions.*;

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
    private File tempDirectory;

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
    public void testCodegenWithAStructReassignment() {
        /*
         * struct B {
         *   A a;
         * }
         *
         * struct A {}
         *
         * B b = new B { a: null };
         * B b2 = b;
         */

        StructDef structDefB = new StructDef(getStructName("B"), List.of(
                new Param(getStructType("A"), getVariable("a"))));
        StructDef structDefA = new StructDef(getStructName("A"), List.of());

        Expression allocExp = new StructAllocExp(getStructType("B"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement allocStatement = new VardecStmt(getStructType("B"), getVariable("b"), allocExp);

        Expression assignExp = new VariableExp(getVariable("b"));
        Statement reassignStmt = new VardecStmt(getStructType("B"), getVariable("b2"), assignExp);

        Program program = new Program(List.of(structDefB, structDefA), List.of(), List.of(allocStatement, reassignStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenWithASameAndDifferentStructFields() {
        /*
         * This should print 2
         *
         * struct B {
         *   int num;
         *   B b;
         *   A a;
         * }
         *
         * struct A {
         *   A a;
         * }
         *
         * B b = new B { 
         *   num: 1,
         *   b: new B {
         *     num: 2,
         *     b: null,
         *     a: null
         *   }
         *   a: new A {
         *     a: null
         *   }
         * };
         * 
         * B innerB = b.b;
         * 
         * println(innerB.num);
         */

        
        StructDef structDefB = new StructDef(getStructName("B"), List.of(
                new Param(new IntType(), getVariable("num")),
                new Param(getStructType("B"), getVariable("b")),
                new Param(getStructType("A"), getVariable("a"))));
        
        StructDef structDefA = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression allocExpA = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression allocExpB2 = new StructAllocExp(getStructType("B"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(2)),
                    new StructActualParam(getVariable("b"), getNullExp()),
                    new StructActualParam(getVariable("a"), getNullExp()))));
        Expression allocExpB = new StructAllocExp(getStructType("B"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                    new StructActualParam(getVariable("b"), allocExpB2),
                    new StructActualParam(getVariable("a"), allocExpA))));
        Statement vardecStmtB = new VardecStmt(getStructType("B"), getVariable("b"), allocExpB);

        Expression dotExp = new DotExp(new VariableExp(getVariable("b")), getVariable("b"));
        Statement vardecStmtInnerB = new VardecStmt(getStructType("B"), getVariable("innerB"), dotExp);

        Expression innerDotExp = new DotExp(new VariableExp(getVariable("innerB")), getVariable("num"));
        innerDotExp.setExpressionType(getIntType());
        PrintlnStmt printlnStmt = new PrintlnStmt(innerDotExp);

        Program program = new Program(List.of(structDefB, structDefA), List.of(),
                List.of(vardecStmtB, vardecStmtInnerB, printlnStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program, "2");
    }

    @Test
    public void testCodegenAbandoningVardecVariableDoesntFreeFromOtherReference() {
        /*
         * This should print 3
         *
         * struct B {
         *   int num;
         *   B b;
         *   A a;
         * }
         *
         * struct A {
         *   A a;
         * }
         *
         * B b = new B { 
         *   num: 1,
         *   b: new B {
         *     num: 2,
         *     b: new B {
         *       num: 3,
         *       b: null,
         *       a: null
         *     },
         *     a: null
         *   },
         *   a: new A {
         *     a: null
         *   }
         * };
         * 
         * B innerB = b.b.b;
         * 
         * b = null;
         * 
         * println(innerB.num);
         */

        
        StructDef structDefB = new StructDef(getStructName("B"), List.of(
                new Param(new IntType(), getVariable("num")),
                new Param(getStructType("B"), getVariable("b")),
                new Param(getStructType("A"), getVariable("a"))));
        
        StructDef structDefA = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression allocExpA = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression allocExpB3 = new StructAllocExp(getStructType("B"), new StructActualParams(
                List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(3)),
                        new StructActualParam(getVariable("b"), getNullExp()),
                        new StructActualParam(getVariable("a"), getNullExp()))));
        Expression allocExpB2 = new StructAllocExp(getStructType("B"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(2)),
                    new StructActualParam(getVariable("b"), allocExpB3),
                    new StructActualParam(getVariable("a"), getNullExp()))));
        Expression allocExpB = new StructAllocExp(getStructType("B"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                    new StructActualParam(getVariable("b"), allocExpB2),
                    new StructActualParam(getVariable("a"), allocExpA))));
        Statement vardecStmtB = new VardecStmt(getStructType("B"), getVariable("b"), allocExpB);

        Expression dotExp2 = new DotExp(new VariableExp(getVariable("b")), getVariable("b"));
        Expression dotExp = new DotExp(dotExp2, getVariable("b"));
        Statement vardecStmtInnerB = new VardecStmt(getStructType("B"), getVariable("innerB"), dotExp);

        Statement assignStmt = new AssignStmt(getVariable("b"), getNullExp());

        Expression innerDotExp = new DotExp(new VariableExp(getVariable("innerB")), getVariable("num"));
        innerDotExp.setExpressionType(getIntType());
        PrintlnStmt printlnStmt = new PrintlnStmt(innerDotExp);

        Program program = new Program(List.of(structDefB, structDefA), List.of(),
                List.of(vardecStmtB, vardecStmtInnerB, assignStmt, printlnStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program, "3");
    }

    @Test
    public void testCodegenWithStructAssignmentsInWhileLoop() {
        /*
         * Should print 2, 2 and not leak
         *
         * struct A {
         *   int num;
         *   A a;
         * }
         * 
         * A outer = new A {
         *   num: 1,
         *   a: new A {
         *     num: 2,
         *     a: null
         *   }
         * };
         * 
         * int count = 1;
         * 
         * while (count > 0) {
         *   A inner = outer.a;
         *   println(inner.num);
         *   count = count - 1;
         * }
         * 
         * println(outer.a.num);
         */

        StructDef structA = new StructDef(getStructName("A"), List.of(
            new Param(getIntType(), getVariable("num")),
            new Param(getStructType("A"), getVariable("a"))));

        Expression structAllocExp2 = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(2)),
                    new StructActualParam(getVariable("a"), getNullExp()))));
        Expression structAllocExp = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                    new StructActualParam(getVariable("a"), structAllocExp2))));
        Statement outerVardec = new VardecStmt(getStructType("A"), getVariable("outer"), structAllocExp);

        Statement countVardec = new VardecStmt(getIntType(), getVariable("count"), new IntLiteralExp(1));

        Expression dotExpInnerNum = new DotExp(new VariableExp(getVariable("inner")), getVariable("num"));
        dotExpInnerNum.setExpressionType(getIntType());
        Statement innerPrint = new PrintlnStmt(dotExpInnerNum);
        Expression dotExpOuterA = new DotExp(new VariableExp(getVariable("outer")), getVariable("a"));
        Statement innerVardec = new VardecStmt(getStructType("A"), getVariable("inner"), dotExpOuterA);
        Expression minusExp = new BinaryOpExp(new VariableExp(getVariable("count")), OperatorEnum.MINUS, new IntLiteralExp(1));
        Statement decrement = new AssignStmt(getVariable("count"), minusExp);
        StmtBlock whileBody = new StmtBlock(List.of(innerVardec, innerPrint, decrement));
        Expression guard = new BinaryOpExp(new VariableExp(getVariable("count")), OperatorEnum.GREATER_THAN, new IntLiteralExp(0));
        Statement whileStmt = new WhileStmt(guard, whileBody);

        Expression dotExpOuterA2 = new DotExp(new VariableExp(getVariable("outer")), getVariable("a"));
        Expression dotExpDotNum = new DotExp(dotExpOuterA2, getVariable("num"));
        dotExpDotNum.setExpressionType(getIntType());
        Statement outerPrint = new PrintlnStmt(dotExpDotNum);

        Program program = new Program(List.of(structA), List.of(), List.of(outerVardec, countVardec, whileStmt, outerPrint));
        testProgramGeneratesAndDoesNotThrowOrLeak(program, "2", "2");
    }

    
    @Test
    public void testCodegenWithStructAssignmentsInIfStatement() {
        /*
         * Should print 2, 2 and not leak
         *
         * struct A {
         *   int num;
         *   A a;
         * }
         * 
         * A outer = new A {
         *   num: 1,
         *   a: new A {
         *     num: 2,
         *     a: null
         *   }
         * };
         * 
         * if (true) {
         *   A inner = outer.a;
         *   println(inner.num);
         * }
         * 
         * println(outer.a.num);
         */

        StructDef structA = new StructDef(getStructName("A"), List.of(
            new Param(getIntType(), getVariable("num")),
            new Param(getStructType("A"), getVariable("a"))));

        Expression structAllocExp2 = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(2)),
                    new StructActualParam(getVariable("a"), getNullExp()))));
        Expression structAllocExp = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                    new StructActualParam(getVariable("a"), structAllocExp2))));
        Statement outerVardec = new VardecStmt(getStructType("A"), getVariable("outer"), structAllocExp);

        Expression dotExpInnerNum = new DotExp(new VariableExp(getVariable("inner")), getVariable("num"));
        dotExpInnerNum.setExpressionType(getIntType());
        Statement innerPrint = new PrintlnStmt(dotExpInnerNum);
        Expression dotExpOuterA = new DotExp(new VariableExp(getVariable("outer")), getVariable("a"));
        Statement innerVardec = new VardecStmt(getStructType("A"), getVariable("inner"), dotExpOuterA);
        StmtBlock ifBody = new StmtBlock(List.of(innerVardec, innerPrint));
        Statement ifStmt = new IfElseStmt(new BoolLiteralExp(true), ifBody);

        Expression dotExpOuterA2 = new DotExp(new VariableExp(getVariable("outer")), getVariable("a"));
        Expression dotExpDotNum = new DotExp(dotExpOuterA2, getVariable("num"));
        dotExpDotNum.setExpressionType(getIntType());
        Statement outerPrint = new PrintlnStmt(dotExpDotNum);

        Program program = new Program(List.of(structA), List.of(), List.of(outerVardec, ifStmt, outerPrint));
        testProgramGeneratesAndDoesNotThrowOrLeak(program, "2", "2");
    }

    @Test
    public void testCodegenReassignStructVariableToNewStructAlloc() {
        /*
         * struct A {
         *   A a;
         * }
         * 
         * A* a1 = new A { a: null };
         * 
         * a1 = new A { a: null };
         */

        StructDef structA = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression allocExp = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement vardecStmt = new VardecStmt(getStructType("A"), getVariable("a"), allocExp);

        Expression allocExp2 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement assignStmt = new AssignStmt(getVariable("a"), allocExp2);

        Program program = new Program(List.of(structA), List.of(),
                List.of(vardecStmt, assignStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenWithNestedIfElseWhile() {
        /*
         * 
         * struct A {
         *   A a;
         * }
         * 
         * A a1 = new A { a: null };
         * 
         * int num = 3;
         * 
         * if (num == 3) {
         *   A a2 = new A { a: a1 };
         * 
         *   while (num > 0) {
         *     A a3 = new A { a: a2 };
         *     num = num - 1;
         * 
         *     if (num == 0) {
         *       A a4 = new A { a: a3 };
         *     }
         *   }
         * }
         */
        StructDef structA = new StructDef(getStructName("A"), List.of(new Param(getStructType("A"), getVariable("a"))));

        Expression structAllocA1 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), new NullExp()))));
        Statement  vardecStmtA1 = new VardecStmt(getStructType("A"), getVariable("a1"), structAllocA1);

        Statement vardecStmtNum = new VardecStmt(getIntType(), getVariable("num"), new IntLiteralExp(3));

        Expression doubleEqualsNum0 = new BinaryOpExp(new VariableExp(getVariable("num")), OperatorEnum.DOUBLE_EQUALS, new IntLiteralExp(0));
        Expression structAllocA4 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), new VariableExp(getVariable("a3"))))));
        Statement ifBlock2 = new VardecStmt(getStructType("A"), getVariable("a4"), structAllocA4);
        Statement ifStmt2 = new IfElseStmt(doubleEqualsNum0, ifBlock2);

        Expression structAllocA3 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), new VariableExp(getVariable("a2"))))));
        Statement vardecStmtA3 = new VardecStmt(getStructType("A"), getVariable("a3"), structAllocA3);
        Expression minusNum1 = new BinaryOpExp(new VariableExp(getVariable("num")), OperatorEnum.MINUS, new IntLiteralExp(1));
        Statement numDecrement = new AssignStmt(getVariable("num"), minusNum1);
        Statement stmtBlock2 = new StmtBlock(List.of(vardecStmtA3, numDecrement, ifStmt2));
        Expression greaterThanNum0 = new BinaryOpExp(new VariableExp(getVariable("num")), OperatorEnum.GREATER_THAN, new IntLiteralExp(0));
        Statement whileStmt = new WhileStmt(greaterThanNum0, stmtBlock2);

        Expression structAllocA2 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), new VariableExp(getVariable("a1"))))));
        Statement vardecStmtA2 = new VardecStmt(getStructType("A"), getVariable("a2"), structAllocA2);
        Expression doubleEqualsNum3 = new BinaryOpExp(new VariableExp(getVariable("num")), OperatorEnum.DOUBLE_EQUALS, new IntLiteralExp(3));
        Statement stmtBlock1 = new StmtBlock(List.of(vardecStmtA2, whileStmt));
        Statement ifStmt = new IfElseStmt(doubleEqualsNum3, stmtBlock1);

        Program program = new Program(List.of(structA), List.of(), List.of(vardecStmtA1, vardecStmtNum, ifStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenWithSingleStatementStructVardecInIf() {
        /*
         *
         * struct A {
         *   int num;
         *   A a;
         * }
         * 
         * if (true) {
         *   A inner = new A {
         *     num: 1
         *     a: null
         *   };
         * }
         * 
         */

        StructDef structA = new StructDef(getStructName("A"), List.of(
                new Param(getIntType(), getVariable("num")),
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                        new StructActualParam(getVariable("num"), getNullExp()))));
        Statement whileBody = new VardecStmt(getStructType("A"), getVariable("inner"), structAlloc);

        Expression testExp = new BoolLiteralExp(true);
        Statement ifStmt = new IfElseStmt(testExp, whileBody);

        Program program = new Program(List.of(structA), List.of(),
                List.of(ifStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testStructAllocInParens() {
        /*
         *
         * struct A {
         *   A a;
         * }
         * 
         * A* a = (new A { a: null });
         * 
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression parenExp = new ParenExp(structAlloc);
        Statement vardecStmt = new VardecStmt(getStructType("A"), getVariable("a"), parenExp);

        Program program = new Program(List.of(structDef), List.of(), List.of(vardecStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testStructAllocExpressionStmt() {
        /*
         * I'm pretty sure this is legal. I'm just checking before I put it in the
         * codegen
         *
         * struct A {
         *   A a;
         * }
         * 
         * new A { a: null };
         * 
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement expStmt = new ExpressionStmt(structAlloc);

        Program program = new Program(List.of(structDef), List.of(), List.of(expStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testStructAllocInParenExpressionInExpressionStmt() {
        /*
         * I'm pretty sure this is legal. I'm just checking before I put it in the
         * codegen
         *
         * struct A {
         *   A a;
         * }
         * 
         * (((new A { a: null })));
         * 
         * (new A { a: null });
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression parenExp = new ParenExp(new ParenExp(new ParenExp(structAlloc)));
        Statement expStmt = new ExpressionStmt(parenExp);

        Expression structAlloc2 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression parenExp2 = new ParenExp(structAlloc2);
        Statement expStmt2 = new ExpressionStmt(parenExp2);

        Program program = new Program(List.of(structDef), List.of(), List.of(expStmt, expStmt2));
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
    public void testCodegenWithElseStmtBlock() {
        /*
         * For coverage
         * 
         * int foo = 0;
         * 
         * if (false) {
         *   int foo = 1;
         * } else {
         *   int foo = 2;
         * }
         * 
         * println(foo);
         */

        Statement vardecStmt = new VardecStmt(getIntType(), getVariable("foo"), new IntLiteralExp(0));

        Statement ifBody = new AssignStmt(
            getVariable("foo"),
            new IntLiteralExp(6));
        Statement elseBody = new AssignStmt(
            getVariable("foo"),
            new IntLiteralExp(0));
        Statement stmtBlock = new StmtBlock(List.of(elseBody));
        Expression condition = new BoolLiteralExp(false);
        Statement ifElseStmt = new IfElseStmt(condition, ifBody, stmtBlock);

        Expression foo = new VariableExp(getVariable("foo"));
        foo.setExpressionType(getIntType());
        Statement printlnStmt = new PrintlnStmt(foo);

        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, ifElseStmt, printlnStmt));
        testProgramGeneratesAndDoesNotThrow(program, "0");
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
        Statement printlnStmt = new PrintlnStmt(new IntLiteralExp(6));

        Program program = new Program(List.of(), List.of(), List.of(printlnStmt));
        String expectedOutput = "6";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithParenInMathExp() {
        /*
         * Should print 10, then 16
         * 
         * println(2 + 2 * 4);
         * 
         * println((2 + 2) * 4);
         */

        Expression mult10 = new BinaryOpExp(new IntLiteralExp(2), OperatorEnum.MULTIPLY, new IntLiteralExp(4));
        Expression math10 = new BinaryOpExp(new IntLiteralExp(2), OperatorEnum.PLUS, mult10);
        math10.setExpressionType(getIntType());
        Statement printlnStmt = new PrintlnStmt(math10);

        Expression parenExp = new ParenExp(new BinaryOpExp(new IntLiteralExp(2), OperatorEnum.PLUS, new IntLiteralExp(2)));
        Expression math16 = new BinaryOpExp(parenExp, OperatorEnum.MULTIPLY, new IntLiteralExp(4));
        math16.setExpressionType(getIntType());
        Statement printlnStmt2 = new PrintlnStmt(math16);

        Program program = new Program(List.of(), List.of(), List.of(printlnStmt, printlnStmt2));
        testProgramGeneratesAndDoesNotThrow(program, "10", "16");
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
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenAllocateStructToTheSameVariableMultipleTimes() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * A a = new A {
         *   a: null
         * };
         * 
         * a = new A {
         *   a: null
         * };
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement vardec = new VardecStmt(getStructType("A"), getVariable("a"), structAlloc);

        Expression structAlloc2 = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Statement assign = new AssignStmt(getVariable("a"), structAlloc2);

        Program program = new Program(List.of(structDef), List.of(), List.of(vardec, assign));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenAssignStmtWithStructAllocInParen() {
        /*
         * This foolishness is for code coverage
         *  
         * struct A {
         *   A a;
         * }
         *
         * A a = null
         * 
         * a = (new A { a: null });
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Statement vardec = new VardecStmt(getStructType("A"), getVariable("a"), getNullExp());

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression parenExp = new ParenExp(structAlloc);
        Statement assign = new AssignStmt(getVariable("a"), parenExp);

        Program program = new Program(List.of(structDef), List.of(), List.of(vardec, assign));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testRepeatedAssignmentAndAbandonmentWithNestedStruct() {
        /*
         * Should print 2, and not leak
         *
         * struct A {
         *   int num;
         *   A a;
         * }
         *
         * A a1 = new A {
         *   num: 1,
         *   a: new A {
         *     num: 2,
         *     a: null
         *   }
         * };
         * 
         * A a2 = a1;
         * 
         * A a3 = a2;
         * 
         * A inner = a1.a;
         * 
         * a1 = null;
         * 
         * a2 = null;
         * 
         * a3 = null;
         * 
         * println(inner.num);
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getIntType(), getVariable("num")),
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAllocExp2 = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(2)),
                    new StructActualParam(getVariable("a"), getNullExp()))));
        Expression structAllocExp = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                    new StructActualParam(getVariable("a"), structAllocExp2))));
        Statement vardecA1 = new VardecStmt(getStructType("A"), getVariable("a1"), structAllocExp);

        Statement vardecA2 = new VardecStmt(getStructType("A"), getVariable("a2"), new VariableExp(getVariable("a1")));
        Statement vardecA3 = new VardecStmt(getStructType("A"), getVariable("a3"), new VariableExp(getVariable("a2")));

        Expression dotExpA1A = new DotExp(new VariableExp(getVariable("a1")), getVariable("a"));
        Statement vardecUnreachable = new VardecStmt(getStructType("A"), getVariable("inner"), dotExpA1A);

        Statement assignA1 = new AssignStmt(getVariable("a1"), getNullExp());
        Statement assignA2 = new AssignStmt(getVariable("a2"), getNullExp());
        Statement assignA3 = new AssignStmt(getVariable("a3"), getNullExp());

        Expression dotExpInnerNum = new DotExp(new VariableExp(getVariable("inner")), getVariable("num"));
        dotExpInnerNum.setExpressionType(getIntType());
        Statement printlnStmt = new PrintlnStmt(dotExpInnerNum);

        Program program = new Program(List.of(structDef), List.of(), 
                List.of(vardecA1, vardecA2, vardecA3, vardecUnreachable, assignA1, assignA2, assignA3, printlnStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program, "2");
    }

    @Test
    public void testCodegenStructFunctionAssignment() {
        /*
         * struct A {
         *   int num;
         *   A a;
         * }
         * 
         * A getA() {
         *   A a = new A {
         *     num: 3,
         *     a: null
         *   }
         * }
         *
         * A a = getA();
         * 
         * println(a.num);
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getIntType(), getVariable("num")),
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAllocExp = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(3)),
                    new StructActualParam(getVariable("a"), getNullExp()))));
        Statement stmtBlock = new VardecStmt(getStructType("A"), getVariable("a"), structAllocExp);
        StmtBlock funcBody = new StmtBlock(List.of(stmtBlock));
        Type returnType = getStructType("A");
        FunctionDef functionDef = new FunctionDef(
            getFunctionName("getA"), 
            List.of(), 
            returnType,
            funcBody);

        CommaExp commaExp = new CommaExp(List.of());
        Expression funcCall = new FuncCallExp(getFunctionName("getA"), commaExp);
        Statement vardecFunc = new VardecStmt(getStructType("A"), getVariable("a"), funcCall);

        Expression dotExp = new DotExp(new VariableExp(getVariable("a")), getVariable("num"));
        dotExp.setExpressionType(getIntType());
        Statement printStmt = new PrintlnStmt(dotExp);

        Program program = new Program(List.of(structDef), List.of(functionDef), List.of(vardecFunc, printStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
    }

    @Test
    public void testCodegenAssignStmtWithMultipleNonStructFields() {
        /*
         * For coverage
         * 
         * struct A {
         *   int num;
         *   bool tof;
         * }
         *
         * A a = null;
         * 
         * a = new A {
         *   num: 1,
         *   tof: true
         * };
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getIntType(), getVariable("num")),
                new Param(getBoolType(), getVariable("tof"))));

        Statement vardecStmt = new VardecStmt(getStructType("A"), getVariable("a"), getNullExp());

        Expression structAllocExp = new StructAllocExp(getStructType("A"), new StructActualParams(
            List.of(new StructActualParam(getVariable("num"), new IntLiteralExp(1)),
                    new StructActualParam(getVariable("tof"), new BoolLiteralExp(true)))));
        Statement assignStmt = new AssignStmt(getVariable("a"), structAllocExp);

        Program program = new Program(List.of(structDef), List.of(), List.of(vardecStmt, assignStmt));
        testProgramGeneratesAndDoesNotThrowOrLeak(program);
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
    public void testCodegenWithElseBlock() {
        /*
         * This should print 2
         *
         * if (false) {
         *   println(1);
         * } else {
         *   println(2);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(1));
        Statement elseBody = new PrintlnStmt(new IntLiteralExp(2));
        Expression condition = new BoolLiteralExp(false);
        Statement ifStmt = new IfElseStmt(condition, ifBody, elseBody);
        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        String expectedOutput = "2";
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

    @Test
    public void testCodegenWithNotEqualsExpCondition() {
        /*
         * This should only print 1
         *
         * if (6 != 5) {
         *   println(1);
         * }
         * 
         * if (6 != 6) {
         *   println(2);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(1));
        Expression condition = new BinaryOpExp(
                new IntLiteralExp(5),
                OperatorEnum.NOT_EQUALS,
                new IntLiteralExp(6));
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Statement ifBody2 = new PrintlnStmt(new IntLiteralExp(2));
        Expression condition2 = new BinaryOpExp(
                new IntLiteralExp(6),
                OperatorEnum.NOT_EQUALS,
                new IntLiteralExp(6));
        Statement ifStmt2 = new IfElseStmt(condition2, ifBody2);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt, ifStmt2));
        String expectedOutput = "1";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithGreaterThanExpCondition() {
        /*
         * This should only print 3
         *
         * if (7 > 6) {
         *   println(3);
         * }
         * 
         * if (3 > 6) {
         *   println(1);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(3));
        Expression condition = new BinaryOpExp(
                new IntLiteralExp(7),
                OperatorEnum.GREATER_THAN,
                new IntLiteralExp(6));
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Statement ifBody2 = new PrintlnStmt(new IntLiteralExp(1));
        Expression condition2 = new BinaryOpExp(
                new IntLiteralExp(3),
                OperatorEnum.GREATER_THAN,
                new IntLiteralExp(6));
        Statement ifStmt2 = new IfElseStmt(condition2, ifBody2);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt, ifStmt2));
        String expectedOutput = "3";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithLessThanExpCondition() {
        /*
         * This should only print 1
         *
         * if (3 < 4) {
         *   println(1);
         * }
         * 
         * if (4 < 3) {
         *   println(2);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(1));
        Expression condition = new BinaryOpExp(
                new IntLiteralExp(3),
                OperatorEnum.LESS_THAN,
                new IntLiteralExp(4));
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Statement ifBody2 = new PrintlnStmt(new IntLiteralExp(2));
        Expression condition2 = new BinaryOpExp(
                new IntLiteralExp(4),
                OperatorEnum.LESS_THAN,
                new IntLiteralExp(3));
        Statement ifStmt2 = new IfElseStmt(condition2, ifBody2);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt, ifStmt2));
        String expectedOutput = "1";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithGreaterThanEqualsExpCondition() {
        /*
         * This should only print 1 and 2
         *
         * if (5 >= 4) {
         *   println(1);
         * }
         * 
         * if (4 >= 4) {
         *   println(2);
         * }
         * 
         * if (4 >= 5) {
         *   println(3);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(1));
        Expression condition = new BinaryOpExp(
                new IntLiteralExp(5),
                OperatorEnum.GREATER_THAN_EQUALS,
                new IntLiteralExp(4));
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Statement ifBody2 = new PrintlnStmt(new IntLiteralExp(2));
        Expression condition2 = new BinaryOpExp(
                new IntLiteralExp(4),
                OperatorEnum.GREATER_THAN_EQUALS,
                new IntLiteralExp(4));
        Statement ifStmt2 = new IfElseStmt(condition2, ifBody2);

        Statement ifBody3 = new PrintlnStmt(new IntLiteralExp(3));
        Expression condition3 = new BinaryOpExp(
                new IntLiteralExp(4),
                OperatorEnum.GREATER_THAN_EQUALS,
                new IntLiteralExp(5));
        Statement ifStmt3 = new IfElseStmt(condition3, ifBody3);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt, ifStmt2, ifStmt3));
        String expectedOutput = "1\n2";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenWithLessThanEqualsExpCondition() {
        /*
         * This should only print 1 and 2
         *
         * if (4 <= 5) {
         *  println(1);
         * }
         * 
         * if (5 <= 5) {
         *  println(2);
         * }
         * 
         * if (5 <= 4) {
         *  println(3);
         * }
         */

        Statement ifBody = new PrintlnStmt(new IntLiteralExp(1));
        Expression condition = new BinaryOpExp(
                new IntLiteralExp(4),
                OperatorEnum.LESS_THAN_EQUALS,
                new IntLiteralExp(5));
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Statement ifBody2 = new PrintlnStmt(new IntLiteralExp(2));
        Expression condition2 = new BinaryOpExp(
                new IntLiteralExp(5),
                OperatorEnum.LESS_THAN_EQUALS,
                new IntLiteralExp(5));
        Statement ifStmt2 = new IfElseStmt(condition2, ifBody2);

        Statement ifBody3 = new PrintlnStmt(new IntLiteralExp(3));
        Expression condition3 = new BinaryOpExp(
                new IntLiteralExp(5),
                OperatorEnum.LESS_THAN_EQUALS,
                new IntLiteralExp(4));
        Statement ifStmt3 = new IfElseStmt(condition3, ifBody3);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt, ifStmt2, ifStmt3));
        String expectedOutput = "1\n2";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenNotOperatorExp() {
        /*
         * bool trueBool = true;
         * bool falseBool = !trueBool;
         * println(falseBool);
         */

        Statement trueAssign = new VardecStmt(
            getBoolType(),
            getVariable("trueBool"),
            new BoolLiteralExp(true));

        Expression notTrue = new UnaryOpExp(OperatorEnum.NOT, new VariableExp(getVariable("trueBool")));
        Statement falseAssign = new VardecStmt(
                getBoolType(),
                getVariable("falseBool"),
                notTrue);
        Expression printExp = new VariableExp(getVariable("falseBool"));
        printExp.setExpressionType(getBoolType());

        Statement falsePrint = new PrintlnStmt(printExp);

        Program program = new Program(List.of(), List.of(), List.of(trueAssign, falseAssign, falsePrint));
        String expectedOutput = "false";
        testProgramGeneratesAndDoesNotThrow(program, expectedOutput);
    }

    @Test
    public void testCodegenNotOperator() {
        /*
         * Should print 3
         *
         * bool trueBool = true;
         * 
         * bool falseBool = false;
         * 
         * if (!(trueBool && falseBool) == trueBool) {
         *   println(3);
         * }
         */


        Statement vardecTrue = new VardecStmt(getBoolType(), getVariable("trueBool"), new BoolLiteralExp(true));

        Statement vardecFalse = new VardecStmt(getBoolType(), getVariable("falseBool"), new BoolLiteralExp(false));

        Expression andExp = new BinaryOpExp(new VariableExp(getVariable("trueBool")), OperatorEnum.AND, new VariableExp(getVariable("falseBool")));
        Expression parenExp = new ParenExp(andExp);
        Expression notExp = new UnaryOpExp(OperatorEnum.NOT, parenExp);
        Expression condition = new BinaryOpExp(notExp, OperatorEnum.DOUBLE_EQUALS, new VariableExp(getVariable("trueBool")));
        Expression int3 = new IntLiteralExp(3);
        int3.setExpressionType(getIntType());
        Statement ifBody = new PrintlnStmt(int3);
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Program program = new Program(List.of(), List.of(), List.of(vardecTrue, vardecFalse, ifStmt));
        testProgramGeneratesAndDoesNotThrow(program, "3");
    }

    @Test
    public void testCodegenNotOperatorWithOrExp() {
        /*
         * Should print 3
         *
         * bool trueBool = true;
         * 
         * bool falseBool = false;
         * 
         * if (!(falseBool || falseBool) == trueBool) {
         *   println(3);
         * }
         */


        Statement vardecTrue = new VardecStmt(getBoolType(), getVariable("trueBool"), new BoolLiteralExp(true));

        Statement vardecFalse = new VardecStmt(getBoolType(), getVariable("falseBool"), new BoolLiteralExp(false));

        Expression andExp = new BinaryOpExp(new VariableExp(getVariable("falseBool")), OperatorEnum.OR, new VariableExp(getVariable("falseBool")));
        Expression parenExp = new ParenExp(andExp);
        Expression notExp = new UnaryOpExp(OperatorEnum.NOT, parenExp);
        Expression condition = new BinaryOpExp(notExp, OperatorEnum.DOUBLE_EQUALS, new VariableExp(getVariable("trueBool")));
        Expression int3 = new IntLiteralExp(3);
        int3.setExpressionType(getIntType());
        Statement ifBody = new PrintlnStmt(int3);
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Program program = new Program(List.of(), List.of(), List.of(vardecTrue, vardecFalse, ifStmt));
        testProgramGeneratesAndDoesNotThrow(program, "3");
    }

    @Test
    public void testCodegenLogicalOperatorPrecedenceNotBeforeOr() {
        /*
         * if (!(true && false) || true == true) {
         *   println(3);
         * }
         * 
         */

        Expression andExp = new BinaryOpExp(new BoolLiteralExp(true), OperatorEnum.OR, new BoolLiteralExp(false));
        Expression parenExp = new ParenExp(andExp);
        Expression notExp = new UnaryOpExp(OperatorEnum.NOT, parenExp);
        Expression doubleEqualsExp = new BinaryOpExp(new BoolLiteralExp(true), OperatorEnum.DOUBLE_EQUALS, new BoolLiteralExp(true));
        Expression condition = new BinaryOpExp(notExp, OperatorEnum.OR, doubleEqualsExp);
        Expression int3 = new IntLiteralExp(3);
        int3.setExpressionType(getIntType());
        Statement ifBody = new PrintlnStmt(int3);
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        testProgramGeneratesAndDoesNotThrow(program, "3");
    }

    @Test
    public void testCodegenLogicalOperatorPrecedenceNotBeforeAndBeforeOr() {
        /*
         * if (true && !true || false == 0) {
         *   println(3); 
         * }
         */

        Expression notExp = new UnaryOpExp(OperatorEnum.NOT, new BoolLiteralExp(true));
        Expression andExp = new BinaryOpExp(new BoolLiteralExp(true), OperatorEnum.OR, notExp);
        Expression doubleEqualsExp = new BinaryOpExp(new BoolLiteralExp(false), OperatorEnum.DOUBLE_EQUALS, new IntLiteralExp(0));
        Expression condition = new BinaryOpExp(andExp, OperatorEnum.DOUBLE_EQUALS, doubleEqualsExp);
        Expression int3 = new IntLiteralExp(3);
        int3.setExpressionType(getIntType());
        Statement ifBody = new PrintlnStmt(int3);
        Statement ifStmt = new IfElseStmt(condition, ifBody);

        Program program = new Program(List.of(), List.of(), List.of(ifStmt));
        testProgramGeneratesAndDoesNotThrow(program, "3");
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
    public void structScopeManagerThrowsWhenExitingScopeOnEmptyStack() {
        // This is for code coverage
        StructScopeManager structScopeManager = new StructScopeManager();
        assertThrows(CodegenException.class, () -> structScopeManager.exitScope());
    }

    @Test
    public void structScopeManagerThrowsWhenCheckingIfVariableIsInScopeOnEmptyStack() {
        // This is for code coverage
        StructScopeManager structScopeManager = new StructScopeManager();
        String variableName = "variableName";
        assertThrows(CodegenException.class, () -> structScopeManager.isInScope(variableName));
    }

    @Test
    public void structScopeManagerThrowsWhenAddingVariableToEmptyStack() {
        // This is for code coverage
        StructScopeManager structScopeManager = new StructScopeManager();
        String variableName = "variableName";
        StructType structType = getStructType("structTypeName");
        assertThrows(CodegenException.class, () -> structScopeManager.addStructVariableToScope(variableName, structType));
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
            testProgramGeneratesAndDoesNotThrowOrLeak(program, "3");
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
            testProgramGeneratesAndDoesNotThrowOrLeak(program, "3", "false");
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

    @Test
    public void testCodegenRefraffProgramFunctionOverloadingWithoutException() {
        String input = ResourceUtil.readInputFile("program_function_overloading.txt");
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
            testProgramGeneratesAndDoesNotThrow(program, "true", "17");
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

    // Needs integration testing with leaks (for when we start working on reference counted memory management)

}

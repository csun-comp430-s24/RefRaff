package refraff.typechecker;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;
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
import refraff.util.ResourceUtil;

import java.util.*;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TypecheckerTest {

    // Helper methods to set some source that would normally be defined through the tokenizer/parser combination
    
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

    // Test valid inputs

    private void testDoesNotThrowTypecheckerException(Program program) {
        assertDoesNotThrow(() -> Typechecker.typecheckProgram(program));
    }

    @Test
    public void testStructDefWithRecursiveStructVariableType() {
        /*
         * struct A {
         *   A a;
         * }
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));

        Program program = new Program(List.of(structDef), List.of(), List.of());
        testDoesNotThrowTypecheckerException(program);
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
        testDoesNotThrowTypecheckerException(program);
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
         * (new A { a: null });
         * 
         */

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));

        Expression structAlloc = new StructAllocExp(getStructType("A"), new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), getNullExp()))));
        Expression parenExp = new ParenExp(structAlloc);
        Statement expStmt = new ExpressionStmt(parenExp);

        Program program = new Program(List.of(structDef), List.of(), List.of(expStmt));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testStructAllocInParens() {
        /*
         * I'm pretty sure this is legal. I'm just checking before I put it in the codegen
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
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testStructDefWithOtherStructVariableType() {
        /*
         * struct A {
         *   int foo;
         *   bool bar;
         * }
         *
         * struct B {
         *   A a;
         * }
         */
        StructDef structDef1 = new StructDef(getStructName("A"), List.of(
                new Param(getIntType(), getVariable("foo")),
                new Param(getBoolType(), getVariable("bar"))
        ));

        StructDef structDef2 = new StructDef(getStructName("B"), List.of(
                new Param(getStructType("A"), getVariable("a"))
        ));

        Program program = new Program(List.of(structDef1, structDef2), List.of(), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testStructAllocExp() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A { a: null };
         */

        StructType aType = getStructType("A");

        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(aType, getVariable("a"))
        ));

        Expression expression = new StructAllocExp(aType,
                new StructActualParams(List.of(
                        new StructActualParam(getVariable("a"), getNullExp()
                        ))));
        Statement statement = new ExpressionStmt(expression);

        Program program = new Program(List.of(structDef), List.of(), List.of(statement));
        testDoesNotThrowTypecheckerException(program);

        assertEquals(aType, expression.getExpressionType());
    }

    @Test
    public void testStructAllocExpRecursive() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A {
         *   a: new A {
         *     a: null
         *   }
         * };
         */
        StructType aType = getStructType("A");
        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(aType, getVariable("a"))
        ));

        // Inside nested new A
        Expression nestedAllocExp = new StructAllocExp(aType,
                new StructActualParams(List.of(
                        new StructActualParam(getVariable("a"), getNullExp()))
                ));

        // Outside alloc that contains a: <nested>
        Expression outsideAllocExp = new StructAllocExp(aType, new StructActualParams(
                List.of(new StructActualParam(getVariable("a"), nestedAllocExp))
        ));

        Statement statement = new ExpressionStmt(outsideAllocExp);

        Program program = new Program(List.of(structDef), List.of(), List.of(statement));
        testDoesNotThrowTypecheckerException(program);

        assertEquals(aType, nestedAllocExp.getExpressionType());
        assertEquals(aType, outsideAllocExp.getExpressionType());
    }

    @Test
    public void testDotExpRecursive() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * A a = null;
         * a.a.a;
         */
        StructName name = getStructName("A");
        StructType type = getStructType("A");

        Variable variable = getVariable("a");

        StructDef structDef = new StructDef(name, List.of(
                new Param(type, variable)
        ));

        VardecStmt vardec = new VardecStmt(type, variable, getNullExp());

        VariableExp variableExp = new VariableExp(variable);

        Expression firstDotExpression = new DotExp(variableExp, variable);
        Expression secondDotExpression = new DotExp(firstDotExpression, variable);
        Statement dotStatement = new ExpressionStmt(secondDotExpression);

        Program program = new Program(List.of(structDef), List.of(), List.of(vardec, dotStatement));
        testDoesNotThrowTypecheckerException(program);

        assertEquals(type, vardec.getExpression().getExpressionType());

        assertEquals(type, firstDotExpression.getExpressionType());
        assertEquals(type, secondDotExpression.getExpressionType());
    }

    @Test
    public void testExpWithBool() {
        /*
         * true;
         */
        Statement boolLitExpStmtTrue = new ExpressionStmt(new BoolLiteralExp(true));
        Program program = new Program(List.of(), List.of(), List.of(boolLitExpStmtTrue));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testVardecExpAndVarExp() {
        /*
         * int foo = 6;
         * foo;
         */
        Statement vardecStmt = new VardecStmt(
            getIntType(), 
            getVariable("foo"),
            new IntLiteralExp(6)
        );

        Statement varExpStmt = new ExpressionStmt(new VariableExp(getVariable("foo")));
        Program program = new Program(List.of(), List.of(), List.of(vardecStmt, varExpStmt));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testAssignStmt() {
        /*
         * bool a = false;
         * a = true;
         */
        Statement vardec = new VardecStmt(getBoolType(), getVariable("a"), new BoolLiteralExp(false));
        Statement assign = new AssignStmt(getVariable("a"), new BoolLiteralExp(true));

        Program program = new Program(List.of(), List.of(), List.of(vardec, assign));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testEmptyWhileStmt() {
        /*
         * while (true) {
         *
         * }
         */
        Statement whileStmt = new WhileStmt(new BoolLiteralExp(true), new StmtBlock());

        Program program = new Program(List.of(), List.of(), List.of(whileStmt));
        testDoesNotThrowTypecheckerException(program);
    }


    @Test
    public void testNonEmptyWhileStmt() {
        /*
         * int x = 7;
         * while (x <= 10)
         *     x = x + 1;
         */
        Variable x = getVariable("x");
        VariableExp xExp = new VariableExp(x);

        Statement vardec = new VardecStmt(getIntType(), x, new IntLiteralExp(7));

        Expression whileCondition = new BinaryOpExp(xExp, OperatorEnum.LESS_THAN_EQUALS, new IntLiteralExp(10));
        Statement whileBody = new AssignStmt(x, new BinaryOpExp(xExp, OperatorEnum.PLUS, new IntLiteralExp(1)));

        Statement whileStmt = new WhileStmt(whileCondition, whileBody);

        Program program = new Program(List.of(), List.of(), List.of(vardec, whileStmt));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testBreakStmt() {
        /*
         * while (true)
         *    break;
         */
        Statement whileStmt = new WhileStmt(new BoolLiteralExp(true), new BreakStmt());

        Program program = new Program(List.of(), List.of(), List.of(whileStmt));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testNestedBreakStmt() {
        /*
         * while (true) {
         *     while (false)
         *         break;
         *    break;
         * }
         */
        Statement whileInner = new WhileStmt(new BoolLiteralExp(false), new BreakStmt());
        Statement whileOuter = new WhileStmt(new BoolLiteralExp(true), new StmtBlock(List.of(whileInner, new BreakStmt())));

        Program program = new Program(List.of(), List.of(), List.of(whileOuter));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testIfStmt() {
        /*
         * int x = 2;
         * if (true)
         *     x = 4;
         */
        Variable x = getVariable("x");

        Statement vardec = new VardecStmt(getIntType(), x, new IntLiteralExp(2));

        Statement ifBody = new AssignStmt(x, new IntLiteralExp(4));
        Statement ifStmt = new IfElseStmt(new BoolLiteralExp(true), ifBody);

        Program program = new Program(List.of(), List.of(), List.of(vardec, ifStmt));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testIfElseStmt() {
        /*
         * int x = 2;
         * if (true)
         *     x = 4;
         * else
         *     x = 7;
         */
        Variable x = getVariable("x");

        Statement vardec = new VardecStmt(getIntType(), x, new IntLiteralExp(2));

        Statement ifBody = new AssignStmt(x, new IntLiteralExp(4));
        Statement elseBody = new AssignStmt(x, new IntLiteralExp(7));
        Statement ifStmt = new IfElseStmt(new BoolLiteralExp(true), ifBody, elseBody);

        Program program = new Program(List.of(), List.of(), List.of(vardec, ifStmt));
        testDoesNotThrowTypecheckerException(program);

    }

    @Test
    public void testPrintlnStmtWithInt() {
        // println(4);
        Program program = new Program(List.of(), List.of(), List.of(new PrintlnStmt(new IntLiteralExp(4))));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testPrintlnStmtWithBool() {
        // println(true);
        Program program = new Program(List.of(), List.of(), List.of(new PrintlnStmt(new BoolLiteralExp(true))));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testLogicalNotExpStmt() {
        // !true;
        Expression expression = new UnaryOpExp(OperatorEnum.NOT, new BoolLiteralExp(true));
        Statement statement = new ExpressionStmt(expression);

        Program program = new Program(List.of(), List.of(), List.of(statement));
        testDoesNotThrowTypecheckerException(program);

        assertTrue(expression.getExpressionType().hasTypeEquality(new BoolType()));
    }

    @TestFactory
    public List<DynamicTest> testOrOp() {
        return testBooleanOnlyBinOp(OperatorEnum.OR, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testAndOp() {
        return testBooleanOnlyBinOp(OperatorEnum.AND, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testDoubleEqualsOp() {
        return testBooleanIntStructBinOp(OperatorEnum.DOUBLE_EQUALS, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testNotEqualsOp() {
        return testBooleanIntStructBinOp(OperatorEnum.NOT_EQUALS, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testLessThanOp() {
        return testIntOnlyBinOp(OperatorEnum.LESS_THAN, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testGreaterThanOp() {
        return testIntOnlyBinOp(OperatorEnum.GREATER_THAN, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testLessThanEqualsOp() {
        return testIntOnlyBinOp(OperatorEnum.LESS_THAN_EQUALS, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testGreaterThanEqualsOp() {
        return testIntOnlyBinOp(OperatorEnum.GREATER_THAN_EQUALS, getBoolType());
    }

    @TestFactory
    public List<DynamicTest> testPlusOp() {
        return testIntOnlyBinOp(OperatorEnum.PLUS, getIntType());
    }

    @TestFactory
    public List<DynamicTest> testMinusOp() {
        return testIntOnlyBinOp(OperatorEnum.MINUS, getIntType());
    }

    @TestFactory
    public List<DynamicTest> testMultOp() {
        return testIntOnlyBinOp(OperatorEnum.MULTIPLY, getIntType());
    }

    @TestFactory
    public List<DynamicTest> testDivideOp() {
        return testIntOnlyBinOp(OperatorEnum.DIVISION, getIntType());
    }

    private List<DynamicTest> testBooleanOnlyBinOp(OperatorEnum op, Type expectedType) {
        return List.of(
                testBinOpBool(op, expectedType),
                failing(testBinOpInt(op, expectedType)),
                failing(testBinOpMatchingStructType(op, expectedType)),
                failing(testBinOpBothNullStructType(op, expectedType)),
                failing(testBinOpNotMatchingStructType(op, expectedType)),
                failing(testBinOpVoid(op, expectedType)),
                failing(testBinOpMismatchedTypes(op, expectedType))
        );
    }

    private List<DynamicTest> testIntOnlyBinOp(OperatorEnum op, Type expectedType) {
        return List.of(
                failing(testBinOpBool(op, expectedType)),
                testBinOpInt(op, expectedType),
                failing(testBinOpMatchingStructType(op, expectedType)),
                failing(testBinOpBothNullStructType(op, expectedType)),
                failing(testBinOpNotMatchingStructType(op, expectedType)),
                failing(testBinOpVoid(op, expectedType)),
                failing(testBinOpMismatchedTypes(op, expectedType))
        );
    }

    private List<DynamicTest> testBooleanIntStructBinOp(OperatorEnum op, Type expectedType) {
        return List.of(
                testBinOpBool(op, expectedType),
                testBinOpInt(op, expectedType),
                testBinOpMatchingStructType(op, expectedType),
                testBinOpBothNullStructType(op, expectedType),
                failing(testBinOpNotMatchingStructType(op, expectedType)),
                failing(testBinOpVoid(op, expectedType)),
                failing(testBinOpMismatchedTypes(op, expectedType))
        );
    }

    private DynamicTest failing(DynamicTest test) {
        return DynamicTest.dynamicTest(test.getDisplayName() + " should fail",
                () -> Assertions.assertThrows(AssertionFailedError.class, test.getExecutable()));
    }

    private DynamicTest testBinOpMismatchedTypes(OperatorEnum op, Type expectedType) {
        // 5 <op> false
        return testBinOp("int types", Optional.empty(), Optional.empty(), Optional.empty(), new IntLiteralExp(5), op,
                new BoolLiteralExp(false), expectedType);
    }

    private DynamicTest testBinOpInt(OperatorEnum op, Type expectedType) {
        // 5 <op> 7
        return testBinOp("int types", Optional.empty(), Optional.empty(), Optional.empty(), new IntLiteralExp(5), op,
                new IntLiteralExp(7), expectedType);
    }

    private DynamicTest testBinOpBool(OperatorEnum op, Type expectedType) {
        // true <op> false
        return testBinOp("bool types", Optional.empty(), Optional.empty(), Optional.empty(), new BoolLiteralExp(true), op,
                new BoolLiteralExp(false), expectedType);
    }

    private DynamicTest testBinOpVoid(OperatorEnum op, Type expectedType) {
        FunctionName functionName = getFunctionName("foo");

        // foo(): void {}
        FunctionDef functionDef = new FunctionDef(functionName, List.of(), getVoidType(), new StmtBlock());
        Optional<List<FunctionDef>> functionDefs = Optional.of(List.of(functionDef));

        // a <op> b
        Expression firstFooCall = new FuncCallExp(functionName, new CommaExp(List.of()));
        Expression secondFooCall = new FuncCallExp(functionName, new CommaExp(List.of()));

        return testBinOp("void types", Optional.empty(), functionDefs, Optional.empty(),
                firstFooCall, op, secondFooCall, expectedType);
    }

    private DynamicTest testBinOpMatchingStructType(OperatorEnum op, Type expectedType) {
        StructName structName = getStructName("A");
        StructType structType = getStructType("A");

        StructDef structDef = new StructDef(structName, List.of());
        Optional<List<StructDef>> structDefs = Optional.of(List.of(structDef));

        // A a = null;
        Variable a = getVariable("a");
        Statement aVardec = new VardecStmt(structType, a, getNullExp());

        // A b = null;
        Variable b = getVariable("b");
        Statement bVardec = new VardecStmt(structType, b, getNullExp());

        Optional<List<Statement>> precedingStatements = Optional.of(List.of(aVardec, bVardec));

        // a <op> b
        VariableExp aExp = new VariableExp(a);
        VariableExp bExp = new VariableExp(b);

        return testBinOp("matching struct type", structDefs, Optional.empty(), precedingStatements,
                aExp, op, bExp, expectedType);
    }

    private DynamicTest testBinOpNotMatchingStructType(OperatorEnum op, Type expectedType) {
        StructName aStructName = getStructName("A");
        StructType aStructType = getStructType("A");
        StructDef aStructDef = new StructDef(aStructName, List.of());

        StructName bStructName = getStructName("B");
        StructType bStructType = getStructType("B");
        StructDef bStructDef = new StructDef(bStructName, List.of());

        Optional<List<StructDef>> structDefs = Optional.of(List.of(aStructDef, bStructDef));

        // A a = null;
        Variable a = getVariable("a");
        Statement aVardec = new VardecStmt(aStructType, a, getNullExp());

        // B b = null;
        Variable b = getVariable("b");
        Statement bVardec = new VardecStmt(bStructType, b, getNullExp());

        Optional<List<Statement>> precedingStatements = Optional.of(List.of(aVardec, bVardec));

        // a <op> b
        VariableExp aExp = new VariableExp(a);
        VariableExp bExp = new VariableExp(b);

        return testBinOp("not matching struct type", structDefs, Optional.empty(), precedingStatements,
                aExp, op, bExp, expectedType);
    }

    private DynamicTest testBinOpBothNullStructType(OperatorEnum op, Type expectedType) {
        return testBinOp("null structs", Optional.empty(), Optional.empty(), Optional.empty(),
                getNullExp(), op, getNullExp(), expectedType);
    }

    private DynamicTest testBinOp(String testName, Optional<List<StructDef>> optionalStructDefs,
                                  Optional<List<FunctionDef>> optionalFunctionDefs,
                                  Optional<List<Statement>> precedingStatements, Expression leftExpression,
                                  OperatorEnum op, Expression rightExpression, Type expectedType) {
        List<StructDef> structDefs = optionalStructDefs.orElse(List.of());
        List<FunctionDef> functionDefs = optionalFunctionDefs.orElse(List.of());

        Expression expression = new BinaryOpExp(leftExpression, op, rightExpression);
        Statement statement = new ExpressionStmt(expression);

        List<Statement> statements = new ArrayList<>(precedingStatements.orElse(List.of()));
        statements.add(statement);

        Program program = new Program(structDefs, functionDefs, statements);

        return DynamicTest.dynamicTest(op.name() + ": " + testName,
                () -> {
                    testDoesNotThrowTypecheckerException(program);
                    assertTrue(expectedType.hasTypeEquality(expression.getExpressionType()));
                });
    }

    @Test
    public void testExpWithBinOpExp() {
        /*
         * (false && (4 + 3 * 7 >= 4 - 3 / 5)) || true;
         */

        Expression intLiteral1 = new IntLiteralExp(1);
        Expression intLiteral5 = new IntLiteralExp(5);
        Expression binOpDivide = new BinaryOpExp(intLiteral1, OperatorEnum.DIVISION, intLiteral5);
        Expression intLiteral6 = new IntLiteralExp(6);
        Expression binOpMinus = new BinaryOpExp(intLiteral6, OperatorEnum.MINUS, binOpDivide);//

        Expression intLiteral3 = new IntLiteralExp(3);
        Expression intLiteral7 = new IntLiteralExp(7);
        Expression binOpMult = new BinaryOpExp(intLiteral3, OperatorEnum.MULTIPLY, intLiteral7);
        Expression intLiteral4 = new IntLiteralExp(4);
        Expression binOpAdd = new BinaryOpExp(intLiteral4, OperatorEnum.PLUS, binOpMult);

        Expression binOpGte = new BinaryOpExp(binOpAdd, OperatorEnum.GREATER_THAN_EQUALS, binOpMinus);
        Expression parenGteExp = new ParenExp(binOpGte);
        Expression falseExp = new BoolLiteralExp(false);

        Expression andExp = new BinaryOpExp(falseExp, OperatorEnum.AND, parenGteExp);
        Expression trueExp = new BoolLiteralExp(true);
        Expression parenAndExp = new ParenExp(andExp);
        Expression orExp = new BinaryOpExp(parenAndExp, OperatorEnum.OR, trueExp);

        Statement statement = new ExpressionStmt(orExp);

        Program program = new Program(List.of(), List.of(), List.of(statement));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testBinOpExpressionsWithVariables() {
        /*
         * int foo = 6
         * bool isBar = true;
         * isBar = (false && (4 + 3 * 7 >= 4 - foo / 5)) || isBar;
         */

        Statement declareFoo = new VardecStmt(
            getIntType(),
            getVariable("foo"),
            new IntLiteralExp(6));
        
        Statement declareIsBar = new VardecStmt(
            getBoolType(),
            getVariable("isBar"),
            new BoolLiteralExp(true));

        Expression varExpFoo = new VariableExp(getVariable("foo"));
        Expression intLiteral5 = new IntLiteralExp(5);
        Expression binOpDivide = new BinaryOpExp(varExpFoo, OperatorEnum.DIVISION, intLiteral5);
        Expression intLiteral6 = new IntLiteralExp(6);
        Expression binOpMinus = new BinaryOpExp(intLiteral6, OperatorEnum.MINUS, binOpDivide);//

        Expression intLiteral3 = new IntLiteralExp(3);
        Expression intLiteral7 = new IntLiteralExp(7);
        Expression binOpMult = new BinaryOpExp(intLiteral3, OperatorEnum.MULTIPLY, intLiteral7);
        Expression intLiteral4 = new IntLiteralExp(4);
        Expression binOpAdd = new BinaryOpExp(intLiteral4, OperatorEnum.PLUS, binOpMult);

        Expression binOpGte = new BinaryOpExp(binOpAdd, OperatorEnum.GREATER_THAN_EQUALS, binOpMinus);
        Expression parenGteExp = new ParenExp(binOpGte);
        Expression falseExp = new BoolLiteralExp(false);

        Expression andExp = new BinaryOpExp(falseExp, OperatorEnum.AND, parenGteExp);
        Expression varExpIsBar = new VariableExp(getVariable("isBar"));
        Expression parenAndExp = new ParenExp(andExp);
        Expression orExp = new BinaryOpExp(parenAndExp, OperatorEnum.OR, varExpIsBar);

        Statement assignIsBar = new ExpressionStmt(orExp);

        Program program = new Program(List.of(), List.of(), List.of(declareFoo, declareIsBar, assignIsBar));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testFunctionDefinitionReturnsBool() {
        /*
        *  func alwaysTrue(): bool {
        *       return true;
        *  }
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

        Program program = new Program(List.of(), List.of(funcDef), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testFunctionDefinitionsWithSameNameDifferentSignature() {
        /*
         * func alwaysTrue(): bool {
         *   return true;
         * }
         * 
         * func alwaysTrue(int num): bool {
         *   return true;
         * }
         */

        Expression boolTrue = new BoolLiteralExp(true);
        Statement returnStmtTrue = new ReturnStmt(boolTrue);
        StmtBlock funcBody = new StmtBlock(List.of(returnStmtTrue));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("alwaysTrue"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody);

        Expression boolTrue2 = new BoolLiteralExp(true);
        Statement returnStmtTrue2 = new ReturnStmt(boolTrue2);
        StmtBlock funcBody2 = new StmtBlock(List.of(returnStmtTrue2));
        Param param = new Param(getIntType(), getVariable("num"));
        FunctionDef funcDef2 = new FunctionDef(
                getFunctionName("alwaysTrue"),
                List.of(param),
                getBoolType(),
                funcBody2);

        Program program = new Program(List.of(), List.of(funcDef, funcDef2), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testFunctionCallWithOverloadFindsCorrectSignature() {
        /*
         * func alwaysTrue(): bool {
         *   return true;
         * }
         *
         * func alwaysTrue(int num): bool {
         *   return true;
         * }
         *
         * alwaysTrue();
         * alwaysTrue(3);
         */

        Expression boolTrue = new BoolLiteralExp(true);
        Statement returnStmtTrue = new ReturnStmt(boolTrue);
        StmtBlock funcBody = new StmtBlock(List.of(returnStmtTrue));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("alwaysTrue"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody);

        Expression boolTrue2 = new BoolLiteralExp(true);
        Statement returnStmtTrue2 = new ReturnStmt(boolTrue2);
        StmtBlock funcBody2 = new StmtBlock(List.of(returnStmtTrue2));
        Param param = new Param(getIntType(), getVariable("num"));
        FunctionDef funcDef2 = new FunctionDef(
                getFunctionName("alwaysTrue"),
                List.of(param),
                getBoolType(),
                funcBody2);

        Expression alwaysTrueCall1 = new FuncCallExp(getFunctionName("alwaysTrue"), new CommaExp(List.of()));
        Statement alwaysTrueCall1Statement = new ExpressionStmt(alwaysTrueCall1);

        Expression alwaysTrueCall2 = new FuncCallExp(getFunctionName("alwaysTrue"), new CommaExp(List.of(new IntLiteralExp(3))));
        Statement alwaysTrueCall2Statement = new ExpressionStmt(alwaysTrueCall2);

        Program program = new Program(List.of(), List.of(funcDef, funcDef2),
                List.of(alwaysTrueCall1Statement, alwaysTrueCall2Statement));
        testDoesNotThrowTypecheckerException(program);

    }

    @Test
    public void testFunctionDefinitionReturnsFunctionCallWithSameType() {
        /*
         * func alwaysTrue(): bool {
         *   return true;
         * }
         * 
         * func returnsCall(): bool {
         *   return alwaysTrue();
         * }
         */

        Expression boolTrue = new BoolLiteralExp(true);
        Statement returnStmtTrue = new ReturnStmt(boolTrue);
        StmtBlock funcBody = new StmtBlock(List.of(returnStmtTrue));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("alwaysTrue"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody);

        CommaExp args = new CommaExp(new ArrayList<Expression>());
        Expression funcCall = new FuncCallExp(getFunctionName("alwaysTrue"), args);
        Statement returnFuncCall = new ReturnStmt(funcCall);
        StmtBlock funcBody2 = new StmtBlock(List.of(returnFuncCall));
        FunctionDef funcDef2 = new FunctionDef(
                getFunctionName("returnsCall"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody2);

        Program program = new Program(List.of(), List.of(funcDef, funcDef2), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testVoidFunctionReturnsWithNoExp() {
        /*
         * func returnsNothing(int x): void {
         *   x = 0;
         *   return;
         * }
         */

        Statement assignStmt = new AssignStmt(getVariable("x"), new IntLiteralExp(0));
        Statement returnStmt = new ReturnStmt();
        StmtBlock funcBody = new StmtBlock(List.of(assignStmt, returnStmt));
        Param paramX = new Param(getIntType(), getVariable("x"));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("returnsNothing"),
                List.of(paramX),
                getVoidType(),
                funcBody);

        Program program = new Program(List.of(), List.of(funcDef), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testVoidFunctionWithNoReturn() {
        /*
         * func thePointOfNoReturn(): void {
         *   int x = 0;
         * }
         */

        Statement vardecStmt = new VardecStmt(getIntType(), getVariable("x"), new IntLiteralExp(0));
        StmtBlock funcBody = new StmtBlock(List.of(vardecStmt));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("thePointOfNoReturn"),
                new ArrayList<Param>(),
                getVoidType(),
                funcBody);

        Program program = new Program(List.of(), List.of(funcDef), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    // Test recursive function
    @Test
    public void testRecursiveFunction() {
        /*
         * func andBeyond(): bool {
         *   return andBeyond();
         * }
         */

        CommaExp commaExp = new CommaExp(new ArrayList<Expression>());
        FuncCallExp funcCallExp = new FuncCallExp(getFunctionName("andBeyond"), commaExp);
        Statement returnStmt = new ReturnStmt(funcCallExp);
        StmtBlock funcBody = new StmtBlock(List.of(returnStmt));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("andBeyond"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody);

        Program program = new Program(List.of(), List.of(funcDef), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    // Tests for function definitions
    // Test function with mutliple return statements (when if/else typechecking is implemented)
    // Test function with different typed return statements throws error (after if/else)
    // Should we check for return reachability?


    @Test
    public void testStructNameAsParamType() {
        /*
         * struct A {
         *   A a;
         * }
         * 
         * func foo(A a) : void {
         *   return;
         * }
         */
        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("A"), getVariable("a"))));
    
        FunctionBody funcBody = new FunctionBody(List.of(new ReturnStmt()));
        Param param = new Param(getStructType("A"), getVariable("a"));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("foo"),
                List.of(param),
                getVoidType(),
                funcBody
        );

        Program program = new Program(List.of(structDef), List.of(funcDef), List.of());
        testDoesNotThrowTypecheckerException(program);
    }

    // Scope tests

    @Test
    public void testFunctionDefHasOwnScopeFromProgramEntry() {
        /*
         *  func foo(): void {
         *       int a = 2;
         *  }
         *
         *  int a = 2;
         */

        Statement aVardec = new VardecStmt(getIntType(), getVariable("a"), new IntLiteralExp(2));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("foo"),
                List.of(),
                getVoidType(),
                new FunctionBody(List.of(aVardec)));

        Program program = new Program(List.of(), List.of(funcDef), List.of(aVardec));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testIfStatementHasLowerScope() {
        /*
         *  if (true)
         *     int a = 2;
         *
         *  int a = 2;
         */

        Statement aVardec = new VardecStmt(getIntType(), getVariable("a"), new IntLiteralExp(2));
        Statement ifStatement = new IfElseStmt(new BoolLiteralExp(true), aVardec);

        Program program = new Program(List.of(), List.of(), List.of(ifStatement, aVardec));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testIfElseStatementHasLowerScope() {
        /*
         *  if (true)
         *     int a = 2;
         *  else
         *     int a = 2;
         *
         *  int a = 2;
         */

        Statement aVardec = new VardecStmt(getIntType(), getVariable("a"), new IntLiteralExp(2));
        Statement ifElseStatement = new IfElseStmt(new BoolLiteralExp(true), aVardec, aVardec);

        Program program = new Program(List.of(), List.of(), List.of(ifElseStatement, aVardec));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testWhileStatementHasLowerScope() {
        /*
         *  while (true)
         *     int a = 2;
         *
         *  int a = 2;
         */

        Statement aVardec = new VardecStmt(getIntType(), getVariable("a"), new IntLiteralExp(2));
        Statement whileStatement = new WhileStmt(new BoolLiteralExp(true), aVardec);

        Program program = new Program(List.of(), List.of(), List.of(whileStatement, aVardec));
        testDoesNotThrowTypecheckerException(program);
    }

    @Test
    public void testStatementBlockHasLowerScope() {
        /*
         *  {
         *     int a = 2;
         *  }
         *
         *  int a = 2;
         */

        Statement aVardec = new VardecStmt(getIntType(), getVariable("a"), new IntLiteralExp(2));
        Statement statementBlock = new StmtBlock(List.of(aVardec));

        Program program = new Program(List.of(), List.of(), List.of(statementBlock, aVardec));
        testDoesNotThrowTypecheckerException(program);
    }


    // Test invalid inputs

    private void testThrowsTypecheckerException(Program invalidProgram) {
        assertThrows(TypecheckerException.class, () -> Typechecker.typecheckProgram(invalidProgram));
    }

    @Test
    public void testStructDefWithVoidTypeVariable() {
        /*
         * struct A {
         *   void b;
         * }
         */
        StructDef invalidStructDef = new StructDef(getStructName("A"), List.of(
                new Param(getVoidType(), getVariable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructDefReusesNameThrowsException() {
        /*
         * struct A {
         *   int b;
         * }
         * 
         * struct A {
         *   int c;
         * }
         */
        StructName aStructName = getStructName("A");

        StructDef structA = new StructDef(
                aStructName,
            List.of(
                new Param(getIntType(), getVariable("b"))
            )
        );
        StructDef structB = new StructDef(
                aStructName,
            List.of(
                new Param(getIntType(), getVariable("c"))
            )
        );

        Program invalidProgram = new Program(List.of(structA, structB), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructDefWithUndefinedStructTypeVariable() {
        /*
         * struct A {
         *   B b;
         * }
         */
        StructDef invalidStructDef = new StructDef(getStructName("A"), List.of(
                new Param(getStructType("B"), getVariable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructDefWithRepeatedVariableName() {
        /*
         * struct A {
         *   int b;
         *   bool b;
         * }
         */
        StructDef invalidStructDef = new StructDef(getStructName("A"), List.of(
                new Param(getIntType(), getVariable("b")),
                new Param(getBoolType(), getVariable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }


    @Test
    public void testStructAllocExpWithTooFewParams() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A {};
         */
        StructType aType = getStructType("A");
        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(aType, getVariable("a"))
        ));

        Expression expression = new StructAllocExp(aType, new StructActualParams(List.of()));

        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructAllocExpWithTooManyParams() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A {
         *   a: null,
         *   b: 6
         * };
         */
        StructType aType = getStructType("A");
        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(aType, getVariable("a"))
        ));

        Expression expression = new StructAllocExp(aType,
                new StructActualParams(List.of(
                        new StructActualParam(getVariable("a"), getNullExp()),
                        new StructActualParam(getVariable("b"), new IntLiteralExp(6)))
                ));

        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructAllocExpWithNoDefinedStructThrowsException() {
        // foo();
        Expression structAllocExp = new StructAllocExp(getStructType("foo"), new StructActualParams(List.of()));
        Statement structAllocExpStatement = new ExpressionStmt(structAllocExp);

        Program invalidProgram = new Program(List.of(), List.of(), List.of(structAllocExpStatement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructAllocExpWithMismatchedLabelThrowsException() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A {
         *   b: null
         * };
         */
        StructType aType = getStructType("A");
        StructDef structDef = new StructDef(getStructName("A"), List.of(
                new Param(aType, getVariable("a"))
        ));

        Expression expression = new StructAllocExp(aType,
                new StructActualParams(List.of(
                        new StructActualParam(getVariable("b"), getNullExp()))
                ));

        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructAllocExpWithMismatchedTypeThrowsException() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A {
         *   a: false
         * };
         */
        StructName name = getStructName("A");
        StructType type = getStructType("A");

        Variable variable = getVariable("a");

        StructDef structDef = new StructDef(name, List.of(
                new Param(type, variable)
        ));

        Expression expression = new StructAllocExp(type,
                new StructActualParams(List.of(
                        new StructActualParam(variable, new BoolLiteralExp(false)))
                ));

        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testDotExpWithoutStructThrowsException() {
        // false.foo;
        Expression expression = new DotExp(new BoolLiteralExp(false), getVariable("foo"));
        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testDotExpWithExplicitNullThrowsException() {
        // null.foo;
        Expression expression = new DotExp(getNullExp(), getVariable("foo"));
        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testDotExpWithUnknownStructFieldThrowsException() {
        /*
         * struct A {
         *   A a;
         * }
         *
         * new A {
         *   a: null
         * }.c;
         */
        StructName name = getStructName("A");
        StructType type = getStructType("A");

        Variable variable = getVariable("a");

        StructDef structDef = new StructDef(name, List.of(
                new Param(type, variable)
        ));

        Expression allocExp = new StructAllocExp(type,
                new StructActualParams(List.of(
                        new StructActualParam(variable, getNullExp()))
                ));

        Expression dotExp = new DotExp(allocExp, getVariable("c"));
        Statement statement = new ExpressionStmt(dotExp);

        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testExpStmtWithBinaryOpsThrowsExceptionOnBoolInGteExp() {
        /*
         * (false && (4 + 3 * 7 >= true)) || true;
         */
        Expression trueExp2 = new BoolLiteralExp(true);

        Expression intLiteral3 = new IntLiteralExp(3);
        Expression intLiteral7 = new IntLiteralExp(7);
        Expression binOpMult = new BinaryOpExp(intLiteral3, OperatorEnum.MULTIPLY, intLiteral7);
        Expression intLiteral4 = new IntLiteralExp(4);
        Expression binOpAdd = new BinaryOpExp(intLiteral4, OperatorEnum.PLUS, binOpMult);

        Expression binOpGte = new BinaryOpExp(binOpAdd, OperatorEnum.GREATER_THAN_EQUALS, trueExp2);
        Expression parenGteExp = new ParenExp(binOpGte);
        Expression falseExp = new BoolLiteralExp(false);

        Expression andExp = new BinaryOpExp(falseExp, OperatorEnum.AND, parenGteExp);
        Expression trueExp = new BoolLiteralExp(true);
        Expression parenAndExp = new ParenExp(andExp);
        Expression orExp = new BinaryOpExp(parenAndExp, OperatorEnum.OR, trueExp);

        Statement statement = new ExpressionStmt(orExp);

        Program invalidProgram = new Program(List.of(), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testExpStmtWithoutVardecThrowsException() {
        /*
         * foo;
         */

        Statement varExpStmt = new ExpressionStmt(new VariableExp(getVariable("foo")));
        Program invalidProgram = new Program(List.of(), List.of(), List.of(varExpStmt));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testAssignStmtWithNoVariableInScopeThrowsException() {
        /*
         * a = true;
         */
        Statement assign = new AssignStmt(getVariable("a"), new BoolLiteralExp(true));

        Program invalidProgram = new Program(List.of(), List.of(), List.of(assign));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testAssignStmtWithMismatchedTypesThrowsException() {
        /*
         * bool a = false;
         * a = 3;
         */
        Statement vardec = new VardecStmt(getBoolType(), getVariable("a"), new BoolLiteralExp(false));
        Statement assign = new AssignStmt(getVariable("a"), new IntLiteralExp(3));

        Program invalidProgram = new Program(List.of(), List.of(), List.of(vardec, assign));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testWhileStmtNonBoolConditionThrowsException() {
        // while (7) {}
        Program invalidProgram = new Program(List.of(), List.of(), List.of(
                new WhileStmt(new IntLiteralExp(7), new StmtBlock())));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testBreakStmtNotInLoopThrowsException() {
        // break;
        Program invalidProgram = new Program(List.of(), List.of(), List.of(new BreakStmt()));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testReturnStmtNotInFunctionDefThrowsException() {
        // return;
        Program invalidProgram = new Program(List.of(), List.of(), List.of(new ReturnStmt()));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testIfElseWithNonBooleanConditionThrowsException() {
        // if (7) {}
        IfElseStmt ifElseStmt = new IfElseStmt(new IntLiteralExp(7), new StmtBlock());

        Program invalidProgram = new Program(List.of(), List.of(), List.of(ifElseStmt));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testPrintlnStmtWithVoidThrowsException() {
        /*
         * func foo(): void {}
         * println(foo());
         */
        FunctionName foo = getFunctionName("foo");

        FunctionDef fooDef = new FunctionDef(foo, List.of(), getVoidType(), new StmtBlock());
        Statement printlnStmt = new PrintlnStmt(new FuncCallExp(foo, new CommaExp(List.of())));

        Program invalidProgram = new Program(List.of(), List.of(fooDef), List.of(printlnStmt));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testPrintlnStmtWithStructThrowsException() {
        /*
         * struct A {}
         * println(new A {});
         */
        StructName a = getStructName("A");
        StructType aType = getStructType("A");

        StructDef structDef = new StructDef(a, List.of());
        StructAllocExp allocationExp = new StructAllocExp(aType, new StructActualParams(List.of()));
        Statement printlnStmt = new PrintlnStmt(allocationExp);

        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(printlnStmt));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testIntVarAssignedBoolThrowsException() {
        /*
         * int foo = 6;
         * bool bar = true;
         * foo = bar;
         * 
         * What is this, Python? I think not!
         * Certified good one ^
         */

        Statement intVardecStmt = new VardecStmt(
                getIntType(),
                getVariable("foo"),
                new IntLiteralExp(6));
        Statement boolVardecStmt = new VardecStmt(
                getIntType(),
                getVariable("bar"),
                new BoolLiteralExp(true));
        Statement assignStmt = new AssignStmt(
                getVariable("foo"),
                new VariableExp(getVariable("bar")));
        Program invalidProgram = new Program(List.of(), List.of(), List.of(intVardecStmt, boolVardecStmt, assignStmt));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testBinExpWithInvalidTypes() {
        /*
         * (6 || 9);
         */

        Statement orExpStmt = new ExpressionStmt(
            new BinaryOpExp(
                new IntLiteralExp(6),
                OperatorEnum.OR,
                new IntLiteralExp(9)
            )
        );
        Program invalidProgram = new Program(List.of(), List.of(), List.of(orExpStmt));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testLogicalNotExpStmtWithoutBoolThrowsException() {
        // !3;
        Expression expression = new UnaryOpExp(OperatorEnum.NOT, new IntLiteralExp(3));
        Statement statement = new ExpressionStmt(expression);

        Program invalidProgram = new Program(List.of(), List.of(), List.of(statement));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testFunctionDefinitionReturnsIntInsteadOfBoolThrowsException() {
        /*
        *  func alwaysTrue(): bool {
        *       return 0;
        *   }
        */

        Expression intLiteral0 = new IntLiteralExp(0);
        Statement returnStmt0 = new ReturnStmt(intLiteral0);
        FunctionBody funcBody = new FunctionBody(List.of(returnStmt0));
        FunctionDef funcDef = new FunctionDef(
            getFunctionName("alwaysTrue"),
            new ArrayList<Param>(),
            getBoolType(),
            funcBody
        );

        Program invalidProgram = new Program(List.of(), List.of(funcDef), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testDuplicateFunctionSignaturesThrowsException() {
        /*
         * func alwaysTrue(): bool {
         *   return true;
         * }
         * 
         * func alwaysTrue(): bool {
         *   return false;
         * }
         */

        Expression boolTrue = new BoolLiteralExp(true);
        Statement returnStmtTrue = new ReturnStmt(boolTrue);
        StmtBlock funcBody = new StmtBlock(List.of(returnStmtTrue));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("alwaysTrue"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody);

        Expression boolTrue2 = new BoolLiteralExp(false);
        Statement returnStmtTrue2 = new ReturnStmt(boolTrue2);
        StmtBlock funcBody2 = new StmtBlock(List.of(returnStmtTrue2));
        FunctionDef funcDef2 = new FunctionDef(
                getFunctionName("alwaysTrue"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody2);

        Program invalidProgram = new Program(List.of(), List.of(funcDef, funcDef2), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testFunctionCallWithTooFewParamsThrowsException() {
        /*
         * func foo(int bar): void {}
         * foo();
         */
        FunctionName foo = new FunctionName("foo");
        FunctionDef fooDef = new FunctionDef(foo, List.of(new Param(getIntType(), getVariable("bar"))),
                getVoidType(), new StmtBlock());

        Expression fooFunctionCallNoParams = new FuncCallExp(foo, new CommaExp(List.of()));
        Statement fooFunctionCallExp = new ExpressionStmt(fooFunctionCallNoParams);

        Program invalidProgram = new Program(List.of(), List.of(fooDef), List.of(fooFunctionCallExp));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testFunctionCallWithWrongParamTypeThrowsException() {
        /*
         * func foo(int bar): void {}
         * foo(false);
         */
        FunctionName foo = new FunctionName("foo");
        FunctionDef fooDef = new FunctionDef(foo, List.of(new Param(getIntType(), getVariable("bar"))),
                getVoidType(), new StmtBlock());

        Expression fooFunctionCallNoParams = new FuncCallExp(foo, new CommaExp(List.of(new BoolLiteralExp(false))));
        Statement fooFunctionCallExp = new ExpressionStmt(fooFunctionCallNoParams);

        Program invalidProgram = new Program(List.of(), List.of(fooDef), List.of(fooFunctionCallExp));
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testVoidFunctionReturnsSomethingThrowsException() {
        /*
         * func voidFunc(): void {
         *   return 0;
         * }
         */

        Expression intLiteral0 = new IntLiteralExp(0);
        Statement returnStmt0 = new ReturnStmt(intLiteral0);
        FunctionBody funcBody = new FunctionBody(List.of(returnStmt0));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("voidFunc"),
                new ArrayList<Param>(),
                getVoidType(),
                funcBody);

        Program invalidProgram = new Program(List.of(), List.of(funcDef), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testFunctionCallsNotYetDefinedFunctionThrowsException() {
        /*
         * func firstFunc(): bool {
         *   return secondFunc();
         * }
         * 
         * func secondFunc(): bool {
         *   return false;
         * }
         */

        CommaExp args = new CommaExp(new ArrayList<Expression>());
        Expression secondFuncCallExp = new FuncCallExp(getFunctionName("secondFunc"), args);
        Statement returnSecondFunc = new ReturnStmt(secondFuncCallExp);
        StmtBlock funcBody = new StmtBlock(List.of(returnSecondFunc));
        FunctionDef funcDef = new FunctionDef(
                getFunctionName("firstFunc"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody);

        Expression boolTrue2 = new BoolLiteralExp(false);
        Statement returnStmtTrue2 = new ReturnStmt(boolTrue2);
        StmtBlock funcBody2 = new StmtBlock(List.of(returnStmtTrue2));
        FunctionDef funcDef2 = new FunctionDef(
                getFunctionName("secondFunc"),
                new ArrayList<Param>(),
                getBoolType(),
                funcBody2);

        Program invalidProgram = new Program(List.of(), List.of(funcDef, funcDef2), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    // Integration test

    @Test
    public void testTokenizeParseTypecheckProgramWithoutException() {
        String input = ResourceUtil.readProgramInputFile();
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

    @Test
    public void testTokenizeParseTypecheckProgram2WithoutException() {
        String input = ResourceUtil.readProgram2InputFile();
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

    @Ignore("Method to screenshot typechecker error messages")
    @Test
    public void testTokenizeParseTypecheckInvalidProgram() {
        String input = """
                struct A {
                    A a;
                }
                
                A a = new A {
                    a: new B {
                        a: new B {}
                    }
                };""";
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);
            Typechecker.typecheckProgram(program);
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

}

package refraff.typechecker;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import refraff.Sourced;
import refraff.parser.*;
import refraff.parser.struct.*;
import refraff.parser.type.BoolType;
import refraff.parser.type.IntType;
import refraff.parser.type.StructType;
import refraff.parser.type.VoidType;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.statement.*;
import refraff.tokenizer.Token;
import refraff.tokenizer.Tokenizer;
import refraff.tokenizer.TokenizerException;
import refraff.util.ResourceUtil;

import java.util.List;

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
    public void testLogicalNotExpStmt() {
        // !true;
        Expression expression = new UnaryOpExp(OperatorEnum.NOT, new BoolLiteralExp(true));
        Statement statement = new ExpressionStmt(expression);

        Program program = new Program(List.of(), List.of(), List.of(statement));
        testDoesNotThrowTypecheckerException(program);

        assertTrue(expression.getExpressionType().hasTypeEquality(new BoolType()));
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

//    @Test
//    public void testStructNameIsReusedInVardecThrowsException() {
//        /*
//         * struct foo = {};
//         * int foo = 6;
//         */
//
//        StructDef structDef = new StructDef(new StructName("foo"), new ArrayList<Param>());
//        Statement vardecStmt = new VardecStmt(
//                getIntType(),
//                getVariable("foo"),
//                new IntLiteralExp(6));
//        Program invalidProgram = new Program(List.of(structDef), List.of(), List.of(vardecStmt));
//        testThrowsTypecheckerException(invalidProgram);
//    }

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
    public void testIntVarAssignedBoolThrowsException() {
        /*
         * int foo = 6;
         * bool bar = true;
         * foo = bar;
         * 
         * What is this Python? I think not!
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

    // Integration test

    @Ignore("Should be ignored until typechecker is complete.")
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

    //@Ignore("Method to screenshot typechecker error messages")
    @Test
    public void testTokenizeParseTypecheckInvalidProgram() {
        String input = "null.var;";
        try {
            List<Sourced<Token>> sourcedTokens = new Tokenizer(input).tokenize();
            Program program = Parser.parseProgram(sourcedTokens);

            ExpressionStmt expressionStmt = (ExpressionStmt) program.getStatements().get(0);
            DotExp dotExp = (DotExp) expressionStmt.getExpression();
            System.out.println(dotExp.getLeftExp().getExpressionType());

            Typechecker.typecheckProgram(program);
        } catch (TokenizerException | ParserException | TypecheckerException ex) {
            fail(ex.toString());
        }
    }

}

package refraff.typechecker;

import org.junit.Test;
import refraff.parser.Program;
import refraff.parser.Variable;
import refraff.parser.struct.Param;
import refraff.parser.struct.StructDef;
import refraff.parser.struct.StructName;
import refraff.parser.type.BoolType;
import refraff.parser.type.IntType;
import refraff.parser.type.StructType;
import refraff.parser.type.VoidType;
import refraff.tokenizer.IdentifierToken;
import refraff.tokenizer.IntLiteralToken;
import refraff.tokenizer.Token;
import refraff.tokenizer.reserved.BoolToken;
import refraff.tokenizer.reserved.FalseToken;
import refraff.tokenizer.symbol.AndToken;
import refraff.tokenizer.symbol.AssignmentToken;
import refraff.tokenizer.symbol.DivisionToken;
import refraff.tokenizer.symbol.DotToken;
import refraff.tokenizer.symbol.GreaterThanEqualsToken;
import refraff.tokenizer.symbol.LeftParenToken;
import refraff.tokenizer.symbol.MinusToken;
import refraff.tokenizer.symbol.MultiplyToken;
import refraff.tokenizer.symbol.NotToken;
import refraff.tokenizer.symbol.OrToken;
import refraff.tokenizer.symbol.PlusToken;
import refraff.tokenizer.symbol.RightParenToken;
import refraff.tokenizer.symbol.SemicolonToken;
import refraff.parser.expression.*;
import refraff.parser.expression.primaryExpression.*;
import refraff.parser.operator.OperatorEnum;
import refraff.parser.statement.*;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TypecheckerTest {

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
        StructDef structDef = new StructDef(new StructName("A"), List.of(
                new Param(new StructType(new StructName("A")), new Variable("a"))
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
         */
        StructDef structDef1 = new StructDef(new StructName("A"), List.of(
                new Param(new IntType(), new Variable("foo")),
                new Param(new BoolType(), new Variable("bar"))
        ));

        StructDef structDef2 = new StructDef(new StructName("B"), List.of(
                new Param(new StructType(new StructName("A")), new Variable("a"))
        ));

        Program program = new Program(List.of(structDef1, structDef2), List.of(), List.of());
        testDoesNotThrowTypecheckerException(program);
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

    

    // Test invalid inputs

    private void testThrowsTypecheckerException(Program invalidProgram) {
        assertThrows(TypecheckerException.class, () -> Typechecker.typecheckProgram(invalidProgram));
    }

    @Test
    public void testStructDefWithVoidTypeVariable() {
        /*
         * struct A {
         *   Void b;
         * }
         */
        StructDef invalidStructDef = new StructDef(new StructName("A"), List.of(
                new Param(new VoidType(), new Variable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
        testThrowsTypecheckerException(invalidProgram);
    }

    @Test
    public void testStructDefWithUndefinedStructTypeVariable() {
        /*
         * struct A {
         *   B b;
         * }
         */
        StructDef invalidStructDef = new StructDef(new StructName("A"), List.of(
                new Param(new StructType(new StructName("B")), new Variable("b"))
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
        StructDef invalidStructDef = new StructDef(new StructName("A"), List.of(
                new Param(new IntType(), new Variable("b")),
                new Param(new BoolType(), new Variable("b"))
        ));

        Program invalidProgram = new Program(List.of(invalidStructDef), List.of(), List.of());
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
}

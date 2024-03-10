package refraff.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;

import org.junit.Test;

import refraff.parser.function.FunctionDef;
import refraff.parser.function.FunctionName;
import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.*;
import refraff.parser.type.*;
import refraff.parser.expression.*;
import refraff.parser.statement.*;
import refraff.parser.structure.Param;
import refraff.parser.structure.StructDef;
import refraff.parser.operator.*;
import refraff.parser.expression.primaryExpression.*;


public class ParserTest {

    private Token[] toArray(Token... tokens) {
        return tokens;
    }

    private ParseResult<Program> parseProgram(Token... tokens) throws ParserException {
        return new ParseResult<>(Parser.parseProgram(tokens), tokens.length);
    }

    private <T extends AbstractSyntaxTreeNode> Optional<ParseResult<T>> testParsesWithoutException(
            ParsingFunction<Parser, Optional<ParseResult<T>>> parsingFunction, Token... tokens) {
        try {
            final Parser parser = new Parser(tokens);
            return parsingFunction.apply(parser);
        } catch (ParserException ex) {
            fail(ex);
        }

        throw new IllegalStateException("This will never occur.");
    }

    private void testProgramMatchesExpectedResult(Program expectedValue, Token... tokens) {
        testMatchesExpectedResult(parser -> Optional.of(parseProgram(tokens)), expectedValue, tokens);
    }

    private <T extends AbstractSyntaxTreeNode> void testMatchesExpectedResult(ParsingFunction<Parser,
            Optional<ParseResult<T>>> parsingFunction, T expectedValue, Token... tokens) {
        Optional<ParseResult<T>> optionalParseResult = testParsesWithoutException(parsingFunction, tokens);
        assertFalse("Parse result must contain something", optionalParseResult.isEmpty());

        ParseResult<T> expectedResult = new ParseResult<>(expectedValue, tokens.length);
        ParseResult<T> actualResult = optionalParseResult.get();

        assertEquals(expectedResult, actualResult);
    }
    
    // Test valid inputs

    private void testExpressionMatchesExpectedResult(Expression expression, Token... inputs) {
        testMatchesExpectedResult(parser -> parser.parseExp(0), expression, inputs);
    }

    @Test
    public void testTrueBoolLiteralExp() {
        testExpressionMatchesExpectedResult(new BoolLiteralExp(true), new TrueToken());
    }

    @Test
    public void testFalseBoolLiteralExp() {
        testExpressionMatchesExpectedResult(new BoolLiteralExp(false), new FalseToken());
    }

    @Test
    public void testIntLiteralExp() {
        testExpressionMatchesExpectedResult(new IntLiteralExp(7), new IntLiteralToken("7"));
    }

    private void testTypeMatchesExpectedResult(Type expected, Token input) {
        testMatchesExpectedResult(parser -> parser.parseType(0), expected, input);
    }

    @Test
    public void testTypeEquals() {
        assertEquals(new IntType(),
                     new IntType());
    }

    @Test
    public void testParseIntType() {
        testTypeMatchesExpectedResult(new IntType(), new IntToken());
    }

    @Test
    public void testParseBoolType() {
        testTypeMatchesExpectedResult(new BoolType(), new BoolToken());
    }

    @Test
    public void testParseVoidType() {
        testTypeMatchesExpectedResult(new VoidType(), new VoidToken());
    }

    @Test
    public void testParseStructNameType() {
        testTypeMatchesExpectedResult(new StructName("a"), new IdentifierToken("a"));
    }

    private void testFunctionDef(FunctionName functionName, List<Param> params, Type returnType,
                                 StmtBlock stmtBlock, Token... tokens) {
        FunctionDef expectedFunctionDef = new FunctionDef(functionName, params, returnType, stmtBlock);
        Program expectedProgram = new Program(List.of(), List.of(expectedFunctionDef), List.of());

        testProgramMatchesExpectedResult(expectedProgram, tokens);
    }

    @Test
    public void testFunctionDefWithNoParams() {
        // func a() : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new RightParenToken(), new ColonToken(),
                new IntToken(), new LeftBraceToken(), new RightBraceToken());

        testFunctionDef(new FunctionName("a"), List.of(), new IntType(), new StmtBlock(List.of()), input);
    }

    @Test
    public void testFunctionDefWithOneParam() {
        // func a(int b) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken());

        testFunctionDef(new FunctionName("a"), List.of(
                new Param(new IntType(), new Variable("b"))
        ), new IntType(), new StmtBlock(List.of()), input);
    }

    @Test
    public void testFunctionDefWithMultipleParams() {
        // func a(int b, custom c) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"), new LeftParenToken(),
                new IntToken(), new IdentifierToken("b"), new CommaToken(),
                new IdentifierToken("custom"), new IdentifierToken("c"), new RightParenToken(),
                new ColonToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken());

        testFunctionDef(new FunctionName("a"), List.of(
                new Param(new IntType(), new Variable("b")),
                new Param(new StructName("custom"), new Variable("c"))
        ), new IntType(), new StmtBlock(List.of()), input);
    }

    private void testStatementMatchesExpected(Statement statement, Token... input) {
        Program expectedProgram = new Program(List.of(), List.of(), List.of(statement));
        testProgramMatchesExpectedResult(expectedProgram, input);
    }

    @Test
    public void testParseVardec() throws ParserException {
        final Token[] input = new Token[] {
            new IntToken(),
            new IdentifierToken("variableName"),
            new AssignmentToken(),
            new IntLiteralToken("6"),
            new SemicolonToken()
        };
        final Parser parser = new Parser(input);
        assertEquals(Optional.of(new ParseResult<VardecStmt>(new VardecStmt(
                                    new IntType(),
                                    new Variable("variableName"),
                                    new IntLiteralExp(6)
                                ), 5)),
                    parser.parseVardec(0));
    }

    @Test
    public void testParseProgramWithVardec() throws ParserException {
        /*
         * int variableName = 6;
         * just a different entry point
         */
        final Token[] input = new Token[] {
            new IntToken(),
            new IdentifierToken("variableName"),
            new AssignmentToken(),
            new IntLiteralToken("6"),
            new SemicolonToken()
        };
        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        statements.add(new VardecStmt(
            new IntType(),
            new Variable("variableName"),
            new IntLiteralExp(6)
            )
        );

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 5),
                    parser.parseProgram(0));
    }

    @Test
    public void testParseIfNoElse() {
        // if (3)
        //    x = false;
        Token[] input = toArray(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken(),
                new IdentifierToken("x"), new AssignmentToken(), new FalseToken(), new SemicolonToken());

        AssignStmt ifBody = new AssignStmt(new Variable("x"), new BoolLiteralExp(false));
        IfElseStmt ifElseStmt = new IfElseStmt(new IntLiteralExp(3), ifBody);

        testStatementMatchesExpected(ifElseStmt, input);
    }

    @Test
    public void testParseIfElse() {
        // if (3)
        //    x = false;
        // else
        //    x = true;
        Token[] input = toArray(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken(),
                new IdentifierToken("x"), new AssignmentToken(), new FalseToken(), new SemicolonToken(), new ElseToken(),
                new IdentifierToken("x"), new AssignmentToken(), new TrueToken(), new SemicolonToken());


        AssignStmt ifBody = new AssignStmt(new Variable("x"), new BoolLiteralExp(false));
        AssignStmt elseBody = new AssignStmt(new Variable("x"), new BoolLiteralExp(true));
        IfElseStmt ifElseStmt = new IfElseStmt(new IntLiteralExp(3), ifBody, elseBody);

        testStatementMatchesExpected(ifElseStmt, input);
    }

    @Test
    public void testParseEmptyStatementBlock() {
        // {}
        Token[] input = toArray(new LeftBraceToken(), new RightBraceToken());

        StmtBlock stmtBlock = new StmtBlock(List.of());
        testStatementMatchesExpected(stmtBlock, input);
    }

    @Test
    public void testParseNonEmptyStatementBlock() {
        // { int variableName = 6; }
        Token[] input = toArray(new LeftBraceToken(),
                new IntToken(),
                new IdentifierToken("variableName"),
                new AssignmentToken(),
                new IntLiteralToken("6"),
                new SemicolonToken(),
                new RightBraceToken());

        StmtBlock stmtBlock = new StmtBlock(List.of(
                new VardecStmt(new IntType(), new Variable("variableName"), new IntLiteralExp(6)))
        );
        testStatementMatchesExpected(stmtBlock, input);
    }

    @Test
    public void testParseProgramWithStructDef() throws ParserException {
        /*
         * struct Node {
         *    int value;
         *    Node rest;
         * }
         */
        final Token[] input = new Token[] {
                new StructToken(),
                new IdentifierToken("Node"),
                new LeftBraceToken(),
                new IntToken(),
                new IdentifierToken("value"),
                new SemicolonToken(),
                new IdentifierToken("Node"),
                new IdentifierToken("rest"),
                new SemicolonToken(),
                new RightBraceToken()
        };
        final Parser parser = new Parser(input);

        final List<Param> params = new ArrayList<>();

        params.add(new Param(
            new IntType(),
            new Variable("value")
        ));

        params.add(new Param(
            new StructName("Node"),
            new Variable("rest")
        ));

        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<StructDef> structDefs = new ArrayList<>();

        structDefs.add(new StructDef(
                new StructName("Node"),
                params
            )
        );

        final List<Statement> statements = new ArrayList<>();

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 10),
                parser.parseProgram(0));
    }

    @Test
    public void testProgramWithDotOpExpression() throws ParserException {
        // retval = example.result;
        final Token[] input = new Token[] {
                new IdentifierToken("retval"),
                new AssignmentToken(),
                new IdentifierToken("example"),
                new DotToken(),
                new IdentifierToken("result"),
                new SemicolonToken()
        };
        final Parser parser = new Parser(input);
        final List<StructDef> structDefs = new ArrayList<>();
        final List<FunctionDef> functionDefs = new ArrayList<>();
        final List<Statement> statements = new ArrayList<>();

        BinaryOpExp dotExp = new BinaryOpExp(new VariableExp("example"), new DotOp(), new VariableExp("result"));

        statements.add(new AssignStmt(
                new Variable("retval"),
                dotExp));

        assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 6),
                parser.parseProgram(0));
    }

    // @Test
    // public void testProgramWithMultOpExpression() throws ParserException {
    //     // retval = retval * 2;
    //     final Token[] input = new Token[] {
    //             new IdentifierToken("retval"),
    //             new AssignmentToken(),
    //             new IdentifierToken("retval"),
    //             new MultiplyToken(),
    //             new IntLiteralToken("2"),
    //             new SemicolonToken()
    //     };
    //     final Parser parser = new Parser(input);
    //     final List<StructDef> structDefs = new ArrayList<>();
    //     final List<FunctionDef> functionDefs = new ArrayList<>();
    //     final List<Statement> statements = new ArrayList<>();

    //     BinaryOpExp multExp = new BinaryOpExp(new VariableExp("retval"), new MultiplyOp(), new IntLiteralExp(1));

    //     statements.add(new AssignStmt(
    //             new Variable("retval"),
    //             multExp)
    //     );

    //     assertEquals(new ParseResult<>(new Program(structDefs, functionDefs, statements), 6),
    //             parser.parseProgram(0));
    // }

    // @Test
    // public void testProgramWithDotOpExpression() throws ParserException {
    //     // list = list.rest;
    // }

    // @Test
    // public void testParseProgramWithWhileLoop() throws ParserException {
    //     /*
    //      *   while (list != null) {
    //      *     retval = retval + 1;
    //      *     list = list.rest;
    //      *   }
    //      */
    //     final Token[] input = new Token[] {
    //             new WhileToken(),
    //             new LeftParenToken(),
    //             new IdentifierToken("list"),
    //             new NotEqualsToken(),
    //             new NullToken(),
    //             new LeftBraceToken(),
    //             new IdentifierToken("retval"),
    //             new AssignmentToken(),
    //             new IdentifierToken("retval"),
    //             new PlusToken(),
    //             new IntLiteralToken("1"),
    //             new SemicolonToken(),
    //             new IdentifierToken("list"),
    //             new AssignmentToken(),
    //             new IdentifierToken("list"),
    //             new DotToken(),
    //             new IdentifierToken("rest"),
    //             new SemicolonToken(),
    //             new RightBraceToken()
    //     };
    //     final Parser parser = new Parser(input);

    //     final List<Param> params = new ArrayList<>();

    //     params.add(new Param(
    //             new IntType(),
    //             new Variable("value")));

    //     params.add(new Param(
    //             new StructName(new Variable("Node")),
    //             new Variable("rest")));

    //     final List<StructDef> structDefs = new ArrayList<>();

    //     structDefs.add(new StructDef(
    //             new StructName(new Variable("Node")),
    //             params));

    //     final List<Statement> statements = new ArrayList<>();

    //     assertEquals(new ParseResult<>(new Program(structDefs, statements), 10),
    //             parser.parseProgram(0));
    // }

    // Test invalid inputs

    private void testProgramParsesWithException(Token... tokens) {
        assertThrows(ParserException.class, () -> parseProgram(tokens));
    }

    @Test
    public void testFunctionDefWithNoFunctionNameThrowsException() {
        // func
        testProgramParsesWithException(new FuncToken());
    }

    @Test
    public void testFunctionDefWithNoLeftParenThrowsException() {
        // func a
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"));
    }

    @Test
    public void testFunctionDefWithNoParameterAfterCommaThrowsException() {
        // func a(int b, )
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new CommaToken(), new RightParenToken());
    }

    @Test
    public void testFunctionDefWithNoRightParenThrowsException() {
        // func a(int b :
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new ColonToken());
    }


    @Test
    public void testFunctionDefWithNoColonThrowsException() {
        // func a(int b) int
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new IntToken());
    }

    @Test
    public void testFunctionDefWithNoReturnTypeThrowsException() {
        // func a(int b) :
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken());
    }

    @Test
    public void testFunctionDefWithNoFunctionBodyThrowsException() {
        // func a(int b) : int
        testProgramParsesWithException(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken(), new IntToken());
    }

    @Test
    public void testIfWithNoLeftParenThrowsException() {
        // if 3
        testProgramParsesWithException(new IfToken(), new IntLiteralToken("3"));
    }

    @Test
    public void testIfWithNoConditionThrowsException() {
        // if ()
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new RightParenToken());
    }

    @Test
    public void testIfWithNoRightParenThrowsException() {
        // if (3
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"));
    }

    @Test
    public void testIfWithNoIfStatementBodyThrowsException() {
        // if (3)
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken());
    }

    @Test
    public void testIfWithNoElseStatementBodyThrowsException() {
        // if (3)
        //   x = false;
        // else
        testProgramParsesWithException(new IfToken(), new LeftParenToken(), new IntLiteralToken("3"), new RightParenToken(),
                new IdentifierToken("x"), new AssignmentToken(), new FalseToken(), new SemicolonToken(), new ElseToken());

    }

}

package refraff.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;

import org.junit.Test;

import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.*;
import refraff.parser.type.*;
import refraff.parser.expression.*;
import refraff.parser.statement.*;


public class ParserTest {

    private Token[] toArray(Token... tokens) {
        return tokens;
    }

    private ParseResult<Program> parseProgram(Token... tokens) throws ParserException {
        final Parser parser = new Parser(tokens);
        return parser.parseProgram(0);
    }

    private ParseResult<Program> testProgramParsesWithoutException(Token... tokens) {
        try {
            return parseProgram(tokens);
        } catch (ParserException ex) {
            fail(ex);
        }

        throw new IllegalStateException("This will never occur.");
    }

    private void testProgramParsesWithException(Token... tokens) {
        try {
            parseProgram(tokens);
            fail("Parser exception should have been thrown, but was not.");
        } catch (ParserException ex) {
            // ignored, we succeed
        }
    }
    
    // Test valid inputs
    @Test
    public void testTypeEquals() {
        assertEquals(new IntType(),
                     new IntType());
    }

    @Test
    public void testParseIntType() throws ParserException {
        final Token[] input = new Token[] {
            new IntToken()
        };
        final Parser parser = new Parser(input);
        assertEquals(Optional.of(new ParseResult<Type>(new IntType(), 1)),
                     parser.parseType(0));
    }

    @Test
    public void testFunctionDefWithNoParams() {
        // func a(int b) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new RightParenToken(), new ColonToken(),
                new IntToken(), new LeftBraceToken(), new RightBraceToken());

        FunctionDef expectedFunctionDef = new FunctionDef(
                new FunctionName("a"),
                List.of(),
                new IntType(),
                new StatementBlock(List.of()));

        Program expectedProgram = new Program(List.of(), List.of(expectedFunctionDef), List.of());

        ParseResult<Program> expectedParseResult = new ParseResult<>(expectedProgram, input.length);
        ParseResult<Program> actualParseResult = testProgramParsesWithoutException(input);

        assertEquals(expectedParseResult, actualParseResult);
    }

    @Test
    public void testFunctionDefWithOneParam() {
        // func a(int b) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"), new RightParenToken(),
                new ColonToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken());

        FunctionDef expectedFunctionDef = new FunctionDef(
                new FunctionName("a"),
                List.of(
                    new Param(new IntType(), new Variable("b"))
                ),
                new IntType(),
                new StatementBlock(List.of()));

        Program expectedProgram = new Program(List.of(), List.of(expectedFunctionDef), List.of());

        ParseResult<Program> expectedParseResult = new ParseResult<>(expectedProgram, input.length);
        ParseResult<Program> actualParseResult = testProgramParsesWithoutException(input);

        assertEquals(expectedParseResult, actualParseResult);
    }

    @Test
    public void testFunctionDefWithMultipleParams() {
        // func a(int b, custom c) : int {}
        Token[] input = toArray(new FuncToken(), new IdentifierToken("a"),
                new LeftParenToken(), new IntToken(), new IdentifierToken("b"),
                new CommaToken(), new IdentifierToken("custom"),
                new IdentifierToken("c"), new RightParenToken(),
                new ColonToken(), new IntToken(), new LeftBraceToken(), new RightBraceToken());

        FunctionDef expectedFunctionDef = new FunctionDef(
                new FunctionName("a"),
                List.of(
                        new Param(new IntType(), new Variable("b")),
                        new Param(new StructName("custom"), new Variable("c"))
                ),
                new IntType(),
                new StatementBlock(List.of()));

        Program expectedProgram = new Program(List.of(), List.of(expectedFunctionDef), List.of());

        ParseResult<Program> expectedParseResult = new ParseResult<>(expectedProgram, input.length);
        ParseResult<Program> actualParseResult = testProgramParsesWithoutException(input);

        assertEquals(expectedParseResult, actualParseResult);
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
        assertEquals(Optional.of(new ParseResult<>(new VardecStmt(
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
    public void testParseEmptyStatementBlock() {
        // {}
        Token[] input = toArray(new LeftBraceToken(),
                new IntToken(),
                new IdentifierToken("variableName"),
                new AssignmentToken(),
                new IntLiteralToken("6"),
                new SemicolonToken(),
                new RightBraceToken());

        StatementBlock statementBlock = new StatementBlock(List.of(
                new VardecStmt(new IntType(), new Variable("variableName"), new IntLiteralExp(6)))
        );
        Program expectedProgram = new Program(List.of(), List.of(), List.of(statementBlock));

        ParseResult<Program> expectedParseResult = new ParseResult<>(expectedProgram, input.length);
        ParseResult<Program> actualParseResult = testProgramParsesWithoutException(input);

        assertEquals(expectedParseResult, actualParseResult);
    }

    @Test
    public void testParseNonEmptyStatementBlock() {
        // {}
        Token[] input = toArray(new LeftBraceToken(), new RightBraceToken());

        StatementBlock statementBlock = new StatementBlock(List.of());
        Program expectedProgram = new Program(List.of(), List.of(), List.of(statementBlock));

        ParseResult<Program> expectedParseResult = new ParseResult<>(expectedProgram, input.length);
        ParseResult<Program> actualParseResult = testProgramParsesWithoutException(input);

        assertEquals(expectedParseResult, actualParseResult);
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

    // @Test
    // public void testProgramWithPlusOpExpression() throws ParserException {
    //     // retval = retval + 1;
    //     final Token[] input = new Token[] {
    //             new IdentifierToken("retval"),
    //             new AssignmentToken(),
    //             new IdentifierToken("retval"),
    //             new PlusToken(),
    //             new IntLiteralToken("1"),
    //             new SemicolonToken()
    //     };
    //     final Parser parser = new Parser(input);
    //     final List<StructDef> structDefs = new ArrayList<>();
    //     final List<Statement> statements = new ArrayList<>();

    //     BinaryOpExp addExp = new BinaryOpExp(new VariableExp("retval"), new PlusOp(), new IntLiteralExp(1));

    //     statements.add(new AssignStmt(
    //             new Variable("retval"),
    //             addExp)
    //     );

    //     assertEquals(new ParseResult<>(new Program(structDefs, statements), 6),
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

}

package refraff.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;

import org.jacoco.agent.rt.internal_4742761.asm.tree.ParameterNode;
import org.junit.Test;

import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.*;
import refraff.parser.Parser;
import refraff.parser.type.*;
import refraff.parser.expression.*;
import refraff.parser.statement.*;
import refraff.parser.operator.*;


public class ParserTest {
    
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
        final List<Statement> statements = new ArrayList<>();

        statements.add(new VardecStmt(
            new IntType(),
            new Variable("variableName"),
            new IntLiteralExp(6)
            )
        );

        assertEquals(new ParseResult<>(new Program(structDefs, statements), 5),
                    parser.parseProgram(0));
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
            new StructName(new Variable("Node")),
            new Variable("rest")
        ));

        final List<StructDef> structDefs = new ArrayList<>();

        structDefs.add(new StructDef(
                new StructName(new Variable("Node")),
                params
            )
        );

        final List<Statement> statements = new ArrayList<>();

        assertEquals(new ParseResult<>(new Program(structDefs, statements), 10),
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
}

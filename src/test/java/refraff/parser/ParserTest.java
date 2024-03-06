package refraff.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.tokenizer.*;
import refraff.parser.Parser;
import refraff.parser.type.*;
import refraff.parser.expression.*;
import refraff.parser.statement.*;


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
    public void testParseProgram() throws ParserException {
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
        final List<Statement> statements = new ArrayList<>();

        statements.add(new VardecStmt(
            new IntType(),
            new Variable("variableName"),
            new IntLiteralExp(6)
            )
        );

        assertEquals(new ParseResult<>(new Program(statements), 5),
                    parser.parseProgram(0));
    }
}

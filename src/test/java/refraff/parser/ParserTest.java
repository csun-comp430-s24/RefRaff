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
    public void testVardec() throws ParserException {
        final Token[] input = new Token[] {
            new IntToken(),
            new IdentifierToken("variableName"),
            new AssignmentToken(),
            new IntLiteralToken("6"),
            new SemicolonToken()
        };
        final Parser parser = new Parser(input);
        assertEquals(Optional.of(new ParseResult<>(new Vardec(
                                    new IntType(),
                                    new Variable("variableName"),
                                    new IntLiteralExp(6)
                                ), 5)),
                    parser.parseVardec(0));
    }

    // private Program getAbstractSyntaxTreeWithoutException(String input) 
    //         throws TokenizerException {

    //     Tokenizer tokenizer = new Tokenizer(input);
    //     List<Token> tokenList = tokenizer.tokenize();
    //     Token[] tokens = new Token[tokenList.size()];
    //     tokenList.toArray(tokens);
    //     // Yikes. I made it an array, because that's what it is in github example
    //     // But this is a mess
        
    //     try {
    //         return Parser.parseProgram(tokens);
    //     } catch (ParserException ex) {
    //         fail(ex.getMessage());
    //     }

    //     throw new IllegalStateException("This will never be called.");
    // }

    // private void testParserInputMatchesExpectedTree(String input, Token... expectedTokensArray) 
    //         throws TokenizerException {
    //     Program program = getAbstractSyntaxTreeWithoutException(input);
    //     // Program expectedProgram = Arrays.asList(expectedTokensArray);

    //     // assertEquals(expectedTokens, actualTokens);
    // }

    // @Test
    // public void testParserParsesWithoutException() 
    //         throws TokenizerException {
    //     String input = "int variableName = 6;";
    //     testParserInputMatchesExpectedTree(input);
    // }
}

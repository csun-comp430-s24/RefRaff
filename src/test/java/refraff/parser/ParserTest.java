package refraff.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import refraff.tokenizer.Token;
import refraff.tokenizer.Tokenizer;
import refraff.tokenizer.TokenizerException;
import refraff.parser.Parser;

public class ParserTest {
    
    // Test valid inputs

    private List<Token> getAbstractSyntaxTreeWithoutException(String input) 
            throws TokenizerException {

        Tokenizer tokenizer = new Tokenizer(input);
        // Should I make this immutable?
        List<Token> tokens = tokenizer.tokenize();
        
        try {
            return new Parser(input).parseProgram();
        } catch (ParserException ex) {
            fail(ex.getMessage());
        }

        throw new IllegalStateException("This will never be called.");
    }

    private void testParserInputMatchesExpectedTree(String input, Token... expectedTokensArray) 
            throws TokenizerException {
        List<Token> actualTokens = getAbstractSyntaxTreeWithoutException(input);
        List<Token> expectedTokens = Arrays.asList(expectedTokensArray);

        assertEquals(expectedTokens, actualTokens);
    }

    @Test
    public void testParserParsesWithoutException() 
            throws TokenizerException {
        String input = "int variableName = 6;";
        testParserInputMatchesExpectedTree(input);
    }
}

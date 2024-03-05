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

    private Program getAbstractSyntaxTreeWithoutException(String input) 
            throws TokenizerException {

        Tokenizer tokenizer = new Tokenizer(input);
        // Should I make this immutable?
        List<Token> tokenList = tokenizer.tokenize();
        Token[] tokens = new Token[tokenList.size()];
        tokenList.toArray(tokens);
        
        try {
            return Parser.parseProgram(tokens);
        } catch (ParserException ex) {
            fail(ex.getMessage());
        }

        throw new IllegalStateException("This will never be called.");
    }

    private void testParserInputMatchesExpectedTree(String input, Token... expectedTokensArray) 
            throws TokenizerException {
        Program program = getAbstractSyntaxTreeWithoutException(input);
        // Program expectedProgram = Arrays.asList(expectedTokensArray);

        // assertEquals(expectedTokens, actualTokens);
    }

    @Test
    public void testParserParsesWithoutException() 
            throws TokenizerException {
        String input = "int variableName = 6;";
        testParserInputMatchesExpectedTree(input);
    }
}

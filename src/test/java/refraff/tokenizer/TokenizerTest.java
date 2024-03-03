package refraff.tokenizer;

import org.junit.Test;

import refraff.tokenizer.reserved.FuncToken;
import refraff.tokenizer.reserved.BoolToken;
import refraff.tokenizer.reserved.StructToken;
import refraff.tokenizer.reserved.IntToken;
import refraff.tokenizer.symbol.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TokenizerTest {

    // Test valid inputs

    private void testTokenizerInputMatchesExpectedTokens(String input, Token... expectedTokensArray) {
        assertDoesNotThrow(() -> {
            List<Token> actualTokens = new Tokenizer(input).tokenize();
            List<Token> expectedTokens = Arrays.asList(expectedTokensArray);

            assertEquals(expectedTokens, actualTokens);
        });
    }

    @Test
    public void testTokenizeEmptyString() {
        testTokenizerInputMatchesExpectedTokens("");
    }

    @Test
    public void testTokenizeWhitespaceString() {
        testTokenizerInputMatchesExpectedTokens("    \t\n\r      ");
    }

    @Test
    public void testTokenizeComma() {
        testTokenizerInputMatchesExpectedTokens(",", new CommaToken());
    }

    @Test
    public void testTokenizeLeftBrace() {
        testTokenizerInputMatchesExpectedTokens("{", new LeftBraceToken());
    }

    @Test
    public void testTokenizeRightBrace() {
        testTokenizerInputMatchesExpectedTokens("}", new RightBraceToken());
    }

    @Test
    public void testTokenizeLogicalAnd() {
        testTokenizerInputMatchesExpectedTokens("&&", new AndToken());
    }

    @Test
    public void testTokenizeLogicalOr() {
        testTokenizerInputMatchesExpectedTokens("||", new OrToken());
    }

    @Test
    public void testTokenizeLessThanEquals() {
        testTokenizerInputMatchesExpectedTokens("<=", new LessThanEqualsToken());
    }

    @Test
    public void testTokenizeLessThan() {
        testTokenizerInputMatchesExpectedTokens("<", new LessThanToken());
    }

    @Test
    public void testTokenizeDoubleEquals() {
        testTokenizerInputMatchesExpectedTokens("==", new DoubleEqualsToken());
    }

    @Test
    public void testTokenizeAssignment() {
        testTokenizerInputMatchesExpectedTokens("=", new AssignmentToken());
    }

    @Test
    public void testTokenizeSemicolon() {
        testTokenizerInputMatchesExpectedTokens(";", new SemicolonToken());
    }

    @Test
    public void testTokenizeLeftParenToken() {
        testTokenizerInputMatchesExpectedTokens("(", new LeftParenToken());
    }

    @Test
    public void testTokenizeRightParenToken() {
        testTokenizerInputMatchesExpectedTokens(")", new RightParenToken());
    }

    @Test
    public void testTokenizeDotToken() {
        testTokenizerInputMatchesExpectedTokens(".", new DotToken());
    }

    @Test
    public void testMultiplyToken() {
        testTokenizerInputMatchesExpectedTokens("*", new MultiplyToken());
    }

    @Test
    public void testTokenizeDivisionToken() {
        testTokenizerInputMatchesExpectedTokens("/", new DivisionToken());
    }

    @Test
    public void testTokenizePlusToken() {
        testTokenizerInputMatchesExpectedTokens("+", new PlusToken());
    }

    @Test
    public void testTokenizeMinusToken() {
        testTokenizerInputMatchesExpectedTokens("-", new MinusToken());
    }

    // @Test
    // public void testTokenizeNotEqualToken() {
    //     testTokenizerInputMatchesExpectedTokens(")", new NotEqualToken());
    // }

    // @Test
    // public void testTokenizeNotToken() {
    //     testTokenizerInputMatchesExpectedTokens(")", new NotToken());
    // }

    // @Test
    // public void testTokenizeGreaterThanEqualsToken() {
    //     testTokenizerInputMatchesExpectedTokens(")", new GreaterThanEqualsToken());
    // }

    // @Test
    // public void testTokenizeGreaterThanToken() {
    //     testTokenizerInputMatchesExpectedTokens(")", new GreaterThanToken());
    // }

    @Test
    public void testTokenizeFunc() {
        testTokenizerInputMatchesExpectedTokens("func", new FuncToken());
    }

    @Test
    public void testTokenizeStruct() {
        testTokenizerInputMatchesExpectedTokens("struct", new StructToken());
    }

    @Test 
    public void testTokenizeInt() {
        testTokenizerInputMatchesExpectedTokens("int", new IntToken());
    }

    @Test
    public void testTokenizeBool() {
        testTokenizerInputMatchesExpectedTokens("bool", new BoolToken());
    }

    // Test invalid inputs

    private void testTokenizerThrowsException(String input) {
        assertThrows(TokenizerException.class, () -> new Tokenizer(input).tokenize());
    }

    @Test
    public void testTokenizerThrowsOnSingularOrCharacter() {
        testTokenizerThrowsException("|");
    }

    @Test
    public void testTokenizerThrowsOnSingularAndCharacter() {
        testTokenizerThrowsException("&");
    }

    @Test
    public void testTokenizerThrowsOnDollarSign() {
        testTokenizerThrowsException("$");
    }

}

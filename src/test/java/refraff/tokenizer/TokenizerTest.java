package refraff.tokenizer;

import org.junit.Test;

import refraff.tokenizer.reserved.*;
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

    @Test
    public void testTokenizeNotEqualToken() {
        testTokenizerInputMatchesExpectedTokens("!=", new NotEqualsToken());
    }

    @Test
    public void testTokenizeNotToken() {
        testTokenizerInputMatchesExpectedTokens("!", new NotToken());
    }

    @Test
    public void testTokenizeGreaterThanEqualsToken() {
        testTokenizerInputMatchesExpectedTokens(">=", new GreaterThanEqualsToken());
    }

    @Test
    public void testTokenizeGreaterThanToken() {
        testTokenizerInputMatchesExpectedTokens(">", new GreaterThanToken());
    }

    @Test
    public void testTokenizeColonToken() {
        testTokenizerInputMatchesExpectedTokens(":", new ColonToken());
    }

    @Test
    public void testTokenizeFuncToken() {
        testTokenizerInputMatchesExpectedTokens("func", new FuncToken());
    }

    @Test
    public void testTokenizeStructToken() {
        testTokenizerInputMatchesExpectedTokens("struct", new StructToken());
    }

    @Test 
    public void testTokenizeIntToken() {
        testTokenizerInputMatchesExpectedTokens("int", new IntToken());
    }

    @Test
    public void testTokenizeBoolToken() {
        testTokenizerInputMatchesExpectedTokens("bool", new BoolToken());
    }

    @Test
    public void testTokenizeTrueToken() {
        testTokenizerInputMatchesExpectedTokens("true", new TrueToken());
    }

    @Test
    public void testTokenizeFalseToken() {
        testTokenizerInputMatchesExpectedTokens("false", new FalseToken());
    }

    @Test
    public void testTokenizeNullToken() {
        testTokenizerInputMatchesExpectedTokens("null", new NullToken());
    }

    @Test
    public void testTokenizeNewToken() {
        testTokenizerInputMatchesExpectedTokens("new", new NewToken());
    }

    @Test
    public void testTokenizeIfToken() {
        testTokenizerInputMatchesExpectedTokens("if", new IfToken());
    }

    @Test
    public void testTokenizeElseToken() {
        testTokenizerInputMatchesExpectedTokens("else", new ElseToken());
    }

    @Test
    public void testTokenizeWhileToken() {
        testTokenizerInputMatchesExpectedTokens("while", new WhileToken());
    }

    @Test
    public void testTokenizeBreakToken() {
        testTokenizerInputMatchesExpectedTokens("break", new BreakToken());
    }

    @Test
    public void testTokenizePrintlnToken() {
        testTokenizerInputMatchesExpectedTokens("println", new PrintlnToken());
    }

    @Test
    public void testTokenizeReturnToken() {
        testTokenizerInputMatchesExpectedTokens("return", new ReturnToken());
    }

    // @Test
    // public void testTokenizeIdentifierToken() {
    //     testTokenizerInputMatchesExpectedTokens("variable", new IdentifierToken("variable"));
    // }

    // @Test
    // public void testTokenizeStructDefinition() {
    //     String input = """
    //             struct Node {
    //                 int value;
    //                 Node rest;
    //             }
    //             """;
    //     testTokenizerInputMatchesExpectedTokens(
    //             input,
    //             new StructToken(),
    //             new IdentifierToken("Node"),
    //             new LeftBraceToken(),
    //             new IntToken(),
    //             new SemicolonToken(),
    //             new IdentifierToken("Node"),
    //             new IdentifierToken("rest"),
    //             new SemicolonToken(),
    //             new RightBraceToken()
    //     );
    // }

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

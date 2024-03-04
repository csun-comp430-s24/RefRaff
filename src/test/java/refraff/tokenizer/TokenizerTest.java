package refraff.tokenizer;

import org.junit.Test;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class TokenizerTest {

    // Test valid inputs

    private List<Token> getTokensWithoutException(String input) {
        try {
            return new Tokenizer(input).tokenize();
        } catch (TokenizerException ex) {
            fail(ex.getMessage());
        }

        throw new IllegalStateException("This will never be called.");
    }

    private void testTokenizerInputMatchesExpectedTokens(String input, Token... expectedTokensArray) {
        List<Token> actualTokens = getTokensWithoutException(input);
        List<Token> expectedTokens = Arrays.asList(expectedTokensArray);

        assertEquals(expectedTokens, actualTokens);
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
    public void testTokenizeVoidToken() {
        testTokenizerInputMatchesExpectedTokens("void", new VoidToken());
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

    @Test
    public void testTokenizeIntLiteralTokenOne() {
        testTokenizerInputMatchesExpectedTokens("1", new IntLiteralToken("1"));
    }

    @Test
    public void testTokenizeIntLiteralTokens() {
        String input = """
                0 1234 10101 100000
                """;
        testTokenizerInputMatchesExpectedTokens(
            input,
            new IntLiteralToken("0"),
            new IntLiteralToken("1234"),
            new IntLiteralToken("10101"),
            new IntLiteralToken("100000")
        );
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

    @Test
    public void testTokenizerThrowsOnIdentifierTokenWithLeadingInterger() {
        testTokenizerThrowsException("1234r");
    }

    @Test
    public void testTokenizerThrowsOnIntLiteralTokenWithLeadingZeros() {
        testTokenizerThrowsException("0123");
    }
  
    // Grammar tests

    private static final String TEST_DIRECTORY = "src/test/resources";
    private static final String GRAMMAR_FILE_PATH = "grammar.txt";

    private static final Pattern MATCHES_GRAMMAR_DEFINED_TOKEN = Pattern.compile("`[^`]+`");

    @TestFactory
    public List<DynamicTest> testGrammarDefinedTokensArePresent() {
        List<String> grammarDefinedTokens = readGrammarDefinedTokens();

        List<DynamicTest> dynamicTests = new ArrayList<>();

        // If our character is not an alphabetic character at the start, we are a symbol
        List<String> symbols = grammarDefinedTokens.stream()
                .filter(str -> !Character.isAlphabetic(str.charAt(0)))
                .toList();

        List<DynamicTest> symbolTests = checkTokensExistAndInheritFromBaseClass(symbols,
                AbstractSymbolToken.class);
        dynamicTests.addAll(symbolTests);

        // Only select the grammar defined tokens that are not symbols
        List<String> reservedWords = grammarDefinedTokens.stream()
                .filter(str -> !symbols.contains(str))
                .toList();

        List<DynamicTest> reservedWordTests = checkTokensExistAndInheritFromBaseClass(reservedWords,
                AbstractReservedToken.class);
        dynamicTests.addAll(reservedWordTests);

        return dynamicTests;
    }

    private List<DynamicTest> checkTokensExistAndInheritFromBaseClass(List<String> tokens,
                                                                      Class<? extends Token> baseClass) {
        String baseClassName = baseClass.getName();

        return tokens.stream()
                .map(token ->
                        dynamicTest(token + " token exists and is a subclass of " + baseClassName, () -> {
                            List<Token> tokenizedTokens = getTokensWithoutException(token);
                            assertEquals("List should contain exactly one token", 1, tokenizedTokens.size());

                            Token tokenizedToken = tokenizedTokens.get(0);
                            assertEquals(token, tokenizedToken.getTokenizedValue());

                            Class<? extends Token> tokenClass = tokenizedToken.getClass();
                            assertTrue("Token class does not inherit from proper base class",
                                    baseClass.isAssignableFrom(tokenClass));
                        })
                )
                .toList();
    }

    private List<String> readGrammarDefinedTokens() {
        File file = new File(TEST_DIRECTORY, GRAMMAR_FILE_PATH);
        assertTrue(GRAMMAR_FILE_PATH + " does not exist to parse grammar defined tokens", file.exists());


        StringBuffer stringBuffer = new StringBuffer();

        assertDoesNotThrow(() -> {
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    stringBuffer.append(scanner.nextLine());
                }
            }
        });

        String grammar = stringBuffer.toString();
        Matcher definedTokenMatcher = MATCHES_GRAMMAR_DEFINED_TOKEN.matcher(grammar);

        List<String> grammarDefinedTokens = new ArrayList<>();

        while (definedTokenMatcher.find()) {
            String grammarDefinedToken = definedTokenMatcher.group();
            grammarDefinedTokens.add(grammarDefinedToken);
        }

        // Grab the distinct tokens with backticks, remove the backticks, and return their list
        return grammarDefinedTokens.stream()
                .distinct()
                .map(tokenWithBackticks -> tokenWithBackticks.substring(1, tokenWithBackticks.length() - 1))
                .toList();
    }
    

}

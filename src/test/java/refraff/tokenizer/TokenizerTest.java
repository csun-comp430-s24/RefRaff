package refraff.tokenizer;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import refraff.Source;
import refraff.SourcePosition;
import refraff.Sourced;
import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.util.ResourceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class TokenizerTest {

    private static final String WHITESPACE_CHARACTERS = " \n\r\t";

    // Test valid inputs

    private List<Sourced<Token>> getTokensWithoutException(String input) {
        try {
            return new Tokenizer(input).tokenize();
        } catch (TokenizerException ex) {
            fail(ex.getMessage());
        }

        throw new IllegalStateException("This will never be called.");
    }

    private void testTokenizerInputMatchesExpectedTokens(String input, Token... expectedTokensArray) {
        List<Sourced<Token>> actualTokens = getTokensWithoutException(input);

        List<Token> actualUnsourcedTokens = actualTokens.stream().map(Sourced::getValue).toList();
        List<Token> expectedUnsourcedTokens = Arrays.asList(expectedTokensArray);

        assertEquals(expectedUnsourcedTokens, actualUnsourcedTokens);
    }

    @Test
    public void testTokenizeEmptyString() {
        testTokenizerInputMatchesExpectedTokens("");
    }

    @Test
    public void testTokenizeWhitespaceString() {
        testTokenizerInputMatchesExpectedTokens("    " + WHITESPACE_CHARACTERS + "      ");
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
    public void testTokenizeIdentifierNotReservedWord1() {
        testTokenizerInputMatchesExpectedTokens("ret urn", new IdentifierToken("ret"), new IdentifierToken("urn"));
    }

    @Test
    public void testTokenizeIdentifierNotReservedWord2() {
        testTokenizerInputMatchesExpectedTokens("returnType", new IdentifierToken("returnType"));
    }

    @Test
    public void testTokenizeIdentifierNotReservedWord3() {
        testTokenizerInputMatchesExpectedTokens("return7", new IdentifierToken("return7"));
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

    @Test
    public void testTokenizeIdentifierToken() {
        testTokenizerInputMatchesExpectedTokens("variable", new IdentifierToken("variable"));
    }

    @Test
    public void testTokenizeIdentifierTokenWithNumbersCapitalsAndUnderscores() {
        final String input = "vAr_1234_XyZ";
        testTokenizerInputMatchesExpectedTokens(input, new IdentifierToken(input));
    }

    @Test
    public void testTokenizeIdentifiersWithSpacesInBetween() {
        testTokenizerInputMatchesExpectedTokens("ab cd", new IdentifierToken("ab"), new IdentifierToken("cd"));
    }

    @Test
    public void testTokenizeAssignmentStatement() {
        String input = """
                int variableName = 6;
                """;
        testTokenizerInputMatchesExpectedTokens(
            input,
            new IntToken(),
            new IdentifierToken("variableName"),
            new AssignmentToken(),
            new IntLiteralToken("6"),
            new SemicolonToken()
        );
    }

    @Test
    public void testTokenizeStructDefinition() {
        String input = """
                struct Node {
                    int value;
                    Node rest;
                }
                """;
        testTokenizerInputMatchesExpectedTokens(
                input,
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
        );
    }

    @Test
    public void testTokenizesProgramWithNoExceptions() {
        // Reads valid input file with whitespace in between most tokens
        String program = ResourceUtil.readProgramInputFile();
        getTokensWithoutException(program);
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

    @Test
    public void testTokenizerThrowsOnIdentifierTokenWithLeadingInteger() {
        testTokenizerThrowsException("1234r");
    }

    @Test
    public void testTokenizerThrowsOnIdentifierTokenWithLeadingUnderscore() {
        testTokenizerThrowsException("_a");
    }

    @Test
    public void testTokenizerThrowsOnIntLiteralTokenWithLeadingZeros() {
        testTokenizerThrowsException("0123");
    }

    // Sourced token tests


    @Test
    public void testTokenizeFuncDefinition() throws TokenizerException {
        String expectedInput = """
                func foo(int a): int {
                  return a + 2;
                }""";

        // func foo(int a): int {
        Source funcSource = new Source("func", new SourcePosition(1, 1), new SourcePosition(1, 5));
        Sourced<Token> funcSourcedToken = new Sourced<>(funcSource, new FuncToken());

        Source fooSource = new Source("foo", new SourcePosition(1, 6), new SourcePosition(1, 9));
        Sourced<Token> fooSourcedToken = new Sourced<>(fooSource, new IdentifierToken("foo"));

        Source leftParenSource = new Source("(", new SourcePosition(1, 9), new SourcePosition(1, 10));
        Sourced<Token> leftParenSourcedToken = new Sourced<>(leftParenSource, new LeftParenToken());

        Source intParamSource = new Source("int", new SourcePosition(1, 10), new SourcePosition(1, 13));
        Sourced<Token> intParamSourcedToken = new Sourced<>(intParamSource, new IntToken());

        Source aParamSource = new Source("a", new SourcePosition(1, 14), new SourcePosition(1, 15));
        Sourced<Token> aParamSourcedToken = new Sourced<>(aParamSource, new IdentifierToken("a"));

        Source rightParenSource = new Source(")", new SourcePosition(1, 15), new SourcePosition(1, 16));
        Sourced<Token> rightParenSourcedToken = new Sourced<>(rightParenSource, new RightParenToken());

        Source colonSource = new Source(":", new SourcePosition(1, 16), new SourcePosition(1, 17));
        Sourced<Token> colonSourcedToken = new Sourced<>(colonSource, new ColonToken());

        Source intReturnSource = new Source("int", new SourcePosition(1, 18), new SourcePosition(1, 21));
        Sourced<Token> intReturnSourcedToken = new Sourced<>(intReturnSource, new IntToken());

        Source leftBraceSource = new Source("{", new SourcePosition(1, 22), new SourcePosition(1, 23));
        Sourced<Token> leftBraceSourcedToken = new Sourced<>(leftBraceSource, new LeftBraceToken());

        // <space><space>return a + 2;
        Source returnSource = new Source("return", new SourcePosition(2, 3), new SourcePosition(2, 9));
        Sourced<Token> returnSourcedToken = new Sourced<>(returnSource, new ReturnToken());

        Source aSource = new Source("a", new SourcePosition(2, 10), new SourcePosition(2, 11));
        Sourced<Token> aSourcedToken = new Sourced<>(aSource, new IdentifierToken("a"));

        Source plusSource = new Source("+", new SourcePosition(2, 12), new SourcePosition(2, 13));
        Sourced<Token> plusSourcedToken = new Sourced<>(plusSource, new PlusToken());

        Source twoSource = new Source("2", new SourcePosition(2, 14), new SourcePosition(2, 15));
        Sourced<Token> twoSourcedToken = new Sourced<>(twoSource, new IntLiteralToken("2"));

        Source semicolonSource = new Source(";", new SourcePosition(2, 15), new SourcePosition(2, 16));
        Sourced<Token> semicolonSourcedToken = new Sourced<>(semicolonSource, new SemicolonToken());

        // }
        Source rightBraceSource = new Source("}", new SourcePosition(3, 1), new SourcePosition(3, 2));
        Sourced<Token> rightBraceSourcedToken = new Sourced<>(rightBraceSource, new RightBraceToken());

        List<Sourced<Token>> actualSourcedTokens = new Tokenizer(expectedInput).tokenize();
        List<Sourced<Token>> expectedSourcedTokens = List.of(funcSourcedToken, fooSourcedToken, leftParenSourcedToken,
                intParamSourcedToken, aParamSourcedToken, rightParenSourcedToken, colonSourcedToken,
                intReturnSourcedToken, leftBraceSourcedToken, returnSourcedToken, aSourcedToken, plusSourcedToken,
                twoSourcedToken, semicolonSourcedToken, rightBraceSourcedToken);

        assertEquals(expectedSourcedTokens, actualSourcedTokens);
    }

    private void testReconstructedSourceInputMatchesExpected(String originalInput, List<Sourced<Token>> sourcedTokens) {
        // Map the actual sources into a singular source composed of the whole program
        Source source = Source.fromSources(sourcedTokens.stream()
                .map(Sourced::getSource)
                .toList());
        String sourceReconstructedInput = source.getSourceString();

        // Assert the source can recreate the original program
        assertEquals(originalInput, sourceReconstructedInput);
    }

    @Test
    public void testTokenizeSourcedExampleProgram() {
        String sampleProgram = ResourceUtil.readProgramInputFile();
        List<Sourced<Token>> sourcedTokens = getTokensWithoutException(sampleProgram);

        testReconstructedSourceInputMatchesExpected(sampleProgram, sourcedTokens);
    }
  
    // Dynamic tests

    private static final String GRAMMAR_FILE_PATH = "grammar.txt";

    // The tokens in our grammar are expressed as: `<token>`, the character ` does not otherwise appear
    private static final Pattern MATCHES_GRAMMAR_DEFINED_TOKEN = Pattern.compile("`[^`]+`");

    /**
     * Creates a test factory to check if all tokens defined in the grammar exist and match their expected base class.
     *
     * If this test factory fails, then either 1) or 2) occurs for one or more grammar-defined tokens:
     * 1) A token defined in the grammar is not created
     * 2) A defined token does not match the expected subclass that it should be in the code
     *
     * @return the list of dynamically generated tests
     */
    @TestFactory
    public List<DynamicTest> testGrammarDefinedTokensExistAndInheritFromBaseClass() {
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
        String baseClassName = baseClass.getSimpleName();

        return tokens.stream()
                .map(token ->
                        dynamicTest(token + " token exists and is a subclass of " + baseClassName, () -> {
                            List<Sourced<Token>> tokenizedSourcedTokens = getTokensWithoutException(token);
                            assertEquals(1, tokenizedSourcedTokens.size(), "List should contain exactly one token");

                            Token tokenizedToken = tokenizedSourcedTokens.get(0).getValue();
                            assertEquals(token, tokenizedToken.getTokenizedValue());

                            Class<? extends Token> tokenClass = tokenizedToken.getClass();
                            assertTrue(baseClass.isAssignableFrom(tokenClass),
                                    "Token class does not inherit from proper base class");
                        })
                )
                .toList();
    }

    private List<String> readGrammarDefinedTokens() {
        String grammar = ResourceUtil.readInputFile(GRAMMAR_FILE_PATH);
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

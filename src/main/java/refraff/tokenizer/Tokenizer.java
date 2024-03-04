package refraff.tokenizer;

import refraff.tokenizer.reserved.*;
import refraff.tokenizer.symbol.*;
import refraff.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizer {

    private static final List<Token> SYMBOL_TOKENS = Arrays.asList(
            // Non-operator symbols
            new CommaToken(),
            new LeftBraceToken(),
            new RightBraceToken(),
            new SemicolonToken(),
            new LeftParenToken(),
            new RightParenToken(),
            new ColonToken(),

            // Operator symbols
            new DotToken(),

            // Arithmetic operators
            new MultiplyToken(),
            new DivisionToken(),
            new PlusToken(),
            new MinusToken(),

            // Logical operators
            new AndToken(),
            new OrToken(),
            new NotEqualsToken(),
            new NotToken(),
            new LessThanEqualsToken(),
            new LessThanToken(),
            new GreaterThanEqualsToken(),
            new GreaterThanToken(),
            new DoubleEqualsToken(),
            new AssignmentToken()
    );

    private static final List<Token> RESERVED_TOKENS = Arrays.asList(
            new IntToken(),
            new BoolToken(),
            new StructToken(),
            new FuncToken(),
            new TrueToken(),
            new FalseToken(),
            new NullToken(),
            new NewToken(),
            new IfToken(),
            new ElseToken(),
            new WhileToken(),
            new BreakToken(),
            new PrintlnToken(),
            new ReturnToken(),
            new VoidToken()
    );

    // Regex patterns

    // The whitespace character for Java's Pattern is \s
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s*");

    // The integer literal pattern.This doesn't allow for leading zeros - do we want that?
    private static final Pattern INT_LITERAL_PATTERN = Pattern.compile("\\b(0|[1-9][0-9]*)\\b");

    // The indentifier regex
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z_0-9]*)\\b");

    // A regex pattern that places all valid symbols and operators as distinct choices to be tokenized
    // Take each symbol, escape it and create a group [e.g. `(\<\=)`], then OR the groups together with regex `|`
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
            SYMBOL_TOKENS.stream()
                    .map(Token::getTokenizedValue)
                    .map(value -> "(" + Pattern.quote(value) + ")")
                    .collect(Collectors.joining("|"))
    );

    private static final Pattern RESERVED_PATTERN = Pattern.compile(
            RESERVED_TOKENS.stream()
                    .map(Token::getTokenizedValue)
                    .map(value -> "(" + Pattern.quote(value) + ")")
                    .collect(Collectors.joining("|"))
    );

    private static final Map<String, Token> SYMBOL_TO_TOKEN = getStringToTokenMap(SYMBOL_TOKENS);
    private static final Map<String, Token> RESERVED_TO_TOKEN = getStringToTokenMap(RESERVED_TOKENS);

    private static Map<String, Token> getStringToTokenMap(List<Token> tokenList) {
        return tokenList.stream()
                .collect(
                        Collectors.toMap(
                                Token::getTokenizedValue,
                                Function.identity()
                        )
                );
    }

    private final String input;
    private final int inputLength;

    private int tokenizerPosition = 0;

    public Tokenizer(String input) {
        this.input = input;
        this.inputLength = input.length();
    }

    public List<Token> tokenize() throws TokenizerException {
        List<Token> tokens = new ArrayList<>();
        boolean alreadySkippedWhitespace = false;

        while (!inputOutOfBounds()) {
            // If we haven't already skipped whitespace, try and skip whitespace
            if (!alreadySkippedWhitespace && skipWhitespace()) {
                alreadySkippedWhitespace = true;
                continue;
            }

            // We will need to try to skip whitespace again in our next iteration
            alreadySkippedWhitespace = false;

            // Try and tokenize symbols
            Optional<Token> optionalSymbolToken = tryTokenizeSymbol();
            if (optionalSymbolToken.isPresent()) {
                tokens.add(optionalSymbolToken.get());
                continue;
            }

            // Try and tokenize reserved word
            Optional<Token> optionalReservedToken = tryTokenizeReserved();
            if (optionalReservedToken.isPresent()) {
                tokens.add(optionalReservedToken.get());
                continue;
            }

            // Try to tokenize identifier
            Optional<Token> optionalIdentifierToken = tryTokenizeIdentifier();
            if (optionalIdentifierToken.isPresent()) {
                tokens.add(optionalIdentifierToken.get());
                continue;
            }

            // Try and tokenize IntLiteral
            Optional<Token> optionalIntLiteralToken = tryTokenizeIntLiteral();
            if (optionalIntLiteralToken.isPresent()) {
                tokens.add(optionalIntLiteralToken.get());
                continue;
            }

            // If we cannot match any whitespace or token by now, throw an exception
            throw new TokenizerException("Could not tokenize '" + input.substring(tokenizerPosition) + "'");
        }

        return tokens;
    }

    private boolean inputOutOfBounds() {
        return tokenizerPosition >= inputLength;
    }

    private boolean skipWhitespace() {
        // If we are out of bounds, do not attempt to tokenize
        if (inputOutOfBounds()) {
            return false;
        }

        Matcher matcher = WHITESPACE_PATTERN.matcher(input);

        // If we didn't find any whitespace, return
        if (!matcher.find(tokenizerPosition)) {
            return false;
        }

        // Set the position to be the next character that is not whitespace
        this.tokenizerPosition = matcher.end();
        return true;
    }

    // Tries to tokenize mapped tokens
    public Optional<Token> tryTokenize(Pattern pattern, Map<String, Token> tokenMap) {
        // If we are out of bounds, do not attempt to tokenize
        if (inputOutOfBounds()) {
            return Optional.empty();
        }

        // Try to find a symbol
        Matcher matcher = pattern.matcher(input);
        
        // If we didn't find any tokens that match a token exactly, return an empty list
        // Or if the position of the found token isn't our current position
        if (!matcher.find(tokenizerPosition) || matcher.start() != tokenizerPosition) {
            return Optional.empty();
        }

        // Get the token from input
        String symbol = matcher.group();
        Token token = tokenMap.get(symbol);

        // Increment the position and return our found token
        this.tokenizerPosition += symbol.length();
        return Optional.of(token);
    }

    public Optional<Token> tryTokenizeSymbol() {
        return tryTokenize(SYMBOL_PATTERN, SYMBOL_TO_TOKEN);
    }

    public Optional<Token> tryTokenizeReserved() {
        return tryTokenize(RESERVED_PATTERN, RESERVED_TO_TOKEN);
    }

    // Tries to tokenize patterns that don't have maps
    public Optional<Token> tryTokenize(Pattern pattern, Function<String, Token> tokenGenerator) {
        // If we are out of bounds, do not attempt to tokenize
        if (inputOutOfBounds()) {
            return Optional.empty();
        }

        Matcher matcher = pattern.matcher(input);

        // If no match found, return empty list
        if (!matcher.find(tokenizerPosition) || matcher.start() != tokenizerPosition) {
            return Optional.empty();
        }

        // Get the token from input
        String token = matcher.group();

        this.tokenizerPosition += token.length();
        return Optional.of(tokenGenerator.apply(token));
    }

    public Optional<Token> tryTokenizeIdentifier() {
        return tryTokenize(IDENTIFIER_PATTERN, IdentifierToken::new);
    }

    public Optional<Token> tryTokenizeIntLiteral() {
        return tryTokenize(INT_LITERAL_PATTERN, IntLiteralToken::new);
    }
    
}

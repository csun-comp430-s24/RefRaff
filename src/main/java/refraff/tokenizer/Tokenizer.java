package refraff.tokenizer;

import refraff.Source;
import refraff.SourcePosition;
import refraff.Sourced;
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

    // The integer literal pattern. This doesn't allow for leading zeros
    private static final Pattern INT_LITERAL_PATTERN = Pattern.compile("\\b(0|[1-9][0-9]*)\\b");

    // The identifier regex (underscores should not be allowed in first char for variables according to C standard)
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\b([a-zA-Z][a-zA-Z_0-9]*)\\b");

    // A regex pattern that places all valid symbols and operators as distinct choices to be tokenized
    // Take each symbol, escape it and create a group [e.g. `(\<\=)`], then OR the groups together with regex `|`
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
            SYMBOL_TOKENS.stream()
                    .map(Token::getTokenizedValue)
                    .map(symbol -> "(" + Pattern.quote(symbol) + ")")
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

    private SourcePosition previousPosition;
    private SourcePosition currentPosition;

    public Tokenizer(String input) {
        this.input = input;
        this.inputLength = input.length();

        this.previousPosition = SourcePosition.DEFAULT_SOURCE_POSITION;
        this.currentPosition = previousPosition;
    }

    public List<Sourced<Token>> tokenize() throws TokenizerException {
        List<Sourced<Token>> tokens = new ArrayList<>();
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
            Optional<Sourced<Token>> optionalSymbolToken = tryTokenizeSymbol();
            if (optionalSymbolToken.isPresent()) {
                tokens.add(optionalSymbolToken.get());
                continue;
            }

            // Try to tokenize reserved word or identifier
            Optional<Sourced<Token>> optionalReservedWordOrIdentifier = tryTokenizeReservedOrIdentifier();
            if (optionalReservedWordOrIdentifier.isPresent()) {
                tokens.add(optionalReservedWordOrIdentifier.get());
                continue;
            }

            // Try and tokenize IntLiteral
            Optional<Sourced<Token>> optionalIntLiteralToken = tryTokenizeIntLiteral();
            if (optionalIntLiteralToken.isPresent()) {
                tokens.add(optionalIntLiteralToken.get());
                continue;
            }

            // If we cannot match any whitespace or token by now, throw an exception
            String errorMessage = "Tokenizer error at %s: could not tokenize starting at `%s`";
            throw new TokenizerException(String.format(errorMessage, currentPosition.toString(),
                    input.substring(tokenizerPosition)));
        }

        return tokens;
    }

    private boolean inputOutOfBounds() {
        return tokenizerPosition >= inputLength;
    }

    private Optional<String> findMatchingPattern(Pattern pattern) {
        // If we are out of bounds, do not attempt to read pattern match
        if (inputOutOfBounds()) {
            return Optional.empty();
        }

        // Setup the regex matcher for the input
        Matcher matcher = pattern.matcher(input);

        // If we didn't find any string that matches our pattern
        // or if the position of the found match isn't our current position, return an empty optional
        if (!matcher.find(tokenizerPosition) || matcher.start() != tokenizerPosition) {
            return Optional.empty();
        }

        // Get the matched string and change our position to immediately after the end of the match
        String matched = matcher.group();

        // If we parsed a blank whitespace character, return it now to not affect our position
        if (matched.isEmpty()) {
            return Optional.of(matched);
        }

        this.tokenizerPosition = matcher.end();

        // Update our previous and current line and column numbers
        this.previousPosition = currentPosition;
        int additionalLines = getNewLines(matched);

        // Start calculating our current position
        int currentLinePosition = currentPosition.getLinePosition() + additionalLines;
        int currentColumnPosition = currentPosition.getColumnPosition();

        // If we have additional lines here, then reset the column head to be at the start of the last line
        if (additionalLines > 0) {
            currentColumnPosition = SourcePosition.STARTING_COLUMN_POSITION;
        }

        // Add the length of the last processed line to our current column position
        String lastLine = matched.lines().reduce("", (a, e) -> e);
        currentColumnPosition += lastLine.length();

        this.currentPosition = new SourcePosition(currentLinePosition, currentColumnPosition);

        // Return our matched string
        return Optional.of(matched);
    }

    private int getNewLines(String matched) {
        // If we're not dealing with whitespace, we can never have a newline
        if (!matched.isBlank()) {
            return 0;
        }

        int newLines = 0;
        boolean hasPrevCarriageReturn = false;

        // https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#lt
        for (char c : matched.toCharArray()) {
            if (hasPrevCarriageReturn && c != '\n') {
                newLines++;
                hasPrevCarriageReturn = false;
                break;
            }

            switch (c) {
                case '\r':
                    hasPrevCarriageReturn = true;
                    break;
                case '\n':
                case '\u0085':
                case '\u2028':
                case '\u2029':
                    newLines++;
                    break;
            }
        }

        return newLines;
    }

    private boolean skipWhitespace() {
        // Return whether whitespace was found
        return findMatchingPattern(WHITESPACE_PATTERN).isPresent();
    }

    // Tries to tokenize mapped tokens
    public Optional<Sourced<Token>> tryTokenize(Pattern pattern, Map<String, Token> tokenMap) {
        return tryTokenize(pattern, tokenMap::get);
    }

    public Optional<Sourced<Token>> tryTokenize(Pattern pattern, Function<String, Token> stringToTokenFunction) {
        Optional<String> optionalMatchedString = findMatchingPattern(pattern);

        // If we did not find a match for our token, return an empty optional
        if (optionalMatchedString.isEmpty()) {
            return Optional.empty();
        }

        // Apply our mapping function from string to token and return its value
        String matchedToken = optionalMatchedString.get();
        Token token = stringToTokenFunction.apply(matchedToken);

        // Create the source for the line/column position for our token
        Source source = new Source(matchedToken, previousPosition, currentPosition);
        Sourced<Token> sourcedToken = new Sourced<>(source, token);

        return Optional.of(sourcedToken);
    }

    public Optional<Sourced<Token>> tryTokenizeSymbol() {
        return tryTokenize(SYMBOL_PATTERN, SYMBOL_TO_TOKEN);
    }

    public Optional<Sourced<Token>> tryTokenizeReservedOrIdentifier() {
        return tryTokenize(
                IDENTIFIER_PATTERN,
                token -> RESERVED_TO_TOKEN.getOrDefault(token, new IdentifierToken(token))
        );
    }

    public Optional<Sourced<Token>> tryTokenizeIntLiteral() {
        return tryTokenize(INT_LITERAL_PATTERN, IntLiteralToken::new);
    }
    
}

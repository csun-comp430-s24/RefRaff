package refraff.tokenizer;

import refraff.tokenizer.reserved.BoolToken;
import refraff.tokenizer.reserved.IntToken;
import refraff.tokenizer.symbol.CommaToken;
import refraff.tokenizer.symbol.DoubleEqualsToken;
import refraff.tokenizer.symbol.LeftBraceToken;
import refraff.tokenizer.symbol.RightBraceToken;
import refraff.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Tokenizer {

    static final List<Token> SYMBOL_TOKENS = Arrays.asList(
            // Non-operator symbols
            new CommaToken(),
            new LeftBraceToken(),
            new RightBraceToken(),
            // new SemicolonToken(),
            // new LeftParenToken(),
            // new RightParenToken(),

            // Operator symbols
            // new DotToken(),

            // Arithmetic operators
            // new MultiplyToken(),
            // new DivisionToken(),
            // new PlusToken(),
            // new MinusToken(),

            // Logical operators
            // new AndToken(),
            // new OrToken(),
            // new NotEqualsToken(),
            // new NotToken(),
            // new LessThanEqualsToken(),
            // new LessThanToken(),
            // new GreaterThanEqualsToken(),
            // new GreaterThanToken(),
            new DoubleEqualsToken()
            // new AssignmentToken()
    );

    // Take the tokens for all symbols, split into chars, select distinct chars, and append together
    // Avoid making manual changes to our set of symbols as we add new symbol tokens
    static final String UNIQUE_SYMBOL_CHARACTERS = SYMBOL_TOKENS.stream()
            .map(Token::getTokenizedValue)
            .map(String::chars)
            .flatMapToInt(Function.identity())
            .mapToObj(charAsInt -> (char) charAsInt)
            .distinct()
            .collect(
                    StringBuilder::new,
                    StringBuilder::append,
                    StringBuilder::append
            )
            .toString();

    static final List<Token> RESERVED_TOKENS = Arrays.asList(
            new IntToken(),
            new BoolToken()
    );

    // Regex patterns
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\w");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(Pattern.quote(UNIQUE_SYMBOL_CHARACTERS));


    private static final List<Pair<String, Token>> SYMBOLS_WITH_PRECEDENCE = SYMBOL_TOKENS.stream()
            .map(symbolToken -> Pair.of(symbolToken.getTokenizedValue(), symbolToken))
            .toList();

    private static final Map<String, Token> RESERVED_WORD_TO_TOKEN = RESERVED_TOKENS.stream()
            .collect(
                    Collectors.toMap(
                            Token::getTokenizedValue,
                            Function.identity()
                    )
            );

    private final String input;
    private int position = 0;

    public Tokenizer(String input) {
        this.input = input;
    }

}

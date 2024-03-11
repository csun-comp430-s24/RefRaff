package refraff.parser.operator;

public enum OperatorEnum {
    OR("||"),
    AND("&&"),
    DOUBLE_EQUALS("=="),
    NOT_EQUALS("!="),
    LESS_THAN_EQUALS("<="),
    GREATER_THAN_EQUALS(">="),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVISION("/"),
    NOT("!"),
    DOT(".");

    private final String symbol;

    OperatorEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}

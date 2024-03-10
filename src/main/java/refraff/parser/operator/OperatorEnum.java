package refraff.parser.operator;

public enum OperatorEnum {
    OR("||"),
    AND("&&"),
    DOUBLE_EQUALS("=="),
    NOT_EQUALS("!=");

    private final String symbol;

    OperatorEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}

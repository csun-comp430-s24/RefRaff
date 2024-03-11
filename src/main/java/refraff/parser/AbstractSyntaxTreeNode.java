package refraff.parser;

public abstract class AbstractSyntaxTreeNode implements Node {

    /*
        * Format for printing the token nicely: "<descriptor> '<tokenizedValue>'"
        * e.g. "Variable 'counter'"
        * "Type 'int'"
        * "Statement 'Variable declaration'"
        * "int literal expression 6"
        * 
        * This may be more useful when debugging the compiler and getting helpful error
        * messages.
     */
    private static final String TO_STRING_FORMAT = "%s '%s'";

    private final String parsedValue;

    public AbstractSyntaxTreeNode(String parsedValue) {
        this.parsedValue = parsedValue;
    }

    @Override
    public String getParsedValue() {
        return this.parsedValue;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, getNodeTypeDescriptor(), getParsedValue());
    }

    @Override
    public int hashCode() {
        return getParsedValue().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AbstractSyntaxTreeNode otherToken 
        && getParsedValue().equals(otherToken.getParsedValue());
    }
}

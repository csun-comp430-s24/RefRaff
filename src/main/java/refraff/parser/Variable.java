package refraff.parser;

import java.util.Objects;

public class Variable extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "variable";
    
    public final String name;

    public Variable(String name) {
        super(NODE_TYPE_DESCRIPTOR);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object other) {
        return super.equals(other)
                && other instanceof Variable otherVariable
                && Objects.equals(name, otherVariable.name);
    }

}

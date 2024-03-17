package refraff.parser.function;

import java.util.List;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.expression.Expression;

public class CommaExp extends AbstractSyntaxTreeNode {
        
    private static final String TYPE = "node";
    private static final String TYPE_DESCRIPTOR = "comma expressions";

    public final List<Expression> expressions;

    public CommaExp(List<Expression> expressions) {
        super(TYPE);
        this.expressions = expressions;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}

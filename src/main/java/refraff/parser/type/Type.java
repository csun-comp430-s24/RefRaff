package refraff.parser.type;

import refraff.parser.AbstractSyntaxTreeNode;

public abstract class Type extends AbstractSyntaxTreeNode {

    public Type(String nodeTypeDescriptor) {
        super(nodeTypeDescriptor);
    }

    public boolean shouldThrowOnAssignment() {
        return true;
    }

    /**
     * A method to check if the types have equality between each other. This should be used over
     * {@link Object#equals(Object)}, since our implementation also considers the source of our node
     * when comparing equality in the AST.
     * @param other the other type to compare
     * @return true if the types are equivalent, false if the types are not equivalent
     */
    public boolean hasTypeEquality(Type other) {
        return this.getClass().equals(other.getClass());
    }

}

package refraff.parser.struct;

import refraff.parser.AbstractSyntaxTreeNode;

import java.util.Objects;

public class StructName extends AbstractSyntaxTreeNode {
    
    private static final String NODE_TYPE_DESCRIPTOR = "struct name";

    public final String structName;

    public StructName(final String structName) {
        super(NODE_TYPE_DESCRIPTOR);

        this.structName = structName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), structName);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StructName otherStructName
                && Objects.equals(structName, otherStructName.structName);
    }

}

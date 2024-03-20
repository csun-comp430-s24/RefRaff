package refraff.parser;

import refraff.Source;
import refraff.Sourceable;

import java.util.Objects;

public abstract class AbstractSyntaxTreeNode implements Node, Sourceable {

    private static final String TO_STRING_FORMAT = "Node %s sourced from %s contains value `%s`";

    private final String nodeTypeDescriptor;

    private Source source;
    private boolean hasSourceBeenSet;

    public AbstractSyntaxTreeNode(String nodeTypeDescriptor) {
        this.nodeTypeDescriptor = nodeTypeDescriptor;

        this.source = Source.DEFAULT_TESTING_SOURCE;
        this.hasSourceBeenSet = false;
    }

    @Override
    public Source getSource() {
        return this.source;
    }

    public void setSource(Source source) throws IllegalStateException {
        if (hasSourceBeenSet) {
            throw new IllegalStateException("This source has already been set.");
        }

        this.hasSourceBeenSet = true;
        this.source = source;
    }

    @Override
    public String getParsedValue() {
        return source.getSourceString();
    }

    @Override
    public String getNodeTypeDescriptor() {
        return this.nodeTypeDescriptor;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, getNodeTypeDescriptor(), source.toPositionString(),
                source.getSourceString());
    }

    @Override
    public int hashCode() {
        return getParsedValue().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AbstractSyntaxTreeNode otherNode
                && Objects.equals(getSource(), otherNode.getSource());
    }
}

package refraff;

import java.util.Objects;

public class Sourced<T> implements Sourceable {

    private final Source source;
    private final T t;

    public Sourced(Source source, T t) {
        this.source = source;
        this.t = t;
    }

    @Override
    public Source getSource() {
        return source;
    }

    public T getValue() {
        return this.t;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Sourced otherSourced
                && Objects.equals(getSource(), otherSourced.getSource())
                && Objects.equals(getValue(), otherSourced.getValue());
    }

    @Override
    public String toString() {
        return source.getSourceString() + " at " + source.toPositionString();
    }

}

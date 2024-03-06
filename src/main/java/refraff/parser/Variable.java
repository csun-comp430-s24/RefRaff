package refraff.parser;

public class Variable {
    public final String name;

    public Variable(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object other) {
        return (other instanceof Variable &&
                name.equals(((Variable)other).name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Variable(" + name + ")";
    }
}

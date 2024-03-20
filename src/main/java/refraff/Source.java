package refraff;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Source {

    public static final Source DEFAULT_TESTING_SOURCE = new Source(
            "",
            SourcePosition.DEFAULT_SOURCE_POSITION,
            SourcePosition.DEFAULT_SOURCE_POSITION
    );

    private final String sourceString;
    private final SourcePosition startPosition;
    private final SourcePosition endPosition;

    public Source(String sourceString, SourcePosition startPosition, SourcePosition endPosition) {
        this.sourceString = sourceString;

        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public String getSourceString() {
        return sourceString;
    }

    public SourcePosition getStartPosition() {
        return startPosition;
    }

    public SourcePosition getEndPosition() {
        return endPosition;
    }

    public String toPositionString() {
        // Special case: our lines are the same
        if (startPosition.getLinePosition() == endPosition.getLinePosition()) {
            String edgeFormat = "line %d, columns %d to %d";
            return String.format(edgeFormat, startPosition.getLinePosition(), startPosition.getColumnPosition(),
                    // Our end column position is exclusive
                    endPosition.getColumnPosition() - 1);
        }

        String multilineFormat = "%s to %s";
        return String.format(multilineFormat, startPosition.toLinePosition(false), endPosition.toLinePosition(true));
    }

    public static Source fromSources(Source... sources) {
        return fromSources(Arrays.asList(sources));
    }

    public static Source fromSources(List<Source> sources) {
        int numberOfSources = sources.size();

        if (numberOfSources == 0) {
            throw new IllegalArgumentException("Source list must not be empty.");
        }

        if (numberOfSources == 1) {
            return sources.get(0);
        }

        StringBuilder stringBuilder = new StringBuilder();

        Source previous = sources.get(0);
        stringBuilder.append(previous.getSourceString());

        for (int i = 1; i < sources.size(); i++) {
            Source source = sources.get(i);

            SourcePosition previousEndPosition = previous.getEndPosition();
            SourcePosition currentStartPosition = source.getStartPosition();

            SourcePosition spaceSeparators = currentStartPosition.getSpaceSeparators(previousEndPosition);

            // Add back the spacing we eliminated previously
            stringBuilder.append("\n".repeat(Math.max(0, spaceSeparators.getLinePosition())));
            stringBuilder.append(" ".repeat(Math.max(0, spaceSeparators.getColumnPosition())));

            stringBuilder.append(source.getSourceString());
            previous = source;
        }

        String collectiveSourceString = stringBuilder.toString();

        SourcePosition startPosition = sources.get(0).getStartPosition();
        SourcePosition endPosition = sources.get(numberOfSources - 1).getEndPosition();

        return new Source(collectiveSourceString, startPosition, endPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSourceString(), getStartPosition(), getEndPosition());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Source otherSource
                && Objects.equals(getSourceString(), otherSource.getSourceString())
                && Objects.equals(getStartPosition(), otherSource.getStartPosition())
                && Objects.equals(getEndPosition(), otherSource.getEndPosition());
    }

    @Override
    public String toString() {
        return String.format(
                "%s to %s: `%s`",
                getStartPosition().toLinePosition(true),
                getEndPosition().toLinePosition(false),
                getSourceString()
        );
    }

}

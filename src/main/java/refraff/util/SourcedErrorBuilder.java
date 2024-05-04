package refraff.util;

import refraff.Source;
import refraff.SourcePosition;
import refraff.Sourceable;
import refraff.Sourced;
import refraff.typechecker.TypecheckerException;

import java.util.Optional;

public class SourcedErrorBuilder {

    public static String getErrorString(String errorType, String beingParsed, Sourceable parent, Sourceable child,
                                  String errorMessage) {
        SourcePosition childStartPosition = child.getSource().getStartPosition();

        StringBuilder errorMessageBuilder = new StringBuilder();

        errorMessageBuilder.append(errorType);
        errorMessageBuilder.append(" error when evaluating ");
        errorMessageBuilder.append(beingParsed);
        errorMessageBuilder.append(" at ");
        errorMessageBuilder.append(childStartPosition.toString());
        errorMessageBuilder.append(":\n");

        String parentNodeString = parent.getSource().getSourceString();

        SourcePosition parentStartPosition = parent.getSource().getStartPosition();
        SourcePosition childEndPosition = child.getSource().getEndPosition();

        int numberOfLinesToPrint = (childEndPosition.getLinePosition() - parentStartPosition.getLinePosition()) + 1;

        // Append all the lines until the child is fully displayed
        parentNodeString.lines()
                .limit(numberOfLinesToPrint)
                .forEach(line -> {
                    errorMessageBuilder.append(line);
                    errorMessageBuilder.append('\n');
                });

        int numberOfSpacesToPrint = numberOfLinesToPrint == 1
                ? childStartPosition.getColumnPosition() - parentStartPosition.getColumnPosition()
                : childStartPosition.getColumnPosition() - 1;

        String spacePrefix = " ".repeat(numberOfSpacesToPrint);
        String caretPointer = "^".repeat(childEndPosition.getColumnPosition() - childStartPosition.getColumnPosition());

        // Add spaces until we're under the child, then use the caret pointer to point to the child and new line
        errorMessageBuilder.append(spacePrefix);
        errorMessageBuilder.append(caretPointer);
        errorMessageBuilder.append('\n');

        // Add space until we're under the child, then print the error message below the caret pointer and new line
        errorMessageBuilder.append(spacePrefix);
        errorMessageBuilder.append("    ");
        errorMessageBuilder.append(errorMessage);
        errorMessageBuilder.append('\n');

        return errorMessageBuilder.toString();
    }

}

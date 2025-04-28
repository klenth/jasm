package edu.westminsteru.jasm;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * An error that occurred while assembling Jasm code
 * @param message       a message describing the error
 * @param sourceLine    the source line that has the error
 * @param lineNumber    the line number at which the error occurred, or UNSPECIFIC if it did occur on a particular line
 *                      (the first line is line 1)
 * @param columnNumber  the column number within the line at which the error occurred, or UNSPECIFIC if it did not occur
 *                      at a particular column (the first column is column 1)
 */
public record ErrorMessage(String message, String sourceLine, int lineNumber, int columnNumber) {
    /** Sentinel value for line or column signifying that the error doesn't pertain to a specific line or column number */
    public static final int UNSPECIFIC = -1;

    /**
     * Creates a new {@code ErrorMessage}.
     * @param message      a description of the error
     * @param sourceLine   the text of the line containing the error, or {@code null} if the error does not occur on a specific line
     * @param lineNumber   the number of the line containing the error, counted from 1, or {@link #UNSPECIFIC} if the error does not occur on a specific line
     * @param columnNumber the number of the column within the line containing the error, counted from 1, or {@code UNSPECIFIC} if the error does not occur at a specific column
     */
    public ErrorMessage {
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("Message cannot be null or blank");
        if (lineNumber < UNSPECIFIC)
            throw new IllegalArgumentException(String.format("Invalid line number: %d", lineNumber));
        if (columnNumber < UNSPECIFIC)
            throw new IllegalArgumentException(String.format("Invalid column number: %d", columnNumber));

    }

    /**
     * Creates a new ErrorMessage with no source line and {@code UNSPECIFIC} line and column numbers.
     * @param message   a description of the error
     */
    public ErrorMessage(String message) {
        this(message, null, UNSPECIFIC, UNSPECIFIC);
    }

    /**
     * Formats this {@code ErrorMessage} in a format suitable for presenting to a user.
     * @return the formatted message
     */
    public String format() {
        var sb = new StringBuilder();
        sb.append("Error");
        if (lineNumber != UNSPECIFIC)
            sb.append(" at line ").append(lineNumber);
        sb.append(": ").append(message).append('\n');
        if (sourceLine != null) {
            sb.append("    ").append(sourceLine).append('\n');
            if (columnNumber != UNSPECIFIC)
                sb.append(String.format(String.format("%%%ds\n", columnNumber + 4), "↑"));
        }

        return sb.toString();
    }

    /**
     * Formats this {@code ErrorMessage} and prints it to the given {@code PrintWriter}.
     * @param out the {@code PrintWriter} to print the message to
     */
    public void print(PrintWriter out) {
        out.print(format());
        out.flush();
    }

    /**
     * Formats this {@code ErrorMessage} and prints it to the given {@code PrintStream}.
     * @param out the {@code PrintStream} to print the message to
     */
    public void print(PrintStream out) {
        out.print(format());
        out.flush();
    }
}

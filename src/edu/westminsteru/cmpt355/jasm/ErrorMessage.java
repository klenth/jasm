package edu.westminsteru.cmpt355.jasm;

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
    public static final int UNSPECIFIC = -1;

    public ErrorMessage(String message, String sourceLine, int lineNumber, int columnNumber) {
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("Message cannot be null or blank");
        if (lineNumber < UNSPECIFIC)
            throw new IllegalArgumentException(String.format("Invalid line number: %d", lineNumber));
        if (columnNumber < UNSPECIFIC)
            throw new IllegalArgumentException(String.format("Invalid column number: %d", columnNumber));

        this.message = message;
        this.sourceLine = sourceLine;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Creates a new ErrorMessage with no source line and {@code UNSPECIFIC} line and column numbers
     */
    public ErrorMessage(String message) {
        this(message, null, UNSPECIFIC, UNSPECIFIC);
    }

    public String format() {
        var sb = new StringBuilder();
        sb.append("Error");
        if (lineNumber != UNSPECIFIC)
            sb.append(" at line ").append(lineNumber);
        sb.append(": ").append(message).append('\n');
        if (sourceLine != null) {
            sb.append(sourceLine).append('\n');
            if (columnNumber != UNSPECIFIC)
                sb.append(String.format(String.format("%%%ds\n", columnNumber), "↑"));
        }

        return sb.toString();
    }

    public void print(PrintWriter out) {
        out.print(format());
        out.flush();
    }

    public void print(PrintStream out) {
        out.print(format());
        out.flush();
    }
}

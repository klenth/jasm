package edu.westminsteru.jasm.parser;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A class representing a syntax error while parsing jasm code. This class includes line and column numbers as well as the usual error message so that more helpful diagnostics can be displayed.
 * This exception is not thrown by {@link JasmParser} but rather passed to the {@link JasmParserListener} so that multiple errors can be reported at once.
 * @see JasmParserListener#exceptionOccurred(JasmParser, JasmSyntaxException)
 */
public class JasmSyntaxException extends RuntimeException {

    private int lineNumber, columnNumber;
    private String line;

    /**
     * Creates a {@code JasmSyntaxException}.
     * @param message       the error message
     * @param lineNumber    the line number at which the error occurred (counted from 1)
     * @param columnNumber  the column number at which the error occurred (counted from 1)
     * @param line          the line of text input containing the error
     */
    public JasmSyntaxException(String message, int lineNumber, int columnNumber, String line) {
        super(message);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.line = line;
    }

    /**
     * Returns the line number at which the error occurred.
     * @return the line number, counted from 1
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the column number (index within the line) at which the error occurred.
     * @return the column number, counted from 1
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Returns the text of the input line that contains the error.
     * @return the text of the input line
     */
    public String getLine() {
        return line;
    }

    /**
     * Prints a nicely-formatted message on the given {@link PrintWriter}.
     * @param out a {@code PrintWriter} to receive the message
     */
    public void print(PrintWriter out) {
        out.printf("Error on line %d: %s\n", lineNumber, getMessage());
        out.println(line);
        out.printf(String.format("%%%ds↑\n", columnNumber), "");
        out.println();
        out.flush();
    }

    /**
     * Prints a nicely-formatted message on the given {@link OutputStream}.
     * @param out an {@code OutputStream} to receive the message
     */
    public void print(OutputStream out) {
        print(new PrintWriter(out));
    }

    /**
     * Convenience method that prints the error message to {@link System#err {@code System.err}}.
     */
    public void print() {
        print(System.err);
    }
}

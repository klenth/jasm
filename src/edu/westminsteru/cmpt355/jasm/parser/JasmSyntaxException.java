package edu.westminsteru.cmpt355.jasm.parser;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class JasmSyntaxException extends RuntimeException {

    private int lineNumber, columnNumber;
    private String line;

    public JasmSyntaxException(String message, int lineNumber, int columnNumber, String line) {
        super(message);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.line = line;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getLine() {
        return line;
    }

    public void print(PrintWriter out) {
        out.printf("Error on line %d: %s\n", lineNumber, getMessage());
        out.println(line);
        out.printf(String.format("%%%ds↑\n", columnNumber), "");
        out.println();
        out.flush();
    }

    public void print(OutputStream out) {
        print(new PrintWriter(out));
    }

    public void print() {
        print(System.out);
    }
}

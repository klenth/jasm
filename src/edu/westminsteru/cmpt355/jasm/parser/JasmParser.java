package edu.westminsteru.cmpt355.jasm.parser;

import edu.westminsteru.cmpt355.jasm.Flags;
import edu.westminsteru.cmpt355.jasm.Operand;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class JasmParser {

    private enum State {
        Global, Code, Table
    }

    private BufferedReader in;
    private JasmParserListener listener;
    private int lineNumber = 0; // line numbers start at 1? I guess?
    private String line;
    private State state = State.Global;
    private StringBuilder stringBuilder = new StringBuilder(50);
    private boolean aborted = false;

    private static final String TYPE_REGEX = "(\\[)*([BSIJFDCZ]|L[a-zA-Z0-9_/$]+;)";
    private static Pattern TYPE_DESCRIPTOR_PATTERN = Pattern.compile(
        "^" + TYPE_REGEX + "$"
    );

    private static Pattern METHOD_SIGNATURE_DESCRIPTOR_PATTERN = Pattern.compile(
        "^\\(" +
            "(" + TYPE_REGEX + ")*" +
            "\\)" +
            "(" +
                "(" + TYPE_REGEX + ")" +
                "|V" +
            ")$"
    );

    public JasmParser(BufferedReader in, JasmParserListener listener) {
        this.in = in;
        this.listener = listener;
    }

    public JasmParser(Reader in, JasmParserListener listener) {
        this(new BufferedReader(in), listener);
    }

    public void parse() {
        in.lines().map(StringView::of).forEach(this::process);
        listener.endOfFile(this);
    }

    public void abortParsing() {
        this.aborted = true;
    }

    public String getCurrentLine() {
        return line;
    }

    public int getCurrentLineNumber() {
        return lineNumber;
    }

    private void process(StringView line) {
        if (aborted)
            return;
        this.line = line.source();
        ++lineNumber;

        // Strip out comment
        line = line.firstWord('#');

        // Get rid of extraneous whitespace
        line = line.trim();

        if (line.isBlank())
            return;

        StringView head = line.firstWord(' ').trim();
        StringView tail = line.tail(head).trim();

        if (state == State.Table) {
            if (head.toString().equals(".end") && tail.toString().equals("table"))
                processEndTableDirective(head, tail);
            else
                processTableLine(head, tail);
        } else {
            switch (head.toString()) {
                case ".source" -> processSourceDirective(head, tail);
                case ".class", ".interface", ".enum" -> processClassDirective(head, tail);
                case ".super" -> processSuperDirective(head, tail);
                case ".implements" -> processImplementsDirective(head, tail);
                case ".field" -> processFieldDirective(head, tail);
                case ".method" -> processMethodDirective(head, tail);
                case ".code" -> processCodeDirective(head, tail);
                case ".table" -> processTableDirective(head, tail);
                case ".end" -> {
                    if (tail.toString().equals("code"))
                        processEndCodeDirective(head, tail);
                    else
                        fireExceptionOccurred(
                            "Unexpected token: " + tail,
                            lineNumber, tail.start(),
                            tail.source()
                        );
                }

                default -> {
                    if (state == State.Code)
                        processCodeLine(head, tail);
                    else
                        fireExceptionOccurred(
                            "Unexpected token: " + head,
                            lineNumber, head.start(),
                            head.source()
                        );
                }
            }
        }
    }

    private void fireExceptionOccurred(
        String message,
        int lineNumber, int columnNumber,
        String line
    ) {
        var ex = new JasmSyntaxException(message, lineNumber, columnNumber, line);
        listener.exceptionOccurred(this, ex);
    }

    private void processSourceDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        StringView filename = tail.firstWord(' ', StringView.Quoted.Keep);
        var rest = tail.tail(filename);
        if (!rest.isBlank())
            fireExceptionOccurred(
                "Unexpected trailing text after .source directive",
                lineNumber, rest.trim().start(),
                tail.source()
            );
        listener.sourceDirective(this,
            filename
        );
    }

    private void processClassDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        StringView classId = head.substring(1);
        List<StringView> flags = tail.split(' ').stream()
            .filter(Predicate.not(StringView::isBlank))
            .map(StringView::trim)
            .toList();
        if (flags.isEmpty())
            fireExceptionOccurred(
                "Expected class name",
                lineNumber, tail.trim().start(),
                tail.source()
            );

        StringView className = flags.getLast();
        flags = flags.subList(0, flags.size() - 1);

        listener.classDirective(this,
            classId, flags, className
        );
    }

    private void processSuperDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        StringView superclassName = tail.firstWord(' ', StringView.Quoted.Keep);
        StringView rest = tail.tail(superclassName);
        if (!rest.isBlank())
            fireExceptionOccurred(
                "Unexpected trailing text after .super directive",
                lineNumber, rest.trim().start(),
                tail.source()
            );

        listener.superDirective(this,
            superclassName
        );
    }

    private void processImplementsDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        StringView interfaceName = tail.firstWord(' ', StringView.Quoted.Keep);
        StringView rest = tail.tail(interfaceName);
        if (!rest.isBlank())
            fireExceptionOccurred(
                "Unexpected trailing text after .super directive",
                lineNumber, rest.trim().start(),
                tail.source()
            );

        listener.implementsDirective(this,
            interfaceName
        );
    }

    private void processFieldDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        List<StringView> words = tail.split(' ', StringView.Quoted.Keep).stream()
            .filter(Predicate.not(StringView::isBlank))
            .map(StringView::trim)
            .toList();
        if (words.size() < 2)
            fireExceptionOccurred(
                "Too few operands to .field: expected name and type descriptor",
                lineNumber, tail.start(),
                tail.source()
            );
        StringView fieldName = words.get(words.size() - 2);
        StringView descriptor = words.getLast();
        List<StringView> flags = words.subList(0, words.size() - 2);

        boolean ok = validateTypeDescriptor(descriptor);

        ok &= flags.stream().map(Object::toString).allMatch(Flags::isValidFlag);

        if (ok)
            listener.fieldDirective(this,
                flags,
                fieldName,
                descriptor
            );
    }

    private void processMethodDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        List<StringView> words = tail.split(' ', StringView.Quoted.Keep).stream()
            .filter(Predicate.not(StringView::isBlank))
            .map(StringView::trim)
            .toList();
        if (words.size() < 2)
            fireExceptionOccurred(
                "Too few operands to .method: expected name and type descriptor",
                lineNumber, tail.start(),
                tail.source()
            );
        StringView methodName = words.get(words.size() - 2);
        StringView descriptor = words.getLast();
        List<StringView> flags = words.subList(0, words.size() - 2);

        boolean ok = validateMethodSignatureDescriptor(descriptor);

        ok &= flags.stream().map(Object::toString).allMatch(Flags::isValidFlag);

        if (ok)
            listener.methodDirective(this,
                flags,
                methodName,
                descriptor
            );
    }

    private void processCodeDirective(StringView head, StringView tail) {
        if (!checkGlobalState(head)) {
            abortParsing();
            return;
        }
        else if (!tail.isBlank())
            fireExceptionOccurred(
                "Unexpected trailing text after .code",
                lineNumber, tail.trim().start(),
                tail.source()
            );

        state = State.Code;
        listener.codeDirective(this);
    }

    private void processCodeLine(StringView head, StringView tail) {
        // only called by process() when in Code state
        if (head.toString().endsWith(":")) {
            StringView labelName = head.substring(0, head.length() - 1).trim();
            listener.codeLabel(this, labelName);
            processCodeLine(tail, StringView.of(""));
        } else if (head.toString().startsWith(".")) {
            fireExceptionOccurred(
                "Invalid directive",
                lineNumber, head.start(),
                head.source()
            );
        } else if (!head.isEmpty() && !Character.isAlphabetic(head.codePointAt(0))) {
            fireExceptionOccurred(
                "Unexpected token",
                lineNumber, head.start(),
                head.source()
            );
        } else if (!head.isBlank()) {
            List<StringView> operandStrings = tail.split(' ', StringView.Quoted.Ignore);
            listener.codeInstruction(this, head, operandStrings);
        }
    }

    private void processEndCodeDirective(StringView head, StringView tail) {
        if (state != State.Code) {
            fireExceptionOccurred(
                (state == State.Table) ? ".end code unexpected here (missing .end table?)" : ".end code unexpected here",
                lineNumber, head.start(),
                head.source()
            );
            abortParsing();
            return;
        } else if (!tail.toString().equals("code"))
            fireExceptionOccurred(
                "Unexpected trailing text after .end code",
                lineNumber, tail.tail(tail.firstWord(' ')).start(),
                head.source()
            );

        state = State.Global;
        listener.endCodeDirective(this);
    }

    private void processTableDirective(StringView head, StringView tail) {
        if (state != State.Code) {
            fireExceptionOccurred(
                ".table unexpected here",
                lineNumber, head.start(),
                head.source()
            );
            abortParsing();
            return;
        } else if (!tail.isBlank())
            fireExceptionOccurred(
                "Unexpected trailing text after .table",
                lineNumber, tail.trim().start(),
                tail.source()
            );

        state = State.Table;
        listener.tableDirective(this);
    }

    private void processTableLine(StringView head, StringView tail) {
        Runnable fireError = () -> fireExceptionOccurred(
            "Expected table entry",
            lineNumber, head.start(),
            head.source()
        );

        if (!head.toString().endsWith(":")) {
            return;
        } else if (tail.isBlank()) {
            fireError.run();
            return;
        }

        listener.tableLine(this, head.substring(0, head.length() - 1), tail);
    }

    private void processEndTableDirective(StringView head, StringView tail) {
        if (state != State.Table) {
            fireExceptionOccurred(
                ".end table unexpected here",
                lineNumber, head.start(),
                head.source()
            );
            abortParsing();
            return;
        } else if (!tail.toString().equals("table"))
            fireExceptionOccurred(
                "Unexpected trailing text after .end table",
                lineNumber, tail.tail(tail.firstWord(' ')).start(),
                head.source()
            );

        state = State.Code;
        listener.endTableDirective(this);
    }


    private boolean validateTypeDescriptor(StringView typeDesc) {
        if (!TYPE_DESCRIPTOR_PATTERN.matcher(typeDesc.toString()).matches()) {
            fireExceptionOccurred(
                "Invalid type descriptor: '" + typeDesc + "'",
                lineNumber, typeDesc.start(),
                typeDesc.source()
            );
            return false;
        }
        return true;
    }

    private boolean validateMethodSignatureDescriptor(StringView typeDesc) {
        if (!METHOD_SIGNATURE_DESCRIPTOR_PATTERN.matcher(typeDesc.toString()).matches()) {
            fireExceptionOccurred(
                "Invalid method descriptor",
                lineNumber, typeDesc.start(),
                typeDesc.source()
            );
            return false;
        }
        return true;
    }

    private boolean checkGlobalState(StringView head) {
        if (state != State.Global) {
            fireExceptionOccurred(
                head.toString() + " unexpected here (missing .end code?)",
                lineNumber, head.start(),
                head.source()
            );
            return false;
        }
        return true;
    }
}

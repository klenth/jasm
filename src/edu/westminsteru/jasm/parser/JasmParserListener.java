package edu.westminsteru.jasm.parser;

import java.util.List;

/**
 * A listener to be notified of events while parsing jasm code. An instance of this interface must be supplied when constructing a {@link edu.westminsteru.jasm.parser.JasmParser}.
 * These callback methods take {@link edu.westminsteru.jasm.parser.StringView} objects rather than bare {@link java.lang.String}s to help in creating helpful diagnostic messages.
 * All the methods have {@code default} implementations that do nothing.
 */
public interface JasmParserListener {

    /**
     * Notifies the listener that an exception has occurred while parsing due to some sort of error in the code. This method may be called with the parser in any state.
     * @param parser the parser that encountered an error
     * @param ex the error that occurred
     */
    default void exceptionOccurred(JasmParser parser, JasmSyntaxException ex) {}

    /**
     * Notifies the listener that a {@code .source} directive has been encountered. This method will only be called when the parser is in its initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     * @param source the source filename from the directive
     */
    default void sourceDirective(JasmParser parser, StringView source) {}

    /**
     * Notifies the listener that a {@code .class}/{@code .interface}/{@code .enum} directive has been encountered. This method will only be called when the parser is in its initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     * @param classId the type of class from the directive (either {@code ".class"}, {@code ".interface"}, or {@code ".enum"}
     * @param flags the flags that were included in the directive such as {@code "public"} and {@code "static"}.
     * @param className the class name specified in the directive
     */
    default void classDirective(JasmParser parser, StringView classId, List<StringView> flags, StringView className) {}

    /**
     * Notifies the listener that a {@code .super} directive has been encountered. This method will only be called when the parser is in its initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     * @param superName the name of the superclass from the directive
     */
    default void superDirective(JasmParser parser, StringView superName) {}

    /**
     * Notifies the listener that an {@code .implements} directive has been encountered. This method will only be called when the parser is in its initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     * @param interfaceName the name of the superinterface from the directive
     */
    default void implementsDirective(JasmParser parser, StringView interfaceName) {}

    /**
     * Notifies the listener that a {@code .field} directive has been encountered. This method will only be called when the parser is in its initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     * @param flags any flags from the directive (such as {@code "public"} or {@code "static"})
     * @param name the field name from the directive
     * @param descriptor the type descriptor from the directive
     */
    default void fieldDirective(JasmParser parser, List<StringView> flags, StringView name, StringView descriptor) {}

    /**
     * Notifies the listener that a {@code .method} directive has been encountered. This method will only be called when the parser is in its initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     * @param flags any flags from the directive (such as {@code "public"} or {@code "static"})
     * @param name the method name from the directive
     * @param descriptor the method type descriptor from the directive
     */
    default void methodDirective(JasmParser parser, List<StringView> flags, StringView name, StringView descriptor) {}

    /**
     * Notifies the listener that a {@code .code} directive has been encountered. Once this method has been called, the parser is in its {@link JasmParser.State#Code code} state, so only callbacks relevant to the code state will be called.
     * @param parser the parser
     */
    default void codeDirective(JasmParser parser) {}

    /**
     * Notifies the listener that the parser has encountered a label. This method will only be called when the parser is in its {@link JasmParser.State#Code code} state.
     * @param parser the parser
     * @param labelName the name of the label
     */
    default void codeLabel(JasmParser parser, StringView labelName) {}

    /**
     * Notifies the listener that the parser has encountered an instruction. This method will only be called when the parser is in its {@link JasmParser.State#Code code} state.
     * @param parser    the parser
     * @param opcode    the opcode (such as {@code "istore_3"} or {@code "invokevirtual"})
     * @param operands  a list of any operands provided (may be empty)
     */
    default void codeInstruction(JasmParser parser, StringView opcode, List<StringView> operands) {}

    /**
     * Notifies the listener that the parser has encountered an {@code .end code} directive. After this call the parser leaves its {@link JasmParser.State#Code code} state and reenters the initial {@link JasmParser.State#Global global} state.
     * @param parser the parser
     */
    default void endCodeDirective(JasmParser parser) {}

    /**
     * Notifies the listener that the parser has encountered a {@code .table} directive. After this call the parser enters its {@link JasmParser.State#Table table} state and only callbacks relevant to this state will be called.
     * @param parser the parser
     */
    default void tableDirective(JasmParser parser) {}

    /**
     * Notifies the listener that the parser has encountered one line of a table. This method will only be called when the parser is in its {@link JasmParser.State#Table table} state.
     * @param parser the parser
     * @param label the label of the table line
     * @param target the target of the table line
     */
    default void tableLine(JasmParser parser, StringView label, StringView target) {}

    /**
     * Notifies the listener that the parser has encountered an {@code .end table} directive. After this call the parser leaves its {@link JasmParser.State#Table table} state and reenters the {@link JasmParser.State#Code code} state.
     * @param parser the parser
     */
    default void endTableDirective(JasmParser parser) {}

    /**
     * Notifies the listener that the parser has exhausted the input. The parser will call no more methods of this listener.
     * @param parser the parser
     */
    default void endOfInput(JasmParser parser) {}
}

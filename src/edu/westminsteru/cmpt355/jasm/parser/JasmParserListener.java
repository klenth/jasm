package edu.westminsteru.cmpt355.jasm.parser;

import edu.westminsteru.cmpt355.jasm.Operand;

import java.util.List;

public interface JasmParserListener {

    default void exceptionOccurred(JasmParser parser, JasmSyntaxException ex) {}
    default void sourceDirective(JasmParser parser, StringView source) {}
    default void classDirective(JasmParser parser, StringView classId, List<StringView> flags, StringView className) {}
    default void superDirective(JasmParser parser, StringView superName) {}
    default void implementsDirective(JasmParser parser, StringView interfaceName) {}
    default void fieldDirective(JasmParser parser, List<StringView> flags, StringView name, StringView descriptor) {}
    default void methodDirective(JasmParser parser, List<StringView> flags, StringView name, StringView descriptor) {}
    default void codeDirective(JasmParser parser) {}
    default void codeLabel(JasmParser parser, StringView labelName) {}
    default void codeInstruction(JasmParser parser, StringView opcode, List<StringView> operands) {}
    default void endCodeDirective(JasmParser parser) {}
    default void endOfFile(JasmParser parser) {}
}

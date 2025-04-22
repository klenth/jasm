package edu.westminsteru.cmpt355.jasm.parser;

import edu.westminsteru.cmpt355.jasm.Operand;

import java.util.List;

public interface JasmParserListener {

    default void exceptionOccurred(JasmParser parser, JasmSyntaxException ex) {}
    default void sourceDirective(JasmParser parser, String source) {}
    default void classDirective(JasmParser parser, String classId, List<String> flags, String className) {}
    default void superDirective(JasmParser parser, String superName) {}
    default void implementsDirective(JasmParser parser, String interfaceName) {}
    default void fieldDirective(JasmParser parser, List<String> flags, String name, String descriptor) {}
    default void methodDirective(JasmParser parser, List<String> flags, String name, String descriptor) {}
    default void codeDirective(JasmParser parser) {}
    default void codeLabel(JasmParser parser, String labelName) {}
    default void codeInstruction(JasmParser parser, String opcode, List<Operand> operands) {}
    default void endCodeDirective(JasmParser parser) {}
    default void endOfFile(JasmParser parser) {}
}

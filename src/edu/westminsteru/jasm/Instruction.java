package edu.westminsteru.jasm;

import edu.westminsteru.jasm.parser.StringView;

import java.util.List;

record Instruction(List<StringView> labels, StringView opcode, List<StringView> operands, String text, int line)
    implements CodeItem {
}

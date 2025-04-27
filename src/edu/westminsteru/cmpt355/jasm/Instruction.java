package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.util.List;

public record Instruction(List<StringView> labels, StringView opcode, List<StringView> operands, String text, int line)
    implements CodeItem {
}

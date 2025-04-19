package edu.westminsteru.cmpt355.jasm;

import java.util.List;

public record Instruction(List<String> labels, String opcode, List<Operand> operands, String text, int line) {
}

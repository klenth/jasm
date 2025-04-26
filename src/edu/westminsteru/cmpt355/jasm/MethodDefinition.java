package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.util.List;

public record MethodDefinition(List<StringView> flags, StringView methodName, StringView descriptor) {
}

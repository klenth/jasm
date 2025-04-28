package edu.westminsteru.jasm;

import edu.westminsteru.jasm.parser.StringView;

import java.util.List;

record MethodDefinition(List<StringView> flags, StringView methodName, StringView descriptor) {
}

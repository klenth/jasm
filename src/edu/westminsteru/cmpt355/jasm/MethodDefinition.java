package edu.westminsteru.cmpt355.jasm;

import java.util.List;

public record MethodDefinition(List<String> flags, String methodName, String descriptor) {
}

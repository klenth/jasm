package edu.westminsteru.cmpt355.jasm;

import java.util.List;

public record FieldDefinition(List<String> flags, String fieldName, String fieldDescriptor) {
}

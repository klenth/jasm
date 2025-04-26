package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.util.List;

public record FieldDefinition(List<StringView> flags, StringView fieldName, StringView fieldDescriptor) {
}

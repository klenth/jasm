package edu.westminsteru.jasm;

import edu.westminsteru.jasm.parser.StringView;

import java.util.List;

record FieldDefinition(List<StringView> flags, StringView fieldName, StringView fieldDescriptor) {
}

package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.util.List;

public record Table(List<Entry> entries) implements CodeItem {
    record Entry(StringView label, StringView target) {}
}

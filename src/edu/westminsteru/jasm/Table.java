package edu.westminsteru.jasm;

import edu.westminsteru.jasm.parser.StringView;

import java.util.List;

record Table(List<Entry> entries) implements CodeItem {
    record Entry(StringView label, StringView target) {}
}

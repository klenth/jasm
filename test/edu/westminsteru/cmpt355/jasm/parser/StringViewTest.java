package edu.westminsteru.cmpt355.jasm.parser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static edu.westminsteru.cmpt355.jasm.parser.StringView.Quoted.*;

class StringViewTest {

    private static StringView sv(String text) {
        return StringView.of(text);
    }

    @org.junit.jupiter.api.Test
    void firstWord() {
        assertEquals("abc", sv("abc 123").firstWord(' ', Keep).toString());
        assertEquals("ab\"c 1\"23", sv("ab\"c 1\"23").firstWord(' ', Ignore).toString());
        assertEquals("", sv(" abc").firstWord(' ', Keep).toString());
        assertEquals("ab\"c", sv("ab\"c 1\"23").firstWord(' ', Keep).toString());
        assertEquals("", sv("").firstWord(' ').toString());
        assertEquals("", new StringView("abc", 3, 3).firstWord(' ').toString());
    }

    @org.junit.jupiter.api.Test
    void trim() {
        assertEquals("abc", sv("abc").trim().toString());
        assertEquals("abc", sv(" abc").trim().toString());
        assertEquals("abc", sv("abc ").trim().toString());
        assertEquals("abc", sv(" abc ").trim().toString());
        assertEquals("", sv("").trim().toString());
        assertEquals("", sv("     ").trim().toString());
    }

    @org.junit.jupiter.api.Test
    void tail() {
        var abc123 = sv("abc 123");
        assertEquals(" 123", abc123.tail(abc123.firstWord(' ')).toString());
    }

    @org.junit.jupiter.api.Test
    void split() {
        assertEquals(List.of("abc", "123", "xyz").toString(), sv("abc 123 xyz").split(' ').toString());
        assertEquals(List.of("", "abc").toString(), sv(" abc").split(' ').toString());
    }
}
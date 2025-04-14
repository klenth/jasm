package edu.westminsteru.cmpt355.jasm.parser;

import java.util.ArrayList;
import java.util.List;

public record StringView(String source, int start, int end) {

    public enum Quoted {
        /** Ignore delimiters inside double quotes */
        Ignore,

        /** Keep delimiters inside double quotes */
        Keep
    }

    public StringView(String source, int start, int end) {
        if (start < 0 || start > source.length()
                || end < 0 || end > source.length()) {
            throw new StringIndexOutOfBoundsException(String.format(
                "Invalid range for string of length %d: start=%d, end=%d",
                source.length(), start, end
            ));
        }

        this.source = source;
        this.start = start;
        this.end = end;
    }

    public static StringView of(String source) {
        return new StringView(source, 0, source.length());
    }

    public StringView firstWord(int ch) {
        return firstWord(ch, Quoted.Ignore);
    }

    public StringView firstWord(int ch, Quoted q) {
        if (isEmpty())
            return substring(0, 0);
        boolean insideQuotes = false;
        for (int i = 0; i < length(); ++i) {
            int c = codePointAt(i);
            if (c == ch && !insideQuotes)
                return substring(0, i);
            else if (q == Quoted.Ignore && c == '"')
                insideQuotes = !insideQuotes;
        }

        return this;
    }

    public int codePointAt(int i) {
        return source.codePointAt(i + start);
    }

    public int length() {
        return end - start;
    }

    public int indexOf(int ch) {
        for (int i = 0; i < length(); ++i)
            if (codePointAt(i) == ch)
                return i;
        return -1;
    }

    public StringView substring(int start, int end) {
        int sstart = this.start + start,
            send = this.start + end;
        if (sstart > source.length())
            sstart = source.length();
        if (send > source.length())
            send = source.length();
        return new StringView(source, sstart, send);
    }

    public StringView substring(int start) {
        return substring(start, this.length());
    }

    public StringView trim() {
        int l, r;

        for (r = length() - 1; r >= 0 && Character.isWhitespace(codePointAt(r)); --r)
            ; // do nothing

        for (l = 0; l < r && Character.isWhitespace(codePointAt(l)); ++l)
            ; // do nothing

        return new StringView(source, start + l, start + r + 1);
    }

    public boolean isBlank() {
        for (int i = 0; i < length(); ++i)
            if (!Character.isWhitespace(codePointAt(i)))
                return false;
        return true;
    }

    public boolean isEmpty() {
        return start >= end;
    }

    public StringView tail(StringView head) {
        if (source != head.source)
            throw new IllegalArgumentException("tail() called for StringView on different string");
        return new StringView(source, head.end, this.end);
    }

    public List<StringView> split(int ch, Quoted q) {
        List<StringView> words = new ArrayList<>();
        StringView tail = this;
        while (!tail.isEmpty()) {
            StringView head = tail.firstWord(ch, q);
            tail = tail.tail(head).substring(1);
            words.add(head);
        }
        return words;
    }

    public List<StringView> split(int ch) {
        return split(ch, Quoted.Ignore);
    }

    @Override
    public String toString() {
        return source.substring(start, end);
    }
}

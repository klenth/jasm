package edu.westminsteru.jasm.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a “slice” of a {@link String}, preserving the index information. {@link JasmParser} uses this class extensively so that better diagnostics can be displayed to the user.
 * This class is designed to replace many of the core features of a {@code String}, so many methods are inspired by {@code String} methods.
 * @param source the source string that this {@code StringView} represents a slice of
 * @param start the beginning index (inclusive) of this slice
 * @param end the ending index (exclusive) of this slice
 */
public record StringView(String source, int start, int end) {

    /**
     * An enum of ways that a {@code StringView} can handle quotes.
     */
    public enum Quoted {
        /** Ignore delimiters inside double quotes */
        Ignore,

        /** Keep delimiters inside double quotes */
        Keep
    }

    /**
     * Creates a new {@code StringView}.
     * @param source the source string that this {@code StringView} represents a slice of
     * @param start the beginning index (inclusive) of this slice
     * @param end the ending index (exclusive) of this slice
     */
    public StringView {
        if (start < 0 || start > source.length()
            || end < 0 || end > source.length()) {
            throw new StringIndexOutOfBoundsException(String.format(
                "Invalid range for string of length %d: start=%d, end=%d",
                source.length(), start, end
            ));
        }

    }

    /**
     * Creates a {@code StringView} consisting of the entire content of a {@code String}.
     * @param source the string content for this {@code StringView}
     * @return a newly created {@code StringView}
     */
    public static StringView of(String source) {
        return new StringView(source, 0, source.length());
    }

    /**
     * Returns a {@code StringView} consisting of the content of the string up to (but not including) the first occurrence of a given codepoint. (This method is the same as calling {@link #firstWord(int, Quoted)} with {@link Quoted#Ignore} and ignores any occurrences of the delimiter inside quotes.)
     * @param ch the codepoint of the delimiter
     * @return a {@code StringView} up to the first occurrence of the delimiter, or the same view if the delimiter is not present
     */
    public StringView firstWord(int ch) {
        return firstWord(ch, Quoted.Ignore);
    }

    /**
     * Returns a {@code StringView} consisting of the content of the string up to (but not including) the first occurrence of a given codepoint.
     * If {@code q} is {@code StringView.Quoted.Ignore}, any occurrences of the delimiter between double quotes {@code " "} are ignored.
     * @param ch the codepoint of the delimiter
     * @param q the quote mode to follow
     * @return a {@code StringView} up to the first occurrence of the delimiter, or the same view if the delimiter is not present
     */
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

    /**
     * Returns the code point at a specified index.
     * @param i the index
     * @return the code point at index {@code i}}
     * @see String#codePointAt(int) 
     */
    public int codePointAt(int i) {
        return source.codePointAt(i + start);
    }

    /**
     * Returns the length of this {@code StringView}.
     * @return the length
     */
    public int length() {
        return end - start;
    }

    /**
     * Returns the first index of the given code point in this {@code StringView}, or {@code -1} if it is not present.
     * @param ch the code point to look for
     * @return the index, or {@code -1} if not found
     * @see String#indexOf(int) 
     */
    public int indexOf(int ch) {
        for (int i = 0; i < length(); ++i)
            if (codePointAt(i) == ch)
                return i;
        return -1;
    }

    /**
     * Returns the first index of the given code point in this {@code StringView}, or {@code -1} if it is not present.
     * If {@code q} is {@code StringView.Quoted.Ignore}, any occurrences of the code point between double quotes are ignored.
     * @param ch the code point to look for
     * @param q the quote mode to follow
     * @return the index, or {@code -1} if not found
     * @see String#indexOf(int)
     */
    public int indexOf(int ch, Quoted q) {
        boolean insideQuotes = false;
        for (int i = 0; i < length(); ++i) {
            int c = codePointAt(i);
            if (c == ch && !insideQuotes)
                return i;
            else if (q == Quoted.Ignore && c == '"')
                insideQuotes = !insideQuotes;
        }

        return -1;
    }

    /**
     * Returns a substring of this {@code StringView}.
     * @param start the beginning index (inclusive)
     * @param end the ending index (exclusive)
     * @return the substring
     * @see String#substring(int, int)
     */
    public StringView substring(int start, int end) {
        int sstart = this.start + start,
            send = this.start + end;
        if (sstart > source.length())
            sstart = source.length();
        if (send > source.length())
            send = source.length();
        return new StringView(source, sstart, send);
    }

    /**
     * Returns a substring of this {@code StringView} starting at a given index and proceeding to the end.
     * @param start the beginning index (inclusive)
     * @return the substring
     * @see String#substring(int)
     */
    public StringView substring(int start) {
        return substring(start, this.length());
    }

    /**
     * Removes any whitespace at the beginning and end of this {@code StringView}.
     * @return a {@code StringView} without whitespace on the ends
     * @see String#trim()
     */
    public StringView trim() {
        int l, r;

        for (r = length() - 1; r >= 0 && Character.isWhitespace(codePointAt(r)); --r)
            ; // do nothing

        for (l = 0; l < r && Character.isWhitespace(codePointAt(l)); ++l)
            ; // do nothing

        return new StringView(source, start + l, start + r + 1);
    }

    /**
     * Returns {@code true} if the entire contents of this {@code StringView} is whitespace.
     * @return whether this {@code StringView} is whitespace only
     * @see String#isBlank()
     */
    public boolean isBlank() {
        for (int i = 0; i < length(); ++i)
            if (!Character.isWhitespace(codePointAt(i)))
                return false;
        return true;
    }

    /**
     * Returns {@code true} if this {@code StringView} has no characters (i.e., has length zero).
     * @return whether this {@code StringView} has no characters
     * @see String#isEmpty()
     */
    public boolean isEmpty() {
        return start >= end;
    }

    /**
     * Returns the remaining content of a {@code StringView} when a prefix is removed.
     * @param head the prefix to remove
     * @return the content of this {@code StringView} after {@code head}
     * @throws IllegalArgumentException if {@code head}'s backing {@code String} is different from this one's
     */
    public StringView tail(StringView head) {
        if (source != head.source)
            throw new IllegalArgumentException("tail() called for StringView on different string");
        return new StringView(source, head.end, this.end);
    }

    /**
     * Splits this {@code StringView} around the given delimiter (code point). If {@code q} is {@code StringView.Quoted.Ignore}, then any instances of the delimiter inside double quotes will not be split around.
     * @param ch the delimiter (code point)
     * @param q the quote mode to follow
     * @return the split {@code StringView}s
     */
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

    /**
     * Splits this {@code StringView} around the given delimiter (code point). Any instances of the delimiter inside double quotes will not be split around.
     * @param ch the delimiter (code point)
     * @return the split {@code StringView}s
     */
    public List<StringView> split(int ch) {
        return split(ch, Quoted.Ignore);
    }

    /**
     * Returns the {@code String} content of this {@code StringView}.
     * @return the {@code String} content
     */
    @Override
    public String toString() {
        return source.substring(start, end);
    }
}

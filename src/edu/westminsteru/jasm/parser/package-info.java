/**
 * Classes related to parsing jasm code. The key classes are
 * <ul>
 *     <li>{@link edu.westminsteru.jasm.parser.JasmParser} does the actual parsing.</li>
 *     <li>{@link edu.westminsteru.jasm.parser.JasmParserListener} is an interface that must be implemented to be notified of events while the {@code JasmParser} parses.</li>
 *     <li>{@link edu.westminsteru.jasm.parser.StringView} represents a “slice” of a {@link java.lang.String} and is used extensively by {@code JasmParser}.</li>
 * </ul>
 */
package edu.westminsteru.jasm.parser;
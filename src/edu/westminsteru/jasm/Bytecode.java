package edu.westminsteru.jasm;

/**
 * A class representing assembled bytecode for a class.
 * @param className the name of the assembled class
 * @param data the bytecode, in a form suitable to be saved to a .class file or handed to a {@link ClassLoader}
 */
public record Bytecode(String className, byte[] data) {
}

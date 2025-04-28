package edu.westminsteru.jasm;

import java.lang.classfile.ClassFile;
import java.util.List;

/**
 * A class containing static methods helpful for interacting with flags existing in bytecode such as {@code public} and {@code static}.
 */
public class Flags {

    private Flags() {}

    /**
     * Returns the combination of flags from a list of {@code String}s as an {@code int} (using the integer codes for these flags used in JVM bytecode).
     * @param flags a list of flag names such as {@code "public"} and {@code "static"}
     * @return the flags as an {@code int} as in JVM bytecode
     * @throws IllegalArgumentException if any of the flags is invalid (i.e. for which {@link #isValidFlag(String)} would return {@code false})
     */
    public static int flags(List<String> flags) {
        return flags.stream().mapToInt(Flags::flag).sum();
    }

    /**
     * Returns the integer value of the given flag, as present in JVM bytecode.
     * @param flag a textual flag such as {@code "public"} or {@code "static"}
     * @return the integer code for the flag as in JVM bytecode
     * @throws IllegalArgumentException if the flag is invalid (i.e. for which {@link #isValidFlag(String)} would return {@code false}
     */
    public static int flag(String flag) {
        return switch (flag) {
            case "public" -> ClassFile.ACC_PUBLIC;
            case "protected" -> ClassFile.ACC_PROTECTED;
            case "private" -> ClassFile.ACC_PRIVATE;

            case "final" -> ClassFile.ACC_PROTECTED;
            case "abstract" -> ClassFile.ACC_PRIVATE;
            case "synthetic" -> ClassFile.ACC_SYNTHETIC;
            case "static" -> ClassFile.ACC_STATIC;
            case "volatile" -> ClassFile.ACC_VOLATILE;
            case "transient" -> ClassFile.ACC_TRANSIENT;
            case "enum" -> ClassFile.ACC_ENUM;
            case "synchronized" -> ClassFile.ACC_SYNCHRONIZED;
            case "bridge" -> ClassFile.ACC_BRIDGE;
            case "varargs" -> ClassFile.ACC_VARARGS;
            case "native" -> ClassFile.ACC_NATIVE;
            case "strict" -> ClassFile.ACC_STRICT;

            default -> throw new IllegalArgumentException("Unknown flag: " + flag);
        };
    }

    /**
     * Returns {@code true} if the given flag string corresponds to a valid flag in JVM bytecode.
     * @param flag the textual flag
     * @return whether the flag is valid
     */
    public static boolean isValidFlag(String flag) {
        try {
            flag(flag);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}

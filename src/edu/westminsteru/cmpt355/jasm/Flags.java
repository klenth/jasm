package edu.westminsteru.cmpt355.jasm;

import java.lang.classfile.ClassFile;
import java.util.List;

public class Flags {
    public static int flags(List<String> flags) {
        return flags.stream().mapToInt(Flags::flag).sum();
    }

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
}

package edu.westminsteru.cmpt355.jasm;

public sealed interface Operand {

    record Int(int value) implements Operand {
    }

    record Float(float value) implements Operand {
    }

    record Long(long value) implements Operand {

    }
    record Double(double value) implements Operand {

    }

    record String(java.lang.String value) implements Operand {
    }

    record ClassName(java.lang.String value) implements Operand {
    }

    enum ArrayType implements Operand {
        Byte, Short, Int, Long, Float, Double, Char, Boolean
    }

    record Identifier(java.lang.String value) implements Operand {}

    record Descriptor(java.lang.String text) implements Operand {}

    record MethodDescriptor(java.lang.String text) implements Operand {}

    record BranchTarget(java.lang.String text) implements Operand {}
}

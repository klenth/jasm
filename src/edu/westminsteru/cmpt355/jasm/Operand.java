package edu.westminsteru.cmpt355.jasm;

public sealed interface Operand permits
    Operand.Int, Operand.Long, Operand.Double, Operand.Float, Operand.String,
    Operand.Identifier
{

    record Int(java.lang.String text) implements Operand {
        public int value() {
            return text.toLowerCase().startsWith("0x")
                ? Integer.parseInt(text.substring(2), 16)
                : Integer.parseInt(text);
        }
    }

    record Long(java.lang.String text) implements Operand {
        public long value() {
            java.lang.String text = this.text.substring(0, this.text.length() - 1); // lose the L
            return text.toLowerCase().startsWith("0x")
                ? java.lang.Long.parseLong(text.substring(2), 16)
                : java.lang.Long.parseLong(text);
        }
    }

    record Double(java.lang.String text) implements Operand {
        public double value() {
            return java.lang.Double.parseDouble(text);
        }
    }

    record Float(java.lang.String text) implements Operand {
        public float value() {
            return java.lang.Float.parseFloat(text.substring(0, text.length() - 1));
        }
    }

    record String(java.lang.String text) implements Operand {
        public java.lang.String value() {
            return text.substring(1, text.length() - 1);
        }
    }

    record Identifier(java.lang.String text) implements Operand {}
}

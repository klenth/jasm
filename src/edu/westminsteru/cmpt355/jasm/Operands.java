package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.util.Map;
import java.util.regex.*;

public class Operands {

    private static final String INTEGER_REGEX =
        // decimal
        "("
            + "[+-]?[0-9]+"
        + ")|"
        // hexadecimal
        + "("
            + "0[Xx]([0-9a-fA-F]+)"
        + ")"
    ;

    private static final String REAL_REGEX =
        // digits before .
        "[+-]?[0-9]+\\.[0-9]*"
        // digits after .
        + "|[+-]?[0-9]*\\.[0-9]+"
        // possibly with exponent
        + "([Ee][+-]?[0-9]+)?"
    ;

    private static final Pattern INT_PATTERN = Pattern.compile(
        "^(" + INTEGER_REGEX + ")$"
    );

    private static final Pattern LONG_PATTERN = Pattern.compile(
        "^(" + INTEGER_REGEX + ")[Ll]$"
    );

    private static final Pattern DOUBLE_PATTERN = Pattern.compile(
        "^("
            + "(" + REAL_REGEX + ")" // written as floating-point
            + "|(" + INTEGER_REGEX + ")[Dd][Bb]" // written as bits
        + ")$"
    );

    private static final Pattern FLOAT_PATTERN = Pattern.compile(
        "^("
            + "(" + REAL_REGEX + ")[Ff]"
            + "|(" + INTEGER_REGEX + ")[Ff][Bb]"
        + ")$"
    );

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
        "^([a-zA-Z_$][a-zA-Z0-9$/]+)$"
    );

    private static final String DESCRIPTOR_REGEX = "(\\[)*([BSIJFDCZ]|L[a-zA-Z0-9_/$]+;)";
    
    private static Pattern DESCRIPTOR_PATTERN = Pattern.compile(
        "^" + DESCRIPTOR_REGEX + "$"
    );

    private static Pattern METHOD_DESCRIPTOR_PATTERN = Pattern.compile(
        "^\\(" +
            "(" + DESCRIPTOR_REGEX + ")*" +
            "\\)" +
            "(" +
            "(" + DESCRIPTOR_REGEX + ")" +
            "|V" +
            ")$"
    );

    public static Operand parseOperand(StringView text, OperandType type) throws AssemblyException {
        return switch (type) {
            case Int -> parseInt(text);
            case Float -> parseFloat(text);
            case Long -> parseLong(text);
            case Double -> parseDouble(text);
            case String -> parseString(text);
            case ClassName -> parseClassName(text);
            case ArrayType -> parseArrayType(text);
            case Identifier -> parseIdentifier(text);
            case Descriptor -> parseDescriptor(text);
            case MethodDescriptor -> parseMethodDescriptor(text);
            case BranchTarget -> parseBranchTarget(text);
        };
    }

    public static Operand.Int parseInt(StringView text) throws AssemblyException {
        String s = text.toString();
        if (s.codePointAt(0) == '\'' && s.codePointAt(s.length() - 1) == '\'') {
            s = s.substring(1, s.length() - 1);
            if (s.startsWith("\\") && s.length() == 2)
                return new Operand.Int(unescape(text.substring(1)));
            else if (s.length() == 1)
                return new Operand.Int((int)s.codePointAt(0));
            else
                throw new AssemblyException("Invalid character", text);
        }

        var matcher = INT_PATTERN.matcher(text.toString());
        try {
            if (matcher.matches()) {
                if (!matcher.group(2).isBlank())
                    return new Operand.Int(Integer.parseInt(matcher.group(2)));
                else if (!matcher.group(4).isBlank())
                    return new Operand.Int(Integer.parseInt(matcher.group(4), 16));
            }
        } catch (NumberFormatException _) {
            // do nothing
        }

        throw new AssemblyException("Invalid integer", text);
    }

    public static Operand.Float parseFloat(StringView text) throws AssemblyException {
        String s = text.toString().toLowerCase();
        try {
            if (s.endsWith("f"))
                return new Operand.Float(Float.parseFloat(s.substring(0, s.length() - 1)));
            else if (s.endsWith("fb"))
                return new Operand.Float(Float.intBitsToFloat(Integer.parseInt(s.substring(0, s.length() - 2))));
        } catch (NumberFormatException ex) {
            // do nothing
        }

        throw new AssemblyException("Invalid float", text);
    }

    public static Operand.Long parseLong(StringView text) throws AssemblyException {
        var matcher = LONG_PATTERN.matcher(text.toString());
        try {
            if (matcher.matches()) {
                if (!matcher.group(2).isBlank())
                    return new Operand.Long(Long.parseLong(matcher.group(2)));
                else if (!matcher.group(4).isBlank())
                    return new Operand.Long(Long.parseLong(matcher.group(4), 16));
            }
        } catch (NumberFormatException _) {
            // do nothing
        }

        throw new AssemblyException("Invalid long", text);
    }

    public static Operand.Double parseDouble(StringView text) throws AssemblyException {
        String s = text.toString().toLowerCase();
        try {
            if (s.endsWith("db"))
                return new Operand.Double(Double.longBitsToDouble(Long.parseLong(s.substring(0, s.length() - 2))));
            else
                return new Operand.Double(Double.parseDouble(s));
        } catch (NumberFormatException _) {
            // do nothing
        }

        throw new AssemblyException("Invalid double", text);
    }

    public static Operand.String parseString(StringView text) throws AssemblyException {
        String s = text.toString();
        if (!s.startsWith("\"") || !s.endsWith("\""))
            throw new AssemblyException("Invalid string", text);
        s = s.substring(1, s.length() - 1);
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); ++i) {
            int ch = s.codePointAt(i);
            if (ch == '\\' && i + 1 < s.length())
                sb.append(unescape(text.substring(++i)));
            else if (ch == '\\' && i + 1 == s.length())
                throw new AssemblyException("Invalid escape sequence", text.substring(i));
            else
                sb.appendCodePoint(ch);
        }

        return new Operand.String(sb.toString());
    }

    public static Operand.ClassName parseClassName(StringView text) throws AssemblyException {
        var matcher = CLASS_NAME_PATTERN.matcher(text.toString());
        if (matcher.matches())
            return new Operand.ClassName(matcher.group(1));

        throw new AssemblyException("Invalid class name", text);
    }

    public static Operand.ArrayType parseArrayType(StringView text) throws AssemblyException {
        return switch (text.toString()) {
            case "byte" -> Operand.ArrayType.Byte;
            case "short" -> Operand.ArrayType.Short;
            case "int" -> Operand.ArrayType.Int;
            case "long" -> Operand.ArrayType.Long;
            case "float" -> Operand.ArrayType.Float;
            case "double" -> Operand.ArrayType.Double;
            case "char" -> Operand.ArrayType.Char;
            case "boolean" -> Operand.ArrayType.Boolean;
            default -> throw new AssemblyException("Invalid array type", text);
        };
    }

    public static Operand.Identifier parseIdentifier(StringView text) throws AssemblyException {
        return new Operand.Identifier(text.toString());
    }

    public static Operand.Descriptor parseDescriptor(StringView text) throws AssemblyException {
        var matcher = DESCRIPTOR_PATTERN.matcher(text.toString());
        if (matcher.matches())
            return new Operand.Descriptor(text.toString());
        
        throw new AssemblyException("Invalid type descriptor", text);
    }
    
    public static Operand.MethodDescriptor parseMethodDescriptor(StringView text) throws AssemblyException {
        var matcher = METHOD_DESCRIPTOR_PATTERN.matcher(text.toString());
        if (matcher.matches())
            return new Operand.MethodDescriptor(text.toString());

        throw new AssemblyException("Invalid method descriptor", text);
    }

    public static Operand.BranchTarget parseBranchTarget(StringView text) throws AssemblyException {
        return new Operand.BranchTarget(text.toString());
    }

    private static char unescape(StringView view) throws AssemblyException{
        int c = view.codePointAt(1);
        return switch (c) {
            case '0' -> '\0';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case '"' -> '\"';
            case '\'' -> '\'';
            case '\\' -> '\\';
            case 'u' -> {
                throw new AssemblyException(
                    "Unicode escape sequences \\uXXXX not supported", view
                );
            }
            default -> {
                throw new AssemblyException(
                    "Invalid escape sequence", view
                );
            }
        };
    }

}

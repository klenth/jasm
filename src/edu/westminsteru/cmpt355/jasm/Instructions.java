package edu.westminsteru.cmpt355.jasm;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

public class Instructions {

    private static final Pattern LOAD_STORE_PATTERN = Pattern.compile(
        "^[adfil](load|store)(_[0123])?$"
    );

    public static void enter(Instruction instr, CodeBuilder cb, Map<String, Label> labels) throws AssemblyException {
        // Go through all the label names on this instruction - if isn't already in the labels map, create and bind it;
        // if it is, bind it to this instruction
        for (var labelName : instr.labels()) {
            switch (labels.getOrDefault(labelName, null)) {
                case null -> labels.put(labelName, cb.newBoundLabel());
                case Label label -> cb.labelBinding(label);
            }
        }

        var isLoadStore = LOAD_STORE_PATTERN.asPredicate();
        var operands = instr.operands();
        switch (instr.opcode()) {
            case String opcode when isLoadStore.test(opcode) ->
                enterLoadStore(instr, cb);

            case "anewarray", "checkcast", "instanceof" -> enterClassDesc(instr, cb);

            case "getfield", "putfield", "getstatic", "putstatic" -> enterFieldDesc(instr, cb);

            case "goto", "goto_w"  -> enterBranchTarget(instr, cb, labels);

            // if_acmp<cond>, if_icmp<cond>, if<cond>, ifnonnull, ifnull
            case
                "if_acmpeq", "if_acmpne",
                "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmple", "if_icmpgt", "if_icmpge",
                "ifeq", "ifne", "iflt", "ifle", "ifgt", "ifge",
                "ifnonnull", "ifnull" -> enterBranchTarget(instr, cb, labels);

            case "ldc", "ldc_w", "ldc2", "ldc2_w" -> enterLdc(instr, cb);

            // invokedynamic: complicated, probably won't support
            // lookupswitch/tableswitch: maybe, not yet
            // jsr, jsr_w, ret: no longer supported as of JVMS 4.9.1
            // wide: probably not (only needed for more than 255 locals)
            case "invokedynamic",
                 "lookupswitch", "tableswitch",
                 "jsr", "jsr_w", "ret" -> throw new AssemblyException("Unsupported instruction: " + instr.opcode());

            case "invokeinterface", "invokespecial", "invokestatic", "invokevirtual" -> enterMethodDesc(instr, cb);

            case "multianewarray" -> {
                testOperands(instr, Operand.Identifier.class, Operand.Int.class);
                String className = ((Operand.Identifier)operands.getFirst()).text();
                var classDesc = ownerClassDesc(className);
                int dims = ((Operand.Int)operands.get(1)).value();
                enter(instr.opcode(), new Object[] { classDesc, (Integer)dims }, cb);
            }

            case "new" -> {
                testOperands(instr, Operand.Identifier.class);
                String className = ((Operand.Identifier)operands.getFirst()).text();
                var classDesc = ownerClassDesc(className);
                enter(instr.opcode(), new Object[] { classDesc }, cb);
            }

            case "newarray" -> {
                testOperands(instr, Operand.Identifier.class);
                TypeKind tk = switch (((Operand.Identifier)instr.operands().getFirst()).text()) {
                    case "boolean" -> TypeKind.BOOLEAN;
                    case "char" -> TypeKind.CHAR;
                    case "float" -> TypeKind.FLOAT;
                    case "double" -> TypeKind.DOUBLE;
                    case "byte" -> TypeKind.BYTE;
                    case "short" -> TypeKind.SHORT;
                    case "int" -> TypeKind.INT;
                    case "long" -> TypeKind.LONG;
                    default -> throw new AssemblyException(String.format("Invalid operand for opcode %s", instr.opcode()));
                };

                enter(instr.opcode(), new Object[] { tk }, cb);
            }

            default -> enter(instr.opcode(), reifyOperands(instr.operands()), cb);
        }
    }

    /* for any (non-array) load/store instruction, e.g. iload_2 or dstore */
    private static void enterLoadStore(Instruction instr, CodeBuilder cb) throws AssemblyException {
        String opcode = instr.opcode();
        Object[] operands = null;
        if (opcode.charAt(opcode.length() - 2) == '_') {
            // aload_2/istore_1/...
            if (!instr.operands().isEmpty())
                throw new AssemblyException(String.format("Opcode %s should have no operands", opcode));
            int localNum = Integer.parseInt(opcode.substring(opcode.length() - 1));
            operands = new Object[] { (Integer)localNum };
            opcode = opcode.substring(0, opcode.length() - 2);
        } else {
            if (instr.operands().size() != 1)
                throw new AssemblyException(String.format("Opcode %s takes one operand", opcode));
            if (instr.operands().getFirst() instanceof Operand.Int oint)
                operands = new Object[] { (Integer) oint.value() };
            else
                throw new AssemblyException(String.format("Operand to opcode %s must be integer", opcode));
        }

        enter(opcode, operands, cb);
    }

    /* for any instruction taking a single class descriptor as operand, e.g. anewarray, checkcast */
    private static void enterClassDesc(Instruction instr, CodeBuilder cb) throws AssemblyException {
        testOperands(instr, Operand.Identifier.class);
        var id = (Operand.Identifier)instr.operands().getFirst();
        enter(instr.opcode(), new Object[] { ClassDesc.ofDescriptor(id.text()) }, cb);
    }

    /* for any instruction naming a field (e.g. getfield, putstatic) */
    private static void enterFieldDesc(Instruction instr, CodeBuilder cb) throws AssemblyException {
        var operands = instr.operands();
        testOperands(instr, Operand.Identifier.class, Operand.Identifier.class, Operand.Identifier.class);
        String owner = ((Operand.Identifier)operands.getFirst()).text();
        String name = ((Operand.Identifier)operands.get(1)).text();
        String desc = ((Operand.Identifier)operands.get(2)).text();
        enter(instr.opcode(), new Object[] {
            ownerClassDesc(owner), name, ClassDesc.ofDescriptor(desc)
        }, cb);
    }

    /* for any instruction taking a branch target as operand */
    private static void enterBranchTarget(Instruction instr, CodeBuilder cb, Map<String, Label> labels) throws AssemblyException {
        var operands = instr.operands();
        testOperands(instr, Operand.Identifier.class);
        String labelName = ((Operand.Identifier)operands.getFirst()).text();

        // Create a label for this if necessary
        // (If it doesn't exist, it will eventually be bound when we find the instruction with that label)
        Label label = labels.computeIfAbsent(labelName, _ -> cb.newLabel());

        enter(instr.opcode(), new Object[] { label }, cb);
    }

    /* for any instruction taking a method descriptor as operand */
    private static void enterMethodDesc(Instruction instr, CodeBuilder cb) throws AssemblyException {
        var operands = instr.operands();
        testOperands(instr, Operand.Identifier.class, Operand.Identifier.class, Operand.Identifier.class);
        String owner = ((Operand.Identifier)operands.getFirst()).text();
        String name = ((Operand.Identifier)operands.get(1)).text();
        String desc = ((Operand.Identifier)operands.get(2)).text();
        enter(instr.opcode(), new Object[] {
            ownerClassDesc(owner), name, MethodTypeDesc.ofDescriptor(desc)
        }, cb);
    }

    /* for ldc and ldc2 */
    private static void enterLdc(Instruction instr, CodeBuilder cb) throws AssemblyException {
        // ldc: int, float, string, class reference (ID), method reference (ID ID ID), method handle (unsupported)
        // ldc2: long or double

        if (instr.opcode().equals("ldc") || instr.opcode().equals("ldc_w")) {
            if (instr.operands().size() == 1) {
                switch (instr.operands().getFirst()) {
                    case Operand.Int i -> cb.ldc((Integer)i.value());
                    case Operand.Float f -> cb.ldc((Float)f.value());
                    case Operand.String s -> cb.ldc(s.value());
                    case Operand.Identifier id -> cb.ldc(ClassDesc.of(id.text()));
                    default -> throw new AssemblyException("Invalid operand for opcode ldc");
                }
            } else if (instr.operands().size() == 3)
                throw new AssemblyException("Unsupported opcode: ldc [method]");
            else
                throw new AssemblyException("Opcode ldc takes either 1 or 3 operands, not " + instr.operands().size());
        } else if (instr.opcode().equals("ldc2") || instr.opcode().equals("ldc2_w")) {
            if (instr.operands().size() == 1) {
                switch (instr.operands().getFirst()) {
                    case Operand.Long l -> cb.ldc((Long)l.value());
                    case Operand.Double d -> cb.ldc((Double)d.value());
                    default -> throw new AssemblyException("Invalid operand for opcode ldc2");
                }
            } else
                throw new AssemblyException("Opcode ldc2 takes exactly 1 operand");
        }
    }

    private static Object[] reifyOperands(List<Operand> operands) {
        Object[] rops = new Object[operands.size()];
        for (int i = 0; i < operands.size(); ++i) {
            rops[i] = switch (operands.get(i)) {
                case Operand.Int o -> (Integer)o.value();
                case Operand.Long o -> (Long)o.value();
                case Operand.Double o -> (Double)o.value();
                case Operand.Float o -> (Float)o.value();
                case Operand.String o -> o.text();
                // what an identifier is varies on the instruction (branch target, descriptor, etc.)
                case Operand.Identifier o -> throw new RuntimeException("Internal error (please report): reifyOperands() given an identifier");
            };
        }
        return rops;
    }

    private static void enter(String opcode, Object[] operands, CodeBuilder cb) throws AssemblyException {
        Class<?>[] paramTypes = Arrays.stream(operands).map(Instructions::operandType).toArray(i -> new Class<?>[i]);
        // Some opcodes have renamed methods due to clashes with Java keywords
        String methodName = switch (opcode) {
            case "goto" -> "goto_";
            case "instanceof" -> "instanceOf";
            case "new" -> "new_";
            case "return" -> "return_";
            default -> opcode;
        };

        try {
            var method = CodeBuilder.class.getMethod(methodName, paramTypes);
            method.invoke(cb, operands);
        } catch (NoSuchMethodException ex) {
            boolean methodExists = false;
            for (var method : CodeBuilder.class.getMethods()) {
                if (method.getName().equals(methodName)) {
                    methodExists = true;
                    break;
                }
            }

            if (!methodExists)
                throw new AssemblyException("Invalid opcode: " + opcode);
            else
                throw new AssemblyException("Invalid operands for opcode " + opcode);
        } catch (InvocationTargetException ex) {
            throw new AssemblyException("Illegal operand value for opcode " + opcode, ex);
        } catch (ReflectiveOperationException ex) {
            throw new AssemblyException("Reflection error while generating bytecode", ex);
        }
    }

    private static Class<?> operandType(Object operand) {
        return switch (operand) {
            case Byte _ -> Byte.TYPE;
            case Short _ -> Short.TYPE;
            case Integer _ -> Integer.TYPE;
            case Long _ -> Long.TYPE;
            case Float _ -> Float.TYPE;
            case Double _ -> Double.TYPE;
            case Character _ -> Character.TYPE;
            case Boolean _ -> Boolean.TYPE;
            case String _ -> String.class;
            case Label _ -> Label.class;
            case ClassDesc _ -> ClassDesc.class;
            case MethodTypeDesc _ -> MethodTypeDesc.class;
            default -> throw new RuntimeException("Internal error (please report): Unexpected operand: " + operand);
        };
    }

    private static void testOperands(Instruction instr, Class<?>... expectedTypes) throws AssemblyException {
        if (instr.operands().size() != expectedTypes.length)
            throw new AssemblyException(String.format("Opcode %s takes exactly %s operand%s",
                instr.opcode(),
                expectedTypes.length == 1 ? "one" : "" + expectedTypes.length,
                expectedTypes.length == 1 ? "" : "s"
            ));
        for (int i = 0; i < expectedTypes.length; ++i)
            if (!(instr.operands().get(i).getClass().equals(expectedTypes[i])))
                throw new AssemblyException(String.format("Wrong types for operands for opcode %s", instr.opcode()));
    }

    private static ClassDesc ownerClassDesc(String owner) {
        return ClassDesc.of(owner.replaceAll("/", "."));
    }
}

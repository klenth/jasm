package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.JasmSyntaxException;
import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

public class Instructions {

    private static final Pattern LOAD_STORE_PATTERN = Pattern.compile(
        "^[adfil](load|store)(_[0123])?$"
    );

    private static EnumMap<Opcode, Method> codeBuilderMethods = new EnumMap<>(Opcode.class);

    private static final Map<String, Opcode> OPCODES;

    static {
        OPCODES = new HashMap<>();
        for (var opcode : Opcode.values()) {
            String name = opcode.getName();
            if (!name.equals("ldc") && !name.equals("ldc2")) {
                OPCODES.put(name, opcode);
            }
        }
    }

    public static void enter(StringView opcode, List<StringView> operands, Map<String, Label> labels, CodeBuilder cb) throws AssemblyException {
        String opcodeString = opcode.toString();
        if (opcodeString.equals("ldc")) {
            enterLdc(opcode, operands, cb);
            return;
        } else if (opcodeString.equals("ldc2")) {
            enterLdc2(opcode, operands, cb);
            return;
        }

        Opcode opc = OPCODES.getOrDefault(opcodeString, null);
        if (opc == null)
            throw new AssemblyException(
                "Invalid opcode", opcode
            );

        var ops = checkAndParseOperands(opcode, opc, operands);

        switch (opc) {
            // opcodes without arguments whose enum names match method names in CodeBuilder
            case aaload, aastore, areturn, arraylength, athrow,
                 baload, bastore,
                 caload, castore, checkcast,
                 d2f, d2i, d2l, dadd, daload, dastore, dcmpg, dcmpl,
                 dconst_0, dconst_1, ddiv, dmul, dneg, drem, dreturn, dsub,
                 dup, dup_x1, dup_x2, dup2, dup2_x1, dup2_x2,
                 f2d, f2i, f2l, fadd, faload, fastore, fcmpg, fcmpl,
                 fconst_0, fconst_1, fconst_2, fdiv, fmul, fneg, frem, freturn, fsub,
                 i2b, i2c, i2d, i2f, i2l, i2s, iadd, iaload, iand, iastore,
                 iconst_0, iconst_1, iconst_2, iconst_3, iconst_4, iconst_5, iconst_m1,
                 idiv, imul, ineg, ior, irem, ireturn, ishl,
                 ishr, isub, iushr, ixor,
                 l2d, l2f, l2i, ladd, laload, land, lastore, lcmp, lconst_0, lconst_1,
                 ldiv, lmul, lneg, lor, lrem, lreturn, lshl, lshr, lsub, lushr, lxor,
                 monitorenter, monitorexit,
                 nop,
                 pop, pop2,
                 return_,
                 saload, sastore, swap -> {
                try {
                    Method method;
                    if (codeBuilderMethods.containsKey(opc))
                        method = codeBuilderMethods.get(opc);
                    else {
                        method = CodeBuilder.class.getMethod(opc.name());
                        codeBuilderMethods.put(opc, method);
                    }
                    method.invoke(cb);
                } catch (ReflectiveOperationException ex) {
                    throw new AssemblyException("Internal assembler error (please report)", ex);
                }
            }

            // loads and stores
            case aload -> cb.aload(_int(ops[0]));
            case aload_0 -> cb.aload(0);
            case aload_1 -> cb.aload(1);
            case aload_2 -> cb.aload(2);
            case aload_3 -> cb.aload(3);
            case astore -> cb.astore(_int(ops[0]));
            case astore_0 -> cb.astore(0);
            case astore_1 -> cb.astore(1);
            case astore_2 -> cb.astore(2);
            case astore_3 -> cb.astore(3);

            case dload -> cb.dload(_int(ops[0]));
            case dload_0 -> cb.dload(0);
            case dload_1 -> cb.dload(1);
            case dload_2 -> cb.dload(2);
            case dload_3 -> cb.dload(3);
            case dstore -> cb.dstore(_int(ops[0]));
            case dstore_0 -> cb.dstore(0);
            case dstore_1 -> cb.dstore(1);
            case dstore_2 -> cb.dstore(2);
            case dstore_3 -> cb.dstore(3);

            case fload -> cb.fload(_int(ops[0]));
            case fload_0 -> cb.fload(0);
            case fload_1 -> cb.fload(1);
            case fload_2 -> cb.fload(2);
            case fload_3 -> cb.fload(3);
            case fstore -> cb.fstore(_int(ops[0]));
            case fstore_0 -> cb.fstore(0);
            case fstore_1 -> cb.fstore(1);
            case fstore_2 -> cb.fstore(2);
            case fstore_3 -> cb.fstore(3);

            case iload -> cb.iload(_int(ops[0]));
            case iload_0 -> cb.iload(0);
            case iload_1 -> cb.iload(1);
            case iload_2 -> cb.iload(2);
            case iload_3 -> cb.iload(3);
            case istore -> cb.istore(_int(ops[0]));
            case istore_0 -> cb.istore(0);
            case istore_1 -> cb.istore(1);
            case istore_2 -> cb.istore(2);
            case istore_3 -> cb.istore(3);

            case lload -> cb.lload(_int(ops[0]));
            case lload_0 -> cb.lload(0);
            case lload_1 -> cb.lload(1);
            case lload_2 -> cb.lload(2);
            case lload_3 -> cb.lload(3);
            case lstore -> cb.lstore(_int(ops[0]));
            case lstore_0 -> cb.lstore(0);
            case lstore_1 -> cb.lstore(1);
            case lstore_2 -> cb.lstore(2);
            case lstore_3 -> cb.lstore(3);


            // fields and methods
            case getfield -> cb.getfield(
                _classDesc(ops[0]),
                _id(ops[1]),
                _typeDesc(ops[2])
            );
            case getstatic -> cb.getstatic(
                _classDesc(ops[0]),
                _id(ops[1]),
                _typeDesc(ops[2])
            );
            case putfield -> cb.putfield(
                _classDesc(ops[0]),
                _id(ops[1]),
                _typeDesc(ops[2])
            );
            case putstatic -> cb.putstatic(
                _classDesc(ops[0]),
                _id(ops[1]),
                _typeDesc(ops[2])
            );
            case invokeinterface -> cb.invokeinterface(
                _classDesc(ops[0]),
                _id(ops[1]),
                _methodDesc(ops[2])
            );
            case invokespecial -> cb.invokespecial(
                _classDesc(ops[0]),
                _id(ops[1]),
                _methodDesc(ops[2])
            );
            case invokestatic -> cb.invokestatic(
                _classDesc(ops[0]),
                _id(ops[1]),
                _methodDesc(ops[2])
            );
            case invokevirtual -> cb.invokevirtual(
                _classDesc(ops[0]),
                _id(ops[1]),
                _methodDesc(ops[2])
            );

            // branches
            case goto_,
                 if_acmpeq, if_acmpne,
                 if_icmpeq, if_icmpne, if_icmplt, if_icmpge, if_icmpgt, if_icmple,
                 ifeq, ifne, iflt, ifge, ifgt, ifle,
                 ifnonnull, ifnull -> {
                var target = labels.computeIfAbsent(
                    ((Operand.BranchTarget)ops[0]).text(),
                    l -> cb.newLabel()
                );

                try {
                    Method method;

                    if (codeBuilderMethods.containsKey(opc))
                        method = codeBuilderMethods.get(opc);
                    else {
                        method = CodeBuilder.class.getMethod(opc.name(), Label.class);
                        codeBuilderMethods.put(opc, method);
                    }

                    method.invoke(cb, target);
                } catch (ReflectiveOperationException ex) {
                    throw new AssemblyException("Internal error (please report)", ex);
                }
            }

            // miscellaneous
            case anewarray -> {
                var desc = _typeDesc(ops[0]);
                if (desc.isPrimitive())
                    throw new AssemblyException("Invalid operand", operands.getFirst());
                cb.anewarray(desc);
            }

            case bipush -> cb.bipush(_int(ops[0]));
            case iinc -> cb.iinc(_int(ops[0]), _int(ops[1]));
            case instanceOf -> cb.instanceOf(_classDesc(ops[0]));

            case multianewarray -> {
                var desc = _typeDesc(ops[0]);
                if (!desc.isArray())
                    throw new AssemblyException("Invalid operand", operands.getFirst());
                cb.multianewarray(desc, _int(ops[1]));
            }

            case new_ -> cb.new_(_classDesc(ops[0]));
            case newarray -> cb.newarray(_typeKind(ops[0]));
            case sipush -> cb.sipush(_int(ops[0]));

            default ->
                throw new AssemblyException("Internal error (please report): unhandled opcode " + opc);
        }
   }

   public static void enterTableInstruction(StringView opcode, List<StringView> operands, Table table, Map<String, Label> labels, CodeBuilder cb) throws AssemblyException {
       Opcode opc = switch (opcode.toString()) {
           case "lookupswitch" -> Opcode.lookupswitch;
           case "tableswitch" -> Opcode.tableswitch;
           default -> throw new AssemblyException(String.format(
               "Internal error (please report): %s handled as a table instruction", opcode.toString()
           ), opcode);
       };

       var ops = checkAndParseOperands(opcode, opc, operands);
       switch (opc) {
           case lookupswitch -> {
               var defaultTarget = labels.computeIfAbsent(
                   ((Operand.BranchTarget)ops[0]).text(),
                   l -> cb.newLabel()
               );
               cb.lookupswitch(defaultTarget, parseTable(table, labels, cb));
           }

           case tableswitch -> {
               var defaultTarget = labels.computeIfAbsent(
                   ((Operand.BranchTarget)ops[2]).text(),
                   l -> cb.newLabel()
               );
               cb.tableswitch(
                   _int(ops[0]), _int(ops[1]), defaultTarget,
                   parseTable(table, labels, cb)
               );
           }
       }
   }

   private static Operand[] checkAndParseOperands(StringView opcode, Opcode opc, List<StringView> operands) throws AssemblyException {
       var opcOperands = opc.getOperandTypes();
       if (opcOperands.length == 0 && !operands.isEmpty())
           throw new AssemblyException(
               "Opcode " + opc + " takes no operands",
               operands.getFirst()
           );
       else if (operands.size() != opcOperands.length)
           throw new AssemblyException(String.format(
               "Opcode " + opc + " takes %s",
               opcOperands.length == 1
                   ? "one operand" : opcOperands.length == 2
                   ? "two operands" : opcOperands.length == 3
                   ? "three operands" : opcOperands.length + " operands"
           ), opcode);

       return parseOperands(operands, opcOperands);
   }

    private static int _int(Operand op) {
        return ((Operand.Int)op).value();
    }

    private static ClassDesc _classDesc(Operand op) {
        return ClassDesc.ofInternalName(((Operand.ClassName)op).value());
    }

    private static String _id(Operand op) {
        return ((Operand.Identifier)op).value();
    }

    private static ClassDesc _typeDesc(Operand op) {
        return ClassDesc.ofDescriptor(((Operand.Descriptor)op).text());
    }

    private static MethodTypeDesc _methodDesc(Operand op) {
        return MethodTypeDesc.ofDescriptor(((Operand.MethodDescriptor)op).text());
    }

    private static TypeKind _typeKind(Operand op) {
        return switch ((Operand.ArrayType)op) {
            case Byte -> TypeKind.BYTE;
            case Short -> TypeKind.SHORT;
            case Int -> TypeKind.INT;
            case Long -> TypeKind.LONG;
            case Float -> TypeKind.FLOAT;
            case Double -> TypeKind.DOUBLE;
            case Char -> TypeKind.CHAR;
            case Boolean -> TypeKind.BOOLEAN;
        };
    }

    private static void enterLdc(StringView opcode, List<StringView> operands, CodeBuilder cb) throws AssemblyException {
        // Possible operands:
        //   int value
        //   float value
        //   String value
        //   class/method type/method handle reference (unimplemented)
        if (operands.size() != 1)
            throw new AssemblyException("Opcode ldc takes one operand", opcode);
        StringView op = operands.getFirst();
        String ops = op.toString();
        if (ops.toLowerCase().endsWith("f") || ops.toLowerCase().endsWith("fb")) {
            var fop = Operands.parseFloat(op);
            cb.ldc(fop.value());
        } else if (ops.startsWith("\"")) {
            var sop = Operands.parseString(op);
            cb.ldc(sop.value());
        } else {
            try {
                var iop = Operands.parseInt(op);
                cb.ldc(iop.value());
            } catch (AssemblyException _) {
                throw new AssemblyException("Invalid operand", op);
            }
        }
    }

    private static void enterLdc2(StringView opcode, List<StringView> operands, CodeBuilder cb) throws AssemblyException {
        // Possible operands:
        //   long value
        //   double value
        if (operands.size() != 1)
            throw new AssemblyException("Opcode ldc2 takes one operand", opcode);
        StringView op = operands.getFirst();
        String ops = op.toString();
        if (ops.toLowerCase().endsWith("db") || ops.contains(".") || ops.toLowerCase().contains("e")) {
            var dop = Operands.parseDouble(op);
            cb.ldc(dop.value());
        } else {
            try {
                var lop = Operands.parseLong(op);
                cb.ldc(lop.value());
            } catch (AssemblyException _) {
                throw new AssemblyException("Invalid operand", op);
            }
        }
    }

    private static Operand[] parseOperands(List<StringView> operands, OperandType[] opTypes) throws AssemblyException {
        Operand[] ops = new Operand[opTypes.length];

        for (int i = 0; i < ops.length; ++i)
            ops[i] = Operands.parseOperand(operands.get(i), opTypes[i]);

        return ops;
    }

    private static List<SwitchCase> parseTable(Table table, Map<String, Label> labels, CodeBuilder cb) throws AssemblyException {
        var cases = new ArrayList<SwitchCase>(table.entries().size());
        for (var entry : table.entries()) {
            try {
                int value = Integer.parseInt(entry.label().toString());
                var target = labels.computeIfAbsent(
                    entry.target().toString(),
                    l -> cb.newLabel()
                );
                cases.add(SwitchCase.of(value, target));
            } catch (NumberFormatException _) {
                throw new AssemblyException(
                    "Invalid integer", entry.label()
                );
            }
        }

        return cases;
    }
}

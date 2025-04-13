package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.JvmAssemblyLexer;
import edu.westminsteru.cmpt355.jasm.parser.JvmAssemblyParser;
import edu.westminsteru.cmpt355.jasm.parser.JvmAssemblyParserBaseListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JvmAssemblyParserBaseListener {

    /*
    private static final String CODE = """
            .source test-input/test.l2
            .class public test
            .super java/lang/Object
            
            .field private static $in Ljava/util/Scanner;
            .field private static valid Z
            .field private static x D
            
            .method private static fizzbuzz(I)V
            .code
            .limit locals 2
            .limit stack 100
            iconst_0
            istore_1
            while0:
            iload_1
            iload_0
            if_icmpge endwhile0
            iload_1
            bipush 6
            irem
            iconst_0
            if_icmpne else3
            getstatic java/lang/System/out Ljava/io/PrintStream;
            ldc "FizzBuzz "
            invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            goto endif3
            else3:
            iload_1
            iconst_2
            irem
            iconst_0
            if_icmpne else2
            getstatic java/lang/System/out Ljava/io/PrintStream;
            ldc "Fizz "
            invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            goto endif2
            else2:
            iload_1
            iconst_3
            irem
            iconst_0
            if_icmpne else1
            getstatic java/lang/System/out Ljava/io/PrintStream;
            ldc "Buzz "
            invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            goto endif1
            else1:
            getstatic java/lang/System/out Ljava/io/PrintStream;
            iload_1
            invokevirtual java/io/PrintStream/print(I)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            endif1:
            endif2:
            endif3:
            iload_1
            iconst_1
            iadd
            istore_1
            goto while0
            endwhile0:
            return
            .end code
            
            .method private static fibonacci(I)V
            .code
            .limit locals 4
            .limit stack 100
            iconst_0
            istore_1
            iconst_1
            istore_2
            while1:
            iload_1
            iload_0
            if_icmpge endwhile1
            getstatic java/lang/System/out Ljava/io/PrintStream;
            iload_1
            invokevirtual java/io/PrintStream/print(I)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            iload_1
            iload_2
            iadd
            istore_3
            iload_2
            istore_1
            iload_3
            istore_2
            goto while1
            endwhile1:
            return
            .end code
            
            .method public static main([Ljava/lang/String;)V
            .code
            .limit locals 1
            .limit stack 100
            new java/util/Scanner
            dup
            getstatic java/lang/System/in Ljava/io/InputStream;
            invokespecial java/util/Scanner/<init>(Ljava/io/InputStream;)V
            putstatic test/$in Ljava/util/Scanner;
            
            getstatic java/lang/System/out Ljava/io/PrintStream;
            ldc "Enter a number: "
            invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
            getstatic test/$in Ljava/util/Scanner;
            invokevirtual java/util/Scanner/nextDouble()D
            putstatic test/x D
            ; Compute valid
            getstatic test/x D
            iconst_0
            i2d
            dcmpg
            ifge comparison-true0
            iconst_0
            goto endcomparison0
            comparison-true0:
            iconst_1
            endcomparison0:
            dup
            ifeq and-shortcircuit0
            pop
            getstatic test/x D
            bipush 100
            i2d
            dcmpg
            ifle comparison-true1
            iconst_0
            goto endcomparison1
            comparison-true1:
            iconst_1
            endcomparison1:
            and-shortcircuit0:
            putstatic test/valid Z
            ; If-else
            getstatic test/valid Z
            ifeq else0
            getstatic java/lang/System/out Ljava/io/PrintStream;
            ldc "This is a valid test score."
            invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            goto endif0
            else0:
            getstatic java/lang/System/out Ljava/io/PrintStream;
            ldc "This is an invalid test score."
            invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
            getstatic java/lang/System/out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream/println()V
            endif0:
            bipush 24
            invokestatic test/fizzbuzz(I)V
            bipush 100
            invokestatic test/fibonacci(I)V
            return
            .end code
        """;
     */

    private static final String CODE = """
        .class test
        .source test.l2
        .super java/lang/Object
        
        .method public static main([Ljava/lang/String;)V
        .code
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "Hello, world!"
            invokevirtual java/io/PrintStream println (Ljava/lang/String;)V
            return
        .end code
        """;

    public static void main(String... args) throws Exception {
        var lexer = new JvmAssemblyLexer(CharStreams.fromString(CODE));
        var parser = new JvmAssemblyParser(new CommonTokenStream(lexer));
        var listener = new Main();
        parser.addParseListener(listener);

        parser.assemblyFile();

        byte[] data = ClassFile.of(
            ClassFile.ShortJumpsOption.FIX_SHORT_JUMPS
        ).build(ClassDesc.of(listener.className), listener::buildClass);

        Files.write(Path.of("test.class"), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private ClassFile classFile;
    private ClassBuilder cb;

    private String sourceName;
    private String classId;
    private String className, superclassName = "java/lang/Object";
    private List<String> classFlags;
    private List<String> superinterfaceNames = new ArrayList<>();
    private List<FieldDefinition> fields = new ArrayList<>();
    private List<MethodDefinition> methods = new ArrayList<>();
    private List<Instruction> currentMethodCodeInstructions = null;
    private Map<MethodDefinition, MethodCode> methodCodes = new HashMap<>();
    private List<Operand> currentInstructionOperands = null;

    private void buildClass(ClassBuilder cb) {
        int flags = Flags.flags(classFlags);
        switch (classId) {
            case "class" -> {}
            case "interface" -> flags |= ClassFile.ACC_INTERFACE;
            case "enum" -> flags |= ClassFile.ACC_ENUM;
            case "record" -> throw new RuntimeException("records not supported");
        }

        if (sourceName != null)
            cb.with(SourceFileAttribute.of(sourceName));
        cb.withSuperclass(ClassDesc.of(superclassName.replaceAll("/", ".")));
        cb.withFlags(flags);
        cb.withInterfaceSymbols(superinterfaceNames.stream().map(ClassDesc::of).toList());


        for (var field : fields) {
            cb.withField(
                field.fieldName(),
                ClassDesc.ofDescriptor(field.fieldDescriptor()),
                Flags.flags(field.flags())
            );
        }

        for (var method : methods) {
            cb.withMethod(
                method.methodName(),
                MethodTypeDesc.ofDescriptor(method.descriptor()),
                Flags.flags(method.flags()),
                mb -> buildMethod(method, mb)
            );
        }
    }

    private void buildMethod(MethodDefinition definition, MethodBuilder mb) {
        MethodCode code = methodCodes.getOrDefault(definition, null);
        if (code != null)
            mb.withCode(cb -> buildMethodCode(code, cb));
    }

    private void buildMethodCode(MethodCode code, CodeBuilder cb) {
        Map<String, Label> labels = new HashMap<>();
        for (var instr : code.instructions()) {
            try {
                Instructions.enter(instr, cb, labels);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void exitSourceDirective(JvmAssemblyParser.SourceDirectiveContext ctx) {
        sourceName = ctx.FILENAME().getText();
    }

    @Override
    public void exitClassDirective(JvmAssemblyParser.ClassDirectiveContext ctx) {
        if (className != null)
            throw new RuntimeException("Multiple .class directives");
        classId = ctx.CLASS_ID().getText();
        className = ctx.CLASS_ID().getText();
        classFlags = ctx.CLASS_FLAG().stream().map(TerminalNode::getText).toList();
    }

    @Override
    public void exitSuperDirective(JvmAssemblyParser.SuperDirectiveContext ctx) {
        superclassName = ctx.CLASSNAME().getText();
    }

    @Override
    public void exitImplementsDirective(JvmAssemblyParser.ImplementsDirectiveContext ctx) {
        superinterfaceNames.add(ctx.CLASSNAME().getText());
    }

    @Override
    public void exitFieldDirective(JvmAssemblyParser.FieldDirectiveContext ctx) {
        List<String> flags = ctx.FIELD_FLAG().stream().map(TerminalNode::getText).toList();
        String name = ctx.FIELD_ID().getText();
        String descriptor = ctx.FIELD_DESC().getText();

        fields.add(new FieldDefinition(flags, name, descriptor));
    }

    @Override
    public void enterMethodDirective(JvmAssemblyParser.MethodDirectiveContext ctx) {
        currentMethodCodeInstructions = null;
    }

    @Override
    public void exitMethodDirective(JvmAssemblyParser.MethodDirectiveContext ctx) {
        List<String> flags = ctx.METHOD_FLAG().stream().map(TerminalNode::getText).toList();
        String name = ctx.METHOD_ID().getText();
        String descriptor = ctx.METHOD_DESC().getText();;

        var method = new MethodDefinition(flags, name, descriptor);
        methods.add(method);
        if (currentMethodCodeInstructions != null)
            methodCodes.put(method, new MethodCode(currentMethodCodeInstructions));
    }

    @Override
    public void enterMethodCode(JvmAssemblyParser.MethodCodeContext ctx) {
        currentMethodCodeInstructions = new ArrayList<>();
    }

    @Override
    public void enterInstruction(JvmAssemblyParser.InstructionContext ctx) {
        currentInstructionOperands = new ArrayList<>();
    }

    @Override
    public void exitOperand(JvmAssemblyParser.OperandContext ctx) {
        Operand op = null;
        if (ctx.INT() != null)
            op = new Operand.Int(ctx.INT().getText());
        else if (ctx.LONG() != null)
            op = new Operand.Long(ctx.LONG().getText());
        else if (ctx.DOUBLE() != null)
            op = new Operand.Double(ctx.DOUBLE().getText());
        else if (ctx.FLOAT() != null)
            op = new Operand.Float(ctx.FLOAT().getText());
        else if (ctx.STRING_STRING() != null)
            op = new Operand.String(ctx.STRING_STRING().getText());
        else if (ctx.CODE_WORD() != null)
            op = new Operand.Identifier(ctx.CODE_WORD().getText());
        else
            throw new RuntimeException("Unknown operand: " + ctx);

        currentInstructionOperands.add(op);
    }

    @Override
    public void exitLabelInstruction(JvmAssemblyParser.LabelInstructionContext ctx) {
        List<String> labels = ctx.CODE_WORD().stream().map(TerminalNode::getText).toList();
        String opcode = ctx.instruction().CODE_WORD().getText();
        currentMethodCodeInstructions.add(new Instruction(labels, opcode, currentInstructionOperands));
    }
}

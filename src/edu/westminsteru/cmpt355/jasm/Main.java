package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.*;

import java.io.StringReader;
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

public class Main implements JasmParserListener {

    private static final String CODE = """
            .source test-input/test.l2
            .class public test
            .super java/lang/Object
            
            .field private static $in Ljava/util/Scanner;
            .field private static valid Z
            .field private static x D
            
            .method private static fizzbuzz (I)V
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
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "FizzBuzz "
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
            goto endif3
            else3:
            iload_1
            iconst_2
            irem
            iconst_0
            if_icmpne else2
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "Fizz "
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
            goto endif2
            else2:
            iload_1
            iconst_3
            irem
            iconst_0
            if_icmpne else1
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "Buzz "
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
            goto endif1
            else1:
            getstatic java/lang/System out Ljava/io/PrintStream;
            iload_1
            invokevirtual java/io/PrintStream print (I)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
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
            
            .method private static fibonacci (I)V
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
            getstatic java/lang/System out Ljava/io/PrintStream;
            iload_1
            invokevirtual java/io/PrintStream print (I)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
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
            
            .method public static main ([Ljava/lang/String;)V
            .code
            .limit locals 1
            .limit stack 100
            new java/util/Scanner
            dup
            getstatic java/lang/System in Ljava/io/InputStream;
            invokespecial java/util/Scanner <init> (Ljava/io/InputStream;)V
            putstatic test/$in Ljava/util/Scanner;
            
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "Enter a number: "
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic test/$in Ljava/util/Scanner;
            invokevirtual java/util/Scanner nextDouble ()D
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
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "This is a valid test score."
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
            goto endif0
            else0:
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "This is an invalid test score."
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic java/lang/System out Ljava/io/PrintStream;
            invokevirtual java/io/PrintStream println ()V
            endif0:
            bipush 24
            invokestatic test fizzbuzz (I)V
            bipush 100
            invokestatic test fibonacci (I)V
            return
            .end code
        """;

    /*
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
    */

    public static void main(String... args) throws Exception {
        /*var lexer = new JvmAssemblyLexer(CharStreams.fromString(CODE));
        var parser = new JvmAssemblyParser(new CommonTokenStream(lexer));
        var listener = new Main();
        parser.addParseListener(listener);

        parser.assemblyFile();*/

        var listener = new Main();
        var parser = new JasmParser(new StringReader(CODE), listener);
        parser.parse();

        if (!listener.parseErrors) {

            byte[] data = ClassFile.of(
                ClassFile.ShortJumpsOption.FIX_SHORT_JUMPS
            ).build(ClassDesc.of(listener.className), listener::buildClass);

            Files.write(Path.of("test.class"), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
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
    private List<String> instructionLabels = new ArrayList<>();
    private List<Instruction> currentMethodCodeInstructions = null;
    private Map<MethodDefinition, MethodCode> methodCodes = new HashMap<>();
    private List<Operand> currentInstructionOperands = null;
    private boolean parseErrors = false;

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
    public void exceptionOccurred(JasmParser parser, JasmSyntaxException ex) {
        ex.print(System.err);
        parseErrors = true;
    }

    @Override
    public void sourceDirective(JasmParser parser, String source) {
        this.sourceName = source;
    }

    @Override
    public void classDirective(JasmParser parser, String classId, List<String> flags, String className) {
        this.classId = classId;
        this.classFlags = flags;
        this.className = className;
    }

    @Override
    public void superDirective(JasmParser parser, String superName) {
        this.superclassName = superName;
    }

    @Override
    public void implementsDirective(JasmParser parser, String interfaceName) {
        this.superinterfaceNames.add(interfaceName);
    }

    @Override
    public void fieldDirective(JasmParser parser, List<String> flags, String name, String descriptor) {
        this.fields.add(new FieldDefinition(flags, name, descriptor));
    }

    @Override
    public void methodDirective(JasmParser parser, List<String> flags, String name, String descriptor) {
        this.methods.add(new MethodDefinition(flags, name, descriptor));
    }

    @Override
    public void codeDirective(JasmParser parser) {
        this.currentMethodCodeInstructions = new ArrayList<>();
    }

    @Override
    public void codeLabel(JasmParser parser, String labelName) {
        this.instructionLabels.add(labelName);
    }

    @Override
    public void codeInstruction(JasmParser parser, String opcode, List<Operand> operands) {
        this.currentMethodCodeInstructions.add(
            new Instruction(instructionLabels, opcode, operands)
        );
        instructionLabels.clear();
    }

    @Override
    public void endCodeDirective(JasmParser parser) {
        methodCodes.put(methods.getLast(), new MethodCode(currentMethodCodeInstructions));
    }
}

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
import java.util.*;

public class Main implements JasmParserListener {

    @FunctionalInterface
    private interface Failable {
        void run() throws Exception;
    }

    private static final String CODE = """
            .source test-input/test.l2
            .class public test
            .super java/lang/Object
            
            .method public static main ([Ljava/lang/String;)V
            .code
            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "Hello, world!"
            invokevirtual java/io/PrintStream println (Ljava/lang/String;)V
            return
            #.end code
            
            .method private abstract static foo ()V
        """;

    /*
    private static final String CODE = """
            .source test-input/test.l2
            .class public test
            .super java/lang/Object

            .field private static $in Ljava/util/Scanner;
            .field private static valid Z
            .field private static x D

            .method private static fizzbuzz (I)V
            .code
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
            new java/util/Scanner
            dup
            getstatic java/lang/System in Ljava/io/InputStream;
            invokespecial java/util/Scanner <init> (Ljava/io/InputStream;)V
            putstatic test $in Ljava/util/Scanner;

            getstatic java/lang/System out Ljava/io/PrintStream;
            ldc "Enter a number: "
            invokevirtual java/io/PrintStream print (Ljava/lang/String;)V
            getstatic test $in Ljava/util/Scanner;
            invokevirtual java/util/Scanner nextDouble ()D
            putstatic test x D
            # Compute valid
            getstatic test x D
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
            getstatic test x D
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
            putstatic test valid Z
            # If-else
            getstatic test valid Z
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
        */
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
        var listener = new Main();
        var parser = new JasmParser(new StringReader(CODE), listener);
        parser.parse();

        List<ErrorMessage> errors = new ArrayList<>();

        if (listener.errors.isEmpty()) {
            try {
                byte[] data = ClassFile.of(
                    ClassFile.ShortJumpsOption.FIX_SHORT_JUMPS
                ).build(ClassDesc.ofInternalName(listener.className), listener::buildClass);

                Files.write(Path.of("test.class"), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (AbortClassfileGenerationException ex) {
                // do nothing (this exception is just to escape the Classfile callbacks)
            } catch (Exception ex) {
                errors.add(new ErrorMessage(ex.getMessage()));
            }
        }

        errors.addAll(listener.errors);

        errors.stream()
            .sorted(Comparator.comparing(ErrorMessage::lineNumber).thenComparing(ErrorMessage::columnNumber))
            .forEach(em -> {
                em.print(System.err);
                System.err.println();
            });
    }

    private ClassFile classFile;
    private ClassBuilder cb;

    private String sourceName;
    private String classId;
    private String className, superclassName;
    private List<String> classFlags;
    private List<String> superinterfaceNames = new ArrayList<>();
    private List<FieldDefinition> fields = new ArrayList<>();
    private List<MethodDefinition> methods = new ArrayList<>();
    private List<String> instructionLabels = new ArrayList<>();
    private List<Instruction> currentMethodCodeInstructions = null;
    private Map<MethodDefinition, MethodCode> methodCodes = new HashMap<>();
    private List<ErrorMessage> errors = new ArrayList<>();

    private void buildClass(ClassBuilder cb) {
        int flags = Flags.flags(classFlags);
        switch (classId) {
            case "class" -> {}
            case "interface" -> flags |= ClassFile.ACC_INTERFACE;
            case "enum" -> flags |= ClassFile.ACC_ENUM;
        }

        if (sourceName != null)
            catchError(
                () -> cb.with(SourceFileAttribute.of(sourceName)),
                "Invalid .source: "
            );

        final String superclassName = (this.superclassName == null) ? "java/lang/Object" : this.superclassName;
        catchError(
            () -> cb.withSuperclass(ClassDesc.ofInternalName(superclassName)),
            "Invalid .super: "
        );

        final int finalFlags = flags;
        catchError(
            () -> cb.withFlags(finalFlags),
            "Invalid flags: "
        );

        catchError(
            () -> cb.withInterfaceSymbols(superinterfaceNames.stream().map(ClassDesc::ofInternalName).toList()),
            "Invalid .interface: "
        );


        for (var field : fields) {
            catchError(
                () -> cb.withField(
                    field.fieldName(),
                    ClassDesc.ofDescriptor(field.fieldDescriptor()),
                    Flags.flags(field.flags())
                ),
                "Invalid .field: "
            );
        }

        for (var method : methods) {
            catchError(
                () -> cb.withMethod(
                    method.methodName(),
                    MethodTypeDesc.ofDescriptor(method.descriptor()),
                    Flags.flags(method.flags()),
                    mb -> buildMethod(method, mb)
                ),
                "Invalid .method: "
            );
        }

        if (!errors.isEmpty())
            throw new AbortClassfileGenerationException();
    }

    private void catchError(Failable f, String messagePrefix) {
        try {
            f.run();
        } catch (Exception ex) {
            errors.add(new ErrorMessage(messagePrefix + ex.getMessage()));
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
            } catch (AssemblyException ex) {
                String message = (ex.getCause() != null && ex.getCause().getMessage() != null)
                    ? ex.getMessage() + "\n" + ex.getCause().getMessage()
                    : ex.getMessage();
                errors.add(new ErrorMessage(
                    message, instr.text(),
                    instr.line(), ErrorMessage.UNSPECIFIC
                ));
            }
        }
    }

    @Override
    public void exceptionOccurred(JasmParser parser, JasmSyntaxException ex) {
        errors.add(new ErrorMessage(
            ex.getMessage(), ex.getLine(),
            ex.getLineNumber(), ex.getColumnNumber() + 1
        ));
    }

    @Override
    public void sourceDirective(JasmParser parser, String source) {
        if (this.sourceName != null)
            errors.add(new ErrorMessage(
                "Duplicate .source directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else
            this.sourceName = source;
    }

    @Override
    public void classDirective(JasmParser parser, String classId, List<String> flags, String className) {
        if (this.className != null)
            errors.add(new ErrorMessage(
                "Duplicate .class/.interface/.enum directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else {
            this.classId = classId;
            this.classFlags = flags;
            this.className = className;
        }
    }

    @Override
    public void superDirective(JasmParser parser, String superName) {
        if (this.superclassName != null)
            errors.add(new ErrorMessage(
                "Duplicate .super directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else
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
        if (this.currentMethodCodeInstructions != null)
            errors.add(new ErrorMessage(
                "Duplicate .code directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else
            this.currentMethodCodeInstructions = new ArrayList<>();
    }

    @Override
    public void codeLabel(JasmParser parser, String labelName) {
        if (this.instructionLabels.contains(labelName)) {
            errors.add(new ErrorMessage(
                "Duplicate label definition", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        } else
            this.instructionLabels.add(labelName);
    }

    @Override
    public void codeInstruction(JasmParser parser, String opcode, List<Operand> operands) {
        this.currentMethodCodeInstructions.add(
            new Instruction(new ArrayList<>(instructionLabels), opcode, operands, parser.getCurrentLine(), parser.getCurrentLineNumber())
        );
        instructionLabels.clear();
    }

    @Override
    public void endCodeDirective(JasmParser parser) {
        if (currentMethodCodeInstructions == null)
            errors.add(new ErrorMessage(
                ".end code directive not expected here", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else {
            methodCodes.put(methods.getLast(), new MethodCode(currentMethodCodeInstructions));
            currentMethodCodeInstructions = null;
        }
    }
}

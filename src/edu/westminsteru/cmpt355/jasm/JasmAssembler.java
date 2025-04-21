package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.*;

import java.io.*;
import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class JasmAssembler implements JasmParserListener {

    @FunctionalInterface
    private interface Failable {
        void run() throws Exception;
    }

    public static void main(String... args) throws Exception {
        var listener = new JasmAssembler();
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

    private BufferedReader in;
    private JasmParser parser;
    private DefaultJasmParserListener listener;
    private ClassFile classFile;
    private ClassBuilder cb;
    private List<ErrorMessage> errorMessages = new ArrayList<>();
    private List<Bytecode> assembledBytecodes = new ArrayList<>();

    private JasmAssembler(BufferedReader in) {
        this.in = in;
    }

    public Status assemble() {
        // first phase: parsing

        listener = new DefaultJasmParserListener();
        parser = new JasmParser(in, listener);
        parser.parse();
        errorMessages.addAll(listener.errors);
        errorMessages.sort(
            Comparator.comparing(ErrorMessage::lineNumber).thenComparing(ErrorMessage::columnNumber)
        );

        if (!errorMessages.isEmpty())
            return Status.Failure;

        // second phase: assembling

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

}

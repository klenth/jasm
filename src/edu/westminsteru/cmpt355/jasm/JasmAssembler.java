package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.*;

import java.io.*;
import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.*;

public class JasmAssembler implements JasmParserListener {

    public static final String JASM_VERSION = "0.2";

    @FunctionalInterface
    private interface Failable {
        void run() throws Exception;
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

    public static JasmAssembler reading(Reader in) {
        BufferedReader buffered = (in instanceof BufferedReader br) ? br : new BufferedReader(in);
        return new JasmAssembler(buffered);
    }

    public static JasmAssembler reading(String input) {
        return new JasmAssembler(
            new BufferedReader(
                new StringReader(input)
            )
        );
    }

    public Status assemble() {
        // first phase: parsing

        listener = new DefaultJasmParserListener();
        parser = new JasmParser(in, listener);
        parser.parse();
        errorMessages.addAll(listener.getErrors());
        errorMessages.sort(
            Comparator.comparing(ErrorMessage::lineNumber).thenComparing(ErrorMessage::columnNumber)
        );

        if (!errorMessages.isEmpty())
            return Status.Failure;

        // second phase: assembling
        boolean success = true;
        List<Bytecode> bytecodes = new ArrayList<>();

        for (var classSpec : listener.getClassSpecs()) {
            if (classSpec.className() == null || classSpec.className().isBlank()) {
                errorMessages.add(new ErrorMessage("Missing .class/.interface/.enum directive"));
                success = false;
                break;
            }

            try {
                byte[] data = ClassFile.of(
                    ClassFile.ShortJumpsOption.FIX_SHORT_JUMPS
                ).build(
                    ClassDesc.ofInternalName(classSpec.className().toString()),
                    classBuilder -> this.buildClass(listener, classSpec, classBuilder)
                );
                bytecodes.add(new Bytecode(classSpec.className().toString(), data));
            } catch (AbortClassfileGenerationException _) {
                success = false;
            } catch (Exception ex) {
                errorMessages.add(new ErrorMessage(ex.getMessage()));
                success = false;
            }
        }

        if (success) {
            this.assembledBytecodes = bytecodes;
            return Status.Success;
        } else
            return Status.Failure;
    }

    public List<Bytecode> getAssembledBytecodes() {
        return assembledBytecodes;
    }

    public List<ErrorMessage> getErrorMessages() {
        return errorMessages;
    }

    private void buildClass(DefaultJasmParserListener listener, DefaultJasmParserListener.ClassSpec spec, ClassBuilder cb) {
        int flags = Flags.flags(spec.classFlags().stream().map(StringView::toString).toList());
        switch (spec.classId().toString()) {
            case "class" -> {}
            case "interface" -> flags |= ClassFile.ACC_INTERFACE;
            case "enum" -> flags |= ClassFile.ACC_ENUM;
        }

        if (listener.getSourceName() != null)
            catchError(
                () -> cb.with(SourceFileAttribute.of(listener.getSourceName().toString())),
                "Invalid .source: "
            );

        final String superclassName = (spec.superclassName() == null) ? "java/lang/Object" : spec.superclassName().toString();
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
            () -> cb.withInterfaceSymbols(spec.superinterfaceNames().stream()
                .map(StringView::toString)
                .map(ClassDesc::ofInternalName)
                .toList()
            ),
            "Invalid .interface: "
        );


        for (var field : spec.fields()) {
            catchError(
                () -> cb.withField(
                        field.fieldName().toString(),
                        ClassDesc.ofDescriptor(field.fieldDescriptor().toString()),
                        Flags.flags(field.flags().stream().map(StringView::toString).toList())
                ),
                "Invalid .field: "
            );
        }

        for (var method : spec.methods()) {
            catchError(
                () -> cb.withMethod(
                        method.methodName().toString(),
                        MethodTypeDesc.ofDescriptor(method.descriptor().toString()),
                        Flags.flags(method.flags().stream().map(StringView::toString).toList()),
                        mb -> buildMethod(method, spec.methodCodes().get(method), mb)
                ),
                "Invalid .method: "
            );
        }

        if (!errorMessages.isEmpty())
            throw new AbortClassfileGenerationException();
    }

    private void catchError(Failable f, String messagePrefix) {
        try {
            f.run();
        } catch (Exception ex) {
            errorMessages.add(new ErrorMessage(messagePrefix + ex.getMessage()));
        }
    }

    private void buildMethod(MethodDefinition definition, MethodCode code, MethodBuilder mb) {
        if (code != null)
            mb.withCode(cb -> buildMethodCode(code, cb));
    }

    private void buildMethodCode(MethodCode code, CodeBuilder cb) {
        Map<String, Label> labels = new HashMap<>();
        for (var instr : code.instructions()) {
            try {
                Instructions.enter(instr.opcode(), instr.operands(), labels, cb);
            } catch (AssemblyException ex) {
                String message = (ex.getCause() != null && ex.getCause().getMessage() != null)
                    ? ex.getMessage() + "\n" + ex.getCause().getMessage()
                    : ex.getMessage();
                errorMessages.add(new ErrorMessage(
                    message, instr.text(),
                    instr.line(), ex.getTarget().map(sv -> sv.start() + 1).orElse(ErrorMessage.UNSPECIFIC)
                ));
            }
        }
    }

}

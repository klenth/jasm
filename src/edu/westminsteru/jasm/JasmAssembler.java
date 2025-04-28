package edu.westminsteru.jasm;

import edu.westminsteru.jasm.parser.JasmParser;
import edu.westminsteru.jasm.parser.StringView;

import java.io.*;
import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.*;

/**
 * Assembler for jasm files. The usual process for using this class looks like
 * <ol>
 *     <li>Use the {@link #reading(String) String} or {@link #reading(Reader) Reader} overload of the static {@code reading} method to obtain an instance that reads from the given input.</li>
 *     <li>Call {@link #assemble()} to attempt assembly of the input code. This method returns {@link Status}.{@code Success} or {@code .Failure} to signify success or failure of assembly.</li>
 *     <li>If {@code assemble()} failed, use {@link #getErrorMessages()} to obtain a list of {@link ErrorMessage}s describing the errors that occurred.</li>
 *     <li>Otherwise, the {@link #getAssembledBytecodes()} returns {@link Bytecode} objects, each consisting of a class name and its assembled bytecode (as a {@code byte[]}). These bytes are suitable to be written to a .class file or given to a {@link ClassLoader}.</li>
 * </ol>
 */
public class JasmAssembler {

    /**
     * The current version of the jasm assembler
     */
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
    private List<Bytecode> assembledBytecodes = null;
    private boolean haveAssembled = false;

    private JasmAssembler(BufferedReader in) {
        this.in = in;
    }

    /**
     * Creates a {@code JasmAssembler} that reads from the given {@code Reader}.
     * @param in the reader to read code from
     * @return a {@code JasmAssembler}
     */
    public static JasmAssembler reading(Reader in) {
        BufferedReader buffered = (in instanceof BufferedReader br) ? br : new BufferedReader(in);
        return new JasmAssembler(buffered);
    }

    /**
     * Creates a {@code JasmAssembler} that reads code directly from a {@code String}.
     * @param input the {@code String} to read code from
     * @return a {@code JasmAssembler}
     */
    public static JasmAssembler reading(String input) {
        return new JasmAssembler(
            new BufferedReader(
                new StringReader(input)
            )
        );
    }

    /**
     * Attempts to assemble the code obtained from whatever input source was given when the {@code JasmAssembler} was created. This method should be called only once.
     * @return {@link Status#Success} if assembly succeeded, or {@link Status#Failure} if there was an error in the code
     */
    public Status assemble() {
        if (haveAssembled)
            throw new IllegalStateException("assemble() has already been called");
        haveAssembled = true;

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

    /**
     * Returns the bytecodes resulting from a previous successful call to {@link #assemble()}.
     * @return a list of the bytecodes
     * @throws IllegalArgumentException if this method is called before {@link #assemble()}, or if {@code assemble()} returned {@link Status#Failure}
     */
    public List<Bytecode> getAssembledBytecodes() {
        if (!haveAssembled)
            throw new IllegalArgumentException("getAssembledBytecodes() called before assemble()");
        else if (assembledBytecodes == null)
            throw new IllegalStateException("getAssembledBytecodes() called after assembly failed");

        return assembledBytecodes;
    }

    /**
     * Returns the error messages resulting from a previous unsuccessful call to {@link #assemble()}.
     * @return a list of error messages (empty if there were no error messages)
     * @throws IllegalStateException if this method is called before {@link #assemble()}
     */
    public List<ErrorMessage> getErrorMessages() {
        if (!haveAssembled)
            throw new IllegalArgumentException("getErrorMessages() called before assemble()");
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

        ListIterator<CodeItem> it = code.codeItems().listIterator();
        while (it.hasNext()) {
            var item = it.next();

            switch (item) {
                case Instruction instr
                    when (instr.opcode().toString().equals("lookupswitch")
                        || instr.opcode().toString().equals("tableswitch")) -> {
                    if (!it.hasNext()) {
                        errorMessages.add(new ErrorMessage(
                            String.format("Expected .table after opcode %s", instr.opcode()),
                            instr.text(), instr.line(), ErrorMessage.UNSPECIFIC
                        ));
                        continue;
                    }

                    if (!(it.next() instanceof Table table)) {
                        it.previous();
                        errorMessages.add(new ErrorMessage(
                            String.format("Expected .table after opcode %s", instr.opcode()),
                            instr.text(), instr.line(), ErrorMessage.UNSPECIFIC
                        ));
                    } else {
                        try {
                            Instructions.enterTableInstruction(instr.opcode(), instr.operands(), table, labels, cb);
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

                case Instruction instr -> {
                    instr.labels().forEach(l ->
                        cb.labelBinding(labels.computeIfAbsent(l.toString(), _ -> cb.newLabel()))
                    );
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

                case Table table -> {
                    errorMessages.add(new ErrorMessage(
                        "Table not expected",
                        null, ErrorMessage.UNSPECIFIC, ErrorMessage.UNSPECIFIC
                    ));
                }
            }
        }
    }

}

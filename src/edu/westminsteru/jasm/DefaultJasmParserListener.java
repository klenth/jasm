package edu.westminsteru.jasm;

import edu.westminsteru.jasm.parser.JasmParser;
import edu.westminsteru.jasm.parser.JasmParserListener;
import edu.westminsteru.jasm.parser.JasmSyntaxException;
import edu.westminsteru.jasm.parser.StringView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DefaultJasmParserListener implements JasmParserListener {

    record ClassSpec(
        StringView classId,
        StringView className, StringView superclassName, List<StringView> superinterfaceNames,
        List<StringView> classFlags,
        List<FieldDefinition> fields,
        List<MethodDefinition> methods,
        Map<MethodDefinition, MethodCode> methodCodes
    ) {}

    // global stuff
    private StringView sourceName;
    private List<ErrorMessage> errors = new ArrayList<>();
    private List<ClassSpec> classSpecs = new ArrayList<>();

    // per-class stuff
    private StringView classId;
    private StringView className, superclassName;
    private List<StringView> classFlags;
    private List<StringView> superinterfaceNames = new ArrayList<>();
    private List<FieldDefinition> fields = new ArrayList<>();
    private List<MethodDefinition> methods = new ArrayList<>();
    private Map<MethodDefinition, MethodCode> methodCodes = new HashMap<>();
    private List<Table.Entry> tableEntries;

    // per-method stuff
    private List<CodeItem> currentMethodCodeItems = null;
    private List<StringView> instructionLabels = new ArrayList<>();

    @Override
    public void exceptionOccurred(JasmParser parser, JasmSyntaxException ex) {
        errors.add(new ErrorMessage(
            ex.getMessage(), ex.getLine(),
            ex.getLineNumber(), ex.getColumnNumber() + 1
        ));
    }

    @Override
    public void sourceDirective(JasmParser parser, StringView source) {
        if (this.sourceName != null)
            errors.add(new ErrorMessage(
                "Duplicate .source directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else
            this.sourceName = source;
    }

    @Override
    public void classDirective(JasmParser parser, StringView classId, List<StringView> flags, StringView className) {
        if (this.className != null)
            commitClassSpec();

        this.classId = classId;
        this.classFlags = flags;
        this.className = className;
    }

    @Override
    public void superDirective(JasmParser parser, StringView superName) {
        if (this.superclassName != null)
            errors.add(new ErrorMessage(
                "Duplicate .super directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else
            this.superclassName = superName;
    }

    @Override
    public void implementsDirective(JasmParser parser, StringView interfaceName) {
        this.superinterfaceNames.add(interfaceName);
    }

    @Override
    public void fieldDirective(JasmParser parser, List<StringView> flags, StringView name, StringView descriptor) {
        this.fields.add(new FieldDefinition(flags, name, descriptor));
    }

    @Override
    public void methodDirective(JasmParser parser, List<StringView> flags, StringView name, StringView descriptor) {
        this.methods.add(new MethodDefinition(flags, name, descriptor));
    }

    @Override
    public void codeDirective(JasmParser parser) {
        if (this.currentMethodCodeItems != null)
            errors.add(new ErrorMessage(
                "Duplicate .code directive", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else
            this.currentMethodCodeItems = new ArrayList<>();
    }

    @Override
    public void codeLabel(JasmParser parser, StringView labelName) {
        if (this.instructionLabels.contains(labelName)) {
            errors.add(new ErrorMessage(
                "Duplicate label definition", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        } else
            this.instructionLabels.add(labelName);
    }

    @Override
    public void codeInstruction(JasmParser parser, StringView opcode, List<StringView> operands) {
        this.currentMethodCodeItems.add(
            new Instruction(new ArrayList<>(instructionLabels), opcode, operands, parser.getCurrentLine(), parser.getCurrentLineNumber())
        );
        instructionLabels.clear();
    }

    @Override
    public void endCodeDirective(JasmParser parser) {
        if (currentMethodCodeItems == null)
            errors.add(new ErrorMessage(
                ".end code directive not expected here", parser.getCurrentLine(),
                parser.getCurrentLineNumber(), ErrorMessage.UNSPECIFIC
            ));
        else if (!methods.isEmpty()) {
            methodCodes.put(methods.getLast(), new MethodCode(currentMethodCodeItems));
            currentMethodCodeItems = null;
        }
    }

    @Override
    public void tableDirective(JasmParser parser) {
        tableEntries = new ArrayList<>();
    }

    @Override
    public void tableLine(JasmParser parser, StringView label, StringView target) {
        tableEntries.add(new Table.Entry(label, target));
    }

    @Override
    public void endTableDirective(JasmParser parser) {
        currentMethodCodeItems.add(new Table(tableEntries));
        tableEntries = null;
    }

    @Override
    public void endOfInput(JasmParser parser) {
        commitClassSpec();
    }

    private void commitClassSpec() {
        var spec = new ClassSpec(
            classId,
            className, superclassName, superinterfaceNames,
            classFlags,
            fields,
            methods,
            methodCodes
        );
        classSpecs.add(spec);

        classId = className = superclassName = null;
        superinterfaceNames = new ArrayList<>();
        classFlags = new ArrayList<>();
        fields = new ArrayList<>();
        methods = new ArrayList<>();
        methodCodes = new HashMap<>();
    }

    public StringView getSourceName() {
        return sourceName;
    }

    public List<ErrorMessage> getErrors() {
        return errors;
    }

    public List<ClassSpec> getClassSpecs() {
        return classSpecs;
    }
}

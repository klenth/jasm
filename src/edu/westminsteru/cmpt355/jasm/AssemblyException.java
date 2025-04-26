package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.StringView;

import java.util.Optional;

public class AssemblyException extends Exception {

    private StringView targetView = null;

    public AssemblyException(String message) {
        super(message);
    }

    public AssemblyException(String message, StringView targetView) {
        this(message);
        this.targetView = targetView;
    }

    public AssemblyException() {
        super();
    }

    public AssemblyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AssemblyException(Throwable cause) {
        super(cause);
    }

    protected AssemblyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Optional<StringView> getTarget() {
        return Optional.ofNullable(targetView);
    }
}

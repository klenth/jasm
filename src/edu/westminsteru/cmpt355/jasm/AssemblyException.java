package edu.westminsteru.cmpt355.jasm;

public class AssemblyException extends Exception {
    public AssemblyException(String message) {
        super(message);
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
}

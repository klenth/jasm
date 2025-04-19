package edu.westminsteru.cmpt355.jasm;

public class AbortClassfileGenerationException extends RuntimeException {
    public AbortClassfileGenerationException(String message) {
        super(message);
    }

    public AbortClassfileGenerationException() {
        super();
    }

    public AbortClassfileGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortClassfileGenerationException(Throwable cause) {
        super(cause);
    }

    protected AbortClassfileGenerationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

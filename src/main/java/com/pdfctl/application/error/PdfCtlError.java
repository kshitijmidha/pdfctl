package com.pdfctl.application.error;

public abstract class PdfCtlError extends RuntimeException {

    private final ExitCode exitCode;

    protected PdfCtlError(String message, ExitCode exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    protected PdfCtlError(String message, ExitCode exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode exitCode() {
        return exitCode;
    }

    public int code() {
        return exitCode.code();
    }
}

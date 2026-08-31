package com.pdfctl.application.error;

public class IoException extends PdfCtlError {
    public IoException(String message) {
        super(message, ExitCode.IO_ERROR);
    }

    public IoException(String message, Throwable cause) {
        super(message, ExitCode.IO_ERROR, cause);
    }
}

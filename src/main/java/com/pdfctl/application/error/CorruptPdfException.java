package com.pdfctl.application.error;

public class CorruptPdfException extends PdfCtlError {
    public CorruptPdfException(String message) {
        super(message, ExitCode.CORRUPT_PDF);
    }

    public CorruptPdfException(String message, Throwable cause) {
        super(message, ExitCode.CORRUPT_PDF, cause);
    }
}

package com.pdfctl.application.error;

public class BadInputException extends PdfCtlError {
    public BadInputException(String message) {
        super(message, ExitCode.USAGE);
    }
}

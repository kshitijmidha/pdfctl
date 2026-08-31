package com.pdfctl.application.error;

public class PageRangeException extends PdfCtlError {
    public PageRangeException(String message) {
        super(message, ExitCode.USAGE);
    }
}

package com.pdfctl.application.error;

public class EncryptedPdfException extends PdfCtlError {
    public EncryptedPdfException(String message) {
        super(message, ExitCode.ENCRYPTED_PDF);
    }
}

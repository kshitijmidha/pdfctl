package com.pdfctl.application.error;

public enum ExitCode {
    OK(0),
    USAGE(1),
    IO_ERROR(2),
    CORRUPT_PDF(3),
    ENCRYPTED_PDF(4);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}

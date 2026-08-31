package com.pdfctl;

import com.pdfctl.application.error.PdfCtlError;
import com.pdfctl.cli.PdfCtl;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new PdfCtl());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof PdfCtlError e) {
                commandLine.getErr().println("pdfctl: error: " + e.getMessage());
                return e.code();
            }
            commandLine.getErr().println("pdfctl: unexpected error: " + ex.getMessage());
            ex.printStackTrace(commandLine.getErr());
            return 3;
        });
        int exit = cmd.execute(args);
        System.exit(exit);
    }
}

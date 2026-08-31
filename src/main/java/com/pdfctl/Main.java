package com.pdfctl;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        CommandLine cmd = AppFactory.createCommandLine();
        int exit = cmd.execute(args);
        System.exit(exit);
    }
}

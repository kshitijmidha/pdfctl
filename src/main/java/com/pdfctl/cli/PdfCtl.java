package com.pdfctl.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "pdfctl",
        description = "pdfctl — production-quality PDF toolkit",
        mixinStandardHelpOptions = true,
        version = "pdfctl 0.1.0",
        descriptionHeading = "%nDescription:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n",
        subcommands = {InfoCommand.class}
)
public class PdfCtl implements Runnable {

    @Spec
    CommandSpec spec;

    @Option(names = "--force", description = "overwrite existing output files")
    boolean force;

    @Override
    public void run() {
        // No subcommand given — print hint via picocli's err writer so tests can capture,
        // falling back to System.err when run outside picocli (e.g. direct instantiation).
        String msg = "pdfctl: no command specified. Try 'pdfctl --help' for usage.";
        if (spec != null && spec.commandLine() != null) {
            spec.commandLine().getErr().println(msg);
        } else {
            System.err.println(msg);
        }
    }
}

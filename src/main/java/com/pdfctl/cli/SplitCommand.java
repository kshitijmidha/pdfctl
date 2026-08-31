package com.pdfctl.cli;

import com.pdfctl.application.usecase.SplitUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "split", description = "Split PDF into pages", mixinStandardHelpOptions = true)
public class SplitCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input PDF file")
    Path input;

    @Option(names = {"-o", "--output"}, required = true, description = "Output directory or file (for --pages)")
    Path output;

    @Option(names = "--pages", description = "Pages to extract (e.g. 1,3,5-7,10-)", paramLabel = "SPEC")
    String pages;

    @Option(names = "--password", description = "Password for encrypted PDF", paramLabel = "PASSWORD")
    String password;

    @Option(names = "--force", description = "Overwrite existing files")
    boolean force;

    @Spec
    CommandSpec spec;

    private final SplitUseCase useCase;

    public SplitCommand(SplitUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Integer call() {
        useCase.execute(input, output, pages, password, force);
        if (pages == null || pages.trim().isEmpty()) {
            spec.commandLine().getOut().println("Split into " + output);
        } else {
            spec.commandLine().getOut().println("Split pages " + pages + " into " + output);
        }
        return 0;
    }
}

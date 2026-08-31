package com.pdfctl.cli;

import com.pdfctl.application.usecase.DeleteUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "delete", description = "Delete pages from PDF", mixinStandardHelpOptions = true)
public class DeleteCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input PDF file")
    Path input;

    @Option(names = "--pages", required = true, description = "Pages to delete (e.g. 2,4-6)", paramLabel = "SPEC")
    String pages;

    @Option(names = {"-o", "--output"}, required = true, description = "Output PDF file")
    Path output;

    @Option(names = "--password", description = "Password for encrypted PDF", paramLabel = "PASSWORD")
    String password;

    @Option(names = "--force", description = "Overwrite existing output")
    boolean force;

    @Spec
    CommandSpec spec;

    private final DeleteUseCase useCase;

    public DeleteCommand(DeleteUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Integer call() {
        useCase.execute(input, output, pages, password, force);
        spec.commandLine().getOut().println("Deleted pages " + pages + " into " + output);
        return 0;
    }
}

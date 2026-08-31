package com.pdfctl.cli;

import com.pdfctl.application.usecase.MergeUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "merge", description = "Merge multiple PDFs into one", mixinStandardHelpOptions = true)
public class MergeCommand implements Callable<Integer> {

    @Parameters(index = "0..*", paramLabel = "INPUT", description = "Input PDF files (at least 2)")
    List<Path> inputs;

    @Option(names = {"-o", "--output"}, required = true, description = "Output PDF file")
    Path output;

    @Option(names = "--password", description = "Password for encrypted PDFs", paramLabel = "PASSWORD")
    String password;

    @Option(names = "--force", description = "Overwrite existing output")
    boolean force;

    @Spec
    CommandSpec spec;

    private final MergeUseCase useCase;

    public MergeCommand(MergeUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Integer call() {
        useCase.execute(inputs, output, password, force);
        spec.commandLine().getOut().println("Merged " + inputs.size() + " files into " + output);
        return 0;
    }
}

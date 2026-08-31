package com.pdfctl.cli;

import com.pdfctl.application.usecase.RotateUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "rotate", description = "Rotate pages in PDF", mixinStandardHelpOptions = true)
public class RotateCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input PDF file")
    Path input;

    @Option(names = "--angle", required = true, description = "Rotation angle: 90, 180, or 270")
    int angle;

    @Option(names = "--pages", description = "Pages to rotate (e.g. 1,3,5-7), default all pages", paramLabel = "SPEC")
    String pages;

    @Option(names = {"-o", "--output"}, required = true, description = "Output PDF file")
    Path output;

    @Option(names = "--password", description = "Password for encrypted PDF", paramLabel = "PASSWORD")
    String password;

    @Option(names = "--force", description = "Overwrite existing output")
    boolean force;

    @Spec
    CommandSpec spec;

    private final RotateUseCase useCase;

    public RotateCommand(RotateUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Integer call() {
        useCase.execute(input, output, pages, angle, password, force);
        spec.commandLine().getOut().println("Rotated " + (pages == null ? "all pages" : "pages " + pages) + " by " + angle + " into " + output);
        return 0;
    }
}

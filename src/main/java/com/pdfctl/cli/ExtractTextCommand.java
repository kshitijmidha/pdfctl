package com.pdfctl.cli;

import com.pdfctl.application.usecase.ExtractTextUseCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "extract-text", description = "Extract text from PDF", mixinStandardHelpOptions = true)
public class ExtractTextCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input PDF file")
    Path input;

    @Option(names = "--pages", description = "Pages to extract (e.g. 2,4-5)", paramLabel = "SPEC")
    String pages;

    @Option(names = {"-o", "--output"}, description = "Output text file (default: stdout)")
    Path output;

    @Option(names = "--password", description = "Password for encrypted PDF", paramLabel = "PASSWORD")
    String password;

    @Option(names = "--force", description = "Overwrite existing output file")
    boolean force;

    @Spec
    CommandSpec spec;

    private final ExtractTextUseCase useCase;

    public ExtractTextCommand(ExtractTextUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Integer call() throws Exception {
        if (output != null) {
            useCase.executeToFile(input, output, pages, password, force);
            spec.commandLine().getOut().println("Extracted text to " + output);
        } else {
            String text = useCase.execute(input, pages, password);
            var out = spec != null ? spec.commandLine().getOut() : new java.io.PrintWriter(System.out, true, StandardCharsets.UTF_8);
            out.print(text);
            out.flush();
        }
        return 0;
    }
}

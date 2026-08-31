package com.pdfctl.cli;

import com.pdfctl.application.dto.PdfDocumentInfo;
import com.pdfctl.application.usecase.InfoUseCase;
import com.pdfctl.infrastructure.pdfbox.PdfBoxServiceImpl;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "info", description = "Show PDF information", mixinStandardHelpOptions = true)
public class InfoCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input PDF file")
    Path input;

    @Option(names = "--json", description = "Output in JSON format")
    boolean json;

    @Option(names = "--password", description = "Password for encrypted PDF", paramLabel = "PASSWORD")
    String password;

    @Spec
    CommandSpec spec;

    // For testability: allow injection
    private final InfoUseCase injectedUseCase;

    public InfoCommand() {
        this.injectedUseCase = null;
    }

    // Test constructor
    InfoCommand(InfoUseCase useCase) {
        this.injectedUseCase = useCase;
    }

    @Override
    public Integer call() {
        InfoUseCase useCase = injectedUseCase != null ? injectedUseCase : new InfoUseCase(new PdfBoxServiceImpl());
        PdfDocumentInfo info = useCase.execute(input, password);

        var out = spec != null ? spec.commandLine().getOut() : new java.io.PrintWriter(System.out, true);

        if (json) {
            out.println(JsonRenderer.toJson(info));
        } else {
            renderHuman(info, out);
        }
        out.flush();
        return 0;
    }

    static void renderHuman(PdfDocumentInfo info, java.io.PrintWriter out) {
        // Keep output deterministic and simple — no table deps
        out.println("File:      " + info.fileName());
        out.println("Size:      " + info.fileSize() + " bytes");
        out.println("Version:   " + info.pdfVersion());
        out.println("Pages:     " + info.pageCount());
        out.println("Encrypted: " + (info.encrypted() ? "yes" : "no"));
        out.println("Title:     " + orDash(info.title()));
        out.println("Author:    " + orDash(info.author()));
        out.println("Creator:   " + orDash(info.creator()));
        out.println("Producer:  " + orDash(info.producer()));
        if (info.subject() != null) {
            out.println("Subject:   " + info.subject());
        }
        if (info.keywords() != null) {
            out.println("Keywords:  " + info.keywords());
        }
    }

    private static String orDash(String s) {
        return s == null ? "-" : s;
    }
}

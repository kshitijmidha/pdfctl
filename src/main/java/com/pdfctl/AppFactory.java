package com.pdfctl;

import com.pdfctl.application.error.PdfCtlError;
import com.pdfctl.application.usecase.DeleteUseCase;
import com.pdfctl.application.usecase.ExtractTextUseCase;
import com.pdfctl.application.usecase.InfoUseCase;
import com.pdfctl.application.usecase.MergeUseCase;
import com.pdfctl.application.usecase.RotateUseCase;
import com.pdfctl.application.usecase.SplitUseCase;
import com.pdfctl.cli.DeleteCommand;
import com.pdfctl.cli.ExtractTextCommand;
import com.pdfctl.cli.InfoCommand;
import com.pdfctl.cli.MergeCommand;
import com.pdfctl.cli.PdfCtl;
import com.pdfctl.cli.RotateCommand;
import com.pdfctl.cli.SplitCommand;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;
import com.pdfctl.infrastructure.pdfbox.PdfBoxServiceImpl;
import picocli.CommandLine;

/**
 * Composition root — the only place that knows concrete infrastructure types.
 * CLI and application layers depend only on abstractions.
 */
public final class AppFactory {

    private AppFactory() {}

    public static CommandLine createCommandLine() {
        PdfBoxService pdfBoxService = new PdfBoxServiceImpl();

        InfoUseCase infoUseCase = new InfoUseCase(pdfBoxService);
        MergeUseCase mergeUseCase = new MergeUseCase(pdfBoxService);
        SplitUseCase splitUseCase = new SplitUseCase(pdfBoxService);
        DeleteUseCase deleteUseCase = new DeleteUseCase(pdfBoxService);
        RotateUseCase rotateUseCase = new RotateUseCase(pdfBoxService);
        ExtractTextUseCase extractUseCase = new ExtractTextUseCase(pdfBoxService);

        PdfCtl root = new PdfCtl();
        CommandLine cmd = new CommandLine(root);

        // Register subcommands with injected use cases — CLI never constructs PdfBoxServiceImpl
        cmd.addSubcommand("info", new InfoCommand(infoUseCase));
        cmd.addSubcommand("merge", new MergeCommand(mergeUseCase));
        cmd.addSubcommand("split", new SplitCommand(splitUseCase));
        cmd.addSubcommand("delete", new DeleteCommand(deleteUseCase));
        cmd.addSubcommand("rotate", new RotateCommand(rotateUseCase));
        cmd.addSubcommand("extract-text", new ExtractTextCommand(extractUseCase));

        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            if (ex instanceof PdfCtlError e) {
                commandLine.getErr().println("pdfctl: error: " + e.getMessage());
                return e.code();
            }
            commandLine.getErr().println("pdfctl: unexpected error: " + ex.getMessage());
            ex.printStackTrace(commandLine.getErr());
            return 3;
        });

        return cmd;
    }
}

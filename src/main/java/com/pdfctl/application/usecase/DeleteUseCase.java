package com.pdfctl.application.usecase;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeleteUseCase {

    private final PdfBoxService pdfBoxService;

    public DeleteUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public void execute(Path input, Path output, String pagesSpec, String password, boolean force) {
        if (pagesSpec == null || pagesSpec.trim().isEmpty()) {
            throw new BadInputException("delete requires --pages");
        }
        FileOperations.validateInputExists(input);
        FileOperations.requireNotSameFile(input, output);
        FileOperations.validateOutput(output, force);
        Path tmp = FileOperations.createTempFileInSameDir(output);
        try {
            pdfBoxService.delete(input, tmp, pagesSpec, password);
            FileOperations.moveTempToOutput(tmp, output);
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw e;
        }
    }
}

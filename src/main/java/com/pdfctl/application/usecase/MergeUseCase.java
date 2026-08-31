package com.pdfctl.application.usecase;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.nio.file.Path;
import java.util.List;

public class MergeUseCase {

    private final PdfBoxService pdfBoxService;

    public MergeUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public void execute(List<Path> inputs, Path output, String password, boolean force) {
        if (inputs == null || inputs.size() < 2) {
            throw new BadInputException("merge requires at least 2 input files");
        }
        for (Path p : inputs) {
            FileOperations.validateInputExists(p);
        }
        for (Path p : inputs) {
            FileOperations.requireNotSameFile(p, output);
        }
        FileOperations.validateOutput(output, force);
        Path tmp = FileOperations.createTempFileInSameDir(output);
        try {
            pdfBoxService.merge(inputs, tmp, password);
            FileOperations.moveTempToOutput(tmp, output);
        } catch (RuntimeException e) {
            // cleanup temp on failure
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw e;
        }
    }
}

package com.pdfctl.application.usecase;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RotateUseCase {

    private final PdfBoxService pdfBoxService;

    public RotateUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public void execute(Path input, Path output, String pagesSpec, int angle, String password, boolean force) {
        if (angle != 90 && angle != 180 && angle != 270) {
            throw new BadInputException("angle must be 90, 180, or 270, got " + angle);
        }
        FileOperations.validateInputExists(input);
        FileOperations.requireNotSameFile(input, output);
        FileOperations.validateOutput(output, force);
        Path tmp = FileOperations.createTempFileInSameDir(output);
        try {
            pdfBoxService.rotate(input, tmp, pagesSpec, angle, password);
            FileOperations.moveTempToOutput(tmp, output);
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw e;
        }
    }
}

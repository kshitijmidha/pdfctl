package com.pdfctl.application.usecase;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.application.error.IoException;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ExtractTextUseCase {

    private final PdfBoxService pdfBoxService;

    public ExtractTextUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public String execute(Path input, String pagesSpec, String password) {
        FileOperations.validateInputExists(input);
        return pdfBoxService.extractText(input, pagesSpec, password);
    }

    public void executeToFile(Path input, Path output, String pagesSpec, String password, boolean force) {
        FileOperations.validateInputExists(input);
        if (output == null) throw new BadInputException("output must not be null");
        FileOperations.requireNotSameFile(input, output);
        FileOperations.validateOutput(output, force);
        Path tmp = FileOperations.createTempFileInSameDir(output);
        try {
            pdfBoxService.extractTextToFile(input, pagesSpec, password, tmp);
            FileOperations.moveTempToOutput(tmp, output);
        } catch (RuntimeException e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw e;
        } catch (Exception e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw new IoException("failed to write text output: " + output, e);
        }
    }
}

package com.pdfctl.application.usecase;

import com.pdfctl.application.dto.PdfDocumentInfo;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.nio.file.Path;

public class InfoUseCase {

    private final PdfBoxService pdfBoxService;

    public InfoUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public PdfDocumentInfo execute(Path input, String password) {
        // 1. validate input through existing file infrastructure (exit 2 on failure)
        FileOperations.validateInputExists(input);
        // 2. delegate to PDF service (maps IOException -> exit 3/4)
        return pdfBoxService.inspect(input, password);
    }
}

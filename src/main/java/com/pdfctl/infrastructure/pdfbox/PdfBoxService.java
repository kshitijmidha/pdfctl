package com.pdfctl.infrastructure.pdfbox;

import com.pdfctl.application.dto.PdfDocumentInfo;

import java.nio.file.Path;

public interface PdfBoxService {
    PdfDocumentInfo inspect(Path input, String password);
}

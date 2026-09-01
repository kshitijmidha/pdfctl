package com.pdfctl.infrastructure.pdfbox;

import com.pdfctl.application.dto.PdfDocumentInfo;

import java.nio.file.Path;
import java.util.List;

public interface PdfBoxService {
    PdfDocumentInfo inspect(Path input, String password);

    void merge(List<Path> inputs, Path output, String password);

    // Split without pages: explode into individual page files in outputDir
    void splitAll(Path input, Path outputDir, String password);

    // Split with pages: single PDF containing selected pages
    void splitSelected(Path input, Path outputFile, String pagesSpec, String password);

    void delete(Path input, Path output, String pagesSpec, String password);

    void rotate(Path input, Path output, String pagesSpec, int angle, String password);

    String extractText(Path input, String pagesSpec, String password);

    void extractTextToFile(Path input, String pagesSpec, String password, Path outputFile);
}

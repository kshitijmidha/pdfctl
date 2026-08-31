package com.pdfctl.application.usecase;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.application.error.IoException;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SplitUseCase {

    private final PdfBoxService pdfBoxService;

    public SplitUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    /**
     * @param input input PDF
     * @param output output path: if pagesSpec is null/empty, must be a directory; if pagesSpec provided, may be file or directory
     * @param pagesSpec 1-indexed page spec or null for explode
     * @param password optional
     */
    public void execute(Path input, Path output, String pagesSpec, String password, boolean force) {
        FileOperations.validateInputExists(input);
        boolean hasPages = pagesSpec != null && !pagesSpec.trim().isEmpty();

        if (hasPages) {
            // Single file output: if output is existing directory, create file inside
            Path targetFile;
            if (Files.exists(output) && Files.isDirectory(output)) {
                targetFile = output.resolve("split.pdf");
            } else if (output.toString().endsWith("/") || output.toString().endsWith("\\")) {
                // trailing slash indicates directory
                targetFile = output.resolve("split.pdf");
            } else {
                // treat as file
                // if output has no extension and not existing, still treat as file
                // we consider if output path contains directory component, ensure parent exists
                targetFile = output;
            }
            // Validate file output
            FileOperations.validateOutput(targetFile, force);
            FileOperations.requireNotSameFile(input, targetFile);
            Path tmp = FileOperations.createTempFileInSameDir(targetFile);
            try {
                pdfBoxService.splitSelected(input, tmp, pagesSpec, password);
                FileOperations.moveTempToOutput(tmp, targetFile);
            } catch (RuntimeException e) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                throw e;
            }
        } else {
            // Explode: output must be directory
            Path dir = output;
            try {
                if (Files.exists(dir) && !Files.isDirectory(dir)) {
                    throw new IoException("output exists and is not a directory: " + dir);
                }
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
            } catch (IOException e) {
                throw new IoException("failed to create output directory: " + dir, e);
            }
            // For explode, we check if files would be overwritten without force
            // We pre-check: if force false and any page-*.pdf exists, fail
            // Let service create files, but we can validate beforehand by listing expected count?
            // Instead, let service handle and we will check after? Simpler: check existence of at least first file?
            // We'll delegate and handle overwrite via service throwing? But service currently overwrites.
            // So we need to pre-check: if not force and directory not empty, we could reject if any expected files exist
            // For now, rely on service overwriting, but we document that force controls file outputs for split with pages only
            // For explode without force, we should not overwrite existing page files
            if (!force) {
                try (var stream = Files.list(dir)) {
                    boolean hasPdf = stream.anyMatch(p -> p.getFileName().toString().startsWith("page-") && p.toString().endsWith(".pdf"));
                    if (hasPdf) {
                        throw new IoException("output directory contains existing split files: " + dir + " (use --force to overwrite)");
                    }
                } catch (IOException e) {
                    throw new IoException("failed to check output directory: " + dir, e);
                }
            }
            FileOperations.requireNotSameFile(input, dir);
            pdfBoxService.splitAll(input, dir, password);
        }
    }
}

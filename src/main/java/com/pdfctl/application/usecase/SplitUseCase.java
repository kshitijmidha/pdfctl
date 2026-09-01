package com.pdfctl.application.usecase;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.application.error.IoException;
import com.pdfctl.infrastructure.io.FileOperations;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SplitUseCase {

    private final PdfBoxService pdfBoxService;

    public SplitUseCase(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public void execute(Path input, Path output, String pagesSpec, String password, boolean force) {
        FileOperations.validateInputExists(input);
        boolean hasPages = pagesSpec != null && !pagesSpec.trim().isEmpty();

        if (hasPages) {
            // OUTPUT must be a single PDF file — reject if existing directory
            if (Files.exists(output) && Files.isDirectory(output)) {
                throw new BadInputException("output exists and is a directory, expected a file for --pages: " + output);
            }
            // Also reject trailing slash which indicates directory intent
            String outStr = output.toString();
            if (outStr.endsWith("/") || outStr.endsWith("\\")) {
                throw new BadInputException("output must be a file for --pages, got directory path: " + output);
            }
            FileOperations.validateOutput(output, force);
            FileOperations.requireNotSameFile(input, output);
            Path tmp = FileOperations.createTempFileInSameDir(output);
            try {
                pdfBoxService.splitSelected(input, tmp, pagesSpec, password);
                FileOperations.moveTempToOutput(tmp, output);
            } catch (RuntimeException e) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                throw e;
            }
        } else {
            // Explode: output must be a directory
            Path dir = output;
            if (Files.exists(dir) && !Files.isDirectory(dir)) {
                throw new BadInputException("output exists and is not a directory: " + dir);
            }
            try {
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
            } catch (IOException e) {
                throw new IoException("failed to create output directory: " + dir, e);
            }
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
            // Track files before to clean up only newly created on failure
            Set<Path> before = listPageFiles(dir);
            try {
                pdfBoxService.splitAll(input, dir, password);
            } catch (RuntimeException e) {
                Set<Path> after = listPageFiles(dir);
                // Delete only files that were not present before
                for (Path p : after) {
                    if (!before.contains(p)) {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    }
                }
                throw e;
            }
        }
    }

    private Set<Path> listPageFiles(Path dir) {
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().startsWith("page-") && p.toString().endsWith(".pdf"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return new HashSet<>();
        }
    }
}

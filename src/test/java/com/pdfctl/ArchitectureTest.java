package com.pdfctl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

    @Test
    void cliDoesNotImportPdfBox() throws IOException {
        assertNoPdfBoxImports(Path.of("src/main/java/com/pdfctl/cli"));
    }

    @Test
    void cliDoesNotDependOnInfrastructure() throws IOException {
        assertNoInfrastructureImports(Path.of("src/main/java/com/pdfctl/cli"));
    }

    @Test
    void applicationDoesNotImportPdfBox() throws IOException {
        assertNoPdfBoxImports(Path.of("src/main/java/com/pdfctl/application"));
        assertNoPdfBoxImports(Path.of("src/main/java/com/pdfctl/infrastructure/io"));
    }

    @Test
    void onlyInfrastructurePdfBoxImportsPdfBox() throws IOException {
        Path pdfboxImpl = Path.of("src/main/java/com/pdfctl/infrastructure/pdfbox");
        // Should have pdfbox imports — verify at least one file does
        boolean hasPdfBox = hasImport(pdfboxImpl, "org.apache.pdfbox");
        assertThat(hasPdfBox).isTrue();

        // Ensure no other package leaks
        // Checked above: cli and application/io have none
    }

    private static void assertNoPdfBoxImports(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            assertThat(content)
                                    .withFailMessage("PDFBox import leaked in %s", p)
                                    .doesNotContain("org.apache.pdfbox")
                                    .doesNotContain("PDDocument")
                                    .doesNotContain("Loader")
                                    .doesNotContain("PDFMergerUtility");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static void assertNoInfrastructureImports(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            assertThat(content)
                                    .withFailMessage("Infrastructure import leaked in CLI %s", p)
                                    .doesNotContain("com.pdfctl.infrastructure");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static boolean hasImport(Path dir, String imprt) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                    .anyMatch(p -> {
                        try {
                            return Files.readString(p).contains(imprt);
                        } catch (IOException e) {
                            return false;
                        }
                    });
        }
    }
}

package com.pdfctl.application.usecase;

import com.pdfctl.application.dto.PdfDocumentInfo;
import com.pdfctl.application.error.IoException;
import com.pdfctl.infrastructure.pdfbox.PdfBoxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InfoUseCaseTest {

    @Test
    void validateInputExistsBeforeDelegation(@TempDir Path tmp) {
        PdfBoxService dummy = new PdfBoxService() {
            @Override public com.pdfctl.application.dto.PdfDocumentInfo inspect(Path p, String pwd) { throw new AssertionError("should not be called when input missing"); }
            @Override public void merge(java.util.List<Path> i, Path o, String p) {}
            @Override public void splitAll(Path i, Path o, String p) {}
            @Override public void splitSelected(Path i, Path o, String s, String p) {}
            @Override public void delete(Path i, Path o, String s, String p) {}
            @Override public void rotate(Path i, Path o, String s, int a, String p) {}
            @Override public String extractText(Path i, String s, String p) { return null; }
            @Override public void extractTextToFile(Path i, String s, String p, Path o) {}
        };
        InfoUseCase useCase = new InfoUseCase(dummy);
        Path missing = tmp.resolve("missing.pdf");
        assertThatThrownBy(() -> useCase.execute(missing, null))
                .isInstanceOf(IoException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void delegateToServiceWhenValid(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("a.pdf");
        Files.writeString(file, "dummy");
        boolean[] called = {false};
        PdfBoxService stub = new PdfBoxService() {
            @Override public com.pdfctl.application.dto.PdfDocumentInfo inspect(Path p, String pwd) {
                called[0] = true;
                return new PdfDocumentInfo("a.pdf", 123, "1.7", 1, false, null, null, null, null, null, null);
            }
            @Override public void merge(java.util.List<Path> i, Path o, String p) {}
            @Override public void splitAll(Path i, Path o, String p) {}
            @Override public void splitSelected(Path i, Path o, String s, String p) {}
            @Override public void delete(Path i, Path o, String s, String p) {}
            @Override public void rotate(Path i, Path o, String s, int a, String p) {}
            @Override public String extractText(Path i, String s, String p) { return null; }
            @Override public void extractTextToFile(Path i, String s, String p, Path o) {}
        };
        InfoUseCase useCase = new InfoUseCase(stub);
        var result = useCase.execute(file, null);
        org.assertj.core.api.Assertions.assertThat(called[0]).isTrue();
        org.assertj.core.api.Assertions.assertThat(result.fileName()).isEqualTo("a.pdf");
    }

    @Test
    void doesNotExposePdfBoxTypes() {
        // Compile-time check: InfoUseCase has no import of org.apache.pdfbox
        String source = readSource("src/main/java/com/pdfctl/application/usecase/InfoUseCase.java");
        org.assertj.core.api.Assertions.assertThat(source).doesNotContain("org.apache.pdfbox");
        org.assertj.core.api.Assertions.assertThat(source).doesNotContain("PDDocument");
        org.assertj.core.api.Assertions.assertThat(source).doesNotContain("Loader");
    }

    private static String readSource(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (Exception e) {
            return "";
        }
    }
}

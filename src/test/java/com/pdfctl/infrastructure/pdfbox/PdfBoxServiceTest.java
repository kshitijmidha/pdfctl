package com.pdfctl.infrastructure.pdfbox;

import com.pdfctl.TestFixtures;
import com.pdfctl.application.dto.PdfDocumentInfo;
import com.pdfctl.application.error.CorruptPdfException;
import com.pdfctl.application.error.EncryptedPdfException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfBoxServiceTest {

    private final PdfBoxService service = new PdfBoxServiceImpl();

    @Test
    void inspectSinglePage(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("one.pdf");
        TestFixtures.createSimplePdf(pdf, 1);
        PdfDocumentInfo info = service.inspect(pdf, null);
        assertThat(info.pageCount()).isEqualTo(1);
        assertThat(info.fileName()).isEqualTo("one.pdf");
        assertThat(info.fileSize()).isGreaterThan(0);
        assertThat(info.pdfVersion()).isNotBlank();
        assertThat(info.encrypted()).isFalse();
    }

    @Test
    void inspectMultiPage(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("multi.pdf");
        TestFixtures.createSimplePdf(pdf, 5);
        PdfDocumentInfo info = service.inspect(pdf, null);
        assertThat(info.pageCount()).isEqualTo(5);
    }

    @Test
    void inspectWithMetadata(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("meta.pdf");
        TestFixtures.createPdfWithMetadata(pdf, 2, "My Title", "My Author", "My Creator", "My Producer", "My Subject", "kw1 kw2");
        PdfDocumentInfo info = service.inspect(pdf, null);
        assertThat(info.title()).isEqualTo("My Title");
        assertThat(info.author()).isEqualTo("My Author");
        assertThat(info.creator()).isEqualTo("My Creator");
        assertThat(info.producer()).isEqualTo("My Producer");
        assertThat(info.subject()).isEqualTo("My Subject");
        assertThat(info.keywords()).isEqualTo("kw1 kw2");
    }

    @Test
    void inspectAbsentMetadataReturnsNull(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("nometa.pdf");
        TestFixtures.createSimplePdf(pdf, 1);
        PdfDocumentInfo info = service.inspect(pdf, null);
        // Simple PDF has no title/author set by us — may have default producer, but title/author should be null or empty normalized
        assertThat(info.title()).isNull();
        assertThat(info.author()).isNull();
        assertThat(info.creator()).isNull();
        // Producer may be present (PDFBox default) — check not failing
        // We only assert title/author null for this case
    }

    @Test
    void inspectEncryptedWithoutPasswordThrows(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("enc.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "user123", "owner123");
        assertThatThrownBy(() -> service.inspect(pdf, null))
                .isInstanceOf(EncryptedPdfException.class)
                .hasMessageContaining("provide --password");
        assertThatThrownBy(() -> service.inspect(pdf, ""))
                .isInstanceOf(EncryptedPdfException.class);
    }

    @Test
    void inspectEncryptedWithWrongPasswordThrows(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("enc2.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "user123", "owner123");
        assertThatThrownBy(() -> service.inspect(pdf, "wrong"))
                .isInstanceOf(EncryptedPdfException.class)
                .hasMessageContaining("wrong password");
    }

    @Test
    void inspectEncryptedWithCorrectPasswordSucceeds(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("enc3.pdf");
        TestFixtures.createEncryptedPdf(pdf, 2, "user123", "owner123");
        PdfDocumentInfo info = service.inspect(pdf, "user123");
        assertThat(info.pageCount()).isEqualTo(2);
        assertThat(info.encrypted()).isTrue();
        assertThat(info.title()).isEqualTo("Encrypted");
    }

    @Test
    void inspectEncryptedWithOwnerPasswordSucceeds(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("encOwner.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "user123", "owner123");
        PdfDocumentInfo info = service.inspect(pdf, "owner123");
        assertThat(info.pageCount()).isEqualTo(1);
    }

    @Test
    void inspectEmptyFileThrowsCorrupt(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("empty.pdf");
        Files.createFile(pdf);
        assertThatThrownBy(() -> service.inspect(pdf, null))
                .isInstanceOf(CorruptPdfException.class);
    }

    @Test
    void inspectTruncatedPdfThrowsCorrupt(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("trunc.pdf");
        TestFixtures.createSimplePdf(pdf, 1);
        // Truncate to half size
        byte[] data = Files.readAllBytes(pdf);
        Files.write(pdf, java.util.Arrays.copyOf(data, data.length / 2));
        assertThatThrownBy(() -> service.inspect(pdf, null))
                .isInstanceOf(CorruptPdfException.class);
    }

    @Test
    void inspectMalformedNonPdfThrowsCorrupt(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("notpdf.pdf");
        Files.writeString(pdf, "This is not a PDF");
        assertThatThrownBy(() -> service.inspect(pdf, null))
                .isInstanceOf(CorruptPdfException.class);
    }

    @Test
    void inspectSpecialCharsMetadata(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("special.pdf");
        String title = "Quote \" and backslash \\ and newline\n and tab\t and unicode \u2603";
        TestFixtures.createPdfWithSpecialMetadata(pdf, title);
        PdfDocumentInfo info = service.inspect(pdf, null);
        assertThat(info.title()).isEqualTo(title);
    }

    @Test
    void inspectDoesNotLeakPasswordInException(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("encLeak.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "secret123", "owner123");
        String attempt = "secret123";
        try {
            service.inspect(pdf, "wrongpass");
        } catch (EncryptedPdfException e) {
            assertThat(e.getMessage()).doesNotContain("wrongpass");
            assertThat(e.getMessage()).doesNotContain("secret123");
        }
        // Also ensure service call itself doesn't leak via inspect with correct password
        // (no exception, but ensure returned info doesn't contain password)
        PdfDocumentInfo info = service.inspect(pdf, attempt);
        assertThat(info.toString()).doesNotContain(attempt);
    }

    @Test
    void inspectFileSizeMatchesActual(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("size.pdf");
        TestFixtures.createSimplePdf(pdf, 3);
        long actual = Files.size(pdf);
        PdfDocumentInfo info = service.inspect(pdf, null);
        assertThat(info.fileSize()).isEqualTo(actual);
    }
}

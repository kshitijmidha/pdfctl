package com.pdfctl.cli;

import com.pdfctl.application.dto.PdfDocumentInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRendererTest {

    @Test
    void escapesQuotesAndBackslashes() {
        PdfDocumentInfo info = new PdfDocumentInfo("a.pdf", 100, "1.7", 1, false,
                "Title \"quoted\" and \\ backslash", null, null, null, null, null);
        String json = JsonRenderer.toJson(info);
        assertThat(json).contains("\"title\":\"Title \\\"quoted\\\" and \\\\ backslash\"");
        // password never in json (no field)
        assertThat(json).doesNotContain("password");
    }

    @Test
    void escapesNewlinesAndUnicode() {
        PdfDocumentInfo info = new PdfDocumentInfo("a.pdf", 100, "1.4", 2, false,
                "Line1\nLine2\tTab", "Auth", "Cr", "Prod", null, null);
        String json = JsonRenderer.toJson(info);
        assertThat(json).contains("\\n");
        assertThat(json).contains("\\t");
    }

    @Test
    void nullMetadataIsNullNotString() {
        PdfDocumentInfo info = new PdfDocumentInfo("a.pdf", 10, "1.7", 1, false,
                null, null, null, null, null, null);
        String json = JsonRenderer.toJson(info);
        assertThat(json).contains("\"title\":null");
        assertThat(json).contains("\"author\":null");
    }

    @Test
    void deterministicOrdering() {
        PdfDocumentInfo info = new PdfDocumentInfo("b.pdf", 123, "1.5", 3, true,
                "T", "A", "C", "P", "S", "K");
        String json = JsonRenderer.toJson(info);
        // Check order: fileName before fileSize before pdfVersion etc.
        int iFileName = json.indexOf("\"fileName\"");
        int iFileSize = json.indexOf("\"fileSize\"");
        int iVersion = json.indexOf("\"pdfVersion\"");
        int iPages = json.indexOf("\"pageCount\"");
        int iEnc = json.indexOf("\"encrypted\"");
        int iTitle = json.indexOf("\"title\"");
        assertThat(iFileName).isLessThan(iFileSize);
        assertThat(iFileSize).isLessThan(iVersion);
        assertThat(iVersion).isLessThan(iPages);
        assertThat(iPages).isLessThan(iEnc);
        assertThat(iEnc).isLessThan(iTitle);
    }

    @Test
    void escapesControlChars() {
        String title = "a" + (char) 0x01 + "b";
        PdfDocumentInfo info = new PdfDocumentInfo("a.pdf", 1, "1.4", 1, false, title, null, null, null, null, null);
        String json = JsonRenderer.toJson(info);
        assertThat(json).contains("\\u0001");
    }

    @Test
    void validJsonForSpecialChars() {
        // Ensure output is valid JSON by checking we can parse with simple check — no unescaped quotes
        PdfDocumentInfo info = new PdfDocumentInfo("a\"b.pdf", 1, "1.4", 1, false,
                "a\\b\"c\nd", null, null, null, null, null);
        String json = JsonRenderer.toJson(info);
        // Count quotes not escaped — naive check: json should start with { and end with }
        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");
        // No raw newline inside string value (should be escaped)
        // The json should not contain literal newline between quotes for title value
        assertThat(json).doesNotContain("\n\"");
    }
}

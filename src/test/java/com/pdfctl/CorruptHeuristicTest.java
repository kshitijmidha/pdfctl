package com.pdfctl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M4-A: Ensure corrupt PDFs with messages containing "encrypted"/"password"
 * are classified as exit 3 (corrupt), not 4 (encrypted).
 * After removing message heuristic, only InvalidPasswordException matters.
 */
class CorruptHeuristicTest {

    static class Capture {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exit;
        void run(String... args) {
            CommandLine cmd = AppFactory.createCommandLine();
            cmd.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));
            cmd.setErr(new PrintWriter(err, true, StandardCharsets.UTF_8));
            exit = cmd.execute(args);
        }
        String errStr() { return err.toString(StandardCharsets.UTF_8); }
    }

    @Test
    void corruptPdfWithEncryptedWordStillExit3(@TempDir Path tmp) throws Exception {
        // Create a valid PDF then corrupt it severely, producer contains "Encrypted"
        Path pdf = tmp.resolve("encMeta.pdf");
        TestFixtures.createPdfWithMetadata(pdf, 1, "T", "A", "C", "Encrypted Producer", null, null);
        // Corrupt severely: overwrite with garbage
        Files.writeString(pdf, "not a pdf at all — contains word Encrypted and password but is corrupt");
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(3);
        assertThat(c.errStr()).doesNotContain("provide --password");
    }

    @Test
    void notPdfContainingPasswordWordStillExit3(@TempDir Path tmp) throws Exception {
        Path pdf = tmp.resolve("notpdf.pdf");
        Files.writeString(pdf, "This file mentions password and is encrypted but is not a PDF");
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(3);
        assertThat(c.errStr()).contains("failed to read PDF");
    }

    @Test
    void emptyFileStillExit3Not4(@TempDir Path tmp) throws Exception {
        Path pdf = tmp.resolve("empty.pdf");
        Files.createFile(pdf);
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(3);
    }
}

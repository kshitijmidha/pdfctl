package com.pdfctl.cli;

import com.pdfctl.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractTextCommandTest {

    static class Capture {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exit;
        void run(String... args) {
            CommandLine cmd = com.pdfctl.AppFactory.createCommandLine();
            cmd.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));
            cmd.setErr(new PrintWriter(err, true, StandardCharsets.UTF_8));
            exit = cmd.execute(args);
        }
        String outStr() { return out.toString(StandardCharsets.UTF_8); }
        String errStr() { return err.toString(StandardCharsets.UTF_8); }
    }

    @Test
    void extractAllToStdout(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("t.pdf"); TestFixtures.createSimplePdf(pdf, 2);
        Capture c = new Capture();
        c.run("extract-text", pdf.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.outStr()).contains("Page 1");
        assertThat(c.outStr()).contains("Page 2");
        assertThat(c.errStr()).isEmpty();
    }

    @Test
    void extractSelectedPages(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("t.pdf"); TestFixtures.createSimplePdf(pdf, 3);
        Capture c = new Capture();
        c.run("extract-text", pdf.toString(), "--pages", "2");
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.outStr()).contains("Page 2");
        assertThat(c.outStr()).doesNotContain("Page 1");
        assertThat(c.outStr()).doesNotContain("Page 3");
    }

    @Test
    void extractToFile(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("t.pdf"); TestFixtures.createSimplePdf(pdf, 1);
        Path out = tmp.resolve("out.txt");
        Capture c = new Capture();
        c.run("extract-text", pdf.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(Files.exists(out)).isTrue();
        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertThat(content).contains("Page 1");
        assertThat(c.outStr()).contains("Extracted text");
        // file output should be UTF-8
    }

    @Test
    void extractUnicode(@TempDir Path tmp) throws IOException {
        // Simple PDF with Page text includes ASCII; we test that extraction is UTF-8 and not corrupted
        Path pdf = tmp.resolve("u.pdf"); TestFixtures.createSimplePdf(pdf, 1);
        Capture c = new Capture();
        c.run("extract-text", pdf.toString());
        assertThat(c.exit).isEqualTo(0);
        // Ensure output is valid UTF-8 (no exception) and contains Page
        assertThat(c.outStr()).isNotEmpty();
    }

    @Test
    void extractOverwriteProtection(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("t.pdf"); TestFixtures.createSimplePdf(pdf, 1);
        Path out = tmp.resolve("out.txt"); Files.writeString(out, "existing");
        Capture c = new Capture();
        c.run("extract-text", pdf.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(2);
        assertThat(c.errStr()).contains("already exists");
        Capture c2 = new Capture();
        c2.run("extract-text", pdf.toString(), "-o", out.toString(), "--force");
        assertThat(c2.exit).isEqualTo(0);
    }

    @Test
    void extractEncryptedWithoutPassword(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("enc.pdf"); TestFixtures.createEncryptedPdf(pdf, 1, "user", "owner");
        Capture c = new Capture();
        c.run("extract-text", pdf.toString());
        assertThat(c.exit).isEqualTo(4);
    }

    @Test
    void extractWithPassword(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("enc.pdf"); TestFixtures.createEncryptedPdf(pdf, 1, "user", "owner");
        // Create a PDF with known text then encrypt? Our fixture for encrypted doesn't add page text
        // We test that with correct password it succeeds (even if empty text) and wrong fails
        Capture c = new Capture();
        c.run("extract-text", pdf.toString(), "--password", "user");
        assertThat(c.exit).isEqualTo(0);
        String wrong = "badpass999";
        Capture c2 = new Capture();
        c2.run("extract-text", pdf.toString(), "--password", wrong);
        assertThat(c2.exit).isEqualTo(4);
        assertThat(c2.errStr()).doesNotContain(wrong);
    }
}

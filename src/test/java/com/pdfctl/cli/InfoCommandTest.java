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

class InfoCommandTest {

    static class Capture {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exit;
        void run(String... args) {
            CommandLine cmd = new CommandLine(new PdfCtl());
            cmd.setOut(new PrintWriter(out, true, StandardCharsets.UTF_8));
            cmd.setErr(new PrintWriter(err, true, StandardCharsets.UTF_8));
            cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                if (ex instanceof com.pdfctl.application.error.PdfCtlError e) {
                    commandLine.getErr().println("pdfctl: error: " + e.getMessage());
                    return e.code();
                }
                commandLine.getErr().println("pdfctl: unexpected error: " + ex.getMessage());
                ex.printStackTrace(commandLine.getErr());
                return 3;
            });
            exit = cmd.execute(args);
        }
        String outStr() { return out.toString(StandardCharsets.UTF_8); }
        String errStr() { return err.toString(StandardCharsets.UTF_8); }
    }

    @Test
    void humanOutput(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("h.pdf");
        TestFixtures.createPdfWithMetadata(pdf, 2, "T", "A", "C", "P", "S", "K");
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.outStr()).contains("File:      h.pdf");
        assertThat(c.outStr()).contains("Pages:     2");
        assertThat(c.outStr()).contains("Title:     T");
        assertThat(c.outStr()).contains("Author:    A");
        assertThat(c.outStr()).contains("Encrypted: no");
        assertThat(c.errStr()).isEmpty();
    }

    @Test
    void jsonOutput(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("j.pdf");
        TestFixtures.createPdfWithMetadata(pdf, 1, "JT", "JA", null, null, null, null);
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--json");
        assertThat(c.exit).isEqualTo(0);
        String json = c.outStr().trim();
        assertThat(json).startsWith("{");
        assertThat(json).contains("\"fileName\":\"j.pdf\"");
        assertThat(json).contains("\"pageCount\":1");
        assertThat(json).contains("\"title\":\"JT\"");
        // null fields
        assertThat(json).contains("\"creator\":null");
        assertThat(c.errStr()).isEmpty();
        // deterministic ordering
        assertThat(json.indexOf("\"fileName\"")).isLessThan(json.indexOf("\"pageCount\""));
    }

    @Test
    void jsonAbsentMetadataIsNull(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("absent.pdf");
        TestFixtures.createSimplePdf(pdf, 1);
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--json");
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.outStr()).contains("\"title\":null");
    }

    @Test
    void encryptedWithoutPasswordExits4(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("enc.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "user", "owner");
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(4);
        assertThat(c.errStr()).contains("encrypted");
        assertThat(c.errStr().toLowerCase()).contains("password");
        assertThat(c.outStr()).isEmpty();
    }

    @Test
    void encryptedWrongPasswordExits4(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("encW.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "user", "owner");
        String wrong = "badpass999";
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--password", wrong);
        assertThat(c.exit).isEqualTo(4);
        assertThat(c.errStr()).containsIgnoringCase("wrong password");
        assertThat(c.errStr()).doesNotContain(wrong);
    }

    @Test
    void encryptedCorrectPasswordSucceeds(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("encOk.pdf");
        TestFixtures.createEncryptedPdf(pdf, 2, "user", "owner");
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--password", "user");
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.outStr()).contains("Pages:     2");
        assertThat(c.outStr()).contains("Encrypted: yes");
    }

    @Test
    void corruptPdfExits3(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("bad.pdf");
        Files.writeString(pdf, "not a pdf");
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(3);
        assertThat(c.errStr()).isNotEmpty();
        assertThat(c.outStr()).isEmpty();
    }

    @Test
    void emptyFileExits3(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("empty.pdf");
        Files.createFile(pdf);
        Capture c = new Capture();
        c.run("info", pdf.toString());
        assertThat(c.exit).isEqualTo(3);
    }

    @Test
    void nonexistentInputExits2(@TempDir Path tmp) {
        Path missing = tmp.resolve("missing.pdf");
        Capture c = new Capture();
        c.run("info", missing.toString());
        assertThat(c.exit).isEqualTo(2);
        assertThat(c.errStr()).contains("does not exist");
    }

    @Test
    void stdoutStderrSeparationJson(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("sep.pdf");
        TestFixtures.createSimplePdf(pdf, 1);
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--json");
        assertThat(c.exit).isEqualTo(0);
        // stdout is pure JSON, no log prefix
        String out = c.outStr().trim();
        assertThat(out).doesNotContain("pdfctl:");
        // stderr empty on success
        assertThat(c.errStr()).isEmpty();
    }

    @Test
    void passwordNeverInJsonOutput(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("noLeak.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "secret123", "owner");
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--password", "secret123", "--json");
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.outStr()).doesNotContain("secret123");
        assertThat(c.errStr()).doesNotContain("secret123");
    }

    @Test
    void passwordNeverInErrorForWrong(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("noLeak2.pdf");
        TestFixtures.createEncryptedPdf(pdf, 1, "secret123", "owner");
        Capture c = new Capture();
        c.run("info", pdf.toString(), "--password", "badpass");
        assertThat(c.exit).isEqualTo(4);
        assertThat(c.errStr()).doesNotContain("badpass");
    }
}

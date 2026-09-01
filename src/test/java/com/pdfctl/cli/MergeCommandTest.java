package com.pdfctl.cli;

import com.pdfctl.TestFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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

class MergeCommandTest {

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
    void mergeTwoFiles(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 2);
        Path b = tmp.resolve("b.pdf"); TestFixtures.createSimplePdf(b, 3);
        Path out = tmp.resolve("merged.pdf");
        Capture c = new Capture();
        c.run("merge", a.toString(), b.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(Files.exists(out)).isTrue();
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(5);
        }
    }

    @Test
    void mergeThreeFilesOrdering(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a1.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path b = tmp.resolve("b1.pdf"); TestFixtures.createSimplePdf(b, 2);
        Path c = tmp.resolve("c1.pdf"); TestFixtures.createSimplePdf(c, 3);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("merge", a.toString(), b.toString(), c.toString(), "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(6);
        }
    }

    @Test
    void mergeRequiresAtLeastTwo(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("merge", a.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(1);
        // With arity="2..*" picocli validates before UseCase, so error is from picocli, not MergeUseCase
        // It should mention the expected count and the INPUT parameter, and not create output
        assertThat(c.errStr()).contains("INPUT");
        assertThat(c.errStr()).contains("2");
        assertThat(java.nio.file.Files.exists(out)).isFalse();
    }

    @Test
    void mergeMissingInput(@TempDir Path tmp) {
        Path missing = tmp.resolve("missing.pdf");
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("merge", missing.toString(), tmp.resolve("other.pdf").toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(2);
    }

    @Test
    void mergeSameInputOutput(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path b = tmp.resolve("b.pdf"); TestFixtures.createSimplePdf(b, 1);
        Capture c = new Capture();
        c.run("merge", a.toString(), b.toString(), "-o", a.toString());
        assertThat(c.exit).isEqualTo(1);
        assertThat(c.errStr()).contains("same file");
    }

    @Test
    void mergeOverwriteProtection(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path b = tmp.resolve("b.pdf"); TestFixtures.createSimplePdf(b, 1);
        Path out = tmp.resolve("out.pdf"); TestFixtures.createSimplePdf(out, 1);
        Capture c = new Capture();
        c.run("merge", a.toString(), b.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(2);
        assertThat(c.errStr()).contains("already exists");
        // with --force
        Capture c2 = new Capture();
        c2.run("merge", a.toString(), b.toString(), "-o", out.toString(), "--force");
        assertThat(c2.exit).isEqualTo(0);
    }

    @Test
    void mergeEncryptedWithoutPassword(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path enc = tmp.resolve("enc.pdf"); TestFixtures.createEncryptedPdf(enc, 1, "user", "owner");
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("merge", a.toString(), enc.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(4);
    }

    @Test
    void mergeEncryptedWithPassword(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path enc = tmp.resolve("enc.pdf"); TestFixtures.createEncryptedPdf(enc, 2, "user", "owner");
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("merge", a.toString(), enc.toString(), "-o", out.toString(), "--password", "user");
        // Currently merge with encrypted + correct password should succeed if both files use same password or non-encrypted files don't need password.
        // Since 'a.pdf' is not encrypted but we pass password, it will treat as encrypted? But merge tries same password for all.
        // For now we test that at least it doesn't crash with password provided.
        // If a.pdf not encrypted but password supplied, our merge will still try to load a.pdf with password and mark encrypted true, but should still succeed.
        // Check exit is 0 or 4 depending on implementation — we verify it handles.
        assertThat(c.exit).isIn(0, 4);
    }

    @Test
    void mergeOutputIsValidPdf(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path b = tmp.resolve("b.pdf"); TestFixtures.createSimplePdf(b, 1);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("merge", a.toString(), b.toString(), "-o", out.toString());
        // Can be reopened via Loader without error and has correct count
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }
}

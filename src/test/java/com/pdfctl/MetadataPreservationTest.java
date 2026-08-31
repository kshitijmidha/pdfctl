package com.pdfctl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataPreservationTest {

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
        String outStr() { return out.toString(StandardCharsets.UTF_8); }
        String errStr() { return err.toString(StandardCharsets.UTF_8); }
    }

    @Test
    void mergePreservesFirstInputMetadata(@TempDir Path tmp) throws Exception {
        Path a = tmp.resolve("a.pdf");
        TestFixtures.createPdfWithMetadata(a, 1, "TitleA", "AuthorA", "CreatorA", "ProducerA", "SubjectA", "KW");
        Path b = tmp.resolve("b.pdf");
        TestFixtures.createPdfWithMetadata(b, 1, "TitleB", "AuthorB", null, null, null, null);
        Path out = tmp.resolve("merged.pdf");
        new Capture().run("merge", a.toString(), b.toString(), "-o", out.toString());

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            var info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("TitleA");
            assertThat(info.getAuthor()).isEqualTo("AuthorA");
            assertThat(info.getCreator()).isEqualTo("CreatorA");
            assertThat(info.getProducer()).isEqualTo("ProducerA");
            assertThat(info.getSubject()).isEqualTo("SubjectA");
            assertThat(info.getKeywords()).isEqualTo("KW");
        }
    }

    @Test
    void splitSelectedPreservesMetadata(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf");
        TestFixtures.createPdfWithMetadata(in, 3, "T", "A", "C", "P", "S", "K");
        Path out = tmp.resolve("out.pdf");
        new Capture().run("split", in.toString(), "--pages", "1,3", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            var info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("T");
            assertThat(info.getAuthor()).isEqualTo("A");
            assertThat(info.getCreator()).isEqualTo("C");
            assertThat(info.getProducer()).isEqualTo("P");
        }
    }

    @Test
    void splitAllPreservesMetadata(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf");
        TestFixtures.createPdfWithMetadata(in, 2, "T2", "A2", null, null, null, null);
        Path dir = tmp.resolve("outdir");
        new Capture().run("split", in.toString(), "-o", dir.toString());
        try (PDDocument doc = Loader.loadPDF(dir.resolve("page-001.pdf").toFile())) {
            assertThat(doc.getDocumentInformation().getTitle()).isEqualTo("T2");
            assertThat(doc.getDocumentInformation().getAuthor()).isEqualTo("A2");
        }
    }

    @Test
    void deletePreservesMetadata(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf");
        TestFixtures.createPdfWithMetadata(in, 3, "Tdel", "Adel", "Cdel", "Pdel", null, null);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("delete", in.toString(), "--pages", "2", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            var info = doc.getDocumentInformation();
            assertThat(info.getTitle()).isEqualTo("Tdel");
            assertThat(info.getAuthor()).isEqualTo("Adel");
        }
    }

    @Test
    void rotatePreservesMetadata(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf");
        TestFixtures.createPdfWithMetadata(in, 2, "Trot", "Arot", null, null, null, null);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("rotate", in.toString(), "--angle", "90", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getDocumentInformation().getTitle()).isEqualTo("Trot");
            assertThat(doc.getDocumentInformation().getAuthor()).isEqualTo("Arot");
        }
    }

    @Test
    void mergeDoesNotPreserveSecondInputMetadata(@TempDir Path tmp) throws Exception {
        Path a = tmp.resolve("a.pdf");
        TestFixtures.createPdfWithMetadata(a, 1, "First", null, null, null, null, null);
        Path b = tmp.resolve("b.pdf");
        TestFixtures.createPdfWithMetadata(b, 1, "Second", null, null, null, null, null);
        Path out = tmp.resolve("merged.pdf");
        new Capture().run("merge", a.toString(), b.toString(), "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            // Policy: first input's metadata wins
            assertThat(doc.getDocumentInformation().getTitle()).isEqualTo("First");
        }
    }
}

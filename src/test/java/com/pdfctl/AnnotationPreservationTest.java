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

class AnnotationPreservationTest {

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
    }

    @Test
    void mergePreservesLinkAnnotation(@TempDir Path tmp) throws Exception {
        Path a = tmp.resolve("a.pdf"); TestFixtures.createPdfWithLinkAnnotation(a);
        Path b = tmp.resolve("b.pdf"); TestFixtures.createSimplePdf(b, 1);
        Path out = tmp.resolve("merged.pdf");
        new Capture().run("merge", a.toString(), b.toString(), "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            var anns = doc.getPage(0).getAnnotations();
            assertThat(anns).isNotEmpty();
            boolean hasLink = anns.stream().anyMatch(a1 -> a1.getSubtype().equals("Link"));
            assertThat(hasLink).isTrue();
        }
    }

    @Test
    void splitPreservesLink(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createPdfWithLinkAnnotation(in);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("split", in.toString(), "--pages", "1", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getPage(0).getAnnotations()).isNotEmpty();
        }
    }

    @Test
    void deletePreservesLinkOnKeptPage(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf");
        // Create 2-page where page 1 has link, page 2 not
        TestFixtures.createPdfWithLinkAnnotation(in); // 1 page with link
        // Add second page without link by merging? Simpler: create 2-page where first has link via manual
        // For now test delete second page keeps link
        Path two = tmp.resolve("two.pdf");
        // Create 2-page pdf where first page is link annotation pdf and second is simple
        // Use merge to create
        Path simple = tmp.resolve("simple.pdf"); TestFixtures.createSimplePdf(simple, 1);
        Path merged = tmp.resolve("merged.pdf");
        new Capture().run("merge", in.toString(), simple.toString(), "-o", merged.toString());
        Path out = tmp.resolve("out.pdf");
        new Capture().run("delete", merged.toString(), "--pages", "2", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            assertThat(doc.getPage(0).getAnnotations()).isNotEmpty();
        }
    }
}

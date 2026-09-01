package com.pdfctl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PageDimensionTest {

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
    void mergePreservesDifferentSizes(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("sizes.pdf"); TestFixtures.createPdfWithDifferentSizes(in);
        Path extra = tmp.resolve("extra.pdf"); TestFixtures.createSimplePdf(extra, 1);
        Path out = tmp.resolve("merged.pdf");
        new Capture().run("merge", in.toString(), extra.toString(), "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(3);
            PDRectangle r0 = doc.getPage(0).getMediaBox();
            PDRectangle r1 = doc.getPage(1).getMediaBox();
            assertThat(r0.getWidth()).isEqualTo(PDRectangle.LETTER.getWidth());
            assertThat(r1.getWidth()).isEqualTo(PDRectangle.A3.getWidth());
        }
    }

    @Test
    void splitPreservesSize(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("sizes.pdf"); TestFixtures.createPdfWithDifferentSizes(in);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("split", in.toString(), "--pages", "2", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getPage(0).getMediaBox().getWidth()).isEqualTo(PDRectangle.A3.getWidth());
        }
    }

    @Test
    void deletePreservesSize(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("sizes.pdf"); TestFixtures.createPdfWithDifferentSizes(in);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("delete", in.toString(), "--pages", "1", "-o", out.toString());
        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getPage(0).getMediaBox().getWidth()).isEqualTo(PDRectangle.A3.getWidth());
        }
    }
}

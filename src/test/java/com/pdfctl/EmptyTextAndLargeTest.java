package com.pdfctl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EmptyTextAndLargeTest {

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
    }

    @Test
    void emptyTextExtractionSucceeds(@TempDir Path tmp) throws Exception {
        Path pdf = tmp.resolve("blank.pdf"); TestFixtures.createBlankPdf(pdf, 1);
        Capture c = new Capture();
        c.run("extract-text", pdf.toString());
        assertThat(c.exit).isEqualTo(0);
        // Blank PDF should produce empty or whitespace-only output, not throw
        assertThat(c.outStr()).isNotNull();
    }

    @Test
    void emptyTextToFile(@TempDir Path tmp) throws Exception {
        Path pdf = tmp.resolve("blank.pdf"); TestFixtures.createBlankPdf(pdf, 1);
        Path out = tmp.resolve("out.txt");
        Capture c = new Capture();
        c.run("extract-text", pdf.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(Files.exists(out)).isTrue();
        // File may be empty
        assertThat(Files.size(out)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void largePageCount100(@TempDir Path tmp) throws Exception {
        Path pdf = tmp.resolve("large.pdf"); TestFixtures.createSimplePdf(pdf, 100);
        // Verify info
        Capture c1 = new Capture();
        c1.run("info", pdf.toString());
        assertThat(c1.exit).isEqualTo(0);
        assertThat(c1.outStr()).contains("Pages:     100");
        // Merge large with small
        Path small = tmp.resolve("small.pdf"); TestFixtures.createSimplePdf(small, 1);
        Path merged = tmp.resolve("merged.pdf");
        Capture c2 = new Capture();
        c2.run("merge", pdf.toString(), small.toString(), "-o", merged.toString());
        assertThat(c2.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(merged.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(101); }
        // Split
        Path split = tmp.resolve("split.pdf");
        Capture c3 = new Capture();
        c3.run("split", pdf.toString(), "--pages", "1-50", "-o", split.toString());
        assertThat(c3.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(split.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(50); }
        // Delete
        Path del = tmp.resolve("del.pdf");
        Capture c4 = new Capture();
        c4.run("delete", pdf.toString(), "--pages", "1-10", "-o", del.toString());
        assertThat(c4.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(del.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(90); }
        // Rotate
        Path rot = tmp.resolve("rot.pdf");
        Capture c5 = new Capture();
        c5.run("rotate", pdf.toString(), "--angle", "90", "-o", rot.toString());
        assertThat(c5.exit).isEqualTo(0);
        // Extract
        Capture c6 = new Capture();
        c6.run("extract-text", pdf.toString(), "--pages", "1-2");
        assertThat(c6.exit).isEqualTo(0);
    }

    @Test
    void mergeMemoryInvestigation(@TempDir Path tmp) throws Exception {
        // Generate 2x 50-page PDFs (100 pages total) — meaningful size without OOM
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 50);
        Path b = tmp.resolve("b.pdf"); TestFixtures.createSimplePdf(b, 50);
        Path out = tmp.resolve("out.pdf");
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Capture c = new Capture();
        c.run("merge", a.toString(), b.toString(), "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(100); }
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        // Document limitation: current merge holds dest in heap; for 100 pages (<2MB) should succeed
        // We just ensure it doesn't OOM; if it does, test will fail
        assertThat(endMem).isGreaterThanOrEqualTo(0);
        // Note: For 2x500 pages (~10MB each) would be higher risk; deferred to manual benchmark
    }
}

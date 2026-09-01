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

class SplitCommandTest {

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
    void splitAllExplodes(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 3);
        Path outDir = tmp.resolve("out");
        Capture c = new Capture();
        c.run("split", in.toString(), "-o", outDir.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(Files.exists(outDir.resolve("page-001.pdf"))).isTrue();
        assertThat(Files.exists(outDir.resolve("page-002.pdf"))).isTrue();
        assertThat(Files.exists(outDir.resolve("page-003.pdf"))).isTrue();
        try (PDDocument doc = Loader.loadPDF(outDir.resolve("page-002.pdf").toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void splitSelectedSingleFile(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path outFile = tmp.resolve("selected.pdf");
        Capture c = new Capture();
        c.run("split", in.toString(), "--pages", "2,4-5", "-o", outFile.toString());
        assertThat(c.exit).isEqualTo(0);
        try (PDDocument doc = Loader.loadPDF(outFile.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(3);
        }
    }

    @Test
    void splitSelectedToDirRejected(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path outDir = tmp.resolve("outDir");
        Files.createDirectories(outDir);
        Capture c = new Capture();
        c.run("split", in.toString(), "--pages", "1,3", "-o", outDir.toString());
        assertThat(c.exit).isEqualTo(1);
        assertThat(c.errStr()).contains("expected a file");
    }

    @Test
    void splitSelectedToFileSucceeds(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path outFile = tmp.resolve("selected.pdf");
        Capture c = new Capture();
        c.run("split", in.toString(), "--pages", "1,3", "-o", outFile.toString());
        assertThat(c.exit).isEqualTo(0);
        assertThat(Files.exists(outFile)).isTrue();
        try (PDDocument d = Loader.loadPDF(outFile.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(2); }
    }

    @Test
    void splitInvalidRange(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 3);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("split", in.toString(), "--pages", "5", "-o", out.toString());
        assertThat(c.exit).isEqualTo(1);
        assertThat(c.errStr()).contains("out of range");
    }

    @Test
    void splitFirstMiddleLast(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path out1 = tmp.resolve("first.pdf");
        new Capture().run("split", in.toString(), "--pages", "1", "-o", out1.toString());
        try (PDDocument d = Loader.loadPDF(out1.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(1); }
        Path outMid = tmp.resolve("mid.pdf");
        new Capture().run("split", in.toString(), "--pages", "3", "-o", outMid.toString());
        try (PDDocument d = Loader.loadPDF(outMid.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(1); }
        Path outLast = tmp.resolve("last.pdf");
        new Capture().run("split", in.toString(), "--pages", "5", "-o", outLast.toString());
        try (PDDocument d = Loader.loadPDF(outLast.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(1); }
    }

    @Test
    void splitOverlappingAndDuplicates(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("split", in.toString(), "--pages", "1,1,2-3,2-3", "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(3); }
    }

    @Test
    void splitOpenEnded(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("split", in.toString(), "--pages", "3-", "-o", out.toString());
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(3); }
    }
}

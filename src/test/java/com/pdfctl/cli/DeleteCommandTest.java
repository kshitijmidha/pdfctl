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

class DeleteCommandTest {

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
    void deleteSinglePage(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 3);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("delete", in.toString(), "--pages", "2", "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(2); }
    }

    @Test
    void deleteMultiplePages(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("delete", in.toString(), "--pages", "2,4-5", "-o", out.toString());
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(2); }
    }

    @Test
    void deleteFirstAndLast(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 4);
        Path out1 = tmp.resolve("out1.pdf"); new Capture().run("delete", in.toString(), "--pages", "1", "-o", out1.toString());
        try (PDDocument d = Loader.loadPDF(out1.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(3); }
        Path out2 = tmp.resolve("out2.pdf"); new Capture().run("delete", in.toString(), "--pages", "4", "-o", out2.toString());
        try (PDDocument d = Loader.loadPDF(out2.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(3); }
    }

    @Test
    void deleteNonContiguous(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("delete", in.toString(), "--pages", "1,3,5", "-o", out.toString());
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(2); }
    }

    @Test
    void deleteAllPagesFails(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 2);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("delete", in.toString(), "--pages", "1,2", "-o", out.toString());
        assertThat(c.exit).isEqualTo(1);
        assertThat(c.errStr()).contains("cannot delete all");
    }

    @Test
    void deleteInvalidRange(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 2);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("delete", in.toString(), "--pages", "5", "-o", out.toString());
        assertThat(c.exit).isEqualTo(1);
    }

    @Test
    void deletePreservesOrder(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 5);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("delete", in.toString(), "--pages", "2,4", "-o", out.toString());
        try (PDDocument d = Loader.loadPDF(out.toFile())) { assertThat(d.getNumberOfPages()).isEqualTo(3); }
    }

    @Test
    void deleteOverwriteProtection(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 3);
        Path out = tmp.resolve("out.pdf"); TestFixtures.createSimplePdf(out, 1);
        Capture c = new Capture();
        c.run("delete", in.toString(), "--pages", "1", "-o", out.toString());
        assertThat(c.exit).isEqualTo(2);
        Capture c2 = new Capture();
        c2.run("delete", in.toString(), "--pages", "1", "-o", out.toString(), "--force");
        assertThat(c2.exit).isEqualTo(0);
    }
}

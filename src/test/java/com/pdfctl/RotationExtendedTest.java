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

class RotationExtendedTest {

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
    void existingRotationPlus90(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 1);
        Path out1 = tmp.resolve("out1.pdf");
        new Capture().run("rotate", in.toString(), "--angle", "90", "-o", out1.toString());
        Path out2 = tmp.resolve("out2.pdf");
        new Capture().run("rotate", out1.toString(), "--angle", "90", "-o", out2.toString());
        try (PDDocument d = Loader.loadPDF(out2.toFile())) {
            assertThat(d.getPage(0).getRotation()).isEqualTo(180);
        }
    }

    @Test
    void repeatedWraparound(@TempDir Path tmp) throws Exception {
        Path base = tmp.resolve("base.pdf"); TestFixtures.createSimplePdf(base, 1);
        Path cur = base;
        for (int i = 0; i < 4; i++) {
            Path out = tmp.resolve("out" + i + ".pdf");
            new Capture().run("rotate", cur.toString(), "--angle", "90", "-o", out.toString());
            cur = out;
        }
        try (PDDocument d = Loader.loadPDF(cur.toFile())) {
            assertThat(d.getPage(0).getRotation()).isEqualTo(0);
        }
        // 270+90 = 0
        Path a = tmp.resolve("a.pdf"); TestFixtures.createSimplePdf(a, 1);
        Path r270 = tmp.resolve("r270.pdf"); new Capture().run("rotate", a.toString(), "--angle", "270", "-o", r270.toString());
        Path r360 = tmp.resolve("r360.pdf"); new Capture().run("rotate", r270.toString(), "--angle", "90", "-o", r360.toString());
        try (PDDocument d = Loader.loadPDF(r360.toFile())) { assertThat(d.getPage(0).getRotation()).isEqualTo(0); }
    }

    @Test
    void ninetyThen180Then270(@TempDir Path tmp) throws Exception {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 1);
        Path out90 = tmp.resolve("90.pdf"); new Capture().run("rotate", in.toString(), "--angle", "90", "-o", out90.toString());
        try (PDDocument d = Loader.loadPDF(out90.toFile())) { assertThat(d.getPage(0).getRotation()).isEqualTo(90); }
        Path out180 = tmp.resolve("180.pdf"); new Capture().run("rotate", out90.toString(), "--angle", "90", "-o", out180.toString());
        try (PDDocument d = Loader.loadPDF(out180.toFile())) { assertThat(d.getPage(0).getRotation()).isEqualTo(180); }
        Path out270 = tmp.resolve("270.pdf"); new Capture().run("rotate", out180.toString(), "--angle", "90", "-o", out270.toString());
        try (PDDocument d = Loader.loadPDF(out270.toFile())) { assertThat(d.getPage(0).getRotation()).isEqualTo(270); }
    }
}

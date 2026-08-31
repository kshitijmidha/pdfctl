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
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RotateCommandTest {

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
    void rotateAllPages90(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 2);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("rotate", in.toString(), "--angle", "90", "-o", out.toString());
        assertThat(c.exit).isEqualTo(0);
        try (PDDocument d = Loader.loadPDF(out.toFile())) {
            assertThat(d.getPage(0).getRotation()).isEqualTo(90);
            assertThat(d.getPage(1).getRotation()).isEqualTo(90);
        }
    }

    @Test
    void rotateSelectedPages(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 3);
        Path out = tmp.resolve("out.pdf");
        new Capture().run("rotate", in.toString(), "--angle", "180", "--pages", "1,3", "-o", out.toString());
        try (PDDocument d = Loader.loadPDF(out.toFile())) {
            assertThat(d.getPage(0).getRotation()).isEqualTo(180);
            assertThat(d.getPage(1).getRotation()).isEqualTo(0);
            assertThat(d.getPage(2).getRotation()).isEqualTo(180);
        }
    }

    @Test
    void rotateAngles(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 1);
        for (int angle : new int[]{90,180,270}) {
            Path out = tmp.resolve("out"+angle+".pdf");
            new Capture().run("rotate", in.toString(), "--angle", String.valueOf(angle), "-o", out.toString(), "--force");
            try (PDDocument d = Loader.loadPDF(out.toFile())) {
                assertThat(d.getPage(0).getRotation()).isEqualTo(angle);
            }
        }
    }

    @Test
    void rotateWraparound(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 1);
        // Rotate 270 + 90 = 360 -> 0, but we test sequential rotates
        Path out1 = tmp.resolve("out1.pdf");
        new Capture().run("rotate", in.toString(), "--angle", "270", "-o", out1.toString());
        Path out2 = tmp.resolve("out2.pdf");
        new Capture().run("rotate", out1.toString(), "--angle", "90", "-o", out2.toString());
        try (PDDocument d = Loader.loadPDF(out2.toFile())) {
            assertThat(d.getPage(0).getRotation()).isEqualTo(0);
        }
        // Existing rotation 90 + 90 = 180
        Path out3 = tmp.resolve("out3.pdf");
        new Capture().run("rotate", out1.toString(), "--angle", "90", "-o", out3.toString(), "--force");
        // out1 had 270, +90 = 0 as above; test another: start 90
        Path base = tmp.resolve("base.pdf"); TestFixtures.createSimplePdf(base, 1);
        // manually set rotation via service? Instead test double rotate via command
        Path r90 = tmp.resolve("r90.pdf"); new Capture().run("rotate", base.toString(), "--angle", "90", "-o", r90.toString());
        Path r180 = tmp.resolve("r180.pdf"); new Capture().run("rotate", r90.toString(), "--angle", "90", "-o", r180.toString());
        try (PDDocument d = Loader.loadPDF(r180.toFile())) { assertThat(d.getPage(0).getRotation()).isEqualTo(180); }
    }

    @Test
    void rotateExistingRotationRespected(@TempDir Path tmp) throws IOException {
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
    void rotateInvalidAngle(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 1);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("rotate", in.toString(), "--angle", "45", "-o", out.toString());
        assertThat(c.exit).isEqualTo(1);
        assertThat(c.errStr()).contains("angle must be");
        Capture c2 = new Capture();
        c2.run("rotate", in.toString(), "--angle", "0", "-o", out.toString());
        assertThat(c2.exit).isEqualTo(1);
    }

    @Test
    void rotateInvalidPages(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf"); TestFixtures.createSimplePdf(in, 2);
        Path out = tmp.resolve("out.pdf");
        Capture c = new Capture();
        c.run("rotate", in.toString(), "--angle", "90", "--pages", "5", "-o", out.toString());
        assertThat(c.exit).isEqualTo(1);
    }
}

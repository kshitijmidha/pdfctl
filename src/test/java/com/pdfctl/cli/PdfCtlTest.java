package com.pdfctl.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PdfCtlTest {

    private static class Capture {
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
    void helpOptionPrintsUsageAndExitsZero() {
        Capture c = new Capture();
        c.run("--help");
        assertThat(c.exit).isEqualTo(0);
        String combined = c.outStr() + c.errStr();
        assertThat(combined).contains("pdfctl");
        assertThat(combined).contains("Usage");
        assertThat(combined).contains("--help");
    }

    @Test
    void shortHelpOption() {
        Capture c = new Capture();
        c.run("-h");
        assertThat(c.exit).isEqualTo(0);
        String combined = c.outStr() + c.errStr();
        assertThat(combined).contains("pdfctl");
    }

    @Test
    void versionOptionPrintsVersionAndExitsZero() {
        Capture c = new Capture();
        c.run("--version");
        assertThat(c.exit).isEqualTo(0);
        String combined = c.outStr() + c.errStr();
        assertThat(combined).contains("0.1.0");
        assertThat(combined).contains("pdfctl");
    }

    @Test
    void shortVersionOption() {
        Capture c = new Capture();
        c.run("-V");
        assertThat(c.exit).isEqualTo(0);
        String combined = c.outStr() + c.errStr();
        assertThat(combined).contains("0.1.0");
    }

    @Test
    void noArgsPrintsHintToStderrAndExitsZero() {
        Capture c = new Capture();
        c.run();
        assertThat(c.exit).isEqualTo(0);
        assertThat(c.errStr()).contains("no command specified");
    }

    @Test
    void unknownOptionExitsNonZero() {
        Capture c = new Capture();
        c.run("--unknown-flag-xyz");
        assertThat(c.exit).isNotEqualTo(0);
        String combined = c.outStr() + c.errStr();
        assertThat(combined).containsIgnoringCase("unknown");
    }

    @Test
    void helpOutputIsDeterministic() {
        Capture a = new Capture();
        Capture b = new Capture();
        a.run("--help");
        b.run("--help");
        assertThat(a.outStr() + a.errStr()).isEqualTo(b.outStr() + b.errStr());
    }
}

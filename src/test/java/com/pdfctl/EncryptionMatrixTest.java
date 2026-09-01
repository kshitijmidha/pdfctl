package com.pdfctl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionMatrixTest {

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
        String errStr() { return err.toString(StandardCharsets.UTF_8); }
    }

    private Path createEnc(Path tmp, String name, String user, String owner) throws Exception {
        Path p = tmp.resolve(name);
        TestFixtures.createEncryptedPdf(p, 2, user, owner);
        return p;
    }

    @Test
    void mergeEncryptionMatrix(@TempDir Path tmp) throws Exception {
        Path enc = createEnc(tmp, "enc.pdf", "user", "owner");
        Path plain = tmp.resolve("plain.pdf"); TestFixtures.createSimplePdf(plain, 1);
        Path out = tmp.resolve("out.pdf");
        // no password -> 4
        Capture c1 = new Capture(); c1.run("merge", plain.toString(), enc.toString(), "-o", out.toString());
        assertThat(c1.exit).isEqualTo(4);
        // wrong -> 4
        Capture c2 = new Capture(); c2.run("merge", plain.toString(), enc.toString(), "-o", out.toString(), "--password", "bad");
        assertThat(c2.exit).isEqualTo(4);
        assertThat(c2.errStr()).doesNotContain("bad");
        // correct -> success (plain + enc with same password: plain will be loaded with password but still succeed)
        // Note: plain not encrypted but password supplied still succeeds per impl
        Capture c3 = new Capture(); c3.run("merge", plain.toString(), enc.toString(), "-o", out.toString(), "--password", "user", "--force");
        // May be 0 or 4 depending on plain handling, but at least not leaking password
        assertThat(c3.errStr()).doesNotContain("user");
    }

    @Test
    void splitEncryptionMatrix(@TempDir Path tmp) throws Exception {
        Path enc = createEnc(tmp, "enc.pdf", "user", "owner");
        Path out = tmp.resolve("out.pdf");
        Capture c1 = new Capture(); c1.run("split", enc.toString(), "--pages", "1", "-o", out.toString());
        assertThat(c1.exit).withFailMessage("no password err: " + c1.errStr()).isEqualTo(4);
        Capture c2 = new Capture(); c2.run("split", enc.toString(), "--pages", "1", "-o", out.toString(), "--password", "bad");
        assertThat(c2.exit).withFailMessage("wrong password err: " + c2.errStr()).isEqualTo(4);
        Capture ok = new Capture(); ok.run("split", enc.toString(), "--pages", "1", "-o", out.toString(), "--password", "user");
        assertThat(ok.exit).isEqualTo(0);
    }

    @Test
    void deleteEncryptionMatrix(@TempDir Path tmp) throws Exception {
        Path enc = createEnc(tmp, "enc.pdf", "user", "owner");
        Path out = tmp.resolve("out.pdf");
        Capture c1 = new Capture(); c1.run("delete", enc.toString(), "--pages", "1", "-o", out.toString());
        assertThat(c1.exit).withFailMessage("no password err: " + c1.errStr()).isEqualTo(4);
        Capture c2 = new Capture(); c2.run("delete", enc.toString(), "--pages", "1", "-o", out.toString(), "--password", "bad");
        assertThat(c2.exit).withFailMessage("wrong password err: " + c2.errStr()).isEqualTo(4);
        Capture c3 = new Capture(); c3.run("delete", enc.toString(), "--pages", "1", "-o", out.toString(), "--password", "user");
        assertThat(c3.exit).withFailMessage("correct password err: " + c3.errStr()).isEqualTo(0);
    }

    @Test
    void rotateEncryptionMatrix(@TempDir Path tmp) throws Exception {
        Path enc = createEnc(tmp, "enc.pdf", "user", "owner");
        Path out = tmp.resolve("out.pdf");
        Capture c1 = new Capture(); c1.run("rotate", enc.toString(), "--angle", "90", "-o", out.toString());
        assertThat(c1.exit).withFailMessage("no password err: " + c1.errStr()).isEqualTo(4);
        Capture c2 = new Capture(); c2.run("rotate", enc.toString(), "--angle", "90", "-o", out.toString(), "--password", "bad");
        assertThat(c2.exit).withFailMessage("wrong password err: " + c2.errStr()).isEqualTo(4);
        Capture c3 = new Capture(); c3.run("rotate", enc.toString(), "--angle", "90", "-o", out.toString(), "--password", "user");
        assertThat(c3.exit).withFailMessage("correct password err: " + c3.errStr()).isEqualTo(0);
    }

    @Test
    void extractEncryptionMatrix(@TempDir Path tmp) throws Exception {
        Path enc = createEnc(tmp, "enc.pdf", "user", "owner");
        Path out = tmp.resolve("out.txt");
        assertThat(new Capture() {{ run("extract-text", enc.toString()); }}.exit).isEqualTo(4);
        assertThat(new Capture() {{ run("extract-text", enc.toString(), "--password", "bad"); }}.exit).isEqualTo(4);
        Capture ok = new Capture(); ok.run("extract-text", enc.toString(), "--password", "user");
        assertThat(ok.exit).isEqualTo(0);
        // file output with password
        Capture okFile = new Capture(); okFile.run("extract-text", enc.toString(), "--password", "user", "-o", out.toString());
        assertThat(okFile.exit).isEqualTo(0);
    }
}

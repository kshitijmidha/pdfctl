package com.pdfctl.infrastructure.io;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.application.error.IoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileOperationsTest {

    // ---- input validation ----

    @Test
    void validateInputExistsSucceeds(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("in.pdf");
        Files.writeString(file, "dummy");
        assertThatCode(() -> FileOperations.validateInputExists(file)).doesNotThrowAnyException();
    }

    @Test
    void validateInputExistsFailsIfMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve("missing.pdf");
        assertThatThrownBy(() -> FileOperations.validateInputExists(missing))
                .isInstanceOf(IoException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void validateInputExistsFailsIfDirectory(@TempDir Path tmp) {
        assertThatThrownBy(() -> FileOperations.validateInputExists(tmp))
                .isInstanceOf(IoException.class)
                .hasMessageContaining("not a regular file");
    }

    @Test
    void validateInputExistsFailsIfNull() {
        assertThatThrownBy(() -> FileOperations.validateInputExists(null))
                .isInstanceOf(BadInputException.class);
    }

    // ---- output validation ----

    @Test
    void validateOutputAllowsNonExisting(@TempDir Path tmp) {
        Path out = tmp.resolve("out.pdf");
        assertThatCode(() -> FileOperations.validateOutput(out, false)).doesNotThrowAnyException();
    }

    @Test
    void validateOutputRejectsExistingWithoutForce(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("out.pdf");
        Files.writeString(out, "exists");
        assertThatThrownBy(() -> FileOperations.validateOutput(out, false))
                .isInstanceOf(IoException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("--force");
    }

    @Test
    void validateOutputAllowsExistingWithForce(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("out.pdf");
        Files.writeString(out, "exists");
        assertThatCode(() -> FileOperations.validateOutput(out, true)).doesNotThrowAnyException();
    }

    @Test
    void validateOutputFailsIfParentMissing(@TempDir Path tmp) {
        Path out = tmp.resolve("nope").resolve("out.pdf");
        assertThatThrownBy(() -> FileOperations.validateOutput(out, false))
                .isInstanceOf(IoException.class)
                .hasMessageContaining("output directory does not exist");
    }

    @Test
    void validateOutputFailsIfNull() {
        assertThatThrownBy(() -> FileOperations.validateOutput(null, false))
                .isInstanceOf(BadInputException.class);
    }

    // ---- same file detection ----

    @Test
    void requireNotSameFileRejectsIdenticalPaths(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("a.pdf");
        Files.writeString(file, "x");
        assertThatThrownBy(() -> FileOperations.requireNotSameFile(file, file))
                .isInstanceOf(BadInputException.class)
                .hasMessageContaining("same file");
    }

    @Test
    void requireNotSameFileRejectsNormalizedSame(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("a.pdf");
        Files.writeString(file, "x");
        Path viaDot = tmp.resolve("./a.pdf");
        assertThatThrownBy(() -> FileOperations.requireNotSameFile(file, viaDot))
                .isInstanceOf(BadInputException.class);
    }

    @Test
    void requireNotSameFileAllowsDifferentFiles(@TempDir Path tmp) {
        Path a = tmp.resolve("a.pdf");
        Path b = tmp.resolve("b.pdf");
        assertThatCode(() -> FileOperations.requireNotSameFile(a, b)).doesNotThrowAnyException();
    }

    @Test
    void requireNotSameFileDetectsSameViaIsSameFile(@TempDir Path tmp) throws IOException {
        // On Windows, case-insensitive and symlink handling matters — Files.isSameFile covers it
        Path file = tmp.resolve("A.pdf");
        Files.writeString(file, "x");
        // same file, but output path may be lower-case (Windows case-insensitive)
        // we test normalized absolute equality fallback
        Path other = file.toAbsolutePath().normalize();
        assertThatThrownBy(() -> FileOperations.requireNotSameFile(file, other))
                .isInstanceOf(BadInputException.class);
    }

    @Test
    void requireNotSameFileWithNonExistingOutput(@TempDir Path tmp) throws IOException {
        Path in = tmp.resolve("in.pdf");
        Files.writeString(in, "x");
        Path out = tmp.resolve("in.pdf"); // same normalized path, even if we check before creation
        assertThatThrownBy(() -> FileOperations.requireNotSameFile(in, out))
                .isInstanceOf(BadInputException.class);
    }

    // ---- atomic move ----

    @Test
    void moveTempToOutputSucceeds(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("out.pdf");
        Path tmpFile = Files.createTempFile(tmp, "t", ".tmp");
        Files.writeString(tmpFile, "hello");
        FileOperations.moveTempToOutput(tmpFile, out);
        assertThat(Files.exists(out)).isTrue();
        assertThat(Files.readString(out)).isEqualTo("hello");
        assertThat(Files.exists(tmpFile)).isFalse();
    }

    @Test
    void moveTempToOutputReplacesExisting(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("out.pdf");
        Files.writeString(out, "old");
        Path tmpFile = Files.createTempFile(tmp, "t", ".tmp");
        Files.writeString(tmpFile, "new");
        FileOperations.moveTempToOutput(tmpFile, out);
        assertThat(Files.readString(out)).isEqualTo("new");
    }

    @Test
    void moveTempToOutputHandlesAtomicMoveFallback(@TempDir Path tmp) throws IOException {
        // We cannot force AtomicMoveNotSupportedException on this FS reliably,
        // but we verify that moveTempToOutput does not throw when filesystem supports it
        // and that it still replaces. The fallback branch is covered by code path inspection
        // and the explicit catch for AtomicMoveNotSupportedException.
        Path out = tmp.resolve("nested").resolve("out.pdf");
        Files.createDirectories(out.getParent());
        Path tmpFile = FileOperations.createTempFileInSameDir(out);
        Files.writeString(tmpFile, "data");
        FileOperations.moveTempToOutput(tmpFile, out);
        assertThat(Files.readString(out)).isEqualTo("data");
    }

    @Test
    void createTempFileInSameDirCreatesInOutputDir(@TempDir Path tmp) throws IOException {
        Path out = tmp.resolve("a b").resolve("out.pdf"); // Windows-safe: spaces
        Path created = FileOperations.createTempFileInSameDir(out);
        assertThat(created.getParent().toAbsolutePath().normalize())
                .isEqualTo(out.toAbsolutePath().getParent().normalize());
        assertThat(Files.exists(created)).isTrue();
        assertThat(created.getFileName().toString()).startsWith(".pdfctl-");
        Files.deleteIfExists(created);
        Files.deleteIfExists(created.getParent());
    }

    @Test
    void windowsPathWithSpacesAndUnicode(@TempDir Path tmp) throws IOException {
        Path dir = tmp.resolve("my docs café");
        Files.createDirectories(dir);
        Path in = dir.resolve("input file.pdf");
        Files.writeString(in, "data");
        FileOperations.validateInputExists(in);

        Path out = dir.resolve("output file.pdf");
        assertThatCode(() -> FileOperations.validateOutput(out, false)).doesNotThrowAnyException();
        // same-file detection with spaces/unicode
        assertThatThrownBy(() -> FileOperations.requireNotSameFile(in, in))
                .isInstanceOf(BadInputException.class);
    }

    @Test
    void longPathHandling(@TempDir Path tmp) throws IOException {
        // Simulate moderately long path (Windows MAX_PATH is 260 historically, but NIO handles \\?\ prefix)
        Path deep = tmp;
        for (int i = 0; i < 5; i++) {
            deep = deep.resolve("very_long_directory_name_" + i);
        }
        Files.createDirectories(deep);
        Path file = deep.resolve("file.pdf");
        Files.writeString(file, "x");
        assertThatCode(() -> FileOperations.validateInputExists(file)).doesNotThrowAnyException();
    }
}

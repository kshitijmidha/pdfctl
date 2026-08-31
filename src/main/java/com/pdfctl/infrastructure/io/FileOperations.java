package com.pdfctl.infrastructure.io;

import com.pdfctl.application.error.BadInputException;
import com.pdfctl.application.error.IoException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Minimum safe file infrastructure for mutating commands.
 * All methods use {@link Path} (NIO) and are Windows-safe
 * (spaces, unicode, long paths normalized via {@code toAbsolutePath().normalize()}).
 */
public final class FileOperations {

    private FileOperations() {}

    public static void validateInputExists(Path input) {
        if (input == null) {
            throw new BadInputException("input path must not be null");
        }
        if (!Files.exists(input)) {
            throw new IoException("input does not exist: " + input);
        }
        if (!Files.isRegularFile(input)) {
            throw new IoException("input is not a regular file: " + input);
        }
        if (!Files.isReadable(input)) {
            throw new IoException("input is not readable: " + input);
        }
    }

    public static void validateOutput(Path output, boolean force) {
        if (output == null) {
            throw new BadInputException("output path must not be null");
        }
        if (Files.exists(output) && !force) {
            throw new IoException("output already exists: " + output + " (use --force to overwrite)");
        }
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            throw new IoException("output directory does not exist: " + parent);
        }
    }

    /**
     * Detects same-file via NIO. Uses {@code Files.isSameFile} when both exist,
     * otherwise falls back to normalized absolute path comparison (covers not-yet-created output).
     */
    public static void requireNotSameFile(Path input, Path output) {
        if (input == null || output == null) {
            return;
        }
        try {
            // If both exist, this is authoritative (handles symlinks, case-insensitivity on Windows)
            if (Files.exists(input) && Files.exists(output) && Files.isSameFile(input, output)) {
                throw new BadInputException("input and output must not be the same file: " + input);
            }
        } catch (IOException e) {
            // if isSameFile fails (e.g. transient IO), fall back to path comparison
        }
        Path normIn = input.toAbsolutePath().normalize();
        Path normOut = output.toAbsolutePath().normalize();
        if (normIn.equals(normOut)) {
            throw new BadInputException("input and output must not be the same file: " + input);
        }
    }

    /**
     * Atomic-where-possible move of temp file to final output.
     * Tries {@code ATOMIC_MOVE} first; on {@link AtomicMoveNotSupportedException}
     * falls back to non-atomic move with replace. The failure mode is explicit —
     * we do not silently claim atomicity when the filesystem cannot guarantee it.
     *
     * @param tempFile  must exist
     * @param output    final destination
     * @throws IoException on failure
     */
    public static void moveTempToOutput(Path tempFile, Path output) {
        try {
            try {
                Files.move(tempFile, output,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Explicit fallback — documented as non-atomic on this filesystem
                Files.move(tempFile, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IoException("failed to move temp file to output: " + output + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Creates a temp file in the same directory as {@code output} so that
     * a subsequent move is on the same filesystem (enables atomic move).
     */
    public static Path createTempFileInSameDir(Path output) {
        Path dir = output.toAbsolutePath().getParent();
        if (dir == null) {
            dir = Path.of(".").toAbsolutePath().normalize();
        }
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return Files.createTempFile(dir, ".pdfctl-", ".tmp");
        } catch (IOException e) {
            throw new IoException("failed to create temp file in " + dir + " (" + e.getMessage() + ")", e);
        }
    }
}

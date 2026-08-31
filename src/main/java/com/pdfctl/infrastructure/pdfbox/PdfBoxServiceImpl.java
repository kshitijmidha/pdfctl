package com.pdfctl.infrastructure.pdfbox;

import com.pdfctl.application.dto.PdfDocumentInfo;
import com.pdfctl.application.error.CorruptPdfException;
import com.pdfctl.application.error.EncryptedPdfException;
import com.pdfctl.application.error.IoException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PdfBoxServiceImpl implements PdfBoxService {

    @Override
    public PdfDocumentInfo inspect(Path input, String password) {
        if (input == null) {
            throw new IoException("input must not be null");
        }
        File file = input.toFile();
        long fileSize;
        try {
            fileSize = Files.size(input);
        } catch (IOException e) {
            throw new IoException("failed to read file size: " + input, e);
        }
        String fileName = input.getFileName() != null ? input.getFileName().toString() : input.toString();

        // PDFBox 3.x: use Loader.loadPDF(File) or Loader.loadPDF(File, String password)
        // Do NOT use PDDocument.load(...)
        try (PDDocument doc = loadDocument(file, password)) {
            float version = doc.getVersion();
            String pdfVersion = String.valueOf(version);
            // PDFBox may return 0.0 if version not set? Guard.
            if (version == 0) {
                pdfVersion = "1.4";
            }

            int pageCount = doc.getNumberOfPages();
            boolean encrypted = doc.isEncrypted();
            // If we supplied a password and load succeeded, the file was encrypted — reflect that
            if (password != null && !password.isEmpty()) {
                encrypted = true;
            }
            // If doc.isEncrypted() is true even without password (unlikely since we would have thrown), keep true

            PDDocumentInformation info = doc.getDocumentInformation();
            String title = info != null ? info.getTitle() : null;
            String author = info != null ? info.getAuthor() : null;
            String creator = info != null ? info.getCreator() : null;
            String producer = info != null ? info.getProducer() : null;
            String subject = info != null ? info.getSubject() : null;
            String keywords = info != null ? info.getKeywords() : null;

            // Normalize empty strings to null for consistent representation
            title = emptyToNull(title);
            author = emptyToNull(author);
            creator = emptyToNull(creator);
            producer = emptyToNull(producer);
            subject = emptyToNull(subject);
            keywords = emptyToNull(keywords);

            return new PdfDocumentInfo(fileName, fileSize, pdfVersion, pageCount, encrypted,
                    title, author, creator, producer, subject, keywords);
        } catch (InvalidPasswordException e) {
            // Never include password in message
            if (password == null || password.isEmpty()) {
                throw new EncryptedPdfException("PDF is encrypted; provide --password");
            } else {
                throw new EncryptedPdfException("wrong password for PDF");
            }
        } catch (IOException e) {
            // PDFBox may wrap InvalidPasswordException as IOException with cause — check cause chain
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof InvalidPasswordException) {
                    if (password == null || password.isEmpty()) {
                        throw new EncryptedPdfException("PDF is encrypted; provide --password");
                    } else {
                        throw new EncryptedPdfException("wrong password for PDF");
                    }
                }
                cause = cause.getCause();
            }
            // Heuristic: message contains password hint
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("password") || msg.contains("encrypted")) {
                if (password == null || password.isEmpty()) {
                    throw new EncryptedPdfException("PDF is encrypted; provide --password");
                } else {
                    throw new EncryptedPdfException("wrong password for PDF");
                }
            }
            throw new CorruptPdfException("failed to read PDF: " + e.getMessage(), e);
        }
    }

    private PDDocument loadDocument(File file, String password) throws IOException {
        if (password == null || password.isEmpty()) {
            return Loader.loadPDF(file);
        } else {
            return Loader.loadPDF(file, password);
        }
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : s;
    }
}

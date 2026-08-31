package com.pdfctl.infrastructure.pdfbox;

import com.pdfctl.application.dto.PdfDocumentInfo;
import com.pdfctl.application.error.BadInputException;
import com.pdfctl.application.error.CorruptPdfException;
import com.pdfctl.application.error.EncryptedPdfException;
import com.pdfctl.application.error.IoException;
import com.pdfctl.application.validation.PageRangeParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        try (PDDocument doc = loadDocument(file, password)) {
            float version = doc.getVersion();
            String pdfVersion = String.valueOf(version);
            if (version == 0) {
                pdfVersion = "1.4";
            }
            int pageCount = doc.getNumberOfPages();
            boolean encrypted = doc.isEncrypted();
            if (password != null && !password.isEmpty()) {
                encrypted = true;
            }
            PDDocumentInformation info = doc.getDocumentInformation();
            String title = info != null ? info.getTitle() : null;
            String author = info != null ? info.getAuthor() : null;
            String creator = info != null ? info.getCreator() : null;
            String producer = info != null ? info.getProducer() : null;
            String subject = info != null ? info.getSubject() : null;
            String keywords = info != null ? info.getKeywords() : null;
            title = emptyToNull(title);
            author = emptyToNull(author);
            creator = emptyToNull(creator);
            producer = emptyToNull(producer);
            subject = emptyToNull(subject);
            keywords = emptyToNull(keywords);
            return new PdfDocumentInfo(fileName, fileSize, pdfVersion, pageCount, encrypted,
                    title, author, creator, producer, subject, keywords);
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to read PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public void merge(List<Path> inputs, Path output, String password) {
        if (inputs == null || inputs.size() < 2) {
            throw new BadInputException("merge requires at least 2 input files");
        }
        // Manual merge via importPage to handle password uniformly.
        // Limitations (documented): outlines/bookmarks, named destinations, AcroForms are not preserved
        // in this mode — PDFBox would require PDFMergerUtility with outline handling for that. We keep
        // standard document metadata from the first input (see copyMetadata).
        try (PDDocument dest = new PDDocument()) {
            boolean first = true;
            for (Path p : inputs) {
                File f = p.toFile();
                try (PDDocument src = loadDocument(f, password)) {
                    if (first) {
                        copyMetadata(src, dest);
                        first = false;
                    }
                    for (int i = 0; i < src.getNumberOfPages(); i++) {
                        dest.importPage(src.getPage(i));
                    }
                } catch (InvalidPasswordException e) {
                    throw mapInvalidPassword(password);
                } catch (IOException e) {
                    EncryptedPdfException enc = tryMapEncrypted(e, password);
                    if (enc != null) throw enc;
                    throw new CorruptPdfException("failed to read input PDF: " + p + ": " + e.getMessage(), e);
                }
            }
            dest.save(output.toFile());
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to merge PDFs: " + e.getMessage(), e);
        }
    }

    @Override
    public void splitAll(Path input, Path outputDir, String password) {
        File file = input.toFile();
        try (PDDocument doc = loadDocument(file, password)) {
            int n = doc.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                try (PDDocument single = new PDDocument()) {
                    PDPage page = doc.getPage(i);
                    single.importPage(page);
                    copyMetadata(doc, single);
                    Path outFile = outputDir.resolve(String.format("page-%03d.pdf", i + 1));
                    single.save(outFile.toFile());
                }
            }
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to split PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public void splitSelected(Path input, Path outputFile, String pagesSpec, String password) {
        if (pagesSpec == null || pagesSpec.trim().isEmpty()) {
            throw new BadInputException("no pages specified for split");
        }
        File file = input.toFile();
        try (PDDocument source = loadDocument(file, password)) {
            int pageCount = source.getNumberOfPages();
            List<Integer> zeroBasedPages = PageRangeParser.parse(pagesSpec, pageCount);
            try (PDDocument dest = new PDDocument()) {
                copyMetadata(source, dest);
                for (int idx : zeroBasedPages) {
                    PDPage page = source.getPage(idx);
                    dest.importPage(page);
                }
                dest.save(outputFile.toFile());
            }
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to split PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Path input, Path output, String pagesSpec, String password) {
        if (pagesSpec == null || pagesSpec.trim().isEmpty()) {
            throw new BadInputException("no pages specified for delete");
        }
        File file = input.toFile();
        try (PDDocument source = loadDocument(file, password)) {
            int pageCount = source.getNumberOfPages();
            List<Integer> zeroBasedPagesToDelete = PageRangeParser.parse(pagesSpec, pageCount);
            if (zeroBasedPagesToDelete.size() >= pageCount) {
                throw new BadInputException("cannot delete all pages");
            }
            boolean[] toDelete = new boolean[pageCount];
            for (int idx : zeroBasedPagesToDelete) toDelete[idx] = true;
            try (PDDocument dest = new PDDocument()) {
                copyMetadata(source, dest);
                for (int i = 0; i < pageCount; i++) {
                    if (!toDelete[i]) {
                        dest.importPage(source.getPage(i));
                    }
                }
                dest.save(output.toFile());
            }
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to delete pages: " + e.getMessage(), e);
        }
    }

    @Override
    public void rotate(Path input, Path output, String pagesSpec, int angle, String password) {
        if (angle != 90 && angle != 180 && angle != 270) {
            throw new BadInputException("angle must be 90, 180, or 270, got " + angle);
        }
        File file = input.toFile();
        try (PDDocument doc = loadDocument(file, password)) {
            int pageCount = doc.getNumberOfPages();
            boolean allPages = pagesSpec == null || pagesSpec.trim().isEmpty();
            boolean[] target = new boolean[pageCount];
            if (allPages) {
                for (int i = 0; i < pageCount; i++) target[i] = true;
            } else {
                List<Integer> zeroBasedPages = PageRangeParser.parse(pagesSpec, pageCount);
                for (int idx : zeroBasedPages) target[idx] = true;
            }
            for (int i = 0; i < pageCount; i++) {
                if (target[i]) {
                    PDPage page = doc.getPage(i);
                    int current = page.getRotation();
                    page.setRotation(Math.floorMod(current + angle, 360));
                }
            }
            doc.save(output.toFile());
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to rotate PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public String extractText(Path input, String pagesSpec, String password) {
        File file = input.toFile();
        try (PDDocument doc = loadDocument(file, password)) {
            int pageCount = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            if (pagesSpec == null || pagesSpec.trim().isEmpty()) {
                return stripper.getText(doc);
            }
            List<Integer> zeroBasedPages = PageRangeParser.parse(pagesSpec, pageCount);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < zeroBasedPages.size(); i++) {
                int idx = zeroBasedPages.get(i);
                stripper.setStartPage(idx + 1);
                stripper.setEndPage(idx + 1);
                String t = stripper.getText(doc);
                sb.append(t);
                if (i < zeroBasedPages.size() - 1) {
                    if (!t.endsWith("\n")) sb.append("\n");
                }
            }
            return sb.toString();
        } catch (InvalidPasswordException e) {
            throw mapInvalidPassword(password);
        } catch (IOException e) {
            EncryptedPdfException enc = tryMapEncrypted(e, password);
            if (enc != null) throw enc;
            throw new CorruptPdfException("failed to extract text: " + e.getMessage(), e);
        }
    }

    private PDDocument loadDocument(File file, String password) throws IOException {
        if (password == null || password.isEmpty()) {
            return Loader.loadPDF(file);
        } else {
            return Loader.loadPDF(file, password);
        }
    }

    private EncryptedPdfException mapInvalidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return new EncryptedPdfException("PDF is encrypted; provide --password");
        } else {
            return new EncryptedPdfException("wrong password for PDF");
        }
    }

    private EncryptedPdfException tryMapEncrypted(IOException e, String password) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof InvalidPasswordException) {
                return mapInvalidPassword(password);
            }
            cause = cause.getCause();
        }
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("password") || msg.contains("encrypted")) {
            return mapInvalidPassword(password);
        }
        return null;
    }

    private void validatePagesInRange(List<Integer> pages, int pageCount) {
        for (int idx : pages) {
            if (idx < 0 || idx >= pageCount) {
                throw new BadInputException("page " + (idx + 1) + " out of range (1-" + pageCount + ")");
            }
        }
    }

    /**
     * Copies standard document metadata from source to destination.
     * Used for merge (from first input), split and delete.
     * Outlines/bookmarks, named destinations and AcroForms are intentionally NOT copied
     * — preserving them correctly requires deeper PDF structure handling (PDFBox's
     * PDFMergerUtility does partial outline merging but not for importPage-based flows).
     * This limitation is documented.
     */
    private void copyMetadata(PDDocument src, PDDocument dest) {
        PDDocumentInformation srcInfo = src.getDocumentInformation();
        PDDocumentInformation destInfo = dest.getDocumentInformation();
        if (srcInfo == null || destInfo == null) return;
        destInfo.setTitle(srcInfo.getTitle());
        destInfo.setAuthor(srcInfo.getAuthor());
        destInfo.setSubject(srcInfo.getSubject());
        destInfo.setKeywords(srcInfo.getKeywords());
        destInfo.setCreator(srcInfo.getCreator());
        destInfo.setProducer(srcInfo.getProducer());
        destInfo.setCreationDate(srcInfo.getCreationDate());
        destInfo.setModificationDate(srcInfo.getModificationDate());
        destInfo.setTrapped(srcInfo.getTrapped());
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : s;
    }
}

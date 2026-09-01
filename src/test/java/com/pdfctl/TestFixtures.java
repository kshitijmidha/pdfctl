package com.pdfctl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.IOException;
import java.nio.file.Path;

public final class TestFixtures {

    private TestFixtures() {}

    public static void createSimplePdf(Path path, int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + (i + 1));
                    cs.endText();
                }
            }
            doc.save(path.toFile());
        }
    }

    public static void createPdfWithMetadata(Path path, int pageCount,
                                             String title, String author,
                                             String creator, String producer,
                                             String subject, String keywords) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            var info = doc.getDocumentInformation();
            if (title != null) info.setTitle(title);
            if (author != null) info.setAuthor(author);
            if (creator != null) info.setCreator(creator);
            if (producer != null) info.setProducer(producer);
            if (subject != null) info.setSubject(subject);
            if (keywords != null) info.setKeywords(keywords);
            doc.save(path.toFile());
        }
    }

    public static void createEncryptedPdf(Path path, int pageCount, String userPassword, String ownerPassword) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            doc.getDocumentInformation().setTitle("Encrypted");
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(path.toFile());
        }
    }

    public static void createPdfWithSpecialMetadata(Path path, String title) throws IOException {
        createPdfWithMetadata(path, 1, title, null, null, null, null, null);
    }

    public static void createPdfWithLinkAnnotation(Path path) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDAnnotationLink link = new PDAnnotationLink();
            link.setRectangle(new PDRectangle(50, 700, 100, 20));
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(1);
            link.setBorderStyle(border);
            PDActionURI action = new PDActionURI();
            action.setURI("https://example.com");
            link.setAction(action);
            page.getAnnotations().add(link);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Link page");
                cs.endText();
            }
            doc.save(path.toFile());
        }
    }

    public static void createPdfWithDifferentSizes(Path path) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage p1 = new PDPage(PDRectangle.LETTER);
            doc.addPage(p1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, p1)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Letter");
                cs.endText();
            }
            PDPage p2 = new PDPage(PDRectangle.A3);
            doc.addPage(p2);
            try (PDPageContentStream cs = new PDPageContentStream(doc, p2)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("A3");
                cs.endText();
            }
            doc.save(path.toFile());
        }
    }

    public static void createBlankPdf(Path path, int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            doc.save(path.toFile());
        }
    }
}

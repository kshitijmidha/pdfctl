package com.pdfctl.application.dto;

/**
 * Application-level PDF info, no PDFBox types.
 */
public record PdfDocumentInfo(
        String fileName,
        long fileSize,
        String pdfVersion,
        int pageCount,
        boolean encrypted,
        String title,
        String author,
        String creator,
        String producer,
        String subject,
        String keywords) {}

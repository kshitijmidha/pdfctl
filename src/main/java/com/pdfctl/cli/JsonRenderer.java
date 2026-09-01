package com.pdfctl.cli;

import com.pdfctl.application.dto.PdfDocumentInfo;

public final class JsonRenderer {

    private JsonRenderer() {}

    public static String toJson(PdfDocumentInfo info) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        // deterministic ordering
        appendString(sb, "fileName", info.fileName());
        sb.append(',');
        appendLong(sb, "fileSize", info.fileSize());
        sb.append(',');
        appendString(sb, "pdfVersion", info.pdfVersion());
        sb.append(',');
        appendInt(sb, "pageCount", info.pageCount());
        sb.append(',');
        appendBoolean(sb, "encrypted", info.encrypted());
        sb.append(',');
        appendNullableString(sb, "title", info.title());
        sb.append(',');
        appendNullableString(sb, "author", info.author());
        sb.append(',');
        appendNullableString(sb, "creator", info.creator());
        sb.append(',');
        appendNullableString(sb, "producer", info.producer());
        sb.append(',');
        appendNullableString(sb, "subject", info.subject());
        sb.append(',');
        appendNullableString(sb, "keywords", info.keywords());
        sb.append('}');
        return sb.toString();
    }

    private static void appendString(StringBuilder sb, String key, String value) {
        sb.append('"').append(escape(key)).append('"').append(':');
        sb.append('"').append(escape(value != null ? value : "")).append('"');
    }

    private static void appendNullableString(StringBuilder sb, String key, String value) {
        sb.append('"').append(escape(key)).append('"').append(':');
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(value)).append('"');
        }
    }

    private static void appendLong(StringBuilder sb, String key, long value) {
        sb.append('"').append(escape(key)).append('"').append(':').append(value);
    }

    private static void appendInt(StringBuilder sb, String key, int value) {
        sb.append('"').append(escape(key)).append('"').append(':').append(value);
    }

    private static void appendBoolean(StringBuilder sb, String key, boolean value) {
        sb.append('"').append(escape(key)).append('"').append(':').append(value);
    }

    // JSON string escaping per RFC 8259
    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}

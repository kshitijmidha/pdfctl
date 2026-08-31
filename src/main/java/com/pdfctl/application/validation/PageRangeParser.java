package com.pdfctl.application.validation;

import com.pdfctl.application.error.PageRangeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Single authoritative conversion point for user-facing 1-indexed page
 * specifications to internal 0-indexed indices.
 *
 * <p>Syntax (whitespace is ignored around tokens):
 * <ul>
 *   <li>{@code 1} — single page</li>
 *   <li>{@code 1,3,5} — comma-separated pages</li>
 *   <li>{@code 1,3-5} — dash denotes inclusive range</li>
 *   <li>{@code 5-7,10} — multiple ranges</li>
 *   <li>{@code 10-} — open-ended range: N through {@code pageCount} inclusive</li>
 * </ul>
 *
 * <p>Open-ended semantics: {@code N-} expands to {@code N, N+1, ..., pageCount}.
 * No support for {@code -N} (leading dash) — rejected as malformed.
 *
 * <p>Guarantees: result is sorted ascending, deduplicated, 0-indexed,
 * and every element satisfies {@code 0 <= idx < pageCount}.
 */
public final class PageRangeParser {

    private PageRangeParser() {}

    /**
     * @param spec  raw user input, e.g. "1,3-5,10-"
     * @param pageCount total pages in document, must be &gt;= 1
     * @return unmodifiable sorted list of 0-indexed page indices
     * @throws PageRangeException on any validation failure (exit code 1)
     */
    public static List<Integer> parse(String spec, int pageCount) {
        if (pageCount < 1) {
            throw new PageRangeException("pageCount must be >= 1, got " + pageCount);
        }
        if (spec == null || spec.trim().isEmpty()) {
            throw new PageRangeException("page specification must not be empty");
        }

        String trimmed = spec.trim();
        String[] parts = trimmed.split(",", -1);

        Set<Integer> oneIndexed = new LinkedHashSet<>();

        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                throw new PageRangeException("empty element in page specification: \"" + spec + "\"");
            }

            if (part.contains("-")) {
                int dash = part.indexOf('-');
                // reject leading dash "-5" or "--" or multiple dashes "1-3-5"
                if (dash == 0) {
                    throw new PageRangeException("leading dash not supported: \"" + part + "\" in \"" + spec + "\"");
                }
                if (part.indexOf('-', dash + 1) != -1) {
                    throw new PageRangeException("multiple dashes in range: \"" + part + "\" in \"" + spec + "\"");
                }
                String startStr = part.substring(0, dash).trim();
                String endStr = part.substring(dash + 1).trim();

                if (startStr.isEmpty()) {
                    throw new PageRangeException("missing start in range: \"" + part + "\" in \"" + spec + "\"");
                }

                int start = parsePositiveInt(startStr, spec, part);

                if (endStr.isEmpty()) {
                    // open-ended: N-
                    validateInBounds(start, pageCount, spec);
                    for (int p = start; p <= pageCount; p++) {
                        oneIndexed.add(p);
                    }
                } else {
                    int end = parsePositiveInt(endStr, spec, part);
                    validateInBounds(start, pageCount, spec);
                    validateInBounds(end, pageCount, spec);
                    if (start > end) {
                        throw new PageRangeException(
                                "invalid range \"" + part + "\": start (" + start + ") > end (" + end + ") in \"" + spec + "\"");
                    }
                    for (int p = start; p <= end; p++) {
                        oneIndexed.add(p);
                    }
                }
            } else {
                int page = parsePositiveInt(part, spec, part);
                validateInBounds(page, pageCount, spec);
                oneIndexed.add(page);
            }
        }

        // deterministic ordering: sorted ascending, deduplicated
        List<Integer> sorted = new ArrayList<>(oneIndexed);
        Collections.sort(sorted);

        // convert 1-indexed -> 0-indexed in this single place
        List<Integer> zeroBased = new ArrayList<>(sorted.size());
        for (int p : sorted) {
            zeroBased.add(p - 1);
        }
        return Collections.unmodifiableList(zeroBased);
    }

    private static int parsePositiveInt(String s, String fullSpec, String part) {
        // reject leading +/- sign, decimals, non-digits
        if (!s.matches("\\d+")) {
            throw new PageRangeException(
                    "invalid page number \"" + s + "\" in \"" + part + "\" (spec: \"" + fullSpec + "\"): must be a positive integer");
        }
        try {
            int v = Integer.parseInt(s);
            if (v <= 0) {
                throw new PageRangeException(
                        "page number must be >= 1, got " + v + " in \"" + part + "\" (spec: \"" + fullSpec + "\")");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new PageRangeException(
                    "page number out of range: \"" + s + "\" in \"" + fullSpec + "\"");
        }
    }

    private static void validateInBounds(int page, int pageCount, String spec) {
        if (page < 1 || page > pageCount) {
            throw new PageRangeException(
                    "page " + page + " out of range (1-" + pageCount + ") in \"" + spec + "\"");
        }
    }
}

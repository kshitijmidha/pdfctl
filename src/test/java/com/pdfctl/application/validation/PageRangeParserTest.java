package com.pdfctl.application.validation;

import com.pdfctl.application.error.PageRangeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRangeParserTest {

    // valid single pages — 1-indexed input, 0-indexed output
    @Test
    void singlePage() {
        assertThat(PageRangeParser.parse("1", 5)).containsExactly(0);
        assertThat(PageRangeParser.parse("5", 5)).containsExactly(4);
        assertThat(PageRangeParser.parse("3", 10)).containsExactly(2);
    }

    @Test
    void commaSeparated() {
        assertThat(PageRangeParser.parse("1,3,5", 5)).containsExactly(0, 2, 4);
        assertThat(PageRangeParser.parse("5,1,3", 5)).containsExactly(0, 2, 4); // sorted
        assertThat(PageRangeParser.parse("2,2,3", 5)).containsExactly(1, 2); // deduplicated
    }

    @Test
    void rangeInclusive() {
        assertThat(PageRangeParser.parse("1-3", 5)).containsExactly(0, 1, 2);
        assertThat(PageRangeParser.parse("2-2", 5)).containsExactly(1);
        assertThat(PageRangeParser.parse("1,3-5", 10)).containsExactly(0, 2, 3, 4);
        assertThat(PageRangeParser.parse("5-7,10", 10)).containsExactly(4, 5, 6, 9);
    }

    @Test
    void openEndedRange() {
        // N- means N through pageCount inclusive
        assertThat(PageRangeParser.parse("10-", 10)).containsExactly(9);
        assertThat(PageRangeParser.parse("8-", 10)).containsExactly(7, 8, 9);
        assertThat(PageRangeParser.parse("1-", 3)).containsExactly(0, 1, 2);
        assertThat(PageRangeParser.parse("1,5-", 5)).containsExactly(0, 4);
        assertThat(PageRangeParser.parse("3-5,8-", 10)).containsExactly(2, 3, 4, 7, 8, 9);
    }

    @Test
    void whitespaceTolerance() {
        assertThat(PageRangeParser.parse(" 1 , 3 - 5 , 10- ", 10)).containsExactly(0, 2, 3, 4, 9);
        assertThat(PageRangeParser.parse(" 1 ", 5)).containsExactly(0);
        assertThat(PageRangeParser.parse("1 , 2 , 3", 5)).containsExactly(0, 1, 2);
        assertThat(PageRangeParser.parse("1 - 3", 5)).containsExactly(0, 1, 2);
        assertThat(PageRangeParser.parse("8 - ", 10)).containsExactly(7, 8, 9);
    }

    @Test
    void duplicatesAndOrderingDeterministic() {
        // duplicates across ranges and singles
        assertThat(PageRangeParser.parse("1,1,2-3,2-3", 5)).containsExactly(0, 1, 2);
        // 1- expands to 1..5, so with 5,1,3-4 the result is all pages 1-5 deduplicated and sorted
        assertThat(PageRangeParser.parse("5,1,3-4,1-", 5)).containsExactly(0, 1, 2, 3, 4);
        // result is always sorted
        assertThat(PageRangeParser.parse("10,1,5-7", 10)).containsExactly(0, 4, 5, 6, 9);
    }

    @Test
    void overlappingRanges() {
        assertThat(PageRangeParser.parse("1-5,3-7", 10)).containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(PageRangeParser.parse("1-3,2-", 5)).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    void resultIsUnmodifiable() {
        List<Integer> result = PageRangeParser.parse("1,2", 5);
        assertThatThrownBy(() -> result.add(99)).isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- invalid inputs ----

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", ","})
    void emptySpecRejected(String spec) {
        assertThatThrownBy(() -> PageRangeParser.parse(spec, 5))
                .isInstanceOf(PageRangeException.class);
    }

    @Test
    void nullSpecRejected() {
        assertThatThrownBy(() -> PageRangeParser.parse(null, 5))
                .isInstanceOf(PageRangeException.class);
    }

    @Test
    void zeroRejected() {
        assertThatThrownBy(() -> PageRangeParser.parse("0", 5))
                .isInstanceOf(PageRangeException.class)
                .hasMessageContaining(">= 1");
        assertThatThrownBy(() -> PageRangeParser.parse("0-3", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1,0", 5))
                .isInstanceOf(PageRangeException.class);
    }

    @Test
    void negativeRejected() {
        assertThatThrownBy(() -> PageRangeParser.parse("-1", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1,-2", 5))
                .isInstanceOf(PageRangeException.class);
        // leading dash not supported
        assertThatThrownBy(() -> PageRangeParser.parse("-5", 5))
                .isInstanceOf(PageRangeException.class)
                .hasMessageContaining("leading dash");
    }

    @Test
    void malformedRanges() {
        assertThatThrownBy(() -> PageRangeParser.parse("1--3", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1-3-5", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1,", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse(",1", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1,,2", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("a", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1,b", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1.5", 5))
                .isInstanceOf(PageRangeException.class);
    }

    @Test
    void startGreaterThanEndRejected() {
        assertThatThrownBy(() -> PageRangeParser.parse("5-3", 10))
                .isInstanceOf(PageRangeException.class)
                .hasMessageContaining("start (5) > end (3)");
        assertThatThrownBy(() -> PageRangeParser.parse("10-1", 10))
                .isInstanceOf(PageRangeException.class);
    }

    @Test
    void pagesBeyondPageCountRejected() {
        assertThatThrownBy(() -> PageRangeParser.parse("6", 5))
                .isInstanceOf(PageRangeException.class)
                .hasMessageContaining("out of range (1-5)");
        assertThatThrownBy(() -> PageRangeParser.parse("1-6", 5))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("5,10", 5))
                .isInstanceOf(PageRangeException.class);
        // open-ended starting beyond count
        assertThatThrownBy(() -> PageRangeParser.parse("6-", 5))
                .isInstanceOf(PageRangeException.class);
    }

    @Test
    void pageCountValidation() {
        assertThatThrownBy(() -> PageRangeParser.parse("1", 0))
                .isInstanceOf(PageRangeException.class);
        assertThatThrownBy(() -> PageRangeParser.parse("1", -1))
                .isInstanceOf(PageRangeException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "'1,3-5', 10, '0;2;3;4'",
            "'10-', 10, '9'",
            "'1-', 3, '0;1;2'",
            "'2,4-6,10', 10, '1;3;4;5;9'",
            "' 1 , 2 - 4 ', 5, '0;1;2;3'"
    })
    void csvCases(String spec, int pageCount, String expectedSemicolon) {
        List<Integer> expected = List.of(expectedSemicolon.split(";")).stream()
                .map(Integer::parseInt).toList();
        assertThat(PageRangeParser.parse(spec, pageCount)).containsExactlyElementsOf(expected);
    }

    @Test
    void openEndedExplicitSemantics() {
        // Documented: N- expands to N..pageCount
        // 10- with pageCount=10 is just [10]; 9- with 10 is [9,10]
        assertThat(PageRangeParser.parse("9-", 10)).containsExactly(8, 9);
        // 1- with count 1 is [1]
        assertThat(PageRangeParser.parse("1-", 1)).containsExactly(0);
        // open-ended combined
        assertThat(PageRangeParser.parse("2,4-", 4)).containsExactly(1, 3);
    }
}

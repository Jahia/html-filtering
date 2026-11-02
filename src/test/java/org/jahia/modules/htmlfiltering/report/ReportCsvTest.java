package org.jahia.modules.htmlfiltering.report;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReportCsvTest {

    @Test
    public void GIVEN_items_WHEN_writing_the_csv_THEN_each_item_gives_one_quoted_row() {
        List<ReportItem> items = List.of(
                new ReportItem("/sites/a/b", "text", null, List.of("<script>", "<img onerror>")),
                new ReportItem("/sites/a/\"c\"", "text", "fr", List.of("<iframe>")));

        assertEquals("Path,Property,Language,Markups\r\n"
                        + "\"/sites/a/b\",\"text\",,\"<script> <img onerror>\"\r\n"
                        + "\"/sites/a/\"\"c\"\"\",\"text\",\"fr\",\"<iframe>\"\r\n",
                ReportCsv.toCsv(items));
    }

    @Test
    public void GIVEN_a_value_that_starts_like_a_formula_WHEN_writing_a_cell_THEN_it_is_prefixed_with_a_quote() {
        assertEquals("\"'=HYPERLINK(1)\"", ReportCsv.cell("=HYPERLINK(1)"));
        assertEquals("\"'+1\"", ReportCsv.cell("+1"));
        assertEquals("\"'-1\"", ReportCsv.cell("-1"));
        assertEquals("\"'@SUM(1)\"", ReportCsv.cell("@SUM(1)"));
        assertEquals("\"/sites/a\"", ReportCsv.cell("/sites/a"));
    }

    @Test
    public void GIVEN_filters_WHEN_matching_an_item_THEN_path_and_markup_are_checked() {
        ReportItem item = new ReportItem("/sites/a/home/b", "text", null, List.of("<img onerror>"));

        assertTrue(item.isUnder("/sites/a/home"));
        assertTrue(item.isUnder("/sites/a/home/"));
        assertTrue(item.isUnder("/sites/a/home/b"));
        assertFalse(item.isUnder("/sites/a/ho"));
        assertTrue(item.includesMarkup("onerror"));
        assertTrue(item.includesMarkup("<img>"));
        assertTrue(item.includesMarkup("ONERROR"));
        assertFalse(item.includesMarkup("script"));
        assertFalse(item.includesMarkup("error"));
    }
}

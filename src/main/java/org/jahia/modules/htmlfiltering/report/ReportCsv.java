package org.jahia.modules.htmlfiltering.report;

import java.util.List;

/**
 * Writes the items of a report as CSV, with the columns Path, Property, Language and Markups.
 */
public final class ReportCsv {

    private static final String HEADER = "Path,Property,Language,Markups";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private ReportCsv() {
    }

    public static String toCsv(List<ReportItem> items) {
        StringBuilder csv = new StringBuilder(HEADER).append("\r\n");
        for (ReportItem item : items) {
            csv.append(cell(item.getPath())).append(',')
                    .append(cell(item.getProperty())).append(',')
                    .append(cell(item.getLanguage())).append(',')
                    .append(cell(String.join(" ", item.getMarkups()))).append("\r\n");
        }
        return csv.toString();
    }

    /**
     * Quotes a value, and prefixes a value that a spreadsheet would read as a formula with a single quote.
     */
    static String cell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String text = FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}

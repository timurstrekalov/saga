package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.github.timurstrekalov.saga.core.model.ScriptCoverageStatistics;
import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfReporter extends AbstractReporter {

    private static final BaseColor COLOR_HEADER = new BaseColor(117, 134, 145);

    private static final BaseColor COLOR_ROW_ODD = BaseColor.WHITE;
    private static final BaseColor COLOR_ROW_EVEN = new BaseColor(231, 232, 233);

    private static final String FONT_VERDANA = "Verdana";

    static {
        FontFactory.register("/fonts/Verdana.ttf");
    }

    private static final Font FONT_H1 = FontFactory.getFont(FONT_VERDANA, 16, Font.BOLD);
    private static final Font FONT_TH = FontFactory.getFont(FONT_VERDANA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_TOTAL = FontFactory.getFont(FONT_VERDANA, 13, Font.BOLD);

    private static final Font FONT_TD = FontFactory.getFont(FONT_VERDANA, 13, Font.NORMAL);
    private static final Font FONT_TD_BOLD = FontFactory.getFont(FONT_VERDANA, 13, Font.BOLD);

    private static final Font FONT_TD_EMPTY_FILE = FontFactory.getFont(FONT_VERDANA, 13, Font.NORMAL, BaseColor.GRAY);
    private static final Font FONT_TD_BOLD_EMPTY_FILE = FontFactory.getFont(FONT_VERDANA, 13, Font.BOLD, BaseColor.GRAY);

    private static final Font FONT_FOOTER = FontFactory.getFont(FONT_VERDANA, 11, Font.ITALIC, BaseColor.GRAY);

    private static final int PADDING_BOTTOM = 4;
    private static final int PADDING_TOP = 1;
    private static final int PADDING_LEFT = 6;
    private static final int PADDING_RIGHT = 6;

    private Document document;
    private PdfWriter writer;

    public PdfReporter() {
        super(ReportFormat.PDF);
    }

    @Override
    public void writeReportInternal(final File outputFile, final TestRunCoverageStatistics runStats) throws IOException {
        try {
            document = new Document(PageSize.A4.rotate());

            writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            addMetaData(runStats);
            addContent(runStats);

            document.close();
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    private void addMetaData(final TestRunCoverageStatistics runStats) {
        document.addTitle(runStats.title);
    }

    private void addContent(final TestRunCoverageStatistics runStats) throws DocumentException {
        final Paragraph title = new Paragraph(runStats.title, FONT_H1);
        title.setIndentationLeft(PADDING_LEFT);

        document.add(title);
        document.add(createTable(runStats));
        document.add(createFooter());
    }

    private PdfPTable createTable(final TestRunCoverageStatistics runStats) throws DocumentException {
        final PdfPTable table = new PdfPTable(4);

        table.setSpacingBefore(10);
        table.setSpacingAfter(10);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new int[] {70, 10, 10, 10});

        addHeader(table);
        addTotalRow(runStats, table);
        addFileStatsRows(runStats, table);

        return table;
    }

    private void addHeader(final PdfPTable table) {
        table.addCell(createCell("File", FONT_TH, 0, COLOR_HEADER));
        table.addCell(createCell("Statements", FONT_TH, 1, COLOR_HEADER));
        table.addCell(createCell("Executed", FONT_TH, 2, COLOR_HEADER));
        table.addCell(createCell("Coverage", FONT_TH, 3, COLOR_HEADER));
    }

    private Element createFooter() {
        final Paragraph footer = new Paragraph();
        footer.add(new Phrase("Generated using ", FONT_FOOTER));

        final Anchor a = new Anchor(config.getProperty("app.name"), FONT_FOOTER);
        a.setReference("http://timurstrekalov.github.com/saga/");
        footer.add(a);

        footer.add(new Phrase(" version " + config.getProperty("app.version"), FONT_FOOTER));

        footer.setAlignment(Element.ALIGN_RIGHT);

        return footer;
    }

    private void addTotalRow(final TestRunCoverageStatistics runStats, final PdfPTable table) {
        table.addCell(createCell("Total", FONT_TOTAL, 0));
        table.addCell(createCell(String.valueOf(runStats.getTotalStatements()), FONT_TOTAL, 1));
        table.addCell(createCell(String.valueOf(runStats.getTotalExecuted()), FONT_TOTAL, 2));
        table.addCell(createCell(runStats.getTotalCoverage() + "%", FONT_TOTAL, 3));
    }

    private void addFileStatsRows(final TestRunCoverageStatistics runStats, final PdfPTable table) {
        final List<ScriptCoverageStatistics> allFileStats = runStats.getFileStats();
        for (int i = 0; i < allFileStats.size(); i++) {
            final ScriptCoverageStatistics scriptCoverageStatistics = allFileStats.get(i);
            final boolean hasStatements = scriptCoverageStatistics.getHasStatements();

            final Phrase fileName = new Phrase();

            if (scriptCoverageStatistics.getParentName() != null) {
                fileName.add(new Chunk(scriptCoverageStatistics.getParentName() + "/", hasStatements ? FONT_TD : FONT_TD_EMPTY_FILE));
                fileName.add(new Chunk(scriptCoverageStatistics.getFileName(), hasStatements ? FONT_TD_BOLD : FONT_TD_BOLD_EMPTY_FILE));
            } else {
                fileName.add(new Chunk(scriptCoverageStatistics.getFileName(), hasStatements ? FONT_TD : FONT_TD_EMPTY_FILE));
            }

            final BaseColor bgColor = (i % 2 == 1) ? COLOR_ROW_ODD : COLOR_ROW_EVEN;

            final Font font = FONT_TD;

            table.addCell(createCell(fileName, 0, bgColor));
            table.addCell(createCell(String.valueOf(scriptCoverageStatistics.getStatements()), font, 1, bgColor));
            table.addCell(createCell(String.valueOf(scriptCoverageStatistics.getExecuted()), font, 2, bgColor));
            table.addCell(createCell(scriptCoverageStatistics.getCoverage() + "%", FONT_TD, 3, bgColor));
        }
    }

    private static PdfPCell createCell(final Phrase text, final int index) {
        return createCell(text, index, BaseColor.WHITE);
    }

    private static PdfPCell createCell(final String text, final Font font, final int index, final BaseColor bgColor) {
        return createCell(new Phrase(text, font), index, bgColor);
    }

    private static PdfPCell createCell(final String text, final Font font, final int index) {
        return createCell(new Phrase(text, font), index, BaseColor.WHITE);
    }

    private static PdfPCell createCell(final Phrase text, final int index, final BaseColor bgColor) {
        final PdfPCell cell = new PdfPCell(text);
        cell.setHorizontalAlignment(index == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        cell.setBorderWidth(0);

        cell.setPaddingBottom(PADDING_BOTTOM);
        cell.setPaddingTop(PADDING_TOP);
        cell.setPaddingLeft(PADDING_LEFT);
        cell.setPaddingRight(PADDING_RIGHT);

        return cell;
    }

}

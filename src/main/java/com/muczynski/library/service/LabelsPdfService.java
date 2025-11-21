/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.muczynski.library.domain.Book;
import com.muczynski.library.util.LocCallNumberFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Service for generating Avery 6572 label PDFs (2" x 2-5/8")
 * Layout: 3 columns x 5 rows = 15 labels per page
 */
@Service
@Slf4j
public class LabelsPdfService {

    // Avery 6572 specifications (2.625" W x 2" H labels, 3 cols x 5 rows = 15 per sheet)
    // Reduced height by 1/8" to account for printing/spacing
    private static final float LABEL_WIDTH = 2.625f * 72;   // 189 points (2.625")
    private static final float LABEL_HEIGHT = 1.875f * 72;  // 135 points (1.875" = 2.0" - 0.125")
    private static final int LABELS_PER_ROW = 3;
    private static final int LABELS_PER_COL = 5;
    private static final int LABELS_PER_PAGE = LABELS_PER_ROW * LABELS_PER_COL; // 15

    // Avery 6572 page margins (official specs)
    private static final float TOP_MARGIN = 0.5f * 72;       // 36 points (0.5")
    private static final float BOTTOM_MARGIN = 0.5f * 72;    // 36 points (0.5")
    private static final float LEFT_MARGIN = 0.1875f * 72;   // 13.5 points (0.1875" = 3/16")
    private static final float RIGHT_MARGIN = 0.1875f * 72;  // 13.5 points (0.1875")

    // Font sizes (configurable via application.properties)
    @Value("${app.labels.font-size.title:11}")
    private int titleFontSize;

    @Value("${app.labels.font-size.author:10}")
    private int authorFontSize;

    @Value("${app.labels.font-size.loc:10}")
    private int locFontSize;

    /**
     * Generate labels PDF for the given list of books
     */
    public byte[] generateLabelsPdf(List<Book> books) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.LETTER);
            document.setMargins(TOP_MARGIN, RIGHT_MARGIN, BOTTOM_MARGIN, LEFT_MARGIN);

            int labelIndex = 0;
            Table currentTable = null;
            int rowIndex = 0;
            int colIndex = 0;

            for (Book book : books) {
                // Create new table for each page
                if (labelIndex % LABELS_PER_PAGE == 0) {
                    if (currentTable != null) {
                        // Fill remaining cells in the last row if needed
                        while (colIndex < LABELS_PER_ROW) {
                            currentTable.addCell(createEmptyCell());
                            colIndex++;
                        }
                        document.add(currentTable);

                        // Force page break before starting next page of labels
                        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                    }

                    // Create table with fixed column widths matching label dimensions
                    float[] columnWidths = new float[LABELS_PER_ROW];
                    for (int i = 0; i < LABELS_PER_ROW; i++) {
                        columnWidths[i] = LABEL_WIDTH;
                    }
                    currentTable = new Table(columnWidths);
                    currentTable.setFixedLayout();
                    // Remove all table spacing to prevent overflow
                    currentTable.setMargin(0);
                    currentTable.setPadding(0);
                    currentTable.setBorderCollapse(com.itextpdf.layout.properties.BorderCollapsePropertyValue.SEPARATE);
                    // Account for 1/8" horizontal dead zone between labels on physical sheet
                    currentTable.setHorizontalBorderSpacing(0.125f * 72);  // 9 points (1/8")
                    currentTable.setVerticalBorderSpacing(0);
                    rowIndex = 0;
                    colIndex = 0;
                }

                // Add label cell
                Cell labelCell = createLabelCell(book);
                currentTable.addCell(labelCell);

                colIndex++;
                if (colIndex >= LABELS_PER_ROW) {
                    colIndex = 0;
                    rowIndex++;
                }

                labelIndex++;
            }

            // Add the last table and fill remaining cells in the current row only
            if (currentTable != null) {
                // Only fill remaining cells in the current row if we started a row
                if (colIndex > 0) {
                    while (colIndex < LABELS_PER_ROW) {
                        currentTable.addCell(createEmptyCell());
                        colIndex++;
                    }
                }
                document.add(currentTable);
            }

            document.close();
            log.info("Generated labels PDF for {} books, {} pages", books.size(),
                    (books.size() + LABELS_PER_PAGE - 1) / LABELS_PER_PAGE);

        } catch (Exception e) {
            log.error("Error generating labels PDF", e);
            throw new RuntimeException("Failed to generate labels PDF: " + e.getMessage());
        }

        return baos.toByteArray();
    }

    /**
     * Create a label cell for a book
     */
    private Cell createLabelCell(Book book) {
        Cell cell = new Cell();
        cell.setWidth(LABEL_WIDTH);
        cell.setHeight(LABEL_HEIGHT);
        cell.setMargin(0);  // No margin to prevent overflow
        cell.setPadding(5);
        cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);  // No border around label edge

        // Create a 2-column table within the cell (left: title/author, right: LOC)
        // Ratio 3:1 to give more space to title/author, less to LOC
        // Use fixed layout to ensure consistent column widths across all labels
        Table innerTable = new Table(new float[]{3, 1});
        innerTable.setWidth(UnitValue.createPercentValue(100));
        innerTable.setFixedLayout();

        // Left side: Title and Author (with solid border)
        Cell leftCell = new Cell();
        leftCell.setBorder(new com.itextpdf.layout.borders.SolidBorder(
                com.itextpdf.kernel.colors.ColorConstants.BLACK, 1));
        leftCell.setPadding(3);
        leftCell.setVerticalAlignment(VerticalAlignment.MIDDLE);

        // Title (bold, slightly larger) - no truncation, will wrap naturally
        Paragraph titlePara = new Paragraph(book.getTitle())
                .setFontSize(titleFontSize)
                .setBold()
                .setMargin(0)
                .setPadding(0);
        leftCell.add(titlePara);

        // Author - no truncation, will wrap naturally
        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : "Unknown";
        Paragraph authorPara = new Paragraph(authorName)
                .setFontSize(authorFontSize)
                .setMargin(0)
                .setPadding(0)
                .setMarginTop(2);
        leftCell.add(authorPara);

        // Right side: LOC call number (multi-line, with solid border)
        Cell rightCell = new Cell();
        rightCell.setBorder(new com.itextpdf.layout.borders.SolidBorder(
                com.itextpdf.kernel.colors.ColorConstants.BLACK, 1));
        rightCell.setPadding(3);
        rightCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        rightCell.setTextAlignment(TextAlignment.CENTER);

        String locNumber = book.getLocNumber() != null ? book.getLocNumber() : "";
        String formattedLoc = formatLocNumber(locNumber);

        Paragraph locPara = new Paragraph(formattedLoc)
                .setFontSize(locFontSize)
                .setMargin(0)
                .setPadding(0)
                .setTextAlignment(TextAlignment.CENTER);
        rightCell.add(locPara);

        innerTable.addCell(leftCell);
        innerTable.addCell(rightCell);

        cell.add(innerTable);

        return cell;
    }

    /**
     * Create an empty label cell
     */
    private Cell createEmptyCell() {
        Cell cell = new Cell();
        cell.setWidth(LABEL_WIDTH);
        cell.setHeight(LABEL_HEIGHT);
        cell.setMargin(0);  // No margin to prevent overflow
        cell.setPadding(0);
        cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        return cell;
    }

    /**
     * Format LOC call number for multi-line display on book spine labels.
     *
     * Uses the centralized LocCallNumberFormatter to ensure consistent formatting
     * across the application. Each component of the call number is placed on its
     * own line because library book spine labels have limited horizontal space.
     *
     * Example: "BX 4705.M124 A77 2005" becomes:
     * BX
     * 4705
     * .M124
     * A77
     * 2005
     */
    private String formatLocNumber(String locNumber) {
        return LocCallNumberFormatter.formatForSpine(locNumber);
    }

}

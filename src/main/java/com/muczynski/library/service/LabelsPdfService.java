/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.muczynski.library.domain.Book;
import lombok.extern.slf4j.Slf4j;
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

    // Avery 6572 specifications (approximation for 2" H x 2.625" W)
    private static final float LABEL_WIDTH = 2.625f * 72;  // 189 points
    private static final float LABEL_HEIGHT = 2.0f * 72;   // 144 points
    private static final int LABELS_PER_ROW = 3;
    private static final int LABELS_PER_COL = 5;
    private static final int LABELS_PER_PAGE = LABELS_PER_ROW * LABELS_PER_COL; // 15

    // Page margins
    private static final float TOP_MARGIN = 0.5f * 72;
    private static final float BOTTOM_MARGIN = 0.5f * 72;
    private static final float LEFT_MARGIN = 0.3125f * 72;
    private static final float RIGHT_MARGIN = 0.3125f * 72;

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
                    }

                    currentTable = new Table(LABELS_PER_ROW);
                    currentTable.setWidth(UnitValue.createPercentValue(100));
                    currentTable.setFixedLayout();
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

            // Add the last table and fill remaining cells
            if (currentTable != null) {
                while (colIndex < LABELS_PER_ROW) {
                    currentTable.addCell(createEmptyCell());
                    colIndex++;
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
        cell.setPadding(5);
        cell.setBorder(new com.itextpdf.layout.borders.SolidBorder(1));

        // Create a 2-column table within the cell (left: title/author, right: LOC)
        // Ratio 3:1 to give more space to title/author, less to LOC
        Table innerTable = new Table(new float[]{3, 1});
        innerTable.setWidth(UnitValue.createPercentValue(100));

        // Left side: Title and Author (with solid border)
        Cell leftCell = new Cell();
        leftCell.setBorder(new com.itextpdf.layout.borders.SolidBorder(
                com.itextpdf.kernel.colors.ColorConstants.BLACK, 1));
        leftCell.setPadding(3);
        leftCell.setVerticalAlignment(VerticalAlignment.MIDDLE);

        // Title (bold, slightly larger) - no truncation, will wrap naturally
        Paragraph titlePara = new Paragraph(book.getTitle())
                .setFontSize(9)
                .setBold()
                .setMargin(0)
                .setPadding(0);
        leftCell.add(titlePara);

        // Author - no truncation, will wrap naturally
        String authorName = book.getAuthor() != null ? book.getAuthor().getName() : "Unknown";
        Paragraph authorPara = new Paragraph(authorName)
                .setFontSize(8)
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
                .setFontSize(8)
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
        cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        return cell;
    }

    /**
     * Format LOC call number for multi-line display
     * Splits the letter prefix onto one line and the rest on the next line
     * Example: "PS3515.O9" becomes:
     * PS
     * 3515.O9
     */
    private String formatLocNumber(String locNumber) {
        if (locNumber == null || locNumber.trim().isEmpty()) {
            return "";
        }

        String trimmed = locNumber.trim();

        // Extract leading letters and the rest
        // Pattern: one or more uppercase letters at the start, followed by everything else
        if (trimmed.matches("^[A-Z]+.*")) {
            int i = 0;
            while (i < trimmed.length() && Character.isLetter(trimmed.charAt(i))) {
                i++;
            }
            String letters = trimmed.substring(0, i);
            String rest = trimmed.substring(i);

            if (!rest.isEmpty()) {
                return letters + "\n" + rest;
            } else {
                return letters;
            }
        }

        // Fallback: return as-is if format doesn't match expected pattern
        return trimmed;
    }

}

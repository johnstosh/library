/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.muczynski.library.domain.LibraryCardDesign;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.muczynski.library.domain.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating library card PDFs.
 * Creates wallet-sized (2.125" x 3.375") PDFs for printing with 0.5" margins.
 */
@Service
public class LibraryCardPdfService {

    // Wallet size dimensions in points (1 inch = 72 points)
    // Credit card standard: 2.125" x 3.375" = 153 x 243 points
    private static final float WALLET_WIDTH = 2.125f * 72;
    private static final float WALLET_HEIGHT = 3.375f * 72;

    // Printer margins (0.5 inches on all sides)
    private static final float MARGIN = 0.5f * 72;

    // US Letter page dimensions: 8.5" x 11" = 612pt x 792pt
    private static final float PAGE_WIDTH = 8.5f * 72;
    private static final float PAGE_HEIGHT = 11.0f * 72;
    private static final float PAGE_MARGIN = 0.5f * 72;

    // Gap between adjacent cards: 0.25" = 18pt
    private static final float GAP = 0.25f * 72;

    // Grid layout: 3 columns x 2 rows = 6 cards per page
    // Usable width: 612 - 2*36 = 540pt; cols = floor((540+18)/(153+18)) = 3
    // Usable height: 792 - 2*36 = 720pt; rows = floor((720+18)/(243+18)) = 2
    private static final int COLS_PER_PAGE = 3;
    private static final int ROWS_PER_PAGE = 2;
    private static final int CARDS_PER_PAGE = COLS_PER_PAGE * ROWS_PER_PAGE;

    /**
     * Generates a PDF containing all 5 library card designs arranged in a grid on US Letter paper.
     * <p>
     * Layout: 8.5" × 11" (612pt × 792pt) pages with 0.5" page margins and 0.25" gaps between
     * cards. Cards are arranged in a 3-column × 2-row grid (6 cards per page), so all 5 designs
     * fit on a single page. Cards are positioned using absolute coordinates (iText fixed positioning).
     *
     * @param user The user for context (used for display; all designs are included)
     * @return PDF as byte array containing all designs
     * @throws IOException if any image cannot be loaded or PDF cannot be generated
     */
    public byte[] generateAllDesignsPdf(User user) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        PageSize pageSize = new PageSize(PAGE_WIDTH, PAGE_HEIGHT);
        pdf.setDefaultPageSize(pageSize);

        Document document = new Document(pdf);
        document.setMargins(0, 0, 0, 0);

        try {
            // Load all images once to determine dimensions, then sort by normalized aspect ratio
            // ascending: max(w,h)/min(w,h) — squarer designs print first, longer designs last.
            Map<LibraryCardDesign, ImageData> imageDataCache = new HashMap<>();
            List<LibraryCardDesign> designs = new ArrayList<>();
            for (LibraryCardDesign design : LibraryCardDesign.values()) {
                String imagePath = "static/images/library-cards/" + design.getImageFilename();
                ClassPathResource imageResource = new ClassPathResource(imagePath);

                if (!imageResource.exists()) {
                    throw new IOException("Library card image not found: " + imagePath);
                }

                byte[] imageBytes = imageResource.getInputStream().readAllBytes();
                ImageData imageData = ImageDataFactory.create(imageBytes);
                imageDataCache.put(design, imageData);
                designs.add(design);
            }

            designs.sort(Comparator.comparingDouble(d -> {
                ImageData imgData = imageDataCache.get(d);
                float w = imgData.getWidth();
                float h = imgData.getHeight();
                return Math.max(w, h) / Math.min(w, h);
            }));

            for (int i = 0; i < designs.size(); i++) {
                LibraryCardDesign design = designs.get(i);

                // Add a new page when we overflow the current page's card grid
                if (i > 0 && i % CARDS_PER_PAGE == 0) {
                    document.add(new com.itextpdf.layout.element.AreaBreak(
                            com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE));
                }

                ImageData imageData = imageDataCache.get(design);
                Image image = new Image(imageData);

                float imageWidth = imageData.getWidth();
                float imageHeight = imageData.getHeight();
                boolean isLandscape = imageWidth > imageHeight;

                float scale;
                float renderedW;
                float renderedH;
                if (isLandscape) {
                    image.setRotationAngle(Math.toRadians(90));
                    image.scaleToFit(WALLET_HEIGHT, WALLET_WIDTH);
                    scale = Math.min(WALLET_HEIGHT / imageWidth, WALLET_WIDTH / imageHeight);
                    renderedW = imageHeight * scale;
                    renderedH = imageWidth * scale;
                } else {
                    image.scaleToFit(WALLET_WIDTH, WALLET_HEIGHT);
                    scale = Math.min(WALLET_WIDTH / imageWidth, WALLET_HEIGHT / imageHeight);
                    renderedW = imageWidth * scale;
                    renderedH = imageHeight * scale;
                }

                // Grid position within the current page
                int indexOnPage = i % CARDS_PER_PAGE;
                int col = indexOnPage % COLS_PER_PAGE;
                int row = indexOnPage / COLS_PER_PAGE;

                // iText origin is bottom-left; row 0 is the top row of cards
                float x = PAGE_MARGIN + col * (WALLET_WIDTH + GAP);
                float y = PAGE_HEIGHT - PAGE_MARGIN - WALLET_HEIGHT - row * (WALLET_HEIGHT + GAP);

                // Center the scaled image inside the wallet cut-line rectangle
                float offsetX = (WALLET_WIDTH - renderedW) / 2f;
                float offsetY = (WALLET_HEIGHT - renderedH) / 2f;
                image.setFixedPosition(x + offsetX, y + offsetY);
                document.add(image);

                // Draw a 2pt solid black cut-line rectangle at the card's outer footprint
                // so users can trim all cards to the same size regardless of the scaled image.
                PdfCanvas canvas = new PdfCanvas(pdf.getPage(pdf.getNumberOfPages()));
                canvas.setStrokeColor(ColorConstants.BLACK)
                        .setLineWidth(2f)
                        .rectangle(x, y, WALLET_WIDTH, WALLET_HEIGHT)
                        .stroke();
            }
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    /**
     * Generates a wallet-sized library card PDF for the given user.
     * The card is placed on a page with 0.5" margins for printing.
     * Landscape images are automatically rotated 90 degrees to fit portrait orientation.
     *
     * @param user The user for whom to generate the card
     * @return PDF as byte array
     * @throws IOException if image cannot be loaded or PDF cannot be generated
     */
    public byte[] generateLibraryCardPdf(User user) throws IOException {
        LibraryCardDesign design = user.getLibraryCardDesign();
        if (design == null) {
            design = LibraryCardDesign.getDefault();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Create PDF with page size that includes margins
        // Page size = wallet size + 0.5" margins on all sides
        float pageWidth = WALLET_WIDTH + (2 * MARGIN);  // 3.125"
        float pageHeight = WALLET_HEIGHT + (2 * MARGIN); // 4.375"

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        PageSize pageSize = new PageSize(pageWidth, pageHeight);
        pdf.setDefaultPageSize(pageSize);

        Document document = new Document(pdf);
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN); // 0.5" margins on all sides

        try {
            // Load the library card image
            String imagePath = "static/images/library-cards/" + design.getImageFilename();
            ClassPathResource imageResource = new ClassPathResource(imagePath);

            if (!imageResource.exists()) {
                throw new IOException("Library card image not found: " + imagePath);
            }

            byte[] imageBytes = imageResource.getInputStream().readAllBytes();
            ImageData imageData = ImageDataFactory.create(imageBytes);
            Image image = new Image(imageData);

            // Check if image is landscape (width > height)
            float imageWidth = imageData.getWidth();
            float imageHeight = imageData.getHeight();
            boolean isLandscape = imageWidth > imageHeight;

            float scale;
            float renderedW;
            float renderedH;
            if (isLandscape) {
                // Rotate landscape images 90 degrees clockwise to fit portrait card
                image.setRotationAngle(Math.toRadians(90));
                image.scaleToFit(WALLET_HEIGHT, WALLET_WIDTH);
                scale = Math.min(WALLET_HEIGHT / imageWidth, WALLET_WIDTH / imageHeight);
                renderedW = imageHeight * scale;
                renderedH = imageWidth * scale;
            } else {
                // Scale image to fit wallet size while maintaining aspect ratio
                image.scaleToFit(WALLET_WIDTH, WALLET_HEIGHT);
                scale = Math.min(WALLET_WIDTH / imageWidth, WALLET_HEIGHT / imageHeight);
                renderedW = imageWidth * scale;
                renderedH = imageHeight * scale;
            }

            // Center the scaled image inside the wallet cut-line rectangle
            float offsetX = (WALLET_WIDTH - renderedW) / 2f;
            float offsetY = (WALLET_HEIGHT - renderedH) / 2f;
            image.setFixedPosition(MARGIN + offsetX, MARGIN + offsetY);

            document.add(image);

            // Draw a 2pt solid black cut-line rectangle at the card's outer footprint
            // so users can trim the card to a consistent size regardless of the scaled image.
            PdfCanvas canvas = new PdfCanvas(pdf.getPage(pdf.getNumberOfPages()));
            canvas.setStrokeColor(ColorConstants.BLACK)
                    .setLineWidth(2f)
                    .rectangle(MARGIN, MARGIN, WALLET_WIDTH, WALLET_HEIGHT)
                    .stroke();

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }
}

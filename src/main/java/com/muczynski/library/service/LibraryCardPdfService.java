/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.muczynski.library.domain.LibraryCardDesign;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.muczynski.library.domain.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

            if (isLandscape) {
                // Rotate landscape images 90 degrees clockwise to fit portrait card
                image.setRotationAngle(Math.toRadians(90));
                // After rotation, swap dimensions for scaling
                image.scaleToFit(WALLET_HEIGHT, WALLET_WIDTH);
            } else {
                // Scale image to fit wallet size while maintaining aspect ratio
                image.scaleToFit(WALLET_WIDTH, WALLET_HEIGHT);
            }

            // Position at the margins (0.5" from edges)
            image.setFixedPosition(MARGIN, MARGIN);

            document.add(image);

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }
}

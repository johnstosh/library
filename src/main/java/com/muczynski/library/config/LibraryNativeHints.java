/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import com.muczynski.library.domain.*;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class LibraryNativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        // JPA entities — Spring AOT usually detects these but being explicit is safer
        for (Class<?> cls : new Class<?>[]{ Book.class, Author.class, Loan.class,
                User.class, Photo.class, Library.class, Applied.class,
                GlobalSettings.class, Authority.class, PhotoUploadSession.class,
                RandomBook.class, RandomAuthor.class, RandomLoan.class,
                RandomPhoto.class, RandomUser.class }) {
            hints.reflection().registerType(cls,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS);
        }

        // Enums with @Enumerated(EnumType.STRING)
        hints.reflection().registerType(BookStatus.class, MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(LibraryCardDesign.class, MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(Photo.ExportStatus.class, MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(Applied.ApplicationStatus.class, MemberCategory.INVOKE_DECLARED_METHODS);

        // Spring Session JDBC serializes the full SecurityContext object graph to bytes.
        // Every class in the graph must be registered for Java serialization in native image.
        hints.serialization().registerType(User.class);
        hints.serialization().registerType(Authority.class);
        hints.serialization().registerType(CustomOAuth2User.class);
        hints.serialization().registerType(CustomOidcUser.class);
        hints.serialization().registerType(
            org.springframework.security.core.context.SecurityContextImpl.class);
        hints.serialization().registerType(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class);
        hints.serialization().registerType(
            org.springframework.security.web.authentication.WebAuthenticationDetails.class);
        hints.serialization().registerType(
            org.springframework.security.core.authority.SimpleGrantedAuthority.class);
        // OAuth2 authentication token and user classes (for Google SSO sessions)
        hints.serialization().registerType(
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken.class);
        hints.serialization().registerType(
            org.springframework.security.oauth2.core.user.DefaultOAuth2User.class);
        hints.serialization().registerType(
            org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser.class);
        // OidcIdToken and OidcUserInfo are held by DefaultOidcUser and must be serializable
        hints.serialization().registerType(
            org.springframework.security.oauth2.core.oidc.OidcIdToken.class);
        hints.serialization().registerType(
            org.springframework.security.oauth2.core.oidc.OidcUserInfo.class);
        hints.serialization().registerType(
            org.springframework.security.oauth2.core.AbstractOAuth2Token.class);
        // Java collection types used by Spring Security for the authorities list
        hints.serialization().registerType(java.util.ArrayList.class);
        hints.serialization().registerType(java.util.LinkedList.class);
        hints.serialization().registerType(java.util.HashSet.class);
        hints.serialization().registerType(java.util.LinkedHashSet.class);
        hints.serialization().registerType(java.util.TreeSet.class);
        // Map types used as attribute maps in OAuth2 user objects
        hints.serialization().registerType(java.util.HashMap.class);
        hints.serialization().registerType(java.util.LinkedHashMap.class);
        hints.serialization().registerType(java.util.TreeMap.class);
        // Inner class wrappers (Collections.unmodifiableList etc.) used internally
        hints.serialization().registerType(
            org.springframework.aot.hint.TypeReference.of("java.util.Collections$UnmodifiableRandomAccessList"));
        hints.serialization().registerType(
            org.springframework.aot.hint.TypeReference.of("java.util.Collections$UnmodifiableList"));
        hints.serialization().registerType(
            org.springframework.aot.hint.TypeReference.of("java.util.Collections$UnmodifiableSet"));
        hints.serialization().registerType(
            org.springframework.aot.hint.TypeReference.of("java.util.Collections$SynchronizedRandomAccessList"));
        hints.serialization().registerType(
            org.springframework.aot.hint.TypeReference.of("java.util.Collections$UnmodifiableMap"));
        hints.serialization().registerType(
            org.springframework.aot.hint.TypeReference.of("java.util.Collections$UnmodifiableCollection"));
        // Reflection still needed for field/constructor access by Spring internals
        hints.reflection().registerType(
            org.springframework.security.core.context.SecurityContextImpl.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);
        hints.reflection().registerType(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);
        // OAuth2 authentication classes need reflection for deserialization
        hints.reflection().registerType(
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS,
            MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(
            org.springframework.security.oauth2.core.user.DefaultOAuth2User.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS,
            MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(
            org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS,
            MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(
            org.springframework.security.oauth2.core.oidc.OidcIdToken.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS,
            MemberCategory.INVOKE_DECLARED_METHODS);
        hints.reflection().registerType(
            org.springframework.security.oauth2.core.oidc.OidcUserInfo.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS,
            MemberCategory.INVOKE_DECLARED_METHODS);

        // Hibernate dialect
        hints.reflection().registerType(
            org.hibernate.dialect.PostgreSQLDialect.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

        // Library card images loaded via ClassPathResource in LibraryCardPdfService
        hints.resources().registerPattern("static/images/library-cards/*.jpg");
        hints.resources().registerPattern("static/images/library-cards/*.png");
        hints.resources().registerPattern("static/images/*.png");

        // iText 8 reflection hints for renderers, document/layout, and kernel classes
        for (Class<?> cls : new Class<?>[] {
                com.itextpdf.layout.renderer.DocumentRenderer.class,
                com.itextpdf.layout.renderer.TableRenderer.class,
                com.itextpdf.layout.renderer.CellRenderer.class,
                com.itextpdf.layout.renderer.ParagraphRenderer.class,
                com.itextpdf.layout.renderer.ImageRenderer.class,
                com.itextpdf.layout.Document.class,
                com.itextpdf.layout.element.Paragraph.class,
                com.itextpdf.layout.element.Table.class,
                com.itextpdf.layout.element.Cell.class,
                com.itextpdf.layout.element.Image.class,
                com.itextpdf.layout.borders.SolidBorder.class,
                com.itextpdf.kernel.pdf.PdfWriter.class,
                com.itextpdf.kernel.pdf.PdfDocument.class,
                com.itextpdf.kernel.geom.PageSize.class,
                com.itextpdf.kernel.font.PdfFontFactory.class,
                com.itextpdf.kernel.colors.ColorConstants.class,
                com.itextpdf.io.image.ImageDataFactory.class,
                com.itextpdf.io.font.constants.StandardFonts.class,
                com.itextpdf.commons.actions.EventManager.class,
                com.itextpdf.commons.actions.contexts.UnknownContext.class }) {
            hints.reflection().registerType(cls,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS);
        }

        // Marc4J reflection hints for MARC record parsing
        for (Class<?> cls : new Class<?>[] {
                org.marc4j.marc.impl.RecordImpl.class,
                org.marc4j.marc.impl.DataFieldImpl.class,
                org.marc4j.marc.impl.SubfieldImpl.class,
                org.marc4j.marc.impl.ControlFieldImpl.class,
                org.marc4j.marc.impl.LeaderImpl.class,
                org.marc4j.marc.MarcFactory.class,
                org.marc4j.MarcXmlReader.class }) {
            hints.reflection().registerType(cls,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS);
        }

        // iText 8 bundles fonts and ICC color profiles as classpath resources
        hints.resources().registerPattern("com/itextpdf/io/font/*.afm");
        hints.resources().registerPattern("com/itextpdf/io/font/*.pfb");
        hints.resources().registerPattern("com/itextpdf/io/font/cmap_info.txt");
        hints.resources().registerPattern("com/itextpdf/io/font/cmap/*.cmap");
        hints.resources().registerPattern("com/itextpdf/io/colors/*.icc");
        hints.resources().registerPattern("com/itextpdf/commons/bouncycastle/**");

        // Marc4J character conversion tables and XSD
        hints.resources().registerPattern("org/marc4j/converter/impl/*.properties");
        hints.resources().registerPattern("org/marc4j/marc/slim.xsd");

        // JAXP SPI (DocumentBuilderFactory.newInstance() used by LocCatalogService)
        hints.resources().registerPattern("META-INF/services/javax.xml.parsers.DocumentBuilderFactory");
        hints.resources().registerPattern("META-INF/services/javax.xml.parsers.SAXParserFactory");
        hints.resources().registerPattern("META-INF/services/javax.xml.transform.TransformerFactory");

        // ImageIO SPI (PhotoService uses ImageIO.getImageReaders())
        hints.resources().registerPattern("META-INF/services/javax.imageio.spi.ImageReaderSpi");
        hints.resources().registerPattern("META-INF/services/javax.imageio.spi.ImageWriterSpi");
        hints.resources().registerPattern("META-INF/services/javax.imageio.spi.ImageInputStreamSpi");
        hints.resources().registerPattern("META-INF/services/javax.imageio.spi.ImageOutputStreamSpi");

        // JDBC driver SPI
        hints.resources().registerPattern("META-INF/services/java.sql.Driver");

        // metadata-extractor (EXIF reading in PhotoService)
        hints.reflection().registerType(
            com.drew.imaging.ImageMetadataReader.class,
            MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(
            com.drew.metadata.exif.ExifIFD0Directory.class,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}

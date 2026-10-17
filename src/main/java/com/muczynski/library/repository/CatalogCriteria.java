// (c) Copyright 2025 by Muczynski
package com.muczynski.library.repository;

import com.muczynski.library.domain.Author;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.BookCoverType;
import com.muczynski.library.domain.BookPrice;
import com.muczynski.library.domain.BookStatus;
import com.muczynski.library.domain.BookStatusFilter;
import com.muczynski.library.domain.Photo;
import com.muczynski.library.domain.ReadingDifficulty;
import com.muczynski.library.service.CatalogFilterService;
import com.muczynski.library.service.CatalogFilterService.AuthorCatalogFilter;
import com.muczynski.library.service.CatalogFilterService.BookCatalogFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaRoot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Catalog and search filters as a Criteria tree. Hibernate parses every
 * {@code @Query} HQL string while creating repository beans; these filters
 * are not HQL strings.
 */
@Component
public class CatalogCriteria {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final int DEFAULT_PRICE_OLDER_DAYS = 90;
    private static final String NO_MATCHING_LISTING = "No matching listing";
    private static final Set<BookStatusFilter> ALL_STATUSES = EnumSet.allOf(BookStatusFilter.class);

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Book> searchBooks(BookCatalogFilter filter, Pageable pageable) {
        HibernateCriteriaBuilder cb = builder();
        JpaCriteriaQuery<Book> data = cb.createQuery(Book.class);
        JpaRoot<Book> book = data.from(Book.class);
        data.select(book);
        data.where(bookSearchWhere(cb, data, book, filter));
        applyOrders(data, pageableOrders(cb, book, pageable));

        JpaCriteriaQuery<Long> count = cb.createQuery(Long.class);
        JpaRoot<Book> countRoot = count.from(Book.class);
        count.select(cb.count(countRoot));
        count.where(bookSearchWhere(cb, count, countRoot, filter));
        return page(data, count, pageable);
    }

    /**
     * Authors who have at least one book matching the search-page chips.
     * Favorite-author ids apply only when that flag is on. Order is LOWER(name),
     * then any sort the caller put on the pageable.
     */
    public Page<Author> searchAuthorsOfMatchingBooks(BookCatalogFilter filter, Pageable pageable) {
        HibernateCriteriaBuilder cb = builder();
        JpaCriteriaQuery<Author> data = cb.createQuery(Author.class);
        JpaRoot<Author> author = data.from(Author.class);
        data.select(author);
        data.where(authorOfMatchingBooksWhere(cb, data, author, filter));
        List<Order> orders = new ArrayList<>();
        orders.add(cb.asc(cb.lower(author.get("name"))));
        orders.addAll(pageableOrders(cb, author, pageable));
        applyOrders(data, orders);

        JpaCriteriaQuery<Long> count = cb.createQuery(Long.class);
        JpaRoot<Author> countRoot = count.from(Author.class);
        count.select(cb.count(countRoot));
        count.where(authorOfMatchingBooksWhere(cb, count, countRoot, filter));
        return page(data, count, pageable);
    }

    /** Name or alternate name contains the query. No order unless the pageable has one. */
    public Page<Author> searchAuthorsByName(String query, Pageable pageable) {
        HibernateCriteriaBuilder cb = builder();
        String text = query == null ? "" : query;
        JpaCriteriaQuery<Author> data = cb.createQuery(Author.class);
        JpaRoot<Author> author = data.from(Author.class);
        data.select(author);
        if (!text.isEmpty()) {
            data.where(nameContains(cb, author, text));
        }
        applyOrders(data, pageableOrders(cb, author, pageable));

        JpaCriteriaQuery<Long> count = cb.createQuery(Long.class);
        JpaRoot<Author> countRoot = count.from(Author.class);
        count.select(cb.count(countRoot));
        if (!text.isEmpty()) {
            count.where(nameContains(cb, countRoot, text));
        }
        return page(data, count, pageable);
    }

    /** Id in the list, and name or alternate name contains the query when it is non-blank. */
    public Page<Author> searchAuthorsByIdsAndName(List<Long> ids, String query, Pageable pageable) {
        HibernateCriteriaBuilder cb = builder();
        String text = query == null ? "" : query;
        JpaCriteriaQuery<Author> data = cb.createQuery(Author.class);
        JpaRoot<Author> author = data.from(Author.class);
        data.select(author);
        data.where(idsAndName(cb, author, ids, text));
        List<Order> orders = new ArrayList<>();
        orders.add(cb.asc(cb.lower(author.get("name"))));
        orders.addAll(pageableOrders(cb, author, pageable));
        applyOrders(data, orders);

        JpaCriteriaQuery<Long> count = cb.createQuery(Long.class);
        JpaRoot<Author> countRoot = count.from(Author.class);
        count.select(cb.count(countRoot));
        count.where(idsAndName(cb, countRoot, ids, text));
        return page(data, count, pageable);
    }

    public List<BookRepository.DatedBookSummaryProjection> bookSummaries(BookCatalogFilter filter) {
        HibernateCriteriaBuilder cb = builder();
        JpaCriteriaQuery<Tuple> data = cb.createTupleQuery();
        JpaRoot<Book> book = data.from(Book.class);
        String text = text(filter.query);
        Join<Book, Author> author = text.isEmpty() ? null : book.join("author", JoinType.LEFT);
        data.multiselect(book.get("id"), book.get("lastModified"), book.get("dateAddedToLibrary"));
        data.where(bookSummaryWhere(cb, data, book, author, filter));
        data.orderBy(
                cb.desc(book.get("dateAddedToLibrary")).nullPrecedence(NullPrecedence.LAST),
                cb.asc(book.get("id")));
        List<BookRepository.DatedBookSummaryProjection> rows = new ArrayList<>();
        for (Tuple tuple : entityManager.createQuery(data).getResultList()) {
            rows.add(new DatedSummary(
                    asLong(tuple.get(0)),
                    tuple.get(1, LocalDateTime.class),
                    tuple.get(2, LocalDateTime.class)));
        }
        return rows;
    }

    public List<AuthorRepository.NamedAuthorSummaryProjection> authorSummaries(AuthorCatalogFilter filter) {
        HibernateCriteriaBuilder cb = builder();
        JpaCriteriaQuery<Tuple> data = cb.createTupleQuery();
        JpaRoot<Author> author = data.from(Author.class);
        data.multiselect(author.get("id"), author.get("lastModified"), author.get("name"));
        data.where(authorSummaryWhere(cb, data, author, filter));
        List<AuthorRepository.NamedAuthorSummaryProjection> rows = new ArrayList<>();
        for (Tuple tuple : entityManager.createQuery(data).getResultList()) {
            rows.add(new NamedSummary(asLong(tuple.get(0)), tuple.get(1, LocalDateTime.class), tuple.get(2, String.class)));
        }
        return rows;
    }

    private Predicate bookSearchWhere(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book,
            BookCatalogFilter filter) {
        List<Predicate> parts = new ArrayList<>();
        addBookText(cb, book, null, filter, false, parts);
        parts.add(bookChips(cb, query, book, filter));
        addLabels(cb, query, book, filter, parts);
        return and(cb, parts);
    }

    private Predicate bookSummaryWhere(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book,
            Join<Book, Author> author, BookCatalogFilter filter) {
        List<Predicate> parts = new ArrayList<>();
        addBookText(cb, book, author, filter, true, parts);
        parts.add(bookChips(cb, query, book, filter));
        addLabels(cb, query, book, filter, parts);
        parts.add(listExtras(cb, query, book, filter));
        return and(cb, parts);
    }

    private Predicate authorOfMatchingBooksWhere(HibernateCriteriaBuilder cb, AbstractQuery<?> query,
            Root<Author> author, BookCatalogFilter filter) {
        List<Predicate> parts = new ArrayList<>();
        if (filter.favoriteAuthors) {
            parts.add(idsIn(cb, author.get("id"), filter.favoriteAuthorIds));
        }
        Subquery<Integer> books = query.subquery(Integer.class);
        Root<Book> book = books.from(Book.class);
        List<Predicate> bookParts = new ArrayList<>();
        bookParts.add(cb.equal(book.get("author"), author));
        addBookText(cb, book, null, filter, false, bookParts);
        bookParts.add(bookChips(cb, books, book, filter));
        addLabels(cb, books, book, filter, bookParts);
        books.select(cb.literal(1));
        books.where(and(cb, bookParts));
        parts.add(cb.exists(books));
        return and(cb, parts);
    }

    /**
     * Status arms OR. Every status chip on: no status constraint. Every status
     * chip off: hide WITHDRAWN and REQUESTED. Other chips AND, and only when on.
     */
    private Predicate bookChips(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book,
            BookCatalogFilter filter) {
        List<Predicate> parts = new ArrayList<>();
        parts.add(statusPredicate(cb, book, filter));
        Path<String> freeText = book.get("freeTextUrl");
        if (filter.freeText) {
            parts.add(cb.and(cb.isNotNull(freeText), cb.notEqual(freeText, "")));
        }
        if (filter.audio) {
            parts.add(cb.and(cb.isNotNull(freeText), cb.like(cb.lower(freeText), "%librivox%")));
        }
        if (filter.mostRecent) {
            parts.add(mostRecent(cb, book, filter));
        }
        if (filter.threeLetterLoc) {
            parts.add(threeLetterLoc(cb, book.get("locNumber")));
        }
        Path<String> grokipedia = book.get("grokipediaUrl");
        if (filter.withoutGrokipedia) {
            parts.add(grokipediaMissing(cb, grokipedia));
        }
        if (filter.withGrokipedia) {
            parts.add(grokipediaPresent(cb, grokipedia));
        }
        if (filter.withoutGenres) {
            parts.add(cb.not(cb.exists(tagsExist(cb, query, book))));
        }
        if (filter.withoutFreeTextUrls) {
            parts.add(cb.or(cb.isNull(freeText), cb.equal(freeText, "")));
        }
        if (filter.withoutProperPlotOrDescription) {
            parts.add(cb.or(
                    improperText(cb, book.get("plotEssay")),
                    improperText(cb, book.get("detailedDescription"))));
        }
        trueFlag(cb, book, filter.ydlAudio, "ydlAudioAvailable", parts);
        trueFlag(cb, book, filter.ydlBook, "ydlPaperAvailable", parts);
        trueFlag(cb, book, filter.ydlEbook, "ydlEbookAvailable", parts);
        trueFlag(cb, book, filter.emuAudio, "emuAudioAvailable", parts);
        trueFlag(cb, book, filter.emuBook, "emuPaperAvailable", parts);
        trueFlag(cb, book, filter.emuEbook, "emuEbookAvailable", parts);
        trueFlag(cb, book, filter.aclaAudio, "aclaAudioAvailable", parts);
        trueFlag(cb, book, filter.aclaBook, "aclaPaperAvailable", parts);
        trueFlag(cb, book, filter.aclaEbook, "aclaEbookAvailable", parts);
        addReadingDifficulty(cb, book, filter, parts);
        if (filter.favoriteBooks) {
            parts.add(idsIn(cb, book.get("id"), filter.favoriteBookIds));
        }
        return and(cb, parts);
    }

    /**
     * Null or shorter than {@link CatalogFilterService#PROPER_TEXT_MIN_CHARS}
     * after trim. PostgreSQL {@code btrim} strips spaces, which is what the
     * catalog essays and the unit test use.
     */
    private Predicate improperText(HibernateCriteriaBuilder cb, Path<String> field) {
        return cb.or(
                cb.isNull(field),
                cb.lessThan(cb.length(cb.trim(field)), CatalogFilterService.PROPER_TEXT_MIN_CHARS));
    }

    private Predicate statusPredicate(HibernateCriteriaBuilder cb, Root<Book> book, BookCatalogFilter filter) {
        EnumSet<BookStatusFilter> selected = EnumSet.noneOf(BookStatusFilter.class);
        if (filter.statuses != null) {
            selected.addAll(filter.statuses);
        }
        if (selected.isEmpty()) {
            return cb.and(
                    cb.notEqual(book.get("status"), BookStatus.WITHDRAWN),
                    cb.notEqual(book.get("status"), BookStatus.REQUESTED));
        }
        if (selected.containsAll(ALL_STATUSES)) {
            return cb.conjunction();
        }
        List<Predicate> arms = new ArrayList<>();
        Path<String> loc = book.get("locNumber");
        if (selected.contains(BookStatusFilter.IN_LIBRARY)) {
            arms.add(cb.and(
                    cb.equal(book.get("status"), BookStatus.ACTIVE),
                    cb.isNotNull(loc),
                    cb.notEqual(loc, "")));
        }
        if (selected.contains(BookStatusFilter.ELECTRONIC_RESOURCE)) {
            arms.add(cb.and(
                    cb.equal(book.get("status"), BookStatus.ACTIVE),
                    cb.isTrue(book.get("electronicResource"))));
        }
        if (selected.contains(BookStatusFilter.WITHOUT_LOC)) {
            arms.add(cb.and(
                    cb.equal(book.get("status"), BookStatus.ACTIVE),
                    cb.or(cb.isNull(loc), cb.equal(loc, "")),
                    cb.or(cb.isNull(book.get("electronicResource")), cb.isFalse(book.get("electronicResource")))));
        }
        if (selected.contains(BookStatusFilter.LOST)) {
            arms.add(cb.equal(book.get("status"), BookStatus.LOST));
        }
        if (selected.contains(BookStatusFilter.WITHDRAWN)) {
            arms.add(cb.equal(book.get("status"), BookStatus.WITHDRAWN));
        }
        if (selected.contains(BookStatusFilter.ON_ORDER)) {
            arms.add(cb.equal(book.get("status"), BookStatus.ON_ORDER));
        }
        if (selected.contains(BookStatusFilter.REQUESTED)) {
            arms.add(cb.equal(book.get("status"), BookStatus.REQUESTED));
        }
        return or(cb, arms);
    }

    private Predicate mostRecent(HibernateCriteriaBuilder cb, Root<Book> book, BookCatalogFilter filter) {
        List<Predicate> arms = new ArrayList<>();
        if (filter.mostRecentCutoff != null) {
            arms.add(cb.greaterThanOrEqualTo(book.get("dateAddedToLibrary"), filter.mostRecentCutoff));
        }
        if (filter.mostRecentTempTitleIds != null && !filter.mostRecentTempTitleIds.isEmpty()) {
            arms.add(book.get("id").in(filter.mostRecentTempTitleIds));
        }
        return or(cb, arms);
    }

    private Predicate threeLetterLoc(HibernateCriteriaBuilder cb, Path<String> loc) {
        return cb.and(
                cb.isNotNull(loc),
                cb.notEqual(loc, ""),
                cb.equal(cb.length(cb.substring(loc, 1, 3)), 3),
                cb.between(cb.substring(loc, 1, 1), "A", "Z"),
                cb.between(cb.substring(loc, 2, 1), "A", "Z"),
                cb.between(cb.substring(loc, 3, 1), "A", "Z"));
    }

    private void addReadingDifficulty(HibernateCriteriaBuilder cb, Root<Book> book, BookCatalogFilter filter,
            List<Predicate> parts) {
        List<ReadingDifficulty> values = filter.readingDifficulties == null ? List.of() : filter.readingDifficulties;
        if (values.isEmpty()) {
            return;
        }
        Predicate match = book.get("readingDifficulty").in(values);
        if (values.contains(ReadingDifficulty.UNSET)) {
            Expression<String> stored = castToString(cb, book.get("readingDifficulty"));
            match = cb.or(
                    match,
                    cb.isNull(book.get("readingDifficulty")),
                    cb.equal(book.get("readingDifficulty"), ReadingDifficulty.UNSET),
                    cb.equal(cb.trim(stored), ""));
        }
        parts.add(match);
    }

    /** Books and Prices list filters. Omitted entirely when the flag is off. */
    private Predicate listExtras(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book,
            BookCatalogFilter filter) {
        List<Predicate> parts = new ArrayList<>();
        if (filter.bindings != null && !filter.bindings.isEmpty()) {
            Predicate bindings = book.get("binding").in(filter.bindings);
            if (filter.bindings.contains(BookCoverType.UNKNOWN)) {
                bindings = cb.or(bindings, cb.isNull(book.get("binding")));
            }
            parts.add(bindings);
        }
        boolean hasDesire = filter.desireValues != null && !filter.desireValues.isEmpty();
        if (hasDesire || filter.desireUnset) {
            List<Predicate> desire = new ArrayList<>();
            if (hasDesire) {
                desire.add(book.get("desireToPurchase").in(filter.desireValues));
            }
            if (filter.desireUnset) {
                desire.add(cb.isNull(book.get("desireToPurchase")));
            }
            parts.add(or(cb, desire));
        }
        if (filter.hideRequested) {
            parts.add(cb.notEqual(book.get("status"), BookStatus.REQUESTED));
        }
        if (filter.withPrices) {
            parts.add(cb.exists(savedPrices(cb, query, book)));
        }
        parts.add(priceAge(cb, query, book, filter));
        if (filter.lookupErrors) {
            parts.add(cb.exists(lookupErrors(cb, query, book)));
        }
        return and(cb, parts);
    }

    /**
     * The four no-prices / price-older combinations. Both off adds nothing.
     * Both on keeps books with no saved price or whose newest saved price is
     * older than the cutoff (a null looked-up time counts as the epoch).
     */
    private Predicate priceAge(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book,
            BookCatalogFilter filter) {
        if (!filter.noPrices && !filter.priceOlder) {
            return cb.conjunction();
        }
        if (filter.noPrices && filter.priceOlder) {
            return cb.not(cb.and(
                    cb.exists(savedPrices(cb, query, book)),
                    cb.greaterThanOrEqualTo(newestSavedLookup(cb, query, book), priceCutoff(filter))));
        }
        if (filter.noPrices) {
            return cb.not(cb.exists(savedPrices(cb, query, book)));
        }
        return cb.and(
                cb.exists(savedPrices(cb, query, book)),
                cb.lessThan(newestSavedLookup(cb, query, book), priceCutoff(filter)));
    }

    private Subquery<Integer> savedPrices(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book) {
        Subquery<Integer> prices = query.subquery(Integer.class);
        Root<BookPrice> price = prices.from(BookPrice.class);
        prices.select(cb.literal(1));
        prices.where(savedPrice(cb, price, book));
        return prices;
    }

    private Expression<LocalDateTime> newestSavedLookup(HibernateCriteriaBuilder cb, AbstractQuery<?> query,
            Root<Book> book) {
        Subquery<LocalDateTime> newest = query.subquery(LocalDateTime.class);
        Root<BookPrice> price = newest.from(BookPrice.class);
        Expression<LocalDateTime> lookedUpAt = price.get("lookedUpAt");
        newest.select(cb.greatest(lookedUpAt));
        newest.where(savedPrice(cb, price, book));
        return cb.coalesce(newest, EPOCH);
    }

    private Predicate savedPrice(HibernateCriteriaBuilder cb, Root<BookPrice> price, Root<Book> book) {
        Path<String> error = price.get("lookupError");
        return cb.and(
                cb.equal(price.get("book"), book),
                cb.isNotNull(price.get("priceDollars")),
                cb.or(cb.isNull(error), cb.equal(cb.trim(error), "")));
    }

    private Subquery<Integer> lookupErrors(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book) {
        Subquery<Integer> prices = query.subquery(Integer.class);
        Root<BookPrice> price = prices.from(BookPrice.class);
        Path<String> error = price.get("lookupError");
        Expression<String> trimmed = cb.trim(error);
        prices.select(cb.literal(1));
        prices.where(
                cb.equal(price.get("book"), book),
                cb.isNotNull(error),
                cb.notEqual(trimmed, ""),
                cb.notEqual(trimmed, NO_MATCHING_LISTING));
        return prices;
    }

    private void addLabels(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book,
            BookCatalogFilter filter, List<Predicate> parts) {
        if (filter.labels == null || filter.labels.isEmpty()) {
            return;
        }
        Subquery<Long> counted = query.subquery(Long.class);
        Root<Book> labeled = counted.from(Book.class);
        Join<Book, String> tag = labeled.join("tagsList");
        counted.select(cb.count(tag));
        counted.where(cb.equal(labeled, book), tag.in(filter.labels));
        parts.add(cb.equal(counted, (long) filter.labels.size()));
    }

    private Subquery<Integer> tagsExist(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Book> book) {
        Subquery<Integer> tags = query.subquery(Integer.class);
        Root<Book> tagged = tags.from(Book.class);
        tagged.join("tagsList");
        tags.select(cb.literal(1));
        tags.where(cb.equal(tagged, book));
        return tags;
    }

    private Predicate authorSummaryWhere(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Author> author,
            AuthorCatalogFilter filter) {
        List<Predicate> parts = new ArrayList<>();
        String text = text(filter.query);
        if (!text.isEmpty()) {
            parts.add(nameContains(cb, author, text));
        }
        if (filter.mostRecent) {
            parts.add(idsIn(cb, author.get("id"), filter.mostRecentAuthorIds));
        }
        if (filter.withoutDescription) {
            parts.add(cb.isNull(author.get("biographicalEssay")));
        }
        Path<String> grokipedia = author.get("grokipediaUrl");
        if (filter.withoutGrokipedia) {
            parts.add(grokipediaMissing(cb, grokipedia));
        }
        if (filter.withGrokipedia) {
            parts.add(grokipediaPresent(cb, grokipedia));
        }
        if (filter.zeroBooks) {
            parts.add(cb.not(cb.exists(authorBooks(cb, query, author, null))));
        }
        if (filter.withoutPhotos) {
            parts.add(cb.not(cb.exists(authorPhotos(cb, query, author))));
        }
        if (filter.withPhotos) {
            parts.add(cb.exists(authorPhotos(cb, query, author)));
        }
        if (filter.withoutBirthDate) {
            parts.add(cb.isNull(author.get("dateOfBirth")));
        }
        if (filter.withoutDeathDate) {
            parts.add(cb.isNull(author.get("dateOfDeath")));
        }
        if (filter.favorites) {
            parts.add(idsIn(cb, author.get("id"), filter.favoriteAuthorIds));
        }
        authorAvailability(cb, query, author, filter.ydlBook, "ydlPaperAvailable", parts);
        authorAvailability(cb, query, author, filter.ydlEbook, "ydlEbookAvailable", parts);
        authorAvailability(cb, query, author, filter.ydlAudio, "ydlAudioAvailable", parts);
        authorAvailability(cb, query, author, filter.emuBook, "emuPaperAvailable", parts);
        authorAvailability(cb, query, author, filter.emuEbook, "emuEbookAvailable", parts);
        authorAvailability(cb, query, author, filter.emuAudio, "emuAudioAvailable", parts);
        authorAvailability(cb, query, author, filter.aclaBook, "aclaPaperAvailable", parts);
        authorAvailability(cb, query, author, filter.aclaEbook, "aclaEbookAvailable", parts);
        authorAvailability(cb, query, author, filter.aclaAudio, "aclaAudioAvailable", parts);
        return and(cb, parts);
    }

    private void authorAvailability(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Author> author,
            boolean on, String flag, List<Predicate> parts) {
        if (on) {
            parts.add(cb.exists(authorBooks(cb, query, author, flag)));
        }
    }

    private Subquery<Integer> authorBooks(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Author> author,
            String trueFlag) {
        Subquery<Integer> books = query.subquery(Integer.class);
        Root<Book> book = books.from(Book.class);
        books.select(cb.literal(1));
        if (trueFlag == null) {
            books.where(cb.equal(book.get("author"), author));
        } else {
            books.where(cb.equal(book.get("author"), author), cb.isTrue(book.get(trueFlag)));
        }
        return books;
    }

    private Subquery<Integer> authorPhotos(HibernateCriteriaBuilder cb, AbstractQuery<?> query, Root<Author> author) {
        Subquery<Integer> photos = query.subquery(Integer.class);
        Root<Photo> photo = photos.from(Photo.class);
        photos.select(cb.literal(1));
        photos.where(cb.equal(photo.get("author"), author), cb.isNull(photo.get("book")));
        return photos;
    }

    private Predicate idsAndName(HibernateCriteriaBuilder cb, Root<Author> author, List<Long> ids, String query) {
        List<Predicate> parts = new ArrayList<>();
        parts.add(idsIn(cb, author.get("id"), ids));
        if (query != null && !query.isEmpty()) {
            parts.add(nameContains(cb, author, query));
        }
        return and(cb, parts);
    }

    private void addBookText(HibernateCriteriaBuilder cb, Root<Book> book, Join<Book, Author> author,
            BookCatalogFilter filter, boolean matchAuthorName, List<Predicate> parts) {
        String text = text(filter == null ? null : filter.query);
        if (text.isEmpty()) {
            return;
        }
        parts.add(bookText(cb, book, author, text, matchAuthorName));
    }

    private Predicate bookText(HibernateCriteriaBuilder cb, Root<Book> book, Join<Book, Author> author, String text,
            boolean matchAuthorName) {
        List<Predicate> parts = new ArrayList<>();
        parts.add(contains(cb, book.get("title"), text));
        parts.add(contains(cb, cb.coalesce(book.get("alternateTitle"), ""), text));
        if (matchAuthorName && author != null) {
            parts.add(contains(cb, cb.coalesce(author.get("name"), ""), text));
            parts.add(contains(cb, cb.coalesce(alternateNames(cb, author), ""), text));
        }
        return or(cb, parts);
    }

    private Predicate nameContains(HibernateCriteriaBuilder cb, Path<?> author, String text) {
        return cb.or(
                contains(cb, author.get("name"), text),
                contains(cb, cb.coalesce(alternateNames(cb, author), ""), text));
    }

    private Expression<String> alternateNames(HibernateCriteriaBuilder cb, Path<?> owner) {
        return cb.function("array_to_string", String.class, owner.get("alternateNames"), cb.literal("||"));
    }

    private Predicate contains(HibernateCriteriaBuilder cb, Expression<String> field, String text) {
        Expression<String> pattern = cb.lower(cb.concat("%", cb.concat(cb.literal(text), "%")));
        return cb.like(cb.lower(field), pattern);
    }

    private Predicate grokipediaMissing(HibernateCriteriaBuilder cb, Path<String> url) {
        return cb.or(cb.isNull(url), cb.equal(url, ""), cb.equal(url, "-"));
    }

    private Predicate grokipediaPresent(HibernateCriteriaBuilder cb, Path<String> url) {
        return cb.and(cb.isNotNull(url), cb.notEqual(url, ""), cb.notEqual(url, "-"));
    }

    private void trueFlag(HibernateCriteriaBuilder cb, Root<Book> book, boolean on, String attribute,
            List<Predicate> parts) {
        if (on) {
            parts.add(cb.isTrue(book.get(attribute)));
        }
    }

    private Predicate idsIn(HibernateCriteriaBuilder cb, Expression<?> id, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return cb.disjunction();
        }
        return id.in(ids);
    }

    private static void applyOrders(CriteriaQuery<?> query, List<Order> orders) {
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
    }

    private LocalDateTime priceCutoff(BookCatalogFilter filter) {
        if (filter.priceOlderCutoff != null) {
            return filter.priceOlderCutoff;
        }
        int days = filter.priceOlderDays > 0 ? filter.priceOlderDays : DEFAULT_PRICE_OLDER_DAYS;
        return LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
    }

    private static String text(String query) {
        return query == null ? "" : query.trim();
    }

    @SuppressWarnings("unchecked")
    private Expression<String> castToString(HibernateCriteriaBuilder cb, Path<?> path) {
        return cb.cast((JpaExpression<Object>) path, String.class);
    }

    private List<Order> pageableOrders(HibernateCriteriaBuilder cb, Root<?> root, Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return List.of();
        }
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            Path<Object> path = root.get(order.getProperty());
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        return orders;
    }

    private Predicate and(HibernateCriteriaBuilder cb, List<Predicate> parts) {
        if (parts.isEmpty()) {
            return cb.conjunction();
        }
        return cb.and(parts.toArray(Predicate[]::new));
    }

    private Predicate or(HibernateCriteriaBuilder cb, List<Predicate> parts) {
        if (parts.isEmpty()) {
            return cb.disjunction();
        }
        return cb.or(parts.toArray(Predicate[]::new));
    }

    private <T> Page<T> page(CriteriaQuery<T> dataQuery, CriteriaQuery<Long> countQuery, Pageable pageable) {
        var data = entityManager.createQuery(dataQuery);
        if (pageable.isPaged()) {
            data.setFirstResult((int) pageable.getOffset());
            data.setMaxResults(pageable.getPageSize());
        }
        List<T> content = data.getResultList();
        long total = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private HibernateCriteriaBuilder builder() {
        return (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private record DatedSummary(Long id, LocalDateTime lastModified, LocalDateTime dateAddedToLibrary)
            implements BookRepository.DatedBookSummaryProjection {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public LocalDateTime getLastModified() {
            return lastModified;
        }

        @Override
        public LocalDateTime getDateAddedToLibrary() {
            return dateAddedToLibrary;
        }
    }

    private record NamedSummary(Long id, LocalDateTime lastModified, String name)
            implements AuthorRepository.NamedAuthorSummaryProjection {
        @Override
        public Long getId() {
            return id;
        }

        @Override
        public LocalDateTime getLastModified() {
            return lastModified;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.freetext;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Global cache of known free text URLs for books.
 * Uses pre-computed lookup results to avoid redundant provider queries.
 *
 * The cache stores normalized author names mapped to normalized titles,
 * which then map to space-separated URL strings.
 *
 * Normalization uses the same algorithms as {@link TitleMatcher}:
 * - Lowercase conversion
 * - Punctuation removal (except spaces)
 * - Trailing parenthetical content removal
 * - Leading article removal
 * - Multiple space collapse
 */
@Slf4j
public final class FreeTextLookupCache {

    /**
     * Map structure: normalizedAuthor -> (normalizedTitle -> space-separated URLs)
     */
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    static {
        // Data extracted from book-lookup-reference-backup/*.log HITs
        // Format: add(author, title, urls...)

        // Louisa May Alcott
        add("Louisa May Alcott", "Eight Cousins",
            "https://www.gutenberg.org/ebooks/2726",
            "https://archive.org/details/eightcousinsora04alcogoog",
            "https://librivox.org/eight-cousins-by-louisa-may-alcott/",
            "http://hdl.loc.gov/loc.gdc/scd0001.00021244107",
            "https://www.loc.gov/item/03020582/");

        add("Louisa May Alcott", "Little Women",
            "https://www.gutenberg.org/ebooks/37106",
            "https://archive.org/details/littlewomencompl0000loui",
            "https://librivox.org/little-women-by-louisa-may-alcott/",
            "https://www.loc.gov/item/14017126/");

        add("Louisa May Alcott", "An Old-Fashioned Girl",
            "https://archive.org/details/anoldfashionedg01alcogoog");

        // Robert Louis Stevenson
        add("Robert Louis Stevenson", "Kidnapped",
            "https://www.gutenberg.org/ebooks/421",
            "https://archive.org/details/kidnapped0000robe_m0o2",
            "https://librivox.org/kidnapped-by-robert-louis-stevenson/",
            "https://www.loc.gov/item/20005406/");

        add("Robert Louis Stevenson", "Treasure Island",
            "https://www.gutenberg.org/ebooks/120",
            "https://archive.org/details/treasureislandab0000unse_m5z9",
            "https://librivox.org/treasure-island-by-robert-louis-stevenson/",
            "https://www.loc.gov/item/50041964/");

        // Dante Alighieri
        add("Dante Alighieri", "The Purgatorio",
            "https://archive.org/details/purgatorio00dantuoft");

        add("Dante Alighieri", "Purgatory",
            "https://archive.org/details/cu31924012867424",
            "https://librivox.org/purgatory-by-rev-francois-xavier-schouppe/");

        add("Dante Alighieri", "Inferno",
            "https://www.loc.gov/item/16003562/");

        add("Dante Alighieri", "The Inferno",
            "https://www.loc.gov/item/16003562/");

        // Burton Egbert Stevenson
        add("Burton Egbert Stevenson", "Home Book of Verse for Young Folks",
            "https://archive.org/details/homebookversefo00stevgoog");

        add("Burton Egbert Stevenson", "The Home Book of Verse for Young Folks",
            "https://archive.org/details/homebookversefo00stevgoog");

        // John Bunyan
        add("John Bunyan", "Pilgrim's Progress",
            "https://archive.org/details/pilgrimsprogres00browgoog",
            "https://www.loc.gov/item/29022878/");

        // John of the Cross
        add("John of the Cross", "Living Flame of Love",
            "https://archive.org/details/johnofthecross00johnuoft");

        // Leo Tolstoy
        add("Leo Tolstoy", "Anna Karenina",
            "https://www.gutenberg.org/ebooks/1399",
            "https://archive.org/details/annakareninavolu0002leot_d7b9",
            "https://librivox.org/anna-karenina-book-1-by-leo-tolstoy/");

        // Dorothy Canfield Fisher
        add("Dorothy Canfield Fisher", "Understood Betsy",
            "https://www.gutenberg.org/ebooks/5347",
            "https://archive.org/details/understoodbetsy00fishgoog",
            "https://librivox.org/understood-betsy-by-dorothy-canfield-fisher-2/");

        // Margaret Sidney
        add("Margaret Sidney", "Five Little Peppers and How They Grew",
            "https://www.gutenberg.org/ebooks/2770",
            "https://archive.org/details/fivelittlepeppe00sidngoog");

        // Alban Butler
        add("Alban Butler", "Butler's Lives of the Saints",
            "https://archive.org/details/onehundredpious00butlgoog");

        // Ignatius of Loyola
        add("Ignatius of Loyola", "The Spiritual Exercises",
            "https://www.gutenberg.org/ebooks/70790",
            "https://archive.org/details/spiritualexercis0000igna_v4z4",
            "https://librivox.org/the-spiritual-exercises-by-st-ignatius-loyola/",
            "https://www.loc.gov/item/32011967/",
            "https://ccel.org/ccel/ignatius/exercises/exercises",
            "http://www.ccel.org/ccel/ignatius/exercises.html",
            "https://www.catholicplanet.com/ebooks/Spiritual-Exercises.pdf");

        // John Croiset
        add("John Croiset", "Devotion to the Sacred Heart of Jesus",
            "https://archive.org/details/devotiontosacred0000frjo",
            "https://www.loc.gov/item/ltf90012752/");

        // Frances Hodgson Burnett
        add("Frances Hodgson Burnett", "The Secret Garden",
            "https://www.gutenberg.org/ebooks/17396",
            "https://archive.org/details/secretgarden0000fran_n3s9",
            "https://librivox.org/the-secret-garden-by-frances-hodgson-burnett/");

        // C. A. Chardenal
        add("C. A. Chardenal", "First French Course or Rules and Exercises for Beginners",
            "https://archive.org/details/firstfrenchcours0000ccha");

        // Bess Streeter Aldrich
        add("Bess Streeter Aldrich", "A Lantern in Her Hand",
            "https://archive.org/details/lanterninherhand0000bess");

        // Gilbert Keith Chesterton
        add("Gilbert Keith Chesterton", "Orthodoxy",
            "https://www.gutenberg.org/ebooks/16769",
            "https://archive.org/details/orthodoxy0000gilb_y5p2",
            "https://librivox.org/orthodoxy-by-gk-chesterton/");

        add("Gilbert Keith Chesterton", "The Innocence of Father Brown",
            "https://www.gutenberg.org/ebooks/204");

        // Eleanor Estes
        add("Eleanor Estes", "Ginger Pye",
            "https://archive.org/details/gingerpyeodyssey0000elea");

        // Erich Maria Remarque
        add("Erich Maria Remarque", "All Quiet on the Western Front",
            "https://www.gutenberg.org/ebooks/75011",
            "https://archive.org/details/allquietonwester0000eric_f4c3");

        // John A. Lomax
        add("John A. Lomax", "American Ballads and Folk Songs",
            "https://archive.org/details/in.ernet.dli.2015.158045",
            "https://www.loc.gov/item/22021580/");

        // Charles Dickens (different name variations in logs)
        add("Charles John Huffam Dickens", "A Tale of Two Cities",
            "https://www.gutenberg.org/ebooks/98",
            "https://www.loc.gov/item/21015938/");

        add("Charles Dickens", "A Tale of Two Cities",
            "https://www.gutenberg.org/ebooks/98",
            "https://www.loc.gov/item/21015938/");

        // Better Homes and Gardens
        add("Better Homes and Gardens", "New Cook Book",
            "https://www.loc.gov/item/12001718/");

        // Louis of Granada
        add("Louis of Granada", "The Sinner's Guide",
            "https://www.loc.gov/item/39001478/",
            "https://www.ewtn.com/catholicism/library/sinners-guide-9832");

        // George MacDonald
        add("George MacDonald", "The Golden Key and Other Stories",
            "https://www.loc.gov/item/10011469/");

        // Claire Huchet Bishop
        add("Claire Huchet Bishop", "Lafayette",
            "https://www.loc.gov/item/22000701/");

        // Fulton J. Sheen
        add("Fulton J. Sheen", "Life of Christ",
            "https://www.loc.gov/item/13020319/");

        // Pope John Paul II
        add("Pope John Paul II", "Catechism of the Catholic Church",
            "https://www.loc.gov/item/99001094/");

        add("Pope John Paul II", "Catechism of the Catholic Church, c. 3",
            "https://www.loc.gov/item/99001094/");

        // Thomas Stearns Eliot
        add("Thomas Stearns Eliot", "Murder in the Cathedral",
            "https://www.loc.gov/item/musftpplaybills.200221187/");

        add("T. S. Eliot", "Murder in the Cathedral",
            "https://www.loc.gov/item/musftpplaybills.200221187/");

        // Oscar Wilde
        add("Oscar Fingal O'Flahertie Wills Wilde", "The Importance of Being Earnest",
            "https://www.gutenberg.org/ebooks/844");

        add("Oscar Wilde", "The Importance of Being Earnest",
            "https://www.gutenberg.org/ebooks/844");

        // Margery Williams
        add("Margery Williams", "The Velveteen Rabbit",
            "https://www.gutenberg.org/ebooks/11757",
            "https://librivox.org/the-velveteen-rabbit-by-margery-williams/");

        // Brother Lawrence
        add("Brother Lawrence", "The Practice of the Presence of God",
            "https://www.gutenberg.org/ebooks/5657",
            "https://ccel.org/ccel/lawrence/practice/practice");

        // Pope Francis
        add("Pope Francis", "Lumen Fidei",
            "https://www.vatican.va/content/francesco/en/encyclicals/documents/papa-francesco_20130629_enciclica-lumen-fidei.html");

        // Thomas Bertram Costain / Georg Ebers (LibriVox matched wrong author)
        add("Thomas Bertram Costain", "Joshua",
            "https://librivox.org/joshua-by-georg-ebers/");

        // Benedict Rohner
        add("Benedict Rohner", "The Life of the Blessed Virgin Mary",
            "https://www.catholicplanet.com/ebooks/Life-of-Blessed-Virgin-Mary.pdf");

        // Catholic Church - Enchiridion of Indulgences (user-requested addition)
        add("Catholic Church", "The Handbook of Indulgences",
            "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");

        add("Catholic Church", "Enchiridion of Indulgences",
            "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");

        add("Catholic Church", "The Handbook of Indulgences: Norms and Grants",
            "https://www.vatican.va/roman_curia/tribunals/apost_penit/documents/rc_trib_appen_doc_20020826_enchiridion-indulgentiarum_lt.html");

        log.info("FreeTextLookupCache initialized with {} authors", CACHE.size());
    }

    private FreeTextLookupCache() {
        // Utility class
    }

    /**
     * Add an entry to the cache with multiple URLs.
     */
    private static void add(String author, String title, String... urls) {
        String normalizedAuthor = normalizeAuthor(author);
        String normalizedTitle = normalizeTitle(title);
        String urlString = String.join(" ", urls);

        CACHE.computeIfAbsent(normalizedAuthor, k -> new HashMap<>())
             .put(normalizedTitle, urlString);
    }

    /**
     * Look up cached URLs for a book by author and title.
     * Uses normalized matching based on TitleMatcher algorithms.
     *
     * @param author the author name (can be null)
     * @param title the book title
     * @return space-separated URLs if found, null otherwise
     */
    public static String lookup(String author, String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String normalizedTitle = normalizeTitle(title);

        // If author is provided, try exact author match first
        if (author != null && !author.isBlank()) {
            String normalizedAuthor = normalizeAuthor(author);

            Map<String, String> authorCache = CACHE.get(normalizedAuthor);
            if (authorCache != null) {
                String urls = authorCache.get(normalizedTitle);
                if (urls != null) {
                    log.debug("Cache hit for author='{}' title='{}' (exact)", author, title);
                    return urls;
                }
            }

            // Try fuzzy author matching (last name match)
            String authorLastName = getLastName(normalizedAuthor);
            for (Map.Entry<String, Map<String, String>> entry : CACHE.entrySet()) {
                String cachedLastName = getLastName(entry.getKey());
                if (cachedLastName.equals(authorLastName)) {
                    String urls = entry.getValue().get(normalizedTitle);
                    if (urls != null) {
                        log.debug("Cache hit for author='{}' title='{}' (last name match)", author, title);
                        return urls;
                    }
                }
            }
        }

        // Fall back to title-only search across all authors
        for (Map<String, String> authorCache : CACHE.values()) {
            String urls = authorCache.get(normalizedTitle);
            if (urls != null) {
                log.debug("Cache hit for title='{}' (title-only)", title);
                return urls;
            }
        }

        return null;
    }

    /**
     * Get the first URL from a cached result.
     *
     * @param author the author name (can be null)
     * @param title the book title
     * @return first URL if found, null otherwise
     */
    public static String lookupFirstUrl(String author, String title) {
        String urls = lookup(author, title);
        if (urls == null) {
            return null;
        }
        int spaceIndex = urls.indexOf(' ');
        return spaceIndex > 0 ? urls.substring(0, spaceIndex) : urls;
    }

    /**
     * Normalize a title for cache lookup using TitleMatcher-style normalization.
     * - Convert to lowercase
     * - Remove trailing parenthetical content
     * - Remove leading articles
     * - Remove punctuation except spaces
     * - Collapse multiple spaces
     */
    static String normalizeTitle(String title) {
        return title.toLowerCase()
                .replaceAll("\\s*\\([^)]*\\)\\s*$", "") // Remove trailing (date), (edition), etc.
                .replaceAll("^(the|a|an)\\s+", "")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Normalize an author name for cache lookup.
     * - Convert to lowercase
     * - Remove punctuation except spaces
     * - Collapse multiple spaces
     */
    static String normalizeAuthor(String author) {
        return author.toLowerCase()
                .replaceAll("[^a-z\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Extract the last name from a normalized author string.
     * Handles both "First Last" and "Last, First" formats.
     */
    private static String getLastName(String author) {
        if (author.contains(",")) {
            return author.split(",")[0].trim();
        }
        String[] parts = author.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : author;
    }

    /**
     * Get cache statistics for monitoring.
     *
     * @return number of unique authors in cache
     */
    public static int getAuthorCount() {
        return CACHE.size();
    }

    /**
     * Get total number of cached book entries.
     *
     * @return total number of author/title combinations
     */
    public static int getBookCount() {
        return CACHE.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}

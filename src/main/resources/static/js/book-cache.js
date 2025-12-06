// (c) Copyright 2025 by Muczynski

/**
 * Browser-based book cache using IndexedDB
 * Caches books based on ID and last-modified timestamp
 */

const BOOK_CACHE_DB_NAME = 'LibraryBookCache';
const BOOK_CACHE_STORE_NAME = 'books';
const BOOK_CACHE_VERSION = 1;

let bookCacheDB = null;

/**
 * Initialize the IndexedDB database for book caching
 */
async function initBookCache() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(BOOK_CACHE_DB_NAME, BOOK_CACHE_VERSION);

        request.onerror = () => {
            console.error('Failed to open book cache database:', request.error);
            reject(request.error);
        };

        request.onsuccess = () => {
            bookCacheDB = request.result;
            console.log('Book cache database initialized');
            resolve(bookCacheDB);
        };

        request.onupgradeneeded = (event) => {
            const db = event.target.result;

            // Delete old object store if it exists
            if (db.objectStoreNames.contains(BOOK_CACHE_STORE_NAME)) {
                db.deleteObjectStore(BOOK_CACHE_STORE_NAME);
            }

            // Create new object store with id as keyPath
            const objectStore = db.createObjectStore(BOOK_CACHE_STORE_NAME, { keyPath: 'id' });

            // Create index on lastModified for efficient querying
            objectStore.createIndex('lastModified', 'lastModified', { unique: false });

            console.log('Book cache object store created');
        };
    });
}

/**
 * Get a book from the cache
 */
async function getCachedBook(id) {
    if (!bookCacheDB) {
        await initBookCache();
    }

    return new Promise((resolve, reject) => {
        const transaction = bookCacheDB.transaction([BOOK_CACHE_STORE_NAME], 'readonly');
        const objectStore = transaction.objectStore(BOOK_CACHE_STORE_NAME);
        const request = objectStore.get(id);

        request.onsuccess = () => {
            resolve(request.result);
        };

        request.onerror = () => {
            console.error('Failed to get cached book:', request.error);
            reject(request.error);
        };
    });
}

/**
 * Save a book to the cache
 */
async function setCachedBook(book) {
    if (!bookCacheDB) {
        await initBookCache();
    }

    return new Promise((resolve, reject) => {
        const transaction = bookCacheDB.transaction([BOOK_CACHE_STORE_NAME], 'readwrite');
        const objectStore = transaction.objectStore(BOOK_CACHE_STORE_NAME);
        const request = objectStore.put(book);

        request.onsuccess = () => {
            resolve();
        };

        request.onerror = () => {
            console.error('Failed to cache book:', request.error);
            reject(request.error);
        };
    });
}

/**
 * Get all cached books
 */
async function getAllCachedBooks() {
    if (!bookCacheDB) {
        await initBookCache();
    }

    return new Promise((resolve, reject) => {
        const transaction = bookCacheDB.transaction([BOOK_CACHE_STORE_NAME], 'readonly');
        const objectStore = transaction.objectStore(BOOK_CACHE_STORE_NAME);
        const request = objectStore.getAll();

        request.onsuccess = () => {
            resolve(request.result);
        };

        request.onerror = () => {
            console.error('Failed to get all cached books:', request.error);
            reject(request.error);
        };
    });
}

/**
 * Clear all cached books
 */
async function clearBookCache() {
    if (!bookCacheDB) {
        await initBookCache();
    }

    return new Promise((resolve, reject) => {
        const transaction = bookCacheDB.transaction([BOOK_CACHE_STORE_NAME], 'readwrite');
        const objectStore = transaction.objectStore(BOOK_CACHE_STORE_NAME);
        const request = objectStore.clear();

        request.onsuccess = () => {
            console.log('Book cache cleared');
            resolve();
        };

        request.onerror = () => {
            console.error('Failed to clear book cache:', request.error);
            reject(request.error);
        };
    });
}

/**
 * Load books with caching
 * Fetches book summaries from server and compares with cache
 * Only fetches full book data for books that are new or modified
 */
async function loadBooksWithCache() {
    try {
        // Initialize cache if needed
        if (!bookCacheDB) {
            await initBookCache();
        }

        // Fetch summaries from server (lightweight - just ID and lastModified)
        const summaries = await fetchData('/api/books/summaries');

        // Get all cached books
        const cachedBooks = await getAllCachedBooks();
        const cachedBooksMap = new Map(cachedBooks.map(book => [book.id, book]));

        // Determine which books need to be fetched
        const idsToFetch = [];
        const cachedResults = [];

        for (const summary of summaries) {
            const cached = cachedBooksMap.get(summary.id);

            if (!cached) {
                // Book not in cache - need to fetch
                idsToFetch.push(summary.id);
            } else if (cached.lastModified !== summary.lastModified) {
                // Book modified since cached - need to fetch
                idsToFetch.push(summary.id);
            } else {
                // Book unchanged - use cached version
                cachedResults.push(cached);
            }
        }

        console.log(`Book cache: ${cachedResults.length} from cache, ${idsToFetch.length} to fetch`);

        // Fetch books that need updating
        let fetchedBooks = [];
        if (idsToFetch.length > 0) {
            fetchedBooks = await fetchData('/api/books/by-ids', {
                method: 'POST',
                body: idsToFetch
            });

            // Cache the newly fetched books
            for (const book of fetchedBooks) {
                await setCachedBook(book);
            }
        }

        // Combine cached and fetched results
        const allBooks = [...cachedResults, ...fetchedBooks];

        // Sort by dateAddedToLibrary (newest first)
        allBooks.sort((a, b) => {
            const dateA = a.dateAddedToLibrary ? new Date(a.dateAddedToLibrary) : new Date(0);
            const dateB = b.dateAddedToLibrary ? new Date(b.dateAddedToLibrary) : new Date(0);
            return dateB - dateA;
        });

        return allBooks;
    } catch (error) {
        console.error('Failed to load books with cache:', error);
        // Fallback to direct API call if cache fails
        console.log('Falling back to direct API call');
        return await fetchData('/api/books');
    }
}

/**
 * Invalidate a single book in the cache (for when a book is updated)
 */
async function invalidateCachedBook(id) {
    if (!bookCacheDB) {
        await initBookCache();
    }

    return new Promise((resolve, reject) => {
        const transaction = bookCacheDB.transaction([BOOK_CACHE_STORE_NAME], 'readwrite');
        const objectStore = transaction.objectStore(BOOK_CACHE_STORE_NAME);
        const request = objectStore.delete(id);

        request.onsuccess = () => {
            console.log(`Invalidated cache for book ID ${id}`);
            resolve();
        };

        request.onerror = () => {
            console.error('Failed to invalidate cached book:', request.error);
            reject(request.error);
        };
    });
}

// Expose functions globally
window.initBookCache = initBookCache;
window.loadBooksWithCache = loadBooksWithCache;
window.invalidateCachedBook = invalidateCachedBook;
window.clearBookCache = clearBookCache;
window.getCachedBook = getCachedBook;

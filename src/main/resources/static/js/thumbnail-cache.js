// (c) Copyright 2025 by Muczynski
// Thumbnail caching utility - caches images by checksum to avoid redundant fetches

// Cache object: checksum -> blob URL
const thumbnailCache = {};

// Pending fetches: checksum -> Promise<blobUrl>
const pendingFetches = {};

/**
 * Load a thumbnail image with caching by checksum
 * @param {HTMLImageElement} imgElement - The img element to load
 * @param {number} photoId - The photo ID
 * @param {string} checksum - The image checksum for caching
 * @param {number} [width] - Optional width for thumbnail sizing
 */
async function loadCachedThumbnail(imgElement, photoId, checksum, width) {
    // If no checksum provided, fall back to direct load without caching
    if (!checksum) {
        imgElement.src = `/api/photos/${photoId}/image`;
        return;
    }

    // Check if already cached
    if (thumbnailCache[checksum]) {
        imgElement.src = thumbnailCache[checksum];
        return;
    }

    // Check if there's already a pending fetch for this checksum
    if (pendingFetches[checksum]) {
        try {
            const blobUrl = await pendingFetches[checksum];
            imgElement.src = blobUrl;
        } catch (error) {
            console.error('[ThumbnailCache] Failed to load from pending fetch:', error);
            // Fallback to direct load
            imgElement.src = `/api/photos/${photoId}/image`;
        }
        return;
    }

    // Create a new fetch promise
    const fetchPromise = (async () => {
        try {
            const response = await fetch(`/api/photos/${photoId}/image`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const blob = await response.blob();
            const blobUrl = URL.createObjectURL(blob);
            thumbnailCache[checksum] = blobUrl;
            return blobUrl;
        } catch (error) {
            // Remove from pending on error
            delete pendingFetches[checksum];
            throw error;
        }
    })();

    pendingFetches[checksum] = fetchPromise;

    try {
        const blobUrl = await fetchPromise;
        imgElement.src = blobUrl;
        // Clean up pending after successful load
        delete pendingFetches[checksum];
    } catch (error) {
        console.error('[ThumbnailCache] Failed to load thumbnail:', error);
        // Fallback to direct load
        imgElement.src = `/api/photos/${photoId}/image`;
    }
}

/**
 * Preload a thumbnail into cache without displaying it
 * @param {number} photoId - The photo ID
 * @param {string} checksum - The image checksum for caching
 */
async function preloadThumbnail(photoId, checksum) {
    if (!checksum || thumbnailCache[checksum] || pendingFetches[checksum]) {
        return;
    }

    const fetchPromise = (async () => {
        try {
            const response = await fetch(`/api/photos/${photoId}/image`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const blob = await response.blob();
            const blobUrl = URL.createObjectURL(blob);
            thumbnailCache[checksum] = blobUrl;
            return blobUrl;
        } catch (error) {
            delete pendingFetches[checksum];
            throw error;
        }
    })();

    pendingFetches[checksum] = fetchPromise;

    try {
        await fetchPromise;
        delete pendingFetches[checksum];
    } catch (error) {
        console.error('[ThumbnailCache] Failed to preload thumbnail:', error);
    }
}

/**
 * Get cache statistics
 * @returns {object} Cache stats
 */
function getThumbnailCacheStats() {
    return {
        cachedCount: Object.keys(thumbnailCache).length,
        pendingCount: Object.keys(pendingFetches).length
    };
}

/**
 * Clear the thumbnail cache
 */
function clearThumbnailCache() {
    // Revoke all blob URLs to free memory
    for (const blobUrl of Object.values(thumbnailCache)) {
        URL.revokeObjectURL(blobUrl);
    }
    // Clear the cache object
    for (const key in thumbnailCache) {
        delete thumbnailCache[key];
    }
    console.log('[ThumbnailCache] Cache cleared');
}

// Make functions globally available
window.loadCachedThumbnail = loadCachedThumbnail;
window.preloadThumbnail = preloadThumbnail;
window.getThumbnailCacheStats = getThumbnailCacheStats;
window.clearThumbnailCache = clearThumbnailCache;

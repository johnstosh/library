// (c) Copyright 2025 by Muczynski
// Books-from-Feed using Google Photos Picker API (New Session-Based Flow)
// All API calls route through backend to handle OAuth token refresh automatically

let currentSessionId = null;
let pollingInterval = null;

async function loadBooksFromFeedSection() {
    console.log('[BooksFromFeed] Loading books-from-feed section');
    clearError('books-from-feed');
}

/**
 * Phase 1: Fetch photos from Google Photos and save to database
 */
async function fetchPhotosFromFeed() {
    clearError('books-from-feed');
    const fetchBtn = document.getElementById('fetch-photos-btn');
    const spinner = fetchBtn.querySelector('.spinner-border');

    try {
        // Show spinner
        spinner.classList.remove('d-none');
        fetchBtn.disabled = true;

        // Check authorization
        const user = await fetchData('/api/user-settings');
        if (!user.googlePhotosApiKey || user.googlePhotosApiKey.trim() === '') {
            showError('books-from-feed', 'Please authorize Google Photos in Settings first.');
            return;
        }

        showInfo('books-from-feed', 'Fetching photos from Google Photos...');

        // Call Phase 1 endpoint
        const result = await postData('/api/books-from-feed/fetch-photos', {});

        // Display results
        displayFetchResults(result);

        if (result.savedCount > 0) {
            showSuccess('books-from-feed',
                `Phase 1 Complete: Saved ${result.savedCount} photo(s) to database. Click "Step 2: Process with AI" to continue.`);
        } else {
            showInfo('books-from-feed', `No new photos found.`);
        }

    } catch (error) {
        console.error('[BooksFromFeed] Phase 1 failed:', error);
        showError('books-from-feed', 'Phase 1 failed: ' + error.message);
    } finally {
        spinner.classList.add('d-none');
        fetchBtn.disabled = false;
    }
}

/**
 * Phase 2: Process saved photos with AI
 */
async function processSavedPhotosFromFeed() {
    clearError('books-from-feed');
    const processBtn = document.getElementById('process-saved-btn');
    const spinner = processBtn.querySelector('.spinner-border');

    try {
        // Show spinner
        spinner.classList.remove('d-none');
        processBtn.disabled = true;

        showInfo('books-from-feed', 'Processing saved photos with AI... This may take a while.');

        // Call Phase 2 endpoint
        const result = await postData('/api/books-from-feed/process-saved', {});

        // Display results
        displayProcessResults(result);

        if (result.processedCount > 0) {
            showSuccess('books-from-feed',
                `Phase 2 Complete: Created ${result.processedCount} book(s) from ${result.totalBooks} saved photo(s).`);

            // Reload books list if on books section
            if (window.loadBooks) {
                await loadBooks();
            }
        } else {
            showInfo('books-from-feed', `No books created from ${result.totalBooks} saved photo(s).`);
        }

    } catch (error) {
        console.error('[BooksFromFeed] Phase 2 failed:', error);
        showError('books-from-feed', 'Phase 2 failed: ' + error.message);
    } finally {
        spinner.classList.add('d-none');
        processBtn.disabled = false;
    }
}

function displayFetchResults(result) {
    const resultsContainer = document.getElementById('processing-results');
    resultsContainer.innerHTML = '';

    const summaryCard = document.createElement('div');
    summaryCard.className = 'card mb-3';
    summaryCard.innerHTML = `
        <div class="card-header bg-primary text-white">
            <h5>Phase 1: Fetch Results</h5>
        </div>
        <div class="card-body">
            <p><strong>Total Photos Found:</strong> ${result.totalPhotos}</p>
            <p><strong>Photos Saved:</strong> ${result.savedCount}</p>
            <p><strong>Photos Skipped:</strong> ${result.skippedCount}</p>
            ${result.lastTimestamp ? `<p><small>Last timestamp: ${result.lastTimestamp}</small></p>` : ''}
        </div>
    `;
    resultsContainer.appendChild(summaryCard);

    // Skipped photos
    if (result.skippedPhotos && result.skippedPhotos.length > 0) {
        const skippedCard = document.createElement('div');
        skippedCard.className = 'card mb-3';
        skippedCard.innerHTML = `
            <div class="card-header">
                <h6>Skipped Photos (${result.skippedPhotos.length})</h6>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                        <tr>
                            <th>Photo ID</th>
                            <th>Reason</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.skippedPhotos.map(photo => `
                            <tr>
                                <td><small>${escapeHtml(photo.id)}</small></td>
                                <td><small>${escapeHtml(photo.reason)}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        resultsContainer.appendChild(skippedCard);
    }
}

function displayProcessResults(result) {
    const resultsContainer = document.getElementById('processing-results');
    resultsContainer.innerHTML = '';

    const summaryCard = document.createElement('div');
    summaryCard.className = 'card mb-3';
    summaryCard.innerHTML = `
        <div class="card-header bg-success text-white">
            <h5>Phase 2: Process Results</h5>
        </div>
        <div class="card-body">
            <p><strong>Total Saved Photos:</strong> ${result.totalBooks}</p>
            <p><strong>Books Created:</strong> ${result.processedCount}</p>
            <p><strong>Failed:</strong> ${result.failedCount}</p>
        </div>
    `;
    resultsContainer.appendChild(summaryCard);

    // Processed books
    if (result.processedBooks && result.processedBooks.length > 0) {
        const booksCard = document.createElement('div');
        booksCard.className = 'card mb-3';
        booksCard.innerHTML = `
            <div class="card-header">
                <h6>Books Created (${result.processedBooks.length})</h6>
            </div>
            <div class="card-body">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Author</th>
                            <th>Book ID</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.processedBooks.map(book => `
                            <tr>
                                <td>${escapeHtml(book.title)}</td>
                                <td>${escapeHtml(book.author)}</td>
                                <td><small>${book.bookId}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        resultsContainer.appendChild(booksCard);
    }

    // Failed books
    if (result.failedBooks && result.failedBooks.length > 0) {
        const failedCard = document.createElement('div');
        failedCard.className = 'card mb-3';
        failedCard.innerHTML = `
            <div class="card-header">
                <h6>Failed (${result.failedBooks.length})</h6>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                        <tr>
                            <th>Book ID</th>
                            <th>Reason</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.failedBooks.map(book => `
                            <tr>
                                <td><small>${book.bookId}</small></td>
                                <td><small>${escapeHtml(book.reason)}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        resultsContainer.appendChild(failedCard);
    }
}

/**
 * Alternative workflow: Use Google Photos Picker (existing functionality)
 */
async function processPhotosFromFeed() {
    clearError('books-from-feed');

    // Check if user has authorized Google Photos
    try {
        const user = await fetchData('/api/user-settings');
        if (!user.googlePhotosApiKey || user.googlePhotosApiKey.trim() === '') {
            showError('books-from-feed', 'Please authorize Google Photos in Settings first.');
            return;
        }

        // Show the new Photos Picker (backend handles token refresh)
        await showPhotoPicker();
    } catch (error) {
        showError('books-from-feed', 'Failed to get authorization: ' + error.message);
    }
}

async function showPhotoPicker() {
    try {
        showInfo('books-from-feed', 'Opening Google Photos Picker...');

        // Create a new picker session
        const session = await createPickerSession();
        currentSessionId = session.id;

        console.log('[BooksFromFeed] Picker session created:', currentSessionId);
        console.log('[BooksFromFeed] Picker URI:', session.pickerUri);

        // Append /autoclose to automatically close the picker window after selection (web best practice)
        const pickerUriWithAutoClose = session.pickerUri + '/autoclose';

        // Open the picker in a new window
        const pickerWindow = window.open(
            pickerUriWithAutoClose,
            'Google Photos Picker',
            'width=800,height=600,resizable=yes,scrollbars=yes'
        );

        if (!pickerWindow) {
            throw new Error('Popup blocked. Please allow popups for this site.');
        }

        // Start polling for session completion
        startPollingSession(session.id);

    } catch (error) {
        console.error('[BooksFromFeed] Failed to show picker:', error);
        showError('books-from-feed', 'Failed to open picker: ' + error.message);
    }
}

async function createPickerSession() {
    // Call backend endpoint which handles token refresh automatically
    const response = await fetch('/api/books-from-feed/picker-session', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
    });

    if (!response.ok) {
        const errorData = await response.json();
        console.error('[BooksFromFeed] Session creation failed:', response.status, errorData);
        throw new Error(`Failed to create picker session: ${errorData.error || response.statusText}`);
    }

    const session = await response.json();
    return session;
}

function startPollingSession(sessionId) {
    // Clear any existing polling interval
    if (pollingInterval) {
        clearInterval(pollingInterval);
    }

    let pollCount = 0;
    const maxPolls = 120; // Poll for up to 10 minutes (120 * 5 seconds)

    showInfo('books-from-feed', 'Waiting for photo selection...');

    pollingInterval = setInterval(async () => {
        pollCount++;

        if (pollCount >= maxPolls) {
            clearInterval(pollingInterval);
            showError('books-from-feed', 'Photo selection timed out. Please try again.');
            return;
        }

        try {
            const sessionData = await getSession(sessionId);

            // Check if user has completed selection
            // Per Google's docs: mediaItemsSet=true when user finishes selecting photos
            console.log('[BooksFromFeed] Polling session... mediaItemsSet:', sessionData.mediaItemsSet);

            if (sessionData.mediaItemsSet === true) {
                clearInterval(pollingInterval);
                pollingInterval = null;

                console.log('[BooksFromFeed] User completed photo selection');

                // Process the selected photos
                await handlePickerResults(sessionId);
            }
        } catch (error) {
            console.error('[BooksFromFeed] Polling error:', error);
            // Continue polling unless it's a fatal error
            if (error.message.includes('404') || error.message.includes('403')) {
                clearInterval(pollingInterval);
                showError('books-from-feed', 'Session expired or unauthorized: ' + error.message);
            }
        }
    }, 5000); // Poll every 5 seconds
}

async function getSession(sessionId) {
    // Call backend endpoint which handles token refresh automatically
    const response = await fetch(`/api/books-from-feed/picker-session/${sessionId}`);

    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(`Failed to get session: ${errorData.error || response.statusText}`);
    }

    const session = await response.json();
    return session;
}

async function handlePickerResults(sessionId) {
    try {
        showInfo('books-from-feed', 'Processing selected photos...');

        // Get the list of selected media items
        const mediaItems = await listMediaItems(sessionId);

        console.log('[BooksFromFeed] User selected', mediaItems.length, 'photos');

        if (mediaItems.length === 0) {
            showInfo('books-from-feed', 'No photos selected.');
            return;
        }

        // Transform new Picker API response to match backend expectations
        // Actual API structure: {id, createTime, type, mediaFile: {baseUrl, filename, mimeType}}
        const photos = mediaItems.map(item => ({
            id: item.id,
            name: item.mediaFile.filename || item.id,
            url: item.mediaFile.baseUrl,
            thumbnailUrl: item.mediaFile.baseUrl, // Same as baseUrl, backend will add size params
            description: '', // Not present in Picker API response
            mimeType: item.mediaFile.mimeType,
            lastEditedUtc: item.createTime || new Date().getTime()
        }));

        // Send to backend for processing
        const result = await postData('/api/books-from-feed/process-from-picker', { photos });

        displayProcessingResults(result);

        if (result.processedCount > 0) {
            showSuccess('books-from-feed', `Successfully processed ${result.processedCount} book(s) from ${result.totalPhotos} photo(s).`);

            // Reload books list if on books section
            if (window.loadBooks) {
                await loadBooks();
            }
        } else {
            showInfo('books-from-feed', `No new books found in ${result.totalPhotos} photo(s).`);
        }

    } catch (error) {
        console.error('[BooksFromFeed] Failed to process photos:', error);
        showError('books-from-feed', 'Failed to process photos: ' + error.message);
    }
}

async function listMediaItems(sessionId) {
    // Fetch media items via backend to avoid CORS restrictions
    // Google's Picker API blocks CORS on the mediaItems endpoint
    const response = await fetchData(`/api/books-from-feed/picker-session/${sessionId}/media-items`);

    if (response.error) {
        console.error('[BooksFromFeed] MediaItems fetch failed:', response.error);
        throw new Error(response.error);
    }

    return response.mediaItems || [];
}

function displayProcessingResults(result) {
    const resultsContainer = document.getElementById('processing-results');
    resultsContainer.innerHTML = '';

    // Summary card
    const summaryCard = document.createElement('div');
    summaryCard.className = 'card mb-3';
    summaryCard.innerHTML = `
        <div class="card-header">
            <h5>Processing Summary</h5>
        </div>
        <div class="card-body">
            <p><strong>Total Photos Selected:</strong> ${result.totalPhotos}</p>
            <p><strong>Books Created:</strong> ${result.processedCount}</p>
            <p><strong>Photos Skipped:</strong> ${result.skippedCount}</p>
        </div>
    `;
    resultsContainer.appendChild(summaryCard);

    // Processed books
    if (result.processedBooks && result.processedBooks.length > 0) {
        const booksCard = document.createElement('div');
        booksCard.className = 'card mb-3';
        booksCard.innerHTML = `
            <div class="card-header">
                <h5>Books Created (${result.processedBooks.length})</h5>
            </div>
            <div class="card-body">
                <table class="table table-striped">
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Author</th>
                            <th>Photo</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.processedBooks.map(book => `
                            <tr>
                                <td>${escapeHtml(book.title)}</td>
                                <td>${escapeHtml(book.author)}</td>
                                <td><small>${escapeHtml(book.photoName || book.photoId)}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        resultsContainer.appendChild(booksCard);
    }

    // Skipped photos
    if (result.skippedPhotos && result.skippedPhotos.length > 0) {
        const skippedCard = document.createElement('div');
        skippedCard.className = 'card mb-3';
        skippedCard.innerHTML = `
            <div class="card-header">
                <h5>Skipped Photos (${result.skippedPhotos.length})</h5>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                        <tr>
                            <th>Photo</th>
                            <th>Reason</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.skippedPhotos.map(photo => `
                            <tr>
                                <td><small>${escapeHtml(photo.name || photo.id)}</small></td>
                                <td><small>${escapeHtml(photo.reason)}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
        resultsContainer.appendChild(skippedCard);
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showSuccess(section, message) {
    const container = document.getElementById(`${section}-section`);
    let successDiv = container.querySelector('[data-test="form-success"]');
    if (!successDiv) {
        successDiv = document.createElement('div');
        successDiv.setAttribute('data-test', 'form-success');
        successDiv.className = 'alert alert-success';
        container.insertBefore(successDiv, container.firstChild);
    }
    successDiv.textContent = message;
    successDiv.style.display = 'block';
}

function showInfo(section, message) {
    const container = document.getElementById(`${section}-section`);
    let infoDiv = container.querySelector('[data-test="form-info"]');
    if (!infoDiv) {
        infoDiv = document.createElement('div');
        infoDiv.setAttribute('data-test', 'form-info');
        infoDiv.className = 'alert alert-info';
        container.insertBefore(infoDiv, container.firstChild);
    }
    infoDiv.textContent = message;
    infoDiv.style.display = 'block';
}

// (c) Copyright 2025 by Muczynski
// Books-from-Feed using Google Photos Picker API (New Session-Based Flow)

let accessToken = null;
let currentSessionId = null;
let pollingInterval = null;

async function loadBooksFromFeedSection() {
    console.log('[BooksFromFeed] Loading books-from-feed section');
    clearError('books-from-feed');
}

async function processPhotosFromFeed() {
    clearError('books-from-feed');

    // Get access token from current user
    try {
        const user = await fetchData('/api/user-settings');
        if (!user.googlePhotosApiKey || user.googlePhotosApiKey.trim() === '') {
            showError('books-from-feed', 'Please authorize Google Photos in Settings first.');
            return;
        }
        accessToken = user.googlePhotosApiKey;

        // Show the new Photos Picker
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

        // Open the picker in a new window
        const pickerWindow = window.open(
            session.pickerUri,
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
    const response = await fetch('https://photospicker.googleapis.com/v1/sessions', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({}),
    });

    if (!response.ok) {
        const errorText = await response.text();
        console.error('[BooksFromFeed] Session creation failed:', response.status, errorText);
        throw new Error(`Failed to create picker session: ${response.status} ${response.statusText}`);
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
            console.log('[BooksFromFeed] Session state:', sessionData.mediaItemsSet ? 'COMPLETED' : 'PENDING');

            // Check if user has completed selection
            if (sessionData.mediaItemsSet) {
                clearInterval(pollingInterval);
                pollingInterval = null;

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
    const response = await fetch(`https://photospicker.googleapis.com/v1/sessions/${sessionId}`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error(`Failed to get session: ${response.status} ${response.statusText}`);
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
        const photos = mediaItems.map(item => ({
            id: item.mediaItem.id,
            name: item.mediaItem.filename || item.mediaItem.id,
            url: item.mediaItem.baseUrl,
            thumbnailUrl: item.mediaItem.baseUrl, // Same as baseUrl, backend will add size params
            description: item.mediaItem.description || '',
            mimeType: item.mediaItem.mimeType,
            lastEditedUtc: item.mediaItem.mediaMetadata?.creationTime || new Date().getTime()
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
    // Fetch media items via backend to avoid CORS issues
    const response = await fetchData(`/api/books-from-feed/picker-session/${sessionId}/media-items`);

    if (response.error) {
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

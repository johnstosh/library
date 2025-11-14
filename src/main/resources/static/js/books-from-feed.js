// (c) Copyright 2025 by Muczynski
// Books-from-Feed using Google Photos Picker API

let pickerApiLoaded = false;
let accessToken = null;

async function loadBooksFromFeedSection() {
    console.log('[BooksFromFeed] Loading books-from-feed section');
    clearError('books-from-feed');

    // Load Google Picker API
    loadGooglePickerAPI();
}

function loadGooglePickerAPI() {
    if (pickerApiLoaded) return;

    const script = document.createElement('script');
    script.src = 'https://apis.google.com/js/api.js';
    script.onload = () => {
        gapi.load('picker', () => {
            console.log('[BooksFromFeed] Google Picker API loaded');
            pickerApiLoaded = true;
        });
    };
    document.head.appendChild(script);
}

async function processPhotosFromFeed() {
    clearError('books-from-feed');

    if (!pickerApiLoaded) {
        showError('books-from-feed', 'Google Picker API is still loading. Please wait and try again.');
        return;
    }

    // Get access token from current user
    try {
        const user = await fetchData('/api/user-settings');
        if (!user.googlePhotosApiKey || user.googlePhotosApiKey.trim() === '') {
            showError('books-from-feed', 'Please authorize Google Photos in Settings first.');
            return;
        }
        accessToken = user.googlePhotosApiKey;

        // Show the picker
        showPhotoPicker();
    } catch (error) {
        showError('books-from-feed', 'Failed to get authorization: ' + error.message);
    }
}

function showPhotoPicker() {
    // Build and show the picker
    const picker = new google.picker.PickerBuilder()
        .addView(google.picker.ViewId.PHOTOS)
        .setOAuthToken(accessToken)
        .setCallback(pickerCallback)
        .setTitle('Select Book Cover Photos')
        .build();

    picker.setVisible(true);
}

async function pickerCallback(data) {
    if (data[google.picker.Response.ACTION] == google.picker.Action.PICKED) {
        const docs = data[google.picker.Response.DOCUMENTS];
        console.log('[BooksFromFeed] User selected', docs.length, 'photos');

        showInfo('books-from-feed', `Processing ${docs.length} selected photo(s)...`);

        try {
            // Extract photo URLs and metadata
            const photos = docs.map(doc => ({
                id: doc[google.picker.Document.ID],
                name: doc[google.picker.Document.NAME],
                url: doc[google.picker.Document.URL],
                thumbnailUrl: doc[google.picker.Document.THUMBNAILS] ?
                    doc[google.picker.Document.THUMBNAILS][0].url : null,
                description: doc[google.picker.Document.DESCRIPTION] || '',
                mimeType: doc[google.picker.Document.MIME_TYPE],
                lastEditedUtc: doc[google.picker.Document.LAST_EDITED_UTC]
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
            showError('books-from-feed', 'Failed to process photos: ' + error.message);
        }
    } else if (data[google.picker.Response.ACTION] == google.picker.Action.CANCEL) {
        console.log('[BooksFromFeed] User cancelled picker');
    }
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

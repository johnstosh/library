// (c) Copyright 2025 by Muczynski

async function loadBooksFromFeedSection() {
    console.log('[BooksFromFeed] Loading books-from-feed section');
    clearError('books-from-feed');
}

async function processPhotosFromFeed() {
    const processBtn = document.getElementById('process-photos-btn');
    const originalText = processBtn.textContent;

    try {
        processBtn.disabled = true;
        processBtn.textContent = 'Processing...';
        document.body.style.cursor = 'wait';

        clearError('books-from-feed');
        showInfo('books-from-feed', 'Processing photos from Google Photos feed... This may take several minutes.');

        const result = await postData('/api/books-from-feed/process', {});

        document.body.style.cursor = 'default';
        processBtn.disabled = false;
        processBtn.textContent = originalText;

        displayProcessingResults(result);

        if (result.processedCount > 0) {
            showSuccess('books-from-feed', `Successfully processed ${result.processedCount} book(s) from ${result.totalPhotos} photo(s).`);
        } else {
            showInfo('books-from-feed', `No new books found in ${result.totalPhotos} photo(s).`);
        }

    } catch (error) {
        document.body.style.cursor = 'default';
        processBtn.disabled = false;
        processBtn.textContent = originalText;
        showError('books-from-feed', 'Failed to process photos: ' + error.message);
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
            <p><strong>Total Photos Scanned:</strong> ${result.totalPhotos}</p>
            <p><strong>Books Created:</strong> ${result.processedCount}</p>
            <p><strong>Photos Skipped:</strong> ${result.skippedCount}</p>
            <p><strong>Last Photo Timestamp:</strong> ${formatTimestamp(result.lastTimestamp)}</p>
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
                            <th>Photo ID</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${result.processedBooks.map(book => `
                            <tr>
                                <td>${escapeHtml(book.title)}</td>
                                <td>${escapeHtml(book.author)}</td>
                                <td><small>${escapeHtml(book.photoId)}</small></td>
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

function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';
    try {
        const date = new Date(timestamp);
        return date.toLocaleString();
    } catch (e) {
        return timestamp;
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

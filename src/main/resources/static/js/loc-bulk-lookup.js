/*
 * (c) Copyright 2025 by Muczynski
 */


/**
 * Initialize event listeners when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Attach event listeners to buttons
    const viewAllBtn = document.getElementById('view-all-books-btn');
    if (viewAllBtn) {
        viewAllBtn.addEventListener('click', () => loadLocLookupBooks('all'));
    }

    const viewMissingBtn = document.getElementById('view-missing-loc-btn');
    if (viewMissingBtn) {
        viewMissingBtn.addEventListener('click', () => loadLocLookupBooks('missing'));
    }

    const lookupTableBtn = document.getElementById('lookup-table-missing-btn');
    if (lookupTableBtn) {
        lookupTableBtn.addEventListener('click', () => lookupTableMissing());
    }

    const lookupAllBtn = document.getElementById('lookup-all-missing-btn');
    if (lookupAllBtn) {
        lookupAllBtn.addEventListener('click', () => lookupAllMissing());
    }
});

/**
 * Load books for LOC lookup table
 * @param {string} mode - 'all', 'missing', or 'most-recent'
 */
async function loadLocLookupBooks(mode) {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

        let endpoint;
        if (mode === 'missing') {
            endpoint = '/api/loc-bulk-lookup/books/missing-loc';
        } else if (mode === 'most-recent') {
            endpoint = '/api/loc-bulk-lookup/books/most-recent';
        } else {
            endpoint = '/api/loc-bulk-lookup/books';
        }

        const books = await fetchData(endpoint);

        const tableBody = document.getElementById('loc-lookup-table-body');
        tableBody.innerHTML = '';

        if (books.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="4" class="text-center">No books found</td>';
            tableBody.appendChild(row);
            return;
        }

        books.forEach(book => {
            const row = createBookRow(book);
            tableBody.appendChild(row);
        });

        const modeText = mode === 'most-recent' ? ' from most recent date' : '';
        showSuccess('loc-lookup', `Loaded ${books.length} book(s)${modeText}`);
    } catch (error) {
        showError('loc-lookup', 'Failed to load books: ' + error.message);
    }
}

/**
 * Load books from the most recent date added to the library
 * This is called when the LOC Bulk Lookup section is shown
 */
async function loadLocBulkLookupSection() {
    await loadLocLookupBooks('most-recent');
}

/**
 * Create a table row for a book
 */
function createBookRow(book) {
    const row = document.createElement('tr');
    row.setAttribute('data-test', 'loc-lookup-book-row');
    row.setAttribute('data-book-id', book.id);

    // Photo cell
    const photoCell = document.createElement('td');
    if (book.firstPhotoId) {
        const img = document.createElement('img');
        img.style.width = '50px';
        img.style.height = 'auto';
        // Use cached thumbnail loading
        window.loadCachedThumbnail(img, book.firstPhotoId, book.firstPhotoChecksum);
        photoCell.appendChild(img);
    } else {
        photoCell.textContent = '-';
    }
    row.appendChild(photoCell);

    // Book cell (title + author)
    const titleCell = document.createElement('td');
    titleCell.setAttribute('data-test', 'book-title');

    // Title on first line
    const titleSpan = document.createElement('span');
    titleSpan.textContent = book.title;
    titleSpan.style.fontWeight = 'bold';
    titleCell.appendChild(titleSpan);

    // Author on second line
    if (book.authorName && book.authorName.trim() !== '') {
        titleCell.appendChild(document.createElement('br'));
        const authorSpan = document.createElement('span');
        authorSpan.textContent = book.authorName;
        authorSpan.style.fontSize = '0.9em';
        authorSpan.style.color = '#6c757d'; // Bootstrap's text-muted color
        titleCell.appendChild(authorSpan);
    }
    row.appendChild(titleCell);

    // LOC Call Number cell - formatted for spine display with each component on its own line
    const locCell = document.createElement('td');
    locCell.setAttribute('data-test', 'loc-number');
    if (book.currentLocNumber) {
        const code = document.createElement('code');
        code.innerHTML = window.formatLocForSpine(book.currentLocNumber);
        code.className = 'text-success';
        locCell.appendChild(code);
    } else {
        const span = document.createElement('span');
        span.textContent = 'Not set';
        span.className = 'text-muted fst-italic';
        locCell.appendChild(span);
    }
    row.appendChild(locCell);

    // Actions cell - Lookup button followed by view/edit/delete icons
    const actionsCell = document.createElement('td');
    const lookupBtn = document.createElement('button');
    lookupBtn.className = 'btn btn-sm btn-primary me-2';
    lookupBtn.textContent = 'Lookup';
    lookupBtn.setAttribute('data-test', 'lookup-single-btn');
    lookupBtn.onclick = () => lookupSingleBook(book.id);
    actionsCell.appendChild(lookupBtn);

    // View button (icon-only)
    const viewBtn = document.createElement('button');
    viewBtn.className = 'btn btn-sm btn-outline-primary me-1';
    viewBtn.innerHTML = '<i class="bi bi-eye"></i>';
    viewBtn.title = 'View';
    viewBtn.setAttribute('data-test', 'view-loc-book-btn');
    viewBtn.onclick = () => window.viewBook(book.id);
    actionsCell.appendChild(viewBtn);

    // Edit button (icon-only)
    const editBtn = document.createElement('button');
    editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
    editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
    editBtn.title = 'Edit';
    editBtn.setAttribute('data-test', 'edit-loc-book-btn');
    editBtn.onclick = () => editBook(book.id);
    actionsCell.appendChild(editBtn);

    // Delete button (icon-only)
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'btn btn-sm btn-outline-danger';
    deleteBtn.innerHTML = '<i class="bi bi-trash"></i>';
    deleteBtn.title = 'Delete';
    deleteBtn.setAttribute('data-test', 'delete-loc-book-btn');
    deleteBtn.onclick = () => deleteBook(book.id);
    actionsCell.appendChild(deleteBtn);

    row.appendChild(actionsCell);

    return row;
}

/**
 * Lookup LOC number for a single book
 */
async function lookupSingleBook(bookId) {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

        // Show progress indicator on the row
        const row = document.querySelector(`tr[data-book-id="${bookId}"]`);
        const actionsCell = row.querySelector('td:last-child');
        actionsCell.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Looking up...';

        const result = await fetchData(`/api/loc-bulk-lookup/lookup/${bookId}`, {
            method: 'POST'
        });

        if (result.success) {
            // Update the LOC number cell (check if row still exists in DOM)
            const currentRow = document.querySelector(`tr[data-book-id="${bookId}"]`);
            if (currentRow) {
                const locCell = currentRow.querySelector('[data-test="loc-number"]');
                if (locCell) {
                    locCell.innerHTML = '';
                    const code = document.createElement('code');
                    code.innerHTML = window.formatLocForSpine(result.locNumber);
                    code.className = 'text-success fw-bold';
                    locCell.appendChild(code);
                }

                // Restore the actions cell with buttons
                const currentActionsCell = currentRow.querySelector('td:last-child');
                if (currentActionsCell) {
                    currentActionsCell.innerHTML = '';
                    const lookupBtn = document.createElement('button');
                    lookupBtn.className = 'btn btn-sm btn-primary me-2';
                    lookupBtn.textContent = 'Lookup';
                    lookupBtn.setAttribute('data-test', 'lookup-single-btn');
                    lookupBtn.onclick = () => lookupSingleBook(bookId);
                    currentActionsCell.appendChild(lookupBtn);

                    // View button
                    const viewBtn = document.createElement('button');
                    viewBtn.className = 'btn btn-sm btn-outline-primary me-1';
                    viewBtn.innerHTML = '<i class="bi bi-eye"></i>';
                    viewBtn.title = 'View';
                    viewBtn.onclick = () => window.viewBook(bookId);
                    currentActionsCell.appendChild(viewBtn);

                    // Edit button
                    const editBtn = document.createElement('button');
                    editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
                    editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
                    editBtn.title = 'Edit';
                    editBtn.onclick = () => editBook(bookId);
                    currentActionsCell.appendChild(editBtn);

                    // Delete button
                    const deleteBtn = document.createElement('button');
                    deleteBtn.className = 'btn btn-sm btn-outline-danger';
                    deleteBtn.innerHTML = '<i class="bi bi-trash"></i>';
                    deleteBtn.title = 'Delete';
                    deleteBtn.onclick = () => deleteBook(bookId);
                    currentActionsCell.appendChild(deleteBtn);
                }
            }

            showSuccess('loc-lookup', `Found LOC number: ${result.locNumber}`);
        } else {
            // Restore the actions cell with buttons
            const currentRow = document.querySelector(`tr[data-book-id="${bookId}"]`);
            if (currentRow) {
                const currentActionsCell = currentRow.querySelector('td:last-child');
                if (currentActionsCell) {
                    currentActionsCell.innerHTML = '';
                    const lookupBtn = document.createElement('button');
                    lookupBtn.className = 'btn btn-sm btn-primary me-2';
                    lookupBtn.textContent = 'Lookup';
                    lookupBtn.setAttribute('data-test', 'lookup-single-btn');
                    lookupBtn.onclick = () => lookupSingleBook(bookId);
                    currentActionsCell.appendChild(lookupBtn);

                    // View button
                    const viewBtn = document.createElement('button');
                    viewBtn.className = 'btn btn-sm btn-outline-primary me-1';
                    viewBtn.innerHTML = '<i class="bi bi-eye"></i>';
                    viewBtn.title = 'View';
                    viewBtn.onclick = () => window.viewBook(bookId);
                    currentActionsCell.appendChild(viewBtn);

                    // Edit button
                    const editBtn = document.createElement('button');
                    editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
                    editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
                    editBtn.title = 'Edit';
                    editBtn.onclick = () => editBook(bookId);
                    currentActionsCell.appendChild(editBtn);

                    // Delete button
                    const deleteBtn = document.createElement('button');
                    deleteBtn.className = 'btn btn-sm btn-outline-danger';
                    deleteBtn.innerHTML = '<i class="bi bi-trash"></i>';
                    deleteBtn.title = 'Delete';
                    deleteBtn.onclick = () => deleteBook(bookId);
                    currentActionsCell.appendChild(deleteBtn);
                }
            }
            showError('loc-lookup', `Lookup failed: ${result.errorMessage}`);
        }

    } catch (error) {
        showError('loc-lookup', 'Lookup failed: ' + error.message);

        // Restore the actions cell on error
        const row = document.querySelector(`tr[data-book-id="${bookId}"]`);
        if (row) {
            const actionsCell = row.querySelector('td:last-child');
            actionsCell.innerHTML = '';
            const lookupBtn = document.createElement('button');
            lookupBtn.className = 'btn btn-sm btn-primary me-2';
            lookupBtn.textContent = 'Lookup';
            lookupBtn.setAttribute('data-test', 'lookup-single-btn');
            lookupBtn.onclick = () => lookupSingleBook(bookId);
            actionsCell.appendChild(lookupBtn);

            // View button
            const viewBtn = document.createElement('button');
            viewBtn.className = 'btn btn-sm btn-outline-primary me-1';
            viewBtn.innerHTML = '<i class="bi bi-eye"></i>';
            viewBtn.title = 'View';
            viewBtn.onclick = () => window.viewBook(bookId);
            actionsCell.appendChild(viewBtn);

            // Edit button
            const editBtn = document.createElement('button');
            editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
            editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
            editBtn.title = 'Edit';
            editBtn.onclick = () => editBook(bookId);
            actionsCell.appendChild(editBtn);

            // Delete button
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'btn btn-sm btn-outline-danger';
            deleteBtn.innerHTML = '<i class="bi bi-trash"></i>';
            deleteBtn.title = 'Delete';
            deleteBtn.onclick = () => deleteBook(bookId);
            actionsCell.appendChild(deleteBtn);
        }
    }
};

/**
 * Lookup LOC numbers for books missing LOC in the current table
 */
async function lookupTableMissing() {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

        // Get all book IDs from the current table that don't have LOC numbers
        const tableBody = document.getElementById('loc-lookup-table-body');
        const rows = tableBody.querySelectorAll('tr[data-book-id]');

        const missingBookIds = [];
        rows.forEach(row => {
            const locCell = row.querySelector('[data-test="loc-number"]');
            // Check if LOC cell shows "Not set"
            if (locCell && locCell.textContent.includes('Not set')) {
                const bookId = row.getAttribute('data-book-id');
                missingBookIds.push(bookId);
            }
        });

        if (missingBookIds.length === 0) {
            showSuccess('loc-lookup', 'No books in table are missing LOC numbers');
            return;
        }

        // Show spinner in button
        const btn = document.getElementById('lookup-table-missing-btn');
        showButtonSpinner(btn, 'Looking up...');

        // Show progress
        const progressDiv = document.getElementById('loc-lookup-progress');
        const progressText = document.getElementById('loc-lookup-progress-text');
        progressDiv.style.display = 'block';
        progressText.textContent = `Looking up ${missingBookIds.length} book(s) from table...`;

        // Lookup each book
        let successCount = 0;
        let failureCount = 0;

        for (const bookId of missingBookIds) {
            try {
                const result = await fetchData(`/api/loc-bulk-lookup/lookup/${bookId}`, {
                    method: 'POST'
                });

                if (result.success) {
                    successCount++;
                    // Update the row
                    const row = document.querySelector(`tr[data-book-id="${bookId}"]`);
                    if (row) {
                        const locCell = row.querySelector('[data-test="loc-number"]');
                        if (locCell) {
                            locCell.innerHTML = '';
                            const code = document.createElement('code');
                            code.innerHTML = window.formatLocForSpine(result.locNumber);
                            code.className = 'text-success fw-bold';
                            locCell.appendChild(code);
                        }
                    }
                } else {
                    failureCount++;
                }
            } catch (error) {
                console.error(`Failed to lookup book ${bookId}:`, error);
                failureCount++;
            }

            // Update progress
            progressText.textContent = `Looked up ${successCount + failureCount}/${missingBookIds.length} book(s)...`;
        }

        // Hide progress
        progressDiv.style.display = 'none';

        let message = `Table lookup completed: ${successCount} success, ${failureCount} failed`;
        if (successCount > 0) {
            showSuccess('loc-lookup', message);
        } else {
            showError('loc-lookup', message);
        }

    } catch (error) {
        const progressDiv = document.getElementById('loc-lookup-progress');
        progressDiv.style.display = 'none';
        showError('loc-lookup', 'Table lookup failed: ' + error.message);
    } finally {
        const btn = document.getElementById('lookup-table-missing-btn');
        hideButtonSpinner(btn, 'Lookup Table Missing');
    }
}

/**
 * Lookup LOC numbers for all books missing LOC numbers
 */
async function lookupAllMissing() {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

        // Show spinner in button
        const btn = document.getElementById('lookup-all-missing-btn');
        showButtonSpinner(btn, 'Looking up...');

        // Show progress
        const progressDiv = document.getElementById('loc-lookup-progress');
        const progressText = document.getElementById('loc-lookup-progress-text');
        progressDiv.style.display = 'block';
        progressText.textContent = 'Starting bulk lookup...';

        const results = await fetchData('/api/loc-bulk-lookup/lookup-all-missing', {
            method: 'POST'
        });

        // Hide progress
        progressDiv.style.display = 'none';

        // Process results
        let successCount = 0;
        let failureCount = 0;

        results.forEach(result => {
            if (result.success) {
                successCount++;
                // Update the row if it exists in the current view
                const row = document.querySelector(`tr[data-book-id="${result.bookId}"]`);
                if (row) {
                    const locCell = row.querySelector('[data-test="loc-number"]');
                    if (locCell) {
                        locCell.innerHTML = '';
                        const code = document.createElement('code');
                        code.innerHTML = window.formatLocForSpine(result.locNumber);
                        code.className = 'text-success fw-bold';
                        locCell.appendChild(code);
                    }
                }
            } else {
                failureCount++;
            }
        });

        let message = `Bulk lookup completed: ${successCount} success, ${failureCount} failed`;
        if (successCount > 0) {
            showSuccess('loc-lookup', message);
        } else {
            showError('loc-lookup', message);
        }

    } catch (error) {
        const progressDiv = document.getElementById('loc-lookup-progress');
        progressDiv.style.display = 'none';
        showError('loc-lookup', 'Bulk lookup failed: ' + error.message);
    } finally {
        const btn = document.getElementById('lookup-all-missing-btn');
        hideButtonSpinner(btn, 'Lookup All Missing');
    }
};

/**
 * Helper functions for showing errors and success messages
 */
function showError(section, message) {
    const errorDiv = document.getElementById(`${section}-error`);
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

function showSuccess(section, message) {
    const successDiv = document.getElementById(`${section}-results`);
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
    }
}

function clearError(section) {
    const errorDiv = document.getElementById(`${section}-error`);
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

function clearSuccess(section) {
    const successDiv = document.getElementById(`${section}-results`);
    if (successDiv) {
        successDiv.style.display = 'none';
    }
}

// Expose functions globally
window.loadLocBulkLookupSection = loadLocBulkLookupSection;
window.lookupTableMissing = lookupTableMissing;


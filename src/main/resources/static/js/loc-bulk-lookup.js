/*
 * (c) Copyright 2025 by Muczynski
 */
import { fetchData, showError, showSuccess, clearError, clearSuccess } from './utils.js';

/**
 * Load books for LOC lookup table
 * @param {string} mode - 'all' or 'missing'
 */
window.loadLocLookupBooks = async function(mode) {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

        const endpoint = mode === 'missing'
            ? '/api/loc-bulk-lookup/books/missing-loc'
            : '/api/loc-bulk-lookup/books';

        const books = await fetchData(endpoint);

        const tableBody = document.getElementById('loc-lookup-table-body');
        tableBody.innerHTML = '';

        if (books.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="6" class="text-center">No books found</td>';
            tableBody.appendChild(row);
            return;
        }

        books.forEach(book => {
            const row = createBookRow(book);
            tableBody.appendChild(row);
        });

        showSuccess('loc-lookup', `Loaded ${books.length} book(s)`);
    } catch (error) {
        showError('loc-lookup', 'Failed to load books: ' + error.message);
    }
};

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
        img.src = `/api/photos/${book.firstPhotoId}/image`;
        img.style.width = '50px';
        img.style.height = '50px';
        img.style.objectFit = 'cover';
        photoCell.appendChild(img);
    } else {
        photoCell.textContent = '-';
    }
    row.appendChild(photoCell);

    // Title cell
    const titleCell = document.createElement('td');
    titleCell.textContent = book.title;
    titleCell.setAttribute('data-test', 'book-title');
    row.appendChild(titleCell);

    // Author cell
    const authorCell = document.createElement('td');
    authorCell.textContent = book.authorName || '-';
    row.appendChild(authorCell);

    // Year cell
    const yearCell = document.createElement('td');
    yearCell.textContent = book.publicationYear || '-';
    row.appendChild(yearCell);

    // LOC Number cell
    const locCell = document.createElement('td');
    locCell.setAttribute('data-test', 'loc-number');
    if (book.currentLocNumber) {
        const code = document.createElement('code');
        code.textContent = book.currentLocNumber;
        code.className = 'text-success';
        locCell.appendChild(code);
    } else {
        const span = document.createElement('span');
        span.textContent = 'Not set';
        span.className = 'text-muted fst-italic';
        locCell.appendChild(span);
    }
    row.appendChild(locCell);

    // Actions cell
    const actionsCell = document.createElement('td');
    const lookupBtn = document.createElement('button');
    lookupBtn.className = 'btn btn-sm btn-primary';
    lookupBtn.textContent = 'Lookup';
    lookupBtn.setAttribute('data-test', 'lookup-single-btn');
    lookupBtn.onclick = () => lookupSingleBook(book.id);
    actionsCell.appendChild(lookupBtn);
    row.appendChild(actionsCell);

    return row;
}

/**
 * Lookup LOC number for a single book
 */
window.lookupSingleBook = async function(bookId) {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

        // Show progress indicator on the row
        const row = document.querySelector(`tr[data-book-id="${bookId}"]`);
        const actionsCell = row.querySelector('td:last-child');
        const originalContent = actionsCell.innerHTML;
        actionsCell.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Looking up...';

        const result = await fetchData(`/api/loc-bulk-lookup/lookup/${bookId}`, {
            method: 'POST'
        });

        if (result.success) {
            // Update the LOC number cell
            const locCell = row.querySelector('[data-test="loc-number"]');
            locCell.innerHTML = '';
            const code = document.createElement('code');
            code.textContent = result.locNumber;
            code.className = 'text-success fw-bold';
            locCell.appendChild(code);

            showSuccess('loc-lookup', `Found LOC number: ${result.locNumber}`);
        } else {
            showError('loc-lookup', `Lookup failed: ${result.errorMessage}`);
        }

        // Restore the actions cell
        actionsCell.innerHTML = originalContent;

    } catch (error) {
        showError('loc-lookup', 'Lookup failed: ' + error.message);

        // Restore the actions cell on error
        const row = document.querySelector(`tr[data-book-id="${bookId}"]`);
        if (row) {
            const actionsCell = row.querySelector('td:last-child');
            const lookupBtn = document.createElement('button');
            lookupBtn.className = 'btn btn-sm btn-primary';
            lookupBtn.textContent = 'Lookup';
            lookupBtn.setAttribute('data-test', 'lookup-single-btn');
            lookupBtn.onclick = () => lookupSingleBook(bookId);
            actionsCell.innerHTML = '';
            actionsCell.appendChild(lookupBtn);
        }
    }
};

/**
 * Lookup LOC numbers for all books missing LOC numbers
 */
window.lookupAllMissing = async function() {
    try {
        clearError('loc-lookup');
        clearSuccess('loc-lookup');

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
                    locCell.innerHTML = '';
                    const code = document.createElement('code');
                    code.textContent = result.locNumber;
                    code.className = 'text-success fw-bold';
                    locCell.appendChild(code);
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

/*
 * (c) Copyright 2025 by Muczynski
 */
import { fetchData } from './utils.js';

/**
 * Initialize event listeners when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    // Attach event listeners to buttons
    const loadBooksBtn = document.getElementById('load-books-for-labels-btn');
    if (loadBooksBtn) {
        loadBooksBtn.addEventListener('click', () => loadBooksForLabels());
    }

    const generateAllBtn = document.getElementById('generate-all-labels-btn');
    if (generateAllBtn) {
        generateAllBtn.addEventListener('click', () => generateLabels('all'));
    }

    const generateSelectedBtn = document.getElementById('generate-selected-labels-btn');
    if (generateSelectedBtn) {
        generateSelectedBtn.addEventListener('click', () => generateLabels('selected'));
    }

    const selectAllCheckbox = document.getElementById('select-all-labels');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('.book-checkbox');
            checkboxes.forEach(cb => cb.checked = this.checked);
        });
    }
});

/**
 * Load books for labels, sorted by date added
 */
async function loadBooksForLabels() {
    try {
        clearError('labels');
        clearSuccess('labels');

        const books = await fetchData('/api/labels/books');

        const tableBody = document.getElementById('labels-table-body');
        tableBody.innerHTML = '';

        if (books.length === 0) {
            const row = document.createElement('tr');
            row.innerHTML = '<td colspan="5" class="text-center">No books found with LOC numbers</td>';
            tableBody.appendChild(row);
            return;
        }

        books.forEach(book => {
            const row = createBookRow(book);
            tableBody.appendChild(row);
        });

        showSuccess('labels', `Loaded ${books.length} book(s)`);
    } catch (error) {
        showError('labels', 'Failed to load books: ' + error.message);
    }
}

/**
 * Create a table row for a book
 */
function createBookRow(book) {
    const row = document.createElement('tr');
    row.setAttribute('data-test', 'labels-book-row');
    row.setAttribute('data-book-id', book.id);

    // Checkbox cell
    const checkboxCell = document.createElement('td');
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'book-checkbox';
    checkbox.setAttribute('data-book-id', book.id);
    checkbox.setAttribute('data-test', 'book-checkbox');
    checkboxCell.appendChild(checkbox);
    row.appendChild(checkboxCell);

    // Photo cell
    const photoCell = document.createElement('td');
    if (book.firstPhotoId) {
        const img = document.createElement('img');
        img.src = `/api/photos/${book.firstPhotoId}/image`;
        img.style.width = '50px';
        img.style.height = 'auto';
        photoCell.appendChild(img);
    } else {
        photoCell.textContent = '-';
    }
    row.appendChild(photoCell);

    // Title/Author cell (combined)
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

    // LOC Number cell - formatted for spine display with each component on its own line
    const locCell = document.createElement('td');
    if (book.locNumber) {
        const code = document.createElement('code');
        code.innerHTML = window.formatLocForSpine(book.locNumber);
        code.className = 'text-success';
        locCell.appendChild(code);
    } else {
        locCell.textContent = '-';
    }
    row.appendChild(locCell);

    // Date Added cell
    const dateCell = document.createElement('td');
    dateCell.textContent = book.dateAdded || '-';
    row.appendChild(dateCell);

    return row;
}

/**
 * Generate labels PDF
 * @param {string} mode - 'all' or 'selected'
 */
async function generateLabels(mode) {
    try {
        clearError('labels');
        clearSuccess('labels');

        let bookIds = [];

        if (mode === 'selected') {
            const checkboxes = document.querySelectorAll('.book-checkbox:checked');
            if (checkboxes.length === 0) {
                showError('labels', 'Please select at least one book');
                return;
            }
            bookIds = Array.from(checkboxes).map(cb => parseInt(cb.getAttribute('data-book-id')));
        } else {
            // Get all book IDs from the table
            const allCheckboxes = document.querySelectorAll('.book-checkbox');
            if (allCheckboxes.length === 0) {
                showError('labels', 'Please load books first');
                return;
            }
            bookIds = Array.from(allCheckboxes).map(cb => parseInt(cb.getAttribute('data-book-id')));
        }

        // Build URL with book IDs as query parameters
        const params = new URLSearchParams();
        bookIds.forEach(id => params.append('bookIds', id));

        const url = `/api/labels/generate?${params.toString()}`;

        // Download the PDF
        const response = await fetch(url, {
            method: 'GET',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Failed to generate labels PDF');
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = 'book-labels.pdf';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);

        showSuccess('labels', `Generated labels for ${bookIds.length} book(s)`);
    } catch (error) {
        showError('labels', 'Failed to generate labels: ' + error.message);
    }
}

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

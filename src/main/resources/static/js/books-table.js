// (c) Copyright 2025 by Muczynski
async function loadBooks() {
    try {
        const books = await fetchData('/api/books');
        const tableBody = document.getElementById('book-list-body');
        tableBody.innerHTML = '';
        books.forEach(book => {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'book-item');
            row.setAttribute('data-entity-id', book.id);

            const photoCell = document.createElement('td');
            if (book.firstPhotoId) {
                const img = document.createElement('img');
                img.style.width = '50px';
                img.style.height = 'auto';
                img.setAttribute('data-test', 'book-thumbnail');
                // Use cached thumbnail loading
                window.loadCachedThumbnail(img, book.firstPhotoId, book.firstPhotoChecksum);
                photoCell.appendChild(img);
            }
            row.appendChild(photoCell);

            const titleCell = document.createElement('td');

            // Title on first line
            const titleSpan = document.createElement('span');
            titleSpan.setAttribute('data-test', 'book-title');
            titleSpan.textContent = book.title;
            titleSpan.style.fontWeight = 'bold';
            titleCell.appendChild(titleSpan);

            // Author on second line
            if (book.author && book.author.trim() !== '') {
                titleCell.appendChild(document.createElement('br'));
                const authorSpan = document.createElement('span');
                authorSpan.setAttribute('data-test', 'book-author');
                authorSpan.textContent = book.author;
                authorSpan.style.fontSize = '0.9em';
                authorSpan.style.color = '#6c757d'; // Bootstrap's text-muted color
                titleCell.appendChild(authorSpan);
            }

            row.appendChild(titleCell);

            const locCell = document.createElement('td');
            locCell.setAttribute('data-test', 'book-loc-number');
            if (book.locNumber) {
                const locCode = document.createElement('code');
                locCode.innerHTML = window.formatLocForSpine(book.locNumber);
                locCode.className = 'text-success';
                locCell.appendChild(locCode);
            } else {
                const locSpan = document.createElement('span');
                locSpan.textContent = '-';
                locSpan.className = 'text-muted';
                locCell.appendChild(locSpan);
            }
            row.appendChild(locCell);

            const loansCell = document.createElement('td');
            if (book.status === 'WITHDRAWN' || book.status === 'LOST') {
                loansCell.textContent = book.status.toLowerCase();
            } else if (book.loanCount > 0) {
                loansCell.textContent = book.loanCount;
            }
            row.appendChild(loansCell);

            const actionsCell = document.createElement('td');
            actionsCell.setAttribute('data-test', 'book-actions');

            // View button (icon-only) - always visible
            const viewBtn = document.createElement('button');
            viewBtn.className = 'btn btn-sm btn-outline-primary me-1';
            viewBtn.innerHTML = '<i class="bi bi-eye"></i>';
            viewBtn.title = 'View';
            viewBtn.setAttribute('data-test', 'view-book-btn');
            viewBtn.onclick = () => viewBook(book.id);
            actionsCell.appendChild(viewBtn);

            // Edit and Delete buttons (icon-only) - only for librarians
            if (window.isLibrarian) {
                const editBtn = document.createElement('button');
                editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
                editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
                editBtn.title = 'Edit';
                editBtn.setAttribute('data-test', 'edit-book-btn');
                editBtn.onclick = () => editBook(book.id);
                actionsCell.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.className = 'btn btn-sm btn-outline-danger';
                delBtn.innerHTML = '<i class="bi bi-trash"></i>';
                delBtn.title = 'Delete';
                delBtn.setAttribute('data-test', 'delete-book-btn');
                delBtn.onclick = () => deleteBook(book.id);
                actionsCell.appendChild(delBtn);
            }
            row.appendChild(actionsCell);
            tableBody.appendChild(row);
        });
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to load books: ' + error.message);
    }
}

function showBookList(show) {
    document.querySelector('[data-test="book-table"]').style.display = show ? 'table' : 'none';
    document.querySelector('[data-test="books-form"]').style.display = show ? 'none' : 'block';
}

async function deleteBook(id) {
    if (!confirm('Are you sure you want to delete this book?')) return;
    try {
        await deleteData(`/api/books/${id}`);
        await loadBooks();
        await loadLoans();
        await populateLoanDropdowns();
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to delete book: ' + error.message);
    }
}

// Expose functions globally for access from other scripts
window.loadBooks = loadBooks;

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
                img.src = `/api/photos/${book.firstPhotoId}/image`;
                img.style.width = '50px';
                img.style.height = 'auto';
                img.setAttribute('data-test', 'book-thumbnail');
                photoCell.appendChild(img);
            }
            row.appendChild(photoCell);

            const titleCell = document.createElement('td');
            const titleSpan = document.createElement('span');
            titleSpan.setAttribute('data-test', 'book-title');
            titleSpan.textContent = book.title;
            titleCell.appendChild(titleSpan);
            row.appendChild(titleCell);

            const locCell = document.createElement('td');
            locCell.setAttribute('data-test', 'book-loc-number');
            if (book.locNumber) {
                const locCode = document.createElement('code');
                locCode.textContent = book.locNumber;
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
            // Only librarians can edit/delete books
            if (window.isLibrarian) {
                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-book-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editBook(book.id);
                actionsCell.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-book-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
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

// (c) Copyright 2025 by Muczynski
async function loadAuthors() {
    try {
        const authors = await fetchData('/api/authors');
        const listBody = document.getElementById('author-list-body');
        listBody.innerHTML = '';
        authors.forEach(author => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-test', 'author-item');
            tr.setAttribute('data-entity-id', author.id);

            const tdPhoto = document.createElement('td');
            tdPhoto.setAttribute('data-test', 'author-photo-cell');
            if (author.firstPhotoId) {
                const thumbnail = document.createElement('div');
                thumbnail.className = 'author-photo-thumbnail';

                const img = document.createElement('img');
                img.alt = `Photo of ${author.name}`;
                img.style.width = '50px';
                img.style.height = 'auto';
                // Use cached thumbnail loading
                window.loadCachedThumbnail(img, author.firstPhotoId, author.firstPhotoChecksum);
                thumbnail.appendChild(img);
                tdPhoto.appendChild(thumbnail);
            }
            tr.appendChild(tdPhoto);

            const tdName = document.createElement('td');
            tdName.setAttribute('data-test', 'author-name');
            tdName.textContent = author.name;
            tr.appendChild(tdName);

            const tdBooks = document.createElement('td');
            tdBooks.setAttribute('data-test', 'author-book-count');
            tdBooks.textContent = author.bookCount || 0;
            tr.appendChild(tdBooks);

            const tdActions = document.createElement('td');
            tdActions.setAttribute('data-test', 'author-actions');

            // View button (icon-only) - always visible
            const viewBtn = document.createElement('button');
            viewBtn.className = 'btn btn-sm btn-outline-primary me-1';
            viewBtn.innerHTML = '<i class="bi bi-eye"></i>';
            viewBtn.title = 'View';
            viewBtn.setAttribute('data-test', 'view-author-btn');
            viewBtn.onclick = () => viewAuthor(author.id);
            tdActions.appendChild(viewBtn);

            // Edit and Delete buttons (icon-only) - only for librarians
            if (window.isLibrarian) {
                const editBtn = document.createElement('button');
                editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
                editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
                editBtn.title = 'Edit';
                editBtn.setAttribute('data-test', 'edit-author-btn');
                editBtn.onclick = () => editAuthor(author.id);
                tdActions.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.className = 'btn btn-sm btn-outline-danger';
                delBtn.innerHTML = '<i class="bi bi-trash"></i>';
                delBtn.title = 'Delete';
                delBtn.setAttribute('data-test', 'delete-author-btn');
                delBtn.onclick = () => deleteAuthor(author.id);
                tdActions.appendChild(delBtn);
            }
            tr.appendChild(tdActions);
            listBody.appendChild(tr);
        });
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to load authors: ' + error.message);
    }
}

function showAuthorList(show) {
    const authorTable = document.querySelector('[data-test="author-table"]');
    if (authorTable) {
        authorTable.style.display = show ? 'table' : 'none';
    }
}

async function deleteAuthor(id) {
    if (!confirm('Are you sure you want to delete this author?')) return;
    try {
        await deleteData(`/api/authors/${id}`);
        await loadAuthors();
        await populateBookDropdowns();
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to delete author: ' + error.message);
    }
}

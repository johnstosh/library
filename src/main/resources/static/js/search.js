// (c) Copyright 2025 by Muczynski
let currentPage = 0;
const pageSize = 20;

async function performSearch(page = 0) {
    const query = document.getElementById('search-input').value.trim();
    if (!query) {
        const resultsDiv = document.getElementById('search-results');
        resultsDiv.innerHTML = '';
        return;
    }
    try {
        currentPage = page;
        const data = await fetchData(`/api/search?query=${query}&page=${currentPage}&size=${pageSize}`);
        displaySearchResults(data.books, data.authors, query, data.bookPage, data.authorPage);
        clearError('search');
    } catch (error) {
        showError('search', 'Failed to search: ' + error.message);
    }
}

function displaySearchResults(books, authors, query, bookPage, authorPage) {
    const resultsDiv = document.getElementById('search-results');
    resultsDiv.innerHTML = `<h3>Search results for "${query}"</h3>`;

    if (books.length === 0 && authors.length === 0) {
        resultsDiv.innerHTML += '<p>No results found.</p>';
        return;
    }

    if (books.length > 0) {
        const startBook = currentPage * pageSize + 1;
        const endBook = currentPage * pageSize + books.length;
        const booksHeader = document.createElement('h4');
        booksHeader.textContent = `Books ${startBook} - ${endBook}`;
        resultsDiv.appendChild(booksHeader);

        const table = document.createElement('table');
        table.className = 'table';
        table.setAttribute('data-test', 'search-book-table');

        const thead = document.createElement('thead');
        thead.innerHTML = `
            <tr>
                <th scope="col">Photo</th>
                <th scope="col">Book</th>
                <th scope="col">LOC Call Number</th>
                <th scope="col">Actions</th>
            </tr>
        `;
        table.appendChild(thead);

        const tableBody = document.createElement('tbody');
        tableBody.setAttribute('data-test', 'search-book-list-body');
        books.forEach(book => {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'search-book-item');

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
            titleSpan.textContent = book.title;
            titleSpan.style.fontWeight = 'bold';
            titleCell.appendChild(titleSpan);

            // Author on second line
            if (book.author && book.author.trim() !== '') {
                titleCell.appendChild(document.createElement('br'));
                const authorSpan = document.createElement('span');
                authorSpan.textContent = book.author;
                authorSpan.style.fontSize = '0.9em';
                authorSpan.style.color = '#6c757d'; // Bootstrap's text-muted color
                titleCell.appendChild(authorSpan);
            }
            row.appendChild(titleCell);

            const locCell = document.createElement('td');
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

            const actionsCell = document.createElement('td');
            const viewBtn = document.createElement('button');
            viewBtn.className = 'btn btn-sm btn-outline-primary';
            viewBtn.textContent = 'View';
            viewBtn.setAttribute('data-test', 'view-search-book-btn');
            viewBtn.onclick = () => viewBook(book.id);
            actionsCell.appendChild(viewBtn);
            row.appendChild(actionsCell);

            tableBody.appendChild(row);
        });
        table.appendChild(tableBody);
        resultsDiv.appendChild(table);
    }

    if (authors.length > 0) {
        const startAuthor = authorPage.currentPage * authorPage.pageSize + 1;
        const endAuthor = authorPage.currentPage * authorPage.pageSize + authors.length;
        const authorsHeader = document.createElement('h4');
        authorsHeader.textContent = `Authors ${startAuthor} - ${endAuthor}`;
        resultsDiv.appendChild(authorsHeader);

        const authorsList = document.createElement('ul');
        authorsList.className = 'list-group mb-3';
        authorsList.setAttribute('data-test', 'search-authors-list');
        authors.forEach((author, index) => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex align-items-center';
            li.setAttribute('data-test', 'search-author-item');

            // Add thumbnail if available
            if (author.firstPhotoId) {
                const img = document.createElement('img');
                img.style.width = '50px';
                img.style.height = 'auto';
                img.style.marginRight = '10px';
                img.setAttribute('data-test', 'author-thumbnail');
                // Use cached thumbnail loading
                window.loadCachedThumbnail(img, author.firstPhotoId, author.firstPhotoChecksum);
                li.appendChild(img);
            }

            const span = document.createElement('span');
            span.textContent = `${startAuthor + index}. ${author.name}`;
            li.appendChild(span);

            const viewBtn = document.createElement('button');
            viewBtn.className = 'btn btn-sm btn-outline-primary ms-2';
            viewBtn.textContent = 'View';
            viewBtn.setAttribute('data-test', 'view-search-author-btn');
            viewBtn.onclick = () => viewAuthor(author.id);
            li.appendChild(viewBtn);

            authorsList.appendChild(li);
        });
        resultsDiv.appendChild(authorsList);
    }

    const paginationControls = document.createElement('div');
    paginationControls.className = 'd-flex justify-content-center mt-3';

    const prevButton = document.createElement('button');
    prevButton.className = 'btn btn-secondary me-2';
    prevButton.textContent = 'Previous';
    prevButton.disabled = currentPage === 0;
    prevButton.onclick = () => performSearch(currentPage - 1);
    paginationControls.appendChild(prevButton);

    const nextButton = document.createElement('button');
    nextButton.className = 'btn btn-secondary';
    nextButton.textContent = 'Next';
    const onLastBookPage = bookPage ? (currentPage + 1) >= bookPage.totalPages : true;
    const onLastAuthorPage = authorPage ? (currentPage + 1) >= authorPage.totalPages : true;
    nextButton.disabled = onLastBookPage && onLastAuthorPage;
    nextButton.onclick = () => performSearch(currentPage + 1);
    paginationControls.appendChild(nextButton);

    resultsDiv.appendChild(paginationControls);
}

/**
 * View a book's details in read-only mode.
 * Navigates to the Books section and displays all book information.
 */
async function viewBook(id) {
    showSection('books');
    showBookList(false);

    const data = await fetchData(`/api/books/${id}`);

    // Populate form fields with book data
    document.getElementById('new-book-title').value = data.title || '';
    document.getElementById('new-book-year').value = data.publicationYear || '';
    document.getElementById('new-book-publisher').value = data.publisher || '';
    document.getElementById('new-book-summary').value = data.plotSummary || '';
    document.getElementById('new-book-related').value = data.relatedWorks || '';
    document.getElementById('new-book-description').value = data.detailedDescription || '';
    // Convert ISO datetime to datetime-local format (YYYY-MM-DDTHH:mm)
    document.getElementById('new-book-added').value = data.dateAddedToLibrary ? data.dateAddedToLibrary.substring(0, 16) : '';
    document.getElementById('new-book-status').value = data.status || 'ACTIVE';
    document.getElementById('new-book-loc').value = data.locNumber || '';
    document.getElementById('new-book-status-reason').value = data.statusReason || '';

    // Set author name and ID
    document.getElementById('book-author-id').value = data.authorId || '';
    if (data.authorId) {
        const author = allAuthors.find(a => a.id == data.authorId);
        document.getElementById('book-author').value = author ? author.name : '';
    } else {
        document.getElementById('book-author').value = '';
    }

    document.getElementById('book-library').value = data.libraryId || '';
    document.getElementById('current-book-id').value = id;

    // For non-librarians, make fields read-only and hide action buttons
    if (!window.isLibrarian) {
        // Make form fields read-only
        document.getElementById('new-book-title').readOnly = true;
        document.getElementById('new-book-year').readOnly = true;
        document.getElementById('new-book-publisher').readOnly = true;
        document.getElementById('new-book-summary').readOnly = true;
        document.getElementById('new-book-related').readOnly = true;
        document.getElementById('new-book-description').readOnly = true;
        document.getElementById('new-book-added').readOnly = true;
        document.getElementById('new-book-status').disabled = true;
        document.getElementById('new-book-loc').readOnly = true;
        document.getElementById('new-book-status-reason').readOnly = true;
        document.getElementById('book-author').readOnly = true;
        document.getElementById('book-library').disabled = true;

        // Hide action buttons
        document.getElementById('add-book-btn').style.display = 'none';
        document.getElementById('add-photo-btn').style.display = 'none';
        document.getElementById('add-photo-google-btn').style.display = 'none';
        document.getElementById('book-by-photo-btn').style.display = 'none';

        // Show back button
        document.getElementById('cancel-book-btn').style.display = 'inline-block';
        document.getElementById('cancel-book-btn').textContent = 'Back to List';
    } else {
        // Librarian mode - set up for editing
        const btn = document.getElementById('add-book-btn');
        btn.textContent = 'Update Book';
        btn.onclick = () => updateBook(id);
        document.getElementById('add-photo-btn').style.display = 'inline-block';
        document.getElementById('add-photo-google-btn').style.display = 'inline-block';
        document.getElementById('cancel-book-btn').style.display = 'inline-block';
    }

    // Load and display photos
    const photos = await fetchData(`/api/books/${id}/photos`);
    await displayBookPhotos(photos, id);
    if (photos && photos.length > 0 && window.isLibrarian) {
        document.getElementById('book-by-photo-btn').style.display = 'inline-block';
        document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(id);
    }
}

/**
 * View an author's details in read-only mode.
 * Navigates to the Authors section and displays all author information.
 */
async function viewAuthor(id) {
    showSection('authors');
    showAuthorList(false);

    const data = await fetchData(`/api/authors/${id}`);

    // Populate form fields with author data
    document.getElementById('new-author-name').value = data.name || '';
    document.getElementById('new-author-dob').value = data.dateOfBirth || '';
    document.getElementById('new-author-dod').value = data.dateOfDeath || '';
    document.getElementById('new-author-religion').value = data.religiousAffiliation || '';
    document.getElementById('new-author-country').value = data.birthCountry || '';
    document.getElementById('new-author-nationality').value = data.nationality || '';
    document.getElementById('new-author-bio').value = data.briefBiography || '';
    document.getElementById('current-author-id').value = id;

    // For non-librarians, make fields read-only and hide action buttons
    if (!window.isLibrarian) {
        // Make form fields read-only
        document.getElementById('new-author-name').readOnly = true;
        document.getElementById('new-author-dob').readOnly = true;
        document.getElementById('new-author-dod').readOnly = true;
        document.getElementById('new-author-religion').readOnly = true;
        document.getElementById('new-author-country').readOnly = true;
        document.getElementById('new-author-nationality').readOnly = true;
        document.getElementById('new-author-bio').readOnly = true;

        // Hide action buttons
        document.getElementById('add-author-btn').style.display = 'none';
        document.getElementById('add-author-photo-btn').style.display = 'none';
        document.getElementById('add-author-photo-google-btn').style.display = 'none';

        // Show back button
        document.getElementById('cancel-author-btn').style.display = 'inline-block';
        document.getElementById('cancel-author-btn').textContent = 'Back to List';
    } else {
        // Librarian mode - set up for editing
        const btn = document.getElementById('add-author-btn');
        btn.textContent = 'Update Author';
        btn.onclick = () => updateAuthor(id);
        document.getElementById('add-author-photo-btn').style.display = 'inline-block';
        document.getElementById('add-author-photo-google-btn').style.display = 'inline-block';
        document.getElementById('cancel-author-btn').style.display = 'inline-block';
    }

    // Load and display photos
    const photos = await fetchData(`/api/authors/${id}/photos`);
    displayAuthorPhotos(photos, id);
}

// Expose view functions globally
window.viewBook = viewBook;
window.viewAuthor = viewAuthor;

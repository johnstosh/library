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

        const booksList = document.createElement('ul');
        booksList.className = 'list-group mb-3';
        booksList.setAttribute('data-test', 'search-books-list');
        books.forEach((book, index) => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.setAttribute('data-test', 'search-book-item');
            const span = document.createElement('span');
            span.textContent = `${startBook + index}. ${book.title}`;
            li.appendChild(span);
            const viewBtn = document.createElement('button');
            viewBtn.className = 'btn btn-sm btn-outline-primary ms-2';
            viewBtn.textContent = 'View';
            viewBtn.setAttribute('data-test', 'view-search-book-btn');
            viewBtn.onclick = () => viewBook(book.id);
            li.appendChild(viewBtn);
            booksList.appendChild(li);
        });
        resultsDiv.appendChild(booksList);
    }

    if (authors.length > 0) {
        const startAuthor = authorPage.pageable.offset + 1;
        const endAuthor = authorPage.pageable.offset + authors.length;
        const authorsHeader = document.createElement('h4');
        authorsHeader.textContent = `Authors ${startAuthor} - ${endAuthor}`;
        resultsDiv.appendChild(authorsHeader);

        const authorsList = document.createElement('ul');
        authorsList.className = 'list-group mb-3';
        authorsList.setAttribute('data-test', 'search-authors-list');
        authors.forEach((author, index) => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.setAttribute('data-test', 'search-author-item');
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
    nextButton.disabled = (currentPage + 1) >= bookPage.totalPages && (currentPage + 1) >= authorPage.totalPages;
    nextButton.onclick = () => performSearch(currentPage + 1);
    paginationControls.appendChild(nextButton);

    resultsDiv.appendChild(paginationControls);
}

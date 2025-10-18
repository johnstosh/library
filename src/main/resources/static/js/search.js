async function performSearch() {
    const query = document.getElementById('search-input').value.trim();
    if (!query) {
        const resultsDiv = document.getElementById('search-results');
        resultsDiv.innerHTML = '';
        return;
    }
    try {
        const books = await fetchData('/api/books');
        const authors = await fetchData('/api/authors');
        const filteredBooks = books.filter(book => book.title.toLowerCase().includes(query.toLowerCase()));
        const filteredAuthors = authors.filter(author => author.name.toLowerCase().includes(query.toLowerCase()));
        displaySearchResults(filteredBooks, filteredAuthors, query);
        clearError('search');
    } catch (error) {
        showError('search', 'Failed to search: ' + error.message);
    }
}

function displaySearchResults(books, authors, query) {
    const resultsDiv = document.getElementById('search-results');
    resultsDiv.innerHTML = `<h3>Search results for "${query}"</h3>`;

    if (books.length === 0 && authors.length === 0) {
        resultsDiv.innerHTML += '<p>No results found.</p>';
        return;
    }

    if (books.length > 0) {
        const booksHeader = document.createElement('h4');
        booksHeader.textContent = 'Books';
        resultsDiv.appendChild(booksHeader);

        const booksList = document.createElement('ul');
        booksList.className = 'list-group mb-3';
        booksList.setAttribute('data-test', 'search-books-list');
        books.forEach(book => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.setAttribute('data-test', 'search-book-item');
            const span = document.createElement('span');
            span.textContent = book.title;
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
        const authorsHeader = document.createElement('h4');
        authorsHeader.textContent = 'Authors';
        resultsDiv.appendChild(authorsHeader);

        const authorsList = document.createElement('ul');
        authorsList.className = 'list-group mb-3';
        authorsList.setAttribute('data-test', 'search-authors-list');
        authors.forEach(author => {
            const li = document.createElement('li');
            li.className = 'list-group-item d-flex justify-content-between align-items-center';
            li.setAttribute('data-test', 'search-author-item');
            const span = document.createElement('span');
            span.textContent = author.name;
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
}

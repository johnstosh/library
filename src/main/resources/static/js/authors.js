async function loadAuthors() {
    try {
        const authors = await fetchData('/api/authors');
        const listBody = document.getElementById('author-list-body');
        listBody.innerHTML = '';
        authors.forEach(author => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-test', 'author-item');
            tr.setAttribute('data-entity-id', author.id);

            const tdName = document.createElement('td');
            tdName.setAttribute('data-test', 'author-name');
            tdName.textContent = author.name;
            tr.appendChild(tdName);

            const tdActions = document.createElement('td');
            if (isLibrarian) {
                const viewBtn = document.createElement('button');
                viewBtn.setAttribute('data-test', 'view-author-btn');
                viewBtn.textContent = '🔍';
                viewBtn.title = 'View details';
                viewBtn.onclick = () => viewAuthor(author.id);
                tdActions.appendChild(viewBtn);

                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-author-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editAuthor(author.id);
                tdActions.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-author-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
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

async function addAuthor() {
    window.scrollTo(0, 0);
    const name = document.getElementById('new-author-name').value;
    const dateOfBirth = document.getElementById('new-author-dob').value;
    const dateOfDeath = document.getElementById('new-author-dod').value;
    const religiousAffiliation = document.getElementById('new-author-religion').value;
    const birthCountry = document.getElementById('new-author-country').value;
    const nationality = document.getElementById('new-author-nationality').value;
    const briefBiography = document.getElementById('new-author-bio').value;
    if (!name) {
        showError('authors', 'Author name is required.');
        return;
    }
    try {
        await postData('/api/authors', { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
        document.getElementById('new-author-name').value = '';
        document.getElementById('new-author-dob').value = '';
        document.getElementById('new-author-dod').value = '';
        document.getElementById('new-author-religion').value = '';
        document.getElementById('new-author-country').value = '';
        document.getElementById('new-author-nationality').value = '';
        document.getElementById('new-author-bio').value = '';
        await loadAuthors();
        await populateBookDropdowns();
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to add author: ' + error.message);
    }
}

async function editAuthor(id) {
    const data = await fetchData(`/api/authors/${id}`);
    document.getElementById('new-author-name').value = data.name || '';
    document.getElementById('new-author-dob').value = data.dateOfBirth || '';
    document.getElementById('new-author-dod').value = data.dateOfDeath || '';
    document.getElementById('new-author-religion').value = data.religiousAffiliation || '';
    document.getElementById('new-author-country').value = data.birthCountry || '';
    document.getElementById('new-author-nationality').value = data.nationality || '';
    document.getElementById('new-author-bio').value = data.briefBiography || '';
    const btn = document.getElementById('add-author-btn');
    btn.textContent = 'Update Author';
    btn.onclick = () => updateAuthor(id);
}

async function updateAuthor(id) {
    const name = document.getElementById('new-author-name').value;
    const dateOfBirth = document.getElementById('new-author-dob').value;
    const dateOfDeath = document.getElementById('new-author-dod').value;
    const religiousAffiliation = document.getElementById('new-author-religion').value;
    const birthCountry = document.getElementById('new-author-country').value;
    const nationality = document.getElementById('new-author-nationality').value;
    const briefBiography = document.getElementById('new-author-bio').value;
    if (!name) {
        showError('authors', 'Author name is required.');
        return;
    }
    try {
        await putData(`/api/authors/${id}`, { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
        document.getElementById('new-author-name').value = '';
        document.getElementById('new-author-dob').value = '';
        document.getElementById('new-author-dod').value = '';
        document.getElementById('new-author-religion').value = '';
        document.getElementById('new-author-country').value = '';
        document.getElementById('new-author-nationality').value = '';
        document.getElementById('new-author-bio').value = '';
        await loadAuthors();
        await populateBookDropdowns();
        const btn = document.getElementById('add-author-btn');
        btn.textContent = 'Add Author';
        btn.onclick = addAuthor;
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to update author: ' + error.message);
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

async function viewAuthor(id) {
    const data = await fetchData(`/api/authors/${id}`);
    alert(`Author Details:\nID: ${data.id}\nName: ${data.name}\nDate of Birth: ${data.dateOfBirth || 'N/A'}\nDate of Death: ${data.dateOfDeath || 'N/A'}\nReligious Affiliation: ${data.religiousAffiliation || 'N/A'}\nBirth Country: ${data.birthCountry || 'N/A'}\nNationality: ${data.nationality || 'N/A'}\nBrief Biography: ${data.briefBiography || 'N/A'}`);
}

async function bulkImportAuthors() {
    const authorsJson = document.getElementById('bulk-authors').value;
    if (!authorsJson) {
        showError('authors', 'Please provide JSON for bulk import.');
        return;
    }
    try {
        const authors = JSON.parse(authorsJson);
        await postData('/api/authors/bulk', authors);
        await loadAuthors();
        showBulkSuccess('bulk-authors');
        document.getElementById('bulk-authors').value = '';
        clearError('authors');
    } catch (error) {
        showError('authors', 'Invalid JSON or failed bulk import: ' + error.message);
    }
}

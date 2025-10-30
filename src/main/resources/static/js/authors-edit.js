// (c) Copyright 2025 by Muczynski
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
        const newAuthor = await postData('/api/authors', { name, dateOfBirth, dateOfDeath, religiousAffiliation, birthCountry, nationality, briefBiography });
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
        await editAuthor(newAuthor.id);
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
    document.getElementById('current-author-id').value = id;
    const btn = document.getElementById('add-author-btn');
    btn.textContent = 'Update Author';
    btn.onclick = () => updateAuthor(id);

    document.getElementById('cancel-author-btn').style.display = 'inline-block';
    document.getElementById('add-author-photo-btn').style.display = 'inline-block';

    showAuthorList(false);

    const photos = await fetchData(`/api/authors/${id}/photos`);
    displayAuthorPhotos(photos, id);
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
        await loadAuthors();
        await populateBookDropdowns();
        resetAuthorForm();
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to update author: ' + error.message);
    }
}

function resetAuthorForm() {
    document.getElementById('new-author-name').value = '';
    document.getElementById('new-author-dob').value = '';
    document.getElementById('new-author-dod').value = '';
    document.getElementById('new-author-religion').value = '';
    document.getElementById('new-author-country').value = '';
    document.getElementById('new-author-nationality').value = '';
    document.getElementById('new-author-bio').value = '';
    document.getElementById('current-author-id').value = '';

    const btn = document.getElementById('add-author-btn');
    btn.textContent = 'Add Author';
    btn.onclick = addAuthor;

    document.getElementById('cancel-author-btn').style.display = 'none';
    document.getElementById('add-author-photo-btn').style.display = 'none';
    document.getElementById('author-photos-container').style.display = 'none';

    showAuthorList(true);
    clearError('authors');
}

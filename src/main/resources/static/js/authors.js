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
                const img = document.createElement('img');
                img.src = `/api/photos/${author.firstPhotoId}/thumbnail?width=50`;
                img.alt = `Photo of ${author.name}`;
                img.style.height = '50px';
                tdPhoto.appendChild(img);
            }
            tr.appendChild(tdPhoto);

            const tdName = document.createElement('td');
            tdName.setAttribute('data-test', 'author-name');
            tdName.textContent = author.name;
            tr.appendChild(tdName);

            const tdActions = document.createElement('td');
            if (isLibrarian) {
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

    const authorTable = document.querySelector('[data-test="author-table"]');
    if (authorTable) {
        authorTable.style.display = 'none';
    }

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

    const authorTable = document.querySelector('[data-test="author-table"]');
    if (authorTable) {
        authorTable.style.display = 'table';
    }
    clearError('authors');
}

function displayAuthorPhotos(photos, authorId) {
    const photosContainer = document.getElementById('author-photos-container');
    const photosDiv = document.getElementById('author-photos');
    photosDiv.innerHTML = '';

    if (photos && photos.length > 0) {
        photosContainer.style.display = 'block';
        photosDiv.className = 'book-photo-thumbnails';
        photos.forEach((photo, index) => {
            const thumbnail = document.createElement('div');
            thumbnail.className = 'book-photo-thumbnail';
            thumbnail.setAttribute('data-photo-id', photo.id);

            const img = document.createElement('img');
            img.setAttribute('data-test', 'author-photo');
            img.src = `/api/photos/${photo.id}/thumbnail?width=300`;
            if (photo.rotation && photo.rotation !== 0) {
                img.style.transform = `rotate(${photo.rotation}deg)`;
            }
            thumbnail.appendChild(img);

            const rotateCcwBtn = document.createElement('button');
            rotateCcwBtn.className = 'photo-overlay-btn rotate-ccw-btn';
            rotateCcwBtn.innerHTML = '↺';
            rotateCcwBtn.title = 'Rotate counterclockwise 90 degrees';
            rotateCcwBtn.onclick = () => rotateAuthorPhotoCCW(authorId, photo.id);
            thumbnail.appendChild(rotateCcwBtn);

            const rotateCwBtn = document.createElement('button');
            rotateCwBtn.className = 'photo-overlay-btn rotate-cw-btn';
            rotateCwBtn.innerHTML = '↻';
            rotateCwBtn.title = 'Rotate clockwise 90 degrees';
            rotateCwBtn.onclick = () => rotateAuthorPhotoCW(authorId, photo.id);
            thumbnail.appendChild(rotateCwBtn);

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'photo-overlay-btn delete-btn';
            deleteBtn.innerHTML = '🗑️';
            deleteBtn.title = 'Delete Photo';
            deleteBtn.onclick = () => deleteAuthorPhoto(authorId, photo.id);
            thumbnail.appendChild(deleteBtn);

            if (index > 0) {
                const moveLeftBtn = document.createElement('button');
                moveLeftBtn.className = 'photo-overlay-btn move-left-btn';
                moveLeftBtn.innerHTML = '←';
                moveLeftBtn.title = 'Move Photo Left';
                moveLeftBtn.onclick = () => moveAuthorPhotoLeft(authorId, photo.id);
                thumbnail.appendChild(moveLeftBtn);
            }

            if (index < photos.length - 1) {
                const moveRightBtn = document.createElement('button');
                moveRightBtn.className = 'photo-overlay-btn move-right-btn';
                moveRightBtn.innerHTML = '→';
                moveRightBtn.title = 'Move Photo Right';
                moveRightBtn.onclick = () => moveAuthorPhotoRight(authorId, photo.id);
                thumbnail.appendChild(moveRightBtn);
            }

            photosDiv.appendChild(thumbnail);
        });
    } else {
        photosContainer.style.display = 'none';
    }
}

async function deleteAuthorPhoto(authorId, photoId) {
    if (!confirm('Are you sure you want to delete this photo?')) return;
    try {
        await deleteData(`/api/authors/${authorId}/photos/${photoId}`);
        const thumbnail = document.querySelector(`.book-photo-thumbnail[data-photo-id='${photoId}']`);
        if (thumbnail) {
            thumbnail.remove();
        }
        const photosDiv = document.getElementById('author-photos');
        if (photosDiv.childElementCount === 0) {
            document.getElementById('author-photos-container').style.display = 'none';
        }
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to delete photo: ' + error.message);
    }
}

async function rotateAuthorPhotoCCW(authorId, photoId) {
    try {
        await putData(`/api/authors/${authorId}/photos/${photoId}/rotate-ccw`, {}, false);
        const photos = await fetchData(`/api/authors/${authorId}/photos`);
        displayAuthorPhotos(photos, authorId);
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to rotate photo counterclockwise: ' + error.message);
    }
}

async function rotateAuthorPhotoCW(authorId, photoId) {
    try {
        await putData(`/api/authors/${authorId}/photos/${photoId}/rotate-cw`, {}, false);
        const photos = await fetchData(`/api/authors/${authorId}/photos`);
        displayAuthorPhotos(photos, authorId);
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to rotate photo clockwise: ' + error.message);
    }
}

async function moveAuthorPhotoLeft(authorId, photoId) {
    try {
        await putData(`/api/authors/${authorId}/photos/${photoId}/move-left`, {}, false);
        const photos = await fetchData(`/api/authors/${authorId}/photos`);
        displayAuthorPhotos(photos, authorId);
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to move photo left: ' + error.message);
    }
}

async function moveAuthorPhotoRight(authorId, photoId) {
    try {
        await putData(`/api/authors/${authorId}/photos/${photoId}/move-right`, {}, false);
        const photos = await fetchData(`/api/authors/${authorId}/photos`);
        displayAuthorPhotos(photos, authorId);
        clearError('authors');
    } catch (error) {
        showError('authors', 'Failed to move photo right: ' + error.message);
    }
}

async function addAuthorPhoto() {
    document.getElementById('author-photo-upload').click();
}

async function handleAuthorPhotoUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    const authorId = document.getElementById('current-author-id').value;
    if (!authorId) {
        showError('authors', 'No author selected for photo upload.');
        return;
    }
    const formData = new FormData();
    formData.append('file', file);

    document.body.style.cursor = 'wait';

    try {
        await postData(`/api/authors/${authorId}/photos`, formData, true);
        const photos = await fetchData(`/api/authors/${authorId}/photos`);
        displayAuthorPhotos(photos, authorId);
        event.target.value = ''; // Reset file input
        clearError('authors');
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    } catch (error) {
        showError('authors', 'Failed to add photo: ' + error.message);
        window.scrollTo({ top: 0, behavior: 'smooth' });
        event.target.value = ''; // Reset file input
    } finally {
        document.body.style.cursor = 'default';
    }
}

document.getElementById('author-photo-upload').addEventListener('change', handleAuthorPhotoUpload);
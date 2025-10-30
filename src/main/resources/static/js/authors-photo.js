// (c) Copyright 2025 by Muczynski
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
            img.src = `/api/photos/${photo.id}/image`;
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
        const photos = await fetchData(`/api/authors/${authorId}/photos`);
        displayAuthorPhotos(photos, authorId);
        await loadAuthors();
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
        await loadAuthors();
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
        await loadAuthors();
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
        await loadAuthors();
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
        await loadAuthors();
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
        await loadAuthors();
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

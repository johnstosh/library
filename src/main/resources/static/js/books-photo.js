// (c) Copyright 2025 by Muczynski
function displayBookPhotos(photos, bookId) {
    const photosContainer = document.getElementById('book-photos-container');
    const photosDiv = document.getElementById('book-photos');
    photosDiv.innerHTML = '';

    if (photos && photos.length > 0) {
        photosContainer.style.display = 'block';
        photosDiv.className = 'book-photo-thumbnails';
        photos.forEach((photo, index) => {
            const thumbnail = document.createElement('div');
            thumbnail.className = 'book-photo-thumbnail';
            thumbnail.setAttribute('data-photo-id', photo.id);

            const img = document.createElement('img');
            img.setAttribute('data-test', 'book-photo');
            img.src = `/api/photos/${photo.id}/image`;
            img.style.width = '300px';
            img.style.height = 'auto';
            thumbnail.appendChild(img);

            // Rotate CCW button (upper left)
            const rotateCcwBtn = document.createElement('button');
            rotateCcwBtn.className = 'photo-overlay-btn rotate-ccw-btn';
            rotateCcwBtn.innerHTML = '↺';
            rotateCcwBtn.title = 'Rotate counterclockwise 90 degrees';
            rotateCcwBtn.onclick = () => rotatePhotoCCW(bookId, photo.id);
            thumbnail.appendChild(rotateCcwBtn);

            // Rotate CW button (upper right)
            const rotateCwBtn = document.createElement('button');
            rotateCwBtn.className = 'photo-overlay-btn rotate-cw-btn';
            rotateCwBtn.innerHTML = '↻';
            rotateCwBtn.title = 'Rotate clockwise 90 degrees';
            rotateCwBtn.onclick = () => rotatePhotoCW(bookId, photo.id);
            thumbnail.appendChild(rotateCwBtn);

            // Delete button (top center)
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'photo-overlay-btn delete-btn';
            deleteBtn.innerHTML = '🗑️';
            deleteBtn.title = 'Delete Photo';
            deleteBtn.onclick = () => deleteBookPhoto(bookId, photo.id);
            thumbnail.appendChild(deleteBtn);

            // Edit button (bottom left, near move-left)
            const editBtn = document.createElement('button');
            editBtn.className = 'photo-overlay-btn edit-btn';
            editBtn.innerHTML = '✏️';
            editBtn.title = 'Edit Photo';
            editBtn.onclick = () => editPhoto(bookId, photo.id);
            thumbnail.appendChild(editBtn);

            // Move Left button (bottom left)
            if (index > 0) {
                const moveLeftBtn = document.createElement('button');
                moveLeftBtn.className = 'photo-overlay-btn move-left-btn';
                moveLeftBtn.innerHTML = '←';
                moveLeftBtn.title = 'Move Photo Left';
                moveLeftBtn.onclick = () => movePhotoLeft(bookId, photo.id);
                thumbnail.appendChild(moveLeftBtn);
            }

            // Move Right button (bottom right)
            if (index < photos.length - 1) {
                const moveRightBtn = document.createElement('button');
                moveRightBtn.className = 'photo-overlay-btn move-right-btn';
                moveRightBtn.innerHTML = '→';
                moveRightBtn.title = 'Move Photo Right';
                moveRightBtn.onclick = () => movePhotoRight(bookId, photo.id);
                thumbnail.appendChild(moveRightBtn);
            }

            photosDiv.appendChild(thumbnail);
        });
        return Promise.resolve();
    } else {
        photosContainer.style.display = 'none';
        return Promise.resolve();
    }
}

async function deleteBookPhoto(bookId, photoId) {
    if (!confirm('Are you sure you want to delete this photo?')) return;
    try {
        await deleteData(`/api/books/${bookId}/photos/${photoId}`);
        const thumbnail = document.querySelector(`.book-photo-thumbnail[data-photo-id='${photoId}']`);
        if (thumbnail) {
            thumbnail.remove();
        }
        const photosDiv = document.getElementById('book-photos');
        if (photosDiv.childElementCount === 0) {
            document.getElementById('book-photos-container').style.display = 'none';
            document.getElementById('book-by-photo-btn').style.display = 'none';
        }
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to delete photo: ' + error.message);
    }
}

async function rotatePhotoCCW(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/rotate-ccw`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to rotate photo counterclockwise: ' + error.message);
    }
}

async function rotatePhotoCW(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/rotate-cw`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to rotate photo clockwise: ' + error.message);
    }
}

async function editPhoto(bookId, photoId) {
    // For now, since PhotoDto fields are not specified in UI, prompt for a potential field like caption
    // Assuming PhotoDto has a 'caption' field; adjust as needed
    const caption = prompt('Enter new caption for the photo (or leave blank):');
    if (caption === null) return; // Cancelled

    try {
        const photoDto = { caption: caption || null };
        await putData(`/api/books/${bookId}/photos/${photoId}`, photoDto);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to update photo: ' + error.message);
    }
}

async function movePhotoLeft(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/move-left`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to move photo left: ' + error.message);
    }
}

async function movePhotoRight(bookId, photoId) {
    try {
        await putData(`/api/books/${bookId}/photos/${photoId}/move-right`, {}, false);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        clearError('books');
    } catch (error) {
        showError('books', 'Failed to move photo right: ' + error.message);
    }
}

async function addPhoto() {
    document.getElementById('photo-upload').click();
}

async function handlePhotoUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    const bookId = document.getElementById('current-book-id').value;
    if (!bookId) {
        showError('books', 'No book selected for photo upload.');
        return;
    }
    const formData = new FormData();
    formData.append('file', file);

    document.body.style.cursor = 'wait';

    try {
        await postData(`/api/books/${bookId}/photos`, formData, true);
        const photos = await fetchData(`/api/books/${bookId}/photos`);
        await displayBookPhotos(photos, bookId);
        if (photos && photos.length > 0) {
            document.getElementById('book-by-photo-btn').style.display = 'inline-block';
            const currentId = document.getElementById('current-book-id').value;
            if (currentId) {
                document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(currentId);
            }
        }
        event.target.value = ''; // Reset file input
        clearError('books');
        // On success, scroll to the bottom to show the new photo
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    } catch (error) {
        showError('books', 'Failed to add photo: ' + error.message);
        // On error, scroll to the top to show the error message
        window.scrollTo({ top: 0, behavior: 'smooth' });
        event.target.value = ''; // Reset file input
    } finally {
        document.body.style.cursor = 'default';
    }
}

document.getElementById('photo-upload').addEventListener('change', handlePhotoUpload);

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

            // Edit/Crop button
            const editBtn = document.createElement('button');
            editBtn.className = 'photo-overlay-btn edit-btn';
            editBtn.innerHTML = '✏️';
            editBtn.title = 'Crop Photo';
            editBtn.onclick = () => openCropModalForAuthor(authorId, photo.id);
            thumbnail.appendChild(editBtn);

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

// Variables for Google Photos picker
let authorGooglePhotosPickerSessionId = null;
let authorGooglePhotosPickerPollingInterval = null;

/**
 * Add photo from Google Photos using the Picker API
 */
async function addAuthorPhotoFromGooglePhotos() {
    const authorId = document.getElementById('current-author-id').value;
    if (!authorId) {
        showError('authors', 'No author selected for photo upload.');
        return;
    }

    // Check if user has authorized Google Photos
    try {
        const user = await fetchData('/api/user-settings');
        if (!user.googlePhotosApiKey || user.googlePhotosApiKey.trim() === '') {
            showError('authors', 'Please authorize Google Photos in Settings first.');
            return;
        }

        // Show the Photos Picker
        await showGooglePhotosPickerForAuthor(authorId);
    } catch (error) {
        showError('authors', 'Failed to get authorization: ' + error.message);
    }
}

async function showGooglePhotosPickerForAuthor(authorId) {
    try {
        clearError('authors');
        document.body.style.cursor = 'wait';

        // Create a new picker session
        const response = await fetch('/api/books-from-feed/picker-session', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || response.statusText);
        }

        const session = await response.json();
        authorGooglePhotosPickerSessionId = session.id;

        console.log('[AuthorsPhoto] Picker session created:', authorGooglePhotosPickerSessionId);
        console.log('[AuthorsPhoto] Picker URI:', session.pickerUri);

        // Append /autoclose to automatically close the picker window after selection
        const pickerUriWithAutoClose = session.pickerUri + '/autoclose';

        // Open the picker in a new window
        const pickerWindow = window.open(
            pickerUriWithAutoClose,
            'Google Photos Picker',
            'width=800,height=600,resizable=yes,scrollbars=yes'
        );

        if (!pickerWindow) {
            throw new Error('Popup blocked. Please allow popups for this site.');
        }

        // Start polling for session completion
        startAuthorGooglePhotosPickerPolling(session.id, authorId);

    } catch (error) {
        console.error('[AuthorsPhoto] Failed to show picker:', error);
        showError('authors', 'Failed to open Google Photos picker: ' + error.message);
    } finally {
        document.body.style.cursor = 'default';
    }
}

function startAuthorGooglePhotosPickerPolling(sessionId, authorId) {
    // Clear any existing polling interval
    if (authorGooglePhotosPickerPollingInterval) {
        clearInterval(authorGooglePhotosPickerPollingInterval);
    }

    let pollCount = 0;
    const maxPolls = 120; // Poll for up to 10 minutes (120 * 5 seconds)

    authorGooglePhotosPickerPollingInterval = setInterval(async () => {
        pollCount++;

        if (pollCount >= maxPolls) {
            clearInterval(authorGooglePhotosPickerPollingInterval);
            showError('authors', 'Photo selection timed out. Please try again.');
            return;
        }

        try {
            const response = await fetch(`/api/books-from-feed/picker-session/${sessionId}`);
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || response.statusText);
            }

            const sessionData = await response.json();

            // Check if user has completed selection
            console.log('[AuthorsPhoto] Polling session... mediaItemsSet:', sessionData.mediaItemsSet);

            if (sessionData.mediaItemsSet === true) {
                clearInterval(authorGooglePhotosPickerPollingInterval);
                authorGooglePhotosPickerPollingInterval = null;

                console.log('[AuthorsPhoto] User completed photo selection');

                // Process the selected photos
                await handleAuthorGooglePhotosPickerResults(sessionId, authorId);
            }
        } catch (error) {
            console.error('[AuthorsPhoto] Polling error:', error);
            // Continue polling unless it's a fatal error
            if (error.message.includes('404') || error.message.includes('403')) {
                clearInterval(authorGooglePhotosPickerPollingInterval);
                showError('authors', 'Session expired or unauthorized: ' + error.message);
            }
        }
    }, 5000); // Poll every 5 seconds
}

async function handleAuthorGooglePhotosPickerResults(sessionId, authorId) {
    try {
        document.body.style.cursor = 'wait';

        // Get the list of selected media items
        const response = await fetchData(`/api/books-from-feed/picker-session/${sessionId}/media-items`);
        const mediaItems = response.mediaItems || [];

        console.log('[AuthorsPhoto] User selected', mediaItems.length, 'photos');

        if (mediaItems.length === 0) {
            clearError('authors');
            return;
        }

        // Transform Picker API response to match backend expectations
        const photos = mediaItems.map(item => ({
            id: item.id,
            url: item.mediaFile.baseUrl,
            mimeType: item.mediaFile.mimeType
        }));

        // Send to backend to add photos to author
        const result = await postData(`/api/authors/${authorId}/photos/from-google-photos`, { photos });

        if (result.savedCount > 0) {
            // Reload photos for the author
            const updatedPhotos = await fetchData(`/api/authors/${authorId}/photos`);
            displayAuthorPhotos(updatedPhotos, authorId);
            await loadAuthors();

            clearError('authors');
            // Scroll to bottom to show new photos
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }

        if (result.failedCount > 0) {
            showError('authors', `Added ${result.savedCount} photo(s), but ${result.failedCount} failed.`);
        }

    } catch (error) {
        console.error('[AuthorsPhoto] Failed to save photos from Google:', error);
        showError('authors', 'Failed to add photos from Google Photos: ' + error.message);
    } finally {
        document.body.style.cursor = 'default';
    }
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

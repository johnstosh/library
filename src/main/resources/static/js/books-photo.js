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
    // Open the crop modal for the photo
    openCropModalForBook(bookId, photoId);
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

// Variables for Google Photos picker
let googlePhotosPickerSessionId = null;
let googlePhotosPickerPollingInterval = null;

/**
 * Add photo from Google Photos using the Picker API
 */
async function addPhotoFromGooglePhotos() {
    const bookId = document.getElementById('current-book-id').value;
    if (!bookId) {
        showError('books', 'No book selected for photo upload.');
        return;
    }

    // Check if user has authorized Google Photos
    try {
        const user = await fetchData('/api/user-settings');
        if (!user.googlePhotosApiKey || user.googlePhotosApiKey.trim() === '') {
            showError('books', 'Please authorize Google Photos in Settings first.');
            return;
        }

        // Show the Photos Picker
        await showGooglePhotosPickerForBook(bookId);
    } catch (error) {
        showError('books', 'Failed to get authorization: ' + error.message);
    }
}

async function showGooglePhotosPickerForBook(bookId) {
    try {
        clearError('books');
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
        googlePhotosPickerSessionId = session.id;

        console.log('[BooksPhoto] Picker session created:', googlePhotosPickerSessionId);
        console.log('[BooksPhoto] Picker URI:', session.pickerUri);

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
        startGooglePhotosPickerPolling(session.id, bookId);

    } catch (error) {
        console.error('[BooksPhoto] Failed to show picker:', error);
        showError('books', 'Failed to open Google Photos picker: ' + error.message);
    } finally {
        document.body.style.cursor = 'default';
    }
}

function startGooglePhotosPickerPolling(sessionId, bookId) {
    // Clear any existing polling interval
    if (googlePhotosPickerPollingInterval) {
        clearInterval(googlePhotosPickerPollingInterval);
    }

    let pollCount = 0;
    const maxPolls = 120; // Poll for up to 10 minutes (120 * 5 seconds)

    googlePhotosPickerPollingInterval = setInterval(async () => {
        pollCount++;

        if (pollCount >= maxPolls) {
            clearInterval(googlePhotosPickerPollingInterval);
            showError('books', 'Photo selection timed out. Please try again.');
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
            console.log('[BooksPhoto] Polling session... mediaItemsSet:', sessionData.mediaItemsSet);

            if (sessionData.mediaItemsSet === true) {
                clearInterval(googlePhotosPickerPollingInterval);
                googlePhotosPickerPollingInterval = null;

                console.log('[BooksPhoto] User completed photo selection');

                // Process the selected photos
                await handleGooglePhotosPickerResults(sessionId, bookId);
            }
        } catch (error) {
            console.error('[BooksPhoto] Polling error:', error);
            // Continue polling unless it's a fatal error
            if (error.message.includes('404') || error.message.includes('403')) {
                clearInterval(googlePhotosPickerPollingInterval);
                showError('books', 'Session expired or unauthorized: ' + error.message);
            }
        }
    }, 5000); // Poll every 5 seconds
}

async function handleGooglePhotosPickerResults(sessionId, bookId) {
    try {
        document.body.style.cursor = 'wait';

        // Get the list of selected media items
        const response = await fetchData(`/api/books-from-feed/picker-session/${sessionId}/media-items`);
        const mediaItems = response.mediaItems || [];

        console.log('[BooksPhoto] User selected', mediaItems.length, 'photos');

        if (mediaItems.length === 0) {
            clearError('books');
            return;
        }

        // Transform Picker API response to match backend expectations
        const photos = mediaItems.map(item => ({
            id: item.id,
            url: item.mediaFile.baseUrl,
            mimeType: item.mediaFile.mimeType
        }));

        // Send to backend to add photos to book
        const result = await postData(`/api/books/${bookId}/photos/from-google-photos`, { photos });

        if (result.savedCount > 0) {
            // Reload photos for the book
            const updatedPhotos = await fetchData(`/api/books/${bookId}/photos`);
            await displayBookPhotos(updatedPhotos, bookId);

            // Show book-by-photo button if photos exist
            if (updatedPhotos && updatedPhotos.length > 0) {
                document.getElementById('book-by-photo-btn').style.display = 'inline-block';
                document.getElementById('book-by-photo-btn').onclick = () => generateBookByPhoto(bookId);
            }

            clearError('books');
            // Scroll to bottom to show new photos
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }

        if (result.failedCount > 0) {
            showError('books', `Added ${result.savedCount} photo(s), but ${result.failedCount} failed.`);
        }

    } catch (error) {
        console.error('[BooksPhoto] Failed to save photos from Google:', error);
        showError('books', 'Failed to add photos from Google Photos: ' + error.message);
    } finally {
        document.body.style.cursor = 'default';
    }
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

// (c) Copyright 2025 by Muczynski

// Global state for cropping
let cropperInstance = null;
let currentCropPhotoId = null;
let currentCropEntityType = null; // 'book' or 'author'
let currentCropEntityId = null;
let cropModal = null;

/**
 * Initialize the crop modal when DOM is ready
 */
document.addEventListener('DOMContentLoaded', function() {
    cropModal = new bootstrap.Modal(document.getElementById('crop-modal'));

    // Set up event listeners
    document.getElementById('crop-auto-btn').addEventListener('click', autoCrop);
    document.getElementById('crop-reset-btn').addEventListener('click', resetCrop);
    document.getElementById('crop-save-btn').addEventListener('click', saveCrop);

    // Aspect ratio buttons
    document.getElementById('crop-ratio-free').addEventListener('click', () => setAspectRatio(NaN));
    document.getElementById('crop-ratio-1-1').addEventListener('click', () => setAspectRatio(1));
    document.getElementById('crop-ratio-4-3').addEventListener('click', () => setAspectRatio(4/3));
    document.getElementById('crop-ratio-16-9').addEventListener('click', () => setAspectRatio(16/9));

    // Clean up cropper when modal is hidden
    document.getElementById('crop-modal').addEventListener('hidden.bs.modal', function() {
        if (cropperInstance) {
            cropperInstance.destroy();
            cropperInstance = null;
        }
    });
});

/**
 * Open the crop modal for a book photo
 * @param {number} bookId - The book ID
 * @param {number} photoId - The photo ID
 */
async function openCropModalForBook(bookId, photoId) {
    currentCropEntityType = 'book';
    currentCropEntityId = bookId;
    currentCropPhotoId = photoId;
    await openCropModal(photoId);
}

/**
 * Open the crop modal for an author photo
 * @param {number} authorId - The author ID
 * @param {number} photoId - The photo ID
 */
async function openCropModalForAuthor(authorId, photoId) {
    currentCropEntityType = 'author';
    currentCropEntityId = authorId;
    currentCropPhotoId = photoId;
    await openCropModal(photoId);
}

/**
 * Open the crop modal and initialize the cropper
 * @param {number} photoId - The photo ID
 */
async function openCropModal(photoId) {
    const image = document.getElementById('crop-image');

    // Set the image source
    image.src = `/api/photos/${photoId}/image`;

    // Wait for image to load
    image.onload = function() {
        // Destroy existing cropper if any
        if (cropperInstance) {
            cropperInstance.destroy();
        }

        // Initialize Cropper.js
        cropperInstance = new Cropper(image, {
            viewMode: 1,
            dragMode: 'move',
            autoCropArea: 1,
            restore: false,
            guides: true,
            center: true,
            highlight: true,
            cropBoxMovable: true,
            cropBoxResizable: true,
            toggleDragModeOnDblclick: false,
        });
    };

    // Show the modal
    cropModal.show();
}

/**
 * Auto-crop using smartcrop.js to detect the best crop area
 */
async function autoCrop() {
    if (!cropperInstance) return;

    const image = document.getElementById('crop-image');

    try {
        // Get the natural dimensions of the image
        const imageData = cropperInstance.getImageData();
        const naturalWidth = imageData.naturalWidth;
        const naturalHeight = imageData.naturalHeight;

        // Calculate target dimensions (square crop for best results)
        const minDimension = Math.min(naturalWidth, naturalHeight);

        // Use smartcrop to find the best crop
        const result = await smartcrop.crop(image, {
            width: minDimension,
            height: minDimension,
            minScale: 0.8
        });

        const crop = result.topCrop;

        // Apply the crop to Cropper.js
        cropperInstance.setData({
            x: crop.x,
            y: crop.y,
            width: crop.width,
            height: crop.height
        });

        console.log('[PhotoCrop] Auto-crop applied:', crop);
    } catch (error) {
        console.error('[PhotoCrop] Auto-crop failed:', error);
        alert('Auto-crop failed: ' + error.message);
    }
}

/**
 * Reset the crop to original
 */
function resetCrop() {
    if (cropperInstance) {
        cropperInstance.reset();
    }
}

/**
 * Set the aspect ratio for cropping
 * @param {number} ratio - The aspect ratio (NaN for free)
 */
function setAspectRatio(ratio) {
    if (cropperInstance) {
        cropperInstance.setAspectRatio(ratio);
    }

    // Update button states
    const buttons = ['crop-ratio-free', 'crop-ratio-1-1', 'crop-ratio-4-3', 'crop-ratio-16-9'];
    buttons.forEach(id => {
        const btn = document.getElementById(id);
        btn.classList.remove('active', 'btn-primary');
        btn.classList.add('btn-outline-secondary');
    });

    // Highlight active button
    let activeId;
    if (isNaN(ratio)) activeId = 'crop-ratio-free';
    else if (ratio === 1) activeId = 'crop-ratio-1-1';
    else if (Math.abs(ratio - 4/3) < 0.01) activeId = 'crop-ratio-4-3';
    else if (Math.abs(ratio - 16/9) < 0.01) activeId = 'crop-ratio-16-9';

    if (activeId) {
        const activeBtn = document.getElementById(activeId);
        activeBtn.classList.remove('btn-outline-secondary');
        activeBtn.classList.add('active', 'btn-primary');
    }
}

/**
 * Save the cropped image
 */
async function saveCrop() {
    if (!cropperInstance || !currentCropPhotoId) {
        alert('No image to save');
        return;
    }

    try {
        document.body.style.cursor = 'wait';

        // Get cropped canvas
        const canvas = cropperInstance.getCroppedCanvas({
            maxWidth: 4096,
            maxHeight: 4096,
            imageSmoothingEnabled: true,
            imageSmoothingQuality: 'high'
        });

        // Convert to blob
        const blob = await new Promise(resolve => {
            canvas.toBlob(resolve, 'image/jpeg', 0.95);
        });

        // Create form data
        const formData = new FormData();
        formData.append('file', blob, 'cropped.jpg');

        // Send to backend
        const response = await fetch(`/api/photos/${currentCropPhotoId}/crop`, {
            method: 'PUT',
            body: formData
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || response.statusText);
        }

        console.log('[PhotoCrop] Cropped image saved successfully');

        // Hide the modal
        cropModal.hide();

        // Refresh the photos display
        if (currentCropEntityType === 'book') {
            const photos = await fetchData(`/api/books/${currentCropEntityId}/photos`);
            await displayBookPhotos(photos, currentCropEntityId);
            clearError('books');
        } else if (currentCropEntityType === 'author') {
            const photos = await fetchData(`/api/authors/${currentCropEntityId}/photos`);
            displayAuthorPhotos(photos, currentCropEntityId);
            clearError('authors');
        }

    } catch (error) {
        console.error('[PhotoCrop] Failed to save cropped image:', error);
        alert('Failed to save cropped image: ' + error.message);
    } finally {
        document.body.style.cursor = 'default';
    }
}

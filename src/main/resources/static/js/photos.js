// (c) Copyright 2025 by Muczynski
// Photo Export Management

/**
 * Load the photos section and display export status
 */
async function loadPhotosSection() {
    console.log('[Photos] Loading photos section');
    await loadPhotoExportStatus();
}

/**
 * Load and display photo export statistics and details
 */
async function loadPhotoExportStatus() {
    try {
        console.log('[Photos] Fetching export status...');

        // Fetch statistics
        const stats = await fetchData('/api/photo-export/stats');
        console.log('[Photos] Stats:', stats);

        // Update statistics display
        document.getElementById('stats-total').textContent = stats.total || 0;
        document.getElementById('stats-exported').textContent = stats.exported || 0;
        document.getElementById('stats-imported').textContent = stats.imported || 0;
        document.getElementById('stats-pending-export').textContent = stats.pendingExport || 0;
        document.getElementById('stats-pending-import').textContent = stats.pendingImport || 0;
        document.getElementById('stats-failed').textContent = stats.failed || 0;

        // Update album information
        const albumNameElement = document.getElementById('album-name');
        if (albumNameElement) {
            if (stats.albumName) {
                albumNameElement.textContent = stats.albumName;
            } else {
                albumNameElement.textContent = '(Not configured)';
                albumNameElement.classList.remove('text-primary');
                albumNameElement.classList.add('text-muted');
            }
        }

        // Fetch photo details
        const photos = await fetchData('/api/photo-export/photos');
        console.log('[Photos] Loaded', photos.length, 'photos');

        // Render photos table
        renderPhotosTable(photos);

    } catch (error) {
        console.error('[Photos] Failed to load export status:', error);
        showError('photos', 'Failed to load export status: ' + error.message);
    }
}

/**
 * Render the photos table with export status
 */
function renderPhotosTable(photos) {
    const tbody = document.getElementById('photos-table-body');
    tbody.innerHTML = '';

    if (photos.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">No photos in the database</td></tr>';
        return;
    }

    photos.forEach(photo => {
        const row = document.createElement('tr');

        // ID
        const idCell = document.createElement('td');
        idCell.textContent = photo.id;
        row.appendChild(idCell);

        // Title/Author column
        const titleCell = document.createElement('td');
        if (photo.bookTitle) {
            // Book photo: show title on first line, author on second line
            const titleSpan = document.createElement('span');
            titleSpan.textContent = photo.bookTitle;
            titleSpan.style.fontWeight = 'bold';
            titleCell.appendChild(titleSpan);

            if (photo.bookAuthorName && photo.bookAuthorName.trim() !== '') {
                titleCell.appendChild(document.createElement('br'));
                const authorSpan = document.createElement('span');
                authorSpan.textContent = photo.bookAuthorName;
                authorSpan.style.fontSize = '0.9em';
                authorSpan.style.color = '#6c757d'; // Bootstrap's text-muted color
                titleCell.appendChild(authorSpan);
            }
        } else if (photo.authorName) {
            // Author photo: show author name
            const authorSpan = document.createElement('span');
            authorSpan.textContent = photo.authorName;
            authorSpan.style.fontWeight = 'bold';
            titleCell.appendChild(authorSpan);
        } else {
            // Unknown photo: show caption or ID
            titleCell.textContent = photo.caption || `Photo #${photo.id}`;
        }
        row.appendChild(titleCell);

        // LOC Call Number column - formatted for spine display with each component on its own line
        const locCell = document.createElement('td');
        if (photo.bookLocNumber) {
            const locCode = document.createElement('code');
            locCode.innerHTML = window.formatLocForSpine(photo.bookLocNumber);
            locCode.className = 'text-success';
            locCell.appendChild(locCode);
        } else {
            const locSpan = document.createElement('span');
            locSpan.textContent = '-';
            locSpan.className = 'text-muted';
            locCell.appendChild(locSpan);
        }
        row.appendChild(locCell);

        // Status
        const statusCell = document.createElement('td');
        const statusBadge = document.createElement('span');
        statusBadge.classList.add('badge');

        switch (photo.exportStatus) {
            case 'COMPLETED':
                statusBadge.classList.add('bg-success');
                statusBadge.textContent = 'Completed';
                break;
            case 'FAILED':
                statusBadge.classList.add('bg-danger');
                statusBadge.textContent = 'Failed';
                if (photo.exportErrorMessage) {
                    statusBadge.title = photo.exportErrorMessage;
                }
                break;
            case 'IN_PROGRESS':
                statusBadge.classList.add('bg-info');
                statusBadge.textContent = 'In Progress';
                break;
            case 'PENDING':
            default:
                statusBadge.classList.add('bg-warning');
                statusBadge.textContent = 'Pending';
                break;
        }

        statusCell.appendChild(statusBadge);
        row.appendChild(statusCell);

        // Exported At
        const exportedAtCell = document.createElement('td');
        if (photo.exportedAt) {
            const date = new Date(photo.exportedAt);
            exportedAtCell.textContent = date.toLocaleString();
        } else {
            exportedAtCell.textContent = '-';
        }
        row.appendChild(exportedAtCell);

        // Permanent ID
        const permanentIdCell = document.createElement('td');
        if (photo.permanentId) {
            const idSpan = document.createElement('span');
            idSpan.classList.add('font-monospace', 'small');
            idSpan.textContent = photo.permanentId.substring(0, 20) + '...';
            idSpan.title = photo.permanentId;
            permanentIdCell.appendChild(idSpan);
        } else {
            permanentIdCell.textContent = '-';
        }
        row.appendChild(permanentIdCell);

        // Actions
        const actionsCell = document.createElement('td');
        actionsCell.classList.add('d-flex', 'gap-1', 'flex-wrap');

        // Export button - show if photo has image but no permanentId
        if (photo.hasImage && !photo.permanentId) {
            const exportBtn = document.createElement('button');
            exportBtn.classList.add('btn', 'btn-sm', 'btn-primary');
            exportBtn.textContent = 'Export';
            exportBtn.title = 'Export to Google Photos';
            exportBtn.onclick = () => exportSinglePhoto(photo.id);
            actionsCell.appendChild(exportBtn);
        }

        // Import button - show if photo has permanentId but no image
        if (photo.permanentId && !photo.hasImage) {
            const importBtn = document.createElement('button');
            importBtn.classList.add('btn', 'btn-sm', 'btn-success');
            importBtn.textContent = 'Import';
            importBtn.title = 'Import from Google Photos';
            importBtn.onclick = () => importSinglePhoto(photo.id);
            actionsCell.appendChild(importBtn);
        }

        // View button - show if exported
        if (photo.permanentId) {
            const viewBtn = document.createElement('a');
            viewBtn.classList.add('btn', 'btn-sm', 'btn-outline-primary');
            viewBtn.textContent = 'View';
            viewBtn.title = 'View in Google Photos';
            viewBtn.href = `https://photos.google.com/lr/photo/${photo.permanentId}`;
            viewBtn.target = '_blank';
            actionsCell.appendChild(viewBtn);
        }

        // Verify button - show if has permanentId
        if (photo.permanentId) {
            const verifyBtn = document.createElement('button');
            verifyBtn.classList.add('btn', 'btn-sm', 'btn-outline-info');
            verifyBtn.textContent = 'Verify';
            verifyBtn.title = 'Verify permanent ID still works';
            verifyBtn.onclick = () => verifyPhoto(photo.id);
            actionsCell.appendChild(verifyBtn);
        }

        // Unlink button - show if has permanentId
        if (photo.permanentId) {
            const unlinkBtn = document.createElement('button');
            unlinkBtn.classList.add('btn', 'btn-sm', 'btn-outline-warning');
            unlinkBtn.textContent = 'Unlink';
            unlinkBtn.title = 'Remove permanent ID';
            unlinkBtn.onclick = () => unlinkPhoto(photo.id);
            actionsCell.appendChild(unlinkBtn);
        }

        // Delete button (trash icon)
        const deleteBtn = document.createElement('button');
        deleteBtn.classList.add('btn', 'btn-sm', 'btn-outline-danger');
        deleteBtn.innerHTML = '&#128465;'; // Trash can emoji
        deleteBtn.title = 'Delete photo';
        deleteBtn.onclick = () => deletePhotoWithUndo(photo.id, row, actionsCell, deleteBtn);
        actionsCell.appendChild(deleteBtn);

        row.appendChild(actionsCell);

        tbody.appendChild(row);
    });
}

/**
 * Export all pending photos
 */
async function exportAllPhotos() {
    const exportBtn = document.getElementById('export-all-photos-btn');

    try {
        // Get all photos from the current table
        const photos = await fetchData('/api/photo-export/photos');

        // Filter to get only pending export photos (hasImage && !permanentId)
        const pendingPhotos = photos.filter(photo => photo.hasImage && !photo.permanentId);

        if (pendingPhotos.length === 0) {
            showInfo('photos', 'No pending photos to export.');
            return;
        }

        const confirmExport = confirm(`Are you sure you want to export ${pendingPhotos.length} pending photo(s)? This may take a while.`);
        if (!confirmExport) {
            return;
        }

        console.log('[Photos] Exporting', pendingPhotos.length, 'pending photos');

        // Show spinner on button
        showButtonSpinner(exportBtn, 'Exporting...');

        let successCount = 0;
        let failureCount = 0;

        // Loop through each pending photo and export it
        for (let i = 0; i < pendingPhotos.length; i++) {
            const photo = pendingPhotos[i];
            const photoNum = i + 1;

            try {
                showInfo('photos', `Exporting photo ${photoNum} of ${pendingPhotos.length} (ID: ${photo.id})...`);

                // Update button text with progress (recreate spinner element each time)
                if (exportBtn) {
                    const spinner = document.createElement('span');
                    spinner.className = 'spinner-border spinner-border-sm me-1';
                    spinner.setAttribute('role', 'status');
                    spinner.setAttribute('aria-hidden', 'true');
                    exportBtn.innerHTML = '';
                    exportBtn.appendChild(spinner);
                    exportBtn.appendChild(document.createTextNode(`Exporting ${photoNum}/${pendingPhotos.length}...`));
                }

                // Export the photo
                const result = await fetchData(`/api/photo-export/export/${photo.id}`, {
                    method: 'POST'
                });

                console.log(`[Photos] Exported photo ${photo.id}:`, result);
                successCount++;

                // Update the table to show the new status
                await loadPhotoExportStatus();

            } catch (error) {
                console.error(`[Photos] Failed to export photo ${photo.id}:`, error);
                failureCount++;
                // Continue with next photo even if this one failed
            }
        }

        // Show final results
        if (failureCount === 0) {
            showSuccess('photos', `Successfully exported all ${successCount} photo(s)!`);
        } else {
            showError('photos', `Export completed: ${successCount} succeeded, ${failureCount} failed.`);
        }

    } catch (error) {
        console.error('[Photos] Failed to export photos:', error);
        showError('photos', 'Failed to export photos: ' + error.message);
    } finally {
        // Hide spinner and restore button
        if (exportBtn) {
            hideButtonSpinner(exportBtn, 'Export All Pending Photos');
        }
    }
}

/**
 * Export a single photo
 */
async function exportSinglePhoto(photoId) {
    try {
        console.log('[Photos] Backing up photo ID:', photoId);

        showInfo('photos', `Backing up photo #${photoId}...`);

        const result = await fetchData(`/api/photo-export/export/${photoId}`, {
            method: 'POST'
        });

        console.log('[Photos] Export result:', result);

        showSuccess('photos', result.message || 'Photo exported successfully!');

        // Reload the export status
        await loadPhotoExportStatus();

    } catch (error) {
        console.error('[Photos] Failed to export photo:', error);
        showError('photos', 'Failed to export photo: ' + error.message);
    }
}

/**
 * Import all pending photos from Google Photos
 */
async function importAllPhotos() {
    const importBtn = document.getElementById('import-all-photos-btn');

    try {
        // Get all photos from the current table
        const photos = await fetchData('/api/photo-export/photos');

        // Filter to get only pending import photos (permanentId && !hasImage)
        const pendingPhotos = photos.filter(photo => photo.permanentId && !photo.hasImage);

        if (pendingPhotos.length === 0) {
            showInfo('photos', 'No pending photos to import.');
            return;
        }

        const confirmImport = confirm(`Are you sure you want to import ${pendingPhotos.length} pending photo(s) from Google Photos? This may take a while.`);
        if (!confirmImport) {
            return;
        }

        console.log('[Photos] Importing', pendingPhotos.length, 'pending photos');

        // Show spinner on button
        showButtonSpinner(importBtn, 'Importing...');

        let successCount = 0;
        let failureCount = 0;

        // Loop through each pending photo and import it
        for (let i = 0; i < pendingPhotos.length; i++) {
            const photo = pendingPhotos[i];
            const photoNum = i + 1;

            try {
                showInfo('photos', `Importing photo ${photoNum} of ${pendingPhotos.length} (ID: ${photo.id})...`);

                // Update button text with progress (recreate spinner element each time)
                if (importBtn) {
                    const spinner = document.createElement('span');
                    spinner.className = 'spinner-border spinner-border-sm me-1';
                    spinner.setAttribute('role', 'status');
                    spinner.setAttribute('aria-hidden', 'true');
                    importBtn.innerHTML = '';
                    importBtn.appendChild(spinner);
                    importBtn.appendChild(document.createTextNode(`Importing ${photoNum}/${pendingPhotos.length}...`));
                }

                // Import the photo
                const result = await fetchData(`/api/photo-export/import/${photo.id}`, {
                    method: 'POST'
                });

                console.log(`[Photos] Imported photo ${photo.id}:`, result);
                successCount++;

                // Update the table to show the new status
                await loadPhotoExportStatus();

            } catch (error) {
                console.error(`[Photos] Failed to import photo ${photo.id}:`, error);
                failureCount++;
                // Continue with next photo even if this one failed
            }
        }

        // Show final results
        if (failureCount === 0) {
            showSuccess('photos', `Successfully imported all ${successCount} photo(s)!`);
        } else {
            showError('photos', `Import completed: ${successCount} succeeded, ${failureCount} failed.`);
        }

    } catch (error) {
        console.error('[Photos] Failed to import photos:', error);
        showError('photos', 'Failed to import photos: ' + error.message);
    } finally {
        // Hide spinner and restore button
        if (importBtn) {
            hideButtonSpinner(importBtn, 'Import All Pending Photos');
        }
    }
}

/**
 * Import a single photo from Google Photos
 */
async function importSinglePhoto(photoId) {
    try {
        console.log('[Photos] Importing photo ID:', photoId);

        showInfo('photos', `Importing photo #${photoId} from Google Photos...`);

        const result = await fetchData(`/api/photo-export/import/${photoId}`, {
            method: 'POST'
        });

        console.log('[Photos] Import result:', result);

        showSuccess('photos', result.message || 'Photo imported successfully!');

        // Reload the export status
        await loadPhotoExportStatus();

    } catch (error) {
        console.error('[Photos] Failed to import photo:', error);
        showError('photos', 'Failed to import photo: ' + error.message);
    }
}

/**
 * Verify a photo's permanent ID
 */
async function verifyPhoto(photoId) {
    try {
        console.log('[Photos] Verifying photo ID:', photoId);

        showInfo('photos', `Verifying photo #${photoId}...`);

        const result = await fetchData(`/api/photo-export/verify/${photoId}`, {
            method: 'POST'
        });

        console.log('[Photos] Verify result:', result);

        if (result.valid) {
            showSuccess('photos', `Photo #${photoId} verified: ${result.message}` +
                (result.filename ? ` (${result.filename})` : ''));
        } else {
            showError('photos', `Photo #${photoId} verification failed: ${result.message}`);
        }

    } catch (error) {
        console.error('[Photos] Failed to verify photo:', error);
        showError('photos', 'Failed to verify photo: ' + error.message);
    }
}

/**
 * Unlink a photo by removing its permanent ID
 */
async function unlinkPhoto(photoId) {
    try {
        const confirmUnlink = confirm(`Are you sure you want to unlink photo #${photoId}? This will remove the permanent ID and the photo will need to be re-exported.`);
        if (!confirmUnlink) {
            return;
        }

        console.log('[Photos] Unlinking photo ID:', photoId);

        showInfo('photos', `Unlinking photo #${photoId}...`);

        const result = await fetchData(`/api/photo-export/unlink/${photoId}`, {
            method: 'POST'
        });

        console.log('[Photos] Unlink result:', result);

        showSuccess('photos', result.message || 'Photo unlinked successfully!');

        // Reload the export status
        await loadPhotoExportStatus();

    } catch (error) {
        console.error('[Photos] Failed to unlink photo:', error);
        showError('photos', 'Failed to unlink photo: ' + error.message);
    }
}

/**
 * Delete a photo with undo functionality
 */
async function deletePhotoWithUndo(photoId, row, actionsCell, deleteBtn) {
    try {
        // Call the delete API
        const response = await fetch(`/api/photos/${photoId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Failed to delete photo');
        }

        // Apply strikeout styling to all cells in the row except the actions cell
        row.querySelectorAll('td').forEach(cell => {
            if (cell !== actionsCell) {
                cell.style.textDecoration = 'line-through';
                cell.style.opacity = '0.5';
            }
        });

        // Replace delete button with undo button
        deleteBtn.innerHTML = '&#8634;'; // Undo arrow emoji
        deleteBtn.classList.remove('btn-outline-danger');
        deleteBtn.classList.add('btn-outline-warning');
        deleteBtn.title = 'Undo delete';
        deleteBtn.onclick = () => restorePhotoUndo(photoId, row, actionsCell, deleteBtn);

        showSuccess('photos', `Photo #${photoId} deleted. Click undo to restore.`);

    } catch (error) {
        console.error('[Photos] Failed to delete photo:', error);
        showError('photos', 'Failed to delete photo: ' + error.message);
    }
}

/**
 * Restore a deleted photo (undo)
 */
async function restorePhotoUndo(photoId, row, actionsCell, undoBtn) {
    try {
        // Call the restore API
        const response = await fetch(`/api/photos/${photoId}/restore`, {
            method: 'POST',
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error('Failed to restore photo');
        }

        // Remove strikeout styling from all cells
        row.querySelectorAll('td').forEach(cell => {
            if (cell !== actionsCell) {
                cell.style.textDecoration = '';
                cell.style.opacity = '';
            }
        });

        // Replace undo button with delete button
        undoBtn.innerHTML = '&#128465;'; // Trash can emoji
        undoBtn.classList.remove('btn-outline-warning');
        undoBtn.classList.add('btn-outline-danger');
        undoBtn.title = 'Delete photo';
        undoBtn.onclick = () => deletePhotoWithUndo(photoId, row, actionsCell, undoBtn);

        showSuccess('photos', `Photo #${photoId} restored.`);

    } catch (error) {
        console.error('[Photos] Failed to restore photo:', error);
        showError('photos', 'Failed to restore photo: ' + error.message);
    }
}

/**
 * Show info message
 */
function showInfo(section, message) {
    const resultsDiv = document.getElementById('processing-results');
    if (!resultsDiv) return;

    resultsDiv.innerHTML = `<div class="alert alert-info mt-3">${message}</div>`;
}

/**
 * Show success message
 */
function showSuccess(section, message) {
    const resultsDiv = document.getElementById('processing-results');
    if (!resultsDiv) return;

    resultsDiv.innerHTML = `<div class="alert alert-success mt-3">${message}</div>`;
}

/**
 * Show error message
 */
function showError(section, message) {
    const resultsDiv = document.getElementById('processing-results');
    if (!resultsDiv) return;

    resultsDiv.innerHTML = `<div class="alert alert-danger mt-3">${message}</div>`;
}

/**
 * Clear messages
 */
function clearMessages() {
    const resultsDiv = document.getElementById('processing-results');
    if (resultsDiv) {
        resultsDiv.innerHTML = '';
    }
}

// Expose functions globally for use in other modules and HTML
window.loadPhotosSection = loadPhotosSection;
window.loadPhotoExportStatus = loadPhotoExportStatus;
window.exportAllPhotos = exportAllPhotos;
window.exportSinglePhoto = exportSinglePhoto;
window.importAllPhotos = importAllPhotos;
window.importSinglePhoto = importSinglePhoto;
window.verifyPhoto = verifyPhoto;
window.unlinkPhoto = unlinkPhoto;

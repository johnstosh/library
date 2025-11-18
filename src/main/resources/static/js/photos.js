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
        document.getElementById('stats-completed').textContent = stats.completed || 0;
        document.getElementById('stats-pending').textContent = stats.pending || 0;
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

        // Backed Up At
        const backedUpCell = document.createElement('td');
        if (photo.backedUpAt) {
            const date = new Date(photo.backedUpAt);
            backedUpCell.textContent = date.toLocaleString();
        } else {
            backedUpCell.textContent = '-';
        }
        row.appendChild(backedUpCell);

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
        if (photo.exportStatus !== 'COMPLETED' && photo.exportStatus !== 'IN_PROGRESS') {
            const exportBtn = document.createElement('button');
            exportBtn.classList.add('btn', 'btn-sm', 'btn-primary');
            exportBtn.textContent = 'Export';
            exportBtn.onclick = () => exportSinglePhoto(photo.id);
            actionsCell.appendChild(exportBtn);
        } else if (photo.exportStatus === 'COMPLETED' && photo.permanentId) {
            const viewBtn = document.createElement('a');
            viewBtn.classList.add('btn', 'btn-sm', 'btn-outline-primary');
            viewBtn.textContent = 'View';
            viewBtn.href = `https://photos.google.com/lr/photo/${photo.permanentId}`;
            viewBtn.target = '_blank';
            actionsCell.appendChild(viewBtn);
        }
        row.appendChild(actionsCell);

        tbody.appendChild(row);
    });
}

/**
 * Export all pending photos
 */
async function exportAllPhotos() {
    try {
        const confirmExport = confirm('Are you sure you want to export all pending photos? This may take a while.');
        if (!confirmExport) {
            return;
        }

        showInfo('photos', 'Starting export process... This may take a few minutes.');

        // Disable export button
        const exportBtn = document.getElementById('export-all-photos-btn');
        if (exportBtn) {
            exportBtn.disabled = true;
            exportBtn.textContent = 'Backing up...';
        }

        const result = await fetchData('/api/photo-export/export-all', {
            method: 'POST'
        });

        console.log('[Photos] Export result:', result);

        showSuccess('photos', result.message || 'Export completed successfully!');

        // Reload the export status
        await loadPhotoExportStatus();

    } catch (error) {
        console.error('[Photos] Failed to export photos:', error);
        showError('photos', 'Failed to export photos: ' + error.message);
    } finally {
        // Re-enable export button
        const exportBtn = document.getElementById('export-all-photos-btn');
        if (exportBtn) {
            exportBtn.disabled = false;
            exportBtn.textContent = 'Export All Pending Photos';
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

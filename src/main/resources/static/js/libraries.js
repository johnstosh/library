// (c) Copyright 2025 by Muczynski
async function loadLibraries() {
    try {
        const libraries = await fetchData('/api/libraries');
        const list = document.getElementById('library-list');
        list.innerHTML = '';
        const table = document.createElement('table');
        table.setAttribute('data-test', 'library-table');
        const thead = document.createElement('thead');
        const headRow = document.createElement('tr');
        const libraryHeader = document.createElement('th');
        libraryHeader.textContent = 'Library';
        const actionsHeader = document.createElement('th');
        actionsHeader.textContent = 'Actions';
        headRow.appendChild(libraryHeader);
        headRow.appendChild(actionsHeader);
        thead.appendChild(headRow);
        table.appendChild(thead);
        const tbody = document.createElement('tbody');
        libraries.forEach(library => {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'library-item');
            row.setAttribute('data-entity-id', library.id);
            const nameCell = document.createElement('td');
            const span = document.createElement('span');
            span.setAttribute('data-test', 'library-name');
            span.textContent = `${library.name} (${library.hostname})`;
            nameCell.appendChild(span);
            row.appendChild(nameCell);
            const actionsCell = document.createElement('td');
            if (isLibrarian) {
                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-library-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editLibrary(library.id);
                actionsCell.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-library-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
                delBtn.onclick = () => deleteLibrary(library.id);
                actionsCell.appendChild(delBtn);
            }
            row.appendChild(actionsCell);
            tbody.appendChild(row);
        });
        table.appendChild(tbody);
        list.appendChild(table);
        const pageTitle = document.getElementById('page-title');
        pageTitle.innerHTML = ''; // Clear existing content
        if (libraries.length > 0) {
            pageTitle.appendChild(document.createTextNode(`The ${libraries[0].name} Branch`));
            pageTitle.appendChild(document.createElement('br'));
            const small = document.createElement('small');
            small.textContent = 'of the Sacred Heart Library System';
            pageTitle.appendChild(small);
        } else {
            pageTitle.textContent = 'Library Management';
        }
        await setupImportUI(); // Set up import/export UI
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to load libraries: ' + error.message);
    }
}

async function addLibrary() {
    const name = document.getElementById('new-library-name').value;
    const hostname = document.getElementById('new-library-hostname').value;
    if (!name || !hostname) {
        showError('libraries', 'Name and hostname are required.');
        return;
    }
    try {
        await postData('/api/libraries', { name, hostname });
        document.getElementById('new-library-name').value = '';
        document.getElementById('new-library-hostname').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to add library: ' + error.message);
    }
}

async function editLibrary(id) {
    const data = await fetchData(`/api/libraries/${id}`);
    document.getElementById('new-library-name').value = data.name || '';
    document.getElementById('new-library-hostname').value = data.hostname || '';
    const btn = document.getElementById('add-library-btn');
    btn.textContent = 'Update Library';
    btn.onclick = () => updateLibrary(id);
}

async function updateLibrary(id) {
    const name = document.getElementById('new-library-name').value;
    const hostname = document.getElementById('new-library-hostname').value;
    if (!name || !hostname) {
        showError('libraries', 'Name and hostname are required.');
        return;
    }
    try {
        await putData(`/api/libraries/${id}`, { name, hostname });
        document.getElementById('new-library-name').value = '';
        document.getElementById('new-library-hostname').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        const btn = document.getElementById('add-library-btn');
        btn.textContent = 'Add Library';
        btn.onclick = addLibrary;
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to update library: ' + error.message);
    }
}

async function deleteLibrary(id) {
    if (!confirm('Are you sure you want to delete this library?')) return;
    try {
        await deleteData(`/api/libraries/${id}`);
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to delete library: ' + error.message);
    }
}

async function importJson() {
    const jsonText = document.getElementById('import-json-textarea').value.trim();
    if (!jsonText) {
        showError('libraries', 'Please enter JSON data to import.');
        return;
    }
    let importData;
    try {
        importData = JSON.parse(jsonText);
    } catch (error) {
        showError('libraries', 'Invalid JSON format: ' + error.message);
        return;
    }
    try {
        await postData('/api/import/json', importData);
        document.getElementById('import-json-textarea').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
        alert('Import completed successfully!');
    } catch (error) {
        showError('libraries', 'Failed to import data: ' + error.message);
    }
}

async function exportJson() {
    try {
        const data = await fetchData('/api/import/json');
        const jsonStr = JSON.stringify(data, null, 2);
        const blob = new Blob([jsonStr], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'library-data.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        clearError('libraries');
        alert('Export completed successfully! File downloaded as library-data.json');
    } catch (error) {
        showError('libraries', 'Failed to export data: ' + error.message);
    }
}

async function setupImportUI() {
    if (!isLibrarian) return;

    // Check if import section already exists
    const existing = document.querySelector('[data-test="import-section"]');
    if (existing) {
        return; // Already set up
    }

    // Fetch photo backup stats
    let photoBackupStatus = '';
    try {
        const stats = await fetchData('/api/photo-backup/stats');
        const notBackedUp = (stats.pending || 0) + (stats.failed || 0) + (stats.inProgress || 0);
        const total = stats.total || 0;

        if (total === 0) {
            photoBackupStatus = `<p class="mb-0"><strong>Photo Backup Status:</strong> No photos in database</p>`;
        } else if (notBackedUp === 0) {
            photoBackupStatus = `<p class="mb-0 text-success"><strong>Photo Backup Status:</strong> All ${total} photos backed up to Google Photos ✓</p>`;
        } else {
            photoBackupStatus = `<p class="mb-0 text-warning"><strong>Photo Backup Status:</strong> ${notBackedUp} out of ${total} photos have not been exported to Google Photos</p>`;
        }
    } catch (error) {
        console.error('Failed to load photo backup stats:', error);
        photoBackupStatus = `<p class="mb-0 text-muted"><strong>Photo Backup Status:</strong> Unable to load stats</p>`;
    }

    const list = document.getElementById('library-list');
    const importSection = document.createElement('div');
    importSection.setAttribute('data-test', 'import-section');
    importSection.innerHTML = `
        <h3>Import/Export Database</h3>
        <div class="alert alert-info">
            ${photoBackupStatus}
        </div>
        <div class="card mb-3">
            <div class="card-body">
                <h5>Export JSON Data</h5>
                <p>Export all data (libraries, authors, books, loans, users, photo metadata) to a JSON file.</p>
                <p><small class="text-muted">Note: Photo image bytes are not exported, only metadata (caption, contentType, Google Photos permanentId, backupStatus, etc.)</small></p>
                <button id="export-json-btn" class="btn btn-primary" data-test="export-json-btn" onclick="exportJson()">Export Database to JSON</button>
            </div>
        </div>
        <div class="card mb-3">
            <div class="card-body">
                <h5>Import JSON Data</h5>
                <p>Import libraries, authors, books, loans, users, and photo metadata from JSON.</p>
                <p><small class="text-muted">Note: Photo image bytes are not imported. Photos must be re-downloaded from Google Photos using their permanentId.</small></p>
                <textarea id="import-json-textarea" class="form-control mb-2" rows="10" placeholder='{"libraries": [...], "authors": [...], "photos": [...]}'></textarea>
                <button id="import-json-btn" class="btn btn-warning" data-test="import-json-btn" onclick="importJson()">Import JSON to Database</button>
            </div>
        </div>
    `;
    list.parentNode.insertBefore(importSection, list.nextSibling);
}

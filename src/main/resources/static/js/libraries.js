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
                // Edit button (icon-only)
                const editBtn = document.createElement('button');
                editBtn.className = 'btn btn-sm btn-outline-secondary me-1';
                editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
                editBtn.title = 'Edit';
                editBtn.setAttribute('data-test', 'edit-library-btn');
                editBtn.onclick = () => editLibrary(library.id);
                actionsCell.appendChild(editBtn);

                // Delete button (icon-only)
                const delBtn = document.createElement('button');
                delBtn.className = 'btn btn-sm btn-outline-danger';
                delBtn.innerHTML = '<i class="bi bi-trash"></i>';
                delBtn.title = 'Delete';
                delBtn.setAttribute('data-test', 'delete-library-btn');
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

    const btn = document.getElementById('add-library-btn');
    showButtonSpinner(btn, 'Adding...');

    try {
        await postData('/api/libraries', { name, hostname });
        document.getElementById('new-library-name').value = '';
        document.getElementById('new-library-hostname').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to add library: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Add Library');
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

    const btn = document.getElementById('add-library-btn');
    showButtonSpinner(btn, 'Updating...');

    try {
        await putData(`/api/libraries/${id}`, { name, hostname });
        document.getElementById('new-library-name').value = '';
        document.getElementById('new-library-hostname').value = '';
        await loadLibraries();
        await populateBookDropdowns();
        btn.textContent = 'Add Library';
        btn.onclick = addLibrary;
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to update library: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Add Library');
        btn.onclick = addLibrary;
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
    const fileInput = document.getElementById('import-json-file');
    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
        showError('libraries', 'Please select a JSON file to import.');
        return;
    }

    const file = fileInput.files[0];
    let jsonText;
    try {
        jsonText = await file.text();
    } catch (error) {
        showError('libraries', 'Failed to read file: ' + error.message);
        return;
    }

    let importData;
    try {
        importData = JSON.parse(jsonText);
    } catch (error) {
        showError('libraries', 'Invalid JSON format: ' + error.message);
        return;
    }

    const btn = document.getElementById('import-json-btn');
    showButtonSpinner(btn, 'Importing...');

    try {
        await postData('/api/import/json', importData);
        fileInput.value = ''; // Clear the file input
        await loadLibraries();
        await populateBookDropdowns();
        clearError('libraries');
        alert('Import completed successfully!');
    } catch (error) {
        showError('libraries', 'Failed to import data: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Import JSON to Database');
    }
}

async function exportJson() {
    const btn = document.getElementById('export-json-btn');
    showButtonSpinner(btn, 'Exporting...');

    try {
        // Get library name, books, and authors for filename
        const [libraries, books, authors] = await Promise.all([
            fetchData('/api/libraries'),
            fetchData('/api/books'),
            fetchData('/api/authors')
        ]);

        let libraryName = 'library';
        if (libraries && libraries.length > 0) {
            // Sanitize library name for use as filename
            libraryName = libraries[0].name
                .toLowerCase()
                .replace(/[^a-z0-9]+/g, '-')
                .replace(/^-+|-+$/g, '');
        }

        const bookCount = books ? books.length : 0;
        const authorCount = authors ? authors.length : 0;
        const filename = `${libraryName}-${bookCount}-books-${authorCount}-authors`;

        const data = await fetchData('/api/import/json');
        const jsonStr = JSON.stringify(data, null, 2);
        const blob = new Blob([jsonStr], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${filename}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        clearError('libraries');
    } catch (error) {
        showError('libraries', 'Failed to export data: ' + error.message);
    } finally {
        hideButtonSpinner(btn, 'Export Database to JSON');
    }
}

async function setupImportUI() {
    if (!isLibrarian) return;

    // Check if import section already exists
    const existing = document.querySelector('[data-test="import-section"]');
    if (existing) {
        return; // Already set up
    }

    const list = document.getElementById('library-list');
    const importSection = document.createElement('div');
    importSection.setAttribute('data-test', 'import-section');
    importSection.innerHTML = `
        <h3>Import/Export Database</h3>
        <div class="card mb-3">
            <div class="card-body">
                <h5>Export JSON Data</h5>
                <p>Export all data (libraries, authors, books, loans, users, photo metadata) to a JSON file.</p>
                <p><small class="text-muted">Note: Photo image bytes are not exported, only metadata (caption, contentType, Google Photos permanentId, exportStatus, etc.)</small></p>
                <button id="export-json-btn" class="btn btn-primary" data-test="export-json-btn" onclick="exportJson()">Export Database to JSON</button>
            </div>
        </div>
        <div class="card mb-3">
            <div class="card-body">
                <h5>Import JSON Data</h5>
                <p>Import libraries, authors, books, loans, users, and photo metadata from JSON.</p>
                <p><small class="text-muted">Note: Photo image bytes are not imported. Photos must be re-downloaded from Google Photos using their permanentId.</small></p>
                <div class="mb-2">
                    <input type="file" id="import-json-file" class="form-control" accept=".json,application/json" data-test="import-json-file">
                </div>
                <button id="import-json-btn" class="btn btn-warning" data-test="import-json-btn" onclick="importJson()">Import JSON to Database</button>
            </div>
        </div>
    `;
    list.parentNode.insertBefore(importSection, list.nextSibling);
}

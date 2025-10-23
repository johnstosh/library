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

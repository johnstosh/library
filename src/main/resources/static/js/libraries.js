async function loadLibraries() {
    try {
        const libraries = await fetchData('/api/libraries');
        const list = document.getElementById('library-list');
        list.innerHTML = '';
        libraries.forEach(library => {
            const li = document.createElement('li');
            li.setAttribute('data-test', 'library-item');
            li.setAttribute('data-entity-id', library.id);
            const span = document.createElement('span');
            span.setAttribute('data-test', 'library-name');
            span.textContent = `${library.name} (${library.hostname})`;
            li.appendChild(span);
            if (isLibrarian) {
                const viewBtn = document.createElement('button');
                viewBtn.setAttribute('data-test', 'view-library-btn');
                viewBtn.textContent = '🔍';
                viewBtn.title = 'View details';
                viewBtn.onclick = () => viewLibrary(library.id);
                li.appendChild(viewBtn);

                const editBtn = document.createElement('button');
                editBtn.setAttribute('data-test', 'edit-library-btn');
                editBtn.textContent = '✏️';
                editBtn.title = 'Edit';
                editBtn.onclick = () => editLibrary(library.id);
                li.appendChild(editBtn);

                const delBtn = document.createElement('button');
                delBtn.setAttribute('data-test', 'delete-library-btn');
                delBtn.textContent = '🗑️';
                delBtn.title = 'Delete';
                delBtn.onclick = () => deleteLibrary(library.id);
                li.appendChild(delBtn);
            }
            list.appendChild(li);
        });
        const pageTitle = document.getElementById('page-title');
        if (libraries.length > 0) {
            pageTitle.innerHTML = `The ${libraries[0].name} Branch<br><small>of the Sacred Heart Library System</small>`;
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

async function viewLibrary(id) {
    const data = await fetchData(`/api/libraries/${id}`);
    alert(`Library Details:\nID: ${data.id}\nName: ${data.name}\nHostname: ${data.hostname}`);
}

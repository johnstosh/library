async function loadUsers() {
    try {
        const users = await fetchData('/api/users');
        const tableBody = document.getElementById('user-list-body');
        tableBody.innerHTML = '';
        users.forEach(user => {
            const row = document.createElement('tr');
            row.setAttribute('data-test', 'user-item');
            row.setAttribute('data-entity-id', user.id);

            const userCell = document.createElement('td');
            const span = document.createElement('span');
            span.setAttribute('data-test', 'user-name');
            const rolesText = user.roles ? Array.from(user.roles).join(', ') : '';
            span.textContent = `${user.username} (${rolesText})`;
            userCell.appendChild(span);
            row.appendChild(userCell);

            const actionsCell = document.createElement('td');

            const viewBtn = document.createElement('button');
            viewBtn.setAttribute('data-test', 'view-user-btn');
            viewBtn.textContent = '🔍';
            viewBtn.title = 'View details';
            viewBtn.onclick = () => viewUser(user.id);
            actionsCell.appendChild(viewBtn);

            const editBtn = document.createElement('button');
            editBtn.setAttribute('data-test', 'edit-user-btn');
            editBtn.textContent = '✏️';
            editBtn.title = 'Edit';
            editBtn.onclick = () => editUser(user.id);
            actionsCell.appendChild(editBtn);

            const delBtn = document.createElement('button');
            delBtn.setAttribute('data-test', 'delete-user-btn');
            delBtn.textContent = '🗑️';
            delBtn.title = 'Delete';
            delBtn.onclick = () => deleteUser(user.id);
            actionsCell.appendChild(delBtn);
            row.appendChild(actionsCell);
            tableBody.appendChild(row);
        });
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to load users: ' + error.message);
    }
}

async function addUser() {
    const username = document.getElementById('new-user-username').value;
    const password = document.getElementById('new-user-password').value;
    const role = document.getElementById('new-user-role').value;
    if (!username || !password || !role) {
        showError('users', 'Please fill in all fields.');
        return;
    }
    try {
        await postData('/api/users', { username, password, role });
        document.getElementById('new-user-username').value = '';
        document.getElementById('new-user-password').value = '';
        document.getElementById('new-user-role').value = 'USER';
        await loadUsers();
        await populateLoanDropdowns();
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to add user: ' + error.message);
    }
}

async function editUser(id) {
    const data = await fetchData(`/api/users/${id}`);
    document.getElementById('new-user-username').value = data.username || '';
    document.getElementById('new-user-password').value = '';
    const roleSelect = document.getElementById('new-user-role');
    const roles = data.roles;
    if (roles && roles.length > 0) {
        roleSelect.value = roles[0];
    }
    const btn = document.getElementById('add-user-btn');
    btn.textContent = 'Update User';
    btn.onclick = () => updateUser(id);
}

async function updateUser(id) {
    const username = document.getElementById('new-user-username').value;
    const password = document.getElementById('new-user-password').value;
    const role = document.getElementById('new-user-role').value;
    if (!username || !role) {
        showError('users', 'Please fill in username and role.');
        return;
    }
    try {
        await putData(`/api/users/${id}`, { username, password: password || null, role });
        document.getElementById('new-user-username').value = '';
        document.getElementById('new-user-password').value = '';
        document.getElementById('new-user-role').value = 'USER';
        await loadUsers();
        await populateLoanDropdowns();
        const btn = document.getElementById('add-user-btn');
        btn.textContent = 'Add User';
        btn.onclick = addUser;
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to update user: ' + error.message);
    }
}

async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
        await deleteData(`/api/users/${id}`);
        await loadUsers();
        await populateLoanDropdowns();
        clearError('users');
    } catch (error) {
        showError('users', 'Failed to delete user: ' + error.message);
    }
}

async function viewUser(id) {
    const data = await fetchData(`/api/users/${id}`);
    const rolesText = data.roles ? Array.from(data.roles).join(', ') : '';
    alert(`User Details:\nID: ${data.id}\nUsername: ${data.username}\nRoles: ${rolesText}`);
}

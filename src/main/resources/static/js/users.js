// (c) Copyright 2025 by Muczynski
import { hashPassword } from './utils.js';

function getSsoProviderBadge(ssoProvider) {
    if (!ssoProvider || ssoProvider === 'local') {
        return '<span class="badge bg-secondary">Local</span>';
    }
    if (ssoProvider === 'google') {
        return '<span class="badge bg-primary">Google</span>';
    }
    return `<span class="badge bg-info">${ssoProvider}</span>`;
}

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

            const loansCell = document.createElement('td');
            loansCell.setAttribute('data-test', 'user-loans-count');
            loansCell.textContent = user.activeLoansCount || 0;
            row.appendChild(loansCell);

            const ssoCell = document.createElement('td');
            ssoCell.setAttribute('data-test', 'user-sso-provider');
            ssoCell.innerHTML = getSsoProviderBadge(user.ssoProvider);
            row.appendChild(ssoCell);

            const actionsCell = document.createElement('td');

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
        showError('users', 'Name, password, and role are required.');
        return;
    }
    try {
        const hashedPassword = await hashPassword(password);
        await postData('/api/users', { username, password: hashedPassword, role });
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
        showError('users', 'Name and role are required.');
        return;
    }
    try {
        const hashedPassword = password ? await hashPassword(password) : null;
        await putData(`/api/users/${id}`, { username, password: hashedPassword, role });
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

// Expose functions globally for HTML onclick handlers and sections.js
window.loadUsers = loadUsers;
window.addUser = addUser;

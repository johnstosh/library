async function loadApplied() {
    if (!isLibrarian) return;
    try {
        const applications = await fetchData('/api/applied');
        const appliedListBody = document.getElementById('applied-list-body');
        appliedListBody.innerHTML = '';
        applications.forEach(app => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${app.name}</td>
                <td>
                    <select class="form-select" onchange="updateAppliedStatus(${app.id}, this.value)">
                        <option value="PENDING" ${app.status === 'PENDING' ? 'selected' : ''}>Pending</option>
                        <option value="APPROVED" ${app.status === 'APPROVED' ? 'selected' : ''}>Approved</option>
                        <option value="NOT_APPROVED" ${app.status === 'NOT_APPROVED' ? 'selected' : ''}>Not Approved</option>
                        <option value="QUESTION" ${app.status === 'QUESTION' ? 'selected' : ''}>Question</option>
                    </select>
                </td>
                <td>
                    <button class="btn btn-success btn-sm" onclick="approveApplication(${app.id})">✔️</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteApplied(${app.id})">🗑️</button>
                </td>
            `;
            appliedListBody.appendChild(row);
        });
    } catch (error) {
        console.error('Failed to load applications:', error);
        showError('library-card', 'Failed to load applications.');
    }
}

async function approveApplication(id) {
    if (!confirm('Are you sure you want to approve this application?')) {
        return;
    }
    try {
        await postData(`/api/applied/${id}/approve`, {}, false, false);
        loadApplied();
    } catch (error) {
        console.error(`Failed to approve application ${id}:`, error);
        showError('library-card', `Failed to approve application ${id}.`);
    }
}

async function updateAppliedStatus(id, status) {
    try {
        await putData(`/api/applied/${id}`, { status });
        loadApplied();
    } catch (error) {
        console.error(`Failed to update application ${id}:`, error);
        showError('library-card', `Failed to update application ${id}.`);
    }
}

async function deleteApplied(id) {
    if (!confirm('Are you sure you want to delete this application?')) {
        return;
    }
    try {
        await deleteData(`/api/applied/${id}`);
        loadApplied();
    } catch (error) {
        console.error(`Failed to delete application ${id}:`, error);
        showError('library-card', `Failed to delete application ${id}.`);
    }
}